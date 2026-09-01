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

        net.minecraft.world.entity.Entity entity = animationState.getData(software.bernie.geckolib.constant.DataTickets.ENTITY);
        Boolean isFirstPerson = animationState.getData(FIRST_PERSON);
        if (isFirstPerson == null) isFirstPerson = false;

        if (entity instanceof net.minecraft.client.player.AbstractClientPlayer player) {
            net.minecraft.client.renderer.entity.EntityRenderer<?> renderer = net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer playerRenderer) {
                @SuppressWarnings("unchecked")
                net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer> playerModel = 
                    (net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer>) playerRenderer.getModel();
                
                // We DO NOT need to call setupAnim! 
                // The PlayerRenderer already called it for this exact frame before invoking RenderLayers.
                // This means playerModel ALREADY has the perfect xRot, yRot, zRot for all body parts!

                if (head != null && !isFirstPerson) {
                    // When the RenderLayer locks this model to the Vanilla head bone,
                    // we DO NOT need to apply any rotations! The PoseStack handles it.
                    // However, if it's the Tail model or Land model, it needs raw tracking.
                    head.setRotX(-playerModel.head.xRot);
                    head.setRotY(-playerModel.head.yRot);
                    head.setRotZ(-playerModel.head.zRot);
                    
                    head.setPosX(playerModel.head.x);
                    head.setPosY(-playerModel.head.y);
                    head.setPosZ(playerModel.head.z);
                }
                if (rightArm != null && !isFirstPerson) {
                    rightArm.setRotX(-playerModel.rightArm.xRot);
                    rightArm.setRotY(-playerModel.rightArm.yRot);
                    rightArm.setRotZ(-playerModel.rightArm.zRot);
                }
                if (leftArm != null && !isFirstPerson) {
                    leftArm.setRotX(-playerModel.leftArm.xRot);
                    leftArm.setRotY(-playerModel.leftArm.yRot);
                    leftArm.setRotZ(-playerModel.leftArm.zRot);
                }
                if (rightLeg != null) {
                    rightLeg.setRotX(-playerModel.rightLeg.xRot);
                    rightLeg.setRotY(-playerModel.rightLeg.yRot);
                    rightLeg.setRotZ(-playerModel.rightLeg.zRot);
                    
                    rightLeg.setPosX(playerModel.rightLeg.x - (-1.9F));
                    rightLeg.setPosY(-(playerModel.rightLeg.y - 12.0F));
                    rightLeg.setPosZ(playerModel.rightLeg.z);
                }
                if (leftLeg != null) {
                    leftLeg.setRotX(-playerModel.leftLeg.xRot);
                    leftLeg.setRotY(-playerModel.leftLeg.yRot);
                    leftLeg.setRotZ(-playerModel.leftLeg.zRot);
                    
                    leftLeg.setPosX(playerModel.leftLeg.x - 1.9F);
                    leftLeg.setPosY(-(playerModel.leftLeg.y - 12.0F));
                    leftLeg.setPosZ(playerModel.leftLeg.z);
                }
                
                GeoBone torso = getAnimationProcessor().getBone("torso");
                GeoBone body = getAnimationProcessor().getBone("body");
                if (body != null) {
                    body.setRotX(-playerModel.body.xRot);
                    body.setRotY(-playerModel.body.yRot);
                    body.setRotZ(-playerModel.body.zRot);
                    
                    body.setPosX(playerModel.body.x);
                    body.setPosY(-playerModel.body.y);
                    body.setPosZ(playerModel.body.z);
                } else if (torso != null) {
                    torso.setRotX(-playerModel.body.xRot);
                    torso.setRotY(-playerModel.body.yRot);
                    torso.setRotZ(-playerModel.body.zRot);
                    
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
