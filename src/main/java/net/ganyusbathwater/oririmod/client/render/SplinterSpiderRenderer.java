package net.ganyusbathwater.oririmod.client.render;

import net.ganyusbathwater.oririmod.entity.custom.SplinterSpiderEntity;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import net.ganyusbathwater.oririmod.OririMod;

public class SplinterSpiderRenderer extends MobRenderer<SplinterSpiderEntity, SpiderModel<SplinterSpiderEntity>> {

    private static final ResourceLocation SPIDER_LOCATION = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/splinter_spider.png");

    public SplinterSpiderRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 0.8F);
    }

    @Override
    public ResourceLocation getTextureLocation(SplinterSpiderEntity entity) {
        return SPIDER_LOCATION;
    }
}
