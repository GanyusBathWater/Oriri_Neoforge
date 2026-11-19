package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidHeadfinsAnimatable;
import net.minecraft.resources.ResourceLocation;

public class SilentNereidHeadfinsModel extends AbstractPlayerCosmeticModel<MermaidHeadfinsAnimatable> {

    @Override
    public ResourceLocation getModelResource(MermaidHeadfinsAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/silent_nereids_essence_land.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MermaidHeadfinsAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/silent_nereids_scale.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MermaidHeadfinsAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/mermaid_essence_headfin.animation.json");
    }
}
