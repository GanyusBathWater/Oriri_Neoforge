package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.events.world.WorldEventManager;
import net.ganyusbathwater.oririmod.events.world.WorldEventType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnPlacements.class)
public abstract class SpawnPlacementsMixin {

    @Inject(
            method = "checkSpawnRules",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void oriri$allowMonsterSpawnsDuringBloodMoon(
            EntityType<?> entityType, ServerLevelAccessor serverLevel, MobSpawnType spawnType, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir
    ) {
        if (WorldEventManager.isEventActive(WorldEventType.ECLIPSE)
                && spawnType == MobSpawnType.NATURAL
                && entityType.getCategory() == MobCategory.MONSTER) {

            cir.setReturnValue(true);
        }
    }
}

