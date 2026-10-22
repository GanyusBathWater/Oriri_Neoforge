package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.LamiaTailAnimatable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;

public class LamiaTailModel extends AbstractPlayerCosmeticModel<LamiaTailAnimatable> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/lamia_essence_tail.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/lamia_essence_tail.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/lamia.animation.json");

    @Override
    public ResourceLocation getModelResource(LamiaTailAnimatable object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(LamiaTailAnimatable object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(LamiaTailAnimatable object) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(LamiaTailAnimatable animatable, long instanceId,
                                    AnimationState<LamiaTailAnimatable> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // 1. Anchor the root tail bone to player body pitch and crouch offset
        GeoBone root = getAnimationProcessor().getBone("root");
        if (root != null) {
            root.setRotX(0.0F);
            root.setRotY(0.0F);
            root.setRotZ(0.0F);
            root.setPosX(0.0F);
            root.setPosY(0.0F);
            root.setPosZ(0.0F);
        }

        GeoBone rootBone = getAnimationProcessor().getBone("body");
        if (rootBone == null) {
            rootBone = getAnimationProcessor().getBone("tail");
        }
        Entity entity = animationState.getData(DataTickets.ENTITY);

        if (entity instanceof AbstractClientPlayer player) {
            EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                PlayerModel<?> playerModel = playerRenderer.getModel();
                if (rootBone != null) {
                    boolean isCrouching = player.isCrouching();
                    // Tail root acts as legs and must stay upright; do not tilt forward with the leaning torso when crouching
                    rootBone.setRotX(0.0F);
                    rootBone.setRotY(-playerModel.body.yRot);
                    rootBone.setRotZ(-playerModel.body.zRot);

                    rootBone.setPosX(-playerModel.body.x);
                    rootBone.setPosY(-playerModel.body.y);
                    // Anchor pelvis cleanly behind torso (shifts back when crouching like vanilla legs: +4.0 pixels along +Z)
                    float crouchZOffset = isCrouching ? 4.0F : 0.0F;
                    rootBone.setPosZ(playerModel.body.z + crouchZOffset);
                }
            }

            // 2. Procedural tail movement, smooth turning & ground pinning
            float partialTick = animationState.getPartialTick();
            boolean isFlying = player.getAbilities().flying || player.isFallFlying();
            boolean isSleeping = player.isSleeping();
            boolean isSwimming = player.isVisuallySwimming() || player.isInWater();
            boolean isPassenger = player.isPassenger();
            Entity vehicle = player.getVehicle();

            net.ganyusbathwater.oririmod.client.physics.TailPhysicsController.applyTailAnimation(
                    name -> getAnimationProcessor().getBone(name),
                    player,
                    partialTick,
                    false, // Player doesn't do mob chokehold
                    isSleeping,
                    isFlying,
                    isSwimming,
                    isPassenger,
                    vehicle
            );
        }
    }
}
