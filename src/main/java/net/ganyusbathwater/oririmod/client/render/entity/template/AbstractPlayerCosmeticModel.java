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

                GeoBone torso = getAnimationProcessor().getBone("torso");
                GeoBone body = getAnimationProcessor().getBone("body");
                GeoBone mainBody = body != null ? body : torso;

                float bodyXRot = 0, bodyYRot = 0, bodyZRot = 0;
                float bodyDX = 0, bodyDY = 0, bodyDZ = 0;

                if (mainBody != null) {
                    mainBody.setRotX(-playerModel.body.xRot);
                    mainBody.setRotY(-playerModel.body.yRot);
                    mainBody.setRotZ(-playerModel.body.zRot);
                    
                    mainBody.setPosX(playerModel.body.x);
                    mainBody.setPosY(-playerModel.body.y);
                    mainBody.setPosZ(playerModel.body.z);
                    
                    bodyXRot = playerModel.body.xRot;
                    bodyYRot = playerModel.body.yRot;
                    bodyZRot = playerModel.body.zRot;
                    bodyDX = playerModel.body.x;
                    bodyDY = playerModel.body.y;
                    bodyDZ = playerModel.body.z;
                }

                // Helper to check if a bone inherits from the main body
                java.util.function.Predicate<GeoBone> isDescendantOfBody = (b) -> {
                    if (mainBody == null) return false;
                    GeoBone p = b.getParent();
                    while (p != null) {
                        if (p == mainBody) return true;
                        p = p.getParent();
                    }
                    return false;
                };

                if (head != null && !isFirstPerson) {
                    boolean childOfBody = isDescendantOfBody.test(head);
                    head.setRotX(-(playerModel.head.xRot - (childOfBody ? bodyXRot : 0)));
                    head.setRotY(-(playerModel.head.yRot - (childOfBody ? bodyYRot : 0)));
                    head.setRotZ(-(playerModel.head.zRot - (childOfBody ? bodyZRot : 0)));
                    
                    if (!childOfBody) {
                        head.setPosX(playerModel.head.x);
                        head.setPosY(-playerModel.head.y);
                        head.setPosZ(playerModel.head.z);
                    } else {
                        head.setPosX(0); head.setPosY(0); head.setPosZ(0);
                    }
                }
                if (rightArm != null && !isFirstPerson) {
                    boolean childOfBody = isDescendantOfBody.test(rightArm);
                    rightArm.setRotX(-(playerModel.rightArm.xRot - (childOfBody ? bodyXRot : 0)));
                    rightArm.setRotY(-(playerModel.rightArm.yRot - (childOfBody ? bodyYRot : 0)));
                    rightArm.setRotZ((playerModel.rightArm.zRot - (childOfBody ? bodyZRot : 0)));
                    
                    if (!childOfBody) {
                        rightArm.setPosX((playerModel.rightArm.x - (-5.0F)));
                        rightArm.setPosY(-(playerModel.rightArm.y - 2.0F));
                        rightArm.setPosZ(playerModel.rightArm.z);
                    } else {
                        rightArm.setPosX(0); rightArm.setPosY(0); rightArm.setPosZ(0);
                    }
                }
                if (leftArm != null && !isFirstPerson) {
                    boolean childOfBody = isDescendantOfBody.test(leftArm);
                    leftArm.setRotX(-(playerModel.leftArm.xRot - (childOfBody ? bodyXRot : 0)));
                    leftArm.setRotY(-(playerModel.leftArm.yRot - (childOfBody ? bodyYRot : 0)));
                    leftArm.setRotZ((playerModel.leftArm.zRot - (childOfBody ? bodyZRot : 0)));
                    
                    if (!childOfBody) {
                        leftArm.setPosX((playerModel.leftArm.x - 5.0F));
                        leftArm.setPosY(-(playerModel.leftArm.y - 2.0F));
                        leftArm.setPosZ(playerModel.leftArm.z);
                    } else {
                        leftArm.setPosX(0); leftArm.setPosY(0); leftArm.setPosZ(0);
                    }
                }
                if (rightLeg != null) {
                    boolean childOfBody = isDescendantOfBody.test(rightLeg);
                    rightLeg.setRotX(-(playerModel.rightLeg.xRot - (childOfBody ? bodyXRot : 0)));
                    rightLeg.setRotY(-(playerModel.rightLeg.yRot - (childOfBody ? bodyYRot : 0)));
                    rightLeg.setRotZ((playerModel.rightLeg.zRot - (childOfBody ? bodyZRot : 0)));
                    
                    if (!childOfBody) {
                        rightLeg.setPosX((playerModel.rightLeg.x - (-1.9F)));
                        rightLeg.setPosY(-(playerModel.rightLeg.y - 12.0F));
                        rightLeg.setPosZ(playerModel.rightLeg.z);
                    } else {
                        rightLeg.setPosX(0); rightLeg.setPosY(0); rightLeg.setPosZ(0);
                    }
                }
                if (leftLeg != null) {
                    boolean childOfBody = isDescendantOfBody.test(leftLeg);
                    leftLeg.setRotX(-(playerModel.leftLeg.xRot - (childOfBody ? bodyXRot : 0)));
                    leftLeg.setRotY(-(playerModel.leftLeg.yRot - (childOfBody ? bodyYRot : 0)));
                    leftLeg.setRotZ((playerModel.leftLeg.zRot - (childOfBody ? bodyZRot : 0)));
                    
                    if (!childOfBody) {
                        leftLeg.setPosX((playerModel.leftLeg.x - 1.9F));
                        leftLeg.setPosY(-(playerModel.leftLeg.y - 12.0F));
                        leftLeg.setPosZ(playerModel.leftLeg.z);
                    } else {
                        leftLeg.setPosX(0); leftLeg.setPosY(0); leftLeg.setPosZ(0);
                    }
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
