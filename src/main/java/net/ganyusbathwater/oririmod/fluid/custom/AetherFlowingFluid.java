package net.ganyusbathwater.oririmod.fluid.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public abstract class AetherFlowingFluid extends BaseFlowingFluid {
    protected AetherFlowingFluid(Properties properties) {
        super(properties);
    }

    @Override
    public void tick(Level level, BlockPos pos, FluidState state) {
        super.tick(level, pos, state);

        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        // Fallback: falls bereits Kontakt besteht (z.B. direkt platziert ohne sofortigen Spread),
        // erzwinge Konvertierung im Fluid-Tick.
        tryAetherMixing(serverLevel, pos);
    }

    @Override
    protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState blockState, Direction direction, FluidState fluidState) {
        // `pos` ist die Zielzelle, in die Aether jetzt reinfließen/platziert werden soll.
        // Hier passiert bei Vanilla das "sofortige" Mixing (siehe LavaFluid#spreadTo).
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            if (tryAetherMixAtTarget(serverLevel, pos, blockState)) {
                return; // NICHT weiter spreaden, weil wir die Zielzelle bereits ersetzt haben
            }
        }

        super.spreadTo(level, pos, blockState, direction, fluidState);
    }

    private static boolean isWater(Fluid f) {
        return f == Fluids.WATER || f == Fluids.FLOWING_WATER;
    }

    private static boolean isLava(Fluid f) {
        return f == Fluids.LAVA || f == Fluids.FLOWING_LAVA;
    }

    private static boolean tryAetherMixAtTarget(ServerLevel level, BlockPos targetPos, BlockState targetState) {
        // Wichtig: über FluidState gehen, damit Flowing/Source erkannt wird.
        FluidState fs = targetState.getFluidState();
        Fluid f = fs.getType();

        if (fs.isEmpty()) return false;

        if (isWater(f)) {
            // Aether + Wasser => Blackstone
            if (targetState.getBlock() instanceof LiquidBlock) {
                level.setBlock(
                        targetPos,
                        EventHooks.fireFluidPlaceBlockEvent(level, targetPos, targetPos, Blocks.BLACKSTONE.defaultBlockState()),
                        3
                );
                fizz(level, targetPos);
                return true;
            }
        }

        if (isLava(f)) {
            // Aether + Lava => Cobblestone
            if (targetState.getBlock() instanceof LiquidBlock) {
                level.setBlock(
                        targetPos,
                        EventHooks.fireFluidPlaceBlockEvent(level, targetPos, targetPos, Blocks.COBBLESTONE.defaultBlockState()),
                        3
                );
                fizz(level, targetPos);
                return true;
            }
        }

        return false;
    }

    private static boolean tryAetherMixing(ServerLevel level, BlockPos aetherPos) {
        // Prüfe Nachbarn um die aktuelle Aether-Zelle herum; wenn Kontakt, ersetze die Aether-Zelle sofort.
        for (Direction dir : Direction.values()) {
            BlockPos otherPos = aetherPos.relative(dir);
            BlockState otherState = level.getBlockState(otherPos);
            FluidState otherFs = otherState.getFluidState();
            Fluid other = otherFs.getType();

            if (otherFs.isEmpty()) continue;

            if (isWater(other)) {
                level.setBlock(
                        aetherPos,
                        EventHooks.fireFluidPlaceBlockEvent(level, aetherPos, otherPos, Blocks.BLACKSTONE.defaultBlockState()),
                        3
                );
                fizz(level, aetherPos);
                return true;
            }

            if (isLava(other)) {
                level.setBlock(
                        aetherPos,
                        EventHooks.fireFluidPlaceBlockEvent(level, aetherPos, otherPos, Blocks.COBBLESTONE.defaultBlockState()),
                        3
                );
                fizz(level, aetherPos);
                return true;
            }
        }
        return false;
    }

    private static void fizz(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);
        level.levelEvent(1501, pos, 0);
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