package net.ganyusbathwater.oririmod.client.render.entity;

import net.ganyusbathwater.oririmod.client.render.entity.model.LamiaTailModel;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticRenderer;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.LamiaTailAnimatable;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class LamiaTailRenderer extends AbstractPlayerCosmeticRenderer<LamiaTailAnimatable> {

    public LamiaTailRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LamiaTailModel(), LamiaTailAnimatable.INSTANCE, true);
    }
}
