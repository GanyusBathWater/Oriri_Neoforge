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

public class TimestopSkyboxRenderer {

    private static final ResourceLocation TIMESTOP_SKY_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/skybox/timestop_sky_hdri.png");
    private static VertexBuffer skyBuffer;

    public static void render(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick) {
        ensureSkyBuilt();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TIMESTOP_SKY_TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        poseStack.pushPose();
        
        // Slight eerie rotation
        long time = System.currentTimeMillis();
        float angle = (time % 200000L) / 200000.0f * 360.0f;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));

        Matrix4f matrix = poseStack.last().pose();

        skyBuffer.bind();
        skyBuffer.drawWithShader(matrix, projectionMatrix, GameRenderer.getPositionTexShader());
        VertexBuffer.unbind();

        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void ensureSkyBuilt() {
        if (skyBuffer != null) return;

        skyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        // Build massive inverted UV sphere (radius 100)
        float radius = 100.0f;
        int rings = 60;
        int sectors = 60;

        for (int r = 0; r < rings; r++) {
            float phi1 = (float) (Math.PI * r / rings);
            float phi2 = (float) (Math.PI * (r + 1) / rings);

            for (int s = 0; s < sectors; s++) {
                float theta1 = (float) (2.0 * Math.PI * s / sectors);
                float theta2 = (float) (2.0 * Math.PI * (s + 1) / sectors);

                // Multiply by radius. NOTE: inside-out mapping means drawing the vertices in reverse order
                float x1 = (float) (radius * Math.sin(phi1) * Math.cos(theta1));
                float y1 = (float) (radius * Math.cos(phi1));
                float z1 = (float) (radius * Math.sin(phi1) * Math.sin(theta1));
                float u1 = (float) s / sectors;
                float v1 = (float) r / rings;

                float x2 = (float) (radius * Math.sin(phi2) * Math.cos(theta1));
                float y2 = (float) (radius * Math.cos(phi2));
                float z2 = (float) (radius * Math.sin(phi2) * Math.sin(theta1));
                float u2 = (float) s / sectors;
                float v2 = (float) (r + 1) / rings;

                float x3 = (float) (radius * Math.sin(phi2) * Math.cos(theta2));
                float y3 = (float) (radius * Math.cos(phi2));
                float z3 = (float) (radius * Math.sin(phi2) * Math.sin(theta2));
                float u3 = (float) (s + 1) / sectors;
                float v3 = (float) (r + 1) / rings;

                float x4 = (float) (radius * Math.sin(phi1) * Math.cos(theta2));
                float y4 = (float) (radius * Math.cos(phi1));
                float z4 = (float) (radius * Math.sin(phi1) * Math.sin(theta2));
                float u4 = (float) (s + 1) / sectors;
                float v4 = (float) r / rings;

                // Invert winding order for inside-out viewing
                builder.addVertex(x1, y1, z1).setUv(u1, v1);
                builder.addVertex(x4, y4, z4).setUv(u4, v4);
                builder.addVertex(x3, y3, z3).setUv(u3, v3);
                builder.addVertex(x2, y2, z2).setUv(u2, v2);
            }
        }

        skyBuffer.bind();
        skyBuffer.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
    }
}
