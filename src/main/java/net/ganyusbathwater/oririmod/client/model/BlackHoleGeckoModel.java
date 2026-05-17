package net.ganyusbathwater.oririmod.client.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.BlackHoleEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BlackHoleGeckoModel extends GeoModel<BlackHoleEntity> {
    @Override
    public ResourceLocation getModelResource(BlackHoleEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/blackhole_entity.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BlackHoleEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/blackhole_entity.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BlackHoleEntity animatable) {
        // Return null or dummy if no animations exist
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/blackhole_entity.animation.json");
    }
}
