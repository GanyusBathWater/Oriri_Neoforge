package net.ganyusbathwater.oririmod.client.render.entity;

import net.ganyusbathwater.oririmod.client.render.entity.model.ElvenEarsModel;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticRenderer;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.ElvenEarsAnimatable;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ElvenEarsRenderer extends AbstractPlayerCosmeticRenderer<ElvenEarsAnimatable> {

    public ElvenEarsRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ElvenEarsModel(), ElvenEarsAnimatable.INSTANCE, true);
    }
}
