package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.RexAraneaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RexAraneaModel extends GeoModel<RexAraneaEntity> {
    @Override
    public ResourceLocation getModelResource(RexAraneaEntity object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/rex_aranea.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RexAraneaEntity object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/rex_aranea.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RexAraneaEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/rex_aranea.animation.json");
    }

    private final java.util.WeakHashMap<RexAraneaEntity, java.util.Map<String, float[]>> smoothedRotations = new java.util.WeakHashMap<>();

    @Override
    public void setCustomAnimations(RexAraneaEntity animatable, long instanceId, software.bernie.geckolib.animation.AnimationState<RexAraneaEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        software.bernie.geckolib.cache.object.GeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            software.bernie.geckolib.model.data.EntityModelData entityData = animationState.getData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(entityData.headPitch() * net.minecraft.util.Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * net.minecraft.util.Mth.DEG_TO_RAD);
        }
    }
}
