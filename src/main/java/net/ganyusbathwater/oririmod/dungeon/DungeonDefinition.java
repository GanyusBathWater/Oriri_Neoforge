package net.ganyusbathwater.oririmod.dungeon;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * Defines the static properties of a Dungeon type (e.g. "Volcano", "Crystal Cave").
 * This is effectively a template from which active DungeonInstances are created.
 */
public record DungeonDefinition(
        String id,                        // Unique string ID for lookup (e.g. "volcano_dungeon")
        String displayName,               // Human-readable title shown in the NPC screen
        String description,               // Lore text shown below the title
        ResourceKey<Level> dimension,     // Target void dimension
        ResourceLocation structureId,     // Path to the .nbt structure file
        @Nullable ResourceLocation rewardLootTable,      // Loot table to spawn upon completion
        @Nullable String requiredPreviousDungeon,        // ID of dungeon that must be completed first
        @Nullable ResourceLocation dungeonTrack,         // Main BGM track for the dungeon
        Map<String, ResourceLocation> stageTrackOverrides // Overrides BGM for specific stages (e.g. boss)
) {
    /** Short constructor for legacy code or simple dungeons. */
    public DungeonDefinition(String id, ResourceKey<Level> dimension, ResourceLocation structureId) {
        this(id, id, "", dimension, structureId, null, null, null, Collections.emptyMap());
    }

    /** Constructor used by registry for dungeons with display info but no advanced phase 7 features yet. */
    public DungeonDefinition(String id, String displayName, String description, ResourceKey<Level> dimension, ResourceLocation structureId) {
        this(id, displayName, description, dimension, structureId, null, null, null, Collections.emptyMap());
    }
}
