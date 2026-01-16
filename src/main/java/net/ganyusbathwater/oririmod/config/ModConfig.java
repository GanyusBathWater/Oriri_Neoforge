package net.ganyusbathwater.oririmod.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.Type;

public class ModConfig {
    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(Type.COMMON, ManaConfig.SPEC, "oririmod-mana.toml");
        modContainer.registerConfig(Type.COMMON, WorldEventConfig.SPEC, "oririmod-world-events.toml");
    }
}