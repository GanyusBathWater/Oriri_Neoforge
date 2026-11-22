package net.ganyusbathwater.oririmod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side visual and UI configuration ({@code oririmod-client.toml}).
 * Editable live in-game via NeoForge's Mods -> Oriri Mod -> Config screen.
 */
public class OririClientConfig {
    public static final ModConfigSpec SPEC;
    public static final Client CLIENT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        CLIENT = new Client(b);
        SPEC = b.build();
    }

    public static class Client {
        public final ModConfigSpec.BooleanValue customInventoryGui;
        public final ModConfigSpec.BooleanValue groundHazeEnabled;
        public final ModConfigSpec.DoubleValue groundHazeDensity;
        public final ModConfigSpec.IntValue groundHazeRadius;

        Client(ModConfigSpec.Builder b) {
            b.comment("Client UI & Inventory styling options").push("ui");
            customInventoryGui = b
                    .comment("Use Oriri's dark reliquary & chromatic-edge GUI style for Survival, Creative, Curios, and all vanilla container screens (Chests, Furnaces, Crafting Tables, Workstations, etc.).",
                            "Can be toggled live in-game without restarting.")
                    .define("customInventoryGui", true);
            b.pop();

            b.comment("Atmospheric ground haze & manual Fog Source rendering options").push("atmosphere");
            groundHazeEnabled = b
                    .comment("Enable Iris/Sodium-compatible floor-hugging atmospheric haze in misty biomes (at night and after rain) and around placed Fog Source blocks.")
                    .define("groundHazeEnabled", true);
            groundHazeDensity = b
                    .comment("Opacity multiplier for atmospheric ground haze (0.0 = hidden, 1.0 = default, 2.0 = dense).")
                    .defineInRange("groundHazeDensity", 1.0D, 0.0D, 2.0D);
            groundHazeRadius = b
                    .comment("Horizontal radius in blocks around the player for terrain ground haze (lower values improve FPS on low-end GPUs).")
                    .defineInRange("groundHazeRadius", 44, 24, 64);
            b.pop();
        }
    }

    public static boolean isCustomInventoryEnabled() {
        return SPEC.isLoaded() && CLIENT.customInventoryGui.get();
    }

    public static boolean isGroundHazeEnabled() {
        return !SPEC.isLoaded() || CLIENT.groundHazeEnabled.get();
    }

    public static float getGroundHazeDensity() {
        return SPEC.isLoaded() ? CLIENT.groundHazeDensity.get().floatValue() : 1.0F;
    }

    public static int getGroundHazeRadius() {
        return SPEC.isLoaded() ? CLIENT.groundHazeRadius.get() : 44;
    }
}
