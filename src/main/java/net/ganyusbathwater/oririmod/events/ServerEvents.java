package net.ganyusbathwater.oririmod.events;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.config.ManaConfig;
import net.ganyusbathwater.oririmod.config.WorldEventConfig;
import net.ganyusbathwater.oririmod.events.world.WorldEventManager;
import net.ganyusbathwater.oririmod.events.world.WorldEventType;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class ServerEvents {

    private static final int NIGHT_START = 13000;
    private static final int DAY_START = 23500; // Kurz vor Tag 0

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
            // Send current world event state to the player who just logged in
            WorldEventManager manager = WorldEventManager.get(sp.serverLevel());
            NetworkHandler.sendWorldEventToPlayer(sp, manager.getActiveEvent(), manager.getTicksRemaining(), manager.getEventDuration());
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
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level == null) return;

        WorldEventManager manager = WorldEventManager.get(level);
        WorldEventManager.tick();

        long timeOfDay = level.getDayTime() % 24000;

        // Trigger für Nacht-Events (Blood Moon, Green Moon)
        if (timeOfDay == NIGHT_START) {
            if (level.random.nextFloat() < WorldEventConfig.COMMON.nightEventChance.get()) {
                int duration = WorldEventConfig.COMMON.nightEventDuration.get();
                if (level.random.nextBoolean()) {
                    manager.startEvent(WorldEventType.BLOOD_MOON, duration, level);
                } else {
                    manager.startEvent(WorldEventType.GREEN_MOON, duration, level);
                }
            }
        }

        // Trigger für Tag-Events (Eclipse)
        if (timeOfDay == DAY_START) {
            if (level.random.nextFloat() < WorldEventConfig.COMMON.eclipseChance.get()) {
                int duration = WorldEventConfig.COMMON.eclipseDuration.get();
                manager.startEvent(WorldEventType.ECLIPSE, duration, level);
            }
        }
    }

    // Effekt: Erhöhte Spawnrate für Blutmond
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Level level = event.getLevel().getLevel();
        if (level instanceof ServerLevel serverLevel && event.getEntity() instanceof Monster monster) {
            WorldEventManager manager = WorldEventManager.get(serverLevel);
            if (manager.isEventActive(WorldEventType.BLOOD_MOON)) {
                // Verdoppelt die Chance, indem ein zweites Monster mit 50% Wahrscheinlichkeit gespawnt wird
                if (serverLevel.random.nextFloat() < 0.5F) {
                    Monster extraMonster = (Monster) monster.getType().create(serverLevel);
                    if (extraMonster != null) {
                        extraMonster.copyPosition(monster);
                        serverLevel.addFreshEntity(extraMonster);
                    }
                }
            }
        }
    }

    // Effekt: Erhöhter Schaden bei Sonnenfinsternis
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof Monster && sourceEntity.level() instanceof ServerLevel serverLevel) {
            WorldEventManager manager = WorldEventManager.get(serverLevel);
            if (manager.isEventActive(WorldEventType.ECLIPSE)) {
                event.setNewDamage(event.getOriginalDamage() * 1.25F);
            }
        }
    }
}