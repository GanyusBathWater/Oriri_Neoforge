package net.ganyusbathwater.oririmod.client.render;

import net.ganyusbathwater.oririmod.client.model.GiantSwordModel;
import net.ganyusbathwater.oririmod.entity.custom.GiantSwordEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class GiantSwordRenderer extends GeoEntityRenderer<GiantSwordEntity> {
    
    private static final ResourceLocation MAGIC_CIRCLE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/effect/magic_circles/light_ground.png");

    public GiantSwordRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GiantSwordModel());
    }

    @Override
    public void render(GiantSwordEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(1.5f, 1.5f, 1.5f);
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        
        if (!entity.isImpacted() && entity.getDeltaMovement().y > -0.1) {
            poseStack.pushPose();
            
            // Move up above the sword (sword is quite tall)
            poseStack.translate(0, 4.5, 0);
            
            // Rotate the circle over time
            float time = entity.tickCount + partialTick;
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(time * 5f));
            
            VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(MAGIC_CIRCLE));
            PoseStack.Pose last = poseStack.last();
            var poseMat = last.pose();
            int light = LightTexture.FULL_BRIGHT;
            int overlay = OverlayTexture.NO_OVERLAY;
            
            float size = 4.0f;
            
            vc.addVertex(poseMat, -size, 0, -size).setColor(255, 255, 255, 255).setUv(0f, 0f).setOverlay(overlay).setLight(light).setNormal(last, 0, 1, 0);
            vc.addVertex(poseMat, -size, 0, size).setColor(255, 255, 255, 255).setUv(0f, 1f).setOverlay(overlay).setLight(light).setNormal(last, 0, 1, 0);
            vc.addVertex(poseMat, size, 0, size).setColor(255, 255, 255, 255).setUv(1f, 1f).setOverlay(overlay).setLight(light).setNormal(last, 0, 1, 0);
            vc.addVertex(poseMat, size, 0, -size).setColor(255, 255, 255, 255).setUv(1f, 0f).setOverlay(overlay).setLight(light).setNormal(last, 0, 1, 0);

            poseStack.popPose();
        }
        
        poseStack.popPose();
    }
}
