package net.ganyusbathwater.oririmod.dungeon.stage;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.ganyusbathwater.oririmod.dungeon.dimension.DungeonInstanceGrid;
import net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * Scans DungeonMarkerEntities in the allocated grid slot and builds
 * a list of StageDefinitions, one per unique stage_id found.
 *
 * Called once after the dungeon structure is placed (in DungeonManager.startDungeon).
 */
public class DungeonStageManager {

    // -------------------------------------------------------------------------
    //  Marker roles
    // -------------------------------------------------------------------------
    public static final String ROLE_SPAWN_POINT   = "SPAWN_POINT";
    public static final String ROLE_BOSS_SPAWN    = "BOSS_SPAWN";
    public static final String ROLE_SWITCH        = "SWITCH";
    public static final String ROLE_DOOR          = "DOOR";
    public static final String ROLE_AREA_MODIFIER = "AREA_MODIFIER";
    public static final String ROLE_STAGE_TRIGGER = "STAGE_TRIGGER";
    public static final String ROLE_PLAYER_SPAWN  = "PLAYER_SPAWN";
    public static final String ROLE_BOUNDS_MIN    = "BOUNDS_MIN";
    public static final String ROLE_BOUNDS_MAX    = "BOUNDS_MAX";
    public static final String ROLE_INFINITE_SPAWNER = "INFINITE_SPAWNER";
    public static final String ROLE_LOOT_CHEST    = "LOOT_CHEST";

    /**
     * Scans all DungeonMarkerEntity instances within the 2048×2048 grid slot
     * of this instance and returns an ordered list of StageDefinitions.
     *
     * Stages are sorted by their stage_id string (stage_0 < stage_1 < stage_2 …).
     */
    public static List<StageDefinition> buildStages(ServerLevel level, DungeonInstance instance) {
        BlockPos origin = instance.getOrigin();
        int slotSize = DungeonInstanceGrid.SLOT_SIZE;

        // Search box covering the entire 2048×2048 slot, full vertical range
        AABB searchBox = new AABB(
                origin.getX(),       level.getMinBuildHeight(),       origin.getZ(),
                origin.getX() + slotSize, level.getMaxBuildHeight(), origin.getZ() + slotSize
        );

        List<DungeonMarkerEntity> markers = level.getEntitiesOfClass(DungeonMarkerEntity.class, searchBox);

        // Group markers by stage_id
        Map<String, List<DungeonMarkerEntity>> byStage = new LinkedHashMap<>();
        for (DungeonMarkerEntity marker : markers) {
            if (ROLE_LOOT_CHEST.equalsIgnoreCase(marker.getRole())) {
                instance.setLootChestPos(marker.blockPosition());
                continue;
            }
            if (ROLE_PLAYER_SPAWN.equalsIgnoreCase(marker.getRole())) {
                instance.setPlayerSpawnPos(marker.blockPosition());
                continue;
            }
            String stageId = marker.getStageId();
            if (stageId.isBlank()) continue;
            byStage.computeIfAbsent(stageId, k -> new ArrayList<>()).add(marker);
        }

        // Build StageDefinitions, sorted by stage ID
        List<String> sortedIds = new ArrayList<>(byStage.keySet());
        Collections.sort(sortedIds);

        List<StageDefinition> definitions = new ArrayList<>();
        for (String stageId : sortedIds) {
            StageDefinition def = buildStageDefinition(stageId, byStage.get(stageId));
            if (def != null) definitions.add(def);
        }
        return definitions;
    }

    private static StageDefinition buildStageDefinition(String stageId, List<DungeonMarkerEntity> markers) {
        // Determine stage type from any marker in this group (they should all agree)
        String typeStr = markers.stream()
                .filter(m -> !m.getStageType().isBlank())
                .map(DungeonMarkerEntity::getStageType)
                .findFirst()
                .orElse("KILL_ALL_ENEMIES");

        StageType stageType = StageType.fromString(typeStr);
        StageDefinition.Builder builder = StageDefinition.builder(stageId, stageType);

        for (DungeonMarkerEntity marker : markers) {
            BlockPos pos = marker.blockPosition();
            String role = marker.getRole().toUpperCase();
            var extra = marker.getExtraData();
            
            String loot = extra.getString(DungeonMarkerEntity.TAG_LOOT_TABLE);
            if (!loot.isBlank()) {
                builder.keyDropStageId(loot);
            }

            switch (role) {
                case ROLE_SPAWN_POINT -> {
                    String entityTypeStr = extra.getString(DungeonMarkerEntity.TAG_ENEMY_TYPE);
                    if (!entityTypeStr.isBlank()) {
                        boolean isTag = entityTypeStr.startsWith("#");
                        String cleanType = entityTypeStr.replace("#", "").toLowerCase().replace(' ', '_');
                        if (!cleanType.contains(":")) cleanType = (isTag ? "oririmod:" : "minecraft:") + cleanType;
                        ResourceLocation entityType = ResourceLocation.tryParse(cleanType);
                        if (entityType == null) continue;
                        int count = extra.contains(DungeonMarkerEntity.TAG_COUNT)
                                ? extra.getInt(DungeonMarkerEntity.TAG_COUNT) : 1;
                        float chance = extra.contains(DungeonMarkerEntity.TAG_SPAWN_CHANCE)
                                ? extra.getFloat(DungeonMarkerEntity.TAG_SPAWN_CHANCE) : 1.0f;
                        builder.addSpawn(entityType, isTag, count, pos, chance);
                    }
                }
                case ROLE_INFINITE_SPAWNER -> {
                    String entityTypeStr = extra.getString(DungeonMarkerEntity.TAG_ENEMY_TYPE);
                    if (!entityTypeStr.isBlank()) {
                        boolean isTag = entityTypeStr.startsWith("#");
                        String cleanType = entityTypeStr.replace("#", "").toLowerCase().replace(' ', '_');
                        if (!cleanType.contains(":")) cleanType = (isTag ? "oririmod:" : "minecraft:") + cleanType;
                        ResourceLocation entityType = ResourceLocation.tryParse(cleanType);
                        if (entityType == null) continue;
                        int cooldownSecs = extra.contains(DungeonMarkerEntity.TAG_COUNT)
                                ? extra.getInt(DungeonMarkerEntity.TAG_COUNT) : 5;
                        if (cooldownSecs <= 0) cooldownSecs = 5;
                        float chance = extra.contains(DungeonMarkerEntity.TAG_SPAWN_CHANCE)
                                ? extra.getFloat(DungeonMarkerEntity.TAG_SPAWN_CHANCE) : 1.0f;
                        builder.addInfiniteSpawn(entityType, isTag, cooldownSecs * 20, pos, chance);
                    }
                }
                case ROLE_BOSS_SPAWN -> {
                    String bossTypeStr = extra.getString(DungeonMarkerEntity.TAG_BOSS_ID);
                    if (!bossTypeStr.isBlank()) {
                        // boss_id may be short ("blizza") or full ("oririmod:blizza")
                        if (!bossTypeStr.contains(":")) bossTypeStr = "oririmod:" + bossTypeStr;
                        builder.boss(ResourceLocation.parse(bossTypeStr), pos);
                    }
                }
                case ROLE_SWITCH -> {
                    String switchId = extra.getString(DungeonMarkerEntity.TAG_SWITCH_ID);
                    if (switchId.isBlank()) switchId = pos.toShortString(); // fallback
                    builder.addSwitch(switchId, pos);
                }
                case ROLE_DOOR -> {
                    String groupId = extra.contains("group_id") ? extra.getString("group_id") : stageId;
                    int required = extra.contains("required_switches") ? extra.getInt("required_switches") : 1;
                    builder.addDoor(groupId, required, pos);
                }
                case ROLE_AREA_MODIFIER -> {
                    String action = extra.getString(DungeonMarkerEntity.TAG_SWITCH_ID);
                    if (action.isBlank()) action = "destroy";
                    int radius = extra.contains(DungeonMarkerEntity.TAG_COUNT) ? extra.getInt(DungeonMarkerEntity.TAG_COUNT) : 3;
                    String filterStr = extra.getString(DungeonMarkerEntity.TAG_ENEMY_TYPE);
                    ResourceLocation filter = null;
                    if (!filterStr.isBlank()) {
                        String cleanFilter = filterStr.toLowerCase().replace(' ', '_');
                        if (!cleanFilter.contains(":")) cleanFilter = "minecraft:" + cleanFilter;
                        filter = ResourceLocation.tryParse(cleanFilter);
                    }
                    builder.addAreaModifier(action, radius, filter, pos);
                }
                case ROLE_STAGE_TRIGGER -> {
                    int radius = extra.contains("radius") ? extra.getInt("radius") : 5;
                    builder.addTrigger(pos, radius);
                    String switchId = extra.getString(DungeonMarkerEntity.TAG_SWITCH_ID);
                    if (!switchId.isBlank()) {
                        builder.keyDropStageId(switchId);
                    }
                }
            }

            // Timer (SURVIVE_TIMER type)
            if (stageType == StageType.SURVIVE_TIMER && extra.contains("duration_seconds")) {
                builder.timerTicks(extra.getInt("duration_seconds") * 20);
            }
        }

        return builder.build();
    }

    /**
     * Factory: creates the correct DungeonStage implementation for a definition.
     */
    public static DungeonStage createStage(StageDefinition definition) {
        return switch (definition.getStageType()) {
            case KILL_ALL_ENEMIES -> new KillAllEnemiesStage(definition);
            case ACTIVATE_SWITCHES -> new ActivateSwitchesStage(definition);
            case SURVIVE_TIMER -> new SurviveTimerStage(definition);
            case BOSS_FIGHT, MINI_BOSS_FIGHT -> new BossFightStage(definition);
            case FETCH_ITEM -> new FetchItemStage(definition);
            case PUZZLE_SOLVE -> new PuzzleSolveStage(definition);
            case SPAWN_ONLY -> new PassThroughStage(definition);
        };
    }
}
