package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidCosmeticAnimatable;
import net.minecraft.resources.ResourceLocation;

public class SilentNereidTailModel extends MermaidTailModel {

    @Override
    public ResourceLocation getTextureResource(MermaidCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/silent_nereids_scale.png");
    }
}
