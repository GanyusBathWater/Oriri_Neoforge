package net.ganyusbathwater.oririmod.client.render.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.entity.BloodCapBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class BloodCapModel extends DefaultedBlockGeoModel<BloodCapBlockEntity> {
    public BloodCapModel() {
        super(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "blood_cap"));
    }

    @Override
    protected String subtype() {
        return "block";
    }
}