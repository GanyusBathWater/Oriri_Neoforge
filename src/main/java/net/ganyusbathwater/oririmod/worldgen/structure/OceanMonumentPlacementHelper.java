package net.ganyusbathwater.oririmod.worldgen.structure;

import net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentStructure;

import java.util.Arrays;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Coordinates adaptive Ocean Monument elevation and crater-free interior clearing
 * inside {@link ElderwoodsChunkGenerator} dimensions.
 */
public final class OceanMonumentPlacementHelper {
    public static final ThreadLocal<Boolean> ELDERWOODS_ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final int SEA_LEVEL = 72;
    private static final int MONUMENT_FOOTPRINT_SPAN = 57; // MonumentBuilding is 58x23x58 (min..min+57)
    private static final int MONUMENT_HALF_OFFSET = 29;
    private static final int COLLAR_MARGIN = 6;
    private static final BlockState FILL_BLOCK = Blocks.WATER.defaultBlockState();
    private static final Set<Block> FILL_KEEP = Set.of(Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.WATER);

    private OceanMonumentPlacementHelper() {
    }

    public interface ShiftableMonument {
        void oririmod$shiftVertical(int dy);
    }

    /**
     * Evaluates whether an Ocean Monument centered on {@code chunkPos} has sufficiently
     * level, non-clipping submarine terrain and returns a seabed-adaptive minimum Y,
     * or {@link OptionalInt#empty()} when the footprint would clip into coastal hills,
     * cliffs, dry land, or hang over steep trench drop-offs.
     */
    public static OptionalInt evaluateSafeMinY(Structure.GenerationContext context, ChunkPos chunkPos) {
        int minX = chunkPos.getMinBlockX() - MONUMENT_HALF_OFFSET;
        int minZ = chunkPos.getMinBlockZ() - MONUMENT_HALF_OFFSET;
        int maxX = minX + MONUMENT_FOOTPRINT_SPAN;
        int maxZ = minZ + MONUMENT_FOOTPRINT_SPAN;

        // Sample a 9x9 interior grid across the 58x58 footprint (inner 1..56)
        int[] samples = new int[81];
        int idx = 0;
        int coreMax = Integer.MIN_VALUE;
        int sMinX = minX + 1;
        int sMaxX = maxX - 1;
        int sMinZ = minZ + 1;
        int sMaxZ = maxZ - 1;
        int coreInset = 10;

        for (int gx = 0; gx < 9; gx++) {
            int sx = sMinX + (sMaxX - sMinX) * gx / 8;
            boolean inCoreX = sx >= minX + coreInset && sx <= maxX - coreInset;
            for (int gz = 0; gz < 9; gz++) {
                int sz = sMinZ + (sMaxZ - sMinZ) * gz / 8;
                int h = sampleOceanFloor(context, sx, sz);
                samples[idx++] = h;
                if (inCoreX && sz >= minZ + coreInset && sz <= maxZ - coreInset && h > coreMax) {
                    coreMax = h;
                }
            }
        }

        // Sample a 24-point collar 6 blocks outside the footprint to detect looming cliffs or overhangs
        int cMinX = minX - COLLAR_MARGIN;
        int cMaxX = maxX + COLLAR_MARGIN;
        int cMinZ = minZ - COLLAR_MARGIN;
        int cMaxZ = maxZ + COLLAR_MARGIN;
        int collarMax = Integer.MIN_VALUE;
        int collarMin = Integer.MAX_VALUE;
        for (int i = 0; i < 6; i++) {
            int px = cMinX + (cMaxX - cMinX) * i / 5;
            int pz = cMinZ + (cMaxZ - cMinZ) * i / 5;
            int[] edgeHeights = {
                    sampleOceanFloor(context, px, cMinZ),
                    sampleOceanFloor(context, px, cMaxZ),
                    sampleOceanFloor(context, cMinX, pz),
                    sampleOceanFloor(context, cMaxX, pz)
            };
            for (int eh : edgeHeights) {
                if (eh > collarMax) collarMax = eh;
                if (eh < collarMin) collarMin = eh;
            }
        }

        Arrays.sort(samples);
        int boxMin = samples[0];
        int p15 = samples[12];
        int p85 = samples[68];
        int boxMax = samples[80];

        // Choose plinth Y so the central pyramid clears seabed rock cleanly while anchoring into gentle undulations
        int targetMinY = Math.max(coreMax - 2, p85 - 2);

        // Grey-area placement gate:
        // 1. Keep footprint & 6-block apron genuinely submerged (< SEA_LEVEL - 4 and < SEA_LEVEL - 2)
        //    while allowing shallow shoals up to Y=68 where the pyramid crown may rise above water.
        if (boxMax > SEA_LEVEL - 4 || collarMax > SEA_LEVEL - 2) {
            return OptionalInt.empty();
        }
        // 2. Reject abyssal trenches / sharp trench walls where stilts would cantilever or overhang.
        if (targetMinY < 30 || boxMin < 26 || collarMin < 22) {
            return OptionalInt.empty();
        }
        // 3. Reject outer-wall burial, cliff overhangs, and excessive seabed relief.
        if ((boxMax - targetMinY) > 4
                || (collarMax - targetMinY) > 7
                || (boxMax - boxMin) > 10
                || (targetMinY - boxMin) > 8
                || (targetMinY - p15) > 6) {
            return OptionalInt.empty();
        }

        int minAllowed = context.heightAccessor().getMinBuildHeight() + 8;
        int maxAllowed = context.heightAccessor().getMaxBuildHeight() - 26;
        return OptionalInt.of(Math.max(minAllowed, Math.min(maxAllowed, targetMinY)));
    }

    public static int computeAdaptiveMinY(Structure.GenerationContext context, BoundingBox box) {
        OptionalInt safe = evaluateSafeMinY(context, context.chunkPos());
        if (safe.isPresent()) {
            return safe.getAsInt();
        }
        int centerX = (box.minX() + box.maxX()) / 2;
        int centerZ = (box.minZ() + box.maxZ()) / 2;
        int fallbackY = sampleOceanFloor(context, centerX, centerZ) - 1;
        int minAllowed = context.heightAccessor().getMinBuildHeight() + 8;
        int maxAllowed = context.heightAccessor().getMaxBuildHeight() - 26;
        return Math.max(minAllowed, Math.min(maxAllowed, fallbackY));
    }

    private static int sampleOceanFloor(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(
                x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
    }

    public static void clearRoomInterior(WorldGenLevel level, BoundingBox chunkBox, BoundingBox roomBox) {
        clearRoomInterior(level, chunkBox, roomBox, FILL_KEEP, FILL_BLOCK);
    }

    /**
     * Clears only an enclosed interior room's bounding box intersection using Elderwoods sea level (BASE_HEIGHT).
     * Leaves surrounding terrain, sand, kelp, and coral untouched outside the monument walls.
     */
    public static void clearRoomInterior(WorldGenLevel level, BoundingBox chunkBox, BoundingBox roomBox,
            Set<Block> fillKeep, BlockState fillBlock) {
        int x0 = Math.max(chunkBox.minX(), roomBox.minX());
        int y0 = Math.max(chunkBox.minY(), roomBox.minY());
        int z0 = Math.max(chunkBox.minZ(), roomBox.minZ());
        int x1 = Math.min(chunkBox.maxX(), roomBox.maxX());
        int y1 = Math.min(chunkBox.maxY(), roomBox.maxY());
        int z1 = Math.min(chunkBox.maxZ(), roomBox.maxZ());
        if (x0 > x1 || y0 > y1 || z0 > z1) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = y0; y <= y1; y++) {
            boolean aboveSea = y >= ElderwoodsChunkGenerator.BASE_HEIGHT;
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    pos.set(x, y, z);
                    BlockState existing = level.getBlockState(pos);
                    if (fillKeep.contains(existing.getBlock()) || existing.is(Blocks.BEDROCK)) {
                        continue;
                    }
                    if (aboveSea && existing != fillBlock) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    } else {
                        level.setBlock(pos, fillBlock, 2);
                    }
                }
            }
        }
    }

    public static boolean isExcludedFromPreClear(StructurePiece piece) {
        return piece instanceof OceanMonumentPieces.OceanMonumentWingRoom
                || piece instanceof OceanMonumentPieces.OceanMonumentPenthouse;
    }

    public enum MonumentProximity {
        NONE,
        HALO,
        INSIDE
    }

    private static final int MONUMENT_CLEANUP_MARGIN = 6;
    private static final int MONUMENT_EXCLUSION_MARGIN_SQ = MONUMENT_CLEANUP_MARGIN * MONUMENT_CLEANUP_MARGIN;
    private static final int MONUMENT_HALO_RADIUS_SQ = 38 * 38;

    public static MonumentProximity getMonumentProximity(WorldGenLevel level, BlockPos pos) {
        return getMonumentProximity(level, pos.getX(), pos.getZ());
    }

    /**
     * Checks nearby {@link ChunkStatus#STRUCTURE_STARTS} entries for Ocean Monuments without fragile
     * perimeter chunk bailouts. Works consistently across chunk decoration borders so cross-chunk
     * kelp/flora spillover never intersects monument masonry or its immediate skirt.
     */
    public static MonumentProximity getMonumentProximity(WorldGenLevel level, int worldX, int worldZ) {
        int cx = worldX >> 4;
        int cz = worldZ >> 4;
        WorldGenRegion region = level instanceof WorldGenRegion r ? r : null;
        MonumentProximity closest = MonumentProximity.NONE;

        // MonumentBuilding spans 58x58 blocks (half-width 29) centered on its start chunk;
        // scanning +-4 chunks covers the full footprint + 38-block outer halo across chunk borders.
        for (int scanX = cx - 4; scanX <= cx + 4; scanX++) {
            for (int scanZ = cz - 4; scanZ <= cz + 4; scanZ++) {
                if (region != null && !region.hasChunk(scanX, scanZ)) {
                    continue;
                }
                ChunkAccess chunk = level.getChunk(scanX, scanZ, ChunkStatus.STRUCTURE_STARTS, false);
                if (chunk == null) {
                    continue;
                }
                for (StructureStart start : chunk.getAllStarts().values()) {
                    if (start.isValid() && start.getStructure() instanceof OceanMonumentStructure) {
                        BoundingBox box = start.getBoundingBox();
                        int dx = Math.max(0, Math.max(box.minX() - worldX, worldX - box.maxX()));
                        int dz = Math.max(0, Math.max(box.minZ() - worldZ, worldZ - box.maxZ()));
                        int distSq = dx * dx + dz * dz;
                        if (distSq <= MONUMENT_EXCLUSION_MARGIN_SQ) {
                            return MonumentProximity.INSIDE;
                        }
                        if (distSq <= MONUMENT_HALO_RADIUS_SQ) {
                            closest = MonumentProximity.HALO;
                        }
                    }
                }
            }
        }
        return closest;
    }

    /**
     * Removes any aquatic plants left floating or stranded on monument masonry inside the current
     * chunk slice after {@link OceanMonumentPieces.MonumentBuilding#postProcess} carves or builds.
     */
    public static void cleanupOrphanedAquaticPlants(WorldGenLevel level, BoundingBox chunkBox, BoundingBox monumentBox) {
        int minX = Math.max(chunkBox.minX(), monumentBox.minX() - MONUMENT_CLEANUP_MARGIN);
        int maxX = Math.min(chunkBox.maxX(), monumentBox.maxX() + MONUMENT_CLEANUP_MARGIN);
        int minZ = Math.max(chunkBox.minZ(), monumentBox.minZ() - MONUMENT_CLEANUP_MARGIN);
        int maxZ = Math.min(chunkBox.maxZ(), monumentBox.maxZ() + MONUMENT_CLEANUP_MARGIN);
        if (minX > maxX || minZ > maxZ) {
            return;
        }

        int minY = Math.max(level.getMinBuildHeight() + 1, monumentBox.minY() - 2);
        int maxY = Math.min(level.getMaxBuildHeight() - 2, ElderwoodsChunkGenerator.BASE_HEIGHT + 2);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos abovePos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // Bottom-up scan clears the base of an orphaned kelp stalk first so upper segments cascade cleanly
                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!isAquaticPlant(state)) {
                        continue;
                    }
                    belowPos.set(x, y - 1, z);
                    BlockState below = level.getBlockState(belowPos);

                    boolean valid = false;
                    if (state.is(Blocks.TALL_SEAGRASS)) {
                        if (state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
                            valid = below.is(Blocks.TALL_SEAGRASS);
                        } else {
                            BlockState above = level.getBlockState(abovePos.set(x, y + 1, z));
                            valid = isValidPlantSubstrate(level, belowPos, below) && above.is(Blocks.TALL_SEAGRASS);
                        }
                    } else if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)) {
                        valid = below.is(Blocks.KELP_PLANT) || isValidPlantSubstrate(level, belowPos, below);
                    } else {
                        valid = isValidPlantSubstrate(level, belowPos, below);
                    }

                    if (!valid) {
                        BlockState replacement = y >= ElderwoodsChunkGenerator.BASE_HEIGHT
                                ? Blocks.AIR.defaultBlockState()
                                : FILL_BLOCK;
                        level.setBlock(pos, replacement, 2);
                    }
                }
            }
        }
    }

    private static boolean isAquaticPlant(BlockState state) {
        return state.is(Blocks.KELP)
                || state.is(Blocks.KELP_PLANT)
                || state.is(Blocks.SEAGRASS)
                || state.is(Blocks.TALL_SEAGRASS)
                || state.is(Blocks.SEA_PICKLE);
    }

    private static boolean isValidPlantSubstrate(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.is(Blocks.WATER) || isMonumentBlock(state)) {
            return false;
        }
        return state.isFaceSturdy(level, pos, Direction.UP);
    }

    private static boolean isMonumentBlock(BlockState state) {
        return state.is(Blocks.PRISMARINE)
                || state.is(Blocks.PRISMARINE_BRICKS)
                || state.is(Blocks.DARK_PRISMARINE)
                || state.is(Blocks.SEA_LANTERN);
    }
}
