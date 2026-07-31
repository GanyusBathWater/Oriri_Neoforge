package net.ganyusbathwater.oririmod.client.render;

import net.ganyusbathwater.oririmod.item.custom.NoxusHelmetItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class NoxusHelmetRenderer extends GeoArmorRenderer<NoxusHelmetItem> {
    public NoxusHelmetRenderer() {
        super(new NoxusHelmetModel());
    }
}
