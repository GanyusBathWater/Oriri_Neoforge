package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.AirSliceEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AirSliceModel extends GeoModel<AirSliceEntity> {

    @Override
    public ResourceLocation getModelResource(AirSliceEntity object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/air_slice.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AirSliceEntity object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/air_slice.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AirSliceEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/air_slice.animation.json");
    }
}
