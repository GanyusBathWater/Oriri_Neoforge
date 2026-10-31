package net.ganyusbathwater.oririmod.client.render.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Procedural tempest sky layer for the Natural Disaster Tornado event.
 *
 * <p>Renders a turbulent, multi-layered rotating supercell sky dome across the entire sky,
 * completely blotting out the heavens during the tornado disaster independent of individual tornado entities.
 */
public class TornadoSkyboxRenderer {

    private static final ResourceLocation SMOKE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/explosion_smoke.png");
    private static final ResourceLocation NOISE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/effect/beam_noise.png");

    private static VertexBuffer skyDomeTexBuffer;
    private static VertexBuffer skyDomeColorBuffer;

    public static void render(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, float alphaProgress) {
        if (alphaProgress <= 0.01f) {
            return;
        }

        ensureBuffersBuilt();

        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        long timeMs = System.currentTimeMillis();
        float angle1 = (timeMs % 140000L) / 140000.0f * 360.0f;
        float angle2 = -(timeMs % 90000L) / 90000.0f * 360.0f;

        // ── Pass 1: Opaque/Dense Overcast Tempest Dome (Completely blocks blue sky / sun / moon) ──
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        poseStack.pushPose();
        Matrix4f baseMatrix = poseStack.last().pose();
        skyDomeColorBuffer.bind();
        skyDomeColorBuffer.drawWithShader(baseMatrix, projectionMatrix, GameRenderer.getPositionColorShader());
        VertexBuffer.unbind();
        poseStack.popPose();

        // ── Pass 2: Rotating Upper Supercell Cloud Noise ──
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, NOISE_TEXTURE);
        float a1 = Math.min(1.0f, alphaProgress * 0.90f);
        RenderSystem.setShaderColor(0.35f, 0.32f, 0.25f, a1);

        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle1));
        Matrix4f matrix1 = poseStack.last().pose();

        skyDomeTexBuffer.bind();
        skyDomeTexBuffer.drawWithShader(matrix1, projectionMatrix, GameRenderer.getPositionTexShader());
        VertexBuffer.unbind();
        poseStack.popPose();

        // ── Pass 3: Counter-Rotating Lower Churning Tempest Smoke ──
        RenderSystem.setShaderTexture(0, SMOKE_TEXTURE);
        float a2 = Math.min(1.0f, alphaProgress * 0.75f);
        RenderSystem.setShaderColor(0.20f, 0.18f, 0.14f, a2);

        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle2));
        poseStack.scale(0.96f, 0.96f, 0.96f);
        Matrix4f matrix2 = poseStack.last().pose();

        skyDomeTexBuffer.bind();
        skyDomeTexBuffer.drawWithShader(matrix2, projectionMatrix, GameRenderer.getPositionTexShader());
        VertexBuffer.unbind();
        poseStack.popPose();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void ensureBuffersBuilt() {
        if (skyDomeTexBuffer != null && skyDomeColorBuffer != null) return;

        float radius = 100.0f;
        int rings = 36;
        int sectors = 36;

        // 1. Textured UV Dome Buffer
        skyDomeTexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder texBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        for (int r = 0; r < rings; r++) {
            float phi1 = (float) (Math.PI * r / rings);
            float phi2 = (float) (Math.PI * (r + 1) / rings);

            for (int s = 0; s < sectors; s++) {
                float theta1 = (float) (2.0 * Math.PI * s / sectors);
                float theta2 = (float) (2.0 * Math.PI * (s + 1) / sectors);

                float u1 = (float) s / sectors * 4.0f;
                float u2 = (float) (s + 1) / sectors * 4.0f;
                float v1 = (float) r / rings * 2.0f;
                float v2 = (float) (r + 1) / rings * 2.0f;

                float x1 = (float) (radius * Math.sin(phi1) * Math.cos(theta1));
                float y1 = (float) (radius * Math.cos(phi1));
                float z1 = (float) (radius * Math.sin(phi1) * Math.sin(theta1));

                float x2 = (float) (radius * Math.sin(phi1) * Math.cos(theta2));
                float y2 = (float) (radius * Math.cos(phi1));
                float z2 = (float) (radius * Math.sin(phi1) * Math.sin(theta2));

                float x3 = (float) (radius * Math.sin(phi2) * Math.cos(theta2));
                float y3 = (float) (radius * Math.cos(phi2));
                float z3 = (float) (radius * Math.sin(phi2) * Math.sin(theta2));

                float x4 = (float) (radius * Math.sin(phi2) * Math.cos(theta1));
                float y4 = (float) (radius * Math.cos(phi2));
                float z4 = (float) (radius * Math.sin(phi2) * Math.sin(theta1));

                // Inverted quad winding
                texBuilder.addVertex(x1, y1, z1).setUv(u1, v1);
                texBuilder.addVertex(x4, y4, z4).setUv(u1, v2);
                texBuilder.addVertex(x3, y3, z3).setUv(u2, v2);
                texBuilder.addVertex(x2, y2, z2).setUv(u2, v1);
            }
        }
        skyDomeTexBuffer.bind();
        skyDomeTexBuffer.upload(texBuilder.buildOrThrow());
        VertexBuffer.unbind();

        // 2. Solid Color Dome Buffer (Dense dark tempest overcast)
        skyDomeColorBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        BufferBuilder colorBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int red = 36;
        int green = 32;
        int blue = 28;
        int alpha = 245;

        for (int r = 0; r < rings; r++) {
            float phi1 = (float) (Math.PI * r / rings);
            float phi2 = (float) (Math.PI * (r + 1) / rings);

            for (int s = 0; s < sectors; s++) {
                float theta1 = (float) (2.0 * Math.PI * s / sectors);
                float theta2 = (float) (2.0 * Math.PI * (s + 1) / sectors);

                float x1 = (float) (radius * Math.sin(phi1) * Math.cos(theta1));
                float y1 = (float) (radius * Math.cos(phi1));
                float z1 = (float) (radius * Math.sin(phi1) * Math.sin(theta1));

                float x2 = (float) (radius * Math.sin(phi1) * Math.cos(theta2));
                float y2 = (float) (radius * Math.cos(phi1));
                float z2 = (float) (radius * Math.sin(phi1) * Math.sin(theta2));

                float x3 = (float) (radius * Math.sin(phi2) * Math.cos(theta2));
                float y3 = (float) (radius * Math.cos(phi2));
                float z3 = (float) (radius * Math.sin(phi2) * Math.sin(theta2));

                float x4 = (float) (radius * Math.sin(phi2) * Math.cos(theta1));
                float y4 = (float) (radius * Math.cos(phi2));
                float z4 = (float) (radius * Math.sin(phi2) * Math.sin(theta1));

                // Inverted quad winding
                colorBuilder.addVertex(x1, y1, z1).setColor(red, green, blue, alpha);
                colorBuilder.addVertex(x4, y4, z4).setColor(red, green, blue, alpha);
                colorBuilder.addVertex(x3, y3, z3).setColor(red, green, blue, alpha);
                colorBuilder.addVertex(x2, y2, z2).setColor(red, green, blue, alpha);
            }
        }
        skyDomeColorBuffer.bind();
        skyDomeColorBuffer.upload(colorBuilder.buildOrThrow());
        VertexBuffer.unbind();
    }
}
