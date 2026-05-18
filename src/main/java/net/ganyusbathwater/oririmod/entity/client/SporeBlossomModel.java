package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.SporeBlossomEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SporeBlossomModel extends GeoModel<SporeBlossomEntity> {
    @Override
    public ResourceLocation getModelResource(SporeBlossomEntity object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/spore_blossom.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SporeBlossomEntity object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/spore_blossom.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SporeBlossomEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/spore_blossom.animation.json");
    }
}
