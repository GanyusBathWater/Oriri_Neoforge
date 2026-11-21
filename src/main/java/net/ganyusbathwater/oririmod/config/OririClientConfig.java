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

        Client(ModConfigSpec.Builder b) {
            b.comment("Client UI & Inventory styling options").push("ui");
            customInventoryGui = b
                    .comment("Use Oriri's dark reliquary & chromatic-edge GUI style for Survival, Creative, Curios, and all vanilla container screens (Chests, Furnaces, Crafting Tables, Workstations, etc.).",
                            "Can be toggled live in-game without restarting.")
                    .define("customInventoryGui", true);
            b.pop();
        }
    }

    public static boolean isCustomInventoryEnabled() {
        return SPEC.isLoaded() && CLIENT.customInventoryGui.get();
    }
}
