package net.ganyusbathwater.oririmod.client.render.entity.template;

import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

public abstract class AbstractPlayerCosmeticRenderer<T extends AbstractPlayerCosmeticAnimatable> extends GeoReplacedEntityRenderer<AbstractClientPlayer, T> {

    public AbstractPlayerCosmeticRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model, T animatable) {
        super(renderManager, model, animatable);
        
        addRenderLayer(new software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer<>(this) {
            @Override
            public void renderForBone(com.mojang.blaze3d.vertex.PoseStack poseStack, T animatable, software.bernie.geckolib.cache.object.GeoBone bone, net.minecraft.client.renderer.RenderType renderType, net.minecraft.client.renderer.MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (((AbstractPlayerCosmeticRenderer<?>) this.getRenderer()).getCurrentEntity() instanceof net.minecraft.world.entity.player.Player player) {
                    boolean isRight = bone.getName().equals("right_item") || bone.getName().equals("rightItem") || bone.getName().equals("bipedRightItem");
                    boolean isLeft = bone.getName().equals("left_item") || bone.getName().equals("leftItem") || bone.getName().equals("bipedLeftItem");
                    
                    if (!isRight && !isLeft) return;
                    
                    net.minecraft.world.item.ItemStack stack = isRight ? 
                        (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? player.getMainHandItem() : player.getOffhandItem()) :
                        (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? player.getMainHandItem() : player.getOffhandItem());
                        
                    if (stack.isEmpty()) return;
                    
                    poseStack.pushPose();
                    
                    software.bernie.geckolib.util.RenderUtil.translateToPivotPoint(poseStack, bone);
                    poseStack.scale(-1.0F, -1.0F, 1.0F);
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
                    poseStack.translate((isLeft ? -1 : 1) / 16.0F, 0.125F, 0.0F);
                    
                    net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(
                        player,
                        stack,
                        isLeft ? net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND : net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        isLeft,
                        poseStack,
                        bufferSource,
                        player.level(),
                        packedLight,
                        packedOverlay,
                        player.getId()
                    );
                    
                    poseStack.popPose();
                }
            }
        });
    }

    @Override
    protected void applyRotations(T animatable, com.mojang.blaze3d.vertex.PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);

        if (this.currentEntity instanceof net.minecraft.world.entity.player.Player player) {
            float swimAmount = player.getSwimAmount(partialTick);
            float viewXRot = player.getViewXRot(partialTick);

            if (player.isFallFlying()) {
                float fallFlyingTicks = (float)player.getFallFlyingTicks() + partialTick;
                float clampedFall = net.minecraft.util.Mth.clamp(fallFlyingTicks * fallFlyingTicks / 100.0F, 0.0F, 1.0F);
                if (!player.isAutoSpinAttack()) {
                    poseStack.translate(0.0F, 1.2F, 0.0F);
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(clampedFall * (-90.0F - viewXRot)));
                    poseStack.translate(0.0F, -1.2F, 0.0F);
                }

                net.minecraft.world.phys.Vec3 viewVector = player.getViewVector(partialTick);
                net.minecraft.world.phys.Vec3 movementVector = player.getDeltaMovement();
                double horizontalSpeed = movementVector.horizontalDistanceSqr();
                double viewDistance = viewVector.horizontalDistanceSqr();
                if (horizontalSpeed > 0.0 && viewDistance > 0.0) {
                    double dotProduct = (movementVector.x * viewVector.x + movementVector.z * viewVector.z) / Math.sqrt(horizontalSpeed * viewDistance);
                    double crossProduct = movementVector.x * viewVector.z - movementVector.z * viewVector.x;
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotation((float)(Math.signum(crossProduct) * Math.acos(dotProduct))));
                }
            } else if (swimAmount > 0.0F) {
                float targetPitch = player.isInWater() || player.isInFluidType((fluidType, height) -> player.canSwimInFluidType(fluidType)) ? -90.0F - player.getXRot() : -90.0F;
                float lerpedPitch = net.minecraft.util.Mth.lerp(swimAmount, 0.0F, targetPitch);
                poseStack.translate(0.0F, 1.2F, 0.0F);
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(lerpedPitch));
                poseStack.translate(0.0F, -1.2F, 0.0F);
                
                // Local Space Translation: Because this is applied AFTER the pitch in code, 
                // it translates the model relative to the player's spine (Local Y-axis).
                // Negative Y slides the tail backward down the spine. Positive Y slides it forward.
                // This perfectly tracks the player's body regardless of looking up or down!
                poseStack.translate(0.0F, -1.0F * swimAmount, 0.0F);
            }
        }
    }
}
