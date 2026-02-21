package net.ganyusbathwater.oririmod.mixin;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.custom.UpgradedSaplingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;
import net.minecraft.world.level.LevelWriter;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;

@Mixin(TreeFeature.class)
public abstract class TreeFeatureMixin extends Feature<TreeConfiguration> {

    public TreeFeatureMixin(Codec<TreeConfiguration> p_65786_) {
        super(p_65786_);
    }

    @Inject(method = "isAirOrLeaves", at = @At("HEAD"), cancellable = true)
    private static void alwaysReturnTrueIfForced(LevelSimulatedReader level, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        if (UpgradedSaplingBlock.IS_FORCING_GROWTH.get()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "validTreePos", at = @At("HEAD"), cancellable = true)
    private static void alwaysValidIfForced(LevelSimulatedReader level, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        if (UpgradedSaplingBlock.IS_FORCING_GROWTH.get()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "setBlockKnownShape", at = @At("HEAD"), cancellable = true)
    private static void onSetBlock(LevelWriter level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (UpgradedSaplingBlock.IS_FORCING_GROWTH.get()) {
            if (level instanceof LevelAccessor accessor) {
                BlockState current = accessor.getBlockState(pos);
                boolean replaceable = current.isAir() || current.is(BlockTags.LEAVES) || current.canBeReplaced();
                if (!replaceable) {
                    ci.cancel(); // Cancel placement if hitting solid stone/wall
                }
            }
        }
    }
}
