package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.block.custom.UpgradedSaplingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void upgradeSaplingNoDestroyBlock(BlockPos pos, BlockState state, int flags, int recursionLeft,
            CallbackInfoReturnable<Boolean> cir) {
        if (UpgradedSaplingBlock.IS_FORCING_GROWTH.get()) {
            Level level = (Level) (Object) this;
            BlockState current = level.getBlockState(pos);
            boolean isTargetReplaceable = current.isAir()
                    || current.is(BlockTags.REPLACEABLE_BY_TREES)
                    || current.is(BlockTags.LEAVES)
                    || current.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock;

            boolean isReplacingWithDirt = state.is(BlockTags.DIRT);
            boolean isTargetDirt = current.is(BlockTags.DIRT) || current.is(Blocks.GRASS_BLOCK)
                    || current.is(Blocks.MYCELIUM) || current.is(Blocks.FARMLAND);

            if (isReplacingWithDirt && isTargetDirt) {
                isTargetReplaceable = true; // Allow podzol/dirt generation at the trunk base to replace grass/dirt
            }

            if (!isTargetReplaceable) {
                // Cancel placement of any tree feature (including logs, leaves, vines, cocoa)
                cir.setReturnValue(false);
            }
        }
    }
}
