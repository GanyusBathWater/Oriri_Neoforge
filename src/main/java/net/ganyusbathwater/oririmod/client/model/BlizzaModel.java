package net.ganyusbathwater.oririmod.client.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.BlizzaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

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
}
