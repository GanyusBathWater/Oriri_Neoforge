package net.ganyusbathwater.oririmod.dungeon;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Constants for Dungeon dimension keys.
 * A dimension is dynamically generated using a datapack (void world)
 * for each registered dungeon type.
 */
public class DungeonDimensions {
    
    // Dungeons
    public static final ResourceKey<Level> OVERWORLD_DAY = createKey("dungeon_overworld_day");
    public static final ResourceKey<Level> OVERWORLD_NIGHT = createKey("dungeon_overworld_night");

    public static ResourceKey<Level> createKey(String name) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
    }
}
