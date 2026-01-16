package net.ganyusbathwater.oririmod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class WorldEventConfig {
    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        COMMON = new Common(b);
        SPEC = b.build();
    }

    public static class Common {
        public final ModConfigSpec.DoubleValue nightEventChance;
        public final ModConfigSpec.DoubleValue eclipseChance;
        public final ModConfigSpec.IntValue nightEventDuration;
        public final ModConfigSpec.IntValue eclipseDuration;


        Common(ModConfigSpec.Builder b) {
            b.comment("World Event configuration").push("world_events");
            nightEventChance = b
                    .comment("Chance for a night event (Blood Moon, Green Moon) to occur at the beginning of a night. (0.0 to 1.0)")
                    .defineInRange("nightEventChance", 0.9, 0.0, 1.0);
            eclipseChance = b
                    .comment("Chance for an eclipse to occur during the day. (0.0 to 1.0)")
                    .defineInRange("eclipseChance", 0.95, 0.0, 1.0);
            nightEventDuration = b
                    .comment("Duration of night events in ticks (e.g., Blood Moon). 12000 ticks = 10 minutes.")
                    .defineInRange("nightEventDuration", 12000, 600, 24000);
            eclipseDuration = b
                    .comment("Duration of an eclipse in ticks. 6000 ticks = 5 minutes.")
                    .defineInRange("eclipseDuration", 6000, 600, 24000);
            b.pop();
        }
    }
}