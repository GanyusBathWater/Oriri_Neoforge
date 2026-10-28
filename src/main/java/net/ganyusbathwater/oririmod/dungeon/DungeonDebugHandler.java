package net.ganyusbathwater.oririmod.dungeon;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.dungeon.stage.AbstractDungeonStage;
import net.ganyusbathwater.oririmod.dungeon.stage.DungeonStage;
import net.ganyusbathwater.oririmod.dungeon.stage.DungeonStageManager;
import net.ganyusbathwater.oririmod.dungeon.stage.KillAllEnemiesStage;
import net.ganyusbathwater.oririmod.dungeon.stage.StageDefinition;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.network.packet.DungeonSkipperActionPayload;
import net.ganyusbathwater.oririmod.network.packet.OpenDungeonSkipperScreenPayload;
import net.ganyusbathwater.oririmod.network.packet.SyncDungeonDebugZonesPayload;
import net.ganyusbathwater.oririmod.network.packet.SyncDungeonLivesPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side controller for the Dungeon Skipper debug menu and live 3D zone overlays.
 */
public final class DungeonDebugHandler {

    private DungeonDebugHandler() {}

    public static void openScreen(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        DungeonManager manager = DungeonManager.get(level);
        DungeonInstance instance = manager.getInstanceForPlayer(sp.getUUID());

        if (instance == null) {
            OpenDungeonSkipperScreenPayload payload = new OpenDungeonSkipperScreenPayload(
                    false,
                    "None",
                    0,
                    0,
                    "None",
                    "NONE",
                    "NONE",
                    List.of(),
                    false,
                    BlockPos.ZERO,
                    false,
                    false,
                    false,
                    false,
                    manager.isNextDungeonSpawnAllSecrets(),
                    0,
                    "None"
            );
            PacketDistributor.sendToPlayer(sp, payload);
            return;
        }

        ensureStagesInitialized(level, instance);

        List<StageDefinition> defs = instance.getStageDefinitions();
        List<String> allStageIds = new ArrayList<>(defs.size());
        for (StageDefinition def : defs) {
            allStageIds.add(def.getStageId());
        }

        DungeonStage active = instance.getActiveStage();
        String stageId = instance.getCurrentStage();
        String stageType = "NONE";
        String stageState = instance.isComplete() ? "COMPLETE" : "NONE";

        if (active != null) {
            stageId = active.getDefinition().getStageId();
            stageType = active.getDefinition().getStageType().name();
            stageState = active.getState().name();
        } else if (instance.getCurrentStageIndex() >= 0 && instance.getCurrentStageIndex() < defs.size()) {
            StageDefinition def = defs.get(instance.getCurrentStageIndex());
            stageId = def.getStageId();
            stageType = def.getStageType().name();
            stageState = "PENDING";
        }

        BlockPos respawnPos = getEffectiveRespawnPos(instance);
        boolean hasRespawn = respawnPos != null;

        OpenDungeonSkipperScreenPayload payload = new OpenDungeonSkipperScreenPayload(
                true,
                instance.getDungeonId(),
                instance.getCurrentStageIndex(),
                defs.size(),
                stageId,
                stageType,
                stageState,
                allStageIds,
                hasRespawn,
                hasRespawn ? respawnPos : BlockPos.ZERO,
                instance.isInfiniteLives(),
                instance.isDebugLoggingEnabled(),
                instance.isShowDebugZones(),
                instance.isSpawnAllSecrets(),
                manager.isNextDungeonSpawnAllSecrets(),
                instance.getPlayerLives(sp.getUUID()),
                buildSecretsStatusSummary(level, instance)
        );
        PacketDistributor.sendToPlayer(sp, payload);
    }

    public static void handleAction(ServerPlayer sp, DungeonSkipperActionPayload payload) {
        ServerLevel level = sp.serverLevel();
        DungeonManager manager = DungeonManager.get(level);
        DungeonInstance instance = manager.getInstanceForPlayer(sp.getUUID());
        String action = payload.action();

        if ("TOGGLE_NEXT_SECRETS".equals(action)) {
            manager.toggleNextDungeonSpawnAllSecrets();
            sp.sendSystemMessage(Component.literal(manager.isNextDungeonSpawnAllSecrets()
                    ? "§d[Dungeon Debug] Next dungeon will spawn ALL secret chests & buttons!"
                    : "§7[Dungeon Debug] Next dungeon will spawn normal secrets (max 3)."));
            openScreen(sp);
            return;
        }

        if (instance == null) {
            sp.sendSystemMessage(Component.literal("§c[Dungeon Debug] You are not in an active dungeon instance."));
            openScreen(sp);
            return;
        }

        ensureStagesInitialized(level, instance);
        List<StageDefinition> defs = instance.getStageDefinitions();

        switch (action) {
            case "SKIP_STAGE" -> {
                DungeonStage stage = instance.getActiveStage();
                if (stage instanceof AbstractDungeonStage abstractStage) {
                    String skippedId = stage.getDefinition().getStageId();
                    if (stage.getDefinition().getPlayerSpawnPos() != null) {
                        instance.setPlayerSpawnPos(stage.getDefinition().getPlayerSpawnPos());
                    }
                    abstractStage.forceComplete();
                    stage.onComplete(level, instance);
                    if (stage.shouldClearMobsOnComplete()) {
                        clearDungeonMobs(level, instance);
                    }
                    instance.setActiveStage(null);
                    int nextIdx = instance.getCurrentStageIndex() + 1;
                    instance.setCurrentStageIndex(nextIdx);
                    if (nextIdx < defs.size()) {
                        startStageAtIndex(level, instance, nextIdx);
                    }
                    sp.sendSystemMessage(Component.literal("§a[Dungeon Debug] Force-completed stage: §f" + skippedId));
                } else {
                    sp.sendSystemMessage(Component.literal("§c[Dungeon Debug] No active stage to skip."));
                }
                syncDebugZonesForInstance(level, instance);
                openScreen(sp);
            }
            case "PREVIOUS_STAGE" -> {
                int currentIdx = instance.getCurrentStageIndex();
                // If dungeon already completed, currentIdx is defs.size(); step back to last stage or previous stage
                int targetIdx = instance.isComplete() ? Math.max(0, defs.size() - 1) : currentIdx - 1;
                if (targetIdx < 0 || defs.isEmpty()) {
                    sp.sendSystemMessage(Component.literal("§c[Dungeon Debug] Already at the first stage (index 0)."));
                } else {
                    jumpToStage(level, manager, instance, targetIdx);
                    sp.sendSystemMessage(Component.literal("§e[Dungeon Debug] Rewound to Stage " + (targetIdx + 1) + "/" + defs.size()
                            + ": §f" + defs.get(targetIdx).getStageId() + " §7(Blocks, barriers & switches reverted)"));
                }
                syncDebugZonesForInstance(level, instance);
                openScreen(sp);
            }
            case "RESTART_STAGE" -> {
                if (defs.isEmpty()) {
                    sp.sendSystemMessage(Component.literal("§c[Dungeon Debug] No stages defined in this dungeon."));
                } else {
                    int targetIdx = Math.min(instance.getCurrentStageIndex(), defs.size() - 1);
                    jumpToStage(level, manager, instance, targetIdx);
                    sp.sendSystemMessage(Component.literal("§e[Dungeon Debug] Restarted Stage " + (targetIdx + 1) + "/" + defs.size()
                            + ": §f" + defs.get(targetIdx).getStageId()));
                }
                syncDebugZonesForInstance(level, instance);
                openScreen(sp);
            }
            case "JUMP_TO_STAGE" -> {
                int targetIdx = payload.targetStageIndex();
                if (targetIdx >= 0 && targetIdx < defs.size()) {
                    jumpToStage(level, manager, instance, targetIdx);
                    sp.sendSystemMessage(Component.literal("§b[Dungeon Debug] Jumped to Stage " + (targetIdx + 1) + "/" + defs.size()
                            + ": §f" + defs.get(targetIdx).getStageId()));
                }
                syncDebugZonesForInstance(level, instance);
                openScreen(sp);
            }
            case "FORCE_START_STAGE" -> {
                DungeonStage stage = instance.getActiveStage();
                if (stage instanceof AbstractDungeonStage abstractStage) {
                    if (stage.getState() == DungeonStage.StageState.PENDING) {
                        abstractStage.forceStart(level, instance);
                        StageDefinition def = stage.getDefinition();
                        if (def.getPlayerSpawnPos() != null) {
                            instance.setPlayerSpawnPos(def.getPlayerSpawnPos());
                        }
                        if (def.getObjectiveText() != null && !def.getObjectiveText().isBlank()) {
                            instance.setLastActiveObjectiveText(def.getObjectiveText());
                        }
                        sp.sendSystemMessage(Component.literal("§a[Dungeon Debug] Force-triggered pending stage: §f" + def.getStageId()));
                    } else {
                        sp.sendSystemMessage(Component.literal("§e[Dungeon Debug] Stage is already " + stage.getState() + "."));
                    }
                } else {
                    sp.sendSystemMessage(Component.literal("§c[Dungeon Debug] No pending stage to trigger."));
                }
                syncDebugZonesForInstance(level, instance);
                openScreen(sp);
            }
            case "TP_STAGE" -> {
                BlockPos targetPos = findStageTeleportPos(instance);
                if (targetPos != null) {
                    sp.teleportTo(level, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, sp.getYRot(), sp.getXRot());
                    sp.sendSystemMessage(Component.literal("§b[Dungeon Debug] Teleported to stage position: §f[" + targetPos.toShortString() + "]"));
                } else {
                    sp.sendSystemMessage(Component.literal("§c[Dungeon Debug] No spawn/trigger/marker position found for this stage."));
                }
            }
            case "TP_RESPAWN" -> {
                BlockPos respawnPos = getEffectiveRespawnPos(instance);
                if (respawnPos != null) {
                    sp.teleportTo(level, respawnPos.getX() + 0.5, respawnPos.getY(), respawnPos.getZ() + 0.5, sp.getYRot(), sp.getXRot());
                    sp.sendSystemMessage(Component.literal("§b[Dungeon Debug] Teleported to current respawn point: §f[" + respawnPos.toShortString() + "]"));
                } else {
                    sp.sendSystemMessage(Component.literal("§c[Dungeon Debug] No respawn point set for this dungeon instance."));
                }
            }
            case "TEST_RESPAWN_DEATH" -> {
                // Ensure the player is in Survival mode so the death & respawn flow can be tested authentically
                if (sp.isCreative() || sp.isSpectator()) {
                    sp.setGameMode(GameType.SURVIVAL);
                }
                sp.sendSystemMessage(Component.literal("§6[Dungeon Debug] Triggering dungeon death to test respawn point..."));
                sp.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            }
            case "TOGGLE_INFINITE_LIVES" -> {
                boolean next = !instance.isInfiniteLives();
                instance.setInfiniteLives(next);
                if (next) {
                    for (UUID pId : instance.getPlayers()) {
                        instance.setPlayerLives(pId, 3);
                        ServerPlayer member = level.getServer().getPlayerList().getPlayer(pId);
                        if (member != null) {
                            PacketDistributor.sendToPlayer(member, new SyncDungeonLivesPayload(3));
                        }
                    }
                }
                manager.setDirty();
                sp.sendSystemMessage(Component.literal(next
                        ? "§a[Dungeon Debug] Infinite Lives ENABLED (Lives locked at 3; death will never eject you)."
                        : "§c[Dungeon Debug] Infinite Lives DISABLED (Normal life consumption resumed)."));
                openScreen(sp);
            }
            case "TOGGLE_DEBUG_LOG" -> {
                boolean next = !instance.isDebugLoggingEnabled();
                instance.setDebugLoggingEnabled(next);
                manager.setDirty();
                sp.sendSystemMessage(Component.literal(next
                        ? "§a[Dungeon Debug] Live Chat Debug Logging ENABLED."
                        : "§c[Dungeon Debug] Live Chat Debug Logging DISABLED."));
                openScreen(sp);
            }
            case "TOGGLE_DEBUG_ZONES" -> {
                boolean next = !instance.isShowDebugZones();
                instance.setShowDebugZones(next);
                syncDebugZonesForInstance(level, instance);
                sp.sendSystemMessage(Component.literal(next
                        ? "§b[Dungeon Debug] 3D Debug Zones ENABLED (Rendering triggers, death areas, spawns & markers)."
                        : "§7[Dungeon Debug] 3D Debug Zones DISABLED."));
                openScreen(sp);
            }
            case "HIGHLIGHT_MOBS" -> {
                int highlighted = highlightDungeonMobs(level, instance);
                sp.sendSystemMessage(Component.literal("§e[Dungeon Debug] Applied Glowing (30s) to §f" + highlighted + " §edungeon mob(s)."));
                openScreen(sp);
            }
            case "GRANT_STAGE_KEY" -> {
                ItemStack keyStack = new ItemStack(ModItems.MANA_DESTABILIZER.get());
                if (!sp.getInventory().add(keyStack)) {
                    sp.drop(keyStack, false);
                }
                sp.sendSystemMessage(Component.literal("§b[Dungeon Debug] Granted 1x Mana Destabilizer."));
                openScreen(sp);
            }
            case "PRINT_DIAGNOSTICS" -> {
                printDiagnostics(sp, level, instance);
                openScreen(sp);
            }
            case "CHECK_SECRETS" -> {
                verifySecretsAndPrintReport(sp, level, instance);
                if (instance.isShowDebugZones()) {
                    syncDebugZonesForInstance(level, instance);
                }
                openScreen(sp);
            }
        }
    }

    private static void ensureStagesInitialized(ServerLevel level, DungeonInstance instance) {
        if (instance.getStageDefinitions().isEmpty()) {
            List<StageDefinition> defs = DungeonStageManager.buildStages(level, instance);
            instance.setStageDefinitions(defs);
        }
    }

    public static void jumpToStage(ServerLevel level, DungeonManager manager, DungeonInstance instance, int targetIndex) {
        List<StageDefinition> defs = instance.getStageDefinitions();
        if (defs.isEmpty() || targetIndex < 0 || targetIndex >= defs.size()) return;

        // 1. Clean up active stage (mobs, boss bars, infinite spawners, switches)
        if (instance.getActiveStage() != null) {
            instance.getActiveStage().cleanup(level, instance);
            instance.setActiveStage(null);
        } else {
            clearDungeonMobs(level, instance);
        }

        // 2. Revert block changes (doors, area modifiers, MagicBarrierCore walls) from current stage down to targetIndex
        int currentIdx = Math.min(instance.getCurrentStageIndex(), defs.size() - 1);
        int highestIdx = Math.max(currentIdx, targetIndex);
        for (int s = highestIdx; s >= targetIndex; s--) {
            instance.revertStageBlockChanges(level, s);
            for (StageDefinition.SwitchEntry sw : defs.get(s).getSwitches()) {
                BlockState st = level.getBlockState(sw.pos());
                if (st.hasProperty(BlockStateProperties.POWERED) && st.getValue(BlockStateProperties.POWERED)) {
                    level.setBlock(sw.pos(), st.setValue(BlockStateProperties.POWERED, false), 3);
                }
            }
        }

        // 3. Cancel completion state if rewinding from a completed dungeon
        if (instance.isComplete()) {
            instance.setComplete(false);
            instance.setBossDefeated(false);
            instance.resetTicksSinceComplete();
            if (instance.getLootChestPos() != null && level.getBlockState(instance.getLootChestPos()).is(Blocks.CHEST)) {
                level.setBlock(instance.getLootChestPos(), Blocks.AIR.defaultBlockState(), 3);
            }
        }

        // 4. Recompute playerSpawnPos from completed stages prior to targetIndex (with stage 0 as baseline fallback)
        BlockPos computedSpawn = defs.get(0).getPlayerSpawnPos() != null ? defs.get(0).getPlayerSpawnPos() : instance.getPlayerSpawnPos();
        for (int s = 0; s < targetIndex; s++) {
            if (defs.get(s).getPlayerSpawnPos() != null) {
                computedSpawn = defs.get(s).getPlayerSpawnPos();
            }
        }
        if (computedSpawn != null) {
            instance.setPlayerSpawnPos(computedSpawn);
        }

        // 5. Start target stage fresh
        instance.setCurrentStageIndex(targetIndex);
        startStageAtIndex(level, instance, targetIndex);
        manager.setDirty();
    }

    private static void startStageAtIndex(ServerLevel level, DungeonInstance instance, int stageIndex) {
        List<StageDefinition> defs = instance.getStageDefinitions();
        if (stageIndex < 0 || stageIndex >= defs.size()) return;

        StageDefinition nextDef = defs.get(stageIndex);
        DungeonStage nextStage = DungeonStageManager.createStage(nextDef);
        instance.setActiveStage(nextStage);
        instance.setCurrentStage(nextDef.getStageId());
        nextStage.onStart(level, instance);
        // Only activate this stage's respawn point if it starts immediately as ACTIVE (not PENDING)
        if (nextStage.getState() == DungeonStage.StageState.ACTIVE && nextDef.getPlayerSpawnPos() != null) {
            instance.setPlayerSpawnPos(nextDef.getPlayerSpawnPos());
        }
        if (nextStage.getState() == DungeonStage.StageState.ACTIVE && nextDef.getObjectiveText() != null && !nextDef.getObjectiveText().isBlank()) {
            instance.setLastActiveObjectiveText(nextDef.getObjectiveText());
        }
    }

    private static void clearDungeonMobs(ServerLevel level, DungeonInstance instance) {
        var bounds = instance.getStructureBounds();
        if (bounds != null) {
            AABB aabb = new AABB(
                    bounds.minX(), bounds.minY(), bounds.minZ(),
                    bounds.maxX(), bounds.maxY(), bounds.maxZ()
            ).inflate(10.0);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, aabb)) {
                mob.discard();
            }
        }
    }

    private static int highlightDungeonMobs(ServerLevel level, DungeonInstance instance) {
        int count = 0;
        if (instance.getActiveStage() instanceof KillAllEnemiesStage killStage) {
            for (UUID uuid : killStage.getSpawnedEntities()) {
                Entity ent = level.getEntity(uuid);
                if (ent instanceof LivingEntity living && living.isAlive()) {
                    living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0, false, false));
                    count++;
                }
            }
        }
        var bounds = instance.getStructureBounds();
        if (bounds != null) {
            AABB aabb = new AABB(
                    bounds.minX(), bounds.minY(), bounds.minZ(),
                    bounds.maxX(), bounds.maxY(), bounds.maxZ()
            ).inflate(10.0);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, aabb)) {
                if (mob.isAlive() && !mob.hasEffect(MobEffects.GLOWING)) {
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0, false, false));
                    count++;
                }
            }
        }
        return count;
    }

    public static BlockPos getEffectiveRespawnPos(DungeonInstance instance) {
        BlockPos respawnPos = instance.getPlayerSpawnPos();
        if (instance.getActiveStage() != null
                && instance.getActiveStage().getState() == DungeonStage.StageState.ACTIVE
                && instance.getActiveStage().getDefinition().getPlayerSpawnPos() != null) {
            respawnPos = instance.getActiveStage().getDefinition().getPlayerSpawnPos();
        }
        return respawnPos;
    }

    private static BlockPos findStageTeleportPos(DungeonInstance instance) {
        DungeonStage active = instance.getActiveStage();
        StageDefinition def = active != null ? active.getDefinition() : null;
        if (def == null && !instance.getStageDefinitions().isEmpty()) {
            int idx = Math.min(instance.getCurrentStageIndex(), instance.getStageDefinitions().size() - 1);
            if (idx >= 0) def = instance.getStageDefinitions().get(idx);
        }
        if (def != null) {
            if (def.getPlayerSpawnPos() != null) return def.getPlayerSpawnPos();
            if (!def.getTriggers().isEmpty()) return def.getTriggers().get(0).pos();
            if (!def.getGoals().isEmpty()) return def.getGoals().get(0).pos();
            if (def.getBossSpawnPos() != null) return def.getBossSpawnPos();
            if (!def.getSpawnEntries().isEmpty()) return def.getSpawnEntries().get(0).pos();
            if (!def.getSwitches().isEmpty()) return def.getSwitches().get(0).pos();
        }
        return getEffectiveRespawnPos(instance);
    }

    public static void printDiagnostics(ServerPlayer sp, ServerLevel level, DungeonInstance instance) {
        List<StageDefinition> defs = instance.getStageDefinitions();
        DungeonStage active = instance.getActiveStage();
        BlockPos respawn = getEffectiveRespawnPos(instance);

        sp.sendSystemMessage(Component.literal("§6§m━━━━━━━━━━§r §6§l[Dungeon Diagnostics]§r §6§m━━━━━━━━━━"));
        sp.sendSystemMessage(Component.literal("§7Dungeon ID: §f" + instance.getDungeonId()
                + " §7| Origin: §f[" + instance.getOrigin().toShortString() + "]"));
        sp.sendSystemMessage(Component.literal("§7Stage Index: §e" + (instance.getCurrentStageIndex() + 1) + " / " + defs.size()
                + " §7| Active Respawn: §b" + (respawn != null ? "[" + respawn.toShortString() + "]" : "NONE")));
        sp.sendSystemMessage(Component.literal("§7Secrets Summary: " + buildSecretsStatusSummary(level, instance)
                + " §7| Infinite Lives: " + (instance.isInfiniteLives() ? "§aON" : "§cOFF")));
        verifySecretsAndPrintReport(sp, level, instance);

        if (active != null) {
            StageDefinition def = active.getDefinition();
            sp.sendSystemMessage(Component.literal("§7Current Stage: §f" + def.getStageId()
                    + " §7(Type: §e" + def.getStageType() + "§7, State: §a" + active.getState() + "§7)"));

            if (!def.getTriggers().isEmpty()) {
                for (StageDefinition.TriggerEntry tr : def.getTriggers()) {
                    sp.sendSystemMessage(Component.literal(String.format("  §e• Trigger at [%s] (Radius: %d, Mode: %s)",
                            tr.pos().toShortString(), tr.radius(), tr.behavior())));
                }
            }
            if (!def.getGoals().isEmpty()) {
                for (StageDefinition.TriggerEntry goal : def.getGoals()) {
                    sp.sendSystemMessage(Component.literal(String.format("  §a• Goal Area at [%s] (Radius: %d)",
                            goal.pos().toShortString(), goal.radius())));
                }
            }
            if (!def.getDoors().isEmpty()) {
                sp.sendSystemMessage(Component.literal("  §9• Stage Barrier Doors: §f" + def.getDoors().size() + " block(s)"));
            }
            if (!def.getAreaModifiers().isEmpty()) {
                for (StageDefinition.AreaModifierEntry mod : def.getAreaModifiers()) {
                    sp.sendSystemMessage(Component.literal(String.format("  §b• Area Modifier: '%s' at [%s] (r=%d, filter=%s)",
                            mod.action(), mod.pos().toShortString(), mod.radius(), mod.blockFilter())));
                }
            }
            for (String line : active.getDiagnosticLines(level, instance)) {
                sp.sendSystemMessage(Component.literal(line));
            }
        } else {
            sp.sendSystemMessage(Component.literal("§7Current Stage: §cNone (Dungeon Complete: " + instance.isComplete() + ")"));
        }
        sp.sendSystemMessage(Component.literal("§6§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    public static String buildSecretsStatusSummary(ServerLevel level, DungeonInstance instance) {
        int totalChests = instance.getSecretChestPositions().size();
        int presentChests = 0;
        for (BlockPos pos : instance.getSecretChestPositions()) {
            if (level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.ChestBlock
                    && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity) {
                presentChests++;
            }
        }

        int totalButtons = instance.getSecretButtonPositions().size();
        int presentButtons = 0;
        boolean allPressed = totalButtons > 0 && instance.getPressedSecretButtons().size() >= totalButtons;
        for (BlockPos pos : instance.getSecretButtonPositions()) {
            if (level.isLoaded(pos)) {
                var state = level.getBlockState(pos);
                if (state.getBlock() instanceof net.minecraft.world.level.block.ButtonBlock && state.canSurvive(level, pos)) {
                    presentButtons++;
                } else if (allPressed && state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
                    presentButtons++;
                }
            }
        }

        int placementIssues = instance.getSecretPlacementIssues();
        String chestCol = (presentChests == totalChests) ? "§a" : "§c";
        String btnCol = (presentButtons == totalButtons) ? "§a" : "§c";
        String placeStr = (placementIssues == 0) ? "§aPlaced OK" : ("§c" + placementIssues + " Issue(s)");

        return String.format("Chests: %s%d/%d §7| Buttons: %s%d/%d §7(%s§7)",
                chestCol, presentChests, totalChests, btnCol, presentButtons, totalButtons, placeStr);
    }

    public static void verifySecretsAndPrintReport(ServerPlayer sp, ServerLevel level, DungeonInstance instance) {
        sp.sendSystemMessage(Component.literal("§d§m━━━━━━━━━━§r §d§l[Secret Chests & Buttons Check]§r §d§m━━━━━━━━━━"));

        // 1. Placement Check Report (captured during DungeonStageManager.buildStages)
        List<String> placementLogs = instance.getSecretPlacementLogs();
        int placementIssues = instance.getSecretPlacementIssues();
        sp.sendSystemMessage(Component.literal("§e§l1. Initial Placement Check: "
                + (placementIssues == 0 ? "§aALL PLACED WITHOUT PROBLEMS" : "§c" + placementIssues + " PLACEMENT ISSUE(S) RECORDED")));
        if (placementLogs.isEmpty()) {
            sp.sendSystemMessage(Component.literal("  §7• No secret markers were found or initialized in this dungeon."));
        } else {
            for (String logLine : placementLogs) {
                sp.sendSystemMessage(Component.literal(logLine));
            }
        }

        // 2. Live World Presence Check — Secret Chests
        int totalChests = instance.getSecretChestPositions().size();
        int okChests = 0;
        int liveIssues = 0;
        sp.sendSystemMessage(Component.literal("§e§l2. Live Secret Chest Presence (" + totalChests + " expected):"));
        if (totalChests == 0) {
            sp.sendSystemMessage(Component.literal("  §7• None spawned for this instance."));
        } else {
            int idx = 1;
            for (BlockPos pos : instance.getSecretChestPositions()) {
                if (!level.isLoaded(pos)) {
                    liveIssues++;
                    sp.sendSystemMessage(Component.literal(String.format("  §e⚠ Chest #%d at [%s]: §cCHUNK UNLOADED", idx++, pos.toShortString())));
                    continue;
                }
                var state = level.getBlockState(pos);
                var be = level.getBlockEntity(pos);
                boolean isChest = state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock;
                boolean hasBE = be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity;
                boolean blockedAbove = isChest && net.minecraft.world.level.block.ChestBlock.isChestBlockedAt(level, pos);

                List<net.minecraft.world.entity.item.ItemEntity> nearbyDrops = level.getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        new AABB(pos).inflate(3.0)
                );

                if (isChest && hasBE && !blockedAbove && nearbyDrops.isEmpty()) {
                    okChests++;
                    net.minecraft.world.level.block.entity.ChestBlockEntity chestBE = (net.minecraft.world.level.block.entity.ChestBlockEntity) be;
                    String lootState = chestBE.getLootTable() != null
                            ? "Unopened (table=" + chestBE.getLootTable().location().getPath() + ")"
                            : "Opened/Unpacked";
                    sp.sendSystemMessage(Component.literal(String.format(
                            "  §a✔ Chest #%d at [%s]: PRESENT & INTACT §7(%s)",
                            idx++, pos.toShortString(), lootState
                    )));
                } else {
                    liveIssues++;
                    String blockName = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                    StringBuilder reason = new StringBuilder();
                    if (!isChest) reason.append("MISSING CHEST (Found block: ").append(blockName).append(") ");
                    else if (!hasBE) reason.append("MISSING ChestBlockEntity! ");
                    if (blockedAbove) reason.append("BLOCKED ABOVE (Cannot open)! ");
                    if (!nearbyDrops.isEmpty()) reason.append(nearbyDrops.size()).append(" loose item entity(s) on ground nearby!");
                    sp.sendSystemMessage(Component.literal(String.format(
                            "  §c✘ Chest #%d at [%s]: %s",
                            idx++, pos.toShortString(), reason.toString().trim()
                    )));
                }
            }
        }

        // 3. Live World Presence Check — Secret Buttons
        int totalButtons = instance.getSecretButtonPositions().size();
        int okButtons = 0;
        boolean allPressed = totalButtons > 0 && instance.getPressedSecretButtons().size() >= totalButtons;
        sp.sendSystemMessage(Component.literal("§e§l3. Live Secret Button Presence (" + totalButtons + " expected, "
                + instance.getPressedSecretButtons().size() + " pressed):"));
        if (totalButtons == 0) {
            sp.sendSystemMessage(Component.literal("  §7• None spawned for this instance."));
        } else {
            int idx = 1;
            for (BlockPos pos : instance.getSecretButtonPositions()) {
                boolean pressed = instance.getPressedSecretButtons().contains(pos);
                if (!level.isLoaded(pos)) {
                    liveIssues++;
                    sp.sendSystemMessage(Component.literal(String.format("  §e⚠ Button #%d at [%s]: §cCHUNK UNLOADED", idx++, pos.toShortString())));
                    continue;
                }
                var state = level.getBlockState(pos);
                boolean isButton = state.getBlock() instanceof net.minecraft.world.level.block.ButtonBlock;
                boolean isRewardChest = allPressed && state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock;
                boolean survives = state.canSurvive(level, pos);

                if (isRewardChest) {
                    okButtons++;
                    sp.sendSystemMessage(Component.literal(String.format(
                            "  §b✔ Button #%d at [%s]: REWARD CHEST PRESENT §7(All buttons pressed)",
                            idx++, pos.toShortString()
                    )));
                } else if (isButton && survives) {
                    okButtons++;
                    String blockName = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                    sp.sendSystemMessage(Component.literal(String.format(
                            "  §a✔ Button #%d at [%s]: PRESENT & SUPPORTED §7(%s, %s)",
                            idx++, pos.toShortString(), blockName, pressed ? "§aPRESSED§7" : "§eUNPRESSED§7"
                    )));
                } else {
                    liveIssues++;
                    String blockName = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                    String problem = !isButton
                            ? "MISSING BUTTON (Found block: " + blockName + ")"
                            : "UNSUPPORTED ATTACHMENT (canSurvive=false, will pop off!)";
                    sp.sendSystemMessage(Component.literal(String.format(
                            "  §c✘ Button #%d at [%s]: %s",
                            idx++, pos.toShortString(), problem
                    )));
                }
            }
        }

        if (placementIssues == 0 && liveIssues == 0) {
            sp.sendSystemMessage(Component.literal(String.format(
                    "§a§l✔ VERDICT: All %d Secret Chest(s) and %d Secret Button(s) placed cleanly and are present in the dungeon!",
                    okChests, okButtons
            )));
        } else {
            sp.sendSystemMessage(Component.literal(String.format(
                    "§c§l⚠ VERDICT: %d placement issue(s) and %d live presence issue(s) found! (Chests OK: %d/%d, Buttons OK: %d/%d)",
                    placementIssues, liveIssues, okChests, totalChests, okButtons, totalButtons
            )));
        }
        sp.sendSystemMessage(Component.literal("§d§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    public static void syncDebugZonesForInstance(ServerLevel level, DungeonInstance instance) {
        boolean enabled = instance.isShowDebugZones();
        List<SyncDungeonDebugZonesPayload.DebugZoneEntry> zones = enabled ? buildDebugZones(level, instance) : List.of();
        SyncDungeonDebugZonesPayload payload = new SyncDungeonDebugZonesPayload(enabled, zones);
        for (UUID pId : instance.getPlayers()) {
            ServerPlayer sp = level.getServer().getPlayerList().getPlayer(pId);
            if (sp != null) {
                PacketDistributor.sendToPlayer(sp, payload);
            }
        }
    }

    private static List<SyncDungeonDebugZonesPayload.DebugZoneEntry> buildDebugZones(ServerLevel level, DungeonInstance instance) {
        List<SyncDungeonDebugZonesPayload.DebugZoneEntry> zones = new ArrayList<>();

        // 1. Current Respawn Point (Aqua)
        BlockPos respawn = getEffectiveRespawnPos(instance);
        if (respawn != null) {
            zones.add(blockBox(respawn, 0xFF00FFFF, "RESPAWN [" + respawn.toShortString() + "]"));
        }

        // 2. Global Death Areas (Crimson Red)
        for (StageDefinition.DeathAreaEntry da : instance.getGlobalDeathAreas()) {
            AABB box = computeDeathAreaBounds(da);
            zones.add(new SyncDungeonDebugZonesPayload.DebugZoneEntry(
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                    0xFFFF2222, "DEATH_AREA [GLOBAL] (" + da.shape() + ")"
            ));
        }

        // 3. Secret Buttons (Magenta/Green/Red) & Secret Chests (Gold/Red) with live presence status
        boolean allPressed = !instance.getSecretButtonPositions().isEmpty()
                && instance.getPressedSecretButtons().size() >= instance.getSecretButtonPositions().size();
        for (BlockPos btnPos : instance.getSecretButtonPositions()) {
            boolean pressed = instance.getPressedSecretButtons().contains(btnPos);
            boolean present = level.isLoaded(btnPos) && (
                    level.getBlockState(btnPos).getBlock() instanceof net.minecraft.world.level.block.ButtonBlock
                    || (allPressed && level.getBlockState(btnPos).getBlock() instanceof net.minecraft.world.level.block.ChestBlock)
            );
            int color = !present ? 0xFFFF0000 : (pressed ? 0xFF55FF55 : 0xFFFF55FF);
            String status = !present ? "[MISSING!]" : (pressed ? "[PRESSED - OK]" : "[UNPRESSED - OK]");
            zones.add(blockBox(btnPos, color, "SECRET_BUTTON " + status));
        }
        for (BlockPos chestPos : instance.getSecretChestPositions()) {
            boolean present = level.isLoaded(chestPos)
                    && level.getBlockState(chestPos).getBlock() instanceof net.minecraft.world.level.block.ChestBlock
                    && level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity;
            int color = present ? 0xFFFFAA00 : 0xFFFF0000;
            String status = present ? "[PRESENT]" : "[MISSING!]";
            zones.add(blockBox(chestPos, color, "SECRET_CHEST " + status + " [" + chestPos.toShortString() + "]"));
        }

        // 4. Final Loot Chest Position (Gold)
        if (instance.getLootChestPos() != null) {
            zones.add(blockBox(instance.getLootChestPos(), 0xFFFFAA00, "LOOT_CHEST"));
        }

        // 5. Active Stage Zones
        DungeonStage active = instance.getActiveStage();
        StageDefinition def = active != null ? active.getDefinition() : null;
        if (def == null && !instance.getStageDefinitions().isEmpty()) {
            int idx = Math.min(instance.getCurrentStageIndex(), instance.getStageDefinitions().size() - 1);
            if (idx >= 0) def = instance.getStageDefinitions().get(idx);
        }

        if (def != null) {
            String sid = def.getStageId();

            // Stage Triggers (Yellow)
            for (StageDefinition.TriggerEntry tr : def.getTriggers()) {
                BlockPos p = tr.pos();
                double r = tr.radius();
                zones.add(new SyncDungeonDebugZonesPayload.DebugZoneEntry(
                        p.getX() - r, p.getY() - r, p.getZ() - r,
                        p.getX() + r, p.getY() + r, p.getZ() + r,
                        0xFFFFEE33, "TRIGGER: " + sid + " (" + tr.behavior() + ", r=" + tr.radius() + ")"
                ));
            }

            // Goal Areas (Bright Green)
            for (StageDefinition.TriggerEntry goal : def.getGoals()) {
                BlockPos p = goal.pos();
                double r = goal.radius();
                zones.add(new SyncDungeonDebugZonesPayload.DebugZoneEntry(
                        p.getX() - r, p.getY() - r, p.getZ() - r,
                        p.getX() + r, p.getY() + r, p.getZ() + r,
                        0xFF33FF55, "GOAL_AREA: " + sid + " (r=" + goal.radius() + ")"
                ));
            }

            // Stage Death Areas (Red)
            for (StageDefinition.DeathAreaEntry da : def.getDeathAreas()) {
                AABB box = computeDeathAreaBounds(da);
                zones.add(new SyncDungeonDebugZonesPayload.DebugZoneEntry(
                        box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                        0xFFFF4444, "DEATH_AREA [" + sid + "] (" + da.shape() + ")"
                ));
            }

            // Stage Spawn Points (Orange)
            for (StageDefinition.SpawnEntry sp : def.getSpawnEntries()) {
                zones.add(blockBox(sp.pos(), 0xFFFF8800,
                        "SPAWN: " + sp.entityType().getPath() + " x" + sp.count()));
            }

            // Infinite Spawners (Deep Orange)
            for (StageDefinition.InfiniteSpawnEntry isp : def.getInfiniteSpawns()) {
                zones.add(blockBox(isp.pos(), 0xFFFF5500,
                        "INF_SPAWN: " + isp.entityType().getPath() + " (" + (isp.cooldownTicks() / 20) + "s)"));
            }

            // Boss Spawn (Dark Red / Gold)
            if (def.getBossSpawnPos() != null) {
                zones.add(blockBox(def.getBossSpawnPos(), 0xFFFF0055,
                        "BOSS_SPAWN: " + (def.getBossEntityType() != null ? def.getBossEntityType().getPath() : "none")));
            }

            // Switches (Purple)
            for (StageDefinition.SwitchEntry sw : def.getSwitches()) {
                zones.add(blockBox(sw.pos(), 0xFFBB55FF, "SWITCH: " + sw.switchId()));
            }

            // Block Matches (Light Blue)
            for (StageDefinition.BlockMatchEntry bm : def.getBlockMatches()) {
                zones.add(blockBox(bm.pos(), 0xFF55AAFF,
                        "BLOCK_MATCH: " + (bm.blockId() != null ? bm.blockId().getPath() : "?") + " [" + bm.stateData() + "]"));
            }

            // Area Modifiers (Cyan Box)
            for (StageDefinition.AreaModifierEntry mod : def.getAreaModifiers()) {
                BlockPos c = mod.pos();
                int r = mod.radius();
                zones.add(new SyncDungeonDebugZonesPayload.DebugZoneEntry(
                        c.getX() - r, c.getY() - r, c.getZ() - r,
                        c.getX() + r + 1, c.getY() + r + 1, c.getZ() + r + 1,
                        0xFF44CCFF, "AREA_MOD (" + mod.action() + ", r=" + r + ")"
                ));
            }

            // Doors (Blue)
            for (StageDefinition.DoorEntry door : def.getDoors()) {
                zones.add(blockBox(door.pos(), 0xFF3366FF, "DOOR"));
            }
        }

        return zones;
    }

    private static SyncDungeonDebugZonesPayload.DebugZoneEntry blockBox(BlockPos pos, int color, String label) {
        return new SyncDungeonDebugZonesPayload.DebugZoneEntry(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0,
                color, label
        );
    }

    private static AABB computeDeathAreaBounds(StageDefinition.DeathAreaEntry area) {
        BlockPos c = area.center();
        switch (area.shape()) {
            case BOX -> {
                String[] dims = area.dimensions().split(",");
                double w = 10, h = 10, d = 10;
                if (dims.length >= 1) try { w = Double.parseDouble(dims[0].trim()); d = w; } catch (Exception ignored) {}
                if (dims.length >= 2) try { h = Double.parseDouble(dims[1].trim()); } catch (Exception ignored) {}
                if (dims.length >= 3) try { d = Double.parseDouble(dims[2].trim()); } catch (Exception ignored) {}
                return new AABB(
                        c.getX() + 0.5 - (w / 2.0), c.getY() + 0.5 - (h / 2.0), c.getZ() + 0.5 - (d / 2.0),
                        c.getX() + 0.5 + (w / 2.0), c.getY() + 0.5 + (h / 2.0), c.getZ() + 0.5 + (d / 2.0)
                );
            }
            case SPHERE -> {
                double r = 5;
                try { r = Double.parseDouble(area.dimensions().trim()); } catch (Exception ignored) {}
                return new AABB(
                        c.getX() + 0.5 - r, c.getY() + 0.5 - r, c.getZ() + 0.5 - r,
                        c.getX() + 0.5 + r, c.getY() + 0.5 + r, c.getZ() + 0.5 + r
                );
            }
            case CYLINDER -> {
                double r = 5, h = 10;
                String[] dims = area.dimensions().split(",");
                if (dims.length >= 1) try { r = Double.parseDouble(dims[0].trim()); } catch (Exception ignored) {}
                if (dims.length >= 2) try { h = Double.parseDouble(dims[1].trim()); } catch (Exception ignored) {}
                return new AABB(
                        c.getX() + 0.5 - r, c.getY() + 0.5 - (h / 2.0), c.getZ() + 0.5 - r,
                        c.getX() + 0.5 + r, c.getY() + 0.5 + (h / 2.0), c.getZ() + 0.5 + r
                );
            }
        }
        return new AABB(c);
    }
}

