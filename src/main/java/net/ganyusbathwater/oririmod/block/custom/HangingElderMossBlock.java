package net.ganyusbathwater.oririmod.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ganyusbathwater.oririmod.block.ModBlocks;

public class HangingElderMossBlock extends GrowingPlantHeadBlock {
    public static final MapCodec<HangingElderMossBlock> CODEC = simpleCodec(HangingElderMossBlock::new);
    protected static final VoxelShape SHAPE = Block.box(4.0D, 9.0D, 4.0D, 12.0D, 16.0D, 12.0D);

    @Override
    public MapCodec<? extends GrowingPlantHeadBlock> codec() {
        return CODEC;
    }

    public HangingElderMossBlock(Properties properties) {
        super(properties, Direction.DOWN, SHAPE, false, 0.1D);
    }

    @Override
    protected int getBlocksToGrowWhenBonemealed(RandomSource random) {
        return 0; // Prevent bonemeal growth
    }

    @Override
    protected boolean canGrowInto(BlockState state) {
        return false; // Prevent natural growth
    }

    @Override
    protected Block getBodyBlock() {
        return ModBlocks.HANGING_ELDER_MOSS_PLANT.get();
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return false; // Prevent natural ticking growth
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Do nothing
    }
}
