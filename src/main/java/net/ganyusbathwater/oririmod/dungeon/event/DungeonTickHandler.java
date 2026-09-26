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
 *   <li>Lazily initialises stages from markers if not yet done.</li>
 *   <li>Starts the next stage if none is active.</li>
 *   <li>Ticks the active stage and advances it when complete.</li>
 * </ol>
 */
@EventBusSubscriber(modid = OririMod.MOD_ID)
public class DungeonTickHandler {

    // Timeout removed per user request

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
            String objText = instance.getLastActiveObjectiveText();
            String progText = "";
            if (instance.getActiveStage() != null && instance.getActiveStage().getState() == DungeonStage.StageState.ACTIVE) {
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

        // Timeout removed per user request
        
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
                if (nextDef.getObjectiveText() != null && !nextDef.getObjectiveText().isBlank()) {
                    instance.setLastActiveObjectiveText(nextDef.getObjectiveText());
                }
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
                if (activeStage.getDefinition().getObjectiveText() != null && !activeStage.getDefinition().getObjectiveText().isBlank()) {
                    instance.setLastActiveObjectiveText(activeStage.getDefinition().getObjectiveText());
                }
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
        
        // ── Process Death Areas ──
        processDeathAreas(level, instance);
    }

    // -------------------------------------------------------------------------
    //  Helpers
    // -------------------------------------------------------------------------

    private static void processDeathAreas(ServerLevel level, DungeonInstance instance) {
        List<StageDefinition.DeathAreaEntry> areas = new java.util.ArrayList<>(instance.getGlobalDeathAreas());
        if (instance.getActiveStage() != null) {
            areas.addAll(instance.getActiveStage().getDefinition().getDeathAreas());
        }
        if (areas.isEmpty()) return;

        for (UUID playerId : instance.getAlivePlayers()) {
            ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
            if (sp != null && !sp.isCreative() && !sp.isSpectator() && sp.isAlive()) {
                for (StageDefinition.DeathAreaEntry area : areas) {
                    if (isPlayerInDeathArea(sp, area)) {
                        sp.hurt(level.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                        break;
                    }
                }
            }
        }
    }

    private static boolean isPlayerInDeathArea(ServerPlayer sp, StageDefinition.DeathAreaEntry area) {
        net.minecraft.core.BlockPos c = area.center();
        double px = sp.getX();
        double py = sp.getY();
        double pz = sp.getZ();
        
        switch (area.shape()) {
            case BOX -> {
                String[] dims = area.dimensions().split(",");
                double w = 10, h = 10, d = 10;
                if (dims.length >= 1) try { w = Double.parseDouble(dims[0].trim()); d = w; } catch(Exception ignored){}
                if (dims.length >= 2) try { h = Double.parseDouble(dims[1].trim()); } catch(Exception ignored){}
                if (dims.length >= 3) try { d = Double.parseDouble(dims[2].trim()); } catch(Exception ignored){}
                
                double minX = c.getX() + 0.5 - (w / 2.0);
                double maxX = c.getX() + 0.5 + (w / 2.0);
                double minY = c.getY() + 0.5 - (h / 2.0);
                double maxY = c.getY() + 0.5 + (h / 2.0);
                double minZ = c.getZ() + 0.5 - (d / 2.0);
                double maxZ = c.getZ() + 0.5 + (d / 2.0);
                
                return px >= minX && px <= maxX && py >= minY && py <= maxY && pz >= minZ && pz <= maxZ;
            }
            case SPHERE -> {
                double r = 5;
                try { r = Double.parseDouble(area.dimensions().trim()); } catch(Exception ignored){}
                double dx = px - (c.getX() + 0.5);
                double dy = py - (c.getY() + 0.5);
                double dz = pz - (c.getZ() + 0.5);
                return (dx*dx + dy*dy + dz*dz) <= (r*r);
            }
            case CYLINDER -> {
                double r = 5;
                double h = 10;
                String[] dims = area.dimensions().split(",");
                if (dims.length >= 1) try { r = Double.parseDouble(dims[0].trim()); } catch(Exception ignored){}
                if (dims.length >= 2) try { h = Double.parseDouble(dims[1].trim()); } catch(Exception ignored){}
                
                double minY = c.getY() + 0.5 - (h / 2.0);
                double maxY = c.getY() + 0.5 + (h / 2.0);
                if (py < minY || py > maxY) return false;
                
                double dx = px - (c.getX() + 0.5);
                double dz = pz - (c.getZ() + 0.5);
                return (dx*dx + dz*dz) <= (r*r);
            }
        }
        return false;
    }

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
