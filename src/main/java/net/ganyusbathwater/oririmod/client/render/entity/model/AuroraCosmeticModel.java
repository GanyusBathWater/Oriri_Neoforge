package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.AuroraCosmeticAnimatable;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel;
import net.minecraft.resources.ResourceLocation;

public class AuroraCosmeticModel extends AbstractPlayerCosmeticModel<AuroraCosmeticAnimatable> {

    @Override
    public ResourceLocation getModelResource(AuroraCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/aurora_cosmetic.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AuroraCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/aurora_cosmetic.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AuroraCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/aurora_cosmetic.animation.json");
    }
}
