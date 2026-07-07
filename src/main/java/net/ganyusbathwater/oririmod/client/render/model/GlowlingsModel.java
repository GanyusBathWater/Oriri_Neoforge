package net.ganyusbathwater.oririmod.client.render.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.entity.GlowlingsBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class GlowlingsModel extends DefaultedBlockGeoModel<GlowlingsBlockEntity> {
    public GlowlingsModel() {
        super(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "glowling"));
    }

    @Override
    protected String subtype() {
        return "block";
    }
}
