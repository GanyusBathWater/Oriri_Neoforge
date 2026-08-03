package net.ganyusbathwater.oririmod.dungeon.event;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.ganyusbathwater.oririmod.dungeon.DungeonManager;
import net.ganyusbathwater.oririmod.dungeon.stage.DungeonStage;
import net.ganyusbathwater.oririmod.dungeon.stage.DungeonStageManager;
import net.ganyusbathwater.oririmod.dungeon.stage.StageDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the dungeon stage lifecycle each server tick:
 * <ol>
 *   <li>Ticks the dungeon timer (30-minute timeout).</li>
 *   <li>Lazily initialises stages from markers if not yet done.</li>
 *   <li>Starts the next stage if none is active.</li>
 *   <li>Ticks the active stage and advances it when complete.</li>
 * </ol>
 */
@EventBusSubscriber(modid = OririMod.MOD_ID)
public class DungeonTickHandler {

    /** 30 minutes in ticks */
    private static final int TIMEOUT_TICKS = 30 * 60 * 20;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Iterate every active dungeon instance across all dimensions
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimension().location().getPath().startsWith("dungeon_")) continue;

            DungeonManager manager = DungeonManager.get(level);
            for (Map.Entry<UUID, DungeonInstance> entry : manager.getActiveInstances().entrySet()) {
                tickInstance(level, manager, entry.getValue());
            }
        }
    }

    private static void tickInstance(ServerLevel level, DungeonManager manager, DungeonInstance instance) {
        instance.tick();

        // ── 30-minute timeout ──
        if (instance.getTicksActive() >= TIMEOUT_TICKS) {
            ejectAll(level, manager, instance, "message.oririmod.dungeon.timeout");
            return;
        }

        // ── Lazy stage initialisation ──
        // Stages are built from markers the first tick after the structure is placed.
        // We check: if there are no definitions yet but the structure should be there (tick > 5),
        // scan the markers.
        if (instance.getStageDefinitions().isEmpty() && instance.getTicksActive() > 5) {
            List<StageDefinition> defs = DungeonStageManager.buildStages(level, instance);
            instance.setStageDefinitions(defs);
            if (defs.isEmpty()) {
                OririMod.LOGGER.warn("[DungeonTickHandler] No stage markers found for instance {} (dungeon: {})",
                        instance.getInstanceId(), instance.getDungeonId());
            }
        }

        // ── Start next stage if none active ──
        if (instance.getActiveStage() == null && instance.hasMoreStages()) {
            StageDefinition nextDef = instance.getStageDefinitions().get(instance.getCurrentStageIndex());
            DungeonStage nextStage = DungeonStageManager.createStage(nextDef);
            instance.setActiveStage(nextStage);
            instance.setCurrentStage(nextDef.getStageId());

            // Teleport players to the stage's designated spawn point if defined
            if (nextDef.getPlayerSpawnPos() != null) {
                var spawnPos = nextDef.getPlayerSpawnPos();
                for (UUID playerId : instance.getPlayers()) {
                    ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
                    if (sp != null) {
                        sp.teleportTo(level, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0f, 0f);
                    }
                }
            }

            // Start the stage — this spawns enemies, locks doors, etc.
            nextStage.onStart(level, instance);

            // Announce stage start to players
            announceStage(level, instance, nextDef);

            // Music: Check overrides or play main track
            var dungeonDef = net.ganyusbathwater.oririmod.dungeon.DungeonDefinitionRegistry.get(instance.getDungeonId());
            if (dungeonDef != null) {
                net.minecraft.resources.ResourceLocation track = dungeonDef.stageTrackOverrides().getOrDefault(nextDef.getStageId(), dungeonDef.dungeonTrack());
                if (track != null) {
                    var payload = new net.ganyusbathwater.oririmod.network.packet.PlayDungeonMusicPayload(track, true, true);
                    for (UUID playerId : instance.getPlayers()) {
                        ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
                        if (sp != null) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, payload);
                    }
                }
            }
        }

        // ── Tick the active stage ──
        DungeonStage activeStage = instance.getActiveStage();
        if (activeStage != null) {
            activeStage.tick(level, instance);

            if (activeStage.isComplete()) {
                activeStage.onComplete(level, instance);
                instance.setActiveStage(null);
                instance.setCurrentStageIndex(instance.getCurrentStageIndex() + 1);

                if (!instance.hasMoreStages()) {
                    // All stages done — dungeon complete!
                    onDungeonComplete(level, manager, instance);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    //  Helpers
    // -------------------------------------------------------------------------

    private static void announceStage(ServerLevel level, DungeonInstance instance, StageDefinition def) {
        String name = def.getStageId().replace("_", " ");
        Component msg = Component.literal("Stage: " + name)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        for (UUID playerId : instance.getPlayers()) {
            ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
            if (sp != null) sp.displayClientMessage(msg, true);
        }
    }

    private static void onDungeonComplete(ServerLevel level, DungeonManager manager, DungeonInstance instance) {
        Component msg = Component.translatable("message.oririmod.dungeon.complete")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
                
        // Award Progression
        net.ganyusbathwater.oririmod.dungeon.data.PlayerDungeonData progressData = net.ganyusbathwater.oririmod.dungeon.data.PlayerDungeonData.get(level);
        for (UUID playerId : instance.getPlayers()) {
            progressData.markCompleted(playerId, instance.getDungeonId());
            ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
            if (sp != null) {
                sp.displayClientMessage(msg, false);
                // Stop music
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new net.ganyusbathwater.oririmod.network.packet.PlayDungeonMusicPayload(null, true, false));
            }
        }
        
        // Spawn Loot Chest if pos is defined
        var def = net.ganyusbathwater.oririmod.dungeon.DungeonDefinitionRegistry.get(instance.getDungeonId());
        if (def != null && def.rewardLootTable() != null && instance.getLootChestPos() != null) {
            net.minecraft.core.BlockPos chestPos = instance.getLootChestPos();
            level.setBlock(chestPos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestBE) {
                chestBE.setLootTable(def.rewardLootTable(), level.getRandom().nextLong());
            }
            OririMod.LOGGER.info("[DungeonTickHandler] Spawned Loot Chest for {} at {}", instance.getDungeonId(), chestPos);
        }
        
        // We do NOT remove the instance here so players can loot and leave via Homeward Item.
        // Instance will be cleaned up either by timeout or when the last player leaves.
    }

    private static void ejectAll(ServerLevel level, DungeonManager manager, DungeonInstance instance, String langKey) {
        Component msg = Component.translatable(langKey).withStyle(ChatFormatting.RED);
        for (UUID playerId : instance.getPlayers()) {
            ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
            if (sp != null) {
                sp.displayClientMessage(msg, false);
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new net.ganyusbathwater.oririmod.network.packet.PlayDungeonMusicPayload(null, true, false));
                net.ganyusbathwater.oririmod.item.custom.HomewardItem.teleportHome(sp);
            }
        }
        manager.removeInstance(level.getServer(), instance.getInstanceId(), instance.getDungeonId());
    }
}
