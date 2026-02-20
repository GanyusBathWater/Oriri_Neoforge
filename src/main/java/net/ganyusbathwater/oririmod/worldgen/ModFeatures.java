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

        public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE,
                        OririMod.MOD_ID);

        public static final DeferredHolder<Feature<?>, ElderGiantTreeFeature> ELDER_GIANT_TREE = FEATURES
                        .register("elder_giant_tree", () -> new ElderGiantTreeFeature(ElderGiantTreeConfig.CODEC));

        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.ScarletBoulderFeature> SCARLET_BOULDER = FEATURES
                        .register("scarlet_boulder",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.ScarletBoulderFeature(
                                                        net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.BloodWaterPondFeature> BLOOD_WATER_POND = FEATURES
                        .register("blood_water_pond",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.BloodWaterPondFeature(
                                                        net.ganyusbathwater.oririmod.worldgen.feature.BloodWaterPondConfig.CODEC));

        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.ScarletDripstoneClusterFeature> DRIPSTONE_CLUSTER = FEATURES
                        .register("dripstone_cluster",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.ScarletDripstoneClusterFeature(
                                                        net.ganyusbathwater.oririmod.worldgen.feature.ScarletDripstoneClusterConfig.CODEC));

        public static void register(IEventBus bus) {
                FEATURES.register(bus);
        }
}
