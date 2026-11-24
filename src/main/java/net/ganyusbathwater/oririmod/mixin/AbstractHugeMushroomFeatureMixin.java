package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.block.custom.UpgradedSaplingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHugeMushroomFeature.class)
public abstract class AbstractHugeMushroomFeatureMixin {

    @Inject(method = "isValidPosition", at = @At("HEAD"), cancellable = true)
    private void oriri$allowGrowAnywhereIfForced(LevelAccessor level, BlockPos pos, int maxHeight,
            BlockPos.MutableBlockPos mutablePos, HugeMushroomFeatureConfiguration config,
            CallbackInfoReturnable<Boolean> cir) {
        if (UpgradedSaplingBlock.IS_FORCING_GROWTH.get()) {
            int y = pos.getY();
            cir.setReturnValue(y >= level.getMinBuildHeight() + 1 && y + maxHeight + 1 < level.getMaxBuildHeight());
        }
    }
}
