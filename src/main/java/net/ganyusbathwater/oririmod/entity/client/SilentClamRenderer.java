package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.entity.custom.SilentClamEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SilentClamRenderer extends GeoEntityRenderer<SilentClamEntity> {

    public SilentClamRenderer(EntityRendererProvider.Context context) {
        super(context, new SilentClamModel());
        this.shadowRadius = 0.35F;
    }
}
