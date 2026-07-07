package net.ganyusbathwater.oririmod.client.model.item;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.custom.ForcefieldEmitterBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ForcefieldEmitterItemModel extends GeoModel<ForcefieldEmitterBlockItem> {
    private final ResourceLocation modelResource = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/block/forcefield_emitter.geo.json");
    private final ResourceLocation textureResource = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/block/forcefield_emitter.png");
    private final ResourceLocation animationResource = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/block/forcefield_emitter.animation.json");

    @Override
    public ResourceLocation getModelResource(ForcefieldEmitterBlockItem animatable) {
        return modelResource;
    }

    @Override
    public ResourceLocation getTextureResource(ForcefieldEmitterBlockItem animatable) {
        return textureResource;
    }

    @Override
    public ResourceLocation getAnimationResource(ForcefieldEmitterBlockItem animatable) {
        return animationResource;
    }
}
