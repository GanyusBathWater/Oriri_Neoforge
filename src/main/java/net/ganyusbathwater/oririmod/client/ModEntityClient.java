package net.ganyusbathwater.oririmod.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.model.MeteorModel;
import net.ganyusbathwater.oririmod.client.model.MeteorRenderer;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class ModEntityClient {

    @net.neoforged.bus.api.SubscribeEvent
    public static void registerLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MeteorModel.LAYER_LOCATION, MeteorModel::createBodyLayer);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.METEOR.get(), MeteorRenderer::new);
    }
}