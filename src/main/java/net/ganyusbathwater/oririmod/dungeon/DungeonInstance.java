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
import java.util.HashSet;
import java.util.List;
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
    private String currentStage = "stage_0";
    private int ticksActive = 0;

    // ── Stage runtime state (not persisted — rebuilt from markers on reload) ──
    private List<StageDefinition> stageDefinitions = new ArrayList<>();
    private int currentStageIndex = 0;
    @Nullable private DungeonStage activeStage = null;
    
    // ── Phase 7 Additions ──
    @Nullable private net.minecraft.world.level.levelgen.structure.BoundingBox structureBounds = null;
    @Nullable private BlockPos lootChestPos = null;

    public DungeonInstance(UUID instanceId, String dungeonId, BlockPos origin) {
        this.instanceId = instanceId;
        this.dungeonId = dungeonId;
        this.origin = origin;
    }

    public void addPlayer(UUID uuid) { this.players.add(uuid); }
    public void removePlayer(UUID uuid) { this.players.remove(uuid); }
    public boolean hasPlayer(UUID uuid) { return this.players.contains(uuid); }
    public Set<UUID> getPlayers() { return this.players; }

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
    public void tick() { this.ticksActive++; }
    
    @Nullable public net.minecraft.world.level.levelgen.structure.BoundingBox getStructureBounds() { return structureBounds; }
    public void setStructureBounds(@Nullable net.minecraft.world.level.levelgen.structure.BoundingBox bounds) { this.structureBounds = bounds; }
    
    @Nullable public BlockPos getLootChestPos() { return lootChestPos; }
    public void setLootChestPos(@Nullable BlockPos pos) { this.lootChestPos = pos; }

    public CompoundTag save(CompoundTag tag) {
        tag.putUUID("InstanceId", instanceId);
        tag.putString("DungeonId", dungeonId);
        tag.putLong("Origin", origin.asLong());
        
        ListTag playersTag = new ListTag();
        for (UUID uuid : players) {
            playersTag.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("Players", playersTag);
        
        tag.putString("CurrentStage", currentStage);
        tag.putInt("TicksActive", ticksActive);

        if (structureBounds != null) {
            tag.putIntArray("StructureBounds", new int[]{
                structureBounds.minX(), structureBounds.minY(), structureBounds.minZ(),
                structureBounds.maxX(), structureBounds.maxY(), structureBounds.maxZ()
            });
        }
        if (lootChestPos != null) {
            tag.putLong("LootChestPos", lootChestPos.asLong());
        }

        return tag;
    }

    public static DungeonInstance load(CompoundTag tag) {
        UUID instanceId = tag.getUUID("InstanceId");
        String dungeonId = tag.getString("DungeonId");
        BlockPos origin = BlockPos.of(tag.getLong("Origin"));
        
        DungeonInstance instance = new DungeonInstance(instanceId, dungeonId, origin);
        
        ListTag playersTag = tag.getList("Players", Tag.TAG_STRING);
        for (int i = 0; i < playersTag.size(); i++) {
            instance.addPlayer(UUID.fromString(playersTag.getString(i)));
        }
        
        if (tag.contains("CurrentStage")) {
            instance.setCurrentStage(tag.getString("CurrentStage"));
        }
        if (tag.contains("TicksActive")) {
            instance.ticksActive = tag.getInt("TicksActive");
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
        return instance;
    }
}
