package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.DeviartrasEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for Deviartras.
 * Points GeckoLib at the three required asset files:
 *  - geo/entity/deviartras.geo.json
 *  - textures/entity/deviartras.png
 *  - animations/entity/deviartras.animation.json
 */
public class DeviartrasModel extends GeoModel<DeviartrasEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/deviartras.geo.json");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/deviartras.png");

    private static final ResourceLocation ANIMATIONS =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/deviartras.animation.json");

    @Override
    public ResourceLocation getModelResource(DeviartrasEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DeviartrasEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DeviartrasEntity animatable) {
        return ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(DeviartrasEntity animatable, long instanceId, software.bernie.geckolib.animation.AnimationState<DeviartrasEntity> animationState) {
        software.bernie.geckolib.cache.object.GeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            software.bernie.geckolib.model.data.EntityModelData entityData = animationState.getData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(entityData.headPitch() * net.minecraft.util.Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * net.minecraft.util.Mth.DEG_TO_RAD);
        }
    }
}
