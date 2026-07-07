package net.ganyusbathwater.oririmod.client.model.block;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.entity.ForcefieldEmitterBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ForcefieldEmitterModel extends GeoModel<ForcefieldEmitterBlockEntity> {
    private final ResourceLocation modelResource = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/block/forcefield_emitter.geo.json");
    private final ResourceLocation textureResource = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/block/forcefield_emitter.png");
    private final ResourceLocation animationResource = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/block/forcefield_emitter.animation.json");

    @Override
    public ResourceLocation getModelResource(ForcefieldEmitterBlockEntity animatable) {
        return modelResource;
    }

    @Override
    public ResourceLocation getTextureResource(ForcefieldEmitterBlockEntity animatable) {
        return textureResource;
    }

    @Override
    public ResourceLocation getAnimationResource(ForcefieldEmitterBlockEntity animatable) {
        return animationResource;
    }
}
