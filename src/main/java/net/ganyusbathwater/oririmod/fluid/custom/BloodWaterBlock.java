package net.ganyusbathwater.oririmod.fluid.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

/**
 * Blood Water Block - The block form of Blood Water fluid.
 */
public class BloodWaterBlock extends LiquidBlock {
    public BloodWaterBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        
        if (entity instanceof LivingEntity living) {
            Vec3 movement = living.getDeltaMovement();
            // When 1-block deep in stationary custom fluids, the vanilla jumpInLiquid velocity (~0.04)
            // is not enough to break the surface tension, trapping the player in a bobbing state.
            // If the entity has positive Y velocity (trying to swim up), we provide a small boost 
            // so they can actually hop out of the fluid!
            if (movement.y > 0.0 && movement.y < 0.1) {
                living.setDeltaMovement(movement.add(0.0D, 0.06D, 0.0D));
            }
        }
    }
}
