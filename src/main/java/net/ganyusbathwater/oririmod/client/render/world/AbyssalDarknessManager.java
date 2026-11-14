package net.ganyusbathwater.oririmod.client.render.world;

import com.mojang.blaze3d.shaders.FogShape;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.ganyusbathwater.oririmod.particle.ModParticles;
import net.ganyusbathwater.oririmod.util.ShaderDetector;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Manages the abyssal darkness system for The Silent Depths and Sunken Trenches.
 *
 * Provides:
 * 1. Interior sightline restriction (strictly <= 12m spherical radius in deep water).
 * 2. Surface sightline restriction (strictly <= 24m spherical radius when in a boat or on water surface).
 * 3. Complete Night Vision neutralization (locks fog far-plane, cancels event).
 * 4. Skybox and unloaded-chunk light leakage elimination (forces black sky color and cancels skybox rendering).
 * 5. Exterior occlusion: looking into the trench from outside pulls fog closer and darkens color.
 * 6. True Vision integration: completely negates abyssal darkness, disables fog obscuration, and purges debuffs.
 */
public class AbyssalDarknessManager {

    public static final ResourceKey<Biome> SILENT_DEPTHS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_silent_depths"));
    public static final ResourceKey<Biome> SUNKEN_TRENCHES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_sunken_trenches"));
    public static final ResourceKey<Biome> SILENT_OCEANS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_silent_oceans"));

    // Fog boundaries
    public static final float ABYSSAL_NEAR_PLANE = 0.5F;
    public static final float ABYSSAL_FAR_PLANE = 13.5F;
    public static final float ABYSSAL_LIT_NEAR_PLANE = 1.6F;
    public static final float ABYSSAL_LIT_FAR_PLANE = 19.5F;
    public static final float SURFACE_FAR_PLANE = 24.0F;
    public static final float SURFACE_LIT_FAR_PLANE = 29.0F;

    // Very dark abyssal ocean color with subtle midnight navy lift for close-up silhouettes
    public static final float ABYSSAL_RED = 0.005F;
    public static final float ABYSSAL_GREEN = 0.011F;
    public static final float ABYSSAL_BLUE = 0.018F;

    // Dark sea-mist color for boat / surface air
    public static final float SURFACE_RED = 0.008F;
    public static final float SURFACE_GREEN = 0.014F;
    public static final float SURFACE_BLUE = 0.022F;

    // Transition tracking
    private static float darknessFactor = 0.0F;
    private static float surfaceDarknessFactor = 0.0F;
    private static float exteriorGazeFactor = 0.0F;
    private static float effectiveDarkness = 0.0F;
    private static float lightRelief = 0.0F;
    private static float prevLightRelief = 0.0F;
    private static float heldLightStrength = 0.0F;
    private static boolean isSubmergedInAbyss = false;

    private static final RandomSource RANDOM = RandomSource.create();

    public static boolean isSubmergedInAbyss() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModEffects.TRUE_VISION_EFFECT)) {
            return false;
        }
        return isSubmergedInAbyss || surfaceDarknessFactor >= 0.75F;
    }

    public static float getDarknessIntensity() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModEffects.TRUE_VISION_EFFECT)) {
            return 0.0F;
        }
        return Math.max(effectiveDarkness, surfaceDarknessFactor * 0.9F);
    }

    /**
     * Strength of lightmap unlit-floor suppression (0 = none, 1 = submerged abyss pitch black).
     * Never suppresses while True Vision is active.
     */
    public static float getLightmapSuppression() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModEffects.TRUE_VISION_EFFECT)) {
            return 0.0F;
        }
        return darknessFactor;
    }

    public static float getHeldLightStrength() {
        return heldLightStrength;
    }

    public static float getEffectiveLightRelief(float partialTick) {
        return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), prevLightRelief, lightRelief);
    }

    public static void renderShaderAbyssOverlay(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModEffects.TRUE_VISION_EFFECT)) {
            return;
        }
        ShaderAbyssOverlayRenderer.render(
                darknessFactor,
                surfaceDarknessFactor,
                exteriorGazeFactor,
                getEffectiveLightRelief(partialTick));
    }

    private static void resetDarknessState() {
        darknessFactor = 0.0F;
        surfaceDarknessFactor = 0.0F;
        exteriorGazeFactor = 0.0F;
        effectiveDarkness = 0.0F;
        lightRelief = 0.0F;
        prevLightRelief = 0.0F;
        heldLightStrength = 0.0F;
        isSubmergedInAbyss = false;
    }

    /**
     * Ticks client-side darkness transitions and spawns volumetric boundary shroud particles.
     * Called every client tick from OririClient.onClientTick.
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            resetDarknessState();
            return;
        }

        // If player has True Vision, completely bypass darkness accumulation
        if (mc.player.hasEffect(ModEffects.TRUE_VISION_EFFECT)) {
            resetDarknessState();
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        BlockPos blockPos = camera.getBlockPosition();
        boolean inWater = (camera.getFluidInCamera() == FogType.WATER);

        Holder<Biome> currentBiome = mc.level.getBiome(blockPos);
        boolean isAbyssBiome = currentBiome.is(SILENT_DEPTHS_KEY) || currentBiome.is(SUNKEN_TRENCHES_KEY);
        boolean isOceanBiome = currentBiome.is(SILENT_OCEANS_KEY);

        // Aphotic zone condition: inside Silent Depths / Trenches, or below Y=38 in oceanic columns
        boolean isAbyssalDepth = isAbyssBiome || (isOceanBiome && blockPos.getY() <= 38);
        isSubmergedInAbyss = inWater && isAbyssalDepth;

        // Smoothly interpolate interior deep-water darkness factor
        float targetFactor = isSubmergedInAbyss ? 1.0F : 0.0F;
        darknessFactor = Mth.lerp(0.08F, darknessFactor, targetFactor);
        if (Math.abs(darknessFactor - targetFactor) < 0.005F) {
            darknessFactor = targetFactor;
        }

        // Surface / Boat air darkness: when player is on a boat or at the water surface (Y <= 78) over the abyss
        float targetSurface = 0.0F;
        if (!inWater && blockPos.getY() <= 78) {
            if (isAbyssBiome) {
                targetSurface = 1.0F;
            } else if (isOceanBiome && blockPos.getY() <= 74) {
                targetSurface = 0.35F;
            }
        }

        // Exterior gaze check: if player is in water or on surface, check if gaze is aimed into the depths
        float targetGaze = 0.0F;
        if ((inWater || blockPos.getY() <= 78) && darknessFactor < 0.9F) {
            org.joml.Vector3f lookVec = camera.getLookVector();
            Vec3 look = new Vec3(lookVec.x(), lookVec.y(), lookVec.z());
            // Raymarch 4 sample points along look vector (8m, 16m, 24m, 32m)
            for (int step = 1; step <= 4; step++) {
                double dist = step * 8.0;
                Vec3 sampleVec = camPos.add(look.scale(dist));
                BlockPos samplePos = BlockPos.containing(sampleVec);
                Holder<Biome> sampleBiome = mc.level.getBiome(samplePos);
                if (sampleBiome.is(SILENT_DEPTHS_KEY) || sampleBiome.is(SUNKEN_TRENCHES_KEY)
                        || (samplePos.getY() <= 38 && sampleBiome.is(SILENT_OCEANS_KEY))) {
                    targetGaze = Math.max(targetGaze, 1.0F - ((step - 1) * 0.22F));
                    break;
                }
            }
        }
        exteriorGazeFactor = Mth.lerp(0.1F, exteriorGazeFactor, targetGaze);
        if (Math.abs(exteriorGazeFactor - targetGaze) < 0.005F) {
            exteriorGazeFactor = targetGaze;
        }

        if (!inWater && blockPos.getY() <= 78) {
            targetSurface = Math.max(targetSurface, exteriorGazeFactor * 0.85F);
        }
        surfaceDarknessFactor = Mth.lerp(0.08F, surfaceDarknessFactor, targetSurface);
        if (Math.abs(surfaceDarknessFactor - targetSurface) < 0.005F) {
            surfaceDarknessFactor = targetSurface;
        }

        // Combine interior darkness and exterior gaze pull-in
        effectiveDarkness = Math.max(darknessFactor, exteriorGazeFactor * 0.82F);

        // Smooth dynamic light response (held sources, local lightmap blocks, forward beacon cone)
        prevLightRelief = lightRelief;
        AbyssalLightProbe.LightSample sample = AbyssalLightProbe.sample(mc.level, mc.player, camera, blockPos);
        heldLightStrength = sample.heldStrength();
        float lerpSpeed = sample.relief() > lightRelief ? 0.14F : 0.08F;
        lightRelief = Mth.lerp(lerpSpeed, lightRelief, sample.relief());
        if (Math.abs(lightRelief - sample.relief()) < 0.004F) {
            lightRelief = sample.relief();
        }

        // Volumetric boundary shroud particle spawner
        tickBoundaryShroudParticles(mc.level, mc.player, isAbyssBiome, isOceanBiome, blockPos);
    }

    /**
     * Spawns physical churning cloud billows along the aphotic boundary (Y=30 to 44)
     * and ambient drifting deep-sea clouds in the trench interior, thinning around light sources.
     */
    private static void tickBoundaryShroudParticles(ClientLevel level, Player player,
                                                    boolean isAbyssBiome, boolean isOceanBiome,
                                                    BlockPos playerPos) {
        if (player.hasEffect(ModEffects.TRUE_VISION_EFFECT)) {
            return;
        }

        // Only spawn when player is in or near oceanic / abyssal territory
        if (!isAbyssBiome && !isOceanBiome && playerPos.getY() > 76) {
            return;
        }

        boolean shaderActive = ShaderDetector.isShaderActive();

        // 1. Boundary ceiling shroud (Y=30 to 44): creates the rolling cloud barrier visible from above
        int boundaryRange = shaderActive ? 20 : 26;
        int boundarySpan = boundaryRange * 2 + 1;
        int boundaryPuffs = 4;
        for (int i = 0; i < boundaryPuffs; i++) {
            int rx = playerPos.getX() + RANDOM.nextInt(boundarySpan) - boundaryRange;
            int rz = playerPos.getZ() + RANDOM.nextInt(boundarySpan) - boundaryRange;
            int ry = 30 + RANDOM.nextInt(15); // Y=30 to 44
            trySpawnShroudPuff(level, player, new BlockPos(rx, ry, rz), true, 0.8);
        }

        // 2. Interior deep murk: when player is down inside the depths, spawn ambient drifting clouds
        if (isAbyssBiome && playerPos.getY() <= 32) {
            int deepRange = shaderActive ? 13 : 16;
            int deepSpan = deepRange * 2 + 1;
            int verticalRange = shaderActive ? 6 : 8;
            int deepPuffs = shaderActive ? 3 : 2;
            for (int i = 0; i < deepPuffs; i++) {
                int rx = playerPos.getX() + RANDOM.nextInt(deepSpan) - deepRange;
                int rz = playerPos.getZ() + RANDOM.nextInt(deepSpan) - deepRange;
                int ry = playerPos.getY() + RANDOM.nextInt(verticalRange * 2 + 1) - verticalRange;
                trySpawnShroudPuff(level, player, new BlockPos(rx, ry, rz), false, 0.5);
            }
        }
    }

    private static void trySpawnShroudPuff(
            ClientLevel level,
            Player player,
            BlockPos candidate,
            boolean checkOceanBiome,
            double jitterScale) {

        if (!level.getFluidState(candidate).is(FluidTags.WATER)) {
            return;
        }
        if (checkOceanBiome) {
            Holder<Biome> biome = level.getBiome(candidate);
            if (!biome.is(SILENT_DEPTHS_KEY) && !biome.is(SUNKEN_TRENCHES_KEY) && !biome.is(SILENT_OCEANS_KEY)) {
                return;
            }
        }

        int blockLight = level.getBrightness(LightLayer.BLOCK, candidate);
        if (blockLight >= 11 || (blockLight > 2 && RANDOM.nextFloat() < (blockLight / 15.0F))) {
            return;
        }
        if (heldLightStrength > 0.25F
                && candidate.distToCenterSqr(player.position()) < 49.0
                && RANDOM.nextFloat() < heldLightStrength * 0.35F) {
            return;
        }

        double px = candidate.getX() + 0.5 + (RANDOM.nextDouble() - 0.5) * jitterScale;
        double py = candidate.getY() + 0.5 + (RANDOM.nextDouble() - 0.5) * jitterScale;
        double pz = candidate.getZ() + 0.5 + (RANDOM.nextDouble() - 0.5) * jitterScale;
        level.addParticle(ModParticles.ABYSSAL_SHROUD.get(), px, py, pz, 0.0, 0.0, 0.0);
    }

    /**
     * Intercepts RenderFog to clamp sightlines and neutralize Night Vision,
     * dynamically opening visibility around placed and handheld light sources.
     * When True Vision is active, completely disables fog obstruction and sets crystal-clear view distance!
     */
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModEffects.TRUE_VISION_EFFECT)) {
            // True Vision: crystal clear perception, pushes fog completely out
            event.setNearPlaneDistance(64.0F);
            event.setFarPlaneDistance(256.0F);
            event.setFogShape(FogShape.SPHERE);
            event.setCanceled(true);
            return;
        }

        float light = getEffectiveLightRelief((float) event.getPartialTick());
        FogType fluid = event.getCamera().getFluidInCamera();

        if (fluid == FogType.WATER) {
            if (effectiveDarkness > 0.01F) {
                float targetNear = Mth.lerp(light, ABYSSAL_NEAR_PLANE, ABYSSAL_LIT_NEAR_PLANE);
                float targetFar = Mth.lerp(light, ABYSSAL_FAR_PLANE, ABYSSAL_LIT_FAR_PLANE);
                applyFogEnvelope(event, effectiveDarkness, targetNear, targetFar);
            }
        } else if (fluid == FogType.NONE) {
            // Surface / Boat air fog: clamp sphere to eliminate distant visibility while responding to nearby light
            if (surfaceDarknessFactor > 0.01F) {
                float targetFar = Mth.lerp(light, SURFACE_FAR_PLANE, SURFACE_LIT_FAR_PLANE);
                applyFogEnvelope(event, surfaceDarknessFactor, 2.0F, targetFar);
            }
        }
    }

    private static void applyFogEnvelope(ViewportEvent.RenderFog event, float intensity, float targetNear, float targetFar) {
        float origNear = event.getNearPlaneDistance();
        float origFar = event.getFarPlaneDistance();

        float clampedFar = Math.min(origFar, targetFar);
        event.setNearPlaneDistance(Mth.lerp(intensity, origNear, targetNear));
        event.setFarPlaneDistance(Mth.lerp(intensity, origFar, clampedFar));
        event.setFogShape(FogShape.SPHERE);
        // Canceling writes back updated planes/shape to RenderSystem across all intensities (smooth transition)
        event.setCanceled(true);
    }

    /**
     * Intercepts ComputeFogColor to force fog color to deep marine black / sea-mist.
     */
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModEffects.TRUE_VISION_EFFECT)) {
            return; // Do not modify fog color for True Vision users
        }

        float light = getEffectiveLightRelief((float) event.getPartialTick());
        FogType fluid = event.getCamera().getFluidInCamera();

        if (fluid == FogType.WATER) {
            if (effectiveDarkness > 0.01F) {
                float targetR = Mth.lerp(light * 0.45F, ABYSSAL_RED, 0.006F);
                float targetG = Mth.lerp(light * 0.45F, ABYSSAL_GREEN, 0.016F);
                float targetB = Mth.lerp(light * 0.45F, ABYSSAL_BLUE, 0.028F);
                event.setRed(Mth.lerp(effectiveDarkness, event.getRed(), targetR));
                event.setGreen(Mth.lerp(effectiveDarkness, event.getGreen(), targetG));
                event.setBlue(Mth.lerp(effectiveDarkness, event.getBlue(), targetB));
            }
        } else if (fluid == FogType.NONE) {
            if (surfaceDarknessFactor > 0.01F) {
                event.setRed(Mth.lerp(surfaceDarknessFactor, event.getRed(), SURFACE_RED));
                event.setGreen(Mth.lerp(surfaceDarknessFactor, event.getGreen(), SURFACE_GREEN));
                event.setBlue(Mth.lerp(surfaceDarknessFactor, event.getBlue(), SURFACE_BLUE));
            }
        }
    }
}
