package net.ganyusbathwater.oririmod.client.render.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.entity.RevivalShrineBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class RevivalShrineModel extends DefaultedBlockGeoModel<RevivalShrineBlockEntity> {
    public RevivalShrineModel() {
        super(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "revival_shrine"));
    }

    @Override
    protected String subtype() {
        return "block";
    }
}
