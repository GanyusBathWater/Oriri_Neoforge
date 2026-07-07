package net.ganyusbathwater.oririmod.client.render.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.entity.MoonshroomBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class MoonshroomModel extends DefaultedBlockGeoModel<MoonshroomBlockEntity> {
    public MoonshroomModel() {
        super(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "moonshroom"));
    }

    @Override
    protected String subtype() {
        return "block";
    }
}
