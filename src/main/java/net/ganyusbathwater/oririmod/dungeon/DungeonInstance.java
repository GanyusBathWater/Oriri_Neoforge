package net.ganyusbathwater.oririmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.ganyusbathwater.oririmod.dungeon.stage.DungeonStage;
import net.ganyusbathwater.oririmod.dungeon.stage.StageDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents an active run of a dungeon. Holds runtime state such as the assigned
 * slot origin in the dimension, participating players, and current stage progress.
 */
public class DungeonInstance {
    private final UUID instanceId;
    private final String dungeonId; // References DungeonDefinition.id
    private final BlockPos origin; // Slot origin in the dimension
    private final Set<UUID> players = new HashSet<>();
    private final Map<UUID, Integer> playerLives = new HashMap<>();
    private final Set<UUID> spectators = new HashSet<>();
    private String currentStage = "stage_0";
    private int ticksActive = 0;
    
    private boolean hasStarted = false;
    private boolean isComplete = false;
    private int ticksSinceComplete = 0;
    private boolean debugLogging = false;

    public boolean hasStarted() { return hasStarted; }
    public void setStarted(boolean s) { this.hasStarted = s; }

    // ── Stage runtime state (not persisted — rebuilt from markers on reload) ──
    private List<StageDefinition> stageDefinitions = new ArrayList<>();
    private int currentStageIndex = 0;
    @Nullable private DungeonStage activeStage = null;
    
    // ── Phase 7 Additions ──
    @Nullable private net.minecraft.world.level.levelgen.structure.BoundingBox structureBounds = null;
    @Nullable private BlockPos lootChestPos = null;
    @Nullable private String lootChestTable = null;
    @Nullable private BlockPos playerSpawnPos = null;

    public DungeonInstance(UUID instanceId, String dungeonId, BlockPos origin) {
        this.instanceId = instanceId;
        this.dungeonId = dungeonId;
        this.origin = origin;
    }

    public void addPlayer(UUID uuid) { 
        this.players.add(uuid); 
        this.playerLives.putIfAbsent(uuid, 3);
    }
    public void removePlayer(UUID uuid) { 
        this.players.remove(uuid); 
        this.playerLives.remove(uuid);
        this.spectators.remove(uuid);
    }
    public boolean hasPlayer(UUID uuid) { return this.players.contains(uuid); }
    public Set<UUID> getPlayers() { return this.players; }

    public int getPlayerLives(UUID uuid) { return this.playerLives.getOrDefault(uuid, 0); }
    public void setPlayerLives(UUID uuid, int lives) { this.playerLives.put(uuid, lives); }
    
    public void addSpectator(UUID uuid) { this.spectators.add(uuid); }
    public boolean isSpectator(UUID uuid) { return this.spectators.contains(uuid); }
    public Set<UUID> getSpectators() { return this.spectators; }
    
    public List<UUID> getAlivePlayers() {
        List<UUID> alive = new ArrayList<>();
        for (UUID p : players) {
            if (!spectators.contains(p)) alive.add(p);
        }
        return alive;
    }

    public UUID getInstanceId() { return instanceId; }
    public String getDungeonId() { return dungeonId; }
    public BlockPos getOrigin() { return origin; }

    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String stage) { this.currentStage = stage; }

    // Stage runtime accessors
    public List<StageDefinition> getStageDefinitions() { return stageDefinitions; }
    public void setStageDefinitions(List<StageDefinition> defs) { this.stageDefinitions = new ArrayList<>(defs); }
    public int getCurrentStageIndex() { return currentStageIndex; }
    public void setCurrentStageIndex(int idx) { this.currentStageIndex = idx; }
    @Nullable public DungeonStage getActiveStage() { return activeStage; }
    public void setActiveStage(@Nullable DungeonStage stage) { this.activeStage = stage; }
    public boolean hasMoreStages() { return currentStageIndex < stageDefinitions.size(); }

    public int getTicksActive() { return ticksActive; }
    public void tick() { 
        this.ticksActive++; 
        if (this.isComplete) {
            this.ticksSinceComplete++;
        }
    }
    
    public boolean isComplete() { return isComplete; }
    public void setComplete(boolean complete) { this.isComplete = complete; }
    public int getTicksSinceComplete() { return ticksSinceComplete; }
    
    public boolean isDebugLoggingEnabled() { return debugLogging; }
    public void setDebugLoggingEnabled(boolean debugLogging) { this.debugLogging = debugLogging; }
    
    @Nullable public net.minecraft.world.level.levelgen.structure.BoundingBox getStructureBounds() { return structureBounds; }
    public void setStructureBounds(@Nullable net.minecraft.world.level.levelgen.structure.BoundingBox bounds) { this.structureBounds = bounds; }
    
    @Nullable public BlockPos getLootChestPos() { return lootChestPos; }
    public void setLootChestPos(@Nullable BlockPos pos) { this.lootChestPos = pos; }
    
    @Nullable public String getLootChestTable() { return lootChestTable; }
    public void setLootChestTable(@Nullable String table) { this.lootChestTable = table; }
    
    @Nullable public BlockPos getPlayerSpawnPos() { return playerSpawnPos; }
    public void setPlayerSpawnPos(@Nullable BlockPos pos) { this.playerSpawnPos = pos; }

    public CompoundTag save(CompoundTag tag) {
        tag.putUUID("InstanceId", instanceId);
        tag.putString("DungeonId", dungeonId);
        tag.putLong("Origin", origin.asLong());
        
        ListTag playersTag = new ListTag();
        for (UUID uuid : players) {
            CompoundTag pt = new CompoundTag();
            pt.putString("UUID", uuid.toString());
            pt.putInt("Lives", playerLives.getOrDefault(uuid, 3));
            pt.putBoolean("IsSpectator", spectators.contains(uuid));
            playersTag.add(pt);
        }
        tag.put("Players", playersTag);
        
        tag.putString("CurrentStage", currentStage);
        tag.putInt("TicksActive", ticksActive);
        tag.putBoolean("HasStarted", hasStarted);
        tag.putBoolean("IsComplete", isComplete);
        tag.putInt("TicksSinceComplete", ticksSinceComplete);

        if (structureBounds != null) {
            tag.putIntArray("StructureBounds", new int[]{
                structureBounds.minX(), structureBounds.minY(), structureBounds.minZ(),
                structureBounds.maxX(), structureBounds.maxY(), structureBounds.maxZ()
            });
        }
        if (lootChestPos != null) {
            tag.putLong("LootChestPos", lootChestPos.asLong());
        }
        if (lootChestTable != null && !lootChestTable.isBlank()) {
            tag.putString("LootChestTable", lootChestTable);
        }
        if (playerSpawnPos != null) {
            tag.putLong("PlayerSpawnPos", playerSpawnPos.asLong());
        }

        return tag;
    }

    public static DungeonInstance load(CompoundTag tag) {
        UUID instanceId = tag.getUUID("InstanceId");
        String dungeonId = tag.getString("DungeonId");
        BlockPos origin = BlockPos.of(tag.getLong("Origin"));
        
        DungeonInstance instance = new DungeonInstance(instanceId, dungeonId, origin);
        
        ListTag playersTag = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < playersTag.size(); i++) {
            CompoundTag pt = playersTag.getCompound(i);
            UUID pUuid = UUID.fromString(pt.getString("UUID"));
            instance.addPlayer(pUuid);
            if (pt.contains("Lives")) {
                instance.setPlayerLives(pUuid, pt.getInt("Lives"));
            }
            if (pt.getBoolean("IsSpectator")) {
                instance.addSpectator(pUuid);
            }
        }
        
        if (tag.contains("CurrentStage")) {
            instance.setCurrentStage(tag.getString("CurrentStage"));
        }
        if (tag.contains("TicksActive")) {
            instance.ticksActive = tag.getInt("TicksActive");
        }
        if (tag.contains("IsComplete")) {
            instance.hasStarted = tag.getBoolean("HasStarted");
        instance.isComplete = tag.getBoolean("IsComplete");
        }
        if (tag.contains("TicksSinceComplete")) {
            instance.ticksSinceComplete = tag.getInt("TicksSinceComplete");
        }
        if (tag.contains("StructureBounds")) {
            int[] b = tag.getIntArray("StructureBounds");
            if (b.length == 6) {
                instance.setStructureBounds(new net.minecraft.world.level.levelgen.structure.BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]));
            }
        }
        if (tag.contains("LootChestPos")) {
            instance.setLootChestPos(BlockPos.of(tag.getLong("LootChestPos")));
        }
        if (tag.contains("LootChestTable")) {
            instance.setLootChestTable(tag.getString("LootChestTable"));
        }
        if (tag.contains("PlayerSpawnPos")) {
            instance.setPlayerSpawnPos(BlockPos.of(tag.getLong("PlayerSpawnPos")));
        }
        return instance;
    }
}
