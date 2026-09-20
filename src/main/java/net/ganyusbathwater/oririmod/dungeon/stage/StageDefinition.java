package net.ganyusbathwater.oririmod.dungeon.stage;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable data snapshot parsed from DungeonMarkerEntities for one stage.
 * The DungeonStageManager builds these by scanning the level after structure placement.
 */
public final class StageDefinition {

    public record SpawnEntry(ResourceLocation entityType, boolean isTag, int count, BlockPos pos, float chance, @Nullable String keyDropItem) {}
    public record InfiniteSpawnEntry(ResourceLocation entityType, boolean isTag, int cooldownTicks, BlockPos pos, float chance, @Nullable String keyDropItem) {}
    public record SwitchEntry(String switchId, BlockPos pos) {}
    public record DoorEntry(String groupId, int requiredSwitches, BlockPos pos) {}
    public record BlockMatchEntry(net.minecraft.resources.ResourceLocation blockId, String stateData, BlockPos pos) {}
    public record AreaModifierEntry(String action, int radius, @Nullable ResourceLocation blockFilter, String extraString, BlockPos pos, float yRot) {}
    public record TriggerEntry(BlockPos pos, int radius) {}

    private final String stageId;
    private final StageType stageType;
    private final List<SpawnEntry> spawnEntries;
    private final List<InfiniteSpawnEntry> infiniteSpawns;
    private final List<SwitchEntry> switches;
    private final List<DoorEntry> doors;
    private final List<BlockMatchEntry> blockMatches;
    private final List<AreaModifierEntry> areaModifiers;
    private final List<TriggerEntry> triggers;
    private final List<TriggerEntry> goals;
    private final int timerTicks;           // For SURVIVE_TIMER stages (in ticks)
    @Nullable private final ResourceLocation bossEntityType; // For BOSS_FIGHT stages
    @Nullable private final BlockPos bossSpawnPos;
    @Nullable private final BlockPos playerSpawnPos; // Where players are positioned at stage start
    @Nullable private final String bossKeyDropItem;
    @Nullable private final String keyDropStageId; // The stage ID to assign to the dropped key

    private StageDefinition(Builder b) {
        this.stageId = b.stageId;
        this.stageType = b.stageType;
        this.spawnEntries = List.copyOf(b.spawnEntries);
        this.infiniteSpawns = List.copyOf(b.infiniteSpawns);
        this.switches = List.copyOf(b.switches);
        this.doors = List.copyOf(b.doors);
        this.blockMatches = List.copyOf(b.blockMatches);
        this.areaModifiers = List.copyOf(b.areaModifiers);
        this.triggers = List.copyOf(b.triggers);
        this.goals = List.copyOf(b.goals);
        this.timerTicks = b.timerTicks;
        this.bossEntityType = b.bossEntityType;
        this.bossSpawnPos = b.bossSpawnPos;
        this.playerSpawnPos = b.playerSpawnPos;
        this.bossKeyDropItem = b.bossKeyDropItem;
        this.keyDropStageId = b.keyDropStageId;
    }

    public String getStageId() { return stageId; }
    public StageType getStageType() { return stageType; }
    public List<SpawnEntry> getSpawnEntries() { return spawnEntries; }
    public List<InfiniteSpawnEntry> getInfiniteSpawns() { return infiniteSpawns; }
    public List<SwitchEntry> getSwitches() { return switches; }
    public List<DoorEntry> getDoors() { return doors; }
    public List<BlockMatchEntry> getBlockMatches() { return blockMatches; }
    public List<AreaModifierEntry> getAreaModifiers() { return areaModifiers; }
    public List<TriggerEntry> getTriggers() { return triggers; }
    public List<TriggerEntry> getGoals() { return goals; }
    public int getTimerTicks() { return timerTicks; }
    @Nullable public ResourceLocation getBossEntityType() { return bossEntityType; }
    @Nullable public BlockPos getBossSpawnPos() { return bossSpawnPos; }
    @Nullable public BlockPos getPlayerSpawnPos() { return playerSpawnPos; }
    @Nullable public String getBossKeyDropItem() { return bossKeyDropItem; }
    @Nullable public String getKeyDropStageId() { return keyDropStageId; }

    // -------------------------------------------------------------------------
    //  Builder
    // -------------------------------------------------------------------------

    public static Builder builder(String stageId, StageType stageType) {
        return new Builder(stageId, stageType);
    }

    public static final class Builder {
        private final String stageId;
        private final StageType stageType;
        private final List<SpawnEntry> spawnEntries = new ArrayList<>();
        private final List<InfiniteSpawnEntry> infiniteSpawns = new ArrayList<>();
        private final List<SwitchEntry> switches = new ArrayList<>();
        private final List<DoorEntry> doors = new ArrayList<>();
        private final List<BlockMatchEntry> blockMatches = new ArrayList<>();
        private final List<AreaModifierEntry> areaModifiers = new ArrayList<>();
        private final List<TriggerEntry> triggers = new ArrayList<>();
        private final List<TriggerEntry> goals = new ArrayList<>();
        private int timerTicks = 0;
        @Nullable private ResourceLocation bossEntityType;
        @Nullable private BlockPos bossSpawnPos;
        @Nullable private BlockPos playerSpawnPos;
        @Nullable private String bossKeyDropItem;
        @Nullable private String keyDropStageId;

        private Builder(String stageId, StageType stageType) {
            this.stageId = stageId;
            this.stageType = stageType;
        }

        public Builder addSpawn(ResourceLocation entityType, boolean isTag, int count, BlockPos pos, float chance, @Nullable String keyDropItem) {
            spawnEntries.add(new SpawnEntry(entityType, isTag, count, pos, chance, keyDropItem));
            return this;
        }

        public Builder addInfiniteSpawn(ResourceLocation entityType, boolean isTag, int cooldownTicks, BlockPos pos, float chance, @Nullable String keyDropItem) {
            infiniteSpawns.add(new InfiniteSpawnEntry(entityType, isTag, cooldownTicks, pos, chance, keyDropItem));
            return this;
        }

        public Builder addSwitch(String switchId, BlockPos pos) {
            switches.add(new SwitchEntry(switchId, pos));
            return this;
        }

        public Builder addDoor(String groupId, int required, BlockPos pos) {
            doors.add(new DoorEntry(groupId, required, pos));
            return this;
        }

        public Builder addBlockMatch(net.minecraft.resources.ResourceLocation blockId, String stateData, BlockPos pos) {
            blockMatches.add(new BlockMatchEntry(blockId, stateData, pos));
            return this;
        }

        public Builder addAreaModifier(String action, int radius, @Nullable ResourceLocation blockFilter, String extraString, BlockPos pos, float yRot) {
            areaModifiers.add(new AreaModifierEntry(action, radius, blockFilter, extraString, pos, yRot));
            return this;
        }

        public Builder addTrigger(BlockPos pos, int radius) {
            triggers.add(new TriggerEntry(pos, radius));
            return this;
        }

        public Builder addGoal(BlockPos pos, int radius) {
            goals.add(new TriggerEntry(pos, radius));
            return this;
        }

        public Builder timerTicks(int ticks) { this.timerTicks = ticks; return this; }
        public Builder boss(ResourceLocation type, BlockPos spawnPos, @Nullable String keyDropItem) {
            this.bossEntityType = type;
            this.bossSpawnPos = spawnPos;
            this.bossKeyDropItem = keyDropItem;
            return this;
        }
        public Builder playerSpawn(BlockPos pos) { this.playerSpawnPos = pos; return this; }
        public Builder keyDropStageId(String id) { this.keyDropStageId = id; return this; }

        public StageDefinition build() { return new StageDefinition(this); }
    }
}
