package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {

    @Inject(method = "onPlace", at = @At("TAIL"))
    private void oririmod$onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        oririmod$tryAetherMixingConvertTarget(serverLevel, pos);
    }

    @Inject(method = "neighborChanged", at = @At("TAIL"))
    private void oririmod$neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighborBlock, BlockPos neighborPos, boolean movedByPiston, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        oririmod$tryAetherMixingConvertTarget(serverLevel, pos);
        oririmod$tryAetherMixingConvertTarget(serverLevel, neighborPos);
    }

    private static void oririmod$tryAetherMixingConvertTarget(ServerLevel level, BlockPos pos) {
        FluidState fsHere = level.getFluidState(pos);
        Fluid here = fsHere.getType();

        boolean hereAether = (here == ModFluids.AETHER_SOURCE.get() || here == ModFluids.AETHER_FLOWING.get());
        boolean hereWater = (here == Fluids.WATER || here == Fluids.FLOWING_WATER);
        boolean hereLava = (here == Fluids.LAVA || here == Fluids.FLOWING_LAVA);

        for (Direction dir : Direction.values()) {
            BlockPos otherPos = pos.relative(dir);

            FluidState fsOther = level.getFluidState(otherPos);
            Fluid other = fsOther.getType();

            boolean otherAether = (other == ModFluids.AETHER_SOURCE.get() || other == ModFluids.AETHER_FLOWING.get());
            boolean otherWater = (other == Fluids.WATER || other == Fluids.FLOWING_WATER);
            boolean otherLava = (other == Fluids.LAVA || other == Fluids.FLOWING_LAVA);

            // Wenn Aether anliegt, konvertiere das Gegenüber (Zielblock), nicht den Aether\-Block.
            if (hereAether && otherWater) {
                level.setBlock(otherPos, Blocks.BLACKSTONE.defaultBlockState(), 3);
                return;
            }
            if (hereAether && otherLava) {
                level.setBlock(otherPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                return;
            }

            // Optional symmetrisch: falls Wasser/Lava platziert wird und an Aether grenzt,
            // dann auch hier das Wasser/Lava (also "pos") konvertieren.
            if (otherAether && hereWater) {
                level.setBlock(pos, Blocks.BLACKSTONE.defaultBlockState(), 3);
                return;
            }
            if (otherAether && hereLava) {
                level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                return;
            }
        }
    }
}