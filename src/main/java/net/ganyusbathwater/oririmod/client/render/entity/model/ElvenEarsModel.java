package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.ElvenEarsAnimatable;
import net.minecraft.resources.ResourceLocation;

public class ElvenEarsModel extends AbstractPlayerCosmeticModel<ElvenEarsAnimatable> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/elven_ears.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/elven_ears.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/elven_ears.animation.json");

    @Override
    public ResourceLocation getModelResource(ElvenEarsAnimatable object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ElvenEarsAnimatable object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ElvenEarsAnimatable object) {
        return ANIMATION;
    }
}
