package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

public class MyriadCactusFeature extends Feature<NoneFeatureConfiguration> {

    public MyriadCactusFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // Must spawn on Sol Sand
        if (!level.getBlockState(origin.below()).is(ModBlocks.SOL_SAND.get())) {
            return false;
        }

        int mainHeight = 3 + random.nextInt(5); // 3 to 7 blocks tall
        
        // Track blocks to place: pos -> state
        java.util.Map<BlockPos, BlockState> blocksToPlace = new java.util.HashMap<>();
        List<BlockPos> trunkPositions = new ArrayList<>();

        // Generate main trunk
        for (int i = 0; i < mainHeight; i++) {
            BlockPos current = origin.above(i);
            if (level.isEmptyBlock(current) || level.getBlockState(current).canBeReplaced()) {
                blocksToPlace.put(current, ModBlocks.MYRIAD_CACTUS.get().defaultBlockState()
                        .setValue(net.ganyusbathwater.oririmod.block.custom.MyriadCactusBlock.FACING, Direction.UP));
                if (i >= 2 && i < mainHeight - 1) {
                    trunkPositions.add(current); // Potential branch points
                }
            } else {
                break;
            }
        }

        // Generate branches (Strictly orthogonal: out, then up)
        int numBranches = 1 + random.nextInt(3); // 1 to 3 branches
        if (!trunkPositions.isEmpty()) {
            List<Direction> availableDirs = new ArrayList<>(List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST));
            java.util.Collections.shuffle(availableDirs, new java.util.Random(random.nextLong()));

            for (int i = 0; i < numBranches && !trunkPositions.isEmpty() && i < availableDirs.size(); i++) {
                // Ensure unique Y-levels for branches by removing the chosen origin
                BlockPos branchOrigin = trunkPositions.remove(random.nextInt(trunkPositions.size()));
                Direction dir = availableDirs.get(i); // Unique direction
                
                // Strictly 2 blocks out to enforce exactly a 1-block gap between the trunk and the vertical branch
                int horizontalLength = 2; 
                BlockPos current = branchOrigin;
                boolean blocked = false;
                
                // Horizontal part
                for (int h = 0; h < horizontalLength; h++) {
                    current = current.relative(dir);
                    if (level.isEmptyBlock(current) || level.getBlockState(current).canBeReplaced()) {
                        blocksToPlace.put(current, ModBlocks.MYRIAD_CACTUS.get().defaultBlockState()
                                .setValue(net.ganyusbathwater.oririmod.block.custom.MyriadCactusBlock.FACING, dir));
                    } else {
                        blocked = true;
                        break;
                    }
                }
                
                // Vertical part of the branch
                if (!blocked) {
                    int verticalLength = 1 + random.nextInt(3); // 1 to 3 blocks up
                    for (int v = 0; v < verticalLength; v++) {
                        current = current.above();
                        if (level.isEmptyBlock(current) || level.getBlockState(current).canBeReplaced()) {
                            blocksToPlace.put(current, ModBlocks.MYRIAD_CACTUS.get().defaultBlockState()
                                    .setValue(net.ganyusbathwater.oririmod.block.custom.MyriadCactusBlock.FACING, Direction.UP));
                        } else {
                            break;
                        }
                    }
                }
            }
        }

        // Second pass: Calculate connections (UP, DOWN, NORTH, SOUTH, EAST, WEST)
        for (java.util.Map.Entry<BlockPos, BlockState> entry : blocksToPlace.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            
            boolean up = blocksToPlace.containsKey(pos.above());
            boolean down = blocksToPlace.containsKey(pos.below()) || pos.equals(origin); // Origin connects to ground
            boolean north = blocksToPlace.containsKey(pos.north());
            boolean south = blocksToPlace.containsKey(pos.south());
            boolean east = blocksToPlace.containsKey(pos.east());
            boolean west = blocksToPlace.containsKey(pos.west());
            
            BlockState connectedState = state
                    .setValue(net.ganyusbathwater.oririmod.block.custom.MyriadCactusBlock.UP, up)
                    .setValue(net.ganyusbathwater.oririmod.block.custom.MyriadCactusBlock.DOWN, down)
                    .setValue(net.ganyusbathwater.oririmod.block.custom.MyriadCactusBlock.NORTH, north)
                    .setValue(net.ganyusbathwater.oririmod.block.custom.MyriadCactusBlock.SOUTH, south)
                    .setValue(net.ganyusbathwater.oririmod.block.custom.MyriadCactusBlock.EAST, east)
                    .setValue(net.ganyusbathwater.oririmod.block.custom.MyriadCactusBlock.WEST, west);
                    
            setBlock(level, pos, connectedState);
        }

        // Scatter dead bushes around the base
        int numBushes = random.nextInt(4);
        BlockState deadBush = Blocks.DEAD_BUSH.defaultBlockState();
        for (int i = 0; i < numBushes; i++) {
            BlockPos bushPos = origin.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            for (int y = -1; y <= 1; y++) {
                BlockPos checkPos = bushPos.above(y);
                if ((level.isEmptyBlock(checkPos) || level.getBlockState(checkPos).canBeReplaced()) && 
                    level.getBlockState(checkPos.below()).is(ModBlocks.SOL_SAND.get())) {
                    setBlock(level, checkPos, deadBush);
                    break;
                }
            }
        }

        return true;
    }
}
