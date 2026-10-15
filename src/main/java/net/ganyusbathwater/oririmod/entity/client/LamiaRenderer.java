package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.entity.custom.LamiaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LamiaRenderer extends GeoEntityRenderer<LamiaEntity> {

    public LamiaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LamiaModel());
        this.shadowRadius = 0.8F;
    }
}
