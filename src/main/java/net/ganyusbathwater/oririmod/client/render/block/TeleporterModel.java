package net.ganyusbathwater.oririmod.client.render.block;

import net.ganyusbathwater.oririmod.block.entity.TeleporterBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TeleporterModel extends GeoModel<TeleporterBlockEntity> {
    @Override
    public ResourceLocation getModelResource(TeleporterBlockEntity object) {
        return ResourceLocation.fromNamespaceAndPath("oririmod", "geo/block/teleporter.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TeleporterBlockEntity object) {
        return ResourceLocation.fromNamespaceAndPath("oririmod", "textures/block/teleporter_main.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TeleporterBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("oririmod", "animations/block/teleporter.animation.json");
    }
}
