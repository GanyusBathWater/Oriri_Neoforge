package net.ganyusbathwater.oririmod.events;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.config.ManaConfig;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class ServerEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        int intervalSeconds = ModManaUtil.getRegenIntervalSeconds(player);
        int regenAmount = ManaConfig.COMMON.regenAmount.get();
        int playerMax = ModManaUtil.getMaxMana(player);

        int intervalTicks = Math.max(1, intervalSeconds * 20);

        if (ModManaUtil.getMana(player) > playerMax) {
            ModManaUtil.setMana(player, playerMax);
        }

        ModManaUtil.incTickCounter(player);
        if (ModManaUtil.getTickCounter(player) >= intervalTicks) {
            ModManaUtil.setTickCounter(player, 0);
            if (ModManaUtil.getMana(player) < playerMax && regenAmount > 0) {
                ModManaUtil.addMana(player, regenAmount);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ModManaUtil.syncToClient(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ModManaUtil.syncToClient(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ModManaUtil.syncToClient(sp);
        }
    }
}