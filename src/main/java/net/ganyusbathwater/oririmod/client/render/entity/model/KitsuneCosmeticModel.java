package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.KitsuneCosmeticAnimatable;
import net.minecraft.resources.ResourceLocation;

public class KitsuneCosmeticModel extends AbstractPlayerCosmeticModel<KitsuneCosmeticAnimatable> {

    @Override
    public ResourceLocation getModelResource(KitsuneCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/kitsune_cosmetic.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KitsuneCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/kitsune_cosmetic_primary.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KitsuneCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/kitsune_cosmetic.animation.json");
    }
}
