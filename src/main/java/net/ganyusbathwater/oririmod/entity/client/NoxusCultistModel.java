package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.NoxusCultistEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NoxusCultistModel extends GeoModel<NoxusCultistEntity> {
    @Override
    public ResourceLocation getModelResource(NoxusCultistEntity object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/noxus_cultist.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NoxusCultistEntity object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/noxus_cultist.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NoxusCultistEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/noxus_cultist.animation.json");
    }

    @Override
    public void setCustomAnimations(NoxusCultistEntity animatable, long instanceId, software.bernie.geckolib.animation.AnimationState<NoxusCultistEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        software.bernie.geckolib.cache.object.GeoBone head = getAnimationProcessor().getBone("head");
        if (head != null) {
            software.bernie.geckolib.model.data.EntityModelData entityData = animationState.getData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA);
            if (entityData != null) {
                head.setRotX(entityData.headPitch() * net.minecraft.util.Mth.DEG_TO_RAD);
                head.setRotY(entityData.netHeadYaw() * net.minecraft.util.Mth.DEG_TO_RAD);
            }
        }
    }
}
