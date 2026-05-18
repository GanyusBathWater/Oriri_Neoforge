package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.entity.custom.SporeBlossomEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SporeBlossomRenderer extends GeoEntityRenderer<SporeBlossomEntity> {
    public SporeBlossomRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SporeBlossomModel());
        this.shadowRadius = 0.5f;
    }
}
