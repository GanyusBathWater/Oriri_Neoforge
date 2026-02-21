package net.ganyusbathwater.oririmod.worldgen.tree;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.ganyusbathwater.oririmod.block.custom.UpgradedSaplingBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ElderGiantTreeFeature extends Feature<ElderGiantTreeConfig> {

    private final ThreadLocal<Set<BlockPos>> placedBranchLogs = ThreadLocal.withInitial(HashSet::new);
    private final ThreadLocal<Set<BlockPos>> placedAllLogs = ThreadLocal.withInitial(HashSet::new);
    private final ThreadLocal<Set<BlockPos>> plannedLeaves = ThreadLocal.withInitial(HashSet::new);
    private final ThreadLocal<Set<BlockPos>> branchEndpoints = ThreadLocal.withInitial(HashSet::new);
    private final ThreadLocal<Integer> canopyCenterYLocal = ThreadLocal.withInitial(() -> 0);
    private final ThreadLocal<Integer> canopyRadiusLocal = ThreadLocal.withInitial(() -> 0);
    private final ThreadLocal<BlockPos> trunkOrigin = ThreadLocal.withInitial(() -> BlockPos.ZERO);
    private final ThreadLocal<Integer> trunkSizeLocal = ThreadLocal.withInitial(() -> 1);

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

        int rawHeight = clamp(cfg.trunkHeight().sample(rnd), 8, 96);
        int rawTrunk = clamp(cfg.trunkRadius().sample(rnd), 1, 5);
        int rawCanopy = clamp(cfg.canopyRadius().sample(rnd), 3, 18);
        int rawBranch = clamp(cfg.branchLength().sample(rnd), 3, 12);

        int maxHeightForTrunk = clamp(8 + rawTrunk * 12, 8, 96);
        int height = Math.min(rawHeight, maxHeightForTrunk);

        int trunkPreferFromHeight = clamp(1 + height / 12, 1, 5);
        int trunkSize = clamp((int) Math.round((rawTrunk * 0.6 + trunkPreferFromHeight * 0.4)), 1, 5);

        int canopyFromHeight = clamp(3 + height / 6, 3, 18);
        int canopyFromTrunk = clamp(trunkSize * 3, 3, 18);
        int canopyR = clamp((int) Math.round(rawCanopy * 0.4 + canopyFromHeight * 0.4 + canopyFromTrunk * 0.2), 3, 18);

        if (containsNonReplaceableBlocks(level, origin, height, trunkSize, canopyR))
            return false;

        int branchLenBase = clamp((int) Math.round(rawBranch * (1.0 + trunkSize / 8.0)), 3, 12);

        trunkSize = Math.min(trunkSize, Math.max(1, Math.min(5, 1 + height / 12)));

        if (origin.getY() + height + canopyR + 2 >= level.getMaxBuildHeight())
            return false;
        if (origin.getY() <= level.getMinBuildHeight() + 1)
            return false;

        BlockPos below = origin.below();
        BlockState ground = level.getBlockState(below);
        if (!UpgradedSaplingBlock.IS_FORCING_GROWTH.get()) {
            if (!ground.is(Blocks.GRASS_BLOCK) && !ground.is(ModBlocks.SCARLET_MOSS.get())
                    && !ground.is(ModBlocks.SCARLET_GRASS_BLOCK.get())) {
                return false;
            }
        }

        for (int y = 0; y <= height; y++) {
            for (int dx = 0; dx < trunkSize; dx++) {
                for (int dz = 0; dz < trunkSize; dz++) {
                    BlockPos p = origin.offset(dx, y, dz);
                    if (!canReplaceForLog(level, p))
                        return false;
                }
            }
        }

        long baseSeed = rnd.nextLong();
        BlockPos top = origin.offset((trunkSize - 1) / 2, height, (trunkSize - 1) / 2);

        int localX = Math.floorMod(origin.getX(), 16);
        int localZ = Math.floorMod(origin.getZ(), 16);
        if (!UpgradedSaplingBlock.IS_FORCING_GROWTH.get()) {
            if (localX == 0 || localX == 15 || localZ == 0 || localZ == 15) {
                return false;
            }
        }

        if (!areChunksLoaded(level, top, canopyR)) {
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
                                sBranchLenBase, sBaseSeed);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                return true;
            } else {
                return false;
            }
        }

        return generateTree(level, cfg, origin, rnd, height, trunkSize, canopyR, branchLenBase, baseSeed);
    }

    private boolean generateTree(WorldGenLevel level, ElderGiantTreeConfig cfg, BlockPos origin, RandomSource rnd,
            int height, int trunkSize, int canopyR, int branchLenBase, long baseSeed) {

        if (containsNonReplaceableBlocks(level, origin, height, trunkSize, canopyR))
            return false;
        placedBranchLogs.get().clear();
        placedAllLogs.get().clear();
        plannedLeaves.get().clear();
        branchEndpoints.get().clear();

        trunkOrigin.set(origin.immutable());
        trunkSizeLocal.set(trunkSize);

        if (origin.getY() + height + canopyR + 2 >= level.getMaxBuildHeight())
            return false;
        if (origin.getY() <= level.getMinBuildHeight() + 1)
            return false;

        for (int y = 0; y < height; y++) {
            for (int dx = 0; dx < trunkSize; dx++) {
                for (int dz = 0; dz < trunkSize; dz++) {
                    BlockPos p = origin.offset(dx, y, dz);
                    placeLog(level, p, Direction.Axis.Y, cfg, rnd, false);
                }
            }
        }

        genPerimeterRoots(level, origin, trunkSize, cfg, rnd);

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
                BlockPos firstStep = origin
                        .offset(rnd.nextInt(trunkSize), by, rnd.nextInt(trunkSize))
                        .offset(dirVec);
                boolean conflict = false;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    if (placedBranchLogs.get().contains(firstStep)
                            || placedBranchLogs.get().contains(firstStep.relative(d))) {
                        conflict = true;
                        break;
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
            genBranch(level, start, dirVec, len, cfg, rnd, canopyCenterY, canopyR);
        }

        if (canopyR >= 6) {
            int internalBranches = 3 + trunkSize;
            for (int i = 0; i < internalBranches; i++) {
                int by = height - 4 - rnd.nextInt(Math.max(1, height / 6));
                int sx = trunkSize / 2;
                int sz = trunkSize / 2;
                double angle = rnd.nextDouble() * Math.PI * 2;
                int len = canopyR / 2 + rnd.nextInt(canopyR / 2 + 1);
                genInternalBranch(level, origin.offset(sx, by, sz), angle, len, cfg, rnd);
            }
            genCentralSupports(level, origin, trunkSize, height, canopyR, cfg, rnd);
        }

        BlockPos top = origin.offset((trunkSize - 1) / 2, height, (trunkSize - 1) / 2);

        // Werte für Spore Blossoms merken
        canopyCenterYLocal.set(top.getY());
        canopyRadiusLocal.set(canopyR);

        // geordnete Kronenäste vom Stammpunkt nach außen
        int crownBranchCount = Math.min(8, 3 + canopyR / 3);
        List<BlockPos> crownDirs = computeNonAdjacentCrownDirs(rnd, crownBranchCount);
        for (BlockPos dir : crownDirs) {
            int len = 2 + rnd.nextInt(Math.max(2, canopyR / 2));
            genCrownBranchFromVector(level, top, dir, len, canopyR, cfg, rnd);
        }

        int finalCanopyR = canopyR;
        if (!areChunksLoaded(level, top, canopyR)) {
            finalCanopyR = clampRadiusToLoadedChunks(level, top, canopyR);
            if (finalCanopyR <= 0) {
                ensureLeavesAroundBranchLogs(level, cfg, top.getY(), finalCanopyR);
                placedBranchLogs.get().clear();
                placedAllLogs.get().clear();
                branchEndpoints.get().clear();
                return true;
            }
        }

        genIrregularCrown(level, top, finalCanopyR, cfg, baseSeed);

        ensureLeavesAroundBranchLogs(level, cfg, top.getY(), finalCanopyR);
        finalizePlannedLeaves(level, cfg, baseSeed);
        randomizeFloweringLeaves(level, cfg, baseSeed);

        // Replace logs with stems where ends are exposed
        replaceExposedLogsWithStems(level, cfg);

        placedBranchLogs.get().clear();
        placedAllLogs.get().clear();
        plannedLeaves.get().clear();
        branchEndpoints.get().clear();
        trunkOrigin.remove();
        trunkSizeLocal.remove();

        return true;
    }

    private void replaceExposedLogsWithStems(WorldGenLevel level, ElderGiantTreeConfig cfg) {
        if (cfg.stemProvider() == null)
            return;

        Set<BlockPos> allLogs = new HashSet<>(placedAllLogs.get());
        for (BlockPos pos : allLogs) {
            BlockState state = level.getBlockState(pos);
            if (!state.hasProperty(RotatedPillarBlock.AXIS))
                continue;

            Direction.Axis axis = state.getValue(RotatedPillarBlock.AXIS);
            boolean exposed = false;

            // Check both ends of the log along its axis
            for (Direction dir : Direction.values()) {
                if (dir.getAxis() == axis) {
                    BlockPos neighborPos = pos.relative(dir);
                    BlockState neighborState = level.getBlockState(neighborPos);
                    // If neighbor is not a log (transparent or different block), it's exposed
                    // We consider leaves as "covering" it enough? No, usually leaves don't hide the
                    // ring if you look closely.
                    // But the requirement said "where a log's top or bottom texture would be
                    // exposed".
                    // If it touches leaves, it's technically hidden by leaves, but if you break
                    // leaves you see it.
                    // Let's say if neighbor is NOT a log, it's exposed.
                    if (!neighborState.is(BlockTags.LOGS)) {
                        exposed = true;
                        break;
                    }
                }
            }

            if (exposed) {
                // Replace with stem
                BlockState stemState = cfg.stemProvider().getState(level.getRandom(), pos);
                if (stemState.hasProperty(RotatedPillarBlock.AXIS)) {
                    stemState = stemState.setValue(RotatedPillarBlock.AXIS, axis);
                }
                setBlock(level, pos, stemState);
            }
        }
    }

    private void placeLog(WorldGenLevel level, BlockPos pos, Direction.Axis axis, ElderGiantTreeConfig cfg,
            RandomSource rnd, boolean markBranch) {
        placeLog(level, pos, axis, cfg, rnd, markBranch, cfg.logProvider());
    }

    private void placeLog(WorldGenLevel level, BlockPos pos, Direction.Axis axis, ElderGiantTreeConfig cfg,
            RandomSource rnd, boolean markBranch, BlockStateProvider provider) {
        if (!isChunkLoaded(level, pos)) {
            return;
        }

        BlockState state = provider.getState(rnd, pos);
        if (state.hasProperty(RotatedPillarBlock.AXIS)) {
            state = state.setValue(RotatedPillarBlock.AXIS, axis);
        }

        boolean placeInWorld = true;
        if (UpgradedSaplingBlock.IS_FORCING_GROWTH.get()) {
            BlockState current = level.getBlockState(pos);
            boolean replaceable = current.isAir() || current.is(BlockTags.LEAVES) || current.canBeReplaced()
                    || current.is(Blocks.GRASS_BLOCK) || current.is(Blocks.TALL_GRASS);
            if (!replaceable) {
                placeInWorld = false;
            }
        }

        if (placeInWorld) {
            level.setBlock(pos, state, 3);
        }

        placedAllLogs.get().add(pos.immutable());
        if (markBranch) {
            placedBranchLogs.get().add(pos.immutable());
            tryPlaceSporeBlossomUnderBranch(level, pos, cfg, rnd);
        }
    }

    private void genPerimeterRoots(WorldGenLevel level, BlockPos origin, int trunkSize, ElderGiantTreeConfig cfg,
            RandomSource rnd) {
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
        // Basis-Dicke: wie vorher, aber Wurzellängen deutlich reduziert (nur kurze
        // Ausläufer)
        int baseThickness = trunkSize >= 3 ? 1 : 0;
        for (BlockPos start : starts) {
            // gelegentlich verankern oder kleine vertikale Stummel
            if (measureGroundDistance(level, start, 6) > 0 && rnd.nextDouble() < 0.8) {
                start = anchorRootToGround(level, start, 6, cfg, rnd);
            } else {
                for (int h = 0; h > -2; h--) {
                    BlockPos p = start.offset(0, h, 0);
                    if (!withinBuildHeight(level, p))
                        break;
                    if (canReplaceForLog(level, p))
                        placeLog(level, p, Direction.Axis.Y, cfg, rnd, false, cfg.rootProvider());
                }
            }

            // Entfernt: kein extra-Cluster direkt am Stamm mehr

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
            genRootOutward(level, start, dirX, dirZ, len, cfg, rnd);
        }
    }

    private void genRootOutward(WorldGenLevel level, BlockPos start, int dx, int dz, int len, ElderGiantTreeConfig cfg,
            RandomSource rnd) {
        double x = start.getX();
        double y = start.getY();
        double z = start.getZ();
        for (int i = 0; i < len; i++) {
            // deutlich kleinere horizontale Schritte, damit Wurzeln kompakter bleiben
            x += dx * (0.4 + rnd.nextDouble() * 0.4);
            z += dz * (0.4 + rnd.nextDouble() * 0.4);

            BlockPos probe = new BlockPos((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
            int drop = measureGroundDistance(level, probe, 8);

            if (drop > 0) {
                // wenn Boden unter dem Pfad ist, mit hoher Wahrscheinlichkeit direkt verankern
                if (rnd.nextDouble() < 0.9) {
                    BlockPos anchored = anchorRootToGround(level, probe, Math.min(4, drop + 1), cfg, rnd);
                    // setze Koordinaten auf verankerten Punkt und beende die Ausweitung (keine
                    // lange Verlängerung)
                    x = anchored.getX();
                    y = anchored.getY();
                    z = anchored.getZ();
                    // gelegentlich nur einen kurzen Seitenausläufer nach dem Verankern
                    if (rnd.nextDouble() < 0.25) {
                        int sx = rnd.nextBoolean() ? 1 : -1;
                        int sz = rnd.nextBoolean() ? 1 : -1;
                        genRootOutward(level, anchored, sx, sz, 1, cfg, rnd);
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
            if (!withinBuildHeight(level, p))
                break;

            // Falls unter dem geplanten Punkt ein längerer Leerraum ist, verankere kurz,
            // bevor du einen einzelnen Block setzt
            int belowGap = measureGroundDistance(level, p, 4);
            if (belowGap > 1) {
                BlockPos anchored = anchorRootToGround(level, p, Math.min(4, belowGap + 1), cfg, rnd);
                p = anchored;
                x = p.getX();
                y = p.getY();
                z = p.getZ();
            }

            if (canReplaceForLog(level, p)) {
                Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X
                        : (Math.abs(dz) > Math.abs(dx) ? Direction.Axis.Z : Direction.Axis.Y);
                placeLog(level, p, axis, cfg, rnd, false, cfg.rootProvider());

                // reduzierte seitliche Logs, damit Wurzel schlank bleibt
                for (Direction s : Direction.Plane.HORIZONTAL) {
                    if (rnd.nextDouble() < 0.25) {
                        BlockPos side = p.relative(s);
                        if (withinBuildHeight(level, side) && canReplaceForLog(level, side)) {
                            placeLog(level, side, Direction.Axis.Y, cfg, rnd, false, cfg.rootProvider());
                        }
                    }
                }

                // sehr selten kleine 3x3-Cluster, um lokale Dicke zu geben
                if (rnd.nextDouble() < 0.08) {
                    placeRootCluster(level, p, 1, cfg, rnd, false);
                }
            }

            // kleine Chance für einen kurzen Seitenausläufer, nicht rekursive Verlängerung
            if (rnd.nextDouble() < 0.12) {
                BlockPos side = p.relative(Direction.Plane.HORIZONTAL.getRandomDirection(rnd));
                if (withinBuildHeight(level, side) && canReplaceForLog(level, side)) {
                    placeLog(level, side, Direction.Axis.Y, cfg, rnd, false, cfg.rootProvider());
                }
            }
        }
    }

    private BlockPos anchorRootToGround(WorldGenLevel level, BlockPos start, int maxDepth, ElderGiantTreeConfig cfg,
            RandomSource rnd) {
        BlockPos p = start.immutable();
        if (!withinBuildHeight(level, p))
            return start;
        int placed = 0;
        // immer durchgehend nach unten setzen, bis nicht mehr ersetzbar oder maxDepth
        // erreicht
        for (int d = 0; d < maxDepth; d++) {
            if (!withinBuildHeight(level, p))
                break;
            BlockPos below = p.below();
            boolean belowReplaceable = withinBuildHeight(level, below)
                    && (level.getBlockState(below).isAir() || level.getBlockState(below).is(BlockTags.REPLACEABLE)
                            || level.getBlockState(below).is(BlockTags.LEAVES));
            if (canReplaceForLog(level, p)) {
                placeLog(level, p, Direction.Axis.Y, cfg, rnd, false, cfg.rootProvider());
                placed++;
            }
            if (!belowReplaceable) {
                // wenn Boden erreicht, Rückgabe der obersten gesetzten Position (für
                // Kontinuität)
                return p;
            }
            p = p.below();
        }
        // wenn wir etwas gesetzt haben, gib die oberste gesetzte Position zurück (so
        // dass caller verbunden bleibt)
        if (placed > 0)
            return p.above();
        return start;
    }

    private int measureGroundDistance(WorldGenLevel level, BlockPos pos, int max) {
        int count = 0;
        BlockPos probe = pos.below();
        for (int i = 0; i < max; i++) {
            if (!withinBuildHeight(level, probe))
                break;
            BlockState s = level.getBlockState(probe);
            if (s.isAir() || s.is(BlockTags.REPLACEABLE) || s.is(BlockTags.LEAVES) || s.is(Blocks.TALL_GRASS)) {
                count++;
                probe = probe.below();
            } else {
                break;
            }
        }
        return count;
    }

    private void genInternalBranch(WorldGenLevel level, BlockPos start, double angle, int len, ElderGiantTreeConfig cfg,
            RandomSource rnd) {
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);
        double ystep = 0.2 + rnd.nextDouble() * 0.6;
        double x = start.getX();
        double y = start.getY();
        double z = start.getZ();
        for (int i = 0; i < len; i++) {
            x += dx;
            y += ystep;
            z += dz;
            BlockPos p = new BlockPos((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
            if (!withinBuildHeight(level, p))
                break;
            if (canReplaceForLog(level, p)) {
                placeLog(level, p, Direction.Axis.X, cfg, rnd, true);
                if (rnd.nextDouble() < 0.35) {
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        BlockPos side = p.relative(d);
                        if (canReplaceForLog(level, side))
                            placeLog(level, side, Direction.Axis.Y, cfg, rnd, true);
                    }
                }
            }
            if (i >= len - 2 && rnd.nextFloat() < 0.35f)
                placeLeafCross(level, p, cfg, rnd);
        }
        genLeafBlob(level, new BlockPos((int) Math.round(x), (int) Math.round(y), (int) Math.round(z)),
                2 + rnd.nextInt(2),
                cfg, rnd);
    }

    private void genIrregularCrown(WorldGenLevel level, BlockPos center, int canopyRadius, ElderGiantTreeConfig cfg,
            long baseSeed) {
        int below = Math.max(1, canopyRadius / 3);
        int above = canopyRadius;
        RandomSource global = RandomSource.create(baseSeed ^ center.asLong());

        Set<BlockPos> planned = new HashSet<>();
        double roundWeightBase = 0.80;

        for (int dy = -below; dy <= above; dy++) {
            int depth = dy + below;
            double taper = (double) depth / Math.max(1, (above + below));
            int layerRadius = Math.max(1, (int) Math.round(canopyRadius * (1.0 - taper * 0.20)));
            double roundWeight = roundWeightBase + (global.nextDouble() - 0.5) * 0.12;

            int search = layerRadius + 3;
            for (int dx = -search; dx <= search; dx++) {
                for (int dz = -search; dz <= search; dz++) {
                    double rx = dx + (global.nextDouble() - 0.5) * 0.9;
                    double rz = dz + (global.nextDouble() - 0.5) * 0.9;
                    double eu = Math.hypot(rx, rz);
                    double cheb = Math.max(Math.abs(rx), Math.abs(rz));
                    double shapeScore = roundWeight * eu + (1.0 - roundWeight) * cheb;

                    long voxelSeed = baseSeed ^ center.asLong() ^ (dx * 341873128712L) ^ (dy * 132897987541L)
                            ^ (dz * 2654435761L);
                    RandomSource local = RandomSource.create(voxelSeed);

                    double noise = (local.nextDouble() - 0.5) * 0.8;
                    double threshold = layerRadius + noise * 0.5;
                    double normalized = shapeScore / Math.max(1.0, layerRadius);
                    double falloff = Math.pow(normalized, 1.6);
                    double baseProb = Math.max(0.05, 1.0 - falloff);
                    double prob = baseProb + (local.nextDouble() - 0.5) * 0.12;

                    boolean place = false;
                    if (shapeScore <= threshold && local.nextDouble() < Math.min(0.98, Math.max(0.06, prob))) {
                        place = true;
                    } else if (shapeScore <= threshold + 0.8 && local.nextDouble() < 0.06) {
                        place = true;
                    }

                    if (place) {
                        BlockPos p = center.offset(dx, dy, dz);
                        if (!withinBuildHeight(level, p))
                            continue;
                        BlockState existing = level.getBlockState(p);
                        if (existing.getBlock() instanceof RotatedPillarBlock)
                            continue;
                        planned.add(p.immutable());
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
            // Small but solid leaf blobs (Radius 2) as requested
            int leafBlobRadius = 2;

            genCrownBranch(level, start, angle, len, canopyRadius, leafBlobRadius, cfg, global, planned);
        }

        Set<BlockPos> next = new HashSet<>();
        for (BlockPos p : planned) {
            if (!withinBuildHeight(level, p))
                continue;
            if (!canReplaceForLeaves(level, p))
                continue;
            int neighbors = 0;
            for (Direction d : Direction.values())
                if (planned.contains(p.relative(d)))
                    neighbors++;
            boolean nearBranch = false;
            for (Direction d : Direction.values())
                if (placedBranchLogs.get().contains(p.relative(d))) {
                    nearBranch = true;
                    break;
                }
            if (neighbors >= 2 || nearBranch || RandomSource.create(p.asLong()).nextDouble() < 0.22)
                next.add(p);
        }
        planned = next;

        Set<BlockPos> finalSet = new HashSet<>();
        for (BlockPos p : planned) {
            boolean hasNeighborLeafOrBranch = false;
            for (Direction d : Direction.values()) {
                if (planned.contains(p.relative(d))) {
                    hasNeighborLeafOrBranch = true;
                    break;
                }
                if (placedBranchLogs.get().contains(p.relative(d))) {
                    hasNeighborLeafOrBranch = true;
                    break;
                }
            }
            if (!hasNeighborLeafOrBranch)
                continue;

            if (p.getY() < center.getY() - Math.max(3, canopyRadius / 2)) {
                boolean nearBranch = false;
                for (Direction d : Direction.values())
                    if (placedBranchLogs.get().contains(p.relative(d))) {
                        nearBranch = true;
                        break;
                    }
                if (!nearBranch)
                    continue;
            }

            finalSet.add(p);
        }

        for (BlockPos p : finalSet) {
            plannedLeaves.get().add(p.immutable());
        }
    }

    private void genCrownBranch(WorldGenLevel level, BlockPos start, double angle, int len, int canopyRadius,
            int leafBlobRadius, ElderGiantTreeConfig cfg, RandomSource rnd, Set<BlockPos> plannedLeaves) {
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);
        double x = start.getX();
        double y = start.getY();
        double z = start.getZ();

        double lateralStrength = 0.4 + rnd.nextDouble() * 0.9;
        double yStepBase = (rnd.nextDouble() - 0.4) * 0.35;

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

            BlockPos p = new BlockPos((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
            if (!withinBuildHeight(level, p))
                break;
            if (canReplaceForLog(level, p)) {
                Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X
                        : (Math.abs(dz) > Math.abs(dx) ? Direction.Axis.Z : Direction.Axis.Y);
                placeLog(level, p, axis, cfg, rnd, true);

                for (Direction d : Direction.values()) {
                    BlockPos leafPos = p.relative(d);
                    if (!withinBuildHeight(level, leafPos))
                        continue;
                    BlockState exist = level.getBlockState(leafPos);
                    if (exist.getBlock() instanceof RotatedPillarBlock)
                        continue;
                    if (canReplaceForLeaves(level, leafPos))
                        plannedLeaves.add(leafPos.immutable());
                }

                // Erlaube r = 0 (sehr kleine Blobs)
                int r = Math.max(0, Math.min(leafBlobRadius, canopyRadius));
                int r2 = r * r;
                for (int ox = -r; ox <= r; ox++) {
                    for (int oy = -r; oy <= r; oy++) {
                        for (int oz = -r; oz <= r; oz++) {
                            if (ox * ox + oy * oy + oz * oz <= r2 + rnd.nextInt(2)) {
                                BlockPos q = p.offset(ox, oy, oz);
                                if (!withinBuildHeight(level, q))
                                    continue;
                                BlockState existing = level.getBlockState(q);
                                if (existing.getBlock() instanceof RotatedPillarBlock)
                                    continue;
                                if (canReplaceForLeaves(level, q))
                                    plannedLeaves.add(q.immutable());
                            }
                        }
                    }
                }

                if (rnd.nextDouble() < 0.28) {
                    for (Direction s : Direction.Plane.HORIZONTAL) {
                        BlockPos side = p.relative(s);
                        if (canReplaceForLog(level, side))
                            placeLog(level, side, Direction.Axis.Y, cfg, rnd, true);
                    }
                }

                // zusätzliche gelegentliche Seitenausläufer (unverändert)
                if (rnd.nextDouble() < 0.12) {
                    double subAngle = angle + (rnd.nextBoolean() ? 0.9 : -0.9) + (rnd.nextDouble() - 0.5) * 0.8;
                    // bei Sub-Branches ebenfalls kleinere Länge übergeben
                    genCrownBranch(level, p, subAngle, Math.max(1, steps / 3), canopyRadius,
                            Math.max(0, leafBlobRadius - 1), cfg, rnd, plannedLeaves);
                }
            }

            // erhöhte Wahrscheinlichkeit für rekursive Sub-Branches (Länge reduziert)
            if (i > 1 && rnd.nextDouble() < 0.35) {
                double subAngle = angle + (rnd.nextBoolean() ? 0.7 : -0.7) + (rnd.nextDouble() - 0.5) * 0.6;
                genCrownBranch(level, p, subAngle, Math.max(1, steps / 4), canopyRadius,
                        Math.max(0, leafBlobRadius - 1), cfg, rnd, plannedLeaves);
            }
        }
    }

    private void genCrownBranchFromVector(WorldGenLevel level, BlockPos start, BlockPos dirVec, int len,
            int canopyRadius, ElderGiantTreeConfig cfg, RandomSource rnd) {
        int stepX = dirVec.getX();
        int stepZ = dirVec.getZ();

        Direction.Axis axis;
        if (Math.abs(stepX) > Math.abs(stepZ))
            axis = Direction.Axis.X;
        else if (Math.abs(stepZ) > Math.abs(stepX))
            axis = Direction.Axis.Z;
        else
            axis = Direction.Axis.Y;

        BlockPos pos = start;
        int liftEvery = 2 + rnd.nextInt(2);

        for (int i = 0; i < len; i++) {
            pos = pos.offset(stepX, 0, stepZ);
            if (i % liftEvery == 0)
                pos = pos.above();
            if (!withinBuildHeight(level, pos))
                break;

            // Kollisionsregel wie bei genBranch()
            boolean conflict = false;
            if (placedBranchLogs.get().contains(pos))
                conflict = true;
            else {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    if (placedBranchLogs.get().contains(pos.relative(d))) {
                        conflict = true;
                        break;
                    }
                }
            }
            if (conflict)
                break;

            if (canReplaceForLog(level, pos)) {
                placeLog(level, pos, axis, cfg, rnd, true);
            } else {
                break;
            }

            // kleine Blattwolke um den Ast
            if (rnd.nextFloat() < 0.45f) {
                int r = 1 + rnd.nextInt(2);
                genLeafBlob(level, pos, r, cfg, rnd);
            }
        }
        branchEndpoints.get().add(pos.immutable());
    }

    private List<BlockPos> computeNonAdjacentCrownDirs(RandomSource rnd, int maxBranches) {
        List<BlockPos> all = new ArrayList<>();
        // 8 Hauptrichtungen: 4 kardinal, 4 diagonal
        all.add(new BlockPos(1, 0, 0));
        all.add(new BlockPos(-1, 0, 0));
        all.add(new BlockPos(0, 0, 1));
        all.add(new BlockPos(0, 0, -1));
        all.add(new BlockPos(1, 0, 1));
        all.add(new BlockPos(1, 0, -1));
        all.add(new BlockPos(-1, 0, 1));
        all.add(new BlockPos(-1, 0, -1));

        // mischen
        java.util.Collections.shuffle(all, new java.util.Random(rnd.nextLong()));

        List<BlockPos> result = new ArrayList<>();
        outer: for (BlockPos dir : all) {
            // nicht Nachbar zu bereits gewählten Richtungen
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

    private void genLeafBlob(WorldGenLevel level, BlockPos center, int radius,
            ElderGiantTreeConfig cfg, RandomSource rnd,
            boolean forbidNearTrunk) {
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > r2 + rnd.nextInt(2))
                        continue;

                    BlockPos p = center.offset(dx, dy, dz);
                    if (!withinBuildHeight(level, p))
                        continue;

                    if (forbidNearTrunk) {
                        // hier Puffer > 0 verwenden, z.B. 1 oder 2
                        if (isNearTrunk(p, 1))
                            continue;
                    }

                    if (canReplaceForLeaves(level, p)) {
                        plannedLeaves.get().add(p.immutable());
                    }
                }
            }
        }
    }

    private void genLeafBlob(WorldGenLevel level, BlockPos center, int radius, ElderGiantTreeConfig cfg,
            RandomSource rnd) {
        genLeafBlob(level, center, radius, cfg, rnd, false);
    }

    private boolean isNearTrunk(BlockPos pos, int padding) {
        BlockPos o = trunkOrigin.get();
        int size = trunkSizeLocal.get();

        int minX = o.getX();
        int maxX = o.getX() + size - 1;
        int minZ = o.getZ();
        int maxZ = o.getZ() + size - 1;

        minX -= padding;
        maxX += padding;
        minZ -= padding;
        maxZ += padding;

        int x = pos.getX();
        int z = pos.getZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    // alte Variante weiterverwenden, ruft padding=0 auf
    private boolean isNearTrunk(BlockPos pos) {
        return isNearTrunk(pos, 0);
    }

    private void genCentralSupports(WorldGenLevel level, BlockPos origin, int trunkSize, int height, int canopyR,
            ElderGiantTreeConfig cfg, RandomSource rnd) {
        int centerX = trunkSize / 2;
        int centerZ = trunkSize / 2;
        BlockPos base = origin.offset(centerX, Math.max(1, height - 6), centerZ);
        int supportHeight = Math.max(2, Math.min(canopyR - 2, 6));
        for (int i = 0; i < supportHeight; i++) {
            BlockPos p = base.offset(0, i, 0);
            if (!withinBuildHeight(level, p))
                break;
            if (canReplaceForLog(level, p))
                placeLog(level, p, Direction.Axis.Y, cfg, rnd, true);
            if (i % 2 == 0 && rnd.nextDouble() < 0.35) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos side = p.relative(d);
                    if (canReplaceForLog(level, side))
                        placeLog(level, side, Direction.Axis.Y, cfg, rnd, true);
                }
            }
        }
    }

    private void genBranch(WorldGenLevel level, BlockPos start, BlockPos dirVec, int len, ElderGiantTreeConfig cfg,
            RandomSource rnd, int canopyCenterY, int canopyRadius) {

        BlockPos pos = start;
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
        boolean inCrown = start.getY() >= canopyCenterY - Math.max(1, canopyRadius / 2);

        for (int i = 0; i < len; i++) {
            pos = pos.offset(stepX, 0, stepZ);
            if (i % liftEvery == 0)
                pos = pos.above();
            if (!withinBuildHeight(level, pos))
                break;

            // \*\*Kollisions\-Regel:\*\* keine Logs direkt auf/nebeneinander zu bestehenden
            // Ast\-Logs
            boolean conflict = false;
            if (placedBranchLogs.get().contains(pos))
                conflict = true;
            else {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    if (placedBranchLogs.get().contains(pos.relative(d))) {
                        conflict = true;
                        break;
                    }
                }
            }
            if (conflict)
                break; // Ast hier hart abbrechen

            if (canReplaceForLog(level, pos)) {
                placeLog(level, pos, axis, cfg, rnd, true);
            } else {
                break;
            }

            // Seitenast: ebenfalls Kollisionsprüfung
            if (i > 1 && i < len - 1 && rnd.nextFloat() < 0.25f) {
                Direction sideDir = (Math.abs(stepX) >= Math.abs(stepZ))
                        ? (stepZ >= 0 ? Direction.SOUTH : Direction.NORTH)
                        : (stepX >= 0 ? Direction.EAST : Direction.WEST);
                BlockPos side = pos.relative(sideDir);

                boolean sideConflict = placedBranchLogs.get().contains(side);
                if (!sideConflict) {
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        if (placedBranchLogs.get().contains(side.relative(d))) {
                            sideConflict = true;
                            break;
                        }
                    }
                }
                if (!sideConflict && canReplaceForLog(level, side)) {
                    placeLog(level, side, axis, cfg, rnd, true);
                }
            }

            if (i >= len - 2) {
                float chance = inCrown ? 0.25f : 0.08f;
                if (rnd.nextFloat() < chance) {
                    placeLeafCross(level, pos, cfg, rnd);
                }
            }
        }

        int endBlob = inCrown ? 2 + rnd.nextInt(2) : 1 + rnd.nextInt(2);

        if (!inCrown && isNearTrunk(pos, 1)) {
            branchEndpoints.get().add(pos.immutable());
            return;
        }

        boolean forbidNearTrunk = !inCrown;
        genLeafBlob(level, pos, endBlob, cfg, rnd, forbidNearTrunk);
        branchEndpoints.get().add(pos.immutable());
    }

    private void placeLeafCross(WorldGenLevel level, BlockPos p, ElderGiantTreeConfig cfg, RandomSource rnd) {
        int padding = 1; // oder 2, wenn du noch konservativer sein willst

        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos q = p.relative(d);
            if (isNearTrunk(q, padding))
                continue;
            if (canReplaceForLeaves(level, q)) {
                plannedLeaves.get().add(q.immutable());
            }
        }
        BlockPos up = p.above();
        if (!isNearTrunk(up, padding) && canReplaceForLeaves(level, up)) {
            plannedLeaves.get().add(up.immutable());
        }
        BlockPos down = p.below();
        if (!isNearTrunk(down, padding) && canReplaceForLeaves(level, down)) {
            plannedLeaves.get().add(down.immutable());
        }
    }

    private void finalizePlannedLeaves(WorldGenLevel level, ElderGiantTreeConfig cfg, long baseSeed) {
        Set<BlockPos> planned = plannedLeaves.get();
        if (planned.isEmpty())
            return;
        Set<BlockPos> snapshot = new HashSet<>(planned);

        // Identify Scarlet Tree by 0 flowering chance
        boolean isScarletTree = cfg.floweringChance() <= 0.0f;

        for (BlockPos p : snapshot) {
            if (!isChunkLoaded(level, p))
                continue;
            RandomSource r = RandomSource.create(p.asLong() ^ baseSeed);

            // Culling removed to ensure full leaf blobs as requested
            // if (isScarletTree && r.nextFloat() < 0.7f) {
            // continue;
            // }

            placeLeaves(level, p, cfg, r, snapshot);

            // Generate Scarlet Vines hanging from leaves for Scarlet Trees
            if (isScarletTree && r.nextFloat() < 0.20f) {
                BlockPos below = p.below();
                if (withinBuildHeight(level, below) && level.isEmptyBlock(below)) {
                    level.setBlock(below, ModBlocks.SCARLET_VINE.get().defaultBlockState(), 2);

                    // Chance to grow down further
                    if (r.nextFloat() < 0.4f) {
                        BlockPos below2 = below.below();
                        if (withinBuildHeight(level, below2) && level.isEmptyBlock(below2)) {
                            level.setBlock(below2, ModBlocks.SCARLET_VINE.get().defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
        planned.clear();
    }

    private void randomizeFloweringLeaves(WorldGenLevel level, ElderGiantTreeConfig cfg, long baseSeed) {
        float floweringChance = cfg.floweringChance();
        if (floweringChance <= 0.0f)
            return;

        Set<BlockPos> candidates = new HashSet<>();

        // Blätter in der Nähe ALLER Logs sammeln
        for (BlockPos log : placedAllLogs.get()) {
            int r = 6;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos p = log.offset(dx, dy, dz);
                        if (!withinBuildHeight(level, p))
                            continue;
                        BlockState s = level.getBlockState(p);
                        if (s.is(BlockTags.LEAVES)) {
                            candidates.add(p.immutable());
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty())
            return;

        // globale Zufallsquelle statt positionsbasiertem Seed
        RandomSource rnd = RandomSource.create(baseSeed ^ 0x9E3779B97F4A7C15L);

        // Kandidaten mischen, damit die Auswahl möglichst „rauschig“ wird
        java.util.List<BlockPos> shuffled = new java.util.ArrayList<>(candidates);
        java.util.Collections.shuffle(shuffled, new java.util.Random(baseSeed));

        for (BlockPos p : shuffled) {
            if (!isChunkLoaded(level, p))
                continue;
            if (rnd.nextFloat() >= floweringChance)
                continue;

            BlockState current = level.getBlockState(p);
            if (!current.is(BlockTags.LEAVES))
                continue;

            BlockState flowering = cfg.floweringLeavesProvider().getState(rnd, p);

            if (current.hasProperty(LeavesBlock.DISTANCE) && flowering.hasProperty(LeavesBlock.DISTANCE)) {
                flowering = flowering.setValue(LeavesBlock.DISTANCE, current.getValue(LeavesBlock.DISTANCE));
            }
            if (current.hasProperty(LeavesBlock.PERSISTENT) && flowering.hasProperty(LeavesBlock.PERSISTENT)) {
                flowering = flowering.setValue(LeavesBlock.PERSISTENT, current.getValue(LeavesBlock.PERSISTENT));
            }

            level.setBlock(p, flowering, 3);
        }
    }

    private void ensureLeavesAroundBranchLogs(WorldGenLevel level, ElderGiantTreeConfig cfg, int canopyCenterY,
            int canopyRadius) {
        for (BlockPos logPos : placedBranchLogs.get()) {
            boolean inCrown = canopyRadius > 0 && logPos.getY() >= canopyCenterY - Math.max(1, canopyRadius / 2);

            boolean allow = inCrown;
            if (!allow) {
                if (branchEndpoints.get().contains(logPos))
                    allow = true;
                else {
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        if (branchEndpoints.get().contains(logPos.relative(d))) {
                            allow = true;
                            break;
                        }
                    }
                }
            }
            if (!allow)
                continue;

            int extraR;
            long seed = logPos.asLong() ^ 0xC13FA9A902A6328FL;
            RandomSource rnd = RandomSource.create(seed);
            extraR = inCrown ? 3 + rnd.nextInt(2) : 1 + rnd.nextInt(2);

            int r2 = extraR * extraR;
            for (int ox = -extraR; ox <= extraR; ox++) {
                for (int oy = -extraR; oy <= extraR; oy++) {
                    for (int oz = -extraR; oz <= extraR; oz++) {
                        if (ox * ox + oy * oy + oz * oz <= r2 + rnd.nextInt(2)) {
                            BlockPos q = logPos.offset(ox, oy, oz);
                            if (!withinBuildHeight(level, q))
                                continue;
                            BlockState existing = level.getBlockState(q);
                            if (existing.getBlock() instanceof RotatedPillarBlock)
                                continue;
                            if (canReplaceForLeaves(level, q)) {
                                plannedLeaves.get().add(q.immutable());
                            }
                        }
                    }
                }
            }
        }
    }

    private void placeRootCluster(WorldGenLevel level, BlockPos center, int thickness, ElderGiantTreeConfig cfg,
            RandomSource rnd, boolean markBranch) {
        for (int rx = -thickness; rx <= thickness; rx++) {
            for (int rz = -thickness; rz <= thickness; rz++) {
                BlockPos p = center.offset(rx, 0, rz);
                if (!withinBuildHeight(level, p))
                    continue;
                if (canReplaceForLog(level, p)) {
                    placeLog(level, p, Direction.Axis.Y, cfg, rnd, markBranch, cfg.rootProvider());
                }
            }
        }
    }

    private int computeDistanceToLog(WorldGenLevel level, BlockPos start, Set<BlockPos> futureLeaves) {
        if (hasAdjacentLog(level, start))
            return 1;

        java.util.Deque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.Set<BlockPos> visited = new HashSet<>();
        queue.addLast(start);
        visited.add(start);

        int depth = 0;
        // vorher depth < 6, jetzt depth < 7 um bis Distanz 7 zu suchen
        while (!queue.isEmpty() && depth < 7) {
            int layerSize = queue.size();
            depth++;
            for (int i = 0; i < layerSize; i++) {
                BlockPos p = queue.removeFirst();
                for (Direction d : Direction.values()) {
                    BlockPos n = p.relative(d);
                    if (!withinBuildHeight(level, n) || visited.contains(n))
                        continue;
                    visited.add(n);
                    BlockState ns = level.getBlockState(n);
                    // erkenne Logs auch über den BlockTag und placedBranchLogs
                    if (ns.getBlock() instanceof RotatedPillarBlock) {
                        return depth;
                    }
                    if (ns.is(BlockTags.LOGS)) {
                        return depth;
                    }
                    if (placedAllLogs.get().contains(n) || placedBranchLogs.get().contains(n)) {
                        return depth;
                    }
                    if (ns.is(BlockTags.LEAVES) || (futureLeaves != null && futureLeaves.contains(n))) {
                        queue.addLast(n);
                    }
                }
            }
        }
        return 7;
    }

    private boolean containsNonReplaceableBlocks(WorldGenLevel level, BlockPos origin, int height, int trunkSize,
            int canopyR) {
        if (UpgradedSaplingBlock.IS_FORCING_GROWTH.get())
            return false;
        int extra = 2; // prüfen um +2 Blöcke erweitern
        int radiusX = canopyR + trunkSize + 2 + extra;
        int radiusZ = canopyR + trunkSize + 2 + extra;
        int minY = origin.getY() - extra;
        int maxY = origin.getY() + height + canopyR + 2 + extra;

        for (int y = minY; y <= maxY; y++) {
            for (int dx = -radiusX; dx <= radiusX; dx++) {
                for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                    BlockPos p = origin.offset(dx, y - origin.getY(), dz);
                    if (!withinBuildHeight(level, p))
                        continue;
                    try {
                        BlockState s = level.getBlockState(p);
                        // Wenn der Block den geschützten Tag hat -> Abbruch
                        if (s.is(ModTags.Blocks.ELDERWOODS_PROTECTED_STRUCTURE_BLOCKS)) {
                            return true;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return false;
    }

    private int computeDistanceToLog(WorldGenLevel level, BlockPos start) {
        return computeDistanceToLog(level, start, null);
    }

    private void placeLeaves(WorldGenLevel level, BlockPos pos, ElderGiantTreeConfig cfg, RandomSource rnd,
            Set<BlockPos> futureLeaves) {
        if (!isChunkLoaded(level, pos))
            return;

        BlockState existing = level.getBlockState(pos);
        if (existing.getBlock() instanceof RotatedPillarBlock)
            return;

        // immer zuerst normale Blätter setzen
        BlockState state = cfg.leavesProvider().getState(rnd, pos);

        if (state.hasProperty(LeavesBlock.DISTANCE)) {
            int dist = computeDistanceToLog(level, pos, futureLeaves);
            dist = Math.max(1, Math.min(6, dist));
            state = state.setValue(LeavesBlock.DISTANCE, dist);
        }

        boolean persistent = hasAdjacentLog(level, pos);
        if (state.hasProperty(LeavesBlock.PERSISTENT)) {
            state = state.setValue(LeavesBlock.PERSISTENT, persistent);
        }

        boolean placeInWorld = true;
        if (UpgradedSaplingBlock.IS_FORCING_GROWTH.get()) {
            boolean replaceable = existing.isAir() || existing.is(BlockTags.LEAVES) || existing.canBeReplaced()
                    || existing.is(Blocks.GRASS_BLOCK) || existing.is(Blocks.TALL_GRASS);
            if (!replaceable) {
                placeInWorld = false;
            }
        }

        if (placeInWorld) {
            level.setBlock(pos, state, 3);
        }
    }

    private void placeLeaves(WorldGenLevel level, BlockPos pos, ElderGiantTreeConfig cfg, RandomSource rnd) {
        placeLeaves(level, pos, cfg, rnd, null);
    }

    private boolean hasAdjacentLog(WorldGenLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!withinBuildHeight(level, n))
                continue;
            BlockState ns = level.getBlockState(n);
            // akzeptiere sowohl RotatedPillarBlock-Instanzen als auch Blocks mit dem
            // logs-Tag
            if (ns.getBlock() instanceof RotatedPillarBlock)
                return true;
            if (ns.is(BlockTags.LOGS))
                return true;
            if (placedBranchLogs.get().contains(n))
                return true;
            if (placedAllLogs.get().contains(n))
                return true;
        }
        return false;
    }

    private void tryPlaceSporeBlossomUnderBranch(WorldGenLevel level, BlockPos branchPos, ElderGiantTreeConfig cfg,
            RandomSource rnd) {
        if (!cfg.placeSporeBlossoms())
            return;

        // 15% Chance
        if (rnd.nextFloat() > 0.15f)
            return;
        int canopyCenterY = canopyCenterYLocal.get();
        int canopyRadius = canopyRadiusLocal.get();

        // Nur unterhalb der Baumkrone Spore Blossoms zulassen
        if (branchPos.getY() >= canopyCenterY - Math.max(1, canopyRadius / 2)) {
            return;
        }

        if (rnd.nextDouble() >= 0.008)
            return;
        if (!isChunkLoaded(level, branchPos))
            return;

        BlockPos target = branchPos.below();
        if (!withinBuildHeight(level, target))
            return;

        BlockState above = level.getBlockState(branchPos);
        boolean aboveIsLog = above.getBlock() instanceof RotatedPillarBlock
                || placedBranchLogs.get().contains(branchPos);
        if (!aboveIsLog)
            return;

        if (!canReplaceForLeaves(level, target))
            return;
        if (!isChunkLoaded(level, target))
            return;

        BlockState spore = ModBlocks.ELDER_SPORE_BLOSSOM.get().defaultBlockState();
        level.setBlock(target, spore, 2);
    }

    private boolean isChunkLoaded(WorldGenLevel level, BlockPos pos) {
        int cx = Math.floorDiv(pos.getX(), 16);
        int cz = Math.floorDiv(pos.getZ(), 16);
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

    private boolean canReplaceForLog(WorldGenLevel level, BlockPos pos) {
        if (UpgradedSaplingBlock.IS_FORCING_GROWTH.get())
            return true;
        BlockState s = level.getBlockState(pos);
        if (!s.getFluidState().isEmpty())
            return false;
        return s.isAir() || s.is(BlockTags.REPLACEABLE) || s.is(BlockTags.LEAVES) || s.is(Blocks.GRASS_BLOCK)
                || s.is(Blocks.TALL_GRASS);
    }

    private boolean canReplaceForLeaves(WorldGenLevel level, BlockPos pos) {
        if (UpgradedSaplingBlock.IS_FORCING_GROWTH.get())
            return true;
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof RotatedPillarBlock)
            return false;
        if (!s.getFluidState().isEmpty())
            return false;
        return s.isAir() || s.is(BlockTags.REPLACEABLE) || s.is(BlockTags.LEAVES) || s.is(Blocks.GRASS_BLOCK)
                || s.is(Blocks.TALL_GRASS);
    }

    private boolean withinBuildHeight(WorldGenLevel level, BlockPos pos) {
        int y = pos.getY();
        return y >= level.getMinBuildHeight() && y < level.getMaxBuildHeight();
    }

    private Direction randomHorizontal(RandomSource rnd) {
        return Direction.Plane.HORIZONTAL.getRandomDirection(rnd);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}