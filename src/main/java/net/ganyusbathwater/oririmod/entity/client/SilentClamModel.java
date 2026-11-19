package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.SilentClamEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SilentClamModel extends GeoModel<SilentClamEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/silent_clam.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/silent_clam.png");
    private static final ResourceLocation ANIMATIONS =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/silent_clam.animation.json");

    @Override
    public ResourceLocation getModelResource(SilentClamEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SilentClamEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SilentClamEntity animatable) {
        return ANIMATIONS;
    }
}
