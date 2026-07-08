package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.ganyusbathwater.oririmod.block.ModBlocks;

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
                        level.setBlock(pos, verticalLogState, 2);
                        
                        // Add spikes to the top of the stump to make it look snapped
                        if (y == stumpHeight - 1 && random.nextFloat() < 0.3f) {
                            if (level.getBlockState(pos.above()).canBeReplaced()) {
                                level.setBlock(pos.above(), verticalLogState, 2);
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
                        level.setBlock(rootPos, stemState, 2);
                        // Maybe extend one block down to ground
                        if (!level.getBlockState(rootPos.below()).isSolid()) {
                            level.setBlock(rootPos.below(), stemState, 2);
                        }
                        
                        // Maybe extend one block outward
                        if (random.nextFloat() < 0.4f) {
                            int dx = x == min ? -1 : (x == max ? 1 : 0);
                            int dz = z == min ? -1 : (z == max ? 1 : 0);
                            BlockPos outPos = rootPos.offset(dx, 0, dz);
                            if (level.getBlockState(outPos).canBeReplaced()) {
                                level.setBlock(outPos, stemState, 2);
                                if (!level.getBlockState(outPos.below()).isSolid()) {
                                    level.setBlock(outPos.below(), stemState, 2);
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
                        level.setBlock(pos, horizontalLogState, 2);
                    }
                    // Drop down blocks if hanging in the air near the start to connect to ground
                    if (y == 0 && !level.getBlockState(pos.below()).isSolid() && random.nextFloat() < 0.5f) {
                        level.setBlock(pos.below(), horizontalLogState, 2);
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
                            level.setBlock(pos, leafState, 2);
                        }
                    }
                }
            }
        }

        return true;
    }
}
