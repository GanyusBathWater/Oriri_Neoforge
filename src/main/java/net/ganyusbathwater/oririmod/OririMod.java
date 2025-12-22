package net.ganyusbathwater.oririmod;

import com.mojang.logging.LogUtils;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.combat.ElementInit;
import net.ganyusbathwater.oririmod.combat.ElementalDamageHandler;
import net.ganyusbathwater.oririmod.config.ManaConfig;
import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.ganyusbathwater.oririmod.enchantment.ModEnchantmentEffects;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.fluid.ModFluidTypes;
import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.ganyusbathwater.oririmod.item.ModItemGroups;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.menu.ModMenus;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.ganyusbathwater.oririmod.potion.ModPotions;
import net.ganyusbathwater.oririmod.util.TooltipHandler;
import net.ganyusbathwater.oririmod.worldgen.ModFeatures;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(OririMod.MOD_ID)
public class OririMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "oririmod";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public OririMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(NetworkHandler::register);
        ModItemGroups.registerItemGroups(modEventBus);
        ModItems.registerModItems(modEventBus);
        ModBlocks.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModEffects.registerEffects(modEventBus);
        ModPotions.registerPotions(modEventBus);
        ModEnchantmentEffects.register(modEventBus);
        ModEntities.register(modEventBus);
        modEventBus.addListener(this::addCreative);
        ModFeatures.register(modEventBus);
        ModFluids.register(modEventBus);
        ModFluidTypes.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, ManaConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ElementInit.init();
            ElementalDamageHandler.register();
            TooltipHandler.register();
        });

    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @SubscribeEvent
    public void onEffectRemoved(MobEffectEvent.Remove event) {
        LivingEntity entity = event.getEntity();

        if (event.getEffect() == ModEffects.STUNNED_EFFECT) {

            if (entity instanceof Mob mob) {
                mob.setNoAi(false);
            } else if (entity instanceof Player player) {
                player.getAbilities().setWalkingSpeed(0.1f);
                player.onUpdateAbilities();
            }
        }
    }
}
