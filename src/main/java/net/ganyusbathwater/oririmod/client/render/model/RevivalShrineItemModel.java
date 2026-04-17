package net.ganyusbathwater.oririmod.client.render.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.custom.RevivalShrineBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class RevivalShrineItemModel extends DefaultedItemGeoModel<RevivalShrineBlockItem> {
    public RevivalShrineItemModel() {
        super(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "revival_shrine"));
    }

    @Override
    protected String subtype() {
        return "block";
    }
}
