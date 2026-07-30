package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.FairyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FairyModel extends GeoModel<FairyEntity> {
    private final ResourceLocation model = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/fairy.geo.json");
    private final ResourceLocation animation = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/fairy.animation.json");
    private final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/fairy.png");

    @Override
    public ResourceLocation getModelResource(FairyEntity entity) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(FairyEntity entity) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(FairyEntity entity) {
        return animation;
    }
}
