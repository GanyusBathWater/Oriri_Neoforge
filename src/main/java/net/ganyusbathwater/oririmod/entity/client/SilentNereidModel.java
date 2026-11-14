package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.SilentNereidEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * Dynamic GeckoLib model for the SilentNereidEntity.
 * Switches between water and land geometry, leveraging mermaid animations,
 * dynamic head tracking, and real-time hair physics.
 */
public class SilentNereidModel extends GeoModel<SilentNereidEntity> {

    private static final ResourceLocation MODEL_WATER =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/silent_nereids.geo.json");
    private static final ResourceLocation MODEL_LAND =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/silent_nereids_land.geo.json");

    private static final ResourceLocation ANIM_WATER =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/mermaid.animation.json");
    private static final ResourceLocation ANIM_LAND =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/mermaid_land.animation.json");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/silent_nereids_skin.png");

    @Override
    public ResourceLocation getModelResource(SilentNereidEntity entity) {
        return entity.isInWaterState() ? MODEL_WATER : MODEL_LAND;
    }

    @Override
    public ResourceLocation getTextureResource(SilentNereidEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SilentNereidEntity entity) {
        return entity.isInWaterState() ? ANIM_WATER : ANIM_LAND;
    }

    @Override
    public void setCustomAnimations(SilentNereidEntity animatable, long instanceId,
                                    AnimationState<SilentNereidEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        MermaidModel.applyMermaidCustomAnimations(
                animatable,
                name -> getAnimationProcessor().getBone(name),
                animationState
        );
    }
}

