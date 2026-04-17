package net.ganyusbathwater.oririmod.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.model.IcicleModel;
import net.ganyusbathwater.oririmod.client.model.IcicleRenderer;
import net.ganyusbathwater.oririmod.client.model.MeteorModel;
import net.ganyusbathwater.oririmod.client.model.MeteorRenderer;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.entity.SwordProjectileEntity;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class ModEntityClient {

        @net.neoforged.bus.api.SubscribeEvent
        public static void registerLayers(
                        net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
                event.registerLayerDefinition(MeteorModel.LAYER_LOCATION, MeteorModel::createBodyLayer);
                event.registerLayerDefinition(IcicleModel.LAYER_LOCATION, IcicleModel::createBodyLayer);
        }

        @net.neoforged.bus.api.SubscribeEvent
        public static void registerRenderers(
                        net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
                event.registerEntityRenderer(ModEntities.METEOR.get(), MeteorRenderer::new);
                event.registerEntityRenderer(ModEntities.FIREBALL_PROJECTILE.get(),
                                net.ganyusbathwater.oririmod.client.render.FireballProjectileRenderer::new);
                event.registerEntityRenderer(ModEntities.ICICLE.get(), IcicleRenderer::new);
                event.registerEntityRenderer(ModEntities.SWORD_PROJECTILE.get(),
                                net.ganyusbathwater.oririmod.client.render.SwordProjectileRenderer::new);
                event.registerEntityRenderer(ModEntities.ROOT_VISUAL.get(),
                                net.ganyusbathwater.oririmod.client.render.RootVisualRenderer::new);
                event.registerEntityRenderer(ModEntities.DOOM_CLOCK.get(),
                                net.ganyusbathwater.oririmod.client.render.DoomClockRenderer::new);
                event.registerEntityRenderer(ModEntities.LASER_BEAM.get(),
                                net.ganyusbathwater.oririmod.client.render.LaserBeamRenderer::new);
                event.registerEntityRenderer(ModEntities.MAGIC_WAVE.get(),
                                net.ganyusbathwater.oririmod.client.render.MagicWaveRenderer::new);
                event.registerEntityRenderer(ModEntities.FIRE_ZOMBIE.get(),
                                net.ganyusbathwater.oririmod.client.render.FireZombieRenderer::new);
                event.registerEntityRenderer(ModEntities.SPORE_ZOMBIE.get(),
                                net.ganyusbathwater.oririmod.client.render.SporeZombieRenderer::new);
                event.registerEntityRenderer(ModEntities.EYE_OF_THE_STORM.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
                event.registerEntityRenderer(ModEntities.EYE_OF_DESOLATION.get(),
                                net.ganyusbathwater.oririmod.entity.client.EyeOfDesolationRenderer::new);
                event.registerEntityRenderer(ModEntities.BLIZZA.get(),
                                net.ganyusbathwater.oririmod.client.render.BlizzaRenderer::new);
                event.registerEntityRenderer(ModEntities.SPLINTER_SPIDER.get(),
                                net.ganyusbathwater.oririmod.client.render.SplinterSpiderRenderer::new);
                event.registerEntityRenderer(ModEntities.MAGIC_PROJECTILE.get(),
                                net.ganyusbathwater.oririmod.client.render.MagicProjectileRenderer::new);
        }
}