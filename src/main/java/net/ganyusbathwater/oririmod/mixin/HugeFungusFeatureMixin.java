package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.block.custom.UpgradedSaplingBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HugeFungusFeature.class)
public abstract class HugeFungusFeatureMixin {

    @Redirect(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",
                    ordinal = 0
            )
    )
    private boolean oriri$allowAnyBaseBlockIfForced(BlockState belowState, Block validBaseBlock) {
        if (UpgradedSaplingBlock.IS_FORCING_GROWTH.get()) {
            return true;
        }
        return belowState.is(validBaseBlock);
    }
}
