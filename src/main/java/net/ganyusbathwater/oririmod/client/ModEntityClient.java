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
                                net.ganyusbathwater.oririmod.entity.client.BlizzaRenderer::new);
                event.registerEntityRenderer(ModEntities.SPLINTER_SPIDER.get(),
                                net.ganyusbathwater.oririmod.client.render.SplinterSpiderRenderer::new);
                event.registerEntityRenderer(ModEntities.MAGIC_PROJECTILE.get(),
                                net.ganyusbathwater.oririmod.client.render.MagicProjectileRenderer::new);
                event.registerEntityRenderer(ModEntities.VENOMOUS_PLANT.get(),
                                net.ganyusbathwater.oririmod.entity.client.VenomousPlantRenderer::new);
                event.registerEntityRenderer(ModEntities.THORN_PROJECTILE.get(),
                                net.ganyusbathwater.oririmod.client.render.ThornProjectileRenderer::new);

                event.registerEntityRenderer(ModEntities.TNT_ARROW.get(), context -> new net.ganyusbathwater.oririmod.client.render.CustomArrowRenderer<>(context, net.minecraft.resources.ResourceLocation.parse("oririmod:textures/entity/tnt_arrow.png")));
                event.registerEntityRenderer(ModEntities.EVENT_HORIZON_ARROW.get(), context -> new net.ganyusbathwater.oririmod.client.render.FlatItemArrowRenderer<>(context, net.minecraft.resources.ResourceLocation.parse("oririmod:textures/item/event_horizon_arrow.png")));
                event.registerEntityRenderer(ModEntities.DRAGON_IRON_ARROW.get(), context -> new net.ganyusbathwater.oririmod.client.render.CustomArrowRenderer<>(context, net.minecraft.resources.ResourceLocation.parse("oririmod:textures/entity/dragon_iron_arrow.png")));
                event.registerEntityRenderer(ModEntities.FROST_ARROW.get(), context -> new net.ganyusbathwater.oririmod.client.render.CustomArrowRenderer<>(context, net.minecraft.resources.ResourceLocation.parse("oririmod:textures/entity/frost_arrow.png")));
                event.registerEntityRenderer(ModEntities.COPPER_ARROW.get(), context -> new net.ganyusbathwater.oririmod.client.render.CustomArrowRenderer<>(context, net.minecraft.resources.ResourceLocation.parse("oririmod:textures/entity/copper_arrow.png")));
                event.registerEntityRenderer(ModEntities.SONIC_ARROW.get(), context -> new net.ganyusbathwater.oririmod.client.render.CustomArrowRenderer<>(context, net.minecraft.resources.ResourceLocation.parse("oririmod:textures/entity/sonic_arrow.png")));
                
                event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), net.ganyusbathwater.oririmod.client.render.BlackHoleRenderer::new);
                event.registerEntityRenderer(ModEntities.GIANT_SWORD.get(), net.ganyusbathwater.oririmod.client.render.GiantSwordRenderer::new);

                event.registerEntityRenderer(ModEntities.MERMAID.get(),
                                net.ganyusbathwater.oririmod.entity.client.MermaidRenderer::new);
                
                event.registerEntityRenderer(ModEntities.LOADED_BLAZE.get(),
                                net.ganyusbathwater.oririmod.entity.client.LoadedBlazeRenderer::new);
                event.registerEntityRenderer(ModEntities.REX_ARANEA.get(),
                                net.ganyusbathwater.oririmod.entity.client.RexAraneaRenderer::new);
                event.registerEntityRenderer(ModEntities.REX_ARANEA_WEB.get(),
                                net.ganyusbathwater.oririmod.client.render.RexAraneaWebRenderer::new);
                event.registerEntityRenderer(ModEntities.AETHER_CHARGE_ENTITY.get(),
                                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);

                event.registerEntityRenderer(ModEntities.MOD_BOAT.get(),
                                pContext -> new net.ganyusbathwater.oririmod.client.render.ModBoatRenderer(pContext, false));
                event.registerEntityRenderer(ModEntities.MOD_CHEST_BOAT.get(),
                                pContext -> new net.ganyusbathwater.oririmod.client.render.ModBoatRenderer(pContext, true));
        }
}