package net.ganyusbathwater.oririmod.dungeon.dimension;

import net.ganyusbathwater.oririmod.dungeon.DungeonDefinition;
import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.resources.ResourceLocation;

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
        BlockPos origin = instance.getOrigin();
        StructurePlaceSettings settings = new StructurePlaceSettings()
                // We must NOT ignore entities, because we need our DungeonMarkerEntity instances to spawn!
                .setIgnoreEntities(false) 
                .setKnownShape(true);

        // Try single file first
        Optional<StructureTemplate> single = dimensionLevel.getStructureManager().get(definition.structureId());
        if (single.isPresent()) {
            single.get().placeInWorld(dimensionLevel, origin, origin, settings, dimensionLevel.getRandom(), 2);
            instance.setStructureBounds(single.get().getBoundingBox(settings, origin));
            return;
        }

        // Try multi-part grid structure (e.g., name_0_0, name_0_48)
        int gridSize = 48;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        boolean foundAny = false;

        // Arbitrary limits to prevent infinite loops, assuming 10x10 chunks max (480x480)
        for (int x = 0; x < 20; x++) {
            for (int z = 0; z < 20; z++) {
                int offsetX = x * gridSize;
                int offsetZ = z * gridSize;
                
                ResourceLocation partId = ResourceLocation.fromNamespaceAndPath(
                    definition.structureId().getNamespace(),
                    definition.structureId().getPath() + "_" + offsetX + "_" + offsetZ
                );
                
                Optional<StructureTemplate> partOpt = dimensionLevel.getStructureManager().get(partId);
                if (partOpt.isPresent()) {
                    foundAny = true;
                    StructureTemplate part = partOpt.get();
                    BlockPos partOrigin = origin.offset(offsetX, 0, offsetZ);
                    
                    part.placeInWorld(dimensionLevel, partOrigin, partOrigin, settings, dimensionLevel.getRandom(), 2);
                    
                    var bounds = part.getBoundingBox(settings, partOrigin);
                    minX = Math.min(minX, bounds.minX());
                    minY = Math.min(minY, bounds.minY());
                    minZ = Math.min(minZ, bounds.minZ());
                    maxX = Math.max(maxX, bounds.maxX());
                    maxY = Math.max(maxY, bounds.maxY());
                    maxZ = Math.max(maxZ, bounds.maxZ());
                }
            }
        }
        
        if (foundAny) {
            instance.setStructureBounds(new net.minecraft.world.level.levelgen.structure.BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
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
        
        BlockPos spawnPos = instance.getPlayerSpawnPos();
        if (spawnPos == null) {
            spawnPos = origin.offset(5, 1, 5); // Fallback
        }
        
        player.teleportTo(
                dimensionLevel, 
                spawnPos.getX() + 0.5, 
                spawnPos.getY(), 
                spawnPos.getZ() + 0.5, 
                0, 0
        );
    }
}
