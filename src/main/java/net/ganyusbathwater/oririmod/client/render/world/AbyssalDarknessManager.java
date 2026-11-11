package net.ganyusbathwater.oririmod.client.render.world;

import com.mojang.blaze3d.shaders.FogShape;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.ganyusbathwater.oririmod.particle.ModParticles;
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
    public static final float ABYSSAL_NEAR_PLANE = 0.0F;
    public static final float ABYSSAL_FAR_PLANE = 12.0F;
    public static final float SURFACE_FAR_PLANE = 24.0F;

    // Pitch black abyssal ocean color with faint midnight navy hue
    public static final float ABYSSAL_RED = 0.002F;
    public static final float ABYSSAL_GREEN = 0.005F;
    public static final float ABYSSAL_BLUE = 0.008F;

    // Dark sea-mist color for boat / surface air
    public static final float SURFACE_RED = 0.008F;
    public static final float SURFACE_GREEN = 0.014F;
    public static final float SURFACE_BLUE = 0.022F;

    // Transition tracking
    private static float darknessFactor = 0.0F;
    private static float surfaceDarknessFactor = 0.0F;
    private static float exteriorGazeFactor = 0.0F;
    private static float effectiveDarkness = 0.0F;
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
     * Ticks client-side darkness transitions and spawns volumetric boundary shroud particles.
     * Called every client tick from OririClient.onClientTick.
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            darknessFactor = 0.0F;
            surfaceDarknessFactor = 0.0F;
            exteriorGazeFactor = 0.0F;
            effectiveDarkness = 0.0F;
            isSubmergedInAbyss = false;
            return;
        }

        // If player has True Vision, completely bypass darkness accumulation
        if (mc.player.hasEffect(ModEffects.TRUE_VISION_EFFECT)) {
            darknessFactor = 0.0F;
            surfaceDarknessFactor = 0.0F;
            exteriorGazeFactor = 0.0F;
            effectiveDarkness = 0.0F;
            isSubmergedInAbyss = false;
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

        // Volumetric boundary shroud particle spawner
        tickBoundaryShroudParticles(mc.level, mc.player, isAbyssBiome, isOceanBiome, blockPos);
    }

    /**
     * Spawns physical churning cloud billows along the aphotic boundary (Y=30 to 44)
     * and ambient drifting deep-sea clouds in the trench interior.
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

        // 1. Boundary ceiling shroud (Y=30 to 44): creates the rolling cloud barrier visible from above
        int boundaryPuffs = 4;
        for (int i = 0; i < boundaryPuffs; i++) {
            int rx = playerPos.getX() + RANDOM.nextInt(53) - 26;
            int rz = playerPos.getZ() + RANDOM.nextInt(53) - 26;
            int ry = 30 + RANDOM.nextInt(15); // Y=30 to 44
            BlockPos candidate = new BlockPos(rx, ry, rz);

            if (level.getFluidState(candidate).is(FluidTags.WATER)) {
                Holder<Biome> b = level.getBiome(candidate);
                if (b.is(SILENT_DEPTHS_KEY) || b.is(SUNKEN_TRENCHES_KEY) || b.is(SILENT_OCEANS_KEY)) {
                    double px = rx + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.8;
                    double py = ry + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.8;
                    double pz = rz + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.8;
                    level.addParticle(ModParticles.ABYSSAL_SHROUD.get(), px, py, pz, 0.0, 0.0, 0.0);
                }
            }
        }

        // 2. Interior deep murk: when player is down inside the depths, spawn ambient drifting clouds
        if (isAbyssBiome && playerPos.getY() <= 32) {
            int deepPuffs = 2;
            for (int i = 0; i < deepPuffs; i++) {
                int rx = playerPos.getX() + RANDOM.nextInt(33) - 16;
                int rz = playerPos.getZ() + RANDOM.nextInt(33) - 16;
                int ry = playerPos.getY() + RANDOM.nextInt(17) - 8;
                BlockPos candidate = new BlockPos(rx, ry, rz);

                if (level.getFluidState(candidate).is(FluidTags.WATER)) {
                    double px = rx + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.5;
                    double py = ry + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.5;
                    double pz = rz + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.5;
                    level.addParticle(ModParticles.ABYSSAL_SHROUD.get(), px, py, pz, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    /**
     * Intercepts RenderFog to clamp sightlines and neutralize Night Vision.
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

        FogType fluid = event.getCamera().getFluidInCamera();

        if (fluid == FogType.WATER) {
            if (effectiveDarkness > 0.01F) {
                float origNear = event.getNearPlaneDistance();
                float origFar = event.getFarPlaneDistance();

                float targetFar = Math.min(origFar, ABYSSAL_FAR_PLANE);
                float newNear = Mth.lerp(effectiveDarkness, origNear, ABYSSAL_NEAR_PLANE);
                float newFar = Mth.lerp(effectiveDarkness, origFar, targetFar);

                event.setNearPlaneDistance(newNear);
                event.setFarPlaneDistance(newFar);
                event.setFogShape(FogShape.SPHERE);

                // Neutralize Night Vision water fog expansion (canceling locks to <= 12m)
                if (effectiveDarkness >= 0.70F) {
                    event.setCanceled(true);
                }
            }
        } else if (fluid == FogType.NONE) {
            // Surface / Boat air fog: clamp to 24m sphere to eliminate distant visibility
            if (surfaceDarknessFactor > 0.01F) {
                float origNear = event.getNearPlaneDistance();
                float origFar = event.getFarPlaneDistance();

                float targetFar = Math.min(origFar, SURFACE_FAR_PLANE);
                float newNear = Mth.lerp(surfaceDarknessFactor, origNear, 2.0F);
                float newFar = Mth.lerp(surfaceDarknessFactor, origFar, targetFar);

                event.setNearPlaneDistance(newNear);
                event.setFarPlaneDistance(newFar);
                event.setFogShape(FogShape.SPHERE);

                // Neutralize Night Vision in air over the depths (canceling locks to <= 24m)
                if (surfaceDarknessFactor >= 0.70F) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * Intercepts ComputeFogColor to force fog color to deep marine black / sea-mist.
     */
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModEffects.TRUE_VISION_EFFECT)) {
            return; // Do not modify fog color for True Vision users
        }

        FogType fluid = event.getCamera().getFluidInCamera();

        if (fluid == FogType.WATER) {
            if (effectiveDarkness > 0.01F) {
                event.setRed(Mth.lerp(effectiveDarkness, event.getRed(), ABYSSAL_RED));
                event.setGreen(Mth.lerp(effectiveDarkness, event.getGreen(), ABYSSAL_GREEN));
                event.setBlue(Mth.lerp(effectiveDarkness, event.getBlue(), ABYSSAL_BLUE));
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
