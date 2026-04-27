package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.VenomousPlantEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the Venomous Plant.
 * Points GeckoLib at the three asset files:
 *  - geo/entity/plant_turret.geo.json
 *  - textures/entity/plant_turret.png
 *  - animations/entity/plant_turret.animation.json
 */
public class VenomousPlantModel extends GeoModel<VenomousPlantEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/plant_turret.geo.json");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/plant_turret.png");

    private static final ResourceLocation ANIMATIONS =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/plant_turret.animation.json");

    @Override
    public ResourceLocation getModelResource(VenomousPlantEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(VenomousPlantEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(VenomousPlantEntity animatable) {
        return ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(VenomousPlantEntity animatable, long instanceId, software.bernie.geckolib.animation.AnimationState<VenomousPlantEntity> animationState) {
        // Disable custom head tracking during spawning or death animations
        if (animatable.isDeadOrDying() || animatable.tickCount <= 40) {
            return;
        }

        software.bernie.geckolib.cache.object.GeoBone stem = getAnimationProcessor().getBone("stem");

        if (stem != null) {
            software.bernie.geckolib.model.data.EntityModelData entityData = animationState.getData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA);
            
            stem.setRotX(entityData.headPitch() * ((float) Math.PI / 180f));
            stem.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180f));
        }
    }
}
