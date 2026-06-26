package net.ganyusbathwater.oririmod.client.render.item;

import net.ganyusbathwater.oririmod.client.render.model.ElementalChoirItemModel;
import net.ganyusbathwater.oririmod.item.custom.ElementalChoirItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ElementalChoirItemRenderer extends GeoItemRenderer<ElementalChoirItem> {
    public static net.minecraft.world.entity.player.Player currentRenderEntity;
    
    public ElementalChoirItemRenderer() {
        super(new ElementalChoirItemModel());
    }
}
