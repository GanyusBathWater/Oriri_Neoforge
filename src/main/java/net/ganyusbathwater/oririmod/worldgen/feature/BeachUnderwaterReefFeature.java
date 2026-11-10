package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.HashSet;
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

        if (!floorState.isSolid()) {
            return false;
        }

        CoralType type = CORAL_TYPES[random.nextInt(CORAL_TYPES.length)];
        Set<BlockPos> placedCoralBlocks = new HashSet<>();

        int structureType = random.nextInt(5);
        if (structureType == 0) {
            // A. Authentic Organic Coral Tree (Vertical trunk + 2-3 branching arms curving
            // upward)
            placeCoralTree(level, waterPos, type, random, placedCoralBlocks);
        } else if (structureType == 1) {
            // B. Authentic Organic Coral Claw (Curving crescent arc reaching upward)
            placeCoralClaw(level, waterPos, type, random, placedCoralBlocks);
        } else if (structureType == 2) {
            // C. Authentic Organic Coral Mushroom (Rounded organic mound/dome)
            placeCoralMushroom(level, waterPos, type, random, placedCoralBlocks);
        } else if (structureType == 3) {
            // D. Small Stone Spike (Submerged 3-5 blocks tall)
            placeStoneSpike(level, floorPos, false, random, placedCoralBlocks);
        } else {
            // E. Big Sea Stack (Tall stone stack sticking outside of the water Y=74..77)
            placeStoneSpike(level, floorPos, true, random, placedCoralBlocks);
        }

        // Decorate placed coral blocks with outward-facing wall fans on sides and flora
        // on top
        decorateCoralBlocks(level, type, placedCoralBlocks, random);

        // Scatter surrounding seagrass and occasional kelp
        scatterAquaticFlora(level, floorPos, random);

        return !placedCoralBlocks.isEmpty();
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

    private void placeStoneSpike(WorldGenLevel level, BlockPos base, boolean isTall, RandomSource random,
            Set<BlockPos> placed) {
        if (base.getY() < 58) {
            return;
        }

        int height;
        int baseRadius;
        if (isTall) {
            // Big sea stack sticking 2 to 5 blocks out of the water (Y=74 to 77)
            int waterDepth = Math.max(1, 72 - base.getY());
            height = waterDepth + 2 + random.nextInt(4);
            baseRadius = 4 + random.nextInt(2); // 4 to 5 blocks wide base
        } else {
            // Small spike submerged underwater
            height = 3 + random.nextInt(3);
            baseRadius = 2 + random.nextInt(2); // 2 to 3 blocks wide base
        }

        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState andesite = Blocks.ANDESITE.defaultBlockState();
        BlockState cobblestone = Blocks.COBBLESTONE.defaultBlockState();

        for (int y = 0; y < height; y++) {
            BlockPos current = base.above(y);
            double progress = (double) y / height;
            int r = Math.max(1, (int) Math.round((1.0 - progress * 0.7) * baseRadius));

            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= r * r) {
                        BlockPos p = current.offset(dx, 0, dz);
                        BlockState existing = level.getBlockState(p);
                        if (existing.is(Blocks.WATER) || existing.isAir()) {
                            BlockState block = (p.getY() >= 71) ? (random.nextBoolean() ? cobblestone : stone)
                                    : (random.nextBoolean() ? stone : andesite);
                            level.setBlock(p, block, 2);
                            placed.add(p);
                        }
                    }
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

    private void scatterAquaticFlora(WorldGenLevel level, BlockPos floorPos, RandomSource random) {
        int count = 4 + random.nextInt(6);
        for (int i = 0; i < count; i++) {
            BlockPos p = floorPos.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            BlockPos waterPos = p.above();
            if (level.getBlockState(waterPos).is(Blocks.WATER) && level.getBlockState(p).isSolid()) {
                if (random.nextFloat() < 0.30f) {
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
