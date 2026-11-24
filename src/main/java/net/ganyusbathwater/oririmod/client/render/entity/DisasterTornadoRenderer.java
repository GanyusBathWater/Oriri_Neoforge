package net.ganyusbathwater.oririmod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.ModRenderTypes;
import net.ganyusbathwater.oririmod.entity.custom.DisasterTornadoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Procedural shader-compatible renderer for the Natural Disaster Tornado.
 *
 * <p>Constructs dynamic swirling ribbon cylinders along a Perlin-bent spine curve.
 * Avoids rigid 3D models while remaining fully compatible with Iris/Sodium shaders,
 * fog, ambient sky lighting, and Bloom.
 */
public class DisasterTornadoRenderer extends EntityRenderer<DisasterTornadoEntity> {

    private static final ResourceLocation BEAM_NOISE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/effect/beam_noise.png");
    private static final ResourceLocation SMOKE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/explosion_smoke.png");

    private static final RenderType VORTEX_SHEATH_LAYER = ModRenderTypes.getTornadoVortex(BEAM_NOISE_TEXTURE);
    private static final RenderType DUST_CORE_LAYER = ModRenderTypes.getTornadoVortex(SMOKE_TEXTURE);

    // Smoothly interpolated per-entity color state: [coreR, coreG, coreB, sheathR, sheathG, sheathB]
    private static final Map<DisasterTornadoEntity, float[]> SMOOTHED_COLORS = new WeakHashMap<>();

    public DisasterTornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(DisasterTornadoEntity livingEntity, net.minecraft.client.renderer.culling.Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(DisasterTornadoEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float totalHeight = entity.getFunnelHeight();
        float baseRadius = entity.getBaseRadius();
        float topRadius = entity.getTopRadius();
        float skirtDepth = Math.max(6.0f, entity.getGroundSkirtDepth());

        if (totalHeight <= 0.5f || baseRadius <= 0.1f) {
            return;
        }

        float time = entity.tickCount + partialTick;

        // Resolve biome-based palette and smoothly morph color as the tornado moves between biomes
        float[] targetColors = resolveBiomeColors(entity);
        float[] currentColors = SMOOTHED_COLORS.computeIfAbsent(entity, e -> targetColors.clone());
        float lerpSpeed = 0.035f;
        for (int c = 0; c < 6; c++) {
            currentColors[c] = Mth.lerp(lerpSpeed, currentColors[c], targetColors[c]);
        }

        poseStack.pushPose();

        // 1. Render Inner Dust Core Cylinder (High resolution slices + seamless downward terrain skirt)
        VertexConsumer coreConsumer = buffer.getBuffer(DUST_CORE_LAYER);
        renderVortexCylinder(poseStack, coreConsumer, time, totalHeight, skirtDepth,
                baseRadius * 0.85f, topRadius * 0.82f, 36, 18,
                currentColors[0], currentColors[1], currentColors[2], 0.84f, 0.06f, 0.035f, packedLight);

        // 2. Render Outer Swirling Wind Ribbons (Depth-writing emissive vortex + seamless downward terrain skirt)
        VertexConsumer sheathConsumer = buffer.getBuffer(VORTEX_SHEATH_LAYER);
        renderVortexCylinder(poseStack, sheathConsumer, time, totalHeight, skirtDepth,
                baseRadius * 1.15f, topRadius * 1.05f, 40, 24,
                currentColors[3], currentColors[4], currentColors[5], 0.68f, -0.09f, 0.055f, LightTexture.FULL_BRIGHT);

        // 3. Render Orbiting 3D Debris Blocks Tumbling Up the Funnel Wall
        renderOrbitingDebris(entity, poseStack, buffer, time, totalHeight, baseRadius, topRadius, packedLight);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /**
     * Computes target [coreR, coreG, coreB, sheathR, sheathG, sheathB] based on the tornado's current biome.
     */
    private static float[] resolveBiomeColors(DisasterTornadoEntity entity) {
        Holder<Biome> biome = entity.level().getBiome(entity.blockPosition());
        String path = biome.unwrapKey()
                .map(ResourceKey::location)
                .map(ResourceLocation::getPath)
                .orElse("");

        // Scarlet biomes & Crimson Nether -> Deep Crimson Red
        if (path.contains("scarlet") || path.contains("crimson")) {
            return new float[] { 0.56f, 0.10f, 0.12f, 0.95f, 0.24f, 0.26f };
        }
        // Vile Plains / Vile Caves -> Dark Violet-Purple
        if (path.contains("vile") || path.contains("end_") || path.equals("the_end")) {
            return new float[] { 0.38f, 0.15f, 0.54f, 0.78f, 0.40f, 0.98f };
        }
        // Desert / Golden Desert / Beach -> Golden-Yellow Sandstorm
        if (path.contains("desert") || path.contains("beach") || biome.is(BiomeTags.IS_BEACH)) {
            return new float[] { 0.78f, 0.64f, 0.26f, 0.98f, 0.88f, 0.44f };
        }
        // Golden Savanna / Mesa / Badlands -> Warm Terracotta-Amber Dust Devil
        if (path.contains("mesa") || path.contains("savanna") || biome.is(BiomeTags.IS_BADLANDS) || biome.is(BiomeTags.IS_SAVANNA)) {
            return new float[] { 0.72f, 0.46f, 0.22f, 0.96f, 0.72f, 0.38f };
        }
        // Snowy / Frozen / Alpine Summit -> Icy White-Cyan Blizzard Funnel
        if (path.contains("snow") || path.contains("ice") || path.contains("frozen")
                || path.contains("glacier") || path.contains("summit") || biome.value().getBaseTemperature() <= 0.15f) {
            return new float[] { 0.72f, 0.84f, 0.92f, 0.95f, 0.98f, 1.00f };
        }
        // Oceans / Silent Depths / Rivers / Lagoons -> Waterspout Teal-Blue
        if (path.contains("silent_") || path.contains("lagoon") || path.contains("valley")
                || biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(BiomeTags.IS_RIVER)) {
            return new float[] { 0.16f, 0.40f, 0.54f, 0.56f, 0.86f, 0.98f };
        }
        // Elderwoods / Elder Highlands -> Mystical Deep Cyan-Slate
        if (path.contains("elder")) {
            return new float[] { 0.18f, 0.36f, 0.44f, 0.58f, 0.86f, 0.94f };
        }
        // Swamp / Mangrove / Jungle -> Murky Mossy Green
        if (path.contains("swamp") || path.contains("mangrove") || biome.is(BiomeTags.IS_JUNGLE)) {
            return new float[] { 0.24f, 0.36f, 0.20f, 0.62f, 0.80f, 0.54f };
        }
        // Cherry / Flower biomes -> Soft Blossom Pink-Grey
        if (path.contains("cherry")) {
            return new float[] { 0.56f, 0.36f, 0.44f, 0.96f, 0.74f, 0.82f };
        }
        // Default (Plains, Forests, Hills, etc.) -> Classic Stormy Charcoal-Grey
        return new float[] { 0.24f, 0.22f, 0.20f, 0.88f, 0.88f, 0.92f };
    }

    private void renderVortexCylinder(PoseStack poseStack, VertexConsumer consumer, float time,
                                      float height, float skirtDepth, float rBase, float rTop, int verticalSlices,
                                      int radialSegments, float r, float g, float b, float alpha,
                                      float rotSpeed, float vSpeed, int light) {
        Matrix4f mat = poseStack.last().pose();
        int skirtSlices = Math.max(4, Mth.ceil(skirtDepth / 2.5f));
        int totalSlices = skirtSlices + verticalSlices;

        for (int i = 0; i < totalSlices; i++) {
            float y0 = (i < skirtSlices)
                    ? -skirtDepth + (i / (float) skirtSlices) * skirtDepth
                    : ((i - skirtSlices) / (float) verticalSlices) * height;
            float y1 = (i + 1 <= skirtSlices)
                    ? -skirtDepth + ((i + 1) / (float) skirtSlices) * skirtDepth
                    : (((i + 1) - skirtSlices) / (float) verticalSlices) * height;

            float t0 = y0 / height;
            float t1 = y1 / height;

            // Above ground: curved expansion (narrow ground spout, dramatic cloud-base trumpet).
            // Below y=0 (terrain skirt): keep grounded at rBase with a gentle ground-hugging flare so
            // the funnel bottom never visibly cuts off in midair on hills, slopes, or cliffs.
            float rad0 = y0 >= 0.0f
                    ? rBase + (rTop - rBase) * (float) Math.pow(t0, 1.55)
                    : rBase * (1.0f + 0.06f * Mth.clamp(-y0 / Math.max(1.0f, skirtDepth), 0.0f, 1.0f));
            float rad1 = y1 >= 0.0f
                    ? rBase + (rTop - rBase) * (float) Math.pow(t1, 1.55)
                    : rBase * (1.0f + 0.06f * Mth.clamp(-y1 / Math.max(1.0f, skirtDepth), 0.0f, 1.0f));

            // Bending spine offsets (anchored to 0 below y=0 so the terrain skirt plunges straight down)
            float offX0 = getSpineOffsetX(time, y0);
            float offZ0 = getSpineOffsetZ(time, y0);
            float offX1 = getSpineOffsetX(time, y1);
            float offZ1 = getSpineOffsetZ(time, y1);

            float v0 = -(time * vSpeed + t0 * 3.0f);
            float v1 = -(time * vSpeed + t1 * 3.0f);

            for (int j = 0; j < radialSegments; j++) {
                float angle0 = (j / (float) radialSegments) * ((float) Math.PI * 2.0f) + (time * rotSpeed);
                float angle1 = ((j + 1) / (float) radialSegments) * ((float) Math.PI * 2.0f) + (time * rotSpeed);

                float u0 = j / (float) radialSegments;
                float u1 = (j + 1) / (float) radialSegments;

                float x00 = offX0 + (float) Math.cos(angle0) * rad0;
                float z00 = offZ0 + (float) Math.sin(angle0) * rad0;

                float x10 = offX0 + (float) Math.cos(angle1) * rad0;
                float z10 = offZ0 + (float) Math.sin(angle1) * rad0;

                float x01 = offX1 + (float) Math.cos(angle0) * rad1;
                float z01 = offZ1 + (float) Math.sin(angle0) * rad1;

                float x11 = offX1 + (float) Math.cos(angle1) * rad1;
                float z11 = offZ1 + (float) Math.sin(angle1) * rad1;

                // Quad vertex definitions
                consumer.addVertex(mat, x00, y0, z00).setColor(r, g, b, alpha).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0f, 1.0f, 0.0f);
                consumer.addVertex(mat, x10, y0, z10).setColor(r, g, b, alpha).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0f, 1.0f, 0.0f);
                consumer.addVertex(mat, x11, y1, z11).setColor(r, g, b, alpha).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0f, 1.0f, 0.0f);
                consumer.addVertex(mat, x01, y1, z01).setColor(r, g, b, alpha).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0f, 1.0f, 0.0f);
            }
        }
    }

    private void renderOrbitingDebris(DisasterTornadoEntity entity, PoseStack poseStack, MultiBufferSource buffer,
                                      float time, float totalHeight, float baseRadius, float topRadius, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        int debrisCount = 36;

        for (int i = 0; i < debrisCount; i++) {
            // Distribute debris along the funnel height
            float heightRatio = ((i * 0.055f + (time * 0.008f)) % 0.85f) + 0.05f;
            float debY = heightRatio * totalHeight;

            float debRad = (baseRadius + (topRadius - baseRadius) * (float) Math.pow(heightRatio, 1.55)) * 0.95f;

            // Helical orbit angle
            float debAngle = (time * 0.12f) + (i * ((float) Math.PI * 2.0f / debrisCount));

            float debX = getSpineOffsetX(time, debY) + (float) Math.cos(debAngle) * debRad;
            float debZ = getSpineOffsetZ(time, debY) + (float) Math.sin(debAngle) * debRad;

            BlockState debrisState = entity.getDebrisState(i);

            poseStack.pushPose();
            poseStack.translate(debX, debY, debZ);
            // Chaotic tumble rotations
            poseStack.mulPose(Axis.XP.rotationDegrees(time * 6.0f + i * 45.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 8.0f + i * 30.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(time * 5.0f + i * 60.0f));
            poseStack.scale(0.55f, 0.55f, 0.55f);

            mc.getBlockRenderer().renderSingleBlock(debrisState, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    private static float getSpineOffsetX(float time, float y) {
        if (y <= 0.0f) return 0.0f;
        return (float) (Math.sin(time * 0.04 + y * 0.08) * (y * 0.14) + Math.cos(time * 0.02 + y * 0.05) * (y * 0.1));
    }

    private static float getSpineOffsetZ(float time, float y) {
        if (y <= 0.0f) return 0.0f;
        return (float) (Math.cos(time * 0.035 + y * 0.07) * (y * 0.14) - Math.sin(time * 0.02 + y * 0.04) * (y * 0.1));
    }

    @Override
    public ResourceLocation getTextureLocation(DisasterTornadoEntity entity) {
        return BEAM_NOISE_TEXTURE;
    }
}

