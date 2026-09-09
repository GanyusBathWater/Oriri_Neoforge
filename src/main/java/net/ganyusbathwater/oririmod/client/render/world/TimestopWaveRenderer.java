package net.ganyusbathwater.oririmod.client.render.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class TimestopWaveRenderer {

    private static final ResourceLocation WAVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/effect/timestop_wave.png");
    private static VertexBuffer waveBuffer;

    public static void render(PoseStack poseStack, Matrix4f projectionMatrix, Camera camera) {
        if (!TimestopState.isActive()) return;

        float radius = TimestopState.getExpansionRadius();
        if (radius <= 0.1f) return;

        ensureBufferBuilt();

        Vec3 camPos = camera.getPosition();
        Vec3 origin = TimestopState.getOrigin();

        double x = origin.x - camPos.x;
        double y = origin.y - camPos.y;
        double z = origin.z - camPos.z;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        
        // Scale to the current radius
        poseStack.scale(radius, radius, radius);

        // Spin it slowly
        long time = System.currentTimeMillis();
        float angle = (time % 10000L) / 10000.0f * 360.0f;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // Additive blend for a glowing energy effect
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WAVE_TEXTURE);
        // Bright white color for full texture visibility
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableCull(); // see from inside and outside

        waveBuffer.bind();
        waveBuffer.drawWithShader(matrix, projectionMatrix, GameRenderer.getPositionTexShader());
        VertexBuffer.unbind();

        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void ensureBufferBuilt() {
        if (waveBuffer != null) return;

        waveBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        int rings = 30;
        int sectors = 30;

        for (int r = 0; r < rings; r++) {
            float phi1 = (float) (Math.PI * r / rings);
            float phi2 = (float) (Math.PI * (r + 1) / rings);

            for (int s = 0; s < sectors; s++) {
                float theta1 = (float) (2.0 * Math.PI * s / sectors);
                float theta2 = (float) (2.0 * Math.PI * (s + 1) / sectors);

                float x1 = (float) (Math.sin(phi1) * Math.cos(theta1));
                float y1 = (float) Math.cos(phi1);
                float z1 = (float) (Math.sin(phi1) * Math.sin(theta1));
                float u1 = (float) s / sectors;
                float v1 = (float) r / rings;

                float x2 = (float) (Math.sin(phi2) * Math.cos(theta1));
                float y2 = (float) Math.cos(phi2);
                float z2 = (float) (Math.sin(phi2) * Math.sin(theta1));
                float u2 = (float) s / sectors;
                float v2 = (float) (r + 1) / rings;

                float x3 = (float) (Math.sin(phi2) * Math.cos(theta2));
                float y3 = (float) Math.cos(phi2);
                float z3 = (float) (Math.sin(phi2) * Math.sin(theta2));
                float u3 = (float) (s + 1) / sectors;
                float v3 = (float) (r + 1) / rings;

                float x4 = (float) (Math.sin(phi1) * Math.cos(theta2));
                float y4 = (float) Math.cos(phi1);
                float z4 = (float) (Math.sin(phi1) * Math.sin(theta2));
                float u4 = (float) (s + 1) / sectors;
                float v4 = (float) r / rings;

                // Repeat texture mapping
                float uvScale = 4.0f;

                builder.addVertex(x1, y1, z1).setUv(u1 * uvScale, v1 * uvScale);
                builder.addVertex(x2, y2, z2).setUv(u2 * uvScale, v2 * uvScale);
                builder.addVertex(x3, y3, z3).setUv(u3 * uvScale, v3 * uvScale);
                builder.addVertex(x4, y4, z4).setUv(u4 * uvScale, v4 * uvScale);
            }
        }

        waveBuffer.bind();
        waveBuffer.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
    }
}
