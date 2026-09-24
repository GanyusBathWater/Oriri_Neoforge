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
        ServerLevel overworld = event.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld == null) return;
        
        DungeonManager manager = DungeonManager.get(overworld);
        
        // ── Party Start Countdown ──
        net.ganyusbathwater.oririmod.dungeon.party.DungeonPartyManager partyManager = net.ganyusbathwater.oririmod.dungeon.party.DungeonPartyManager.get(overworld);
        partyManager.tickParties(overworld);
        
        // Tick background generator tasks
        var activeTasks = manager.getActiveTasks();
        activeTasks.removeIf(task -> {
            task.tick();
            return task.isFinished() || task.isCancelled();
        });
        
        // Iterate every active dungeon instance across all dimensions
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimension().location().getPath().startsWith("dungeon_")) continue;
            
            String thisDimKey = level.dimension().location().toString();
            List<DungeonInstance> activeInstances = new java.util.ArrayList<>(manager.getActiveInstances().values());
            for (DungeonInstance instance : activeInstances) {
                // Only tick instances that belong to THIS dimension, not all dimensions
                var def = net.ganyusbathwater.oririmod.dungeon.DungeonDefinitionRegistry.get(instance.getDungeonId());
                if (def == null) continue;
                String instanceDimKey = def.dimension().location().toString();
                if (!instanceDimKey.equals(thisDimKey)) continue;

                tickInstance(level, manager, instance);
            }
        }
    }

    private static void tickInstance(ServerLevel level, DungeonManager manager, DungeonInstance instance) {
        if (!instance.hasStarted()) return;
        
        instance.tick();

        // ── Time Sync ──
        if (instance.getTicksActive() % 20 == 0) {
            String objText = "";
            String progText = "";
            if (instance.getActiveStage() != null) {
                if (instance.getActiveStage().getDefinition().getObjectiveText() != null) {
                    objText = instance.getActiveStage().getDefinition().getObjectiveText();
                }
                if (instance.getActiveStage().getProgressText() != null) {
                    progText = instance.getActiveStage().getProgressText();
                }
            }
            net.ganyusbathwater.oririmod.network.packet.SyncDungeonTimePayload payload = 
                new net.ganyusbathwater.oririmod.network.packet.SyncDungeonTimePayload(instance.getTicksActive(), instance.isComplete(), objText, progText);
            for (UUID playerId : new java.util.ArrayList<>(instance.getPlayers())) {
                ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
                if (sp != null) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, payload);
                }
            }
        }

        // ── 30-minute timeout ──
        if (instance.getTicksActive() >= TIMEOUT_TICKS) {
            ejectAll(level, manager, instance, "message.oririmod.dungeon.timeout");
            return;
        }
        
        // ── Completion Countdown ──
        if (instance.isComplete()) {
            if (instance.getTicksSinceComplete() % 20 == 0) {
                int secondsLeft = 10 - (instance.getTicksSinceComplete() / 20);
                if (secondsLeft > 0) {
                    Component msg = Component.translatable("message.oririmod.dungeon.closing_in", secondsLeft)
                            .withStyle(ChatFormatting.YELLOW);
                    for (UUID playerId : new java.util.ArrayList<>(instance.getPlayers())) {
                        ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
                        if (sp != null) sp.displayClientMessage(msg, true);
                    }
                }
            }
            
            if (instance.getTicksSinceComplete() >= 200) {
                ejectAll(level, manager, instance, "message.oririmod.dungeon.closed");
            }
            return; // Stop ticking stages if complete
        }

        // ── Lazy stage initialisation ──
        // Stages are built from markers the first tick after the structure is placed.
        // We check: if there are no definitions yet but the structure should be there (tick > 5),
        // scan the markers.
        if (instance.getStageDefinitions().isEmpty() && instance.getTicksActive() > 5) {
            List<StageDefinition> defs = DungeonStageManager.buildStages(level, instance);
            instance.setStageDefinitions(defs);
            
            // Restore currentStageIndex from saved currentStage string
            for (int i = 0; i < defs.size(); i++) {
                if (defs.get(i).getStageId().equals(instance.getCurrentStage())) {
                    instance.setCurrentStageIndex(i);
                    break;
                }
            }
            
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
            debugLog(level, instance, "Started Stage: " + nextDef.getStageId() + " (" + nextDef.getStageType() + ")");

            // Update the dungeon's respawn point to this stage's designated spawn point if defined.
            // Players will only be sent here if they die/respawn, avoiding forced teleports mid-dungeon.
            if (nextDef.getPlayerSpawnPos() != null) {
                instance.setPlayerSpawnPos(nextDef.getPlayerSpawnPos());
            }

            // Start the stage — this spawns enemies, locks doors, etc.
            nextStage.onStart(level, instance);

            // If it started immediately (no triggers), announce it now
            if (nextStage.getState() == DungeonStage.StageState.ACTIVE) {
                announceStage(level, instance, nextDef);
                playMusic(level, instance, nextDef);
            }
        }

        // ── Tick the active stage ──
        DungeonStage activeStage = instance.getActiveStage();
        if (activeStage != null) {
            DungeonStage.StageState prevState = activeStage.getState();
            activeStage.tick(level, instance);
            DungeonStage.StageState newState = activeStage.getState();

            // Handle transition from PENDING -> ACTIVE
            if (prevState == DungeonStage.StageState.PENDING && newState == DungeonStage.StageState.ACTIVE) {
                debugLog(level, instance, "Stage " + activeStage.getDefinition().getStageId() + " became ACTIVE (Trigger tripped).");
                announceStage(level, instance, activeStage.getDefinition());
                playMusic(level, instance, activeStage.getDefinition());
            }

            if (activeStage.isComplete()) {
                debugLog(level, instance, "Stage " + activeStage.getDefinition().getStageId() + " is COMPLETE.");
                activeStage.onComplete(level, instance);
                
                // Clear leftover mobs from this stage to improve performance
                if (activeStage.shouldClearMobsOnComplete()) {
                    var bounds = instance.getStructureBounds();
                    if (bounds != null) {
                        net.minecraft.world.phys.AABB aabb = new net.minecraft.world.phys.AABB(
                            bounds.minX(), bounds.minY(), bounds.minZ(),
                            bounds.maxX(), bounds.maxY(), bounds.maxZ()
                        ).inflate(10.0);
                        
                        for (net.minecraft.world.entity.Mob mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, aabb)) {
                            mob.discard();
                        }
                    }
                }

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

    private static void debugLog(ServerLevel level, DungeonInstance instance, String message) {
        if (instance.isDebugLoggingEnabled()) {
            net.minecraft.network.chat.Component comp = net.minecraft.network.chat.Component.literal("[Dungeon Debug] " + message).withStyle(net.minecraft.ChatFormatting.GRAY);
            for (UUID playerId : new java.util.ArrayList<>(instance.getPlayers())) {
                ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
                if (sp != null) sp.sendSystemMessage(comp);
            }
            OririMod.LOGGER.info("[Dungeon Debug] [{}] {}", instance.getDungeonId(), message);
        }
    }

    private static void announceStage(ServerLevel level, DungeonInstance instance, StageDefinition def) {
        // Disabled per user request
    }

    private static void playMusic(ServerLevel level, DungeonInstance instance, StageDefinition def) {
        var dungeonDef = net.ganyusbathwater.oririmod.dungeon.DungeonDefinitionRegistry.get(instance.getDungeonId());
        if (dungeonDef != null) {
            net.minecraft.resources.ResourceLocation track = dungeonDef.stageTrackOverrides().getOrDefault(def.getStageId(), dungeonDef.dungeonTrack());
            if (track != null) {
                var payload = new net.ganyusbathwater.oririmod.network.packet.PlayDungeonMusicPayload(track, true, true);
                for (UUID playerId : new java.util.ArrayList<>(instance.getPlayers())) {
                    ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
                    if (sp != null) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, payload);
                }
            }
        }
    }

    private static void onDungeonComplete(ServerLevel level, DungeonManager manager, DungeonInstance instance) {
        Component msg = Component.translatable("message.oririmod.dungeon.complete")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
                
        // Award Progression
        net.ganyusbathwater.oririmod.dungeon.data.PlayerDungeonData progressData = net.ganyusbathwater.oririmod.dungeon.data.PlayerDungeonData.get(level);
        for (UUID playerId : new java.util.ArrayList<>(instance.getPlayers())) {
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
        
        net.minecraft.resources.ResourceLocation lootTableId = null;
        if (instance.getLootChestTable() != null && !instance.getLootChestTable().isBlank()) {
            String table = instance.getLootChestTable();
            if (!table.contains(":")) table = "minecraft:" + table;
            lootTableId = net.minecraft.resources.ResourceLocation.tryParse(table);
        } else if (def != null && def.rewardLootTable() != null) {
            lootTableId = def.rewardLootTable();
        }

        if (lootTableId != null && instance.getLootChestPos() != null) {
            net.minecraft.core.BlockPos chestPos = instance.getLootChestPos();
            level.setBlock(chestPos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestBE) {
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootKey = 
                        net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, lootTableId);
                chestBE.setLootTable(lootKey, level.getRandom().nextLong());
            }
            OririMod.LOGGER.info("[DungeonTickHandler] Spawned Loot Chest for {} at {} with table {}", instance.getDungeonId(), chestPos, lootTableId);
        }
        
        // Revive all spectators so they can collect loot and leave normally
        for (UUID specId : new java.util.ArrayList<>(instance.getSpectators())) {
            ServerPlayer spec = level.getServer().getPlayerList().getPlayer(specId);
            if (spec != null) {
                spec.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                spec.setCamera(spec);
                spec.setHealth(spec.getMaxHealth());
                instance.setPlayerLives(specId, 1);
                
                net.minecraft.core.BlockPos respawnPos = instance.getPlayerSpawnPos();
                if (respawnPos != null) {
                    spec.teleportTo(level, respawnPos.getX() + 0.5, respawnPos.getY(), respawnPos.getZ() + 0.5, 0f, 0f);
                } else if (instance.getLootChestPos() != null) {
                    spec.teleportTo(level, instance.getLootChestPos().getX() + 0.5, instance.getLootChestPos().getY(), instance.getLootChestPos().getZ() + 0.5, 0f, 0f);
                }
                
                spec.displayClientMessage(Component.translatable("message.oririmod.dungeon.revived").withStyle(ChatFormatting.GREEN), false);
                instance.getSpectators().remove(specId);
                
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(spec, new net.ganyusbathwater.oririmod.network.packet.SyncDungeonLivesPayload(1));
            }
        }
        
        // Mark instance as complete to start the countdown
        instance.setComplete(true);
    }

    private static void ejectAll(ServerLevel level, DungeonManager manager, DungeonInstance instance, String langKey) {
        Component msg = Component.translatable(langKey).withStyle(ChatFormatting.RED);
        for (UUID playerId : new java.util.ArrayList<>(instance.getPlayers())) {
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
