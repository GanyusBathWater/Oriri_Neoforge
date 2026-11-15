package net.ganyusbathwater.oririmod.worldgen.tree;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.block.custom.MoonshroomBlock;
import net.ganyusbathwater.oririmod.block.custom.UpgradedSaplingBlock;
import net.ganyusbathwater.oririmod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class ElderGiantTreeFeature extends Feature<ElderGiantTreeConfig> {

    private static final Predicate<BlockState> PROTECTED_STRUCTURE_PREDICATE =
            state -> state.is(ModTags.Blocks.ELDERWOODS_PROTECTED_STRUCTURE_BLOCKS);

    private static final BooleanProperty[] VINE_SIDES = {
            VineBlock.NORTH, VineBlock.SOUTH, VineBlock.EAST, VineBlock.WEST
    };

    private static final Direction[] ALL_DIRS = Direction.values();

    private static final int FLOWER_RADIUS = 6;
    private static final int MAX_FLOWER_MASK_VOL = 4_000_000;

    private final ThreadLocal<TreeWorkState> workStateLocal = ThreadLocal.withInitial(TreeWorkState::new);

    private static final class TreeWorkState {
        WorldGenLevel level;
        boolean forcingGrowth;
        boolean isWorldGenRegion;
        int centerChunkX;
        int centerChunkZ;
        int writeMinX;
        int writeMaxX;
        int writeMinZ;
        int writeMaxZ;
        int minY;
        int maxY;
        final ChunkAccess[] chunkGrid = new ChunkAccess[9];
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        final LongOpenHashSet placedBranchLogs = new LongOpenHashSet(256);
        final LongOpenHashSet placedAllLogs = new LongOpenHashSet(512);
        final Long2ByteOpenHashMap placedLogAxes = new Long2ByteOpenHashMap(512);
        final LongOpenHashSet plannedLeaves = new LongOpenHashSet(4096);
        final LongOpenHashSet branchEndpoints = new LongOpenHashSet(64);
        final LongOpenHashSet rejectedLeafSpots = new LongOpenHashSet(4096);
        final LongOpenHashSet crownPlanned = new LongOpenHashSet(4096);
        final LongOpenHashSet crownNext = new LongOpenHashSet(4096);
        final LongOpenHashSet knownWorldLogs = new LongOpenHashSet(64);
        final Long2IntOpenHashMap leafDistances = new Long2IntOpenHashMap(4096);
        final Long2ByteOpenHashMap extNeighborCache = new Long2ByteOpenHashMap(2048);
        final LongArrayList[] bfsBuckets = new LongArrayList[7];
        byte[] flowerMask = new byte[0];
        final RandomSource reusableRnd = RandomSource.create(0L);

        int canopyCenterY;
        int canopyRadius;
        int trunkOriginX;
        int trunkOriginZ;
        int trunkSize = 1;

        TreeWorkState() {
            this.placedLogAxes.defaultReturnValue((byte) -1);
            this.leafDistances.defaultReturnValue(7);
            this.extNeighborCache.defaultReturnValue((byte) 0);
            for (int i = 0; i < this.bfsBuckets.length; i++) {
                this.bfsBuckets[i] = new LongArrayList(256);
            }
        }

        void bindLevel(WorldGenLevel level) {
            this.level = level;
            this.forcingGrowth = UpgradedSaplingBlock.IS_FORCING_GROWTH.get();
            this.minY = level.getMinBuildHeight();
            this.maxY = level.getMaxBuildHeight() - 1;
            Arrays.fill(this.chunkGrid, null);
            if (level instanceof WorldGenRegion region) {
                this.isWorldGenRegion = true;
                ChunkPos center = region.getCenter();
                this.centerChunkX = center.x;
                this.centerChunkZ = center.z;
                // WorldGenRegion FEATURES step allows writing within center +-1 chunks (48x48 area)
                this.writeMinX = (center.x - 1) << 4;
                this.writeMaxX = this.writeMinX + 47;
                this.writeMinZ = (center.z - 1) << 4;
                this.writeMaxZ = this.writeMinZ + 47;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int cx = center.x + dx;
                        int cz = center.z + dz;
                        if (region.hasChunk(cx, cz)) {
                            this.chunkGrid[(dx + 1) * 3 + (dz + 1)] = region.getChunk(cx, cz);
                        }
                    }
                }
            } else {
                this.isWorldGenRegion = false;
            }
        }

        void clearTreeData() {
            this.placedBranchLogs.clear();
            this.placedAllLogs.clear();
            this.placedLogAxes.clear();
            this.plannedLeaves.clear();
            this.branchEndpoints.clear();
            this.rejectedLeafSpots.clear();
            this.crownPlanned.clear();
            this.crownNext.clear();
            this.knownWorldLogs.clear();
            this.leafDistances.clear();
            this.extNeighborCache.clear();
            for (LongArrayList bucket : this.bfsBuckets) {
                bucket.clear();
            }
        }

        void unbind() {
            clearTreeData();
            this.level = null;
            Arrays.fill(this.chunkGrid, null);
        }

        boolean withinHeight(int y) {
            return y >= this.minY && y <= this.maxY;
        }

        boolean canWrite(int x, int y, int z) {
            if (y < this.minY || y > this.maxY) {
                return false;
            }
            if (this.isWorldGenRegion) {
                return x >= this.writeMinX && x <= this.writeMaxX && z >= this.writeMinZ && z <= this.writeMaxZ;
            }
            return isChunkLoaded(this.level, x, z);
        }

        BlockState getBlockState(int x, int y, int z) {
            if (this.isWorldGenRegion) {
                int gx = (x >> 4) - this.centerChunkX + 1;
                int gz = (z >> 4) - this.centerChunkZ + 1;
                if ((gx | gz) >= 0 && gx < 3 && gz < 3) {
                    ChunkAccess chunk = this.chunkGrid[gx * 3 + gz];
                    if (chunk != null) {
                        return chunk.getBlockState(this.cursor.set(x, y, z));
                    }
                }
            }
            return this.level.getBlockState(this.cursor.set(x, y, z));
        }

        boolean isNearTrunk(int x, int z, int padding) {
            int minX = this.trunkOriginX - padding;
            int maxX = this.trunkOriginX + this.trunkSize - 1 + padding;
            int minZ = this.trunkOriginZ - padding;
            int maxZ = this.trunkOriginZ + this.trunkSize - 1 + padding;
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static double mixDouble(long z) {
        return (mix64(z) >>> 11) * 0x1.0p-53;
    }

    public ElderGiantTreeFeature(Codec<ElderGiantTreeConfig> codec) {
        super(codec);
    }

    private static final Direction[] CARDINAL_DIRECTIONS = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private static final BlockPos[] DIAGONAL_DIRECTIONS = {
            new BlockPos(1, 0, 1), // SOUTH_EAST
            new BlockPos(1, 0, -1), // NORTH_EAST
            new BlockPos(-1, 0, 1), // SOUTH_WEST
            new BlockPos(-1, 0, -1) // NORTH_WEST
    };

    private BlockPos randomEightDir(RandomSource rnd) {
        // 0..3: Kardinal, 4..7: diagonal
        int idx = rnd.nextInt(8);
        if (idx < 4) {
            Direction d = CARDINAL_DIRECTIONS[idx];
            return new BlockPos(d.getStepX(), 0, d.getStepZ());
        } else {
            return DIAGONAL_DIRECTIONS[idx - 4];
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<ElderGiantTreeConfig> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rnd = ctx.random();
        BlockPos origin = ctx.origin();
        ElderGiantTreeConfig cfg = ctx.config();

        // Sample twice and take the maximum to bias towards larger sizes
        int rawHeight = clamp(Math.max(cfg.trunkHeight().sample(rnd), cfg.trunkHeight().sample(rnd)), 8, 96);
        int rawTrunk = clamp(Math.max(cfg.trunkRadius().sample(rnd), cfg.trunkRadius().sample(rnd)), 1, 5);
        int rawCanopy = clamp(Math.max(cfg.canopyRadius().sample(rnd), cfg.canopyRadius().sample(rnd)), 3, 18);
        int rawBranch = clamp(Math.max(cfg.branchLength().sample(rnd), cfg.branchLength().sample(rnd)), 3, 12);

        int maxHeightForTrunk = clamp(8 + rawTrunk * 12, 8, 96);
        int height = Math.min(rawHeight, maxHeightForTrunk);

        int trunkSize = rawTrunk;

        int canopyFromHeight = clamp(3 + height / 6, 3, 18);
        int canopyFromTrunk = clamp(trunkSize * 3, 3, 18);
        int canopyR = clamp((int) Math.round(rawCanopy * 0.4 + canopyFromHeight * 0.4 + canopyFromTrunk * 0.2), 3, 18);

        int branchLenBase = clamp((int) Math.round(rawBranch * (1.0 + trunkSize / 8.0)), 3, 12);

        // Run O(1) bounds, border, and substrate checks before scanning ~100k canopy blocks
        int localX = Math.floorMod(origin.getX(), 16);
        int localZ = Math.floorMod(origin.getZ(), 16);
        boolean forcingGrowth = UpgradedSaplingBlock.IS_FORCING_GROWTH.get();
        if (!forcingGrowth) {
            if (localX == 0 || localX == 15 || localZ == 0 || localZ == 15) {
                return false;
            }
        }

        if (origin.getY() + height + canopyR + 2 >= level.getMaxBuildHeight())
            return false;
        if (origin.getY() <= level.getMinBuildHeight() + 1)
            return false;

        TreeWorkState state = workStateLocal.get();
        state.bindLevel(level);
        try {
            if (!forcingGrowth) {
                BlockState ground = state.getBlockState(origin.getX(), origin.getY() - 1, origin.getZ());
                if (!ground.is(Blocks.GRASS_BLOCK) && !ground.is(ModBlocks.SCARLET_MOSS.get())) {
                    return false;
                }
            }

            int ox = origin.getX();
            int oy = origin.getY();
            int oz = origin.getZ();
            for (int y = 0; y <= height; y++) {
                for (int dx = 0; dx < trunkSize; dx++) {
                    for (int dz = 0; dz < trunkSize; dz++) {
                        if (!canReplaceForLog(state, ox + dx, oy + y, oz + dz))
                            return false;
                    }
                }
            }

            if (containsNonReplaceableBlocks(level, origin, height, trunkSize, canopyR))
                return false;

            long baseSeed = rnd.nextLong();
            BlockPos top = origin.offset((trunkSize - 1) / 2, height, (trunkSize - 1) / 2);

            if (!state.isWorldGenRegion && !areChunksLoaded(level, top, canopyR)) {
                if (level instanceof ServerLevel serverLevel) {
                    int sHeight = height;
                    int sTrunkSize = trunkSize;
                    int sCanopyR = canopyR;
                    int sBranchLenBase = branchLenBase;
                    long sBaseSeed = baseSeed;
                    BlockPos sOrigin = origin.immutable();
                    ElderGiantTreeConfig sCfg = cfg;
                    long deferredRndSeed = rnd.nextLong();

                    serverLevel.getServer().execute(() -> {
                        try {
                            RandomSource deferredRnd = RandomSource.create(deferredRndSeed);
                            generateTree(serverLevel, sCfg, sOrigin, deferredRnd, sHeight, sTrunkSize, sCanopyR,
                                    sBranchLenBase, sBaseSeed, true);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                    return true;
                } else {
                    return false;
                }
            }

            return generateTree(level, cfg, origin, rnd, height, trunkSize, canopyR, branchLenBase, baseSeed, false);
        } finally {
            state.unbind();
        }
    }

    private boolean generateTree(WorldGenLevel level, ElderGiantTreeConfig cfg, BlockPos origin, RandomSource rnd,
            int height, int trunkSize, int canopyR, int branchLenBase, long baseSeed, boolean checkNonReplaceable) {

        TreeWorkState state = workStateLocal.get();
        boolean boundHere = (state.level != level);
        if (boundHere) {
            state.bindLevel(level);
        }

        try {
            if (checkNonReplaceable && containsNonReplaceableBlocks(level, origin, height, trunkSize, canopyR))
                return false;

            state.clearTreeData();
            state.trunkOriginX = origin.getX();
            state.trunkOriginZ = origin.getZ();
            state.trunkSize = trunkSize;

            if (origin.getY() + height + canopyR + 2 >= level.getMaxBuildHeight())
                return false;
            if (origin.getY() <= level.getMinBuildHeight() + 1)
                return false;

            BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
            for (int y = 0; y < height; y++) {
                for (int dx = 0; dx < trunkSize; dx++) {
                    for (int dz = 0; dz < trunkSize; dz++) {
                        mPos.set(origin.getX() + dx, origin.getY() + y, origin.getZ() + dz);
                        placeLog(level, state, mPos, Direction.Axis.Y, cfg, rnd, false);
                    }
                }
            }

            genPerimeterRoots(level, state, origin, trunkSize, cfg, rnd);

            int branches = 5 + trunkSize * 2 + rnd.nextInt(5);
            int branchStartMin = Math.max(4, height / 3);
            int branchStartMax = Math.max(branchStartMin + 1, height - 6);
            int canopyCenterY = origin.getY() + height;
            for (int i = 0; i < branches; i++) {
                int by = branchStartMin + rnd.nextInt(Math.max(1, branchStartMax - branchStartMin));
                BlockPos dirVec = randomEightDir(rnd);

                // kleine Sicherheit: falls die erste Position sofort mit anderem Ast
                // kollidiert, wähle neue Richtung
                int tries = 0;
                while (tries < 6) {
                    int fx = origin.getX() + rnd.nextInt(trunkSize) + dirVec.getX();
                    int fy = origin.getY() + by;
                    int fz = origin.getZ() + rnd.nextInt(trunkSize) + dirVec.getZ();
                    long fKey = BlockPos.asLong(fx, fy, fz);
                    boolean conflict = state.placedBranchLogs.contains(fKey);
                    if (!conflict) {
                        for (Direction d : CARDINAL_DIRECTIONS) {
                            if (state.placedBranchLogs.contains(BlockPos.asLong(fx + d.getStepX(), fy, fz + d.getStepZ()))) {
                                conflict = true;
                                break;
                            }
                        }
                    }
                    if (!conflict)
                        break;
                    dirVec = randomEightDir(rnd);
                    tries++;
                }

                int len = branchLenBase + rnd.nextInt(3) + trunkSize / 2;

                int stepX = dirVec.getX();
                int stepZ = dirVec.getZ();
                if (stepX != 0 && stepZ != 0) {
                    // 0.6–0.8 der normalen Länge
                    double factor = 0.6 + rnd.nextDouble() * 0.2;
                    len = Math.max(2, (int) Math.round(len * factor));
                }

                BlockPos start = origin.offset(rnd.nextInt(trunkSize), by, rnd.nextInt(trunkSize));
                genBranch(level, state, start, dirVec, len, cfg, rnd, canopyCenterY, canopyR);
            }

            if (canopyR >= 6) {
                int internalBranches = 3 + trunkSize;
                for (int i = 0; i < internalBranches; i++) {
                    int by = height - 4 - rnd.nextInt(Math.max(1, height / 6));
                    int sx = trunkSize / 2;
                    int sz = trunkSize / 2;
                    double angle = rnd.nextDouble() * Math.PI * 2;
                    int len = canopyR / 2 + rnd.nextInt(canopyR / 2 + 1);
                    genInternalBranch(level, state, origin.offset(sx, by, sz), angle, len, cfg, rnd);
                }
                genCentralSupports(level, state, origin, trunkSize, height, canopyR, cfg, rnd);
            }

            BlockPos top = origin.offset((trunkSize - 1) / 2, height, (trunkSize - 1) / 2);

            // Werte für Spore Blossoms merken
            state.canopyCenterY = top.getY();
            state.canopyRadius = canopyR;

            // geordnete Kronenäste vom Stammpunkt nach außen
            int crownBranchCount = Math.min(8, 3 + canopyR / 3);
            List<BlockPos> crownDirs = computeNonAdjacentCrownDirs(rnd, crownBranchCount);
            for (BlockPos dir : crownDirs) {
                int len = 2 + rnd.nextInt(Math.max(2, canopyR / 2));
                genCrownBranchFromVector(level, state, top, dir, len, canopyR, cfg, rnd);
            }

            int finalCanopyR = canopyR;
            if (!state.isWorldGenRegion && !areChunksLoaded(level, top, canopyR)) {
                finalCanopyR = clampRadiusToLoadedChunks(level, top, canopyR);
                if (finalCanopyR <= 0) {
                    ensureLeavesAroundBranchLogs(state, top.getY(), finalCanopyR);
                    return true;
                }
            }

            genIrregularCrown(level, state, top, finalCanopyR, cfg, baseSeed);

            ensureLeavesAroundBranchLogs(state, top.getY(), finalCanopyR);
            finalizePlannedLeaves(level, state, cfg, baseSeed);

            // Replace logs with stems where ends are exposed
            replaceExposedLogsWithStems(level, state, cfg);

            if (cfg.placeMoonshrooms()) {
                placeMoonshrooms(level, state, cfg, rnd, origin);
            }

            if (cfg.placeSporeBlossoms() && rnd.nextInt(3) == 0) {
                placeSporeBlossom(level, state, cfg, rnd);
            }

            // Scarlet trees use Scarlet Vines under leaves (finalizePlannedLeaves) instead
            // of Hanging Elder Moss.
            if (cfg.floweringChance() > 0.0f) {
                placeHangingMoss(level, state, rnd);
            }

            return true;
        } finally {
            if (boundHere) {
                state.unbind();
            } else {
                state.clearTreeData();
            }
        }
    }

    private void replaceExposedLogsWithStems(WorldGenLevel level, TreeWorkState state, ElderGiantTreeConfig cfg) {
        if (cfg.stemProvider() == null || state.placedLogAxes.isEmpty())
            return;

        Direction.Axis[] axes = Direction.Axis.values();
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        LongIterator it = state.placedLogAxes.keySet().iterator();
        while (it.hasNext()) {
            long posKey = it.nextLong();
            byte axisIdx = state.placedLogAxes.get(posKey);
            if (axisIdx < 0 || axisIdx >= axes.length)
                continue;

            int px = BlockPos.getX(posKey);
            int py = BlockPos.getY(posKey);
            int pz = BlockPos.getZ(posKey);
            if (!state.canWrite(px, py, pz))
                continue;

            Direction.Axis axis = axes[axisIdx];
            int dx = axis == Direction.Axis.X ? 1 : 0;
            int dy = axis == Direction.Axis.Y ? 1 : 0;
            int dz = axis == Direction.Axis.Z ? 1 : 0;

            boolean exposed = !state.placedAllLogs.contains(BlockPos.asLong(px + dx, py + dy, pz + dz))
                    || !state.placedAllLogs.contains(BlockPos.asLong(px - dx, py - dy, pz - dz));

            if (exposed) {
                mPos.set(px, py, pz);
                BlockState stemState = cfg.stemProvider().getState(level.getRandom(), mPos);
                if (stemState.hasProperty(RotatedPillarBlock.AXIS)) {
                    stemState = stemState.setValue(RotatedPillarBlock.AXIS, axis);
                }
                setBlock(level, mPos, stemState);
            }
        }
    }

    private void placeMoonshrooms(WorldGenLevel level, TreeWorkState state, ElderGiantTreeConfig cfg, RandomSource rnd,
            BlockPos origin) {
        if (rnd.nextInt(2) != 0)
            return; // 50% chance to have moonshrooms at all

        int maxBaseY = origin.getY() + 4;
        LongArrayList baseLogs = new LongArrayList();
        LongIterator it = state.placedAllLogs.iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            if (BlockPos.getY(key) <= maxBaseY) {
                baseLogs.add(key);
            }
        }

        // Fisher-Yates shuffle on primitive long list
        for (int i = baseLogs.size() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            long tmp = baseLogs.getLong(i);
            baseLogs.set(i, baseLogs.getLong(j));
            baseLogs.set(j, tmp);
        }

        int toPlace = 1 + rnd.nextInt(3);
        int placed = 0;
        int setFlag = level instanceof ServerLevel ? 3 : 2;
        Direction[] dirs = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();

        for (int idx = 0, n = baseLogs.size(); idx < n; idx++) {
            long posKey = baseLogs.getLong(idx);
            int px = BlockPos.getX(posKey);
            int py = BlockPos.getY(posKey);
            int pz = BlockPos.getZ(posKey);

            // Shuffle 4 horizontal directions inline
            for (int i = 3; i > 0; i--) {
                int j = rnd.nextInt(i + 1);
                Direction tmp = dirs[i];
                dirs[i] = dirs[j];
                dirs[j] = tmp;
            }

            for (Direction dir : dirs) {
                int tx = px + dir.getStepX();
                int tz = pz + dir.getStepZ();
                if (!state.canWrite(tx, py, tz))
                    continue;
                if (state.getBlockState(tx, py, tz).isAir()) {
                    targetPos.set(tx, py, tz);
                    level.setBlock(targetPos,
                            ModBlocks.MOONSHROOM_BLOCK.get().defaultBlockState()
                                    .setValue(MoonshroomBlock.FACING, dir),
                            setFlag);
                    placed++;
                    break; // Move to next log
                }
            }
            if (placed >= toPlace)
                break;
        }
    }

    private void placeSporeBlossom(WorldGenLevel level, TreeWorkState state, ElderGiantTreeConfig cfg, RandomSource rnd) {
        if (state.placedBranchLogs.isEmpty())
            return;

        LongArrayList branchList = new LongArrayList(state.placedBranchLogs.size());
        LongIterator it = state.placedBranchLogs.iterator();
        while (it.hasNext()) {
            branchList.add(it.nextLong());
        }
        for (int i = branchList.size() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            long tmp = branchList.getLong(i);
            branchList.set(i, branchList.getLong(j));
            branchList.set(j, tmp);
        }

        BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
        for (int i = 0, n = branchList.size(); i < n; i++) {
            long posKey = branchList.getLong(i);
            int bx = BlockPos.getX(posKey);
            int by = BlockPos.getY(posKey) - 1;
            int bz = BlockPos.getZ(posKey);
            if (!state.canWrite(bx, by, bz))
                continue;
            if (state.getBlockState(bx, by, bz).isAir()) {
                belowPos.set(bx, by, bz);
                level.setBlock(belowPos, ModBlocks.ELDER_SPORE_BLOSSOM.get().defaultBlockState(), 2);
                break;
            }
        }
    }

    private void placeLog(WorldGenLevel level, TreeWorkState state, BlockPos pos, Direction.Axis axis,
            ElderGiantTreeConfig cfg, RandomSource rnd, boolean markBranch) {
        placeLog(level, state, pos, axis, cfg, rnd, markBranch, cfg.logProvider());
    }

    private void placeLog(WorldGenLevel level, TreeWorkState state, BlockPos pos, Direction.Axis axis,
            ElderGiantTreeConfig cfg, RandomSource rnd, boolean markBranch, BlockStateProvider provider) {
        int px = pos.getX();
        int py = pos.getY();
        int pz = pos.getZ();
        if (!state.canWrite(px, py, pz)) {
            return;
        }

        BlockState blockState = provider.getState(rnd, pos);
        if (blockState.hasProperty(RotatedPillarBlock.AXIS)) {
            blockState = blockState.setValue(RotatedPillarBlock.AXIS, axis);
        }

        boolean placeInWorld = true;
        if (state.forcingGrowth) {
            BlockState current = state.getBlockState(px, py, pz);
            boolean replaceable = current.isAir() || current.is(BlockTags.LEAVES) || current.canBeReplaced()
                    || current.is(Blocks.GRASS_BLOCK) || current.is(Blocks.TALL_GRASS);
            if (!replaceable) {
                placeInWorld = false;
            }
        }

        if (placeInWorld) {
            level.setBlock(pos, blockState, level instanceof ServerLevel ? 3 : 2);
        }

        long key = pos.asLong();
        state.placedAllLogs.add(key);
        if (placeInWorld && blockState.hasProperty(RotatedPillarBlock.AXIS)) {
            state.placedLogAxes.put(key, (byte) axis.ordinal());
        }
        if (markBranch) {
            state.placedBranchLogs.add(key);
            tryPlaceSporeBlossomUnderBranch(level, state, px, py, pz, cfg, rnd);
        }
    }

    private void genPerimeterRoots(WorldGenLevel level, TreeWorkState state, BlockPos origin, int trunkSize,
            ElderGiantTreeConfig cfg, RandomSource rnd) {
        int min = -1;
        int max = trunkSize;
        List<BlockPos> starts = new ArrayList<>();

        // collect border positions once to avoid duplicate logic
        for (int x = min; x <= max; x++) {
            starts.add(origin.offset(x, 0, min));
            starts.add(origin.offset(x, 0, max));
        }
        for (int z = min + 1; z <= max - 1; z++) {
            starts.add(origin.offset(min, 0, z));
            starts.add(origin.offset(max, 0, z));
        }

        int centerOffset = (trunkSize - 1) / 2;
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        for (BlockPos start : starts) {
            // gelegentlich verankern oder kleine vertikale Stummel
            if (measureGroundDistance(state, start, 6) > 0 && rnd.nextDouble() < 0.8) {
                start = anchorRootToGround(level, state, start, 6, cfg, rnd);
            } else {
                for (int h = 0; h > -2; h--) {
                    int py = start.getY() + h;
                    if (!state.withinHeight(py))
                        break;
                    int px = start.getX();
                    int pz = start.getZ();
                    if (canReplaceForLog(state, px, py, pz)) {
                        mPos.set(px, py, pz);
                        placeLog(level, state, mPos, Direction.Axis.Y, cfg, rnd, false, cfg.rootProvider());
                    }
                }
            }

            // bewusst sehr kurz: nur 1 Schritt nach außen, damit Wurzeln nicht länger
            // werden
            int len = 1;
            int relX = start.getX() - origin.getX();
            int relZ = start.getZ() - origin.getZ();
            int dirX = Integer.signum(relX - centerOffset);
            int dirZ = Integer.signum(relZ - centerOffset);
            if (dirX == 0 && dirZ == 0) {
                int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };
                int[] d = dirs[rnd.nextInt(dirs.length)];
                dirX = d[0];
                dirZ = d[1];
            }
            genRootOutward(level, state, start, dirX, dirZ, len, cfg, rnd);
        }
    }

    private void genRootOutward(WorldGenLevel level, TreeWorkState state, BlockPos start, int dx, int dz, int len,
            ElderGiantTreeConfig cfg, RandomSource rnd) {
        double x = start.getX();
        double y = start.getY();
        double z = start.getZ();
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < len; i++) {
            // deutlich kleinere horizontale Schritte, damit Wurzeln kompakter bleiben
            x += dx * (0.4 + rnd.nextDouble() * 0.4);
            z += dz * (0.4 + rnd.nextDouble() * 0.4);

            BlockPos probe = new BlockPos((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
            int drop = measureGroundDistance(state, probe, 8);

            if (drop > 0) {
                // wenn Boden unter dem Pfad ist, mit hoher Wahrscheinlichkeit direkt verankern
                if (rnd.nextDouble() < 0.9) {
                    BlockPos anchored = anchorRootToGround(level, state, probe, Math.min(4, drop + 1), cfg, rnd);
                    // setze Koordinaten auf verankerten Punkt und beende die Ausweitung (keine
                    // lange Verlängerung)
                    x = anchored.getX();
                    y = anchored.getY();
                    z = anchored.getZ();
                    // gelegentlich nur einen kurzen Seitenausläufer nach dem Verankern
                    if (rnd.nextDouble() < 0.25) {
                        int sx = rnd.nextBoolean() ? 1 : -1;
                        int sz = rnd.nextBoolean() ? 1 : -1;
                        genRootOutward(level, state, anchored, sx, sz, 1, cfg, rnd);
                    }
                    // nach Verankerung nicht weiter horizontal fortsetzen
                    break;
                } else {
                    // seltene moderate Abstufung, aber maximal 1 Block runter, um Sprünge zu
                    // vermeiden
                    y -= 1.0;
                }
            } else {
                // kleinere Wahrscheinlichkeit, 1 Block abzusinken, sonst gerade weiter
                if (rnd.nextDouble() < 0.35)
                    y -= 1;
            }

            BlockPos p = new BlockPos((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
            if (!state.withinHeight(p.getY()))
                break;

            // Falls unter dem geplanten Punkt ein längerer Leerraum ist, verankere kurz,
            // bevor du einen einzelnen Block setzt
            int belowGap = measureGroundDistance(state, p, 4);
            if (belowGap > 1) {
                BlockPos anchored = anchorRootToGround(level, state, p, Math.min(4, belowGap + 1), cfg, rnd);
                p = anchored;
                x = p.getX();
                y = p.getY();
                z = p.getZ();
            }

            int px = p.getX();
            int py = p.getY();
            int pz = p.getZ();
            if (canReplaceForLog(state, px, py, pz)) {
                Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X
                        : (Math.abs(dz) > Math.abs(dx) ? Direction.Axis.Z : Direction.Axis.Y);
                placeLog(level, state, p, axis, cfg, rnd, false, cfg.rootProvider());

                // reduzierte seitliche Logs, damit Wurzel schlank bleibt
                for (Direction s : CARDINAL_DIRECTIONS) {
                    if (rnd.nextDouble() < 0.25) {
                        int sx = px + s.getStepX();
                        int sz = pz + s.getStepZ();
                        if (state.withinHeight(py) && canReplaceForLog(state, sx, py, sz)) {
                            mPos.set(sx, py, sz);
                            placeLog(level, state, mPos, Direction.Axis.Y, cfg, rnd, false, cfg.rootProvider());
                        }
                    }
                }

                // sehr selten kleine 3x3-Cluster, um lokale Dicke zu geben
                if (rnd.nextDouble() < 0.08) {
                    placeRootCluster(level, state, p, 1, cfg, rnd, false);
                }
            }

            // kleine Chance für einen kurzen Seitenausläufer, nicht rekursive Verlängerung
            if (rnd.nextDouble() < 0.12) {
                Direction sideDir = Direction.Plane.HORIZONTAL.getRandomDirection(rnd);
                int sx = px + sideDir.getStepX();
                int sz = pz + sideDir.getStepZ();
                if (state.withinHeight(py) && canReplaceForLog(state, sx, py, sz)) {
                    mPos.set(sx, py, sz);
                    placeLog(level, state, mPos, Direction.Axis.Y, cfg, rnd, false, cfg.rootProvider());
                }
            }
        }
    }

    private BlockPos anchorRootToGround(WorldGenLevel level, TreeWorkState state, BlockPos start, int maxDepth,
            ElderGiantTreeConfig cfg, RandomSource rnd) {
        int px = start.getX();
        int py = start.getY();
        int pz = start.getZ();
        if (!state.withinHeight(py))
            return start;
        int placed = 0;
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        // immer durchgehend nach unten setzen, bis nicht mehr ersetzbar oder maxDepth
        // erreicht
        for (int d = 0; d < maxDepth; d++) {
            if (!state.withinHeight(py))
                break;
            int belowY = py - 1;
            boolean belowReplaceable = false;
            if (state.withinHeight(belowY)) {
                BlockState bs = state.getBlockState(px, belowY, pz);
                belowReplaceable = bs.isAir() || bs.is(BlockTags.REPLACEABLE)
                        || bs.is(BlockTags.LEAVES) || !bs.getFluidState().isEmpty();
            }
            if (canReplaceForLog(state, px, py, pz)) {
                mPos.set(px, py, pz);
                placeLog(level, state, mPos, Direction.Axis.Y, cfg, rnd, false, cfg.rootProvider());
                placed++;
            }
            if (!belowReplaceable) {
                return new BlockPos(px, py, pz);
            }
            py--;
        }
        if (placed > 0)
            return new BlockPos(px, py + 1, pz);
        return start;
    }

    private int measureGroundDistance(TreeWorkState state, BlockPos pos, int max) {
        int count = 0;
        int px = pos.getX();
        int py = pos.getY() - 1;
        int pz = pos.getZ();
        for (int i = 0; i < max; i++) {
            if (!state.withinHeight(py))
                break;
            BlockState s = state.getBlockState(px, py, pz);
            if (s.isAir() || s.is(BlockTags.REPLACEABLE) || s.is(BlockTags.LEAVES) || s.is(Blocks.TALL_GRASS)
                    || !s.getFluidState().isEmpty()) {
                count++;
                py--;
            } else {
                break;
            }
        }
        return count;
    }

    private void genInternalBranch(WorldGenLevel level, TreeWorkState state, BlockPos start, double angle, int len,
            ElderGiantTreeConfig cfg, RandomSource rnd) {
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);
        double ystep = 0.2 + rnd.nextDouble() * 0.6;
        double x = start.getX();
        double y = start.getY();
        double z = start.getZ();
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < len; i++) {
            x += dx;
            y += ystep;
            z += dz;
            int px = (int) Math.round(x);
            int py = (int) Math.round(y);
            int pz = (int) Math.round(z);
            if (!state.withinHeight(py))
                break;
            if (canReplaceForLog(state, px, py, pz)) {
                mPos.set(px, py, pz);
                placeLog(level, state, mPos, Direction.Axis.X, cfg, rnd, true);
                if (rnd.nextDouble() < 0.35) {
                    for (Direction d : CARDINAL_DIRECTIONS) {
                        int sx = px + d.getStepX();
                        int sz = pz + d.getStepZ();
                        if (canReplaceForLog(state, sx, py, sz)) {
                            mPos.set(sx, py, sz);
                            placeLog(level, state, mPos, Direction.Axis.Y, cfg, rnd, true);
                        }
                    }
                }
            }
            if (i >= len - 2 && rnd.nextFloat() < 0.35f)
                placeLeafCross(state, px, py, pz);
        }
        genLeafBlob(state, (int) Math.round(x), (int) Math.round(y), (int) Math.round(z),
                2 + rnd.nextInt(2), rnd, false);
    }

    private void genIrregularCrown(WorldGenLevel level, TreeWorkState state, BlockPos center, int canopyRadius,
            ElderGiantTreeConfig cfg, long baseSeed) {
        int below = Math.max(1, canopyRadius / 3);
        int above = canopyRadius;
        long centerLong = center.asLong();
        RandomSource global = RandomSource.create(baseSeed ^ centerLong);

        LongOpenHashSet planned = state.crownPlanned;
        planned.clear();
        double roundWeightBase = 0.80;

        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        for (int dy = -below; dy <= above; dy++) {
            int wy = cy + dy;
            if (!state.withinHeight(wy))
                continue;
            int depth = dy + below;
            double taper = (double) depth / Math.max(1, (above + below));
            int layerRadius = Math.max(1, (int) Math.round(canopyRadius * (1.0 - taper * 0.20)));
            double roundWeight = roundWeightBase + (global.nextDouble() - 0.5) * 0.12;

            int search = layerRadius + 3;
            for (int dx = -search; dx <= search; dx++) {
                int wx = cx + dx;
                for (int dz = -search; dz <= search; dz++) {
                    double rx = dx + (global.nextDouble() - 0.5) * 0.9;
                    double rz = dz + (global.nextDouble() - 0.5) * 0.9;
                    double eu = Math.hypot(rx, rz);
                    double cheb = Math.max(Math.abs(rx), Math.abs(rz));
                    double shapeScore = roundWeight * eu + (1.0 - roundWeight) * cheb;

                    int wz = cz + dz;
                    if (!state.canWrite(wx, wy, wz))
                        continue;

                    long voxelSeed = baseSeed ^ centerLong ^ (dx * 341873128712L) ^ (dy * 132897987541L)
                            ^ (dz * 2654435761L);
                    double noise = (mixDouble(voxelSeed) - 0.5) * 0.8;
                    double threshold = layerRadius + noise * 0.5;

                    boolean placeLeaf = false;
                    if (shapeScore <= threshold) {
                        double normalized = shapeScore / Math.max(1.0, layerRadius);
                        double falloff = Math.pow(normalized, 1.6);
                        double baseProb = Math.max(0.05, 1.0 - falloff);
                        double prob = baseProb + (mixDouble(voxelSeed ^ 0x9E3779B97F4A7C15L) - 0.5) * 0.12;
                        if (mixDouble(voxelSeed ^ 0xBF58476D1CE4E5B9L) < Math.min(0.98, Math.max(0.06, prob))) {
                            placeLeaf = true;
                        }
                    } else if (shapeScore <= threshold + 0.8 && mixDouble(voxelSeed ^ 0x94D049BB133111EBL) < 0.06) {
                        placeLeaf = true;
                    }

                    if (placeLeaf) {
                        long key = BlockPos.asLong(wx, wy, wz);
                        if (state.placedAllLogs.contains(key) || state.knownWorldLogs.contains(key))
                            continue;
                        BlockState existing = state.getBlockState(wx, wy, wz);
                        if (existing.getBlock() instanceof RotatedPillarBlock) {
                            state.knownWorldLogs.add(key);
                            state.rejectedLeafSpots.add(key);
                            continue;
                        }
                        planned.add(key);
                    }
                }
            }
        }

        // deutlich mehr Kronen-Äste erzeugen
        int branchCount = Math.max(60, canopyRadius * 10) + global.nextInt(Math.max(1, canopyRadius * 2 + 1));
        for (int i = 0; i < branchCount; i++) {
            double angle = global.nextDouble() * Math.PI * 2;
            int sy = -below + global.nextInt(Math.max(1, above + below + 1));
            int sx = (int) Math.round(Math.cos(angle) * (global.nextDouble() * (canopyRadius * 0.4)));
            int sz = (int) Math.round(Math.sin(angle) * (global.nextDouble() * (canopyRadius * 0.4)));
            BlockPos start = center.offset(sx, sy, sz);

            int len = Math.max(3, Math.min(canopyRadius, 2 + global.nextInt(canopyRadius)));
            int leafBlobRadius = 2;

            genCrownBranch(level, state, start, angle, len, canopyRadius, leafBlobRadius, cfg, global, planned);
        }

        LongOpenHashSet next = state.crownNext;
        next.clear();
        LongIterator it = planned.iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            int px = BlockPos.getX(key);
            int py = BlockPos.getY(key);
            int pz = BlockPos.getZ(key);
            if (!state.canWrite(px, py, pz))
                continue;
            if (!canReplaceForLeavesCached(state, px, py, pz, key))
                continue;

            int neighbors = 0;
            boolean nearBranch = false;
            for (Direction d : ALL_DIRS) {
                long nKey = BlockPos.asLong(px + d.getStepX(), py + d.getStepY(), pz + d.getStepZ());
                if (planned.contains(nKey))
                    neighbors++;
                if (!nearBranch && state.placedBranchLogs.contains(nKey))
                    nearBranch = true;
            }
            if (neighbors >= 2 || nearBranch || mixDouble(key) < 0.22)
                next.add(key);
        }

        int lowerCutoff = cy - Math.max(3, canopyRadius / 2);
        LongIterator nextIt = next.iterator();
        while (nextIt.hasNext()) {
            long key = nextIt.nextLong();
            int px = BlockPos.getX(key);
            int py = BlockPos.getY(key);
            int pz = BlockPos.getZ(key);

            boolean hasNeighborLeafOrBranch = false;
            boolean nearBranch = false;
            for (Direction d : ALL_DIRS) {
                long nKey = BlockPos.asLong(px + d.getStepX(), py + d.getStepY(), pz + d.getStepZ());
                if (next.contains(nKey)) {
                    hasNeighborLeafOrBranch = true;
                }
                if (state.placedBranchLogs.contains(nKey)) {
                    hasNeighborLeafOrBranch = true;
                    nearBranch = true;
                    break;
                }
            }
            if (!hasNeighborLeafOrBranch)
                continue;
            if (py < lowerCutoff && !nearBranch)
                continue;

            state.plannedLeaves.add(key);
        }
        planned.clear();
        next.clear();
    }

    private void genCrownBranch(WorldGenLevel level, TreeWorkState state, BlockPos start, double angle, int len,
            int canopyRadius, int leafBlobRadius, ElderGiantTreeConfig cfg, RandomSource rnd,
            LongOpenHashSet planned) {
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);
        double x = start.getX();
        double y = start.getY();
        double z = start.getZ();

        double lateralStrength = 0.4 + rnd.nextDouble() * 0.9;
        double yStepBase = (rnd.nextDouble() - 0.4) * 0.35;
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

        int steps = Math.min(len, canopyRadius);
        for (int i = 0; i < steps; i++) {
            x += dx * (0.7 + rnd.nextDouble() * 0.6);
            z += dz * (0.7 + rnd.nextDouble() * 0.6);
            double sideX = -dz;
            double sideZ = dx;
            double lateral = (rnd.nextDouble() - 0.5) * lateralStrength;
            x += sideX * lateral;
            z += sideZ * lateral;
            y += yStepBase + (rnd.nextDouble() - 0.5) * 0.25;

            int px = (int) Math.round(x);
            int py = (int) Math.round(y);
            int pz = (int) Math.round(z);
            if (!state.withinHeight(py))
                break;

            mPos.set(px, py, pz);
            if (canReplaceForLog(state, px, py, pz)) {
                Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X
                        : (Math.abs(dz) > Math.abs(dx) ? Direction.Axis.Z : Direction.Axis.Y);
                placeLog(level, state, mPos, axis, cfg, rnd, true);

                for (Direction d : ALL_DIRS) {
                    int lx = px + d.getStepX();
                    int ly = py + d.getStepY();
                    int lz = pz + d.getStepZ();
                    if (!state.canWrite(lx, ly, lz))
                        continue;
                    long lKey = BlockPos.asLong(lx, ly, lz);
                    if (planned.contains(lKey) || state.placedAllLogs.contains(lKey) || state.rejectedLeafSpots.contains(lKey))
                        continue;
                    if (canReplaceForLeavesCached(state, lx, ly, lz, lKey)) {
                        planned.add(lKey);
                    }
                }

                int r = Math.max(0, Math.min(leafBlobRadius, canopyRadius));
                int r2 = r * r;
                for (int ox = -r; ox <= r; ox++) {
                    int qx = px + ox;
                    for (int oy = -r; oy <= r; oy++) {
                        int qy = py + oy;
                        if (!state.withinHeight(qy))
                            continue;
                        for (int oz = -r; oz <= r; oz++) {
                            if (ox * ox + oy * oy + oz * oz <= r2 + rnd.nextInt(2)) {
                                int qz = pz + oz;
                                if (!state.canWrite(qx, qy, qz))
                                    continue;
                                long qKey = BlockPos.asLong(qx, qy, qz);
                                if (planned.contains(qKey) || state.placedAllLogs.contains(qKey) || state.rejectedLeafSpots.contains(qKey))
                                    continue;
                                if (canReplaceForLeavesCached(state, qx, qy, qz, qKey)) {
                                    planned.add(qKey);
                                }
                            }
                        }
                    }
                }

                if (rnd.nextDouble() < 0.28) {
                    for (Direction s : CARDINAL_DIRECTIONS) {
                        int sx = px + s.getStepX();
                        int sz = pz + s.getStepZ();
                        if (canReplaceForLog(state, sx, py, sz)) {
                            mPos.set(sx, py, sz);
                            placeLog(level, state, mPos, Direction.Axis.Y, cfg, rnd, true);
                        }
                    }
                }

                if (rnd.nextDouble() < 0.12) {
                    double subAngle = angle + (rnd.nextBoolean() ? 0.9 : -0.9) + (rnd.nextDouble() - 0.5) * 0.8;
                    genCrownBranch(level, state, mPos.set(px, py, pz), subAngle, Math.max(1, steps / 3), canopyRadius,
                            Math.max(0, leafBlobRadius - 1), cfg, rnd, planned);
                }
            }

            if (i > 1 && rnd.nextDouble() < 0.35) {
                double subAngle = angle + (rnd.nextBoolean() ? 0.7 : -0.7) + (rnd.nextDouble() - 0.5) * 0.6;
                genCrownBranch(level, state, new BlockPos(px, py, pz), subAngle, Math.max(1, steps / 4), canopyRadius,
                        Math.max(0, leafBlobRadius - 1), cfg, rnd, planned);
            }
        }
    }

    private void genCrownBranchFromVector(WorldGenLevel level, TreeWorkState state, BlockPos start, BlockPos dirVec,
            int len, int canopyRadius, ElderGiantTreeConfig cfg, RandomSource rnd) {
        int stepX = dirVec.getX();
        int stepZ = dirVec.getZ();

        Direction.Axis axis;
        if (Math.abs(stepX) > Math.abs(stepZ))
            axis = Direction.Axis.X;
        else if (Math.abs(stepZ) > Math.abs(stepX))
            axis = Direction.Axis.Z;
        else
            axis = Direction.Axis.Y;

        int px = start.getX();
        int py = start.getY();
        int pz = start.getZ();
        int liftEvery = 2 + rnd.nextInt(2);
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < len; i++) {
            px += stepX;
            pz += stepZ;
            if (i % liftEvery == 0)
                py++;
            if (!state.withinHeight(py))
                break;

            long posKey = BlockPos.asLong(px, py, pz);
            boolean conflict = state.placedBranchLogs.contains(posKey);
            if (!conflict) {
                for (Direction d : CARDINAL_DIRECTIONS) {
                    if (state.placedBranchLogs.contains(BlockPos.asLong(px + d.getStepX(), py, pz + d.getStepZ()))) {
                        conflict = true;
                        break;
                    }
                }
            }
            if (conflict)
                break;

            if (canReplaceForLog(state, px, py, pz)) {
                mPos.set(px, py, pz);
                placeLog(level, state, mPos, axis, cfg, rnd, true);
            } else {
                break;
            }

            // kleine Blattwolke um den Ast
            if (rnd.nextFloat() < 0.45f) {
                int r = 1 + rnd.nextInt(2);
                genLeafBlob(state, px, py, pz, r, rnd, false);
            }
        }
        state.branchEndpoints.add(BlockPos.asLong(px, py, pz));
    }

    private List<BlockPos> computeNonAdjacentCrownDirs(RandomSource rnd, int maxBranches) {
        List<BlockPos> all = new ArrayList<>(8);
        all.add(new BlockPos(1, 0, 0));
        all.add(new BlockPos(-1, 0, 0));
        all.add(new BlockPos(0, 0, 1));
        all.add(new BlockPos(0, 0, -1));
        all.add(new BlockPos(1, 0, 1));
        all.add(new BlockPos(1, 0, -1));
        all.add(new BlockPos(-1, 0, 1));
        all.add(new BlockPos(-1, 0, -1));

        for (int i = all.size() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            BlockPos tmp = all.get(i);
            all.set(i, all.get(j));
            all.set(j, tmp);
        }

        List<BlockPos> result = new ArrayList<>();
        outer: for (BlockPos dir : all) {
            for (BlockPos used : result) {
                int dx = dir.getX() - used.getX();
                int dz = dir.getZ() - used.getZ();
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    continue outer;
                }
            }
            result.add(dir);
            if (result.size() >= maxBranches)
                break;
        }
        return result;
    }

    private void genLeafBlob(TreeWorkState state, int cx, int cy, int cz, int radius,
            RandomSource rnd, boolean forbidNearTrunk) {
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            int px = cx + dx;
            for (int dz = -radius; dz <= radius; dz++) {
                int pz = cz + dz;
                if (forbidNearTrunk && state.isNearTrunk(px, pz, 1))
                    continue;
                for (int dy = -radius; dy <= radius; dy++) {
                    int py = cy + dy;
                    int d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > r2 + rnd.nextInt(2))
                        continue;
                    tryPlanLeaf(state, px, py, pz);
                }
            }
        }
    }

    private void genCentralSupports(WorldGenLevel level, TreeWorkState state, BlockPos origin, int trunkSize,
            int height, int canopyR, ElderGiantTreeConfig cfg, RandomSource rnd) {
        int centerX = trunkSize / 2;
        int centerZ = trunkSize / 2;
        int bx = origin.getX() + centerX;
        int by = origin.getY() + Math.max(1, height - 6);
        int bz = origin.getZ() + centerZ;
        int supportHeight = Math.max(2, Math.min(canopyR - 2, 6));
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < supportHeight; i++) {
            int py = by + i;
            if (!state.withinHeight(py))
                break;
            if (canReplaceForLog(state, bx, py, bz)) {
                mPos.set(bx, py, bz);
                placeLog(level, state, mPos, Direction.Axis.Y, cfg, rnd, true);
            }
            if (i % 2 == 0 && rnd.nextDouble() < 0.35) {
                for (Direction d : CARDINAL_DIRECTIONS) {
                    int sx = bx + d.getStepX();
                    int sz = bz + d.getStepZ();
                    if (canReplaceForLog(state, sx, py, sz)) {
                        mPos.set(sx, py, sz);
                        placeLog(level, state, mPos, Direction.Axis.Y, cfg, rnd, true);
                    }
                }
            }
        }
    }

    private void genBranch(WorldGenLevel level, TreeWorkState state, BlockPos start, BlockPos dirVec, int len,
            ElderGiantTreeConfig cfg, RandomSource rnd, int canopyCenterY, int canopyRadius) {

        int px = start.getX();
        int py = start.getY();
        int pz = start.getZ();
        int stepX = dirVec.getX();
        int stepZ = dirVec.getZ();
        Direction.Axis axis;
        if (Math.abs(stepX) > Math.abs(stepZ))
            axis = Direction.Axis.X;
        else if (Math.abs(stepZ) > Math.abs(stepX))
            axis = Direction.Axis.Z;
        else
            axis = Direction.Axis.Y;

        int liftEvery = 3 + rnd.nextInt(3);
        boolean inCrown = py >= canopyCenterY - Math.max(1, canopyRadius / 2);
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < len; i++) {
            px += stepX;
            pz += stepZ;
            if (i % liftEvery == 0)
                py++;
            if (!state.withinHeight(py))
                break;

            long posKey = BlockPos.asLong(px, py, pz);
            boolean conflict = state.placedBranchLogs.contains(posKey);
            if (!conflict) {
                for (Direction d : CARDINAL_DIRECTIONS) {
                    if (state.placedBranchLogs.contains(BlockPos.asLong(px + d.getStepX(), py, pz + d.getStepZ()))) {
                        conflict = true;
                        break;
                    }
                }
            }
            if (conflict)
                break; // Ast hier hart abbrechen

            if (canReplaceForLog(state, px, py, pz)) {
                mPos.set(px, py, pz);
                placeLog(level, state, mPos, axis, cfg, rnd, true);
            } else {
                break;
            }

            // Seitenast: ebenfalls Kollisionsprüfung
            if (i > 1 && i < len - 1 && rnd.nextFloat() < 0.25f) {
                Direction sideDir = (Math.abs(stepX) >= Math.abs(stepZ))
                        ? (stepZ >= 0 ? Direction.SOUTH : Direction.NORTH)
                        : (stepX >= 0 ? Direction.EAST : Direction.WEST);
                int sx = px + sideDir.getStepX();
                int sz = pz + sideDir.getStepZ();
                long sideKey = BlockPos.asLong(sx, py, sz);

                boolean sideConflict = state.placedBranchLogs.contains(sideKey);
                if (!sideConflict) {
                    for (Direction d : CARDINAL_DIRECTIONS) {
                        if (state.placedBranchLogs.contains(BlockPos.asLong(sx + d.getStepX(), py, sz + d.getStepZ()))) {
                            sideConflict = true;
                            break;
                        }
                    }
                }
                if (!sideConflict && canReplaceForLog(state, sx, py, sz)) {
                    mPos.set(sx, py, sz);
                    placeLog(level, state, mPos, axis, cfg, rnd, true);
                }
            }

            if (i >= len - 2) {
                float chance = inCrown ? 0.25f : 0.08f;
                if (rnd.nextFloat() < chance) {
                    placeLeafCross(state, px, py, pz);
                }
            }
        }

        int endBlob = inCrown ? 2 + rnd.nextInt(2) : 1 + rnd.nextInt(2);
        long endKey = BlockPos.asLong(px, py, pz);
        if (!inCrown && state.isNearTrunk(px, pz, 1)) {
            state.branchEndpoints.add(endKey);
            return;
        }

        boolean forbidNearTrunk = !inCrown;
        genLeafBlob(state, px, py, pz, endBlob, rnd, forbidNearTrunk);
        state.branchEndpoints.add(endKey);
    }

    private void placeLeafCross(TreeWorkState state, int px, int py, int pz) {
        int padding = 1;
        for (Direction d : CARDINAL_DIRECTIONS) {
            int qx = px + d.getStepX();
            int qz = pz + d.getStepZ();
            if (!state.isNearTrunk(qx, qz, padding)) {
                tryPlanLeaf(state, qx, py, qz);
            }
        }
        if (!state.isNearTrunk(px, pz, padding)) {
            tryPlanLeaf(state, px, py + 1, pz);
            tryPlanLeaf(state, px, py - 1, pz);
        }
    }

    private boolean tryPlanLeaf(TreeWorkState state, int x, int y, int z) {
        if (!state.canWrite(x, y, z)) {
            return false;
        }
        long key = BlockPos.asLong(x, y, z);
        if (state.placedAllLogs.contains(key) || state.plannedLeaves.contains(key)
                || state.rejectedLeafSpots.contains(key)) {
            return false;
        }
        if (canReplaceForLeavesCached(state, x, y, z, key)) {
            state.plannedLeaves.add(key);
            return true;
        }
        return false;
    }

    private void finalizePlannedLeaves(WorldGenLevel level, TreeWorkState state, ElderGiantTreeConfig cfg,
            long baseSeed) {
        if (state.plannedLeaves.isEmpty())
            return;

        // Filter out non-writable spots or existing world logs in one linear sweep
        LongIterator cleanIt = state.plannedLeaves.iterator();
        while (cleanIt.hasNext()) {
            long key = cleanIt.nextLong();
            int px = BlockPos.getX(key);
            int py = BlockPos.getY(key);
            int pz = BlockPos.getZ(key);
            if (!state.canWrite(px, py, pz) || state.placedAllLogs.contains(key)
                    || state.knownWorldLogs.contains(key)) {
                cleanIt.remove();
                continue;
            }
            BlockState existing = state.getBlockState(px, py, pz);
            if (existing.getBlock() instanceof RotatedPillarBlock) {
                state.knownWorldLogs.add(key);
                cleanIt.remove();
            }
        }
        if (state.plannedLeaves.isEmpty())
            return;

        // Multi-source O(L) BFS for LeavesBlock.DISTANCE and PERSISTENT
        Long2IntOpenHashMap leafDist = state.leafDistances;
        leafDist.clear();
        leafDist.defaultReturnValue(7);
        Long2ByteOpenHashMap extCache = state.extNeighborCache;
        extCache.clear();
        for (int i = 1; i <= 6; i++) {
            state.bfsBuckets[i].clear();
        }

        // Seed distance 1 directly from placed logs (zero world block reads)
        LongIterator logSeedIt = state.placedAllLogs.iterator();
        while (logSeedIt.hasNext()) {
            long logKey = logSeedIt.nextLong();
            int lx = BlockPos.getX(logKey);
            int ly = BlockPos.getY(logKey);
            int lz = BlockPos.getZ(logKey);
            for (Direction d : ALL_DIRS) {
                long nKey = BlockPos.asLong(lx + d.getStepX(), ly + d.getStepY(), lz + d.getStepZ());
                if (state.plannedLeaves.contains(nKey) && leafDist.get(nKey) > 1) {
                    leafDist.put(nKey, 1);
                    state.bfsBuckets[1].add(nKey);
                }
            }
        }

        // Check boundary external neighbors (external logs / existing world leaves) at most once each
        LongIterator hullIt = state.plannedLeaves.iterator();
        while (hullIt.hasNext()) {
            long pKey = hullIt.nextLong();
            if (leafDist.get(pKey) == 1)
                continue;
            int px = BlockPos.getX(pKey);
            int py = BlockPos.getY(pKey);
            int pz = BlockPos.getZ(pKey);
            for (Direction d : ALL_DIRS) {
                int nx = px + d.getStepX();
                int ny = py + d.getStepY();
                int nz = pz + d.getStepZ();
                if (!state.withinHeight(ny))
                    continue;
                long nKey = BlockPos.asLong(nx, ny, nz);
                if (state.plannedLeaves.contains(nKey) || state.placedAllLogs.contains(nKey))
                    continue;
                byte extCode = extCache.get(nKey);
                if (extCode == 0) {
                    BlockState ns = state.getBlockState(nx, ny, nz);
                    if (ns.getBlock() instanceof RotatedPillarBlock || ns.is(BlockTags.LOGS)) {
                        extCode = 1; // adjacent log -> leaf distance 1
                    } else if (ns.is(BlockTags.LEAVES)) {
                        int extDist = ns.hasProperty(LeavesBlock.DISTANCE) ? ns.getValue(LeavesBlock.DISTANCE) : 7;
                        extCode = (extDist >= 1 && extDist <= 5) ? (byte) (extDist + 1) : 127;
                    } else {
                        extCode = 127;
                    }
                    extCache.put(nKey, extCode);
                }
                if (extCode <= 6 && leafDist.get(pKey) > extCode) {
                    leafDist.put(pKey, extCode);
                    state.bfsBuckets[extCode].add(pKey);
                }
            }
        }

        // Propagate distances 1 -> 6 across plannedLeaves in O(L)
        for (int dist = 1; dist <= 5; dist++) {
            LongArrayList bucket = state.bfsBuckets[dist];
            int nextDist = dist + 1;
            for (int i = 0, n = bucket.size(); i < n; i++) {
                long uKey = bucket.getLong(i);
                if (leafDist.get(uKey) != dist)
                    continue;
                int ux = BlockPos.getX(uKey);
                int uy = BlockPos.getY(uKey);
                int uz = BlockPos.getZ(uKey);
                for (Direction d : ALL_DIRS) {
                    long vKey = BlockPos.asLong(ux + d.getStepX(), uy + d.getStepY(), uz + d.getStepZ());
                    if (state.plannedLeaves.contains(vKey) && leafDist.get(vKey) > nextDist) {
                        leafDist.put(vKey, nextDist);
                        state.bfsBuckets[nextDist].add(vKey);
                    }
                }
            }
        }

        // Prepare separable 3D Chebyshev dilation (r = 6) mask around placedAllLogs for flowering leaves
        float floweringChance = cfg.floweringChance();
        boolean useFlowerMask = false;
        int minFX = 0, maxFX = -1, minFY = 0, maxFY = -1, minFZ = 0, maxFZ = -1;
        int sx = 0, sy = 0, sz = 0;
        byte[] flowerMask = null;
        if (floweringChance > 0.0f && !state.placedAllLogs.isEmpty()) {
            int minLX = Integer.MAX_VALUE, maxLX = Integer.MIN_VALUE;
            int minLY = Integer.MAX_VALUE, maxLY = Integer.MIN_VALUE;
            int minLZ = Integer.MAX_VALUE, maxLZ = Integer.MIN_VALUE;
            LongIterator logBoxIt = state.placedAllLogs.iterator();
            while (logBoxIt.hasNext()) {
                long k = logBoxIt.nextLong();
                int lx = BlockPos.getX(k);
                int ly = BlockPos.getY(k);
                int lz = BlockPos.getZ(k);
                if (lx < minLX) minLX = lx;
                if (lx > maxLX) maxLX = lx;
                if (ly < minLY) minLY = ly;
                if (ly > maxLY) maxLY = ly;
                if (lz < minLZ) minLZ = lz;
                if (lz > maxLZ) maxLZ = lz;
            }
            minFX = minLX - FLOWER_RADIUS;
            maxFX = maxLX + FLOWER_RADIUS;
            minFY = Math.max(state.minY, minLY - FLOWER_RADIUS);
            maxFY = Math.min(state.maxY, maxLY + FLOWER_RADIUS);
            minFZ = minLZ - FLOWER_RADIUS;
            maxFZ = maxLZ + FLOWER_RADIUS;
            sx = maxFX - minFX + 1;
            sy = maxFY - minFY + 1;
            sz = maxFZ - minFZ + 1;
            long volL = (long) sx * sy * sz;
            if (volL > 0L && volL <= MAX_FLOWER_MASK_VOL) {
                int vol = (int) volL;
                if (state.flowerMask.length < vol) {
                    state.flowerMask = new byte[vol];
                }
                flowerMask = state.flowerMask;
                Arrays.fill(flowerMask, 0, vol, (byte) 0);

                LongIterator seedLogIt = state.placedAllLogs.iterator();
                while (seedLogIt.hasNext()) {
                    long k = seedLogIt.nextLong();
                    int lx = BlockPos.getX(k);
                    int ly = BlockPos.getY(k);
                    int lz = BlockPos.getZ(k);
                    if (ly >= minFY && ly <= maxFY) {
                        flowerMask[((ly - minFY) * sx + (lx - minFX)) * sz + (lz - minFZ)] = 4;
                    }
                }

                int yStride = sx * sz;
                int spread = FLOWER_RADIUS + 1;
                // Pass X: bit 2 -> bit 1
                for (int y = 0; y < sy; y++) {
                    int yBase = y * yStride;
                    for (int z = 0; z < sz; z++) {
                        int base = yBase + z;
                        int rem = 0;
                        for (int x = 0; x < sx; x++) {
                            int idx = base + x * sz;
                            if ((flowerMask[idx] & 4) != 0)
                                rem = spread;
                            if (rem > 0) {
                                flowerMask[idx] |= 2;
                                rem--;
                            }
                        }
                        rem = 0;
                        for (int x = sx - 1; x >= 0; x--) {
                            int idx = base + x * sz;
                            if ((flowerMask[idx] & 4) != 0)
                                rem = spread;
                            if (rem > 0) {
                                flowerMask[idx] |= 2;
                                rem--;
                            }
                        }
                    }
                }
                // Pass Y: bit 1 -> bit 0
                for (int x = 0; x < sx; x++) {
                    int xOff = x * sz;
                    for (int z = 0; z < sz; z++) {
                        int base = xOff + z;
                        int rem = 0;
                        for (int y = 0; y < sy; y++) {
                            int idx = base + y * yStride;
                            if ((flowerMask[idx] & 2) != 0)
                                rem = spread;
                            if (rem > 0) {
                                flowerMask[idx] |= 1;
                                rem--;
                            }
                        }
                        rem = 0;
                        for (int y = sy - 1; y >= 0; y--) {
                            int idx = base + y * yStride;
                            if ((flowerMask[idx] & 2) != 0)
                                rem = spread;
                            if (rem > 0) {
                                flowerMask[idx] |= 1;
                                rem--;
                            }
                        }
                    }
                }
                // Pass Z: bit 0 -> bit 3
                for (int y = 0; y < sy; y++) {
                    int yBase = y * yStride;
                    for (int x = 0; x < sx; x++) {
                        int rowBase = yBase + x * sz;
                        int rem = 0;
                        for (int z = 0; z < sz; z++) {
                            int idx = rowBase + z;
                            if ((flowerMask[idx] & 1) != 0)
                                rem = spread;
                            if (rem > 0) {
                                flowerMask[idx] |= 8;
                                rem--;
                            }
                        }
                        rem = 0;
                        for (int z = sz - 1; z >= 0; z--) {
                            int idx = rowBase + z;
                            if ((flowerMask[idx] & 1) != 0)
                                rem = spread;
                            if (rem > 0) {
                                flowerMask[idx] |= 8;
                                rem--;
                            }
                        }
                    }
                }
                useFlowerMask = true;
            }
        }

        boolean isScarletTree = floweringChance <= 0.0f;
        boolean forcingGrowth = state.forcingGrowth;
        int setFlags = forcingGrowth ? 3 : 2;
        RandomSource rnd = state.reusableRnd;
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        BlockState scarletVineUpBase = isScarletTree
                ? ModBlocks.SCARLET_VINE.get().defaultBlockState().setValue(VineBlock.UP, true)
                : null;
        BlockState scarletVineDownBase = isScarletTree
                ? ModBlocks.SCARLET_VINE.get().defaultBlockState()
                : null;

        LongIterator placeIt = state.plannedLeaves.iterator();
        while (placeIt.hasNext()) {
            long pKey = placeIt.nextLong();
            int px = BlockPos.getX(pKey);
            int py = BlockPos.getY(pKey);
            int pz = BlockPos.getZ(pKey);
            mPos.set(px, py, pz);

            if (forcingGrowth) {
                BlockState existing = state.getBlockState(px, py, pz);
                boolean replaceable = existing.isAir() || existing.is(BlockTags.LEAVES) || existing.canBeReplaced()
                        || existing.is(Blocks.GRASS_BLOCK) || existing.is(Blocks.TALL_GRASS);
                if (!replaceable) {
                    continue;
                }
            }

            rnd.setSeed(pKey ^ baseSeed);

            boolean isFlowering = false;
            if (floweringChance > 0.0f) {
                boolean nearLog;
                if (useFlowerMask) {
                    nearLog = px >= minFX && px <= maxFX && py >= minFY && py <= maxFY && pz >= minFZ && pz <= maxFZ
                            && (flowerMask[((py - minFY) * sx + (px - minFX)) * sz + (pz - minFZ)] & 8) != 0;
                } else {
                    nearLog = hasPlacedLogWithinChebyshev(state.placedAllLogs, px, py, pz, FLOWER_RADIUS);
                }
                if (nearLog && mixDouble(pKey ^ baseSeed ^ 0x9E3779B97F4A7C15L) < floweringChance) {
                    isFlowering = true;
                }
            }

            BlockState leafState = isFlowering
                    ? cfg.floweringLeavesProvider().getState(rnd, mPos)
                    : cfg.leavesProvider().getState(rnd, mPos);

            int rawDist = leafDist.get(pKey);
            boolean persistent = (rawDist == 1);
            if (leafState.hasProperty(LeavesBlock.DISTANCE)) {
                int clampedDist = Math.max(1, Math.min(6, rawDist));
                leafState = leafState.setValue(LeavesBlock.DISTANCE, clampedDist);
            }
            if (leafState.hasProperty(LeavesBlock.PERSISTENT)) {
                leafState = leafState.setValue(LeavesBlock.PERSISTENT, persistent);
            }

            level.setBlock(mPos, leafState, setFlags);

            // Generate Scarlet Vines hanging from leaves for Scarlet Trees
            if (isScarletTree && rnd.nextFloat() < 0.20f) {
                int belowY = py - 1;
                if (state.canWrite(px, belowY, pz) && state.getBlockState(px, belowY, pz).isAir()) {
                    BooleanProperty chosenSide = VINE_SIDES[rnd.nextInt(4)];
                    mPos.set(px, belowY, pz);
                    level.setBlock(mPos, scarletVineUpBase.setValue(chosenSide, true), 2);

                    if (rnd.nextFloat() < 0.4f) {
                        int below2Y = belowY - 1;
                        if (state.canWrite(px, below2Y, pz) && state.getBlockState(px, below2Y, pz).isAir()) {
                            mPos.set(px, below2Y, pz);
                            level.setBlock(mPos, scarletVineDownBase.setValue(chosenSide, true), 2);
                        }
                    }
                }
            }
        }
        state.plannedLeaves.clear();
    }

    private static boolean hasPlacedLogWithinChebyshev(LongOpenHashSet logs, int px, int py, int pz, int radius) {
        LongIterator it = logs.iterator();
        while (it.hasNext()) {
            long k = it.nextLong();
            if (Math.abs(BlockPos.getX(k) - px) <= radius
                    && Math.abs(BlockPos.getY(k) - py) <= radius
                    && Math.abs(BlockPos.getZ(k) - pz) <= radius) {
                return true;
            }
        }
        return false;
    }

    private void ensureLeavesAroundBranchLogs(TreeWorkState state, int canopyCenterY, int canopyRadius) {
        if (state.placedBranchLogs.isEmpty())
            return;

        LongIterator it = state.placedBranchLogs.iterator();
        while (it.hasNext()) {
            long logKey = it.nextLong();
            int lx = BlockPos.getX(logKey);
            int ly = BlockPos.getY(logKey);
            int lz = BlockPos.getZ(logKey);
            boolean inCrown = canopyRadius > 0 && ly >= canopyCenterY - Math.max(1, canopyRadius / 2);

            boolean allow = inCrown || state.branchEndpoints.contains(logKey);
            if (!allow) {
                for (Direction d : CARDINAL_DIRECTIONS) {
                    if (state.branchEndpoints.contains(BlockPos.asLong(lx + d.getStepX(), ly, lz + d.getStepZ()))) {
                        allow = true;
                        break;
                    }
                }
            }
            if (!allow)
                continue;

            long seed = logKey ^ 0xC13FA9A902A6328FL;
            RandomSource rnd = state.reusableRnd;
            rnd.setSeed(seed);
            int extraR = inCrown ? 3 + rnd.nextInt(2) : 1 + rnd.nextInt(2);
            int r2 = extraR * extraR;

            for (int ox = -extraR; ox <= extraR; ox++) {
                int qx = lx + ox;
                for (int oy = -extraR; oy <= extraR; oy++) {
                    int qy = ly + oy;
                    if (!state.withinHeight(qy))
                        continue;
                    for (int oz = -extraR; oz <= extraR; oz++) {
                        if (ox * ox + oy * oy + oz * oz <= r2 + rnd.nextInt(2)) {
                            tryPlanLeaf(state, qx, qy, lz + oz);
                        }
                    }
                }
            }
        }
    }

    private void placeRootCluster(WorldGenLevel level, TreeWorkState state, BlockPos center, int thickness,
            ElderGiantTreeConfig cfg, RandomSource rnd, boolean markBranch) {
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        if (!state.withinHeight(cy))
            return;
        for (int rx = -thickness; rx <= thickness; rx++) {
            for (int rz = -thickness; rz <= thickness; rz++) {
                int px = cx + rx;
                int pz = cz + rz;
                if (canReplaceForLog(state, px, cy, pz)) {
                    mPos.set(px, cy, pz);
                    placeLog(level, state, mPos, Direction.Axis.Y, cfg, rnd, markBranch, cfg.rootProvider());
                }
            }
        }
    }

    private boolean containsNonReplaceableBlocks(WorldGenLevel level, BlockPos origin, int height, int trunkSize,
            int canopyR) {
        if (UpgradedSaplingBlock.IS_FORCING_GROWTH.get())
            return false;
        int extra = 2;
        int radiusX = canopyR + trunkSize + 2 + extra;
        int radiusZ = canopyR + trunkSize + 2 + extra;
        int ox = origin.getX();
        int oz = origin.getZ();
        int minY = Math.max(level.getMinBuildHeight(), origin.getY() - extra);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, origin.getY() + height + canopyR + 2 + extra);
        if (minY > maxY) {
            return false;
        }

        int minX = ox - radiusX;
        int maxX = ox + radiusX;
        int minZ = oz - radiusZ;
        int maxZ = oz + radiusZ;

        int minCx = Math.floorDiv(minX, 16);
        int maxCx = Math.floorDiv(maxX, 16);
        int minCz = Math.floorDiv(minZ, 16);
        int maxCz = Math.floorDiv(maxZ, 16);
        int minSec = level.getSectionIndex(minY);
        int maxSec = level.getSectionIndex(maxY);

        for (int cx = minCx; cx <= maxCx; cx++) {
            int chunkMinX = cx << 4;
            int localStartX = Math.max(0, minX - chunkMinX);
            int localEndX = Math.min(15, maxX - chunkMinX);
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!level.hasChunk(cx, cz))
                    continue;
                ChunkAccess chunk;
                try {
                    chunk = level.getChunk(cx, cz);
                } catch (Exception ignored) {
                    continue;
                }
                int secLow = Math.max(0, minSec);
                int secHigh = Math.min(chunk.getSectionsCount() - 1, maxSec);
                int chunkMinZ = cz << 4;
                int localStartZ = Math.max(0, minZ - chunkMinZ);
                int localEndZ = Math.min(15, maxZ - chunkMinZ);

                for (int secIdx = secLow; secIdx <= secHigh; secIdx++) {
                    LevelChunkSection section = chunk.getSection(secIdx);
                    if (section.hasOnlyAir() || !section.maybeHas(PROTECTED_STRUCTURE_PREDICATE)) {
                        continue;
                    }
                    int secMinY = level.getSectionYFromSectionIndex(secIdx) << 4;
                    int localStartY = Math.max(0, minY - secMinY);
                    int localEndY = Math.min(15, maxY - secMinY);
                    for (int ly = localStartY; ly <= localEndY; ly++) {
                        for (int lx = localStartX; lx <= localEndX; lx++) {
                            for (int lz = localStartZ; lz <= localEndZ; lz++) {
                                if (section.getBlockState(lx, ly, lz).is(ModTags.Blocks.ELDERWOODS_PROTECTED_STRUCTURE_BLOCKS)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private void tryPlaceSporeBlossomUnderBranch(WorldGenLevel level, TreeWorkState state, int bx, int by, int bz,
            ElderGiantTreeConfig cfg, RandomSource rnd) {
        if (!cfg.placeSporeBlossoms())
            return;

        if (rnd.nextFloat() > 0.15f)
            return;
        int canopyCenterY = state.canopyCenterY;
        int canopyRadius = state.canopyRadius;

        if (by >= canopyCenterY - Math.max(1, canopyRadius / 2)) {
            return;
        }

        if (rnd.nextDouble() >= 0.008)
            return;
        if (!state.canWrite(bx, by, bz))
            return;

        int targetY = by - 1;
        if (!state.canWrite(bx, targetY, bz))
            return;

        BlockState above = state.getBlockState(bx, by, bz);
        boolean aboveIsLog = above.getBlock() instanceof RotatedPillarBlock
                || state.placedBranchLogs.contains(BlockPos.asLong(bx, by, bz));
        if (!aboveIsLog)
            return;

        if (!canReplaceForLeaves(state, bx, targetY, bz))
            return;

        level.setBlock(new BlockPos(bx, targetY, bz), ModBlocks.ELDER_SPORE_BLOSSOM.get().defaultBlockState(), 2);
    }

    private static boolean isChunkLoaded(WorldGenLevel level, int x, int z) {
        int cx = Math.floorDiv(x, 16);
        int cz = Math.floorDiv(z, 16);
        try {
            if (level.hasChunk(cx, cz))
                return true;
            if (level.getChunkSource() != null && level.getChunkSource().hasChunk(cx, cz))
                return true;
        } catch (NoSuchMethodError ignored) {
        }
        return false;
    }

    private boolean areChunksLoaded(WorldGenLevel level, BlockPos center, int radius) {
        int minX = Math.floorDiv(center.getX() - radius, 16);
        int maxX = Math.floorDiv(center.getX() + radius, 16);
        int minZ = Math.floorDiv(center.getZ() - radius, 16);
        int maxZ = Math.floorDiv(center.getZ() + radius, 16);

        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                try {
                    if (level.hasChunk(cx, cz))
                        continue;
                    if (level.getChunkSource() != null && level.getChunkSource().hasChunk(cx, cz))
                        continue;
                } catch (NoSuchMethodError ignored) {
                }
                return false;
            }
        }
        return true;
    }

    private int clampRadiusToLoadedChunks(WorldGenLevel level, BlockPos center, int desiredRadius) {
        int maxRadius = desiredRadius;
        while (maxRadius > 0) {
            if (areChunksLoaded(level, center, maxRadius))
                return maxRadius;
            maxRadius--;
        }
        return 0;
    }

    private boolean canReplaceForLog(TreeWorkState state, int x, int y, int z) {
        if (state.forcingGrowth)
            return true;
        BlockState s = state.getBlockState(x, y, z);
        if (!s.getFluidState().isEmpty())
            return true;
        return s.isAir() || s.is(BlockTags.REPLACEABLE) || s.is(BlockTags.LEAVES) || s.is(Blocks.GRASS_BLOCK)
                || s.is(Blocks.TALL_GRASS) || s.is(Blocks.MUD);
    }

    private boolean canReplaceForLeavesCached(TreeWorkState state, int x, int y, int z, long key) {
        if (state.forcingGrowth)
            return true;
        BlockState s = state.getBlockState(x, y, z);
        if (s.getBlock() instanceof RotatedPillarBlock) {
            state.knownWorldLogs.add(key);
            state.rejectedLeafSpots.add(key);
            return false;
        }
        boolean ok = !s.getFluidState().isEmpty()
                || s.isAir() || s.is(BlockTags.REPLACEABLE) || s.is(BlockTags.LEAVES) || s.is(Blocks.GRASS_BLOCK)
                || s.is(Blocks.TALL_GRASS) || s.is(Blocks.MUD);
        if (!ok) {
            state.rejectedLeafSpots.add(key);
        }
        return ok;
    }

    private boolean canReplaceForLeaves(TreeWorkState state, int x, int y, int z) {
        if (state.forcingGrowth)
            return true;
        BlockState s = state.getBlockState(x, y, z);
        if (s.getBlock() instanceof RotatedPillarBlock)
            return false;
        if (!s.getFluidState().isEmpty())
            return true;
        return s.isAir() || s.is(BlockTags.REPLACEABLE) || s.is(BlockTags.LEAVES) || s.is(Blocks.GRASS_BLOCK)
                || s.is(Blocks.TALL_GRASS) || s.is(Blocks.MUD);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void placeHangingMoss(WorldGenLevel level, TreeWorkState state, RandomSource rnd) {
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        LongIterator it = state.placedBranchLogs.iterator();
        while (it.hasNext()) {
            long logKey = it.nextLong();
            if (rnd.nextFloat() < 0.15f) {
                int bx = BlockPos.getX(logKey);
                int by = BlockPos.getY(logKey);
                int bz = BlockPos.getZ(logKey);
                int belowY = by - 1;
                if (state.canWrite(bx, belowY, bz) && state.getBlockState(bx, belowY, bz).isAir()) {
                    int mossLength = 2 + rnd.nextInt(4);
                    for (int i = 0; i < mossLength; i++) {
                        int cy = by - (i + 1);
                        if (!state.canWrite(bx, cy, bz) || !state.getBlockState(bx, cy, bz).isAir()) {
                            break;
                        }
                        boolean isTip = (i == mossLength - 1)
                                || !state.withinHeight(cy - 1)
                                || !state.getBlockState(bx, cy - 1, bz).isAir();
                        BlockState mossState = isTip
                                ? ModBlocks.HANGING_ELDER_MOSS.get().defaultBlockState()
                                : ModBlocks.HANGING_ELDER_MOSS_PLANT.get().defaultBlockState();

                        mPos.set(bx, cy, bz);
                        level.setBlock(mPos, mossState, 2);
                    }
                }
            }
        }
    }
}