package net.ganyusbathwater.oririmod.client.render.entity;

import net.ganyusbathwater.oririmod.entity.custom.cosmetic.AuroraCosmeticAnimatable;
import net.ganyusbathwater.oririmod.client.render.entity.model.AuroraCosmeticModel;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class AuroraCosmeticRenderer extends GeoReplacedEntityRenderer<AbstractClientPlayer, AuroraCosmeticAnimatable> {

    public AuroraCosmeticRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AuroraCosmeticModel(), AuroraCosmeticAnimatable.INSTANCE);
        
        addRenderLayer(new software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer<>(this) {
            @Override
            public void renderForBone(com.mojang.blaze3d.vertex.PoseStack poseStack, AuroraCosmeticAnimatable animatable, software.bernie.geckolib.cache.object.GeoBone bone, net.minecraft.client.renderer.RenderType renderType, net.minecraft.client.renderer.MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (((AuroraCosmeticRenderer) this.getRenderer()).getCurrentEntity() instanceof net.minecraft.world.entity.player.Player player) {
                    boolean isRight = bone.getName().equals("right_item") || bone.getName().equals("rightItem") || bone.getName().equals("bipedRightItem");
                    boolean isLeft = bone.getName().equals("left_item") || bone.getName().equals("leftItem") || bone.getName().equals("bipedLeftItem");
                    
                    if (!isRight && !isLeft) return;
                    
                    net.minecraft.world.item.ItemStack stack = isRight ? 
                        (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? player.getMainHandItem() : player.getOffhandItem()) :
                        (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? player.getMainHandItem() : player.getOffhandItem());
                        
                    if (stack.isEmpty()) return;
                    
                    poseStack.pushPose();
                    
                    // FIXED: GeckoLib's default `translateAndRotateMatrixForBone` actually applies the bone's rotation TWICE 
                    // because `renderRecursively` already applied it. This is why locators spun wildly for you in the past!
                    // We only need to translate back to the pivot point.
                    software.bernie.geckolib.util.RenderUtil.translateToPivotPoint(poseStack, bone);
                    
                    // Convert GeckoLib's Y-up space to Vanilla's Y-down space. 
                    // We must scale TWO axes by -1 to prevent turning the mesh inside-out (which breaks textures).
                    poseStack.scale(-1.0F, -1.0F, 1.0F);
                    
                    // Apply standard Vanilla item rotation for the hand
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
                    
                    // Center the item in the palm (Vanilla does this, but we skip Z so we stay at the locator pivot)
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
    protected void applyRotations(AuroraCosmeticAnimatable animatable, com.mojang.blaze3d.vertex.PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);

        if (this.currentEntity instanceof net.minecraft.world.entity.player.Player player) {
            float swimAmount = player.getSwimAmount(partialTick);
            float viewXRot = player.getViewXRot(partialTick);

            if (player.isFallFlying()) {
                float fallFlyingTicks = (float)player.getFallFlyingTicks() + partialTick;
                float clampedFall = net.minecraft.util.Mth.clamp(fallFlyingTicks * fallFlyingTicks / 100.0F, 0.0F, 1.0F);
                if (!player.isAutoSpinAttack()) {
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(clampedFall * (-90.0F - viewXRot)));
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
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(lerpedPitch));
                if (player.isVisuallySwimming()) {
                    poseStack.translate(0.0F, -1.0F, 0.3F);
                }
            }
        }
    }
}
