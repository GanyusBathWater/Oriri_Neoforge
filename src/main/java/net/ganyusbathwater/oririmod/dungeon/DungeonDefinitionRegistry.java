package net.ganyusbathwater.oririmod.dungeon;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for all DungeonDefinitions.
 * Populated at mod startup (FMLCommonSetupEvent).
 *
 * Each dungeon needs a unique string ID (e.g. "volcano_dungeon"),
 * a display name, description, a dimension key, and a structure NBT path.
 */
public class DungeonDefinitionRegistry {

    private static final Map<String, DungeonDefinition> REGISTRY = new HashMap<>();

    public static void register(DungeonDefinition definition) {
        REGISTRY.put(definition.id(), definition);
    }

    @Nullable
    public static DungeonDefinition get(String id) {
        return REGISTRY.get(id);
    }

    public static Collection<DungeonDefinition> all() {
        return REGISTRY.values();
    }

    /** Called during FMLCommonSetupEvent to populate built-in definitions. */
    public static void init() {
        register(new DungeonDefinition(
                "volcano_dungeon",
                "The Volcanic Forge",
                "An ancient forge inside a burning volcano. Watch your step.",
                DungeonDimensions.VOLCANO,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("oririmod", "dungeon/volcano_dungeon")
        ));
        register(new DungeonDefinition(
                "mountain_dungeon",
                "The Mountain Citadel",
                "A fortress carved into the peaks of the Crimson Mountains.",
                DungeonDimensions.MOUNTAIN,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("oririmod", "dungeon/mountain_dungeon")
        ));
        register(new DungeonDefinition(
                "underwater_dungeon",
                "The Sunken Temple",
                "A flooded temple beneath the ocean floor.",
                DungeonDimensions.UNDERWATER,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("oririmod", "dungeon/underwater_dungeon")
        ));
    }
}
