package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.DryadEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DryadModel extends GeoModel<DryadEntity> {
    @Override
    public ResourceLocation getModelResource(DryadEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/dryade.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DryadEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/dryad.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DryadEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/dryad.animation.json");
    }

    @Override
    public void setCustomAnimations(DryadEntity animatable, long instanceId, software.bernie.geckolib.animation.AnimationState<DryadEntity> animationState) {
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
