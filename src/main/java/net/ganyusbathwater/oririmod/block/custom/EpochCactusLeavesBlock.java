package net.ganyusbathwater.oririmod.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class EpochCactusLeavesBlock extends LeavesBlock {
    public static final IntegerProperty EXTENDED_DISTANCE = IntegerProperty.create("extended_distance", 1, 15);
    public static final int MAX_DISTANCE = 15;

    public EpochCactusLeavesBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(DISTANCE, 7)
                .setValue(PERSISTENT, false)
                .setValue(WATERLOGGED, false)
                .setValue(EXTENDED_DISTANCE, MAX_DISTANCE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EXTENDED_DISTANCE);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(EXTENDED_DISTANCE) == MAX_DISTANCE && !state.getValue(PERSISTENT);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PERSISTENT) && state.getValue(EXTENDED_DISTANCE) == MAX_DISTANCE) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState newState = updateExtendedDistance(state, level, pos);
        
        // Only update if the distance actually changed
        if (state.getValue(EXTENDED_DISTANCE) != newState.getValue(EXTENDED_DISTANCE)) {
            level.setBlock(pos, newState, 3);
            
            // Manually propagate block updates to the remaining 20 diagonal neighbors.
            // Vanilla setBlock() only handles the 6 orthogonal faces automatically.
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        // Skip the center block and the 6 orthogonal faces
                        if (Math.abs(x) + Math.abs(y) + Math.abs(z) <= 1) continue;
                        
                        mutable.setWithOffset(pos, x, y, z);
                        BlockState neighbor = level.getBlockState(mutable);
                        
                        // If it's a cactus leaf, schedule a tick so it can recalculate its distance
                        if (neighbor.getBlock() instanceof EpochCactusLeavesBlock) {
                            level.scheduleTick(mutable.immutable(), neighbor.getBlock(), 1);
                        }
                    }
                }
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, net.minecraft.world.level.material.Fluids.WATER, net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));
        }

        // Must add 1 to match vanilla logic, preventing infinite tick loops
        int i = getDistanceAt(facingState) + 1;
        if (i != 1 || state.getValue(EXTENDED_DISTANCE) != i) {
            level.scheduleTick(currentPos, this, 1);
        }

        return state;
    }

    private BlockState updateExtendedDistance(BlockState state, LevelAccessor level, BlockPos pos) {
        int i = MAX_DISTANCE;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        // Check ALL 26 surrounding blocks (including diagonals) for connections
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    
                    mutable.setWithOffset(pos, x, y, z);
                    i = Math.min(i, getDistanceAt(level.getBlockState(mutable)) + 1);
                    if (i == 1) {
                        break;
                    }
                }
                if (i == 1) break;
            }
            if (i == 1) break;
        }

        return state.setValue(EXTENDED_DISTANCE, i);
    }

    private int getDistanceAt(BlockState neighborState) {
        if (neighborState.is(BlockTags.LOGS)) {
            return 0;
        } else if (neighborState.getBlock() instanceof EpochCactusLeavesBlock) {
            return neighborState.getValue(EXTENDED_DISTANCE);
        } else {
            return MAX_DISTANCE;
        }
    }
}
