package net.ganyusbathwater.oririmod.events.vestiges;

import net.ganyusbathwater.oririmod.item.custom.vestiges.SnowBoots;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.concurrent.atomic.AtomicBoolean;

@EventBusSubscriber(modid = "oririmod")
public final class SnowBootsDetectionEvents {

    private SnowBootsDetectionEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null) return;
        if (player.level().isClientSide) return;

        // Hält Knockback\-/PowderedSnow\-State stabil, auch wenn Biome während des Tragens wechselt.
        AtomicBoolean hasSnowBoots = new AtomicBoolean(false);

        CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
            inv.getCurios().values().forEach(handler -> {
                for (int i = 0; i < handler.getSlots(); i++) {
                    var stack = handler.getStacks().getStackInSlot(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof SnowBoots) {
                        hasSnowBoots.set(true);
                        return;
                    }
                }
            });
        });

        if (!hasSnowBoots.get()) {
        }
    }
}