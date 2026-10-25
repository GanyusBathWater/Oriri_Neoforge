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

                GeoBone head = getAnimationProcessor().getBone("head");
                GeoBone torso = getAnimationProcessor().getBone("torso");
                GeoBone body = getAnimationProcessor().getBone("body");
                GeoBone mainBody = body != null ? body : torso;

                GeoBone rightArm = getAnimationProcessor().getBone("right_arm");
                GeoBone leftArm = getAnimationProcessor().getBone("left_arm");
                GeoBone rightLeg = getAnimationProcessor().getBone("right_leg");
                GeoBone leftLeg = getAnimationProcessor().getBone("left_leg");

                // Legacy grouping containers (if any exist in older models)
                GeoBone arms = getAnimationProcessor().getBone("arms");
                if (arms != null) { arms.updateRotation(0, 0, 0); arms.setRotX(0); arms.setRotY(0); arms.setRotZ(0); }
                GeoBone legsBone = getAnimationProcessor().getBone("legs");
                if (legsBone != null) { legsBone.updateRotation(0, 0, 0); legsBone.setRotX(0); legsBone.setRotY(0); legsBone.setRotZ(0); }

                // Helper to check if a bone inherits from the main body in the model hierarchy
                java.util.function.Predicate<GeoBone> isDescendantOfBody = (b) -> {
                    if (mainBody == null || b == null) return false;
                    GeoBone p = b.getParent();
                    while (p != null) {
                        if (p == mainBody) return true;
                        p = p.getParent();
                    }
                    return false;
                };

                float bodyXRot = 0, bodyYRot = 0, bodyZRot = 0;
                float bodyDX = 0, bodyDY = 0, bodyDZ = 0;

                // 1. Synchronize Body / Torso
                if (mainBody != null) {
                    mainBody.setRotX(-playerModel.body.xRot);
                    mainBody.setRotY(-playerModel.body.yRot);
                    mainBody.setRotZ(-playerModel.body.zRot);
                    
                    mainBody.setPosX(-playerModel.body.x);
                    mainBody.setPosY(-playerModel.body.y);
                    mainBody.setPosZ(playerModel.body.z);
                    
                    bodyXRot = playerModel.body.xRot;
                    bodyYRot = playerModel.body.yRot;
                    bodyZRot = playerModel.body.zRot;
                    bodyDX = playerModel.body.x;
                    bodyDY = playerModel.body.y;
                    bodyDZ = playerModel.body.z;
                }

                // 2. Synchronize Head
                if (head != null && !isFirstPerson) {
                    boolean childOfBody = isDescendantOfBody.test(head);
                    float parentXRot = childOfBody ? bodyXRot : 0;
                    float parentYRot = childOfBody ? bodyYRot : 0;
                    float parentZRot = childOfBody ? bodyZRot : 0;
                    float parentDX = childOfBody ? bodyDX : 0;
                    float parentDY = childOfBody ? bodyDY : 0;
                    float parentDZ = childOfBody ? bodyDZ : 0;

                    head.setRotX(-(playerModel.head.xRot - parentXRot));
                    head.setRotY(-(playerModel.head.yRot - parentYRot));
                    head.setRotZ(-(playerModel.head.zRot - parentZRot));
                    
                    head.setPosX(-(playerModel.head.x - parentDX));
                    head.setPosY(-(playerModel.head.y - parentDY));
                    head.setPosZ(playerModel.head.z - parentDZ);

                    // Compensate for any non-standard body pivot so the head stays locked to the neck during tilt
                    if (!childOfBody && mainBody != null) {
                        float bodyPivotY = mainBody.getPivotY();
                        float deltaNeckY = 24.0F - bodyPivotY;
                        if (Math.abs(deltaNeckY) > 0.01F) {
                            float rotX = mainBody.getRotX();
                            float neckShiftZ = deltaNeckY * (float) Math.sin(-rotX);
                            float neckShiftY = deltaNeckY * ((float) Math.cos(rotX) - 1.0F);
                            head.setPosZ(head.getPosZ() + neckShiftZ);
                            head.setPosY(head.getPosY() + neckShiftY);
                        }
                    }
                }

                // 3. Synchronize Right Arm
                if (rightArm != null && !isFirstPerson) {
                    boolean childOfBody = isDescendantOfBody.test(rightArm);
                    float parentXRot = childOfBody ? bodyXRot : 0;
                    float parentYRot = childOfBody ? bodyYRot : 0;
                    float parentZRot = childOfBody ? bodyZRot : 0;
                    float parentDX = childOfBody ? bodyDX : 0;
                    float parentDY = childOfBody ? bodyDY : 0;
                    float parentDZ = childOfBody ? bodyDZ : 0;

                    rightArm.setRotX(-(playerModel.rightArm.xRot - parentXRot));
                    rightArm.setRotY(-(playerModel.rightArm.yRot - parentYRot));
                    rightArm.setRotZ(-(playerModel.rightArm.zRot - parentZRot));
                    
                    float dX = playerModel.rightArm.x - (-5.0F);
                    float dY = playerModel.rightArm.y - 2.0F;
                    float dZ = playerModel.rightArm.z - 0.0F;

                    rightArm.setPosX(-(dX - parentDX));
                    rightArm.setPosY(-(dY - parentDY));
                    rightArm.setPosZ(dZ - parentDZ);
                }

                // 4. Synchronize Left Arm
                if (leftArm != null && !isFirstPerson) {
                    boolean childOfBody = isDescendantOfBody.test(leftArm);
                    float parentXRot = childOfBody ? bodyXRot : 0;
                    float parentYRot = childOfBody ? bodyYRot : 0;
                    float parentZRot = childOfBody ? bodyZRot : 0;
                    float parentDX = childOfBody ? bodyDX : 0;
                    float parentDY = childOfBody ? bodyDY : 0;
                    float parentDZ = childOfBody ? bodyDZ : 0;

                    leftArm.setRotX(-(playerModel.leftArm.xRot - parentXRot));
                    leftArm.setRotY(-(playerModel.leftArm.yRot - parentYRot));
                    leftArm.setRotZ(-(playerModel.leftArm.zRot - parentZRot));
                    
                    float dX = playerModel.leftArm.x - 5.0F;
                    float dY = playerModel.leftArm.y - 2.0F;
                    float dZ = playerModel.leftArm.z - 0.0F;

                    leftArm.setPosX(-(dX - parentDX));
                    leftArm.setPosY(-(dY - parentDY));
                    leftArm.setPosZ(dZ - parentDZ);
                }

                // 5. Synchronize Right Leg
                if (rightLeg != null) {
                    boolean childOfBody = isDescendantOfBody.test(rightLeg);
                    float parentXRot = childOfBody ? bodyXRot : 0;
                    float parentYRot = childOfBody ? bodyYRot : 0;
                    float parentZRot = childOfBody ? bodyZRot : 0;
                    float parentDX = childOfBody ? bodyDX : 0;
                    float parentDY = childOfBody ? bodyDY : 0;
                    float parentDZ = childOfBody ? bodyDZ : 0;

                    rightLeg.setRotX(-(playerModel.rightLeg.xRot - parentXRot));
                    rightLeg.setRotY(-(playerModel.rightLeg.yRot - parentYRot));
                    rightLeg.setRotZ(-(playerModel.rightLeg.zRot - parentZRot));
                    
                    float dX = playerModel.rightLeg.x - (-1.9F);
                    float dY = playerModel.rightLeg.y - 12.0F;
                    float dZ = playerModel.rightLeg.z - 0.0F;

                    rightLeg.setPosX(-(dX - parentDX));
                    rightLeg.setPosY(-(dY - parentDY));
                    rightLeg.setPosZ(dZ - parentDZ);
                }

                // 6. Synchronize Left Leg
                if (leftLeg != null) {
                    boolean childOfBody = isDescendantOfBody.test(leftLeg);
                    float parentXRot = childOfBody ? bodyXRot : 0;
                    float parentYRot = childOfBody ? bodyYRot : 0;
                    float parentZRot = childOfBody ? bodyZRot : 0;
                    float parentDX = childOfBody ? bodyDX : 0;
                    float parentDY = childOfBody ? bodyDY : 0;
                    float parentDZ = childOfBody ? bodyDZ : 0;

                    leftLeg.setRotX(-(playerModel.leftLeg.xRot - parentXRot));
                    leftLeg.setRotY(-(playerModel.leftLeg.yRot - parentYRot));
                    leftLeg.setRotZ(-(playerModel.leftLeg.zRot - parentZRot));
                    
                    float dX = playerModel.leftLeg.x - 1.9F;
                    float dY = playerModel.leftLeg.y - 12.0F;
                    float dZ = playerModel.leftLeg.z - 0.0F;

                    leftLeg.setPosX(-(dX - parentDX));
                    leftLeg.setPosY(-(dY - parentDY));
                    leftLeg.setPosZ(dZ - parentDZ);
                }

                // 7. Synchronize Wings (attached to back) if not already parented to body
                GeoBone wings = getAnimationProcessor().getBone("wings");
                if (wings != null && mainBody != null && !isDescendantOfBody.test(wings)) {
                    wings.setRotX(mainBody.getRotX());
                    wings.setRotY(mainBody.getRotY());
                    wings.setRotZ(mainBody.getRotZ());
                    wings.setPosX(mainBody.getPosX());
                    wings.setPosY(mainBody.getPosY());
                    wings.setPosZ(mainBody.getPosZ());
                }

                // Crawling / Swimming pose enhancement
                float partialTick = animationState.getPartialTick();
                float swimAmount = player.getSwimAmount(partialTick);
                if (player.isVisuallySwimming() && swimAmount < 0.01F) {
                    swimAmount = 1.0F;
                }

                if (swimAmount > 0.0F && !isFirstPerson) {
                    // 1. Head: Lift face so player looks forward along the crawlway instead of down into the dirt
                    if (head != null) {
                        float lookPitch = player.getXRot(); // negative = looking up, positive = looking down
                        // When looking forward (pitch = 0), tilt head up ~75 degrees (1.30 rad) relative to horizontal body
                        float crawlHeadX = 1.30F - (lookPitch * net.minecraft.util.Mth.DEG_TO_RAD * 0.7F);
                        head.setRotX(net.minecraft.util.Mth.rotLerp(swimAmount, head.getRotX(), crawlHeadX));
                    }

                    // 2. Arms: Reach forward along the floor and perform alternating crawl strokes
                    if (rightArm != null && leftArm != null) {
                        float limbSpeed = Math.min(player.walkAnimation.speed(partialTick), 1.0F);
                        double horizDist = player.getDeltaMovement().horizontalDistance();
                        float effectiveSpeed = Math.max(limbSpeed, (float) Math.min(horizDist * 4.0, 1.0));

                        float limbPos = player.walkAnimation.position(partialTick);
                        if (effectiveSpeed > 0.02F && limbPos == 0.0F) {
                            limbPos = (player.tickCount + partialTick) * 0.35F;
                        }

                        // In Blockbench, reaching forward along the body is NEGATIVE rotX (~112 degrees forward along floor)
                        float baseReach = -1.95F;
                        float strokeAmp = Math.min(effectiveSpeed * 1.8F, 1.0F) * 0.65F;
                        float stroke = (float) Math.sin(limbPos * 1.2F) * strokeAmp;

                        float rightCrawlX = baseReach + stroke;
                        float leftCrawlX = baseReach - stroke;

                        rightArm.setRotX(net.minecraft.util.Mth.rotLerp(swimAmount, rightArm.getRotX(), rightCrawlX));
                        leftArm.setRotX(net.minecraft.util.Mth.rotLerp(swimAmount, leftArm.getRotX(), leftCrawlX));

                        // Slight natural inward/outward motion of forearms during crawl
                        float rightCrawlZ = -0.15F + (float) Math.cos(limbPos * 1.2F) * 0.15F * strokeAmp;
                        float leftCrawlZ = 0.15F - (float) Math.cos(limbPos * 1.2F) * 0.15F * strokeAmp;

                        rightArm.setRotZ(net.minecraft.util.Mth.rotLerp(swimAmount, rightArm.getRotZ(), rightCrawlZ));
                        leftArm.setRotZ(net.minecraft.util.Mth.rotLerp(swimAmount, leftArm.getRotZ(), leftCrawlZ));
                    }
                }

                float netHeadYaw = playerModel.head.yRot * net.minecraft.util.Mth.RAD_TO_DEG;
                float headPitch = playerModel.head.xRot * net.minecraft.util.Mth.RAD_TO_DEG;
                net.ganyusbathwater.oririmod.client.physics.HairPhysicsController.applyHairPhysics(
                        player,
                        name -> getAnimationProcessor().getBone(name),
                        partialTick,
                        netHeadYaw,
                        headPitch
                );
            }
        }
    }

    public static net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(net.minecraft.client.player.AbstractClientPlayer player, net.minecraft.world.InteractionHand hand) {
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
