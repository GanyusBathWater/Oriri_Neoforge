package net.ganyusbathwater.oririmod.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AbyssCrownSaplingBlock extends SaplingBlock {
    protected static final VoxelShape CEILING_SHAPE = Block.box(3.0D, 8.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    private final boolean upgraded;

    public AbyssCrownSaplingBlock(TreeGrower treeGrower, Properties properties, boolean upgraded) {
        super(treeGrower, properties);
        this.upgraded = upgraded;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CEILING_SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos posAbove = pos.above();
        return this.mayPlaceOn(level.getBlockState(posAbove), level, posAbove);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        if (upgraded) {
            return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.MOSS_BLOCK) || state.canOcclude();
        }
        // Normal variant: only dirt blocks and moss
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.MOSS_BLOCK);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        return !state.canSurvive(level, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (upgraded) {
            // Force the tree to grow immediately on the first bonemeal
            UpgradedSaplingBlock.IS_FORCING_GROWTH.set(true);
            try {
                this.treeGrower.growTree(level, level.getChunkSource().getGenerator(), pos, state, random);
            } finally {
                UpgradedSaplingBlock.IS_FORCING_GROWTH.remove();
            }
        } else {
            super.performBonemeal(level, random, pos, state);
        }
    }
}
