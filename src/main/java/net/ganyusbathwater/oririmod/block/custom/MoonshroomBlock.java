package net.ganyusbathwater.oririmod.block.custom;

import com.mojang.serialization.MapCodec;
import net.ganyusbathwater.oririmod.block.entity.MoonshroomBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class MoonshroomBlock extends Block implements EntityBlock {
    public static final MapCodec<MoonshroomBlock> CODEC = simpleCodec(MoonshroomBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Voxel shapes for different facings
    protected static final VoxelShape NORTH_SHAPE = Block.box(4.0D, 4.0D, 8.0D, 12.0D, 12.0D, 16.0D);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(4.0D, 4.0D, 0.0D, 12.0D, 12.0D, 8.0D);
    protected static final VoxelShape WEST_SHAPE = Block.box(8.0D, 4.0D, 4.0D, 16.0D, 12.0D, 12.0D);
    protected static final VoxelShape EAST_SHAPE = Block.box(0.0D, 4.0D, 4.0D, 8.0D, 12.0D, 12.0D);

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public MoonshroomBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        BlockPos attachedPos = pos.relative(direction.getOpposite());
        BlockState attachedState = level.getBlockState(attachedPos);
        return attachedState.isFaceSturdy(level, attachedPos, direction);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (facing.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction[] directions = context.getNearestLookingDirections();
        for (Direction direction : directions) {
            if (direction.getAxis().isHorizontal()) {
                Direction opposite = direction.getOpposite();
                BlockState state = this.defaultBlockState().setValue(FACING, opposite);
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MoonshroomBlockEntity(pos, state);
    }
}
