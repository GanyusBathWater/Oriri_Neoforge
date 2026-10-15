package net.ganyusbathwater.oririmod.client.render.entity;

import net.ganyusbathwater.oririmod.client.render.entity.model.LamiaEarsModel;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticRenderer;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.LamiaEarsAnimatable;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class LamiaEarsRenderer extends AbstractPlayerCosmeticRenderer<LamiaEarsAnimatable> {

    public LamiaEarsRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LamiaEarsModel(), LamiaEarsAnimatable.INSTANCE, true);
    }
}
