package net.ganyusbathwater.oririmod.client.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.BlizzaEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class BlizzaModel extends GeoModel<BlizzaEntity> {

    @Override
    public ResourceLocation getModelResource(BlizzaEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/blizza.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BlizzaEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/blizza.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BlizzaEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/blizza.animation.json");
    }

    @Override
    public void setCustomAnimations(BlizzaEntity animatable, long instanceId, AnimationState<BlizzaEntity> animationState) {
        GeoBone head = getAnimationProcessor().getBone("head");
        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }
    }
}
