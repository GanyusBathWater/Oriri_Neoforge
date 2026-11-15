package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;

public class FallenLogsFeature extends Feature<BlockStateConfiguration> {

    public FallenLogsFeature(Codec<BlockStateConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockStateConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockStateConfiguration config = context.config();

        ChunkPos centerChunk = (level instanceof net.minecraft.server.level.WorldGenRegion wgr)
                ? wgr.getCenter()
                : null;

        if (config.state.is(ModBlocks.ELDER_PINE_LOG.get())) {
            return placeFallenPineTree(level, origin, random, config.state, centerChunk);
        }

        if (!level.getBlockState(origin.below()).isSolid() || !level.getBlockState(origin).isAir()) {
            return false;
        }

        // 1. Determine size (solid 2x2 to 4x4)
        int trunkSize = 2 + random.nextInt(3); // 2, 3, or 4

        // 2. Generate vertical stump with roots
        int stumpHeight = 2 + random.nextInt(3); // 2 to 4 blocks tall
        BlockState verticalLogState = config.state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        
        // The normal Elder Trees generate trunks from dx=0 to dx<trunkSize, and dz=0 to dz<trunkSize
        // Let's mimic that exact placement for the stump
        for (int x = 0; x < trunkSize; x++) {
            for (int z = 0; z < trunkSize; z++) {
                for (int y = 0; y < stumpHeight; y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (level.getBlockState(pos).canBeReplaced()) {
                        safeSetBlock(level, pos, verticalLogState, centerChunk);
                        
                        // Add spikes to the top of the stump to make it look snapped
                        if (y == stumpHeight - 1 && random.nextFloat() < 0.3f) {
                            if (level.getBlockState(pos.above()).canBeReplaced()) {
                                safeSetBlock(level, pos.above(), verticalLogState, centerChunk);
                            }
                        }
                    }
                }
            }
        }

        BlockState stemState = ModBlocks.ELDER_STEM_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);

        // Generate simple roots around the base to match the real trees
        int min = -1;
        int max = trunkSize;
        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                // Only perimeter
                if (x != min && x != max && z != min && z != max) continue;
                
                // Random chance to have a root extension
                if (random.nextFloat() < 0.6f) {
                    BlockPos rootPos = origin.offset(x, 0, z);
                    if (level.getBlockState(rootPos).canBeReplaced()) {
                        safeSetBlock(level, rootPos, stemState, centerChunk);
                        // Maybe extend one block down to ground
                        if (!level.getBlockState(rootPos.below()).isSolid()) {
                            safeSetBlock(level, rootPos.below(), stemState, centerChunk);
                        }
                        
                        // Maybe extend one block outward
                        if (random.nextFloat() < 0.4f) {
                            int dx = x == min ? -1 : (x == max ? 1 : 0);
                            int dz = z == min ? -1 : (z == max ? 1 : 0);
                            BlockPos outPos = rootPos.offset(dx, 0, dz);
                            if (level.getBlockState(outPos).canBeReplaced()) {
                                safeSetBlock(level, outPos, stemState, centerChunk);
                                if (!level.getBlockState(outPos.below()).isSolid()) {
                                    safeSetBlock(level, outPos.below(), stemState, centerChunk);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Generate fallen trunk extending from stump
        Direction.Axis trunkAxis = random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
        Direction moveDir = trunkAxis == Direction.Axis.X ? (random.nextBoolean() ? Direction.EAST : Direction.WEST) : (random.nextBoolean() ? Direction.SOUTH : Direction.NORTH);
        Direction sideDir = trunkAxis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        
        int trunkLength = 12 + random.nextInt(12); // 12 to 23 blocks long
        BlockState horizontalLogState = config.state.setValue(RotatedPillarBlock.AXIS, trunkAxis);

        // Gap between the stump and the fallen trunk
        int gap = 2 + random.nextInt(3);
        
        // Calculate correct starting position
        int startOffset = (moveDir == Direction.EAST || moveDir == Direction.SOUTH) ? (trunkSize + gap - 1) : -gap;
        BlockPos trunkStart = origin.relative(moveDir, startOffset);
        BlockPos trunkEnd = trunkStart;

        for (int i = 0; i < trunkLength; i++) {
            BlockPos currentAlongAxis = trunkStart.relative(moveDir, i);
            trunkEnd = currentAlongAxis;
            
            // Generate the cross-section
            for (int s = 0; s < trunkSize; s++) {
                for (int y = 0; y < trunkSize; y++) {
                    BlockPos pos = currentAlongAxis.relative(sideDir, s).above(y);
                    
                    // Make the snapped face jagged
                    if (i < 2 && random.nextFloat() < 0.4f) {
                        continue;
                    }

                    // To make it look slightly degraded, random missing blocks on the edges
                    if (i > trunkLength / 2 && (s == 0 || s == trunkSize - 1 || y == trunkSize - 1)) {
                        if (random.nextFloat() < 0.2f) continue;
                    }
                    
                    if (level.getBlockState(pos).canBeReplaced()) {
                        safeSetBlock(level, pos, horizontalLogState, centerChunk);
                    }
                    // Drop down blocks if hanging in the air near the start to connect to ground
                    if (y == 0 && !level.getBlockState(pos.below()).isSolid() && random.nextFloat() < 0.5f) {
                        safeSetBlock(level, pos.below(), horizontalLogState, centerChunk);
                    }
                }
            }
        }

        // 4. Generate the crushed crown at the end of the trunk
        int crownRadius = Math.max(3, (int)(trunkSize * 1.5) + random.nextInt(2));
        int radiusSq = crownRadius * crownRadius;
        BlockPos crownCenter = trunkEnd.above(trunkSize / 2);

        for (int x = -crownRadius; x <= crownRadius; x++) {
            for (int y = -crownRadius; y <= crownRadius; y++) {
                for (int z = -crownRadius; z <= crownRadius; z++) {
                    BlockPos pos = crownCenter.offset(x, y, z);
                    int distSq = x * x + y * y + z * z;
                    
                    // Messy sphere shape
                    if (distSq <= radiusSq && level.getBlockState(pos).canBeReplaced()) {
                        // Less dense further out
                        float fillChance = 1.0f - ((float)distSq / radiusSq) * 0.5f;
                        if (random.nextFloat() < fillChance) {
                            BlockState leafState = random.nextFloat() < 0.15f ? 
                                ModBlocks.ELDER_LEAVES_FLOWERING.get().defaultBlockState() : 
                                ModBlocks.ELDER_LEAVES.get().defaultBlockState();
                            safeSetBlock(level, pos, leafState, centerChunk);
                        }
                    }
                }
            }
        }

        return true;
    }

    private boolean placeFallenPineTree(
            WorldGenLevel level,
            BlockPos origin,
            RandomSource random,
            BlockState logBaseState,
            ChunkPos centerChunk) {
        if (!isValidPineGround(level.getBlockState(origin.below()))) {
            return false;
        }
        BlockState originState = level.getBlockState(origin);
        if ((!originState.isAir() && !originState.canBeReplaced()) || !level.getFluidState(origin).isEmpty()) {
            return false;
        }

        BlockState verticalLog = logBaseState.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState woodState = ModBlocks.ELDER_PINE_WOOD.get().defaultBlockState();
        BlockState pineLeaves = ModBlocks.ELDER_PINE_LEAVES.get().defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, false)
                .setValue(LeavesBlock.DISTANCE, 1);

        // 1. Snapped 1x1 Elder Pine stump (2-3 blocks tall) with all-bark buttress roots
        int stumpHeight = 2 + random.nextInt(2);
        for (int y = 0; y < stumpHeight; y++) {
            BlockPos p = origin.above(y);
            if (level.getBlockState(p).canBeReplaced()) {
                safeSetBlock(level, p, y == 0 ? woodState : verticalLog, centerChunk);
            }
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (random.nextFloat() < 0.70f) {
                BlockPos rootPos = origin.relative(dir);
                if (level.getBlockState(rootPos).canBeReplaced() && level.getFluidState(rootPos).isEmpty()) {
                    safeSetBlock(level, rootPos, woodState, centerChunk);
                    if (!level.getBlockState(rootPos.below()).isSolid() && level.getFluidState(rootPos.below()).isEmpty()) {
                        safeSetBlock(level, rootPos.below(), woodState, centerChunk);
                    }
                }
            }
        }

        // 2. Pick downhill/level horizontal direction so the fallen conifer rests naturally on Highland terrain
        Direction moveDir = pickBestPineFallDirection(level, origin, random);
        Direction.Axis trunkAxis = moveDir.getAxis();
        Direction sideDir = (trunkAxis == Direction.Axis.X) ? Direction.SOUTH : Direction.EAST;
        BlockState horizontalLog = logBaseState.setValue(RotatedPillarBlock.AXIS, trunkAxis);

        int gap = 1 + random.nextInt(2);
        int trunkLength = 9 + random.nextInt(7); // 9 to 15 blocks
        int currentY = origin.getY();
        int crownStartIdx = Math.max(4, trunkLength / 2);

        for (int i = 0; i < trunkLength; i++) {
            BlockPos stepXZ = origin.relative(moveDir, 1 + gap + i);
            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, stepXZ.getX(), stepXZ.getZ());
            // Gently drape the fallen trunk downward along mountain slopes without climbing cliffs
            if (groundY < currentY && currentY - groundY >= 1) {
                currentY--;
            } else if (groundY > currentY && groundY - currentY == 1) {
                currentY++;
            } else if (groundY - currentY > 2) {
                trunkLength = i;
                break;
            }

            BlockPos logPos = new BlockPos(stepXZ.getX(), currentY, stepXZ.getZ());
            if (!level.getFluidState(logPos).isEmpty()) {
                trunkLength = i;
                break;
            }
            if (level.getBlockState(logPos).canBeReplaced()) {
                safeSetBlock(level, logPos, horizontalLog, centerChunk);
            }

            // Weathered bark stubs protruding horizontally or slightly upward
            if (i >= 2 && i < trunkLength - 1 && random.nextFloat() < 0.30f) {
                Direction stubSide = random.nextBoolean() ? sideDir : sideDir.getOpposite();
                BlockPos stubPos = logPos.relative(stubSide);
                if (level.getBlockState(stubPos).canBeReplaced()) {
                    safeSetBlock(level, stubPos, woodState, centerChunk);
                }
            }

            // Conical crushed pine boughs along the upper half toward the tip
            if (i >= crownStartIdx) {
                float crownProgress = (float) (i - crownStartIdx) / Math.max(1, (trunkLength - 1 - crownStartIdx));
                int whorlRadius = crownProgress < 0.45f ? 2 : (crownProgress < 0.85f ? 1 : 0);
                placeFallenPineBoughs(level, logPos, sideDir, whorlRadius, random, pineLeaves, centerChunk);
            }
        }

        return true;
    }

    private Direction pickBestPineFallDirection(WorldGenLevel level, BlockPos origin, RandomSource random) {
        Direction[] dirs = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
        Direction bestDir = dirs[random.nextInt(dirs.length)];
        int bestScore = Integer.MAX_VALUE;
        for (int idx = 0; idx < 4; idx++) {
            Direction d = dirs[(idx + random.nextInt(4)) & 3];
            BlockPos check = origin.relative(d, 7);
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, check.getX(), check.getZ());
            int drop = origin.getY() - h;
            int penalty = (drop < -1) ? 50 + (-drop) * 10 : Math.abs(drop - 1);
            if (penalty < bestScore) {
                bestScore = penalty;
                bestDir = d;
            }
        }
        return bestDir;
    }

    private void placeFallenPineBoughs(
            WorldGenLevel level,
            BlockPos center,
            Direction sideDir,
            int radius,
            RandomSource random,
            BlockState pineLeaves,
            ChunkPos centerChunk) {
        for (int s = -radius; s <= radius; s++) {
            for (int dy = 0; dy <= Math.max(1, radius); dy++) {
                if (s == 0 && dy == 0) continue;
                int dist = Math.abs(s) + dy;
                if (dist > radius + 1) continue;
                if (dist == radius + 1 && random.nextFloat() < 0.45f) continue;

                BlockPos leafPos = center.relative(sideDir, s).above(dy);
                BlockState existing = level.getBlockState(leafPos);
                if ((existing.isAir() || existing.canBeReplaced()) && level.getFluidState(leafPos).isEmpty()) {
                    safeSetBlock(level, leafPos, pineLeaves, centerChunk);
                }
            }
        }
        // Always cap the tip/top ridge with a leaf cluster if space permits
        BlockPos topPos = center.above();
        if (level.getBlockState(topPos).canBeReplaced()) {
            safeSetBlock(level, topPos, pineLeaves, centerChunk);
        }
    }

    private static boolean isValidPineGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.MOSS_BLOCK);
    }

    private void safeSetBlock(WorldGenLevel level, BlockPos pos, BlockState state, ChunkPos centerChunk) {
        if (centerChunk != null) {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            if (Math.abs(centerChunk.x - chunkX) > 0 || Math.abs(centerChunk.z - chunkZ) > 0) {
                return; // Suppress log spam by safely discarding out-of-bounds blocks
            }
        }
        level.setBlock(pos, state, 2);
    }
}
