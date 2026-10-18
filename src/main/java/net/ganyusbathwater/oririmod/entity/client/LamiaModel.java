package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.entity.model.SnakeTailPhysics;
import net.ganyusbathwater.oririmod.entity.custom.LamiaEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class LamiaModel extends GeoModel<LamiaEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/lamia.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/lamia.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/lamia.animation.json");

    @Override
    public ResourceLocation getModelResource(LamiaEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(LamiaEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(LamiaEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(LamiaEntity animatable, long instanceId,
                                    AnimationState<LamiaEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // Ensure upper body is visible
        GeoBone torso = getAnimationProcessor().getBone("torso");
        if (torso != null) {
            torso.setHidden(false);
        }

        // Orient tail to extend backwards behind the lower back
        GeoBone tail = getAnimationProcessor().getBone("tail");
        if (tail == null) {
            tail = getAnimationProcessor().getBone("body");
        }
        if (tail != null) {
            tail.setRotY(0.0F);
            tail.setPosZ(0.0F);
        }

        // 1. Head tracking
        GeoBone head = getAnimationProcessor().getBone("head");
        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            if (entityData != null) {
                head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
                head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
            }
        }

        // 2. Procedural Slithering, Smooth Turning & Tail Ground Pinning
        float partialTick = animationState.getPartialTick();
        boolean isChoking = animatable.getAttackState() == LamiaEntity.ATTACK_CHOKEHOLD;
        boolean isSwimming = animatable.isInWater() || animatable.isVisuallySwimming();

        net.ganyusbathwater.oririmod.client.physics.TailPhysicsController.applyTailAnimation(
                name -> getAnimationProcessor().getBone(name),
                animatable,
                partialTick,
                isChoking,
                animatable.isSleeping(),
                animatable.isFallFlying(),
                isSwimming
        );
    }
}

