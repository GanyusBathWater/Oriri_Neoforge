package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.List;

public class EpochTreeFeature extends Feature<EpochTreeFeature.EpochTreeConfig> {

    public EpochTreeFeature(Codec<EpochTreeConfig> codec) {
        super(codec);
    }

    public static class EpochTreeConfig implements net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration {
        public static final Codec<EpochTreeConfig> CODEC = Codec.BOOL.fieldOf("has_fruit").xmap(EpochTreeConfig::new, EpochTreeConfig::hasFruit).codec();
        private final boolean hasFruit;
        public EpochTreeConfig(boolean hasFruit) { this.hasFruit = hasFruit; }
        public boolean hasFruit() { return hasFruit; }
    }

    @Override
    public boolean place(FeaturePlaceContext<EpochTreeConfig> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        boolean generateFruit = context.config().hasFruit();

        // Need sol sand or dirt/grass beneath to grow
        BlockState belowState = level.getBlockState(origin.below());
        if (!belowState.is(ModBlocks.SOL_SAND.get()) && !belowState.is(net.minecraft.world.level.block.Blocks.DIRT) && !belowState.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
            return false;
        }

        BlockState logState = ModBlocks.EPOCH_WOOD_LOG.get().defaultBlockState();
        BlockState vineState = ModBlocks.EPOCH_CACTUS.get().defaultBlockState();

        int height = 10 + (random.nextInt(9) - 4); // 6 to 14
        int rootDepth = 1 + random.nextInt(2); // 1 to 2 blocks down

        // Place roots/lower trunk
        for (int i = 1; i <= rootDepth; i++) {
            BlockPos p = origin.below(i);
            if (level.getBlockState(p).canBeReplaced() || level.getBlockState(p).is(ModBlocks.SOL_SAND.get())) {
                setBlock(level, p, logState);
            }
        }

        // Place main trunk
        for (int i = 0; i < height; i++) {
            BlockPos p = origin.above(i);
            if (level.isEmptyBlock(p) || level.getBlockState(p).canBeReplaced()) {
                setBlock(level, p, logState);
                
                // Add some cluster leaves near the top of the main trunk
                if (i > height - 4) {
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        if (random.nextFloat() < 0.5f) {
                            BlockPos leafPos = p.relative(d);
                            if (level.isEmptyBlock(leafPos) || level.getBlockState(leafPos).canBeReplaced()) {
                                setBlock(level, leafPos, vineState.setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE, 1));
                            }
                        }
                    }
                    if (i == height - 1 && (level.isEmptyBlock(p.above()) || level.getBlockState(p.above()).canBeReplaced())) {
                        setBlock(level, p.above(), vineState.setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE, 1));
                    }
                }
            }
        }

        // Branches
        List<BlockPos> branchTips = new ArrayList<>();
        int numBranches = 10 + random.nextInt(6); // 10 to 15 branches for a much denser canopy
        for (int b = 0; b < numBranches; b++) {
            // Branches start anywhere from middle of the trunk to the very top
            int branchHeight = height / 2 + random.nextInt(height / 2 + 1);
            BlockPos current = origin.above(branchHeight);
            
            Direction dir1 = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            Direction dir2 = random.nextBoolean() ? dir1.getClockWise() : dir1.getCounterClockWise();
            
            int branchLength = 2 + random.nextInt(4); // 2 to 5 blocks long
            for (int i = 0; i < branchLength; i++) {
                // Diagonally and orthogonally outwards
                if (random.nextBoolean()) {
                    current = current.relative(dir1);
                }
                if (random.nextBoolean()) {
                    current = current.relative(dir2);
                }
                // Arch slightly upwards at the start of branches
                if (i < 2 && random.nextBoolean()) {
                    current = current.above();
                }
                
                if (level.isEmptyBlock(current) || level.getBlockState(current).canBeReplaced()) {
                    setBlock(level, current, logState);
                    
                    // Wrap the branch in short leaf clusters to make the canopy look full and lush!
                    for (Direction d : Direction.values()) {
                        if (d != Direction.DOWN && random.nextFloat() < 0.7f) {
                            BlockPos leafPos = current.relative(d);
                            if (level.isEmptyBlock(leafPos) || level.getBlockState(leafPos).canBeReplaced()) {
                                setBlock(level, leafPos, vineState.setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE, 1));
                            }
                        }
                    }
                    
                    // More tips for leaves to hang from
                    if (i == branchLength - 1 || random.nextFloat() < 0.4f) {
                        branchTips.add(current);
                    }
                } else {
                    break;
                }
            }
        }

        // Weeping Vines (Cactus Strips) & Fruit
        for (BlockPos tip : branchTips) {
            int vineStrips = 1 + random.nextInt(2); // 1 to 2 vine strips per tip
            
            for (int v = 0; v < vineStrips; v++) {
                int vineLength = 3 + random.nextInt(6); // 3 to 8 blocks long
                BlockPos vPos = tip;
                BlockPos lastVinePos = null;
                
                // Determine a slight diagonal drift for this specific strip
                Direction driftDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);

                for (int i = 0; i < vineLength; i++) {
                    vPos = vPos.below();
                    // Make the cactus strips occasionally go diagonally!
                    if (i > 0 && random.nextFloat() < 0.35f) {
                        vPos = vPos.relative(driftDir);
                    }

                    if (level.isEmptyBlock(vPos) || level.getBlockState(vPos).canBeReplaced()) {
                        // Set distance from log to prevent decay (maxes at 7 in vanilla)
                        int distance = Math.min(7, i + 1);
                        setBlock(level, vPos, vineState.setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE, distance));
                        lastVinePos = vPos;
                    } else {
                        break;
                    }
                }
                
                // Add Dragonfruit hanging from the very bottom of the cactus strip
                if (generateFruit && lastVinePos != null && random.nextFloat() < 0.85f) { // 85% chance to bear fruit
                    BlockPos fruitPos = lastVinePos.below();
                    // Must not replace water!
                    if ((level.isEmptyBlock(fruitPos) || level.getBlockState(fruitPos).canBeReplaced()) && level.getFluidState(fruitPos).isEmpty()) {
                        // Fruit FACING=DOWN means its attachment face is UP (pointing to the cactus above it)
                        setBlock(level, fruitPos, ModBlocks.DRAGONFRUIT_PLANT.get().defaultBlockState()
                                .setValue(net.ganyusbathwater.oririmod.block.custom.DragonfruitPlantBlock.FACING, Direction.DOWN));
                    }
                }
            }
        }

        return true;
    }
}
