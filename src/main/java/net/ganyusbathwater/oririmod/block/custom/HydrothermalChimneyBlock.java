package net.ganyusbathwater.oririmod.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class HydrothermalChimneyBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<DripstoneThickness> THICKNESS = BlockStateProperties.DRIPSTONE_THICKNESS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape TIP_SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 11.0D, 11.0D);
    private static final VoxelShape FRUSTUM_SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);
    private static final VoxelShape MIDDLE_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    private static final VoxelShape BASE_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public HydrothermalChimneyBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(THICKNESS, DripstoneThickness.TIP).setValue(WATERLOGGED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        DripstoneThickness thickness = state.getValue(THICKNESS);
        return switch (thickness) {
            case TIP -> TIP_SHAPE;
            case FRUSTUM -> FRUSTUM_SHAPE;
            case MIDDLE -> MIDDLE_SHAPE;
            default -> BASE_SHAPE;
        };
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return belowState.is(this) || belowState.isFaceSturdy(level, below, Direction.UP);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluid = level.getFluidState(pos);
        DripstoneThickness thickness = calculateThickness(level, pos);
        if (this.canSurvive(this.defaultBlockState(), level, pos)) {
            return this.defaultBlockState().setValue(THICKNESS, thickness).setValue(WATERLOGGED, fluid.is(FluidTags.WATER) && fluid.getAmount() == 8);
        }
        return null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        if (!state.canSurvive(level, currentPos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }

        if (facing == Direction.UP || facing == Direction.DOWN) {
            DripstoneThickness thickness = calculateThickness(level, currentPos);
            return state.setValue(THICKNESS, thickness);
        }

        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    private static DripstoneThickness calculateThickness(LevelReader level, BlockPos pos) {
        boolean hasAbove = level.getBlockState(pos.above()).getBlock() instanceof HydrothermalChimneyBlock;
        boolean hasBelow = level.getBlockState(pos.below()).getBlock() instanceof HydrothermalChimneyBlock;

        if (!hasAbove && !hasBelow) {
            return DripstoneThickness.TIP;
        } else if (!hasAbove) {
            return DripstoneThickness.TIP;
        } else if (!hasBelow) {
            return DripstoneThickness.BASE;
        } else {
            boolean hasAboveAbove = level.getBlockState(pos.above(2)).getBlock() instanceof HydrothermalChimneyBlock;
            if (!hasAboveAbove) {
                return DripstoneThickness.FRUSTUM;
            } else {
                return DripstoneThickness.MIDDLE;
            }
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (state.getValue(THICKNESS) == DripstoneThickness.TIP && !entity.fireImmune()) {
            entity.hurt(level.damageSources().hotFloor(), 2.0F);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(THICKNESS) == DripstoneThickness.TIP) {
            double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D;
            double y = pos.getY() + 0.7D;
            double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D;

            if (state.getValue(WATERLOGGED)) {
                level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 0.0D, 0.1D, 0.0D);
            } else {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0.0D, 0.07D, 0.0D);
            }
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
