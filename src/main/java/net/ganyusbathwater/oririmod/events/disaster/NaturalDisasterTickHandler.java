package net.ganyusbathwater.oririmod.events.disaster;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.config.OririConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ticks the active natural disaster and performs the once-per-day natural roll.
 */
@EventBusSubscriber(modid = OririMod.MOD_ID)
public final class NaturalDisasterTickHandler {

    /** Time of day at which disasters are rolled (shortly after night has fallen, so meteors are visible). */
    private static final long ROLL_TIME = 14000L;
    /** Width of the roll window - robust against /time add skipping a single tick. */
    private static final long ROLL_WINDOW = 200L;

    private NaturalDisasterTickHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().overworld();
        if (overworld == null) {
            return;
        }
        NaturalDisasterManager manager = NaturalDisasterManager.get(overworld);
        manager.tick(event.getServer());
        tryNaturalRoll(overworld, manager);
    }

    private static void tryNaturalRoll(ServerLevel level, NaturalDisasterManager manager) {
        // With a frozen daylight cycle the time-of-day never changes - never auto-roll then
        if (!level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            return;
        }
        long dayTime = level.getDayTime();
        long timeOfDay = dayTime % 24000L;
        long day = dayTime / 24000L;
        if (timeOfDay < ROLL_TIME || timeOfDay >= ROLL_TIME + ROLL_WINDOW || manager.getLastRolledDay() == day) {
            return;
        }
        manager.setLastRolledDay(day);

        OririConfig.NaturalDisasters cfg = OririConfig.COMMON.naturalDisasters;
        if (!cfg.disastersEnabled.get() || manager.isActive()) {
            return;
        }

        // Shuffle so earlier enum entries are not favoured when multiple disasters pass their roll
        List<NaturalDisasterType> candidates = new ArrayList<>();
        for (NaturalDisasterType type : NaturalDisasterType.values()) {
            if (type.isImplemented() && type.settings() != null && type.settings().enabled.get()) {
                candidates.add(type);
            }
        }
        Collections.shuffle(candidates, new java.util.Random(level.random.nextLong()));

        for (NaturalDisasterType type : candidates) {
            if (level.random.nextDouble() < type.settings().chance.get()) {
                manager.start(level, type, type.settings().duration.get(), false);
                return;
            }
        }
    }
}

