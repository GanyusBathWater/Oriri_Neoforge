package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.worldgen.tree.ElderGiantTreeConfig;
import net.ganyusbathwater.oririmod.worldgen.tree.ElderGiantTreeFeature;
import net.ganyusbathwater.oririmod.worldgen.tree.AbyssCrownTreeConfig;
import net.ganyusbathwater.oririmod.worldgen.tree.AbyssCrownTreeFeature;
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

        public static final DeferredHolder<Feature<?>, AbyssCrownTreeFeature> ABYSS_CROWN_TREE_FEATURE = FEATURES
                        .register("abyss_crown_tree", () -> new AbyssCrownTreeFeature(AbyssCrownTreeConfig.CODEC));

        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.ScarletBoulderFeature> SCARLET_BOULDER = FEATURES
                        .register("scarlet_boulder",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.ScarletBoulderFeature(
                                                        net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));



        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.ScarletDripstoneClusterFeature> DRIPSTONE_CLUSTER = FEATURES
                        .register("dripstone_cluster",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.ScarletDripstoneClusterFeature(
                                                        net.ganyusbathwater.oririmod.worldgen.feature.ScarletDripstoneClusterConfig.CODEC));

        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.StoneMushRoomFeature> STONE_MUSHROOM = FEATURES
                        .register("stone_mushroom",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.StoneMushRoomFeature(
                                                        net.ganyusbathwater.oririmod.worldgen.feature.StoneMushRoomConfig.CODEC));

        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.GlobalAetherRiverFeature> GLOBAL_AETHER_RIVER = FEATURES
                        .register("global_aether_river",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.GlobalAetherRiverFeature(
                                                        net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.AbyssPillarFeature> ABYSS_PILLAR = FEATURES
                        .register("abyss_pillar",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.AbyssPillarFeature(
                                                        net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.AbyssSpikeFeature> ABYSS_SPIKE = FEATURES
                        .register("abyss_spike",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.AbyssSpikeFeature(
                                                        net.ganyusbathwater.oririmod.worldgen.feature.AbyssSpikeFeature.AbyssSpikeConfig.CODEC));



        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.SeaUrchinFeature> SEA_URCHIN_FEATURE = FEATURES
                        .register("sea_urchin",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.SeaUrchinFeature(
                                                        net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.FallenLogsFeature> FALLEN_LOGS = FEATURES
                        .register("fallen_logs",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.FallenLogsFeature(
                                                        net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration.CODEC));

        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.ElderLeafPileFeature> ELDER_LEAF_PILE = FEATURES
                        .register("elder_leaf_pile",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.ElderLeafPileFeature(
                                                        net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

        // Golden Desert Features
        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.MyriadCactusFeature> MYRIAD_CACTUS_FEATURE = FEATURES
                        .register("myriad_cactus",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.MyriadCactusFeature(
                                                        net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.EpochTreeFeature> EPOCH_TREE_FEATURE = FEATURES
                        .register("epoch_tree",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.EpochTreeFeature(
                                                        net.ganyusbathwater.oririmod.worldgen.feature.EpochTreeFeature.EpochTreeConfig.CODEC));

        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.WarGraveFeature> WAR_GRAVE_FEATURE = FEATURES
                        .register("war_grave",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.WarGraveFeature(
                                                        net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.OasisFeature> OASIS_FEATURE = FEATURES
                        .register("oasis",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.OasisFeature(
                                                        net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

        public static final DeferredHolder<Feature<?>, net.ganyusbathwater.oririmod.worldgen.feature.QuicksandPondFeature> QUICKSAND_POND_FEATURE = FEATURES
                        .register("quicksand_pond",
                                        () -> new net.ganyusbathwater.oririmod.worldgen.feature.QuicksandPondFeature(
                                                        net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

        public static void register(IEventBus bus) {
                FEATURES.register(bus);
        }
}

