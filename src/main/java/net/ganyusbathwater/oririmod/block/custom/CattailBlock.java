package net.ganyusbathwater.oririmod.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class CattailBlock extends Block implements SimpleWaterloggedBlock, BonemealableBlock {
    public enum CattailPart implements StringRepresentable {
        BOTTOM("bottom"),
        TOP("top");

        private final String name;

        CattailPart(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static final EnumProperty<CattailPart> PART = EnumProperty.create("part", CattailPart.class);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public CattailBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(PART, CattailPart.TOP)
                .setValue(AGE, 0)
                .setValue(WATERLOGGED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, AGE, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    private boolean isSoilValid(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SUSPICIOUS_GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD)
                || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        int countBelow = 0;
        BlockPos current = pos.below();
        while (countBelow < 8 && level.getBlockState(current).is(this)) {
            countBelow++;
            current = current.below();
        }

        // Maximum height is 7 blocks (so countBelow cannot be 7 or more)
        if (countBelow >= 7) {
            return false;
        }

        BlockState groundState = level.getBlockState(current);
        return isSoilValid(groundState);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluid = level.getFluidState(pos);
        boolean waterlogged = fluid.getType() == Fluids.WATER;

        boolean hasCattailAbove = level.getBlockState(pos.above()).is(this);
        CattailPart part = hasCattailAbove ? CattailPart.BOTTOM : CattailPart.TOP;

        BlockState state = this.defaultBlockState()
                .setValue(PART, part)
                .setValue(AGE, 0)
                .setValue(WATERLOGGED, waterlogged);

        if (state.canSurvive(level, pos)) {
            return state;
        }
        return null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (facing == Direction.DOWN && !state.canSurvive(level, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        }

        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        if (facing == Direction.UP) {
            boolean hasCattailAbove = facingState.is(this);
            return state.setValue(PART, hasCattailAbove ? CattailPart.BOTTOM : CattailPart.TOP);
        }

        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos abovePos = pos.above();
        FluidState aboveFluid = level.getFluidState(abovePos);
        BlockState aboveState = level.getBlockState(abovePos);

        boolean canGrowAbove = aboveState.isAir() || (aboveFluid.getType() == Fluids.WATER && !aboveState.isSolid());
        if (canGrowAbove) {
            int height = 1;
            while (level.getBlockState(pos.below(height)).is(this)) {
                height++;
            }

            // Naturally grows up to 2 blocks
            if (height < 2) {
                int age = state.getValue(AGE);
                if (age == 15) {
                    boolean aboveWaterlogged = aboveFluid.getType() == Fluids.WATER;
                    level.setBlockAndUpdate(abovePos, this.defaultBlockState()
                            .setValue(PART, CattailPart.TOP)
                            .setValue(WATERLOGGED, aboveWaterlogged)
                            .setValue(AGE, 0));
                    level.setBlock(pos, state.setValue(PART, CattailPart.BOTTOM).setValue(AGE, 0), 3);
                } else {
                    level.setBlock(pos, state.setValue(AGE, age + 1), 4);
                }
            }
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        int height = 1;
        while (level.getBlockState(pos.below(height)).is(this)) {
            height++;
        }
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        FluidState aboveFluid = level.getFluidState(abovePos);
        return height < 2 && (aboveState.isAir() || (aboveFluid.getType() == Fluids.WATER && !aboveState.isSolid()));
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos abovePos = pos.above();
        FluidState aboveFluid = level.getFluidState(abovePos);
        boolean aboveWaterlogged = aboveFluid.getType() == Fluids.WATER;

        level.setBlockAndUpdate(abovePos, this.defaultBlockState()
                .setValue(PART, CattailPart.TOP)
                .setValue(WATERLOGGED, aboveWaterlogged)
                .setValue(AGE, 0));
        level.setBlock(pos, state.setValue(PART, CattailPart.BOTTOM).setValue(AGE, 0), 3);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
