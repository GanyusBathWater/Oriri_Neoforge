package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.MermaidEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * Dynamic GeckoLib model for the MermaidEntity.
 * Automatically switches between the water (tail) and land (legs) .geo.json
 * and .animation.json files based on the entity's current state.
 */
public class MermaidModel extends GeoModel<MermaidEntity> {

    private static final ResourceLocation MODEL_WATER =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/mermaid.geo.json");
    private static final ResourceLocation MODEL_LAND =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/mermaid_land.geo.json");

    private static final ResourceLocation ANIM_WATER =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/mermaid.animation.json");
    private static final ResourceLocation ANIM_LAND =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/mermaid_land.animation.json");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/mermaid.png");

    @Override
    public ResourceLocation getModelResource(MermaidEntity entity) {
        return entity.isInWaterState() ? MODEL_WATER : MODEL_LAND;
    }

    @Override
    public ResourceLocation getTextureResource(MermaidEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MermaidEntity entity) {
        return entity.isInWaterState() ? ANIM_WATER : ANIM_LAND;
    }

    @Override
    public void setCustomAnimations(MermaidEntity animatable, long instanceId,
                                    AnimationState<MermaidEntity> animationState) {
        GeoBone head = getAnimationProcessor().getBone("head");
        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }
    }
}
