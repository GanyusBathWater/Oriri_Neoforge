package net.ganyusbathwater.oririmod.client.render.entity.template;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

public abstract class AbstractPlayerCosmeticRenderer<T extends AbstractPlayerCosmeticAnimatable>
        extends GeoReplacedEntityRenderer<AbstractClientPlayer, T> {

    public final boolean isRenderLayer;

    public AbstractPlayerCosmeticRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model, T animatable, boolean isRenderLayer) {
        super(renderManager, model, animatable);
        this.isRenderLayer = isRenderLayer;
        
        addRenderLayer(new software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer<>(this) {
            @Override
            public void renderForBone(com.mojang.blaze3d.vertex.PoseStack poseStack, T animatable, software.bernie.geckolib.cache.object.GeoBone bone, net.minecraft.client.renderer.RenderType renderType, net.minecraft.client.renderer.MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                // If this cosmetic is rendered as a layer on the native player model,
                // the native ItemInHandLayer will already render the items. We skip it here to prevent double rendering.
                if (isRenderLayer) return;

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
    public boolean shouldShowName(AbstractClientPlayer entity) {
        return false;
    }

    @Override
    public void render(AbstractClientPlayer entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    public void actuallyRender(PoseStack poseStack, T animatable, BakedGeoModel model, @Nullable RenderType renderType,
                               MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender,
                               float partialTick, int packedLight, int packedOverlay, int colour) {
        poseStack.pushPose();

        LivingEntity livingEntity = this.currentEntity instanceof LivingEntity entity ? entity : null;

        boolean shouldSit = this.currentEntity.isPassenger() && (this.currentEntity.getVehicle() != null);
        float lerpBodyRot = livingEntity == null ? 0 : Mth.rotLerp(partialTick, livingEntity.yBodyRotO, livingEntity.yBodyRot);
        float lerpHeadRot = livingEntity == null ? 0 : Mth.rotLerp(partialTick, livingEntity.yHeadRotO, livingEntity.yHeadRot);
        float netHeadYaw = lerpHeadRot - lerpBodyRot;

        if (shouldSit && this.currentEntity.getVehicle() instanceof LivingEntity livingentity) {
            lerpBodyRot = Mth.rotLerp(partialTick, livingentity.yBodyRotO, livingentity.yBodyRot);
            netHeadYaw = lerpHeadRot - lerpBodyRot;
            float clampedHeadYaw = Mth.clamp(Mth.wrapDegrees(netHeadYaw), -85, 85);
            lerpBodyRot = lerpHeadRot - clampedHeadYaw;

            if (clampedHeadYaw * clampedHeadYaw > 2500f)
                lerpBodyRot += clampedHeadYaw * 0.2f;

            netHeadYaw = lerpHeadRot - lerpBodyRot;
        }

        // Only apply GeckoLib's sleeping translation if this is NOT a render layer.
        // For render layers, LivingEntityRenderer already applied the bed translation!
        if (!this.isRenderLayer && this.currentEntity.getPose() == Pose.SLEEPING && livingEntity != null) {
            Direction bedDirection = livingEntity.getBedOrientation();

            if (bedDirection != null) {
                float eyePosOffset = livingEntity.getEyeHeight(Pose.STANDING) - 0.1F;

                poseStack.translate(-bedDirection.getStepX() * eyePosOffset, 0, -bedDirection.getStepZ() * eyePosOffset);
            }
        }

        float nativeScale = livingEntity != null ? livingEntity.getScale() : 1;
        float ageInTicks = this.currentEntity.tickCount + partialTick;
        float limbSwingAmount = 0;
        float limbSwing = 0;

        // CRITICAL FIX: Only scale if NOT a RenderLayer!
        // When rendered as a RenderLayer, LivingEntityRenderer has ALREADY scaled the poseStack by entity.getScale().
        // Scaling again here would cause S^2 double-scaling (e.g. 2x scale becoming 4x, 0.5x becoming 0.25x).
        if (!this.isRenderLayer) {
            poseStack.scale(nativeScale, nativeScale, nativeScale);
        }
        applyRotations(animatable, poseStack, ageInTicks, lerpBodyRot, partialTick, nativeScale);

        if (!shouldSit && this.currentEntity.isAlive() && livingEntity != null) {
            limbSwingAmount = livingEntity.walkAnimation.speed(partialTick);
            limbSwing = livingEntity.walkAnimation.position(partialTick);

            if (livingEntity.isBaby())
                limbSwing *= 3f;

            if (limbSwingAmount > 1f)
                limbSwingAmount = 1f;
        }

        float headPitch = Mth.lerp(partialTick, this.currentEntity.xRotO, this.currentEntity.getXRot());
        float motionThreshold = getMotionAnimThreshold(animatable);
        boolean isMoving;

        if (livingEntity != null) {
            Vec3 velocity = livingEntity.getDeltaMovement();
            float avgVelocity = (float)(Math.abs(velocity.x) + Math.abs(velocity.z)) / 2f;

            isMoving = avgVelocity >= motionThreshold && limbSwingAmount != 0;
        }
        else {
            isMoving = (limbSwingAmount <= -motionThreshold || limbSwingAmount >= motionThreshold);
        }

        if (!isReRender) {
            AnimationState<T> animationState = new AnimationState<>(animatable, limbSwing, limbSwingAmount, partialTick, isMoving);
            long instanceId = getInstanceId(animatable);
            GeoModel<T> currentModel = getGeoModel();

            animationState.setData(DataTickets.TICK, animatable.getTick(this.currentEntity));
            animationState.setData(DataTickets.ENTITY, this.currentEntity);
            animationState.setData(DataTickets.ENTITY_MODEL_DATA, new EntityModelData(shouldSit, livingEntity != null && livingEntity.isBaby(), -netHeadYaw, -headPitch));
            currentModel.addAdditionalStateData(animatable, instanceId, animationState::setData);
            currentModel.handleAnimations(animatable, instanceId, animationState, partialTick);
        }

        poseStack.translate(0, 0.01f, 0);

        this.modelRenderTranslations = new Matrix4f(poseStack.last().pose());

        if (buffer != null) {
            updateAnimatedTextureFrame(animatable);

            for (software.bernie.geckolib.cache.object.GeoBone group : model.topLevelBones()) {
                renderRecursively(poseStack, animatable, group, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
            }
        }

        poseStack.popPose();
    }

    @Override
    protected void applyRotations(T animatable, com.mojang.blaze3d.vertex.PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        if (!isRenderLayer) {
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
                    
                    // Pivot rotation around the player's waist (Y = 0.75m) and position the crawling body flat inside the 0.6m tunnel
                    poseStack.translate(0.0F, 0.25F * swimAmount, 0.0F);
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(lerpedPitch));
                    poseStack.translate(0.0F, -0.75F * swimAmount, 0.0F);
                }
            }
        } else {
            // RenderLayers do not need to apply any global rotations or translations.
            // The Vanilla PoseStack passed to the layer ALREADY contains the player's 
            // swimming pitch, fall flying pitch, and swimming translations!
        }
    }
}
