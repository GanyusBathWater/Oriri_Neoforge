package net.ganyusbathwater.oririmod.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.Type;

public class ModConfig {
    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(Type.COMMON, OririConfig.SPEC, "oririmod-common.toml");
    }
}