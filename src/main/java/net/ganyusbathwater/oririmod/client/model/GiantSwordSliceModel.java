package net.ganyusbathwater.oririmod.client.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.GiantSwordSliceEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GiantSwordSliceModel extends GeoModel<GiantSwordSliceEntity> {
    
    @Override
    public ResourceLocation getModelResource(GiantSwordSliceEntity object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/giant_blade.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GiantSwordSliceEntity object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/giant_sword.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GiantSwordSliceEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/giant_blade.animation.json");
    }
}
