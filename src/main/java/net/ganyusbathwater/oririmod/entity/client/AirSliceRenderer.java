package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.entity.AirSliceEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AirSliceRenderer extends GeoEntityRenderer<AirSliceEntity> {

    public AirSliceRenderer(EntityRendererProvider.Context context) {
        super(context, new AirSliceModel());
    }

    @Override
    public RenderType getRenderType(AirSliceEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    @Override
    public void render(AirSliceEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    protected int getBlockLightLevel(AirSliceEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    protected void applyRotations(AirSliceEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        float yaw = Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot());
        float pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
        
        // Align with direction of travel:
        // Translate to center (Y = 1.0 block) so yaw, pitch, and roll rotate around the projectile's geometric center
        poseStack.translate(0.0D, 1.0D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        
        // Apply custom roll around the Z-axis (forward flight axis)
        poseStack.mulPose(Axis.ZP.rotationDegrees(animatable.getRollAngle()));
        poseStack.translate(0.0D, -1.0D, 0.0D);
    }

    @Override
    public software.bernie.geckolib.util.Color getRenderColor(AirSliceEntity animatable, float partialTick, int packedLight) {
        return software.bernie.geckolib.util.Color.ofOpaque(animatable.getColor());
    }
}

