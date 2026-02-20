package net.ganyusbathwater.oririmod.block.custom;

import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Scarlet Lily - A water lily variant that can be placed on water or Blood
 * Water.
 * Uses spore blossom-like texture but facing upward, placed on fluid surfaces.
 */
public class ScarletLilyBlock extends WaterlilyBlock {
    protected static final VoxelShape AABB = box(1.0D, 0.0D, 1.0D, 15.0D, 1.5D, 15.0D);

    public ScarletLilyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return AABB;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        FluidState aboveFluidState = level.getFluidState(pos.above());

        // Can be placed on water or Blood Water
        return (fluidState.getType() == Fluids.WATER || isBloodWater(fluidState))
                && aboveFluidState.getType() == Fluids.EMPTY;
    }

    private boolean isBloodWater(FluidState fluidState) {
        // Check if it's Blood Water (will be registered later)
        try {
            return fluidState.getType() == ModFluids.BLOOD_WATER_SOURCE.get()
                    || fluidState.getType() == ModFluids.BLOOD_WATER_FLOWING.get();
        } catch (Exception e) {
            // Fluid not yet registered
            return false;
        }
    }
}
