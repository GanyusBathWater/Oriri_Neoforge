package net.ganyusbathwater.oririmod.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public class MyriadCactusBlock extends DirectionalBlock {
    public static final MapCodec<MyriadCactusBlock> CODEC = simpleCodec(MyriadCactusBlock::new);

    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> BRANCH_SHAPES = new EnumMap<>(Direction.class);

    static {
        // Main pillars spanning the full block length on their axis (0 to 16)
        SHAPES.put(Direction.UP, Block.box(1, 0, 1, 15, 16, 15));
        SHAPES.put(Direction.DOWN, Block.box(1, 0, 1, 15, 16, 15));
        SHAPES.put(Direction.NORTH, Block.box(1, 1, 0, 15, 15, 16));
        SHAPES.put(Direction.SOUTH, Block.box(1, 1, 0, 15, 15, 16));
        SHAPES.put(Direction.WEST, Block.box(0, 1, 1, 16, 15, 15));
        SHAPES.put(Direction.EAST, Block.box(0, 1, 1, 16, 15, 15));

        // 1-pixel branches extending outwards from the 14x14 core
        BRANCH_SHAPES.put(Direction.UP, Block.box(1, 15, 1, 15, 16, 15));
        BRANCH_SHAPES.put(Direction.DOWN, Block.box(1, 0, 1, 15, 1, 15));
        BRANCH_SHAPES.put(Direction.NORTH, Block.box(1, 1, 0, 15, 15, 1));
        BRANCH_SHAPES.put(Direction.SOUTH, Block.box(1, 1, 15, 15, 15, 16));
        BRANCH_SHAPES.put(Direction.WEST, Block.box(0, 1, 1, 1, 15, 15));
        BRANCH_SHAPES.put(Direction.EAST, Block.box(15, 1, 1, 16, 15, 15));
    }

    @Override
    public @NotNull MapCodec<MyriadCactusBlock> codec() {
        return CODEC;
    }

    public MyriadCactusBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(UP, false).setValue(DOWN, false)
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return this.defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(UP, level.getBlockState(pos.above()).is(this))
                .setValue(DOWN, level.getBlockState(pos.below()).is(this))
                .setValue(NORTH, level.getBlockState(pos.north()).is(this))
                .setValue(SOUTH, level.getBlockState(pos.south()).is(this))
                .setValue(EAST, level.getBlockState(pos.east()).is(this))
                .setValue(WEST, level.getBlockState(pos.west()).is(this));
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        return state.setValue(getDirectionProperty(direction), neighborState.is(this));
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        Direction parentDirection = state.getValue(FACING).getOpposite();
        BlockPos parentPos = pos.relative(parentDirection);
        BlockState parentState = level.getBlockState(parentPos);
        
        if (parentState.is(this)) {
            return true;
        }
        
        return parentState.isFaceSturdy(level, parentPos, state.getValue(FACING)) || parentState.is(net.minecraft.tags.BlockTags.SAND);
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    public static BooleanProperty getDirectionProperty(Direction direction) {
        return switch (direction) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
        };
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = SHAPES.get(state.getValue(FACING));
        
        // Add branch shapes if they are connected
        if (state.getValue(UP)) shape = Shapes.or(shape, BRANCH_SHAPES.get(Direction.UP));
        if (state.getValue(DOWN)) shape = Shapes.or(shape, BRANCH_SHAPES.get(Direction.DOWN));
        if (state.getValue(NORTH)) shape = Shapes.or(shape, BRANCH_SHAPES.get(Direction.NORTH));
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, BRANCH_SHAPES.get(Direction.SOUTH));
        if (state.getValue(EAST)) shape = Shapes.or(shape, BRANCH_SHAPES.get(Direction.EAST));
        if (state.getValue(WEST)) shape = Shapes.or(shape, BRANCH_SHAPES.get(Direction.WEST));

        return shape;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, UP, DOWN, NORTH, SOUTH, EAST, WEST);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
