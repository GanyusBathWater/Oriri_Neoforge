package net.ganyusbathwater.oririmod.config;

import net.ganyusbathwater.oririmod.util.SummonTargetMode;
import net.neoforged.neoforge.common.ModConfigSpec;

public class OririConfig {
    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        COMMON = new Common(b);
        SPEC = b.build();
    }

    public static class Common {
        public final Mana mana;
        public final WorldEvents worldEvents;
        public final Summoner summoner;

        Common(ModConfigSpec.Builder b) {
            // Mana section
            b.comment("Mana configuration").push("mana");
            mana = new Mana(b);
            b.pop();

            // World events section
            b.comment("World Event configuration").push("world_events");
            worldEvents = new WorldEvents(b);
            b.pop();

            // Summoner section
            b.comment("Summoner Weapon configuration").push("summoner");
            summoner = new Summoner(b);
            b.pop();
        }
    }

    public static class Mana {
        public final ModConfigSpec.IntValue regenIntervalSeconds;
        public final ModConfigSpec.IntValue regenAmount;
        public final ModConfigSpec.IntValue maxMana;

        Mana(ModConfigSpec.Builder b) {
            regenIntervalSeconds = b
                    .comment("Seconds between each mana regeneration step (server-side)")
                    .defineInRange("regenIntervalSeconds", 5, 1, 600);
            regenAmount = b
                    .comment("Amount of mana added every regen interval")
                    .defineInRange("regenAmount", 1, 0, 100);
            maxMana = b
                    .comment("Maximum mana value")
                    .defineInRange("maxMana", 100, 1, 10000);
        }
    }

    public static class WorldEvents {
        public final ModConfigSpec.DoubleValue nightEventChance;
        public final ModConfigSpec.DoubleValue eclipseChance;
        public final ModConfigSpec.IntValue nightEventDuration;
        public final ModConfigSpec.IntValue eclipseDuration;

        WorldEvents(ModConfigSpec.Builder b) {
            nightEventChance = b
                    .comment(
                            "Chance for a night event (Blood Moon, Green Moon) to occur at the beginning of a night. (0.0 to 1.0)")
                    .defineInRange("nightEventChance", 0.10, 0.0, 1.0);
            eclipseChance = b
                    .comment("Chance for an eclipse to occur during the day. (0.0 to 1.0)")
                    .defineInRange("eclipseChance", 0.05, 0.0, 1.0);
            nightEventDuration = b
                    .comment("Duration of night events in ticks (e.g., Blood Moon). 12000 ticks = 10 minutes.")
                    .defineInRange("nightEventDuration", 12000, 600, 24000);
            eclipseDuration = b
                    .comment("Duration of an eclipse in ticks. 6000 ticks = 5 minutes.")
                    .defineInRange("eclipseDuration", 6000, 600, 24000);
        }
    }

    public static class Summoner {
        public final ModConfigSpec.EnumValue<SummonTargetMode> targetMode;

        Summoner(ModConfigSpec.Builder b) {
            targetMode = b
                    .comment("Target mode for summoned mobs.",
                            "PVP = attacks everything including other players (except the summoner)",
                            "ALL_MOBS = attacks all mobs (hostile, neutral, passive) but never players",
                            "HOSTILE_ONLY = attacks only hostile mobs (monsters)")
                    .defineEnum("targetMode", SummonTargetMode.HOSTILE_ONLY);
        }
    }
}
