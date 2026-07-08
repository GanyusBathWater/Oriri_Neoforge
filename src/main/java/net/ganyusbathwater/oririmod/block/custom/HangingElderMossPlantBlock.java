package net.ganyusbathwater.oririmod.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ganyusbathwater.oririmod.block.ModBlocks;

public class HangingElderMossPlantBlock extends GrowingPlantBodyBlock {
    public static final MapCodec<HangingElderMossPlantBlock> CODEC = simpleCodec(HangingElderMossPlantBlock::new);
    public static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);

    @Override
    public MapCodec<? extends GrowingPlantBodyBlock> codec() {
        return CODEC;
    }

    public HangingElderMossPlantBlock(Properties properties) {
        super(properties, Direction.DOWN, SHAPE, false);
    }

    @Override
    protected GrowingPlantHeadBlock getHeadBlock() {
        return (GrowingPlantHeadBlock) ModBlocks.HANGING_ELDER_MOSS.get();
    }
}
