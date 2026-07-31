package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.entity.AirSliceEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AirSliceRenderer extends GeoEntityRenderer<AirSliceEntity> {

    public AirSliceRenderer(EntityRendererProvider.Context context) {
        super(context, new AirSliceModel());
    }

    @Override
    public void preRender(PoseStack poseStack, AirSliceEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
        
        if (!isReRender) {
            float yaw = Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot());
            float pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
            
            // Align with direction of travel
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            
            // Apply custom roll around the Z-axis (forward axis)
            poseStack.mulPose(Axis.ZP.rotationDegrees(animatable.getRollAngle()));
        }
    }

    @Override
    public software.bernie.geckolib.util.Color getRenderColor(AirSliceEntity animatable, float partialTick, int packedLight) {
        return software.bernie.geckolib.util.Color.ofOpaque(animatable.getColor());
    }
}
