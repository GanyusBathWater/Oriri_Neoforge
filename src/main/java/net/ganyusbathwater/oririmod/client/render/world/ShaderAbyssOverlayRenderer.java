package net.ganyusbathwater.oririmod.client.render.world;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.ganyusbathwater.oririmod.util.ShaderDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Renders a light-reactive abyssal darkness veil at {@code AFTER_LEVEL} when Iris/OptiFine shaders are active.
 * Deferred/PBR shaderpacks ignore vanilla underwater fog planes and lightmap textures, so applying an
 * HDR-contrast multiplicative pass plus soft peripheral alpha shroud before {@code renderItemInHand} and GUI
 * keeps torches/lanterns luminous while plunging unlit Silent Depths into black without washing out HUD or hand.
 */
final class ShaderAbyssOverlayRenderer {

    private static final int RADIAL_SECTORS = 20;
    private static final float INNER_RADIUS = 0.48F;
    private static final float OUTER_RADIUS = 1.50F;

    private ShaderAbyssOverlayRenderer() {
    }

    static void render(
            float submergedDarkness,
            float surfaceDarkness,
            float exteriorGaze,
            float lightRelief) {

        if (!ShaderDetector.isShaderActive()) {
            return;
        }

        float underwaterDrive = Math.max(submergedDarkness, exteriorGaze * 0.65F);
        float totalDrive = Math.max(underwaterDrive, surfaceDarkness * 0.55F);
        if (totalDrive <= 0.01F) {
            return;
        }

        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(
                new Matrix4f().setOrtho(-1.0F, 1.0F, -1.0F, 1.0F, -1.0F, 1.0F),
                VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        try {
            // Pass 1: Multiplicative darkening keeps silhouettes faintly visible while holding back light relief.
            float centerAbyssMul = Mth.lerp(lightRelief, 0.095F, 0.34F);
            float centerMul = Mth.lerp(underwaterDrive, 1.0F, centerAbyssMul);
            if (surfaceDarkness > underwaterDrive) {
                float surfaceMul = Mth.lerp(surfaceDarkness * (1.0F - 0.30F * lightRelief), 1.0F, 0.58F);
                centerMul = Math.min(centerMul, surfaceMul);
            }
            float midMul = Mth.lerp(0.45F, centerMul, Mth.lerp(totalDrive, 1.0F, Mth.lerp(lightRelief, 0.042F, 0.16F)));
            float edgeMul = Mth.lerp(totalDrive, 1.0F, Mth.lerp(lightRelief, 0.018F, 0.065F));

            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO,
                    GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            drawRadialOverlay(
                    centerMul * 0.88F, centerMul * 0.96F, centerMul, 1.0F,
                    midMul * 0.86F, midMul * 0.95F, midMul, 1.0F,
                    edgeMul * 0.82F, edgeMul * 0.92F, edgeMul, 1.0F);

            // Pass 2: Deep midnight-navy alpha clamp extinguishes unlit flat shader water scatter.
            float centerAlpha = Math.max(
                    underwaterDrive * Mth.lerp(lightRelief, 0.52F, 0.28F),
                    surfaceDarkness * Mth.lerp(lightRelief, 0.18F, 0.08F));
            float midAlpha = Math.max(
                    underwaterDrive * Mth.lerp(lightRelief, 0.68F, 0.44F),
                    surfaceDarkness * Mth.lerp(lightRelief, 0.32F, 0.18F));
            float edgeAlpha = Math.max(
                    underwaterDrive * Mth.lerp(lightRelief, 0.80F, 0.62F),
                    surfaceDarkness * Mth.lerp(lightRelief, 0.52F, 0.38F));

            if (edgeAlpha > 0.005F) {
                RenderSystem.defaultBlendFunc();
                drawRadialOverlay(
                        0.003F, 0.006F, 0.012F, centerAlpha,
                        0.002F, 0.005F, 0.010F, midAlpha,
                        0.001F, 0.003F, 0.007F, edgeAlpha);
            }
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    private static void drawRadialOverlay(
            float cR, float cG, float cB, float cA,
            float mR, float mG, float mB, float mA,
            float eR, float eG, float eB, float eA) {

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < RADIAL_SECTORS; i++) {
            double a0 = (2.0 * Math.PI * i) / RADIAL_SECTORS;
            double a1 = (2.0 * Math.PI * (i + 1)) / RADIAL_SECTORS;

            float cos0 = (float) Math.cos(a0);
            float sin0 = (float) Math.sin(a0);
            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);

            float ix0 = cos0 * INNER_RADIUS;
            float iy0 = sin0 * INNER_RADIUS;
            float ix1 = cos1 * INNER_RADIUS;
            float iy1 = sin1 * INNER_RADIUS;

            float ox0 = cos0 * OUTER_RADIUS;
            float oy0 = sin0 * OUTER_RADIUS;
            float ox1 = cos1 * OUTER_RADIUS;
            float oy1 = sin1 * OUTER_RADIUS;

            // Inner focus cone triangle
            buffer.addVertex(0.0F, 0.0F, 0.0F).setColor(cR, cG, cB, cA);
            buffer.addVertex(ix0, iy0, 0.0F).setColor(mR, mG, mB, mA);
            buffer.addVertex(ix1, iy1, 0.0F).setColor(mR, mG, mB, mA);

            // Outer periphery band split into two triangles
            buffer.addVertex(ix0, iy0, 0.0F).setColor(mR, mG, mB, mA);
            buffer.addVertex(ox0, oy0, 0.0F).setColor(eR, eG, eB, eA);
            buffer.addVertex(ox1, oy1, 0.0F).setColor(eR, eG, eB, eA);

            buffer.addVertex(ix0, iy0, 0.0F).setColor(mR, mG, mB, mA);
            buffer.addVertex(ox1, oy1, 0.0F).setColor(eR, eG, eB, eA);
            buffer.addVertex(ix1, iy1, 0.0F).setColor(mR, mG, mB, mA);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}
