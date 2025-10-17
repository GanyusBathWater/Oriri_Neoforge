package net.ganyusbathwater.oririmod;

import com.mojang.datafixers.util.Either;
import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.potion.ModPotions;
import net.ganyusbathwater.oririmod.util.ModItemProperties;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.UUID;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = OririMod.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public class OririClient {
    public OririClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.

        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ModItemProperties.addCustomItemProperties();
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ModRarityCarrier holder)) return;

        ModRarity rarity = holder.getModRarity();
        Component line = Component.literal(rarity.displayName())
                .setStyle(Style.EMPTY.withColor(rarity.textColor()));

        event.getToolTip().add(line);
    }

    @SubscribeEvent
    public static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ModRarityCarrier holder)) return;

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

    @SubscribeEvent // on the game event bus
    public static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        // Gets the builder to add recipes to
        PotionBrewing.Builder builder = event.getBuilder();

        // Will add brewing recipes for all container potions (e.g. potion, splash potion, lingering potion)
        builder.addMix(
                // The initial potion to apply to
                Potions.AWKWARD,
                // The brewing ingredient. This is the item at the top of the brewing stand.
                ModItems.TORTURED_SOUL.asItem(),
                // The resulting potion
                ModPotions.BROKEN_POTION1
        );
        builder.addMix(ModPotions.BROKEN_POTION1, Items.GLOWSTONE_DUST, ModPotions.BROKEN_POTION2);
        builder.addMix(ModPotions.BROKEN_POTION2, Items.GLOWSTONE_DUST, ModPotions.BROKEN_POTION3);
        builder.addMix(Potions.AWKWARD, ModItems.DAMNED_SOUL.asItem(), ModPotions.STUNNED_POTION);
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        //  Check: does attacker have the effect "Charmed"?
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
    }
}