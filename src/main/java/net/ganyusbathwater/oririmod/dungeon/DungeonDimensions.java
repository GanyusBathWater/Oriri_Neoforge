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
    public static final ResourceKey<Level> TUTORIAL = createKey("dungeon_tutorial");

    public static ResourceKey<Level> createKey(String name) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
    }
}
