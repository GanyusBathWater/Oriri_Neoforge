package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class FlatItemArrowRenderer<T extends AbstractArrow> extends EntityRenderer<T> {
    private final ResourceLocation textureLocation;

    public FlatItemArrowRenderer(EntityRendererProvider.Context context, ResourceLocation textureLocation) {
        super(context);
        this.textureLocation = textureLocation;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        // 1. Align the renderer with the direction of flight (+X is forward)
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        
        // Rotate 45 degrees around the flight axis so the arrow forms an 'X' shape instead of a '+' shape
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));

        // 2. Scale the arrow to appropriate size (1.5x larger than a standard 1 block size to match vanilla arrows visually)
        float scale = 1.0F;
        poseStack.scale(scale, scale, scale);

        VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.entityCutout(this.getTextureLocation(entity)));

        // Plane 1 (Standard vertical cross-section)
        poseStack.pushPose();
        // Rotate -45 degrees on Z so the top-right corner of the texture (the tip) points perfectly forward (+X)
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        drawQuad(vertexconsumer, poseStack.last(), packedLight);
        poseStack.popPose();

        // Plane 2 (Horizontal cross-section)
        poseStack.pushPose();
        // Rotate 90 degrees around the flight axis (+X) to form the cross shape
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        // Rotate -45 degrees on Z again
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        drawQuad(vertexconsumer, poseStack.last(), packedLight);
        poseStack.popPose();

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void drawQuad(VertexConsumer consumer, PoseStack.Pose pose, int packedLight) {
        // Front side
        vertex(consumer, pose, packedLight, -0.5F,  0.5F, 0.0F, 0.0F, 0.0F); // Top-Left
        vertex(consumer, pose, packedLight, -0.5F, -0.5F, 0.0F, 0.0F, 1.0F); // Bottom-Left
        vertex(consumer, pose, packedLight,  0.5F, -0.5F, 0.0F, 1.0F, 1.0F); // Bottom-Right
        vertex(consumer, pose, packedLight,  0.5F,  0.5F, 0.0F, 1.0F, 0.0F); // Top-Right
        
        // Back side (reverse winding order)
        vertex(consumer, pose, packedLight, -0.5F,  0.5F, 0.0F, 0.0F, 0.0F); // Top-Left
        vertex(consumer, pose, packedLight,  0.5F,  0.5F, 0.0F, 1.0F, 0.0F); // Top-Right
        vertex(consumer, pose, packedLight,  0.5F, -0.5F, 0.0F, 1.0F, 1.0F); // Bottom-Right
        vertex(consumer, pose, packedLight, -0.5F, -0.5F, 0.0F, 0.0F, 1.0F); // Bottom-Left
    }

    private void vertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight, float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.textureLocation;
    }
}
