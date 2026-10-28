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
    private final java.util.Set<UUID> collectedMarkerIds = new java.util.HashSet<>();
    private final java.util.Set<net.minecraft.world.level.ChunkPos> forcedChunks = new java.util.HashSet<>();
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
            PlacementStep step = new PlacementStep(single.get(), instance.getOrigin());
            placementQueue.add(step);
            preForceStepChunks(step);
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
                    PlacementStep step = new PlacementStep(partOpt.get(), partOrigin);
                    placementQueue.add(step);
                    preForceStepChunks(step);
                }
            }
        }

        if (placementQueue.isEmpty()) {
            OririMod.LOGGER.error("[DungeonGeneratorTask] CRITICAL: Could not find any structure files for: {}", definition.structureId());
            this.isFinished = true;
        }
    }

    private void preForceStepChunks(PlacementStep step) {
        var bounds = step.template.getBoundingBox(settings, step.origin);
        net.minecraft.world.level.ChunkPos startChunk = new net.minecraft.world.level.ChunkPos(new BlockPos(bounds.minX(), 0, bounds.minZ()));
        net.minecraft.world.level.ChunkPos endChunk = new net.minecraft.world.level.ChunkPos(new BlockPos(bounds.maxX(), 0, bounds.maxZ()));
        for (int cx = startChunk.x; cx <= endChunk.x; cx++) {
            for (int cz = startChunk.z; cz <= endChunk.z; cz++) {
                net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(cx, cz);
                if (forcedChunks.add(cp)) {
                    level.setChunkForced(cx, cz, true);
                }
            }
        }
    }

    public boolean isFinished() { return isFinished; }
    public boolean isCancelled() { return isCancelled; }
    public void cancel() {
        this.isCancelled = true;
        releaseForcedChunks();
    }

    private void releaseForcedChunks() {
        for (net.minecraft.world.level.ChunkPos cp : forcedChunks) {
            level.setChunkForced(cp.x, cp.z, false);
        }
        forcedChunks.clear();
    }
    
    public DungeonInstance getInstance() { return instance; }

    /**
     * Executes one step of the generation queue. Call this every server tick.
     */
    public void tick() {
        if (isFinished || isCancelled) return;

        // Process 1 piece per tick to heavily reduce lag spike
        PlacementStep step = placementQueue.poll();
        if (step != null) {
            var bounds = step.template.getBoundingBox(settings, step.origin);

            // Force-load chunks and clear any stale BlockEntities (e.g. secret chests from an uncleaned session)
            // BEFORE placeInWorld overwrites them with AIR, preventing ChestBlock.onRemove from spilling loot!
            net.minecraft.world.level.ChunkPos startChunk = new net.minecraft.world.level.ChunkPos(new BlockPos(bounds.minX(), 0, bounds.minZ()));
            net.minecraft.world.level.ChunkPos endChunk = new net.minecraft.world.level.ChunkPos(new BlockPos(bounds.maxX(), 0, bounds.maxZ()));
            for (int cx = startChunk.x; cx <= endChunk.x; cx++) {
                for (int cz = startChunk.z; cz <= endChunk.z; cz++) {
                    net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(cx, cz);
                    if (forcedChunks.add(cp)) {
                        level.setChunkForced(cx, cz, true);
                    }
                    net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(cx, cz);
                    List<BlockPos> existingBEPositions = new ArrayList<>(chunk.getBlockEntities().keySet());
                    for (BlockPos bePos : existingBEPositions) {
                        if (bounds.isInside(bePos)) {
                            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(bePos);
                            if (be != null) {
                                net.minecraft.world.Clearable.tryClear(be);
                                level.removeBlockEntity(bePos);
                            }
                        }
                    }
                }
            }

            // Discard any stale entities (old markers, mobs, dropped items) strictly inside this piece's bounds before placement
            net.minecraft.world.phys.AABB exactPieceBox = new net.minecraft.world.phys.AABB(
                bounds.minX(), level.getMinBuildHeight(), bounds.minZ(),
                bounds.maxX() + 1.0, level.getMaxBuildHeight(), bounds.maxZ() + 1.0
            );
            for (net.minecraft.world.entity.Entity staleEntity : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, exactPieceBox)) {
                if (!(staleEntity instanceof net.minecraft.world.entity.player.Player)) {
                    staleEntity.discard();
                }
            }

            int placeFlags = net.minecraft.world.level.block.Block.UPDATE_CLIENTS 
                           | net.minecraft.world.level.block.Block.UPDATE_SUPPRESS_DROPS;
            net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.setSpawnCaptureHook(marker -> {
                if (collectedMarkerIds.add(marker.getUUID())) {
                    collectedMarkers.add(marker);
                }
            });
            try {
                step.template.placeInWorld(level, step.origin, step.origin, settings, level.getRandom(), placeFlags);
            } finally {
                net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.setSpawnCaptureHook(null);
            }
            
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

            for (net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity marker : level.getEntitiesOfClass(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.class, pieceBox)) {
                if (collectedMarkerIds.add(marker.getUUID())) {
                    collectedMarkers.add(marker);
                }
            }
        }

        if (placementQueue.isEmpty()) {
            this.isFinished = true;
            if (foundAny) {
                instance.setStructureBounds(new net.minecraft.world.level.levelgen.structure.BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
                OririMod.LOGGER.info("[DungeonGeneratorTask] Finished placing multi-part structure for {} at origin {}", definition.structureId(), instance.getOrigin());
                
                net.minecraft.world.phys.AABB fullBox = new net.minecraft.world.phys.AABB(
                    minX - 4.0, level.getMinBuildHeight(), minZ - 4.0,
                    maxX + 5.0, level.getMaxBuildHeight(), maxZ + 5.0
                );
                for (net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity marker : level.getEntitiesOfClass(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.class, fullBox)) {
                    if (collectedMarkerIds.add(marker.getUUID())) {
                        collectedMarkers.add(marker);
                    }
                }

                // Build stages immediately after generation before chunks unload
                List<net.ganyusbathwater.oririmod.dungeon.stage.StageDefinition> stages = 
                    net.ganyusbathwater.oririmod.dungeon.stage.DungeonStageManager.buildStages(collectedMarkers, instance);
                instance.setStageDefinitions(stages);
                
                // Final sweep: remove any items dropped during generation due to overlapping bounds or block updates
                java.util.List<net.minecraft.world.entity.item.ItemEntity> droppedItems = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, fullBox);
                for (net.minecraft.world.entity.item.ItemEntity item : droppedItems) {
                    item.discard();
                }
            }
            releaseForcedChunks();
        }
    }

    private record PlacementStep(StructureTemplate template, BlockPos origin) {}
}
