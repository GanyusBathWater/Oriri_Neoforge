package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.FairyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FairyModel extends GeoModel<FairyEntity> {
    private final ResourceLocation model = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/fairy.geo.json");
    private final ResourceLocation animation = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/fairy.animation.json");
    private final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/fairy.png");

    @Override
    public ResourceLocation getModelResource(FairyEntity entity) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(FairyEntity entity) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(FairyEntity entity) {
        return animation;
    }

    @Override
    public void setCustomAnimations(FairyEntity animatable, long instanceId, software.bernie.geckolib.animation.AnimationState<FairyEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        software.bernie.geckolib.cache.object.GeoBone head = getAnimationProcessor().getBone("head");
        float netHeadYaw = 0.0F;
        float headPitch = 0.0F;

        if (head != null) {
            software.bernie.geckolib.model.data.EntityModelData entityData = animationState.getData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA);
            if (entityData != null) {
                netHeadYaw = entityData.netHeadYaw();
                headPitch = entityData.headPitch();
                head.setRotX(headPitch * net.minecraft.util.Mth.DEG_TO_RAD);
                head.setRotY(netHeadYaw * net.minecraft.util.Mth.DEG_TO_RAD);
            }
        }

        float partialTick = animationState.getPartialTick();
        net.ganyusbathwater.oririmod.client.physics.HairPhysicsController.applyHairPhysics(
                animatable,
                name -> getAnimationProcessor().getBone(name),
                partialTick,
                netHeadYaw,
                headPitch
        );
    }
}
