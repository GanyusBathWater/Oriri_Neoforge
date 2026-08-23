package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.AuroraCosmeticAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class AuroraCosmeticModel extends GeoModel<AuroraCosmeticAnimatable> {

    @Override
    public ResourceLocation getModelResource(AuroraCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/aurora_cosmetic.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AuroraCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/aurora_cosmetic.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AuroraCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/aurora_cosmetic.animation.json");
    }

    public static final software.bernie.geckolib.constant.dataticket.DataTicket<Boolean> FIRST_PERSON = new software.bernie.geckolib.constant.dataticket.DataTicket<>("first_person", Boolean.class);

    @Override
    public void setCustomAnimations(AuroraCosmeticAnimatable animatable, long instanceId, AnimationState<AuroraCosmeticAnimatable> animationState) {
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
                
                // Synchronize all necessary Vanilla model properties because the standard PlayerRenderer is cancelled
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

                // Copy rotations to GeckoLib bones. 
                // Vanilla uses Y-down, GeckoLib uses Y-up. The transformation is a 180-degree flip around Z.
                // This means X and Y rotations must be inverted, while Z rotation remains identical!
                if (head != null && !isFirstPerson) {
                    float headXRot = playerModel.head.xRot;
                    // Because the global rotation makes the model fly belly-up to keep the wings on top,
                    // the vanilla -45 degree neck bend points at the ground. We invert it to +45 to point up.
                    // This applies to gliding, swimming, and crawling (which all tilt the body horizontally).
                    if (player.isFallFlying() || player.isVisuallySwimming() || player.getSwimAmount(animationState.getPartialTick()) > 0.0F) {
                        headXRot = -headXRot;
                    }
                    head.setRotX(headXRot);
                    head.setRotY(playerModel.head.yRot);
                    head.setRotZ(playerModel.head.zRot);
                }
                if (rightArm != null && !isFirstPerson) {
                    rightArm.setRotX(-playerModel.rightArm.xRot);
                    rightArm.setRotY(-playerModel.rightArm.yRot);
                    rightArm.setRotZ(playerModel.rightArm.zRot);
                }
                if (leftArm != null && !isFirstPerson) {
                    leftArm.setRotX(-playerModel.leftArm.xRot);
                    leftArm.setRotY(-playerModel.leftArm.yRot);
                    leftArm.setRotZ(playerModel.leftArm.zRot);
                }
                if (rightLeg != null) {
                    rightLeg.setRotX(-playerModel.rightLeg.xRot);
                    rightLeg.setRotY(-playerModel.rightLeg.yRot);
                    rightLeg.setRotZ(playerModel.rightLeg.zRot);
                }
                if (leftLeg != null) {
                    leftLeg.setRotX(-playerModel.leftLeg.xRot);
                    leftLeg.setRotY(-playerModel.leftLeg.yRot);
                    leftLeg.setRotZ(playerModel.leftLeg.zRot);
                }
                
                GeoBone torso = getAnimationProcessor().getBone("torso");
                if (torso != null) {
                    torso.setRotX(-playerModel.body.xRot);
                    torso.setRotY(-playerModel.body.yRot);
                    torso.setRotZ(playerModel.body.zRot);
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
