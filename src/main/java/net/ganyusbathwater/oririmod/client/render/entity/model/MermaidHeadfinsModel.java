package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidHeadfinsAnimatable;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel;
import net.minecraft.resources.ResourceLocation;

public class MermaidHeadfinsModel extends AbstractPlayerCosmeticModel<MermaidHeadfinsAnimatable> {
    @Override
    public ResourceLocation getModelResource(MermaidHeadfinsAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/mermaid_essence_headfins.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MermaidHeadfinsAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/mermaid_fin_template.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MermaidHeadfinsAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/mermaid_essence_headfin.animation.json");
    }
}
