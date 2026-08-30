package net.ganyusbathwater.oririmod.client.render.entity.template;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public abstract class AbstractPlayerCosmeticModel<T extends AbstractPlayerCosmeticAnimatable> extends GeoModel<T> {

    public static final software.bernie.geckolib.constant.dataticket.DataTicket<Boolean> FIRST_PERSON = new software.bernie.geckolib.constant.dataticket.DataTicket<>("first_person", Boolean.class);

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        
        GeoBone head = getAnimationProcessor().getBone("head");
        GeoBone rightArm = getAnimationProcessor().getBone("right_arm");
        GeoBone leftArm = getAnimationProcessor().getBone("left_arm");
        GeoBone rightLeg = getAnimationProcessor().getBone("right_leg");
        GeoBone leftLeg = getAnimationProcessor().getBone("left_leg");

        EntityModelData entityData = animationState.getData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA);
        net.minecraft.world.entity.Entity entity = animationState.getData(software.bernie.geckolib.constant.DataTickets.ENTITY);
        Boolean isFirstPerson = animationState.getData(FIRST_PERSON);
        if (isFirstPerson == null) isFirstPerson = false;

        if (entityData != null && entity instanceof net.minecraft.client.player.AbstractClientPlayer player) {
            net.minecraft.client.renderer.entity.EntityRenderer<?> renderer = net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer playerRenderer) {
                @SuppressWarnings("unchecked")
                net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer> playerModel = 
                    (net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer>) playerRenderer.getModel();
                
                float partialTick = animationState.getPartialTick();
                float limbSwing = animationState.getLimbSwing();
                float limbSwingAmount = animationState.getLimbSwingAmount();
                float netHeadYaw = entityData.netHeadYaw();
                float headPitch = entityData.headPitch();
                
                // Synchronize all necessary Vanilla model properties
                playerModel.crouching = player.isCrouching();
                playerModel.attackTime = player.getAttackAnim(partialTick);
                playerModel.riding = player.isPassenger() && (player.getVehicle() != null && player.getVehicle().shouldRiderSit());
                playerModel.young = player.isBaby();

                net.minecraft.client.model.HumanoidModel.ArmPose mainHandPose = getArmPose(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                net.minecraft.client.model.HumanoidModel.ArmPose offHandPose = getArmPose(player, net.minecraft.world.InteractionHand.OFF_HAND);
                if (mainHandPose.isTwoHanded()) {
                    offHandPose = player.getOffhandItem().isEmpty() ? net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY : net.minecraft.client.model.HumanoidModel.ArmPose.ITEM;
                }
                if (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) {
                    playerModel.rightArmPose = mainHandPose;
                    playerModel.leftArmPose = offHandPose;
                } else {
                    playerModel.rightArmPose = offHandPose;
                    playerModel.leftArmPose = mainHandPose;
                }
                
                playerModel.prepareMobModel(player, limbSwing, limbSwingAmount, partialTick);
                playerModel.setupAnim(player, limbSwing, limbSwingAmount, player.tickCount + partialTick, netHeadYaw, headPitch);

                if (head != null && !isFirstPerson) {
                    float headXRot = playerModel.head.xRot;
                    if (player.isFallFlying() || player.isVisuallySwimming() || player.getSwimAmount(animationState.getPartialTick()) > 0.0F) {
                        headXRot = -headXRot;
                    }
                    
                    head.setRotX(headXRot + playerModel.body.xRot);
                    head.setRotY(-playerModel.head.yRot + playerModel.body.yRot);
                    head.setRotZ(playerModel.head.zRot - playerModel.body.zRot);
                    
                    head.setPosX(playerModel.head.x - playerModel.body.x);
                    head.setPosY(-(playerModel.head.y - playerModel.body.y));
                    head.setPosZ(playerModel.head.z - playerModel.body.z);
                }
                if (rightArm != null && !isFirstPerson) {
                    rightArm.setRotX(-playerModel.rightArm.xRot + playerModel.body.xRot);
                    rightArm.setRotY(-playerModel.rightArm.yRot + playerModel.body.yRot);
                    rightArm.setRotZ(playerModel.rightArm.zRot - playerModel.body.zRot);
                    
                    rightArm.setPosX((playerModel.rightArm.x - (-5.0F)) - playerModel.body.x);
                    rightArm.setPosY(-((playerModel.rightArm.y - 2.0F) - playerModel.body.y));
                    rightArm.setPosZ(playerModel.rightArm.z - playerModel.body.z);
                }
                if (leftArm != null && !isFirstPerson) {
                    leftArm.setRotX(-playerModel.leftArm.xRot + playerModel.body.xRot);
                    leftArm.setRotY(-playerModel.leftArm.yRot + playerModel.body.yRot);
                    leftArm.setRotZ(playerModel.leftArm.zRot - playerModel.body.zRot);
                    
                    leftArm.setPosX((playerModel.leftArm.x - 5.0F) - playerModel.body.x);
                    leftArm.setPosY(-((playerModel.leftArm.y - 2.0F) - playerModel.body.y));
                    leftArm.setPosZ(playerModel.leftArm.z - playerModel.body.z);
                }
                if (rightLeg != null) {
                    rightLeg.setRotX(-playerModel.rightLeg.xRot);
                    rightLeg.setRotY(-playerModel.rightLeg.yRot);
                    rightLeg.setRotZ(playerModel.rightLeg.zRot);
                    
                    rightLeg.setPosX(playerModel.rightLeg.x - (-1.9F));
                    rightLeg.setPosY(-(playerModel.rightLeg.y - 12.0F));
                    rightLeg.setPosZ(playerModel.rightLeg.z);
                }
                if (leftLeg != null) {
                    leftLeg.setRotX(-playerModel.leftLeg.xRot);
                    leftLeg.setRotY(-playerModel.leftLeg.yRot);
                    leftLeg.setRotZ(playerModel.leftLeg.zRot);
                    
                    leftLeg.setPosX(playerModel.leftLeg.x - 1.9F);
                    leftLeg.setPosY(-(playerModel.leftLeg.y - 12.0F));
                    leftLeg.setPosZ(playerModel.leftLeg.z);
                }
                
                GeoBone torso = getAnimationProcessor().getBone("torso");
                GeoBone body = getAnimationProcessor().getBone("body");
                if (body != null) {
                    body.setRotX(-playerModel.body.xRot);
                    body.setRotY(-playerModel.body.yRot);
                    body.setRotZ(playerModel.body.zRot);
                    
                    body.setPosX(playerModel.body.x);
                    body.setPosY(-playerModel.body.y);
                    body.setPosZ(playerModel.body.z);
                } else if (torso != null) {
                    torso.setRotX(-playerModel.body.xRot);
                    torso.setRotY(-playerModel.body.yRot);
                    torso.setRotZ(playerModel.body.zRot);
                    
                    torso.setPosX(playerModel.body.x);
                    torso.setPosY(-playerModel.body.y);
                    torso.setPosZ(playerModel.body.z);
                }
            }
        }
    }

    private static net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(net.minecraft.client.player.AbstractClientPlayer player, net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.item.ItemStack itemstack = player.getItemInHand(hand);
        if (itemstack.isEmpty()) {
            return net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        } else {
            if (player.getUsedItemHand() == hand && player.getUseItemRemainingTicks() > 0) {
                net.minecraft.world.item.UseAnim useanim = itemstack.getUseAnimation();
                if (useanim == net.minecraft.world.item.UseAnim.BLOCK) return net.minecraft.client.model.HumanoidModel.ArmPose.BLOCK;
                if (useanim == net.minecraft.world.item.UseAnim.BOW) return net.minecraft.client.model.HumanoidModel.ArmPose.BOW_AND_ARROW;
                if (useanim == net.minecraft.world.item.UseAnim.SPEAR) return net.minecraft.client.model.HumanoidModel.ArmPose.THROW_SPEAR;
                if (useanim == net.minecraft.world.item.UseAnim.CROSSBOW && hand == player.getUsedItemHand()) return net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                if (useanim == net.minecraft.world.item.UseAnim.SPYGLASS) return net.minecraft.client.model.HumanoidModel.ArmPose.SPYGLASS;
                if (useanim == net.minecraft.world.item.UseAnim.TOOT_HORN) return net.minecraft.client.model.HumanoidModel.ArmPose.TOOT_HORN;
                if (useanim == net.minecraft.world.item.UseAnim.BRUSH) return net.minecraft.client.model.HumanoidModel.ArmPose.BRUSH;
            } else if (!player.swinging && itemstack.getItem() instanceof net.minecraft.world.item.CrossbowItem && net.minecraft.world.item.CrossbowItem.isCharged(itemstack)) {
                return net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
            net.minecraft.client.model.HumanoidModel.ArmPose forgeArmPose = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(itemstack).getArmPose(player, hand, itemstack);
            if (forgeArmPose != null) return forgeArmPose;

            return net.minecraft.client.model.HumanoidModel.ArmPose.ITEM;
        }
    }
}
