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
    
    // Example Dungeons
    public static final ResourceKey<Level> VOLCANO = createKey("dungeon_volcano");
    public static final ResourceKey<Level> MOUNTAIN = createKey("dungeon_mountain");
    public static final ResourceKey<Level> UNDERWATER = createKey("dungeon_underwater");

    public static ResourceKey<Level> createKey(String name) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
    }
}
