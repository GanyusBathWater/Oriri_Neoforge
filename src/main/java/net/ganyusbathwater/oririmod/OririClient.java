package net.ganyusbathwater.oririmod;

import com.mojang.datafixers.util.Either;
import net.ganyusbathwater.oririmod.client.render.world.CustomDimensionSpecialEffects;
import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.particle.ModParticles;
import net.ganyusbathwater.oririmod.particle.custom.ShiningParticle;
import net.ganyusbathwater.oririmod.particle.custom.ElderwoodsCaveParticle;
import net.ganyusbathwater.oririmod.particle.custom.ScarletCaveParticle;
import net.ganyusbathwater.oririmod.particle.custom.ElysianAbyssParticle;
import net.ganyusbathwater.oririmod.potion.ModPotions;
import net.ganyusbathwater.oririmod.util.ModItemProperties;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.ganyusbathwater.oririmod.client.tooltip.CosmicClientTooltipFactory;
import net.ganyusbathwater.oririmod.client.tooltip.CosmicTooltipSurrogate;
import net.ganyusbathwater.oririmod.client.tooltip.ModTooltipRenderTypes;
import net.ganyusbathwater.oririmod.item.component.ModDataComponents;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.UUID;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
// You can use EventBusSubscriber to automatically register all static methods
// in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public class OririClient {

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SHINING_PARTICLES.get(), ShiningParticle.Provider::new);
        event.registerSpriteSet(ModParticles.ELDERWOODS_CAVE_PARTICLE.get(), ElderwoodsCaveParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SCARLET_CAVE_PARTICLE.get(), ScarletCaveParticle.Provider::new);
        event.registerSpriteSet(ModParticles.ELYSIAN_ABYSS_PARTICLE.get(), ElysianAbyssParticle.Provider::new);
        event.registerSpriteSet(ModParticles.DEVIARTRAS_SPORE_PARTICLE.get(), net.ganyusbathwater.oririmod.particle.custom.DeviartrasSporeParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModItemProperties.addCustomItemProperties();
    }

    // ── Tooltip Component Factory ─────────────────────────────────────────────
    //
    // Fired on the MOD event bus, CLIENT dist only.
    // Associates our common-side surrogate record class with the factory method
    // that constructs the actual rendering component.
    // After this registration, whenever the game encounters a CosmicTooltipSurrogate
    // inside a tooltip component list it will call CosmicClientTooltipFactory::create.
    @SubscribeEvent
    public static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CosmicTooltipSurrogate.class, CosmicClientTooltipFactory::create);
    }

    // ── Shader Registration ───────────────────────────────────────────────────
    //
    // Fired on the MOD event bus, CLIENT dist only, during resource load.
    // The ShaderInstance is constructed by NeoForge against the JSON descriptor
    // at assets/oririmod/shaders/core/cosmic_tooltip_bg.json.
    //
    // Stage 1: the shader is registered and stored, but ModTooltipRenderTypes
    //          still uses the vanilla RenderType.gui() placeholder.
    // Stage 2: ModTooltipRenderTypes.onShadersRegistered(shader) will be called
    //          here to build the real custom CompositeRenderType.
    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            ModTooltipRenderTypes.COSMIC_BG_SHADER_LOC,
                            DefaultVertexFormat.POSITION_COLOR
                    ),
                    // Callback: build the real CompositeRenderType against the compiled shader.
                    ModTooltipRenderTypes::onShadersRegistered
            );
        } catch (java.io.IOException e) {
            OririMod.LOGGER.error("[OririMod] Failed to register cosmic_tooltip_bg shader: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Level.OVERWORLD.location(), new CustomDimensionSpecialEffects());
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ModRarityCarrier holder))
            return;

        ModRarity rarity = holder.getModRarity();
        Component line = Component.literal(rarity.displayName())
                .setStyle(Style.EMPTY.withColor(rarity.textColor()));

        event.getToolTip().add(line);
    }

    /**
     * Replaces the vanilla item-name element (position 0) with our
     * CosmicTooltipSurrogate so that CosmicTooltipComponent owns the name row.
     * Description lines stay as vanilla TextComponents and are never touched.
     */
    @SubscribeEvent
    public static void onGatherTooltipCosmic(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        var cosmicData = stack.get(ModDataComponents.COSMIC_TOOLTIP.get());
        if (cosmicData == null) return;

        List<Either<FormattedText, TooltipComponent>> list = event.getTooltipElements();
        if (list.isEmpty() || list.get(0).left().isEmpty()) return;

        // Extract the vanilla name text and wrap it in our surrogate.
        FormattedText nameText = list.get(0).left().get();
        Component nameComponent = nameText instanceof Component c
                ? c
                : Component.literal(nameText.getString());

        // Replace element 0 — our component now owns the name row.
        list.set(0, Either.right(new CosmicTooltipSurrogate(cosmicData, nameComponent)));
    }

    @SubscribeEvent
    public static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ModRarityCarrier holder))
            return;

        ModRarity rarity = holder.getModRarity();
        List<Either<FormattedText, TooltipComponent>> list = event.getTooltipElements();

        if (!list.isEmpty() && list.get(0).left().isPresent()) {
            FormattedText originalTitle = list.get(0).left().get();
            if (originalTitle instanceof Component component) {
                Component colored = component.copy()
                        .setStyle(Style.EMPTY.withColor(rarity.textColor()));
                list.set(0, Either.left(colored));
            }
        }
    }

    @SubscribeEvent
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        ItemStack stack = event.getItemStack();
        var cosmicData = stack.get(ModDataComponents.COSMIC_TOOLTIP.get());
        if (cosmicData == null) return;

        // DO NOT modify the vanilla background colors here. 
        // We want the normal Tooltip background to render as usual.

        // Calculate total tooltip dimensions dynamically, identically to GuiGraphics.renderTooltipInternal.
        int width = 0;
        int height = event.getComponents().size() == 1 ? -2 : 0;
        for (var c : event.getComponents()) {
            width = Math.max(width, c.getWidth(event.getFont()));
            height += c.getHeight();
        }

        // In the Color event, event.getX() and event.getY() are the FINAL positioned coordinates.
        int x = event.getX();
        int y = event.getY();

        net.minecraft.client.gui.GuiGraphics graphics = event.getGraphics();
        
        // 1. Draw the Vanilla Background FIRST manually
        net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil.renderTooltipBackground(
                graphics, x, y, width, height, 400,
                event.getBackgroundStart(), event.getBackgroundEnd(),
                event.getBorderStart(), event.getBorderEnd()
        );

        // 2. Force the vanilla background to render to the screen immediately.
        // This guarantees that it sits underneath our Nebula Mist.
        graphics.flush();

        // 3. Draw our Nebula Mist ON TOP of the vanilla background.
        org.joml.Matrix4f pose = graphics.pose().last().pose();
        
        org.joml.Vector4f tl = new org.joml.Vector4f(x, y, 0, 1);
        org.joml.Vector4f br = new org.joml.Vector4f(x + width, y + height, 0, 1);
        pose.transform(tl);
        pose.transform(br);
        
        ModTooltipRenderTypes.setTooltipUniforms(tl.x(), tl.y(), br.x() - tl.x(), br.y() - tl.y(), (float) cosmicData.style());
        
        com.mojang.blaze3d.vertex.VertexConsumer vc = graphics.bufferSource().getBuffer(
                ModTooltipRenderTypes.cosmicBackground());
                
        // Local Z = 400.0f to match the background's Z-level exactly. The Mist will be drawn 
        // strictly on top because it is queued after the flush.
        float z = 400.0f; 
        vc.addVertex(pose, x,         y + height, z).setColor(0xFF, 0xFF, 0xFF, 0xFF);
        vc.addVertex(pose, x + width, y + height, z).setColor(0xFF, 0xFF, 0xFF, 0xFF);
        vc.addVertex(pose, x + width, y,          z).setColor(0xFF, 0xFF, 0xFF, 0xFF);
        vc.addVertex(pose, x,         y,          z).setColor(0xFF, 0xFF, 0xFF, 0xFF);

        ModTooltipRenderTypes.pushCosmicTimeUniform();
        
        // 4. Set the vanilla background and border colors to completely transparent.
        // This effectively "cancels" the vanilla rendering of the background AFTER our Mist,
        // preventing it from drawing another 94% opaque dark box OVER our Nebula Mist.
        event.setBackgroundStart(0);
        event.setBackgroundEnd(0);
        event.setBorderStart(0);
        event.setBorderEnd(0);
    }



    @SubscribeEvent // on the game event bus
    public static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        // Gets the builder to add recipes to
        PotionBrewing.Builder builder = event.getBuilder();

        // Will add brewing recipes for all container potions (e.g. potion, splash
        // potion, lingering potion)
        builder.addMix(
                // The initial potion to apply to
                Potions.AWKWARD,
                // The brewing ingredient. This is the item at the top of the brewing stand.
                ModItems.TORTURED_SOUL.asItem(),
                // The resulting potion
                ModPotions.BROKEN_POTION1);
        builder.addMix(ModPotions.BROKEN_POTION1, Items.GLOWSTONE_DUST, ModPotions.BROKEN_POTION2);
        builder.addMix(ModPotions.BROKEN_POTION2, Items.GLOWSTONE_DUST, ModPotions.BROKEN_POTION3);
        builder.addMix(Potions.AWKWARD, ModItems.DAMNED_SOUL.asItem(), ModPotions.STUNNED_POTION);
        builder.addMix(Potions.NIGHT_VISION, ModItems.ELDERBERRY.asItem(), ModPotions.MOB_SENSE_POTION1);
        builder.addMix(ModPotions.MOB_SENSE_POTION1, Items.GLOWSTONE_DUST, ModPotions.MOB_SENSE_POTION2);
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker))
            return;

        // Check: does attacker have the effect "Charmed"?
        if (attacker.hasEffect(ModEffects.CHARMED_EFFECT)) {
            // UUID from Caster out of PersistentData
            if (attacker.getPersistentData().hasUUID("CharmCaster")) {
                UUID casterId = attacker.getPersistentData().getUUID("CharmCaster");

                // Check: does the attacker attack his own caster?
                if (event.getEntity().getUUID().equals(casterId)) {
                    event.setNewDamage(0); // Block damage
                }
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MAGIC_BOLT.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.EYE_OF_DESOLATION.get(),
                net.ganyusbathwater.oririmod.entity.client.EyeOfDesolationRenderer::new);
        event.registerEntityRenderer(ModEntities.DEVIARTRAS.get(),
                net.ganyusbathwater.oririmod.entity.client.DeviartrasRenderer::new);
        event.registerEntityRenderer(ModEntities.SPORE_BLOSSOM.get(),
                net.ganyusbathwater.oririmod.entity.client.SporeBlossomRenderer::new);
        event.registerBlockEntityRenderer(net.ganyusbathwater.oririmod.block.entity.ModBlockEntities.REVIVAL_SHRINE.get(),
                net.ganyusbathwater.oririmod.client.render.block.RevivalShrineRenderer::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(
                net.ganyusbathwater.oririmod.block.menu.ModMenuTypes.EQUINOX_TABLE_MENU.get(),
                net.ganyusbathwater.oririmod.client.screen.EquinoxTableScreen::new);
    }
}