package net.ganyusbathwater.oririmod.fluid.custom;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Blood Water - A fluid with lava-like viscosity and sounds but water-like
 * properties.
 * Used for ponds in Scarlet biomes.
 */
public abstract class BloodWaterFlowingFluid extends BaseFlowingFluid {
    protected BloodWaterFlowingFluid(Properties properties) {
        super(properties);
    }

    @Override
    public void tick(Level level, BlockPos pos, FluidState state) {
        super.tick(level, pos, state);

        // Play lava ambient sound occasionally
        if (!level.isClientSide && level.random.nextInt(200) == 0) {
            level.playSound(null, pos, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.2F, 1.0F);
        }
    }

    public static final class Source extends BaseFlowingFluid.Source {
        public Source(Properties properties) {
            super(properties);
        }
    }

    public static final class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing(Properties properties) {
            super(properties);
        }
    }
}
