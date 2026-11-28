package net.ganyusbathwater.oririmod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side visual and UI configuration ({@code oririmod-client.toml}).
 * Editable live in-game via NeoForge's Mods -> Oriri Mod -> Config screen.
 */
public class OririClientConfig {
    public static final ModConfigSpec SPEC;
    public static final Client CLIENT;

    /**
     * Controls how mythical head cosmetics (Kitsune ears, Elven ears, Lamia ears,
     * Mermaid & Silent Nereid headfins) interact with equipped helmets.
     */
    public enum CosmeticHelmetMode {
        /** Hide the helmet armor model when a head-cosmetic Essence is equipped (keeps stats, mythical ears always visible). */
        HIDE_HELMET,
        /** Keep the helmet visible and hide cosmetic ears/headfins whenever a helmet is equipped (zero clipping). */
        HIDE_COSMETIC_HEAD,
        /** Render both the helmet and the cosmetic ears/headfins. */
        SHOW_BOTH
    }

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
        public final ModConfigSpec.EnumValue<CosmeticHelmetMode> cosmeticHelmetMode;

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

            b.comment("Player Essence cosmetic visual preferences").push("cosmetics");
            cosmeticHelmetMode = b
                    .comment("How mythical head cosmetics (Kitsune ears, Elven ears, Lamia ears, Mermaid/Silent Nereid headfins) handle equipped helmets:",
                            "  HIDE_HELMET        - Hide the helmet visual while a head-cosmetic Essence is equipped (armor stats still apply).",
                            "  HIDE_COSMETIC_HEAD - Show the helmet and hide cosmetic ears/headfins while a helmet is worn (no clipping).",
                            "  SHOW_BOTH          - Render both the helmet and the cosmetic ears/headfins.")
                    .defineEnum("cosmeticHelmetMode", CosmeticHelmetMode.HIDE_HELMET);
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

    public static CosmeticHelmetMode getCosmeticHelmetMode() {
        return SPEC.isLoaded() ? CLIENT.cosmeticHelmetMode.get() : CosmeticHelmetMode.HIDE_HELMET;
    }
}
