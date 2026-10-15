package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.LamiaEarsAnimatable;
import net.minecraft.resources.ResourceLocation;

public class LamiaEarsModel extends AbstractPlayerCosmeticModel<LamiaEarsAnimatable> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/lamia_essence_ears.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/lamia.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/lamia_essence_tail.animation.json");

    @Override
    public ResourceLocation getModelResource(LamiaEarsAnimatable object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(LamiaEarsAnimatable object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(LamiaEarsAnimatable object) {
        return ANIMATION;
    }
}
