package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidCosmeticAnimatable;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel;
import net.minecraft.resources.ResourceLocation;

public class MermaidLandModel extends AbstractPlayerCosmeticModel<MermaidCosmeticAnimatable> {


    @Override
    public ResourceLocation getModelResource(MermaidCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/mermaid_essence_land.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MermaidCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/mermaid_fin_template.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MermaidCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/mermaid_essence.animation.json");
    }
}
