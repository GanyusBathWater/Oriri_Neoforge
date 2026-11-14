package net.ganyusbathwater.oririmod.block.custom;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsBiomeSource;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.Optional;
import java.util.OptionalInt;

public class ElderwoodsPortalForcer {
    private static final int PORTAL_WIDTH = 2;
    private static final int PORTAL_HEIGHT = 4;
    private static final int FINE_SEARCH_RADIUS = 96;
    private static final int FINE_SEARCH_STEP = 3;
    private static final int WIDE_SEARCH_RADIUS = 384;
    private static final int WIDE_SEARCH_STEP = 8;
    private static final int OCEAN_MARGIN_CHECK = 10;

    private final ServerLevel level;

    public ElderwoodsPortalForcer(ServerLevel level) {
        this.level = level;
    }

    public Optional<BlockUtil.FoundRectangle> findPortalAround(BlockPos pos, boolean isEntrance,
            WorldBorder worldBorder) {
        int radius = isEntrance ? 16 : 48;
        Block portalBlock = ModBlocks.ELDERWOODS_PORTAL_BLOCK.get();

        int minBlockX = Math.max((int) worldBorder.getMinX() + 1, pos.getX() - radius);
        int maxBlockX = Math.min((int) worldBorder.getMaxX() - 1, pos.getX() + radius);
        int minBlockZ = Math.max((int) worldBorder.getMinZ() + 1, pos.getZ() - radius);
        int maxBlockZ = Math.min((int) worldBorder.getMaxZ() - 1, pos.getZ() + radius);

        int minChunkX = minBlockX >> 4;
        int maxChunkX = maxBlockX >> 4;
        int minChunkZ = minBlockZ >> 4;
        int maxChunkZ = maxBlockZ >> 4;

        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                ChunkAccess chunk = level.getChunk(cx, cz);
                BlockPos hit = findClosestPortalInChunk(chunk, pos, radius, worldBorder, portalBlock);
                if (hit != null) {
                    double distSq = hit.distSqr(pos);
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq;
                        bestPos = hit;
                    }
                }
            }
        }

        if (bestPos == null) {
            return Optional.empty();
        }

        BlockPos minCorner = locateBottomCorner(bestPos, portalBlock);
        return Optional.of(new BlockUtil.FoundRectangle(minCorner, PORTAL_WIDTH, PORTAL_HEIGHT));
    }

    private BlockPos findClosestPortalInChunk(ChunkAccess chunk, BlockPos origin, int radius,
            WorldBorder worldBorder, Block portalBlock) {
        LevelChunkSection[] sections = chunk.getSections();
        int minSectionY = chunk.getMinSection();
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        double maxDistSq = (double) radius * radius;

        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int sIdx = 0; sIdx < sections.length; sIdx++) {
            LevelChunkSection section = sections[sIdx];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(state -> state.is(portalBlock))) {
                continue;
            }
            int baseY = (minSectionY + sIdx) << 4;
            for (int ly = 0; ly < 16; ly++) {
                int y = baseY + ly;
                for (int lz = 0; lz < 16; lz++) {
                    int z = chunkMinZ + lz;
                    if (Math.abs(z - origin.getZ()) > radius) continue;
                    for (int lx = 0; lx < 16; lx++) {
                        int x = chunkMinX + lx;
                        if (Math.abs(x - origin.getX()) > radius) continue;
                        if (!section.getBlockState(lx, ly, lz).is(portalBlock)) continue;

                        cursor.set(x, y, z);
                        if (!worldBorder.isWithinBounds(cursor)) continue;

                        double distSq = cursor.distSqr(origin);
                        if (distSq <= maxDistSq && distSq < bestDistSq) {
                            bestDistSq = distSq;
                            best = cursor.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    private BlockPos locateBottomCorner(BlockPos portalBlockPos, Block portalBlock) {
        BlockState state = level.getBlockState(portalBlockPos);
        Direction.Axis axis = state.hasProperty(ElderwoodsPortalBlock.AXIS)
                ? state.getValue(ElderwoodsPortalBlock.AXIS)
                : Direction.Axis.X;
        Direction left = frameRightDirection(axis).getOpposite();

        BlockPos.MutableBlockPos corner = portalBlockPos.mutable();
        while (corner.getY() > level.getMinBuildHeight() + 1
                && level.getBlockState(corner.below()).is(portalBlock)) {
            corner.move(Direction.DOWN);
        }
        while (level.getBlockState(corner.relative(left)).is(portalBlock)) {
            corner.move(left);
        }
        return corner.immutable();
    }

    public Optional<BlockUtil.FoundRectangle> createPortal(BlockPos pos, Direction.Axis axis) {
        WorldBorder worldBorder = level.getWorldBorder();
        Direction right = frameRightDirection(axis);
        Direction forward = right.getClockWise();

        BlockPos targetPos = findSafeSurfacePos(pos, worldBorder, right, forward);
        boolean fallbackDock = targetPos == null;
        if (fallbackDock) {
            int fallbackX = clampToBorder(pos.getX(), worldBorder.getMinX(), worldBorder.getMaxX());
            int fallbackZ = clampToBorder(pos.getZ(), worldBorder.getMinZ(), worldBorder.getMaxZ());
            int safeY = Math.max(level.getSeaLevel() + 1,
                    level.getHeight(Heightmap.Types.WORLD_SURFACE, fallbackX, fallbackZ));
            safeY = Math.min(level.getMaxBuildHeight() - 7, safeY);
            targetPos = new BlockPos(fallbackX, safeY, fallbackZ);
        }

        prepareDoorwayAndFooting(targetPos, right, forward, fallbackDock);
        buildPortalFrame(targetPos, axis);

        // targetPos is j = 0 (bottom-left interior portal block), resting flush atop the j = -1 sill
        return Optional.of(new BlockUtil.FoundRectangle(targetPos.immutable(), PORTAL_WIDTH, PORTAL_HEIGHT));
    }

    private BlockPos findSafeSurfacePos(BlockPos origin, WorldBorder worldBorder,
            Direction right, Direction forward) {
        ServerChunkCache chunkSource = level.getChunkSource();
        ChunkGenerator generator = chunkSource.getGenerator();
        BiomeSource biomeSource = generator.getBiomeSource();
        RandomState randomState = chunkSource.randomState();
        int seaLevel = level.getSeaLevel();

        // Phase 1: Fine local spiral keeps portals close to 1:1 target coordinates on real ground
        BlockPos fineHit = spiralSearch(origin, worldBorder, right, forward,
                0, FINE_SEARCH_RADIUS, FINE_SEARCH_STEP, biomeSource, generator, randomState, seaLevel);
        if (fineHit != null) {
            return fineHit;
        }

        // Phase 2: Wide continental spiral escapes oceans, trenches, or dense giant-tree groves
        return spiralSearch(origin, worldBorder, right, forward,
                FINE_SEARCH_RADIUS + WIDE_SEARCH_STEP, WIDE_SEARCH_RADIUS, WIDE_SEARCH_STEP,
                biomeSource, generator, randomState, seaLevel);
    }

    private BlockPos spiralSearch(BlockPos origin, WorldBorder worldBorder, Direction right, Direction forward,
            int minRadius, int maxRadius, int step, BiomeSource biomeSource, ChunkGenerator generator,
            RandomState randomState, int seaLevel) {
        for (int radius = minRadius; radius <= maxRadius; radius += step) {
            if (radius == 0) {
                BlockPos centerHit = evaluateCandidate(origin.getX(), origin.getZ(), worldBorder,
                        right, forward, biomeSource, generator, randomState, seaLevel);
                if (centerHit != null) {
                    return centerHit;
                }
                continue;
            }
            for (int dx = -radius; dx <= radius; dx += step) {
                for (int dz = -radius; dz <= radius; dz += step) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    BlockPos hit = evaluateCandidate(origin.getX() + dx, origin.getZ() + dz, worldBorder,
                            right, forward, biomeSource, generator, randomState, seaLevel);
                    if (hit != null) {
                        return hit;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Zero-chunk-load precheck rejects aquatic biomes and below-sea-level basins via noise
     * before loading chunks to inspect real surface blocks (preventing server hangs).
     */
    private BlockPos evaluateCandidate(int x, int z, WorldBorder worldBorder, Direction right, Direction forward,
            BiomeSource biomeSource, ChunkGenerator generator, RandomState randomState, int seaLevel) {
        if (!isMarginWithinBorder(x, z, worldBorder)) {
            return null;
        }

        // Reject ocean/river biomes via BiomeSource BEFORE forcing any chunk generation
        if (isAquaticNoiseBiome(biomeSource, x, seaLevel, z, randomState)) {
            return null;
        }

        // Ensure candidate isn't a tiny 1-block sandspit surrounded on 3+ sides by ocean
        int dryNeighbors = 0;
        if (!isAquaticNoiseBiome(biomeSource, x + OCEAN_MARGIN_CHECK, seaLevel, z, randomState)) dryNeighbors++;
        if (!isAquaticNoiseBiome(biomeSource, x - OCEAN_MARGIN_CHECK, seaLevel, z, randomState)) dryNeighbors++;
        if (!isAquaticNoiseBiome(biomeSource, x, seaLevel, z + OCEAN_MARGIN_CHECK, randomState)) dryNeighbors++;
        if (!isAquaticNoiseBiome(biomeSource, x, seaLevel, z - OCEAN_MARGIN_CHECK, randomState)) dryNeighbors++;
        if (dryNeighbors < 2) {
            return null;
        }

        // Precheck raw generator floor height without loading chunk
        int floorEstimate = generator.getBaseHeight(
                x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
        if (floorEstimate < seaLevel - 1) {
            return null;
        }

        // Chunk passes noise filters -> inspect actual placed blocks to escape tree canopies/trunks/lakes
        return validateWorldSurfaceColumn(x, z, right, forward, seaLevel);
    }

    private BlockPos validateWorldSurfaceColumn(int x, int z, Direction right, Direction forward, int seaLevel) {
        int minColY = Integer.MAX_VALUE;
        int maxColY = Integer.MIN_VALUE;

        for (int i = -1; i <= 2; i++) {
            int cx = x + right.getStepX() * i;
            int cz = z + right.getStepZ() * i;

            if (isAquaticWorldBiome(cx, seaLevel, cz)) {
                return null;
            }

            OptionalInt foundY = findSolidGroundY(cx, cz, seaLevel);
            if (foundY.isEmpty()) {
                return null;
            }
            int gy = foundY.getAsInt();
            if (gy < minColY) minColY = gy;
            if (gy > maxColY) maxColY = gy;
            if (maxColY - minColY > 1) {
                return null;
            }
        }

        // Anchor the j = -1 Mana Crystal sill at maxColY so j = 0 clears all 4 columns above ground
        BlockPos doorBase = new BlockPos(x, maxColY + 1, z);
        if (!hasClearDoorwayVolume(doorBase, right, forward)) {
            return null;
        }

        return doorBase;
    }

    /**
     * Descends from {@link Heightmap.Types#MOTION_BLOCKING_NO_LEAVES} past any branches,
     * logs, giant mushroom caps, shroomlights, or foliage to locate true non-aquatic terrain.
     */
    private OptionalInt findSolidGroundY(int x, int z, int seaLevel) {
        int topSurfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        int startY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int scanStart = Math.min(level.getMaxBuildHeight() - 7, Math.max(topSurfaceY, startY));
        int minValidY = Math.max(level.getMinBuildHeight() + 4, seaLevel - 1);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = scanStart; y >= minValidY; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);

            // Reject any column holding standing water/lava on or above the landing floor
            if (!level.getFluidState(cursor).isEmpty()) {
                return OptionalInt.empty();
            }

            if (state.isAir() || isCanopyOrFoliage(state) || state.canBeReplaced()) {
                continue;
            }

            if (isDisqualifiedFoundation(state)) {
                return OptionalInt.empty();
            }

            if (state.isFaceSturdy(level, cursor, Direction.UP)
                    && level.getFluidState(cursor.above()).isEmpty()) {
                return OptionalInt.of(y);
            }
        }
        return OptionalInt.empty();
    }

    private boolean hasClearDoorwayVolume(BlockPos doorBase, Direction right, Direction forward) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // Check 4x5 doorway footprint: no fluids and no hard tree trunks/bedrock/stone walls inside the opening
        for (int i = -1; i <= 2; i++) {
            for (int j = 0; j <= 4; j++) {
                cursor.set(
                        doorBase.getX() + right.getStepX() * i,
                        doorBase.getY() + j,
                        doorBase.getZ() + right.getStepZ() * i);
                if (!level.getFluidState(cursor).isEmpty()) {
                    return false;
                }
                BlockState state = level.getBlockState(cursor);
                if (state.is(BlockTags.LOGS) || isDisqualifiedFoundation(state)) {
                    return false;
                }
                if (!state.isAir() && !state.canBeReplaced() && !isSoftTrimBlock(state)) {
                    return false;
                }
            }
        }

        // Check front and back stepping tiles (j = 0..2) so players don't emerge into water or a wall
        for (int side : new int[] { -1, 1 }) {
            for (int i = 0; i <= 1; i++) {
                int sx = doorBase.getX() + right.getStepX() * i + forward.getStepX() * side;
                int sz = doorBase.getZ() + right.getStepZ() * i + forward.getStepZ() * side;
                for (int j = 0; j <= 2; j++) {
                    cursor.set(sx, doorBase.getY() + j, sz);
                    if (!level.getFluidState(cursor).isEmpty()) {
                        return false;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (j <= 1 && !state.isAir() && !state.canBeReplaced() && !isSoftTrimBlock(state)) {
                        return false;
                    }
                }
                // Check footing beneath the step isn't liquid or a deep chasm
                cursor.set(sx, doorBase.getY() - 1, sz);
                if (!level.getFluidState(cursor).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void prepareDoorwayAndFooting(BlockPos doorBase, Direction right, Direction forward,
            boolean buildLandingDock) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // Trim overhanging leaves/foliage inside the 4x5 doorway and immediate front/back clearance
        for (int i = -1; i <= 2; i++) {
            for (int j = 0; j <= 4; j++) {
                cursor.set(
                        doorBase.getX() + right.getStepX() * i,
                        doorBase.getY() + j,
                        doorBase.getZ() + right.getStepZ() * i);
                if (!level.isEmptyBlock(cursor)) {
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            // Ensure solid foundation directly beneath the j = -1 frame sill (handles slope = 1 cleanly)
            cursor.set(
                    doorBase.getX() + right.getStepX() * i,
                    doorBase.getY() - 2,
                    doorBase.getZ() + right.getStepZ() * i);
            if (!level.getBlockState(cursor).isFaceSturdy(level, cursor, Direction.UP)) {
                level.setBlockAndUpdate(cursor, Blocks.STONE.defaultBlockState());
            }
        }

        for (int side : new int[] { -1, 1 }) {
            for (int i = -1; i <= 2; i++) {
                int sx = doorBase.getX() + right.getStepX() * i + forward.getStepX() * side;
                int sz = doorBase.getZ() + right.getStepZ() * i + forward.getStepZ() * side;
                for (int j = 0; j <= 2; j++) {
                    cursor.set(sx, doorBase.getY() + j, sz);
                    BlockState state = level.getBlockState(cursor);
                    if (isSoftTrimBlock(state) || !level.getFluidState(cursor).isEmpty() || buildLandingDock) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                cursor.set(sx, doorBase.getY() - 1, sz);
                if (buildLandingDock
                        || (!level.getBlockState(cursor).isFaceSturdy(level, cursor, Direction.UP)
                                && !level.getBlockState(cursor.below()).isFaceSturdy(level, cursor.below(), Direction.UP))) {
                    level.setBlockAndUpdate(cursor, ModBlocks.MANA_CRYSTAL_BLOCK.get().defaultBlockState());
                }
            }
        }
    }

    private static boolean isCanopyOrFoliage(BlockState state) {
        return state.getBlock() instanceof LeavesBlock
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.WART_BLOCKS)
                || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SNOW)
                || state.is(Blocks.SHROOMLIGHT)
                || state.is(Blocks.RED_MUSHROOM_BLOCK)
                || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.is(Blocks.MUSHROOM_STEM)
                || state.is(Blocks.VINE)
                || state.is(Blocks.MOSS_CARPET)
                || state.is(Blocks.GLOW_LICHEN);
    }

    private static boolean isSoftTrimBlock(BlockState state) {
        return state.getBlock() instanceof LeavesBlock
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SNOW)
                || state.is(Blocks.VINE)
                || state.is(Blocks.MOSS_CARPET)
                || state.is(Blocks.GLOW_LICHEN);
    }

    private static boolean isDisqualifiedFoundation(BlockState state) {
        return state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.WATER)
                || state.is(Blocks.BEDROCK);
    }

    private static boolean isAquaticBiome(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_OCEAN)
                || biome.is(BiomeTags.IS_DEEP_OCEAN)
                || biome.is(BiomeTags.IS_RIVER)
                || biome.is(ElderwoodsBiomeSource.THE_SILENT_OCEANS_KEY)
                || biome.is(ElderwoodsBiomeSource.THE_SILENT_DEPTHS_KEY)
                || biome.is(ElderwoodsBiomeSource.THE_SUNKEN_TRENCHES_KEY);
    }

    private static boolean isAquaticNoiseBiome(BiomeSource biomeSource, int x, int y, int z, RandomState randomState) {
        Holder<Biome> biome = biomeSource.getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), randomState.sampler());
        return isAquaticBiome(biome);
    }

    private boolean isAquaticWorldBiome(int x, int y, int z) {
        return isAquaticBiome(level.getBiome(new BlockPos(x, y, z)));
    }

    private static boolean isMarginWithinBorder(int x, int z, WorldBorder border) {
        int margin = 4;
        return x >= border.getMinX() + margin
                && x <= border.getMaxX() - margin
                && z >= border.getMinZ() + margin
                && z <= border.getMaxZ() - margin;
    }

    private static int clampToBorder(int coord, double min, double max) {
        int minC = (int) Math.ceil(min) + 4;
        int maxC = (int) Math.floor(max) - 4;
        if (minC > maxC) {
            return (minC + maxC) / 2;
        }
        return Math.max(minC, Math.min(maxC, coord));
    }

    private static Direction frameRightDirection(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
    }

    private void buildPortalFrame(BlockPos pos, Direction.Axis axis) {
        BlockState frame = ModBlocks.MANA_CRYSTAL_BLOCK.get().defaultBlockState();
        BlockState portal = ModBlocks.ELDERWOODS_PORTAL_BLOCK.get().defaultBlockState()
                .setValue(ElderwoodsPortalBlock.AXIS, axis);

        Direction right = frameRightDirection(axis);

        // First build the frame
        for (int i = -1; i <= 2; i++) {
            for (int j = -1; j <= 4; j++) {
                if (i == -1 || i == 2 || j == -1 || j == 4) {
                    BlockPos p = pos.relative(right, i).above(j);
                    level.setBlockAndUpdate(p, frame);
                }
            }
        }

        // Then place the portal blocks inside using flag 18 to avoid breaking due to partial updates
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 3; j++) {
                BlockPos p = pos.relative(right, i).above(j);
                level.setBlock(p, portal, 18);
            }
        }
    }
}
