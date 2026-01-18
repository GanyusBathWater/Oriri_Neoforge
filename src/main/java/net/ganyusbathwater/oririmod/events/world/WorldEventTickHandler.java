// language: java
package net.ganyusbathwater.oririmod.events.world;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.level.ServerLevel;

/**
 * Periodischer Tick\-Handler: ruft alle CHECK_INTERVAL Ticks für alle ServerLevels
 * WorldEventManager.tick(level) auf, damit Tageszeitänderungen sofort erkannt werden.
 */
@EventBusSubscriber(modid = OririMod.MOD_ID)
public final class WorldEventTickHandler {
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // alle 20 ticks (1s)

    private WorldEventTickHandler() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // nur am Ende der Tick\-Phase prüfen (reduziert Risiko von Race\-Conditions)
        if (!event.hasTime()) return;

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        var server = event.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            WorldEventManager.tick(overworld);
        }
    }
}