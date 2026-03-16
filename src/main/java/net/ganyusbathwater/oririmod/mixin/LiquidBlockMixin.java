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
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {

    @Inject(method = "onPlace", at = @At("TAIL"))
    private void oririmod$onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
            boolean movedByPiston, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel))
            return;
        oririmod$tryFluidMixingConvertTarget(serverLevel, pos);
    }

    @Inject(method = "neighborChanged", at = @At("TAIL"))
    private void oririmod$neighborChanged(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.level.block.Block neighborBlock, BlockPos neighborPos, boolean movedByPiston,
            CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel))
            return;
        oririmod$tryFluidMixingConvertTarget(serverLevel, pos);
        oririmod$tryFluidMixingConvertTarget(serverLevel, neighborPos);
    }

    @Unique
    private static void oririmod$tryFluidMixingConvertTarget(ServerLevel level, BlockPos pos) {
        FluidState fsHere = level.getFluidState(pos);
        Fluid here = fsHere.getType();
        if (here == Fluids.EMPTY)
            return;

        boolean hereAether = (here == ModFluids.AETHER_SOURCE.get() || here == ModFluids.AETHER_FLOWING.get());
        boolean hereBloodWater = (here == ModFluids.BLOOD_WATER_SOURCE.get()
                || here == ModFluids.BLOOD_WATER_FLOWING.get());
        boolean hereWater = (here == Fluids.WATER || here == Fluids.FLOWING_WATER);
        boolean hereLava = (here == Fluids.LAVA || here == Fluids.FLOWING_LAVA);

        for (Direction dir : Direction.values()) {
            BlockPos otherPos = pos.relative(dir);

            FluidState fsOther = level.getFluidState(otherPos);
            Fluid other = fsOther.getType();
            if (other == Fluids.EMPTY)
                continue;

            boolean otherAether = (other == ModFluids.AETHER_SOURCE.get() || other == ModFluids.AETHER_FLOWING.get());
            boolean otherBloodWater = (other == ModFluids.BLOOD_WATER_SOURCE.get()
                    || other == ModFluids.BLOOD_WATER_FLOWING.get());
            boolean otherWater = (other == Fluids.WATER || other == Fluids.FLOWING_WATER);
            boolean otherLava = (other == Fluids.LAVA || other == Fluids.FLOWING_LAVA);

            // ------ SPECIFIC MIXING RULES ------
            // When Aether is involved
            if (hereAether && otherWater) {
                level.setBlock(otherPos, Blocks.BLACKSTONE.defaultBlockState(), 3);
                level.levelEvent(1501, otherPos, 0);
                return;
            }
            if (otherAether && hereWater) {
                level.setBlock(pos, Blocks.BLACKSTONE.defaultBlockState(), 3);
                level.levelEvent(1501, pos, 0);
                return;
            }
            if (hereAether && otherLava) {
                level.setBlock(otherPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                level.levelEvent(1501, otherPos, 0);
                return;
            }
            if (otherAether && hereLava) {
                level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                level.levelEvent(1501, pos, 0);
                return;
            }

            // When Blood Water is involved
            if (hereBloodWater && otherWater) {
                level.setBlock(otherPos,
                        net.ganyusbathwater.oririmod.block.ModBlocks.BLOOD_SLUDGE.get().defaultBlockState(), 3);
                level.levelEvent(1501, otherPos, 0);
                return;
            }
            if (otherBloodWater && hereWater) {
                level.setBlock(pos, net.ganyusbathwater.oririmod.block.ModBlocks.BLOOD_SLUDGE.get().defaultBlockState(),
                        3);
                level.levelEvent(1501, pos, 0);
                return;
            }
            if (hereBloodWater && otherAether) {
                level.setBlock(otherPos, Blocks.BLACKSTONE.defaultBlockState(), 3);
                level.levelEvent(1501, otherPos, 0);
                return;
            }
            if (otherBloodWater && hereAether) {
                level.setBlock(pos, Blocks.BLACKSTONE.defaultBlockState(), 3);
                level.levelEvent(1501, pos, 0);
                return;
            }

            // ------ FAILSAFE RULES FOR UNKNOWN FLUIDS ------
            // Check if "here" is Aether or Blood water and "other" is unknown
            if (hereAether && !(otherWater || otherLava || otherBloodWater || otherAether)) {
                level.setBlock(otherPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                level.levelEvent(1501, otherPos, 0);
                return;
            }
            if (hereBloodWater && !(otherWater || otherLava || otherAether || otherBloodWater)) {
                FluidType type = other.getFluidType();
                if (other.is(net.minecraft.tags.FluidTags.WATER) || type.getTemperature() <= 500) {
                    level.setBlock(otherPos,
                            net.ganyusbathwater.oririmod.block.ModBlocks.BLOOD_SLUDGE.get().defaultBlockState(), 3);
                    level.levelEvent(1501, otherPos, 0);
                    return;
                } else if (other.is(net.minecraft.tags.FluidTags.LAVA) || type.getTemperature() > 500) {
                    level.setBlock(otherPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                    level.levelEvent(1501, otherPos, 0);
                    return;
                }
            }

            // Symmetrical: Check if "other" is Aether or Blood water and "here" is unknown
            if (otherAether && !(hereWater || hereLava || hereBloodWater || hereAether)) {
                level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                level.levelEvent(1501, pos, 0);
                return;
            }
            if (otherBloodWater && !(hereWater || hereLava || hereAether || hereBloodWater)) {
                FluidType type = here.getFluidType();
                if (here.is(net.minecraft.tags.FluidTags.WATER) || type.getTemperature() <= 500) {
                    level.setBlock(pos,
                            net.ganyusbathwater.oririmod.block.ModBlocks.BLOOD_SLUDGE.get().defaultBlockState(), 3);
                    level.levelEvent(1501, pos, 0);
                    return;
                } else if (here.is(net.minecraft.tags.FluidTags.LAVA) || type.getTemperature() > 500) {
                    level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                    level.levelEvent(1501, pos, 0);
                    return;
                }
            }
        }
    }
}