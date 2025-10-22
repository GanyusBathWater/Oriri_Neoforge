package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.worldgen.tree.ElderGiantTreeConfig;
import net.ganyusbathwater.oririmod.worldgen.tree.ElderGiantTreeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, OririMod.MOD_ID);

    public static final DeferredHolder<Feature<?>, ElderGiantTreeFeature> ELDER_GIANT_TREE =
            FEATURES.register("elder_giant_tree", () -> new ElderGiantTreeFeature(ElderGiantTreeConfig.CODEC));

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}
