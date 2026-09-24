package net.ganyusbathwater.oririmod.dungeon.dimension;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.dungeon.DungeonDefinition;
import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class DungeonGeneratorTask {
    private final ServerLevel level;
    private final DungeonDefinition definition;
    private final DungeonInstance instance;
    private final StructurePlaceSettings settings;
    
    private final Queue<PlacementStep> placementQueue = new LinkedList<>();
    private final List<net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity> collectedMarkers = new ArrayList<>();
    private boolean isFinished = false;
    private boolean isCancelled = false;

    private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    private boolean foundAny = false;

    public DungeonGeneratorTask(ServerLevel level, DungeonDefinition definition, DungeonInstance instance) {
        this.level = level;
        this.definition = definition;
        this.instance = instance;
        this.settings = new StructurePlaceSettings().setIgnoreEntities(false).setKnownShape(true);
        
        // Try single file first
        Optional<StructureTemplate> single = DungeonDimensionManager.loadTemplateFromClasspath(level, definition.structureId());
        if (single.isPresent()) {
            placementQueue.add(new PlacementStep(single.get(), instance.getOrigin()));
            return;
        }

        // Enqueue multi-part grid structure (e.g., name_0_0, name_0_48)
        int gridSize = 48;
        for (int x = 0; x < 20; x++) {
            for (int z = 0; z < 20; z++) {
                int offsetX = x * gridSize;
                int offsetZ = z * gridSize;

                ResourceLocation partId = ResourceLocation.fromNamespaceAndPath(
                    definition.structureId().getNamespace(),
                    definition.structureId().getPath() + "_" + offsetX + "_" + offsetZ
                );

                Optional<StructureTemplate> partOpt = DungeonDimensionManager.loadTemplateFromClasspath(level, partId);
                if (partOpt.isPresent()) {
                    BlockPos partOrigin = instance.getOrigin().offset(offsetX, 0, offsetZ);
                    placementQueue.add(new PlacementStep(partOpt.get(), partOrigin));
                }
            }
        }

        if (placementQueue.isEmpty()) {
            OririMod.LOGGER.error("[DungeonGeneratorTask] CRITICAL: Could not find any structure files for: {}", definition.structureId());
            this.isFinished = true;
        }
    }

    public boolean isFinished() { return isFinished; }
    public boolean isCancelled() { return isCancelled; }
    public void cancel() { this.isCancelled = true; }
    
    public DungeonInstance getInstance() { return instance; }

    /**
     * Executes one step of the generation queue. Call this every server tick.
     */
    public void tick() {
        if (isFinished || isCancelled) return;

        // Process 1 piece per tick to heavily reduce lag spike
        PlacementStep step = placementQueue.poll();
        if (step != null) {
            DungeonDimensionManager.forceLoadChunks(level, step.origin, step.template);
            int placeFlags = net.minecraft.world.level.block.Block.UPDATE_CLIENTS 
                           | net.minecraft.world.level.block.Block.UPDATE_SUPPRESS_DROPS;
            step.template.placeInWorld(level, step.origin, step.origin, settings, level.getRandom(), placeFlags);
            
            var bounds = step.template.getBoundingBox(settings, step.origin);
            minX = Math.min(minX, bounds.minX());
            minY = Math.min(minY, bounds.minY());
            minZ = Math.min(minZ, bounds.minZ());
            maxX = Math.max(maxX, bounds.maxX());
            maxY = Math.max(maxY, bounds.maxY());
            maxZ = Math.max(maxZ, bounds.maxZ());
            foundAny = true;

            net.minecraft.world.phys.AABB pieceBox = new net.minecraft.world.phys.AABB(
                bounds.minX() - 2.0, level.getMinBuildHeight(), bounds.minZ() - 2.0,
                bounds.maxX() + 3.0, level.getMaxBuildHeight(), bounds.maxZ() + 3.0
            );
            
            // Sweep any accidental items baked into this structure's NBT, expanding bounds slightly to catch edge items.
            java.util.List<net.minecraft.world.entity.item.ItemEntity> droppedItems = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, pieceBox);
            for (net.minecraft.world.entity.item.ItemEntity item : droppedItems) {
                item.discard();
            }

            collectedMarkers.addAll(level.getEntitiesOfClass(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.class, pieceBox));
        }

        if (placementQueue.isEmpty()) {
            this.isFinished = true;
            if (foundAny) {
                instance.setStructureBounds(new net.minecraft.world.level.levelgen.structure.BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
                OririMod.LOGGER.info("[DungeonGeneratorTask] Finished placing multi-part structure for {} at origin {}", definition.structureId(), instance.getOrigin());
                
                // Build stages immediately after generation before chunks unload
                List<net.ganyusbathwater.oririmod.dungeon.stage.StageDefinition> stages = 
                    net.ganyusbathwater.oririmod.dungeon.stage.DungeonStageManager.buildStages(collectedMarkers, instance);
                instance.setStageDefinitions(stages);
                
                // Final sweep: remove any items dropped during generation due to overlapping bounds or block updates
                net.minecraft.world.phys.AABB fullBox = new net.minecraft.world.phys.AABB(
                    minX, level.getMinBuildHeight(), minZ,
                    maxX, level.getMaxBuildHeight(), maxZ
                );
                java.util.List<net.minecraft.world.entity.item.ItemEntity> droppedItems = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, fullBox);
                for (net.minecraft.world.entity.item.ItemEntity item : droppedItems) {
                    item.discard();
                }
            }
        }
    }

    private record PlacementStep(StructureTemplate template, BlockPos origin) {}
}
