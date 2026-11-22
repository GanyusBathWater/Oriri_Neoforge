package net.ganyusbathwater.oririmod.fluid.custom;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Blood Water - A fluid with lava-like viscosity and sounds but water-like
 * properties.
 * Used for ponds in Scarlet biomes and aquifers in Scarlet Caves.
 */
public abstract class BloodWaterFlowingFluid extends BaseFlowingFluid {
    protected BloodWaterFlowingFluid(Properties properties) {
        super(properties);
    }

    @Override
    public void tick(Level level, BlockPos pos, FluidState state) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            if (tryBloodWaterLavaMix(serverLevel, pos)) {
                return;
            }
        }

        super.tick(level, pos, state);

        // Play lava ambient sound occasionally
        if (!level.isClientSide && level.random.nextInt(200) == 0) {
            level.playSound(null, pos, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.2F, 1.0F);
        }
    }

    @Override
    protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState blockState, Direction direction,
            FluidState fluidState) {
        // Intercept Blood Water flowing into Lava (including downwards) so it solidifies Lava
        // instead of overwriting the Lava block with falling Blood Water.
        FluidState targetFs = blockState.getFluidState();
        if (targetFs.is(FluidTags.LAVA)) {
            if (!level.isClientSide()) {
                BlockState result = targetFs.isSource()
                        ? Blocks.OBSIDIAN.defaultBlockState()
                        : ModBlocks.COBBLED_SCARLET_DEEPSLATE.get().defaultBlockState();
                level.setBlock(pos, EventHooks.fireFluidPlaceBlockEvent(level, pos, pos, result), 3);
                level.levelEvent(1501, pos, 0);
            }
            return;
        }

        super.spreadTo(level, pos, blockState, direction, fluidState);
    }

    private static boolean tryBloodWaterLavaMix(ServerLevel level, BlockPos bloodPos) {
        for (Direction dir : Direction.values()) {
            BlockPos otherPos = bloodPos.relative(dir);
            FluidState otherFs = level.getFluidState(otherPos);
            if (!otherFs.is(FluidTags.LAVA)) {
                continue;
            }
            if (dir == Direction.UP) {
                // Lava above Blood Water -> Blood Water turns into Scarlet Stone
                BlockState result = ModBlocks.SCARLET_STONE.get().defaultBlockState();
                level.setBlock(bloodPos, EventHooks.fireFluidPlaceBlockEvent(level, bloodPos, otherPos, result), 3);
                level.levelEvent(1501, bloodPos, 0);
                return true;
            } else {
                // Blood Water above or beside Lava -> Lava source turns to Obsidian, flowing Lava to Cobbled Scarlet Deepslate
                BlockState result = otherFs.isSource()
                        ? Blocks.OBSIDIAN.defaultBlockState()
                        : ModBlocks.COBBLED_SCARLET_DEEPSLATE.get().defaultBlockState();
                level.setBlock(otherPos, EventHooks.fireFluidPlaceBlockEvent(level, otherPos, bloodPos, result), 3);
                level.levelEvent(1501, otherPos, 0);
                return false;
            }
        }
        return false;
    }

    public static final class Source extends BloodWaterFlowingFluid {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static final class Flowing extends BloodWaterFlowingFluid {
        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
