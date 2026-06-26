package net.ganyusbathwater.oririmod.client.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.GiantSwordEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GiantSwordModel extends GeoModel<GiantSwordEntity> {
    
    @Override
    public ResourceLocation getModelResource(GiantSwordEntity object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/giant_sword.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GiantSwordEntity object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/giant_sword.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GiantSwordEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/giant_sword.animation.json");
    }
}
