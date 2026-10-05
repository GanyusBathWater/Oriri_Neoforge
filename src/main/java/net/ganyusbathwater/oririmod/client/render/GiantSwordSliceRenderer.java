package net.ganyusbathwater.oririmod.client.render;

import net.ganyusbathwater.oririmod.client.model.GiantSwordSliceModel;
import net.ganyusbathwater.oririmod.entity.custom.GiantSwordSliceEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public class GiantSwordSliceRenderer extends GeoEntityRenderer<GiantSwordSliceEntity> {

    public GiantSwordSliceRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GiantSwordSliceModel());
    }

    @Override
    public void render(GiantSwordSliceEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!entity.isActive()) {
            return; // Invisible until active
        }
        
        if (!entity.isSlicing()) {
            poseStack.pushPose();
            // Ground level, slightly up to avoid Z-fighting
            poseStack.translate(0, 0.05, 0);
            
            // Rotate flat to the ground (X axis 90 degrees)
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90f));
            
            // Apply a slight slow spin for cool magic effect
            float time = entity.tickCount + partialTick;
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(time * 5f));
            
            // Scale the circle (3x3 blocks wide)
            float circleSize = 3.0f;
            poseStack.scale(circleSize, circleSize, circleSize);
            
            net.minecraft.resources.ResourceLocation MAGIC_CIRCLE = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("oririmod", "textures/effect/magic_circles/light_ground.png");
            com.mojang.blaze3d.vertex.VertexConsumer vc = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucentEmissive(MAGIC_CIRCLE));
            
            PoseStack.Pose last = poseStack.last();
            org.joml.Matrix4f poseMat = last.pose();
            
            int light = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
            int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
            
            vc.addVertex(poseMat, -0.5F, -0.5F, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(last, 0.0F, 0.0F, -1.0F);
            vc.addVertex(poseMat, -0.5F,  0.5F, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(last, 0.0F, 0.0F, -1.0F);
            vc.addVertex(poseMat,  0.5F,  0.5F, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(last, 0.0F, 0.0F, -1.0F);
            vc.addVertex(poseMat,  0.5F, -0.5F, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(last, 0.0F, 0.0F, -1.0F);
            
            poseStack.popPose();
        }
        
        poseStack.pushPose();
        
        // Scale up if needed. We assume the new model is natively modeled facing forward (North) and upward.
        poseStack.scale(1.5f, 1.5f, 1.5f);

        // Geckolib's renderer doesn't automatically rotate standard Entity subclasses to face their yaw.
        // Since the model natively points North (180 degrees yRot in Minecraft), we apply this offset.
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f - entityYaw));

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        
        poseStack.popPose();
    }
}
