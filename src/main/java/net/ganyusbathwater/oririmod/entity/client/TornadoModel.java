package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.TornadoEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TornadoModel extends GeoModel<TornadoEntity> {

    @Override
    public ResourceLocation getModelResource(TornadoEntity object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/tornado.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TornadoEntity object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/tornado.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TornadoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/tornado.animation.json");
    }
}
