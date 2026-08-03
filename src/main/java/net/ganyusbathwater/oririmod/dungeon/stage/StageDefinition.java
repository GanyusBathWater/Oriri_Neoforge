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

    public record SpawnEntry(ResourceLocation entityType, int count, BlockPos pos) {}
    public record SwitchEntry(String switchId, BlockPos pos) {}
    public record DoorEntry(String groupId, int requiredSwitches, BlockPos pos) {}
    public record AreaModifierEntry(String action, int radius, @Nullable ResourceLocation blockFilter, BlockPos pos) {}

    private final String stageId;
    private final StageType stageType;
    private final List<SpawnEntry> spawnEntries;
    private final List<SwitchEntry> switches;
    private final List<DoorEntry> doors;
    private final List<AreaModifierEntry> areaModifiers;
    private final int timerTicks;           // For SURVIVE_TIMER stages (in ticks)
    @Nullable private final ResourceLocation bossEntityType; // For BOSS_FIGHT stages
    @Nullable private final BlockPos bossSpawnPos;
    @Nullable private final BlockPos playerSpawnPos; // Where players are positioned at stage start

    private StageDefinition(Builder b) {
        this.stageId = b.stageId;
        this.stageType = b.stageType;
        this.spawnEntries = List.copyOf(b.spawnEntries);
        this.switches = List.copyOf(b.switches);
        this.doors = List.copyOf(b.doors);
        this.areaModifiers = List.copyOf(b.areaModifiers);
        this.timerTicks = b.timerTicks;
        this.bossEntityType = b.bossEntityType;
        this.bossSpawnPos = b.bossSpawnPos;
        this.playerSpawnPos = b.playerSpawnPos;
    }

    public String getStageId() { return stageId; }
    public StageType getStageType() { return stageType; }
    public List<SpawnEntry> getSpawnEntries() { return spawnEntries; }
    public List<SwitchEntry> getSwitches() { return switches; }
    public List<DoorEntry> getDoors() { return doors; }
    public List<AreaModifierEntry> getAreaModifiers() { return areaModifiers; }
    public int getTimerTicks() { return timerTicks; }
    @Nullable public ResourceLocation getBossEntityType() { return bossEntityType; }
    @Nullable public BlockPos getBossSpawnPos() { return bossSpawnPos; }
    @Nullable public BlockPos getPlayerSpawnPos() { return playerSpawnPos; }

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
        private final List<SwitchEntry> switches = new ArrayList<>();
        private final List<DoorEntry> doors = new ArrayList<>();
        private final List<AreaModifierEntry> areaModifiers = new ArrayList<>();
        private int timerTicks = 0;
        @Nullable private ResourceLocation bossEntityType;
        @Nullable private BlockPos bossSpawnPos;
        @Nullable private BlockPos playerSpawnPos;

        private Builder(String stageId, StageType stageType) {
            this.stageId = stageId;
            this.stageType = stageType;
        }

        public Builder addSpawn(ResourceLocation entityType, int count, BlockPos pos) {
            spawnEntries.add(new SpawnEntry(entityType, count, pos));
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

        public Builder addAreaModifier(String action, int radius, @Nullable ResourceLocation blockFilter, BlockPos pos) {
            areaModifiers.add(new AreaModifierEntry(action, radius, blockFilter, pos));
            return this;
        }

        public Builder timerTicks(int ticks) { this.timerTicks = ticks; return this; }
        public Builder boss(ResourceLocation type, BlockPos spawnPos) {
            this.bossEntityType = type;
            this.bossSpawnPos = spawnPos;
            return this;
        }
        public Builder playerSpawn(BlockPos pos) { this.playerSpawnPos = pos; return this; }

        public StageDefinition build() { return new StageDefinition(this); }
    }
}
