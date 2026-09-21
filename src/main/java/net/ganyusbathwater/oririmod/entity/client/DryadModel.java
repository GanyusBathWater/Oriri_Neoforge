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
}
