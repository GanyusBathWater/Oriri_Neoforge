package net.ganyusbathwater.oririmod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ManaConfig {
    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        COMMON = new Common(b);
        SPEC = b.build();
    }

    public static class Common {
        public final ModConfigSpec.IntValue regenIntervalSeconds;
        public final ModConfigSpec.IntValue regenAmount;
        public final ModConfigSpec.IntValue maxMana;

        Common(ModConfigSpec.Builder b) {
            b.comment("Mana configuration").push("mana");
            regenIntervalSeconds = b
                    .comment("Seconds between each mana regeneration step (server-side)")
                    .defineInRange("regenIntervalSeconds", 5, 1, 600);
            regenAmount = b
                    .comment("Amount of mana added every regen interval")
                    .defineInRange("regenAmount", 1, 0, 100);
            maxMana = b
                    .comment("Maximum mana value")
                    .defineInRange("maxMana", 100, 1, 10000);
            b.pop();
        }
    }
}
