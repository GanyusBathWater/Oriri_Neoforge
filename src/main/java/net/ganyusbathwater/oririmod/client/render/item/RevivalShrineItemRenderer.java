package net.ganyusbathwater.oririmod.client.render.item;

import net.ganyusbathwater.oririmod.client.render.model.RevivalShrineItemModel;
import net.ganyusbathwater.oririmod.item.custom.RevivalShrineBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RevivalShrineItemRenderer extends GeoItemRenderer<RevivalShrineBlockItem> {
    public RevivalShrineItemRenderer() {
        super(new RevivalShrineItemModel());
    }
}
