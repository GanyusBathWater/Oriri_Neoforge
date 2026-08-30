package net.ganyusbathwater.oririmod.client.render.entity;

import net.ganyusbathwater.oririmod.entity.custom.cosmetic.AuroraCosmeticAnimatable;
import net.ganyusbathwater.oririmod.client.render.entity.model.AuroraCosmeticModel;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class AuroraCosmeticRenderer extends AbstractPlayerCosmeticRenderer<AuroraCosmeticAnimatable> {

    public AuroraCosmeticRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AuroraCosmeticModel(), AuroraCosmeticAnimatable.INSTANCE);
    }
}
