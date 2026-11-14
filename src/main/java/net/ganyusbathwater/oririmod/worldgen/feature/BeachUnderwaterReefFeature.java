package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator;
import net.ganyusbathwater.oririmod.worldgen.feature.OceanEcologyHelper.SilentOceanHabitat;
import net.ganyusbathwater.oririmod.worldgen.structure.OceanMonumentPlacementHelper;
import net.ganyusbathwater.oririmod.worldgen.structure.OceanMonumentPlacementHelper.MonumentProximity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Authentic Underwater Reef Feature.
 * Generates organic, branching vanilla-styled coral formations:
 * 1. Coral Trees: 3D branching antler colonies.
 * 2. Coral Claws: Curving arcs reaching toward the surface.
 * 3. Coral Mushrooms: Organic dome-shaped colonies.
 * Complete with outward-facing wall fans on sides and coral flora on top.
 * Zero flat 3x3 platforms!
 */
public class BeachUnderwaterReefFeature extends Feature<NoneFeatureConfiguration> {

    private record CoralType(BlockState block, BlockState plant, BlockState fan, BlockState wallFan) {
    }

    private static final CoralType[] CORAL_TYPES = new CoralType[] {
            new CoralType(
                    Blocks.TUBE_CORAL_BLOCK.defaultBlockState(),
                    Blocks.TUBE_CORAL.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
                    Blocks.TUBE_CORAL_FAN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
                    Blocks.TUBE_CORAL_WALL_FAN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true)),
            new CoralType(
                    Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(),
                    Blocks.BRAIN_CORAL.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
                    Blocks.BRAIN_CORAL_FAN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
                    Blocks.BRAIN_CORAL_WALL_FAN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true)),
            new CoralType(
                    Blocks.BUBBLE_CORAL_BLOCK.defaultBlockState(),
                    Blocks.BUBBLE_CORAL.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
                    Blocks.BUBBLE_CORAL_FAN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
                    Blocks.BUBBLE_CORAL_WALL_FAN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true)),
            new CoralType(
                    Blocks.FIRE_CORAL_BLOCK.defaultBlockState(),
                    Blocks.FIRE_CORAL.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
                    Blocks.FIRE_CORAL_FAN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
                    Blocks.FIRE_CORAL_WALL_FAN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true)),
            new CoralType(
                    Blocks.HORN_CORAL_BLOCK.defaultBlockState(),
                    Blocks.HORN_CORAL.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
                    Blocks.HORN_CORAL_FAN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
                    Blocks.HORN_CORAL_WALL_FAN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true))
    };

    public BeachUnderwaterReefFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        if (!level.getBlockState(origin).is(Blocks.WATER)) {
            return false;
        }

        // 1. Locate the solid seabed under water
        BlockPos.MutableBlockPos mPos = origin.mutable();
        while (mPos.getY() > level.getMinBuildHeight() + 5 && level.getBlockState(mPos).is(Blocks.WATER)) {
            mPos.move(Direction.DOWN);
        }

        BlockPos floorPos = mPos.immutable();
        BlockState floorState = level.getBlockState(floorPos);
        BlockPos waterPos = floorPos.above();

        // Must be submerged in photic water below sea level (35 <= Y < 71)
        if (!level.getBlockState(waterPos).is(Blocks.WATER) || floorPos.getY() >= 71 || floorPos.getY() < 35) {
            return false;
        }

        MonumentProximity monumentProximity = OceanMonumentPlacementHelper.getMonumentProximity(level, floorPos);
        if (monumentProximity == MonumentProximity.INSIDE) {
            return false;
        }
        boolean nearMonument = monumentProximity == MonumentProximity.HALO;

        if (!floorState.isSolid()) {
            return false;
        }

        long seed = level.getSeed();
        Holder<Biome> biome = level.getBiome(floorPos);
        boolean isSilentOcean = biome.is(ElderwoodsChunkGenerator.THE_SILENT_OCEANS_KEY);

        boolean allowRock = OceanEcologyHelper.shouldSpawnRockFormation(
                seed, floorPos.getX(), floorPos.getZ(), isSilentOcean, nearMonument);

        boolean allowCoral = false;
        if (!nearMonument) {
            float canopyMask = OceanEcologyHelper.getFloraCanopyMask(seed, floorPos.getX(), floorPos.getZ());
            if (isSilentOcean) {
                SilentOceanHabitat habitat = OceanEcologyHelper.getSilentOceanHabitat(
                        seed, floorPos.getX(), floorPos.getZ());
                allowCoral = (habitat == SilentOceanHabitat.CORAL_GARDEN
                        || habitat == SilentOceanHabitat.MIXED_REEF) && canopyMask > 0.26f;
            } else {
                allowCoral = OceanEcologyHelper.isBeachCoralCove(seed, floorPos.getX(), floorPos.getZ())
                        && canopyMask > 0.28f;
            }
        }

        if (!allowRock && !allowCoral) {
            return false;
        }

        Set<BlockPos> placedBlocks = new HashSet<>();

        // Rock formations are gated into tight shoals/coves rather than competing equally with every coral roll
        boolean tryRock = allowRock && (!allowCoral || random.nextFloat() < (isSilentOcean ? 0.45f : 0.60f));
        if (tryRock) {
            float spawnChance = nearMonument ? 0.24f : (isSilentOcean ? 0.42f : 0.52f);
            if (random.nextFloat() >= spawnChance) {
                if (!allowCoral) {
                    return false;
                }
            } else {
                placeNaturalStoneFormation(level, floorPos, isSilentOcean, nearMonument, random, placedBlocks);
                if (!placedBlocks.isEmpty() && allowCoral && random.nextFloat() < 0.35f) {
                    CoralType accentType = CORAL_TYPES[random.nextInt(CORAL_TYPES.length)];
                    decorateRockWithLightAccents(level, accentType, placedBlocks, random);
                }
                return !placedBlocks.isEmpty();
            }
        }

        if (!allowCoral) {
            return false;
        }

        CoralType type = CORAL_TYPES[random.nextInt(CORAL_TYPES.length)];
        int coralVariant = random.nextInt(3);
        if (coralVariant == 0) {
            placeCoralTree(level, waterPos, type, random, placedBlocks);
        } else if (coralVariant == 1) {
            placeCoralClaw(level, waterPos, type, random, placedBlocks);
        } else {
            placeCoralMushroom(level, waterPos, type, random, placedBlocks);
        }

        decorateCoralBlocks(level, type, placedBlocks, random);
        scatterAquaticFlora(level, floorPos, seed, isSilentOcean, random);

        return !placedBlocks.isEmpty();
    }

    private void placeCoralTree(WorldGenLevel level, BlockPos base, CoralType type, RandomSource random,
            Set<BlockPos> placed) {
        int trunkHeight = 2 + random.nextInt(2);
        for (int y = 0; y < trunkHeight; y++) {
            BlockPos p = base.above(y);
            if (level.getBlockState(p).is(Blocks.WATER)) {
                level.setBlock(p, type.block, 2);
                placed.add(p);
            }
        }

        BlockPos trunkTop = base.above(trunkHeight - 1);
        Direction[] dirs = Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new);
        int numBranches = 2 + random.nextInt(2);

        for (int i = 0; i < numBranches; i++) {
            Direction dir = dirs[random.nextInt(dirs.length)];
            BlockPos current = trunkTop;
            int branchLength = 2 + random.nextInt(3);

            for (int s = 0; s < branchLength; s++) {
                current = current.relative(dir);
                if (s >= 1 || random.nextBoolean()) {
                    current = current.above();
                }
                if (level.getBlockState(current).is(Blocks.WATER) && current.getY() < 71) {
                    level.setBlock(current, type.block, 2);
                    placed.add(current);
                }
                // Optional sub-branch fork at the tip
                if (s == branchLength - 1 && random.nextBoolean()) {
                    Direction side = random.nextBoolean() ? dir.getClockWise() : dir.getCounterClockWise();
                    BlockPos tipFork = current.relative(side).above();
                    if (level.getBlockState(tipFork).is(Blocks.WATER) && tipFork.getY() < 71) {
                        level.setBlock(tipFork, type.block, 2);
                        placed.add(tipFork);
                    }
                }
            }
        }
    }

    private void placeCoralClaw(WorldGenLevel level, BlockPos base, CoralType type, RandomSource random,
            Set<BlockPos> placed) {
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int length = 3 + random.nextInt(3);
        BlockPos current = base;

        for (int s = 0; s < length; s++) {
            if (s > 0) {
                current = current.relative(dir).above();
            }
            if (level.getBlockState(current).is(Blocks.WATER) && current.getY() < 71) {
                level.setBlock(current, type.block, 2);
                placed.add(current);
            }
            if (s >= 2 && random.nextBoolean()) {
                BlockPos clawTip = current.relative(dir.getClockWise()).above();
                if (level.getBlockState(clawTip).is(Blocks.WATER) && clawTip.getY() < 71) {
                    level.setBlock(clawTip, type.block, 2);
                    placed.add(clawTip);
                }
            }
        }
    }

    private void placeCoralMushroom(WorldGenLevel level, BlockPos base, CoralType type, RandomSource random,
            Set<BlockPos> placed) {
        // Multi-spired Horn Coral Colony (tall central horn + 2-3 asymmetric ascending
        // spires)
        int centerHeight = 3 + random.nextInt(3);
        for (int y = 0; y < centerHeight; y++) {
            BlockPos p = base.above(y);
            if (level.getBlockState(p).is(Blocks.WATER) && p.getY() < 71) {
                level.setBlock(p, type.block, 2);
                placed.add(p);
            }
        }

        Direction[] dirs = Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new);
        int spires = 2 + random.nextInt(2);
        for (int i = 0; i < spires; i++) {
            Direction dir = dirs[random.nextInt(dirs.length)];
            int spireHeight = 2 + random.nextInt(centerHeight);
            BlockPos spireBase = base.relative(dir);
            for (int y = 0; y < spireHeight; y++) {
                BlockPos sp = spireBase.above(y);
                if (level.getBlockState(sp).is(Blocks.WATER) && sp.getY() < 71) {
                    level.setBlock(sp, type.block, 2);
                    placed.add(sp);
                }
            }
        }
    }

    private enum RockFormationStyle {
        PINNACLE_CLUSTER,
        LAYERED_SEA_STACK,
        TIDAL_REEF_LEDGE,
        LEANING_FIN
    }

    /**
     * Generates varied, anisotropic geological stone formations (craggy pinnacle clusters,
     * stratified sea stacks, low reef shelves with tumbled boulders, and sweeping leaning fins)
     * with solid seabed grounding and monument perimeter safety.
     */
    private void placeNaturalStoneFormation(WorldGenLevel level, BlockPos base, boolean isSilentOcean,
            boolean nearMonument, RandomSource random, Set<BlockPos> placed) {
        RockFormationStyle[] styles = RockFormationStyle.values();
        RockFormationStyle style = styles[random.nextInt(styles.length)];
        int waterDepth = Math.max(2, ElderwoodsChunkGenerator.BASE_HEIGHT - base.getY());
        boolean canBreachSurface = !nearMonument && base.getY() >= 54 && random.nextFloat() < (isSilentOcean ? 0.38f : 0.52f);

        // Track lowest Y per (x, z) column so every base edge anchors cleanly into sloped seabeds
        Map<Long, Integer> columnLowestY = new HashMap<>();

        switch (style) {
            case PINNACLE_CLUSTER -> {
                int mainHeight = canBreachSurface
                        ? waterDepth + 3 + random.nextInt(5)
                        : Mth.clamp(8 + random.nextInt(6), 6, Math.max(7, waterDepth));
                double yaw = random.nextDouble() * Math.PI * 2.0;
                double leanAngle = yaw + (random.nextDouble() - 0.5) * 0.9;
                double mainLean = (mainHeight >= 8) ? (1.6 + random.nextDouble() * 2.1) : 0.8;
                sculptErodedSpire(level, base, mainHeight,
                        3.0 + random.nextDouble() * 1.3, 2.1 + random.nextDouble() * 0.9,
                        yaw, leanAngle, mainLean, false, random, placed, columnLowestY);

                // Add 1-3 smaller asymmetrical satellite pinnacles welded to the root
                int satellites = 1 + random.nextInt(3);
                for (int s = 0; s < satellites; s++) {
                    double sa = random.nextDouble() * Math.PI * 2.0;
                    int dist = 3 + random.nextInt(3);
                    BlockPos satBase = base.offset(
                            (int) Math.round(Math.cos(sa) * dist),
                            -1,
                            (int) Math.round(Math.sin(sa) * dist));
                    int satH = Math.max(5, (int) Math.round(mainHeight * (0.48 + random.nextDouble() * 0.30)));
                    sculptErodedSpire(level, satBase, satH,
                            1.9 + random.nextDouble() * 0.8, 1.4 + random.nextDouble() * 0.6,
                            sa + 0.5, sa, 0.9, false, random, placed, columnLowestY);
                }
            }
            case LAYERED_SEA_STACK -> {
                int height = canBreachSurface
                        ? waterDepth + 2 + random.nextInt(4)
                        : Mth.clamp(7 + random.nextInt(5), 6, Math.max(7, waterDepth));
                double yaw = random.nextDouble() * Math.PI * 2.0;
                double rx = 3.4 + random.nextDouble() * 1.4;
                double rz = 2.5 + random.nextDouble() * 1.0;
                sculptStratifiedStack(level, base, height, rx, rz, yaw, random, placed, columnLowestY);
            }
            case TIDAL_REEF_LEDGE -> {
                double yaw = random.nextDouble() * Math.PI * 2.0;
                int lengthSteps = 4 + random.nextInt(3);
                double dirX = Math.cos(yaw);
                double dirZ = Math.sin(yaw);
                for (int step = -lengthSteps / 2; step <= lengthSteps / 2; step++) {
                    int ox = (int) Math.round(step * dirX * 1.7);
                    int oz = (int) Math.round(step * dirZ * 1.7);
                    int segHeight = 4 + random.nextInt(4) - (Math.abs(step) == lengthSteps / 2 ? 1 : 0);
                    segHeight = Math.max(3, segHeight);
                    sculptErodedSpire(level, base.offset(ox, -1, oz), segHeight + 1,
                            2.3 + random.nextDouble() * 0.9, 1.6 + random.nextDouble() * 0.6,
                            yaw, yaw, 0.5, true, random, placed, columnLowestY);
                }
                // Scatter 2-4 loose tumbled seabed boulders beside the main ridge
                int boulders = 2 + random.nextInt(3);
                for (int b = 0; b < boulders; b++) {
                    double ba = random.nextDouble() * Math.PI * 2.0;
                    int bDist = 4 + random.nextInt(4);
                    BlockPos bPos = base.offset(
                            (int) Math.round(Math.cos(ba) * bDist),
                            -1,
                            (int) Math.round(Math.sin(ba) * bDist));
                    sculptBoulder(level, bPos, 1.6 + random.nextDouble() * 0.8, random, placed, columnLowestY);
                }
            }
            case LEANING_FIN -> {
                int height = canBreachSurface
                        ? waterDepth + 4 + random.nextInt(4)
                        : Mth.clamp(8 + random.nextInt(6), 7, Math.max(8, waterDepth));
                double yaw = random.nextDouble() * Math.PI * 2.0;
                sculptErodedSpire(level, base, height,
                        3.6 + random.nextDouble() * 1.3, 1.9 + random.nextDouble() * 0.7,
                        yaw, yaw + (random.nextDouble() - 0.5) * 0.4,
                        2.8 + random.nextDouble() * 2.2, false, random, placed, columnLowestY);
                // Add a compact counter-buttress at the rear heel
                BlockPos heelBase = base.offset(
                        (int) Math.round(-Math.cos(yaw) * 2.5),
                        -1,
                        (int) Math.round(-Math.sin(yaw) * 2.5));
                sculptErodedSpire(level, heelBase, Math.max(4, height / 2),
                        2.0, 1.5, yaw + 0.7, yaw + Math.PI, 0.5, false, random, placed, columnLowestY);
            }
        }

        // Plug any stray 1-block cavities or rasterization slits before anchoring column footings
        fillInteriorRockPinholes(level, placed, base.getY(), random, columnLowestY);

        // Ground all lower column footings deep enough into sloped seabeds so no stone slabs float
        anchorFormationColumns(level, base.getY(), columnLowestY, random);
    }

    private void sculptErodedSpire(WorldGenLevel level, BlockPos base, int height,
            double baseRx, double baseRz, double yaw, double leanAngle, double totalLean,
            boolean flatTop, RandomSource random, Set<BlockPos> placed, Map<Long, Integer> columnLowestY) {
        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);
        double leanX = Math.cos(leanAngle);
        double leanZ = Math.sin(leanAngle);
        double phase1 = random.nextDouble() * Math.PI * 2.0;
        double phase2 = random.nextDouble() * Math.PI * 2.0;

        for (int y = -1; y < height; y++) {
            double progress = Math.max(0.0, (double) y / Math.max(1, height - 1));
            double offsetX = Math.pow(progress, 1.15) * totalLean * leanX;
            double offsetZ = Math.pow(progress, 1.15) * totalLean * leanZ;

            double taper = flatTop
                    ? (1.0 - progress * 0.28)
                    : (y < 0 ? 1.15 : Math.pow(Math.max(0.12, 1.0 - progress * 0.78), 0.54));
            double minRadius = (y >= height - 1 && !flatTop) ? 0.85 : 1.12;
            double rx = Math.max(minRadius, baseRx * taper);
            double rz = Math.max(minRadius, baseRz * taper);
            int maxScan = (int) Math.ceil(Math.max(rx, rz)) + 2;

            for (int dx = -maxScan; dx <= maxScan; dx++) {
                for (int dz = -maxScan; dz <= maxScan; dz++) {
                    double lx = dx - offsetX;
                    double lz = dz - offsetZ;
                    // Rotate into rock-local horizontal axes so shapes never align squarely to N/S axes
                    double u = lx * cosY + lz * sinY;
                    double v = -lx * sinY + lz * cosY;

                    double angle = Math.atan2(v, u);
                    int wx = base.getX() + dx;
                    int wy = base.getY() + y;
                    int wz = base.getZ() + dz;

                    // Spatially coherent harmonics avoid isolated single-voxel dropouts
                    double lobeFactor = 1.0
                            + 0.16 * Math.cos(angle * 2.0 + phase1)
                            + 0.11 * Math.sin(angle * 3.0 + y * 0.35 + phase2)
                            + 0.06 * smoothSpatialWave(wx, wy, wz, phase1, phase2);

                    double normDist = Math.sqrt((u * u) / (rx * rx) + (v * v) / (rz * rz));
                    if (normDist <= lobeFactor) {
                        placeRockVoxel(level, wx, wy, wz, base.getY(), random, placed, columnLowestY);
                    }
                }
            }
        }
    }

    private void sculptStratifiedStack(WorldGenLevel level, BlockPos base, int height,
            double baseRx, double baseRz, double yaw, RandomSource random,
            Set<BlockPos> placed, Map<Long, Integer> columnLowestY) {
        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);
        double phase1 = random.nextDouble() * Math.PI * 2.0;
        double phase2 = random.nextDouble() * Math.PI * 2.0;
        double strataShiftX = 0.0;
        double strataShiftZ = 0.0;

        for (int y = -1; y < height; y++) {
            double progress = Math.max(0.0, (double) y / Math.max(1, height));
            // Subtle natural horizontal bedding step offsets rather than smooth conical tapering
            if (y > 0 && y % 2 == 0) {
                strataShiftX += (random.nextDouble() - 0.5) * 0.45;
                strataShiftZ += (random.nextDouble() - 0.5) * 0.45;
            }

            // Carve subtle mid-height wave undercut notch above a broader flared seabed plinth
            double notch = 1.0;
            if (y == 0 || y == -1) {
                notch = 1.15;
            } else if (progress >= 0.30 && progress <= 0.58 && height >= 5) {
                notch = 0.84;
            } else if (progress > 0.78) {
                notch = 0.90 - (progress - 0.78) * 0.85;
            }

            double rx = Math.max(1.5, baseRx * notch);
            double rz = Math.max(1.3, baseRz * notch);
            int maxScan = (int) Math.ceil(Math.max(rx, rz)) + 2;

            for (int dx = -maxScan; dx <= maxScan; dx++) {
                for (int dz = -maxScan; dz <= maxScan; dz++) {
                    double lx = dx - strataShiftX;
                    double lz = dz - strataShiftZ;
                    double u = lx * cosY + lz * sinY;
                    double v = -lx * sinY + lz * cosY;

                    double angle = Math.atan2(v, u);
                    int wx = base.getX() + dx;
                    int wy = base.getY() + y;
                    int wz = base.getZ() + dz;

                    double lobe = 1.0
                            + 0.17 * Math.cos(angle * 2.0 + phase1)
                            + 0.10 * Math.sin(angle * 4.0 + phase2)
                            + 0.06 * smoothSpatialWave(wx, wy, wz, phase1, phase2);
                    double normDist = Math.sqrt((u * u) / (rx * rx) + (v * v) / (rz * rz));

                    // Stepped crest ledge along outer terrace ring without punching holes in the core
                    if (y == height - 1 && normDist > 0.58 && Math.sin(wx * 0.45 + wz * 0.35 + phase1) < -0.2) {
                        continue;
                    }

                    if (normDist <= lobe) {
                        placeRockVoxel(level, wx, wy, wz, base.getY(), random, placed, columnLowestY);
                    }
                }
            }
        }
    }

    private void sculptBoulder(WorldGenLevel level, BlockPos center, double radius,
            RandomSource random, Set<BlockPos> placed, Map<Long, Integer> columnLowestY) {
        int rInt = (int) Math.ceil(radius) + 1;
        double squashedY = Math.max(1.0, radius * 0.75);
        double phase = random.nextDouble() * Math.PI * 2.0;

        for (int dx = -rInt; dx <= rInt; dx++) {
            for (int dy = -1; dy <= rInt; dy++) {
                for (int dz = -rInt; dz <= rInt; dz++) {
                    int wx = center.getX() + dx;
                    int wy = center.getY() + dy;
                    int wz = center.getZ() + dz;
                    double dist = Math.sqrt((dx * dx) / (radius * radius)
                            + (dy * dy) / (squashedY * squashedY)
                            + (dz * dz) / (radius * radius));
                    double jitter = 0.88 + 0.20 * Math.sin(dx * 0.75 + phase) * Math.cos(dz * 0.75)
                            + 0.07 * smoothSpatialWave(wx, wy, wz, phase, phase * 1.3);
                    if (dist <= jitter) {
                        placeRockVoxel(level, wx, wy, wz, center.getY(), random, placed, columnLowestY);
                    }
                }
            }
        }
    }

    private static double smoothSpatialWave(int wx, int wy, int wz, double phase1, double phase2) {
        return Math.sin(wx * 0.42 + wy * 0.22 + phase1) * Math.cos(wz * 0.42 - wy * 0.16 + phase2);
    }

    /**
     * Fills isolated 1-block interior cavities or slits created by voxel discretization
     * of leaning fins or merging satellite spires without blurring convex rock silhouettes.
     */
    private void fillInteriorRockPinholes(WorldGenLevel level, Set<BlockPos> placed, int baseY,
            RandomSource random, Map<Long, Integer> columnLowestY) {
        if (placed.isEmpty()) {
            return;
        }
        Set<BlockPos> candidates = new HashSet<>();
        for (BlockPos rockPos : placed) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos adj = rockPos.relative(dir);
                if (!placed.contains(adj)) {
                    candidates.add(adj);
                }
            }
        }

        List<BlockPos> holesToFill = new ArrayList<>();
        for (BlockPos cand : candidates) {
            if (!isSpikeReplaceable(level.getBlockState(cand))) {
                continue;
            }
            boolean north = placed.contains(cand.north());
            boolean south = placed.contains(cand.south());
            boolean east = placed.contains(cand.east());
            boolean west = placed.contains(cand.west());
            int horizontalNeighbors = (north ? 1 : 0) + (south ? 1 : 0) + (east ? 1 : 0) + (west ? 1 : 0);

            boolean pinchedSlit = (north && south && (east || west)) || (east && west && (north || south));
            boolean enclosedCavity = horizontalNeighbors >= 3
                    && (horizontalNeighbors == 4 || placed.contains(cand.above()) || placed.contains(cand.below()));
            if (pinchedSlit || enclosedCavity) {
                holesToFill.add(cand);
            }
        }

        for (BlockPos hole : holesToFill) {
            placeRockVoxel(level, hole.getX(), hole.getY(), hole.getZ(), baseY, random, placed, columnLowestY);
        }
    }

    private void placeRockVoxel(WorldGenLevel level, int wx, int wy, int wz, int baseY,
            RandomSource random, Set<BlockPos> placed, Map<Long, Integer> columnLowestY) {
        if (wy <= level.getMinBuildHeight() + 2 || wy >= level.getMaxBuildHeight() - 2) {
            return;
        }
        if (OceanMonumentPlacementHelper.getMonumentProximity(level, wx, wz) == MonumentProximity.INSIDE) {
            return;
        }
        BlockPos pos = new BlockPos(wx, wy, wz);
        BlockState existing = level.getBlockState(pos);
        if (!isSpikeReplaceable(existing) && wy >= baseY) {
            return;
        }
        // Allow root penetration into surface sand/gravel at wy < baseY so sides fuse cleanly
        if (wy < baseY && !isSpikeReplaceable(existing) && !existing.is(Blocks.SAND) && !existing.is(Blocks.GRAVEL)) {
            return;
        }

        BlockState rockBlock = pickGeologicalRock(wx, wy, wz, baseY, random);
        level.setBlock(pos, rockBlock, 2);
        placed.add(pos);

        long key = (((long) wx) << 32) | (wz & 0xFFFFFFFFL);
        columnLowestY.merge(key, wy, Math::min);
    }

    private void anchorFormationColumns(WorldGenLevel level, int baseY,
            Map<Long, Integer> columnLowestY, RandomSource random) {
        BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
        for (Map.Entry<Long, Integer> entry : columnLowestY.entrySet()) {
            int wx = (int) (entry.getKey() >> 32);
            int wz = (int) (long) entry.getKey();
            int lowY = entry.getValue();

            // Lower foothills & sloped rims must always seal down into the seabed;
            // allow only mild overhang steps high above the anchored base.
            if (lowY > baseY + 3) {
                belowPos.set(wx, lowY - 1, wz);
                if (!level.getBlockState(belowPos).isSolid()) {
                    // Check if adjacent column supports this overhang as a minor ledge; if empty beneath, ground it
                    if (level.getBlockState(belowPos.below()).is(Blocks.WATER)
                            && lowY > baseY + 4) {
                        continue;
                    }
                }
            }

            for (int depth = 1; depth <= 10; depth++) {
                int y = lowY - depth;
                if (y <= level.getMinBuildHeight() + 2) {
                    break;
                }
                belowPos.set(wx, y, wz);
                BlockState existing = level.getBlockState(belowPos);
                if (!isSpikeReplaceable(existing)) {
                    break;
                }
                level.setBlock(belowPos, pickGeologicalRock(wx, y, wz, baseY, random), 2);
            }
        }
    }

    private static BlockState pickGeologicalRock(int wx, int wy, int wz, int baseY, RandomSource random) {
        double strata = Math.sin(wy * 0.68 + wx * 0.14 - wz * 0.11);
        double grain = coordHash01(wx, wy, wz);

        if (wy >= 70 && wy <= 74 && grain > 0.78) {
            return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        }
        if (wy <= baseY && strata < -0.15) {
            return Blocks.TUFF.defaultBlockState();
        }
        if (strata > 0.35 || grain > 0.80) {
            return Blocks.ANDESITE.defaultBlockState();
        }
        if (wy >= 71 && random.nextFloat() < 0.38f) {
            return Blocks.COBBLESTONE.defaultBlockState();
        }
        return (grain < 0.22) ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }

    private static double coordHash01(int x, int y, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xBF58476D1CE4E5B9L ^ z * 0x94D049BB133111EBL;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        return (double) (h & 0xFFFFL) / 65535.0;
    }

    private static boolean isSpikeReplaceable(BlockState state) {
        return state.is(Blocks.WATER)
                || state.isAir()
                || state.is(Blocks.SEAGRASS)
                || state.is(Blocks.TALL_SEAGRASS)
                || state.is(Blocks.KELP)
                || state.is(Blocks.KELP_PLANT)
                || state.is(Blocks.SEA_PICKLE);
    }

    private void decorateRockWithLightAccents(WorldGenLevel level, CoralType type,
            Set<BlockPos> placedBlocks, RandomSource random) {
        for (BlockPos pos : placedBlocks) {
            if (pos.getY() >= ElderwoodsChunkGenerator.BASE_HEIGHT - 1) {
                continue;
            }
            BlockPos above = pos.above();
            if (level.getBlockState(above).is(Blocks.WATER) && !placedBlocks.contains(above)) {
                if (random.nextFloat() < 0.14f) {
                    level.setBlock(above, Blocks.SEAGRASS.defaultBlockState(), 2);
                } else if (random.nextFloat() < 0.08f) {
                    level.setBlock(above, type.fan, 2);
                } else if (random.nextFloat() < 0.05f) {
                    level.setBlock(above, Blocks.SEA_PICKLE.defaultBlockState()
                            .setValue(BlockStateProperties.PICKLES, 1 + random.nextInt(3))
                            .setValue(BlockStateProperties.WATERLOGGED, true), 2);
                }
            }
        }
    }

    private void decorateCoralBlocks(WorldGenLevel level, CoralType type, Set<BlockPos> placedBlocks,
            RandomSource random) {
        for (BlockPos pos : placedBlocks) {
            // A. Top face: Coral Fan or Coral Plant (Waterlogged)
            BlockPos above = pos.above();
            if (level.getBlockState(above).is(Blocks.WATER) && !placedBlocks.contains(above)) {
                if (random.nextFloat() < 0.65f) {
                    BlockState topFlora = random.nextBoolean() ? type.fan : type.plant;
                    level.setBlock(above, topFlora, 2);
                } else if (random.nextFloat() < 0.20f) {
                    // Sea pickles in clusters
                    int pickles = 1 + random.nextInt(4);
                    level.setBlock(above, Blocks.SEA_PICKLE.defaultBlockState()
                            .setValue(BlockStateProperties.PICKLES, pickles)
                            .setValue(BlockStateProperties.WATERLOGGED, true), 2);
                }
            }

            // B. Horizontal faces: Coral Wall Fan facing outward away from solid block
            for (Direction hDir : Direction.Plane.HORIZONTAL) {
                if (random.nextFloat() < 0.35f) {
                    BlockPos side = pos.relative(hDir);
                    if (level.getBlockState(side).is(Blocks.WATER) && !placedBlocks.contains(side)) {
                        BlockState wallFan = type.wallFan.setValue(BaseCoralWallFanBlock.FACING, hDir);
                        level.setBlock(side, wallFan, 2);
                    }
                }
            }
        }
    }

    private void scatterAquaticFlora(WorldGenLevel level, BlockPos floorPos, long seed,
            boolean isSilentOcean, RandomSource random) {
        int count = 3 + random.nextInt(5);
        for (int i = 0; i < count; i++) {
            BlockPos p = floorPos.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            if (OceanMonumentPlacementHelper.getMonumentProximity(level, p) == MonumentProximity.INSIDE) {
                continue;
            }
            if (OceanEcologyHelper.getFloraCanopyMask(seed, p.getX(), p.getZ()) < 0.25f) {
                continue;
            }
            boolean allowKelp = !isSilentOcean || OceanEcologyHelper.getSilentOceanHabitat(
                    seed, p.getX(), p.getZ()) == SilentOceanHabitat.MIXED_REEF;

            BlockPos waterPos = p.above();
            if (level.getBlockState(waterPos).is(Blocks.WATER) && level.getBlockState(p).isSolid()) {
                if (allowKelp && random.nextFloat() < 0.26f) {
                    int kelpHeight = 3 + random.nextInt(5);
                    for (int kh = 0; kh < kelpHeight; kh++) {
                        BlockPos kp = waterPos.above(kh);
                        if (level.getBlockState(kp).is(Blocks.WATER) && kp.getY() < 70) {
                            level.setBlock(kp, kh == kelpHeight - 1 ? Blocks.KELP.defaultBlockState()
                                    : Blocks.KELP_PLANT.defaultBlockState(), 2);
                        } else {
                            break;
                        }
                    }
                } else {
                    level.setBlock(waterPos, Blocks.SEAGRASS.defaultBlockState(), 2);
                }
            }
        }
    }
}
