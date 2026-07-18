package net.ganyusbathwater.oririmod.events;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.custom.ForcefieldEmitterBlock;
import net.ganyusbathwater.oririmod.block.entity.ForcefieldEmitterBlockEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class ForcefieldEvents {

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getLevel() instanceof Level level) {
            if (ForcefieldEmitterBlockEntity.isAttractingNearby(level, event.getEntity().blockPosition())) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.SUCCEED); // Bypasses light level constraints and other position checks
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() != null && !event.getPlayer().isCreative()) {
            Level level = event.getPlayer().level();
            // Allow breaking the forcefield emitter itself so players can remove it
            if (event.getState().getBlock() instanceof ForcefieldEmitterBlock) {
                return;
            }
            if (ForcefieldEmitterBlockEntity.isProtectionNearby(level, event.getPos())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player && !player.isCreative()) {
            Level level = player.level();
            // Pass the placed block's position as the excluded position, so the emitter doesn't block itself
            if (ForcefieldEmitterBlockEntity.isProtectionNearby(level, event.getPos(), event.getPos())) {
                event.setCanceled(true);
            }
        }
    }
}
