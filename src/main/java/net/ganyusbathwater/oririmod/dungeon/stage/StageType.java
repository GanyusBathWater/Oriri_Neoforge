package net.ganyusbathwater.oririmod.dungeon.stage;

/**
 * All supported stage goal types.
 * Each value maps to a string used in DungeonMarkerEntity NBT (e.g. "KILL_ALL_ENEMIES").
 */
public enum StageType {
    KILL_ALL_ENEMIES,
    ACTIVATE_SWITCHES,
    SURVIVE_TIMER,
    BOSS_FIGHT,
    MINI_BOSS_FIGHT,
    PUZZLE_SOLVE,
    REACH_GOAL,
    SPAWN_ONLY;

    public static StageType fromString(String s) {
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return KILL_ALL_ENEMIES; // Safe fallback
        }
    }
}
