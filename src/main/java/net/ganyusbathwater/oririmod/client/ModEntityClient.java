package net.ganyusbathwater.oririmod.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.model.MeteorModel;
import net.ganyusbathwater.oririmod.client.render.MeteorRenderer;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

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