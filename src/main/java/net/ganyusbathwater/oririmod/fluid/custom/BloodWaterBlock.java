package net.ganyusbathwater.oririmod.fluid.custom;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Blood Water Block - The block form of Blood Water fluid.
 */
public class BloodWaterBlock extends LiquidBlock {
    public BloodWaterBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }
}
