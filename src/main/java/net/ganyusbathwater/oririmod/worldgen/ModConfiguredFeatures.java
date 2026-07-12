package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.worldgen.tree.ElderGiantTreeConfig;
import net.ganyusbathwater.oririmod.worldgen.feature.ScarletDripstoneClusterConfig;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;

import java.util.List;

public class ModConfiguredFeatures {
        public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_MANA_GEODE_KEY = registerKey("mana_geode");
        public static final ResourceKey<ConfiguredFeature<?, ?>> ELDER_TREE_KEY = registerKey("elder_tree");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_TREE_KEY = registerKey("scarlet_tree");
        public static final ResourceKey<ConfiguredFeature<?, ?>> ABYSS_CROWN_TREE_KEY = registerKey("abyss_crown_tree");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_BOULDER_KEY = registerKey("scarlet_boulder");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_GRASS_PATCH_KEY = registerKey(
                        "scarlet_grass_patch");
        public static final ResourceKey<ConfiguredFeature<?, ?>> ELDERWOODS_GRASS_PATCH_KEY = registerKey(
                        "elderwoods_grass_patch");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_TOOTH_LEAVES_PATCH_KEY = registerKey(
                        "scarlet_tooth_leaves_patch");
        public static final ResourceKey<ConfiguredFeature<?, ?>> STAR_HERB_PATCH_KEY = registerKey(
                        "star_herb_patch");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_LILY_PATCH_KEY = registerKey(
                        "scarlet_lily_patch");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_POND_PLAINS_KEY = registerKey(
                        "scarlet_pond_plains");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_POND_FOREST_KEY = registerKey(
                        "scarlet_pond_forest");
        public static final ResourceKey<ConfiguredFeature<?, ?>> ELDERWOODS_DRIPSTONE_CLUSTER_KEY = registerKey(
                        "elderwoods_dripstone_cluster");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_DRIPSTONE_CLUSTER_KEY = registerKey(
                        "scarlet_dripstone_cluster");
        public static final ResourceKey<ConfiguredFeature<?, ?>> JADE_ORE_KEY = registerKey("jade_ore");
        public static final ResourceKey<ConfiguredFeature<?, ?>> DRAGON_IRON_ORE_KEY = registerKey("dragon_iron_ore");

        public static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_LOGS_KEY = registerKey("fallen_logs");
        public static final ResourceKey<ConfiguredFeature<?, ?>> LEAF_PILE_KEY = registerKey("leaf_pile_patch");

        // Elysian Abyss features
        public static final ResourceKey<ConfiguredFeature<?, ?>> ELYSIAN_STONE_MUSHROOM_KEY = registerKey(
                        "elysian_stone_mushroom");
        public static final ResourceKey<ConfiguredFeature<?, ?>> BLOOD_CAP_KEY = registerKey("blood_cap");
        public static final ResourceKey<ConfiguredFeature<?, ?>> GLOWLINGS_KEY = registerKey("glowlings");
        public static final ResourceKey<ConfiguredFeature<?, ?>> ELYSIAN_ABYSS_CROWN_TREE_KEY = registerKey(
                        "elysian_abyss_crown_tree");
        public static final ResourceKey<ConfiguredFeature<?, ?>> ELYSIAN_ABYSS_PILLAR_KEY = registerKey(
                        "elysian_abyss_pillar");
        public static final ResourceKey<ConfiguredFeature<?, ?>> ELYSIAN_FLOOR_MOSS_KEY = registerKey(
                        "elysian_floor_moss");
        public static final ResourceKey<ConfiguredFeature<?, ?>> ELYSIAN_LICHEN_KEY = registerKey("elysian_lichen");
        public static final ResourceKey<ConfiguredFeature<?, ?>> ELYSIAN_FLOOR_SPIKE_KEY = registerKey(
                        "elysian_floor_spike");
        public static final ResourceKey<ConfiguredFeature<?, ?>> ELYSIAN_CEILING_SPIKE_KEY = registerKey(
                        "elysian_ceiling_spike");

        // Global Aether Rivers
        public static final ResourceKey<ConfiguredFeature<?, ?>> GLOBAL_AETHER_RIVER_KEY = registerKey(
                        "global_aether_river");

        public static final ResourceKey<ConfiguredFeature<?, ?>> ABYSS_RAVINE_KEY = registerKey("abyss_ravine");

        public static final ResourceKey<ConfiguredFeature<?, ?>> SEA_URCHIN_KEY = registerKey("sea_urchin");

        public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
                GeodeBlockSettings layerConfig = new GeodeBlockSettings(
                                BlockStateProvider.simple(Blocks.AIR.defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.MANA_CRYSTAL_BLOCK.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.MANA_CRYSTAL_BLOCK.get().defaultBlockState()),
                                BlockStateProvider.simple(Blocks.CALCITE.defaultBlockState()),
                                BlockStateProvider.simple(Blocks.SMOOTH_BASALT.defaultBlockState()),
                                List.<BlockState>of(ModBlocks.MANA_CRYSTAL_CLUSTER.get().defaultBlockState()),
                                BlockTags.FEATURES_CANNOT_REPLACE,
                                BlockTags.GEODE_INVALID_BLOCKS);

                GeodeLayerSettings thickness = new GeodeLayerSettings(1.7D, 2.2D, 3.2D, 4.2D);
                GeodeCrackSettings crack = new GeodeCrackSettings(0.95D, 1.75D, 2);
                GeodeConfiguration geodeConfig = new GeodeConfiguration(layerConfig, thickness, crack, 0.1D, 0.083D,
                                false, UniformInt.of(4, 7), UniformInt.of(3, 5), UniformInt.of(1, 3), -16, 16, 0.05D,
                                1);
                register(context, OVERWORLD_MANA_GEODE_KEY, Feature.GEODE, geodeConfig);

                var elderCfg = new ElderGiantTreeConfig(UniformInt.of(18, 46), UniformInt.of(1, 3), UniformInt.of(3, 5),
                                UniformInt.of(2, 4),
                                BlockStateProvider.simple(ModBlocks.ELDER_LOG_BLOCK.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.ELDER_STEM_BLOCK.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.ELDER_STEM_BLOCK.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.ELDER_LEAVES.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.ELDER_LEAVES_FLOWERING.get().defaultBlockState()),
                                0.2f, true, true);
                register(context, ELDER_TREE_KEY, ModFeatures.ELDER_GIANT_TREE.get(), elderCfg);

                var scarletCfg = new ElderGiantTreeConfig(UniformInt.of(8, 15), UniformInt.of(1, 2),
                                UniformInt.of(2, 4), UniformInt.of(2, 4),
                                BlockStateProvider.simple(ModBlocks.SCARLET_LOG.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.SCARLET_STEM.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.SCARLET_STEM.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.SCARLET_LEAVES.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.SCARLET_LEAVES.get().defaultBlockState()), 0.0f,
                                false, false);
                register(context, SCARLET_TREE_KEY, ModFeatures.ELDER_GIANT_TREE.get(), scarletCfg);

                var abyssCrownCfg = new net.ganyusbathwater.oririmod.worldgen.tree.AbyssCrownTreeConfig(
                                UniformInt.of(6, 20), UniformInt.of(1, 2), UniformInt.of(5, 12),
                                BlockStateProvider.simple(ModBlocks.ABYSS_CROWN_LOG.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.ABYSS_CROWN_STEM.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.ABYSS_CROWN_LEAVES.get().defaultBlockState()));
                register(context, ABYSS_CROWN_TREE_KEY, ModFeatures.ABYSS_CROWN_TREE_FEATURE.get(), abyssCrownCfg);

                register(context, SCARLET_BOULDER_KEY, ModFeatures.SCARLET_BOULDER.get(),
                                NoneFeatureConfiguration.INSTANCE);

                register(context, SCARLET_GRASS_PATCH_KEY, Feature.RANDOM_PATCH,
                                FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK,
                                                new SimpleBlockConfiguration(BlockStateProvider
                                                                .simple(ModBlocks.SCARLET_GRASS.get())),
                                                List.of(Blocks.GRASS_BLOCK, ModBlocks.SCARLET_GRASS_BLOCK.get())));

                register(context, ELDERWOODS_GRASS_PATCH_KEY, Feature.RANDOM_PATCH,
                                FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK,
                                                new SimpleBlockConfiguration(new net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider(
                                                                net.minecraft.util.random.SimpleWeightedRandomList.<BlockState>builder()
                                                                                .add(Blocks.SHORT_GRASS.defaultBlockState(), 3)
                                                )),
                                                List.of(Blocks.GRASS_BLOCK)));
                register(context, SCARLET_TOOTH_LEAVES_PATCH_KEY, Feature.RANDOM_PATCH,
                                FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK,
                                                new SimpleBlockConfiguration(BlockStateProvider
                                                                .simple(ModBlocks.SCARLET_TOOTH_LEAVES.get())),
                                                List.of(Blocks.GRASS_BLOCK, ModBlocks.SCARLET_GRASS_BLOCK.get())));
                register(context, SCARLET_LILY_PATCH_KEY, Feature.RANDOM_PATCH, new RandomPatchConfiguration(10, 7, 3,
                                PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
                                                BlockStateProvider.simple(ModBlocks.SCARLET_LILY.get())))));
                register(context, STAR_HERB_PATCH_KEY, Feature.FLOWER,
                                new RandomPatchConfiguration(16, 6, 2, PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK,
                                                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.STAR_HERB.get())))));

                register(context, FALLEN_LOGS_KEY, ModFeatures.FALLEN_LOGS.get(),
                                new BlockStateConfiguration(ModBlocks.ELDER_LOG_BLOCK.get().defaultBlockState()));

                register(context, LEAF_PILE_KEY, ModFeatures.ELDER_LEAF_PILE.get(),
                                net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.NONE);

                register(context, SCARLET_POND_PLAINS_KEY, ModFeatures.BLOOD_WATER_POND.get(),
                                new net.ganyusbathwater.oririmod.worldgen.feature.BloodWaterPondConfig(
                                                UniformInt.of(5, 12), UniformInt.of(2, 4)));
                register(context, SCARLET_POND_FOREST_KEY, ModFeatures.BLOOD_WATER_POND.get(),
                                new net.ganyusbathwater.oririmod.worldgen.feature.BloodWaterPondConfig(
                                                UniformInt.of(3, 6), UniformInt.of(1, 2)));

                register(context, ELDERWOODS_DRIPSTONE_CLUSTER_KEY, ModFeatures.DRIPSTONE_CLUSTER.get(),
                                new ScarletDripstoneClusterConfig(UniformInt.of(5, 10), 3, 3, 12, false));
                register(context, SCARLET_DRIPSTONE_CLUSTER_KEY, ModFeatures.DRIPSTONE_CLUSTER.get(),
                                new ScarletDripstoneClusterConfig(UniformInt.of(5, 10), 3, 3, 12, true));

                List<OreConfiguration.TargetBlockState> jadeOreTargets = List.of(
                                OreConfiguration.target(new BlockMatchTest(Blocks.STONE),
                                                ModBlocks.JADE_ORE.get().defaultBlockState()),
                                OreConfiguration.target(new BlockMatchTest(Blocks.DEEPSLATE),
                                                ModBlocks.DEEPSLATE_JADE_ORE.get().defaultBlockState()));
                register(context, JADE_ORE_KEY, Feature.ORE, new OreConfiguration(jadeOreTargets, 12));

                List<OreConfiguration.TargetBlockState> dragonIronOreTargets = List.of(
                                OreConfiguration.target(new BlockMatchTest(Blocks.STONE),
                                                ModBlocks.DRAGON_IRON_ORE.get().defaultBlockState()),
                                OreConfiguration.target(new BlockMatchTest(Blocks.DEEPSLATE),
                                                ModBlocks.DEEPSLATE_DRAGON_IRON_ORE.get().defaultBlockState()));
                register(context, DRAGON_IRON_ORE_KEY, Feature.ORE, new OreConfiguration(dragonIronOreTargets, 8));

                register(context, ELYSIAN_STONE_MUSHROOM_KEY, ModFeatures.STONE_MUSHROOM.get(),
                                new net.ganyusbathwater.oririmod.worldgen.feature.StoneMushRoomConfig(10, 30));

                register(context, BLOOD_CAP_KEY, Feature.RANDOM_PATCH,
                                FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK,
                                                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.BLOOD_CAP_BLOCK.get())),
                                                List.of(ModBlocks.SCARLET_GRASS_BLOCK.get(), ModBlocks.SCARLET_MOSS.get())));

                register(context, GLOWLINGS_KEY, Feature.RANDOM_PATCH,
                                FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK,
                                                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.GLOWLINGS_BLOCK.get())),
                                                List.of(Blocks.STONE, Blocks.DEEPSLATE)));

                var abyssElysianCfg = new net.ganyusbathwater.oririmod.worldgen.tree.AbyssCrownTreeConfig(
                                UniformInt.of(8, 20), UniformInt.of(1, 2), UniformInt.of(5, 10),
                                BlockStateProvider.simple(ModBlocks.ABYSS_CROWN_LOG.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.ABYSS_CROWN_STEM.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.ABYSS_CROWN_LEAVES.get().defaultBlockState()));
                register(context, ELYSIAN_ABYSS_CROWN_TREE_KEY, ModFeatures.ABYSS_CROWN_TREE_FEATURE.get(),
                                abyssElysianCfg);

                register(context, ELYSIAN_ABYSS_PILLAR_KEY, ModFeatures.ABYSS_PILLAR.get(),
                                NoneFeatureConfiguration.INSTANCE);
                register(context, ELYSIAN_FLOOR_SPIKE_KEY, ModFeatures.ABYSS_SPIKE.get(),
                                new net.ganyusbathwater.oririmod.worldgen.feature.AbyssSpikeFeature.AbyssSpikeConfig(
                                                false));
                register(context, ELYSIAN_CEILING_SPIKE_KEY, ModFeatures.ABYSS_SPIKE.get(),
                                new net.ganyusbathwater.oririmod.worldgen.feature.AbyssSpikeFeature.AbyssSpikeConfig(
                                                true));

                // Floor moss — two-step: place MOSS_BLOCK patches on stone/deepslate,
                // then MOSS_CARPET on top of those. Bone-meal-analogue plants (carpet, roots).
                register(context, ELYSIAN_FLOOR_MOSS_KEY, Feature.RANDOM_PATCH,
                                FeatureUtils.simplePatchConfiguration(
                                                Feature.SIMPLE_BLOCK,
                                                new SimpleBlockConfiguration(
                                                                BlockStateProvider.simple(Blocks.MOSS_CARPET)),
                                                List.of(Blocks.MOSS_BLOCK)));

                // Glow lichen on floor, walls, and ceiling — MULTIFACE_GROWTH handles all axes.
                HolderSet<Block> lichenOn = HolderSet.direct(Block::builtInRegistryHolder,
                                Blocks.STONE, Blocks.DEEPSLATE, Blocks.CALCITE,
                                Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE,
                                Blocks.TUFF, Blocks.BASALT, Blocks.MOSS_BLOCK);
                register(context, ELYSIAN_LICHEN_KEY, Feature.MULTIFACE_GROWTH,
                                new MultifaceGrowthConfiguration(
                                                (MultifaceBlock) Blocks.GLOW_LICHEN,
                                                20, // search range
                                                true, // can place on floor
                                                true, // can place on ceiling
                                                true, // can place on walls
                                                0.5f, // spread chance
                                                lichenOn));

                register(context, GLOBAL_AETHER_RIVER_KEY, ModFeatures.GLOBAL_AETHER_RIVER.get(),
                                FeatureConfiguration.NONE);
                register(context, ABYSS_RAVINE_KEY, ModFeatures.ABYSS_RAVINE.get(), new NoneFeatureConfiguration());

                register(context, SEA_URCHIN_KEY, ModFeatures.SEA_URCHIN_FEATURE.get(), new NoneFeatureConfiguration());
        }

        public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
                return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
        }

        private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(
                        BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key,
                        F feature, FC configuration) {
                context.register(key, new ConfiguredFeature<>(feature, configuration));
        }
}