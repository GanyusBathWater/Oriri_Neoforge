package net.ganyusbathwater.oririmod.dungeon.dimension;

import net.ganyusbathwater.oririmod.dungeon.DungeonDefinition;
import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

/**
 * Handles physical instantiation of dungeons (placing structures) and
 * teleporting players into them.
 */
public class DungeonDimensionManager {

    /**
     * Loads the structure NBT defined in the DungeonDefinition and places it
     * in the target dimension at the instance's allocated origin.
     */
    public static void placeDungeonStructure(ServerLevel dimensionLevel, DungeonDefinition definition, DungeonInstance instance) {
        Optional<StructureTemplate> templateOpt = dimensionLevel.getStructureManager().get(definition.structureId());
        
        if (templateOpt.isPresent()) {
            StructureTemplate template = templateOpt.get();
            BlockPos origin = instance.getOrigin();
            
            StructurePlaceSettings settings = new StructurePlaceSettings()
                    // We must NOT ignore entities, because we need our DungeonMarkerEntity instances to spawn!
                    .setIgnoreEntities(false) 
                    .setKnownShape(true);
            
            // Block flags: 2 = send to client
            template.placeInWorld(dimensionLevel, origin, origin, settings, dimensionLevel.getRandom(), 2);
            
            // Save the exact bounding box for chunk cleanup later
            instance.setStructureBounds(template.getBoundingBox(settings, origin));
        } else {
            System.err.println("[OririMod] CRITICAL: Could not find dungeon structure: " + definition.structureId());
        }
    }

    /**
     * Teleports a player into the dungeon dimension relative to the instance origin.
     * In Phase 4, we will read the "SPAWN_POINT" marker to find the exact location,
     * but for now we fallback to a safe offset near the origin.
     */
    public static void teleportPlayerToDungeon(ServerPlayer player, ServerLevel dimensionLevel, DungeonInstance instance) {
        BlockPos origin = instance.getOrigin();
        
        // Fallback spawn position (can be overwritten later by marker scanning)
        BlockPos spawnPos = origin.offset(5, 1, 5);
        
        player.teleportTo(
                dimensionLevel, 
                spawnPos.getX() + 0.5, 
                spawnPos.getY(), 
                spawnPos.getZ() + 0.5, 
                0, 0
        );
    }
}
