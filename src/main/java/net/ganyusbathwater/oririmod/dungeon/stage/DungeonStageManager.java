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
    public static final String ROLE_MINI_BOSS_SPAWN = "MINI_BOSS_SPAWN";
    public static final String ROLE_SWITCH        = "SWITCH";
    public static final String ROLE_DOOR          = "DOOR";
    public static final String ROLE_AREA_MODIFIER = "AREA_MODIFIER";
    public static final String ROLE_STAGE_TRIGGER = "STAGE_TRIGGER";
    public static final String ROLE_GOAL_AREA     = "GOAL_AREA";
    public static final String ROLE_BLOCK_MATCH   = "BLOCK_MATCH";
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
                String table = marker.getExtraData().getString(DungeonMarkerEntity.TAG_LOOT_TABLE);
                if (!table.isBlank()) {
                    instance.setLootChestTable(table);
                }
                continue;
            }
            if (ROLE_PLAYER_SPAWN.equalsIgnoreCase(marker.getRole())) {
                if (instance.getPlayerSpawnPos() == null) {
                    instance.setPlayerSpawnPos(marker.blockPosition());
                }
                // Do NOT continue here; if it has a stage ID, we want it in the stage definition!
            }
            String stageId = marker.getStageId();
            if (stageId.isBlank()) continue;
            byStage.computeIfAbsent(stageId, k -> new ArrayList<>()).add(marker);
        }

        // Build StageDefinitions, sorted by stage ID (alphanumeric)
        List<String> sortedIds = new ArrayList<>(byStage.keySet());
        sortedIds.sort((s1, s2) -> {
            int i1 = 0, i2 = 0;
            while (i1 < s1.length() && i2 < s2.length()) {
                char c1 = s1.charAt(i1);
                char c2 = s2.charAt(i2);
                boolean isDigit1 = Character.isDigit(c1);
                boolean isDigit2 = Character.isDigit(c2);
                
                if (isDigit1 && isDigit2) {
                    int start1 = i1;
                    while (i1 < s1.length() && Character.isDigit(s1.charAt(i1))) i1++;
                    int start2 = i2;
                    while (i2 < s2.length() && Character.isDigit(s2.charAt(i2))) i2++;
                    
                    try {
                        long n1 = Long.parseLong(s1.substring(start1, i1));
                        long n2 = Long.parseLong(s2.substring(start2, i2));
                        if (n1 != n2) return Long.compare(n1, n2);
                    } catch (NumberFormatException e) {
                        int cmp = s1.substring(start1, i1).compareTo(s2.substring(start2, i2));
                        if (cmp != 0) return cmp;
                    }
                } else {
                    int start1 = i1;
                    while (i1 < s1.length() && !Character.isDigit(s1.charAt(i1))) i1++;
                    int start2 = i2;
                    while (i2 < s2.length() && !Character.isDigit(s2.charAt(i2))) i2++;
                    
                    int cmp = s1.substring(start1, i1).compareTo(s2.substring(start2, i2));
                    if (cmp != 0) return cmp;
                }
            }
            return s1.length() - s2.length();
        });

        List<StageDefinition> definitions = new ArrayList<>();
        for (String stageId : sortedIds) {
            StageDefinition def = buildStageDefinition(stageId, byStage.get(stageId));
            if (def != null) definitions.add(def);
        }
        
        // After building the ordered stages, set the initial dungeon spawn to the first stage's spawn point.
        if (!definitions.isEmpty() && definitions.get(0).getPlayerSpawnPos() != null) {
            instance.setPlayerSpawnPos(definitions.get(0).getPlayerSpawnPos());
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
            if (!loot.isBlank() && !ROLE_SPAWN_POINT.equalsIgnoreCase(role) && !ROLE_INFINITE_SPAWNER.equalsIgnoreCase(role) && !ROLE_BOSS_SPAWN.equalsIgnoreCase(role) && !ROLE_MINI_BOSS_SPAWN.equalsIgnoreCase(role)) {
                builder.keyDropStageId(loot);
            }

            switch (role) {
                case ROLE_PLAYER_SPAWN -> {
                    builder.playerSpawn(pos);
                }
                case ROLE_SPAWN_POINT -> {
                    String entityTypeStr = extra.getString(DungeonMarkerEntity.TAG_ENEMY_TYPE);
                    if (!entityTypeStr.isBlank()) {
                        boolean isTag = entityTypeStr.startsWith("#");
                        String cleanType = entityTypeStr.replace("#", "").toLowerCase().replace(' ', '_');
                        if (!cleanType.contains(":")) {
                            if (!isTag && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(net.minecraft.resources.ResourceLocation.parse("oririmod:" + cleanType))) {
                                cleanType = "oririmod:" + cleanType;
                            } else {
                                cleanType = (isTag ? "oririmod:" : "minecraft:") + cleanType;
                            }
                        }
                        ResourceLocation entityType = ResourceLocation.tryParse(cleanType);
                        if (entityType == null) continue;
                        int count = extra.contains(DungeonMarkerEntity.TAG_COUNT)
                                ? extra.getInt(DungeonMarkerEntity.TAG_COUNT) : 1;
                        float chance = extra.contains(DungeonMarkerEntity.TAG_SPAWN_CHANCE)
                                ? extra.getFloat(DungeonMarkerEntity.TAG_SPAWN_CHANCE) : 1.0f;
                        builder.addSpawn(entityType, isTag, count, pos, chance, loot.isBlank() ? null : loot);
                    }
                }
                case ROLE_INFINITE_SPAWNER -> {
                    String entityTypeStr = extra.getString(DungeonMarkerEntity.TAG_ENEMY_TYPE);
                    if (!entityTypeStr.isBlank()) {
                        boolean isTag = entityTypeStr.startsWith("#");
                        String cleanType = entityTypeStr.replace("#", "").toLowerCase().replace(' ', '_');
                        if (!cleanType.contains(":")) {
                            if (!isTag && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(net.minecraft.resources.ResourceLocation.parse("oririmod:" + cleanType))) {
                                cleanType = "oririmod:" + cleanType;
                            } else {
                                cleanType = (isTag ? "oririmod:" : "minecraft:") + cleanType;
                            }
                        }
                        ResourceLocation entityType = ResourceLocation.tryParse(cleanType);
                        if (entityType == null) continue;
                        int cooldownSecs = extra.contains(DungeonMarkerEntity.TAG_COUNT)
                                ? extra.getInt(DungeonMarkerEntity.TAG_COUNT) : 5;
                        if (cooldownSecs <= 0) cooldownSecs = 5;
                        float chance = extra.contains(DungeonMarkerEntity.TAG_SPAWN_CHANCE)
                                ? extra.getFloat(DungeonMarkerEntity.TAG_SPAWN_CHANCE) : 1.0f;
                        builder.addInfiniteSpawn(entityType, isTag, cooldownSecs * 20, pos, chance, loot.isBlank() ? null : loot);
                    }
                }
                case ROLE_BOSS_SPAWN, ROLE_MINI_BOSS_SPAWN -> {
                    String bossTypeStr = extra.getString(DungeonMarkerEntity.TAG_BOSS_ID);
                    if (!bossTypeStr.isBlank()) {
                        // boss_id may be short ("blizza") or full ("oririmod:blizza")
                        if (!bossTypeStr.contains(":")) bossTypeStr = "oririmod:" + bossTypeStr;
                        builder.boss(ResourceLocation.parse(bossTypeStr), pos, loot.isBlank() ? null : loot);
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
                    if (radius <= 0) radius = 3;
                    String filterStr = extra.getString(DungeonMarkerEntity.TAG_ENEMY_TYPE);
                    ResourceLocation filter = null;
                    if (!filterStr.isBlank()) {
                        String cleanFilter = filterStr.toLowerCase().replace(' ', '_');
                        if (!cleanFilter.contains(":")) cleanFilter = "minecraft:" + cleanFilter;
                        filter = ResourceLocation.tryParse(cleanFilter);
                    }
                    builder.addAreaModifier(action, radius, filter, filterStr, pos, marker.getYRot());
                }
                case ROLE_STAGE_TRIGGER -> {
                    int radius = extra.contains(DungeonMarkerEntity.TAG_COUNT) ? extra.getInt(DungeonMarkerEntity.TAG_COUNT) : 5;
                    if (radius <= 0) radius = 5;
                    builder.addTrigger(pos, radius);
                    String switchId = extra.getString(DungeonMarkerEntity.TAG_SWITCH_ID);
                    if (!switchId.isBlank()) {
                        builder.keyDropStageId(switchId);
                    }
                }
                case ROLE_GOAL_AREA -> {
                    int radius = extra.contains(DungeonMarkerEntity.TAG_COUNT) ? extra.getInt(DungeonMarkerEntity.TAG_COUNT) : 5;
                    if (radius <= 0) radius = 5;
                    builder.addGoal(pos, radius);
                }
                case ROLE_BLOCK_MATCH -> {
                    String blockId = extra.getString(DungeonMarkerEntity.TAG_ENEMY_TYPE);
                    String blockState = extra.getString(DungeonMarkerEntity.TAG_COUNT);
                    if (!blockId.isBlank()) {
                        if (!blockId.contains(":")) blockId = "minecraft:" + blockId;
                        builder.addBlockMatch(net.minecraft.resources.ResourceLocation.tryParse(blockId), blockState, pos);
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
            case PUZZLE_SOLVE -> new PuzzleSolveStage(definition);
            case REACH_GOAL -> new ReachGoalStage(definition);
            case SPAWN_ONLY -> new PassThroughStage(definition);
        };
    }
}
