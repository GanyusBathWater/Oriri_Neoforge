package net.ganyusbathwater.oririmod.worldgen;

import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.worldgen.tree.ElderGiantTreeConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.List;

import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.ganyusbathwater.oririmod.worldgen.feature.ScarletDripstoneClusterConfig;

public class ModConfiguredFeatures {
        // How the Tree/Block/Ore/Structure will look like, basically the shape and all

        public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_MANA_GEODE_KEY = registerKey("mana_geode");

        public static final ResourceKey<ConfiguredFeature<?, ?>> ELDER_TREE_KEY = registerKey("elder_tree");

        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_TREE_KEY = registerKey("scarlet_tree");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_BOULDER_KEY = registerKey(
                        "scarlet_boulder");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_GRASS_PATCH_KEY = registerKey(
                        "scarlet_grass_patch");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_TOOTH_LEAVES_PATCH_KEY = registerKey(
                        "scarlet_tooth_leaves_patch");
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

        // here will be the Features be defined and later turned into json files
        public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
                // there need to exist rules for the feature, for example which Blocks it may
                // override to place itself

                // Layer (filling, inner, alternateInner, middle, outer, inner_placements,
                // cannotReplaceTag, invalidBlocksTag)
                GeodeBlockSettings layerConfig = new GeodeBlockSettings(

                                BlockStateProvider.simple(Blocks.AIR.defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.MANA_CRYSTAL_BLOCK.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.MANA_CRYSTAL_BLOCK.get().defaultBlockState()),
                                BlockStateProvider.simple(Blocks.CALCITE.defaultBlockState()),
                                BlockStateProvider.simple(Blocks.SMOOTH_BASALT.defaultBlockState()),

                                // inner placements (Clusters)
                                List.<BlockState>of(
                                                ModBlocks.MANA_CRYSTAL_CLUSTER.get().defaultBlockState()),
                                BlockTags.FEATURES_CANNOT_REPLACE,
                                BlockTags.GEODE_INVALID_BLOCKS);

                GeodeLayerSettings thickness = new GeodeLayerSettings(
                                1.7D, // filling radius
                                2.2D, // inner radius
                                3.2D, // middle radius
                                4.2D // outer radius
                );

                GeodeCrackSettings crack = new GeodeCrackSettings(
                                0.95D, // Crack Chance
                                1.75D, // Crack Size
                                2 // Crack Offset
                );

                IntProvider outerWallDistance = UniformInt.of(4, 7);
                IntProvider distributionPoints = UniformInt.of(3, 5);
                IntProvider pointOffset = UniformInt.of(1, 3);

                GeodeConfiguration geodeConfig = new GeodeConfiguration(
                                layerConfig,
                                thickness,
                                crack,
                                0.1D,
                                0.083D,
                                false,
                                outerWallDistance,
                                distributionPoints,
                                pointOffset,
                                -16, // minGenOffset
                                16, // maxGenOffset
                                0.05D, // noiseMultiplier
                                1 // invalidBlocksThreshold
                );

                register(context, OVERWORLD_MANA_GEODE_KEY, Feature.GEODE, geodeConfig);

                var elderCfg = new ElderGiantTreeConfig(
                                UniformInt.of(10, 20),
                                UniformInt.of(1, 3),
                                UniformInt.of(3, 5),
                                UniformInt.of(2, 4),
                                BlockStateProvider.simple(ModBlocks.ELDER_LOG_BLOCK.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.ELDER_STEM_BLOCK.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.ELDER_STEM_BLOCK.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.ELDER_LEAVES.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.ELDER_LEAVES_FLOWERING.get().defaultBlockState()),
                                0.2f,
                                true // Place Spore Blossoms
                );
                register(context, ELDER_TREE_KEY, ModFeatures.ELDER_GIANT_TREE.get(), elderCfg);

                // Scarlet Tree - uses same feature as Elder but with sparser leaves and
                // different blocks
                var scarletCfg = new ElderGiantTreeConfig(
                                UniformInt.of(8, 15), // Shorter trunk height (was 10-20)
                                UniformInt.of(1, 2), // Thinner trunk radius (was 1-3)
                                UniformInt.of(2, 4), // Smaller canopy radius (was 3-5)
                                UniformInt.of(2, 4), // Longer branches
                                BlockStateProvider.simple(ModBlocks.SCARLET_LOG.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.SCARLET_STEM.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.SCARLET_STEM.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.SCARLET_LEAVES.get().defaultBlockState()),
                                BlockStateProvider.simple(ModBlocks.SCARLET_LEAVES.get().defaultBlockState()), // No
                                                                                                               // flowering
                                                                                                               // variant
                                0.0f, // No flowering chance
                                false // No Spore Blossoms
                );
                register(context, SCARLET_TREE_KEY, ModFeatures.ELDER_GIANT_TREE.get(), scarletCfg);

                register(context, SCARLET_BOULDER_KEY, ModFeatures.SCARLET_BOULDER.get(),
                                net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE);

                // Scarlet Vegetation
                // Grass Patch
                register(context, SCARLET_GRASS_PATCH_KEY, Feature.RANDOM_PATCH,
                                FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK,
                                                new SimpleBlockConfiguration(BlockStateProvider
                                                                .simple(ModBlocks.SCARLET_GRASS.get())),
                                                List.of(Blocks.GRASS_BLOCK, ModBlocks.SCARLET_GRASS_BLOCK.get())));

                // Tooth Leaves Patch (scattered)
                register(context, SCARLET_TOOTH_LEAVES_PATCH_KEY, Feature.RANDOM_PATCH,
                                FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK,
                                                new SimpleBlockConfiguration(BlockStateProvider
                                                                .simple(ModBlocks.SCARLET_TOOTH_LEAVES.get())),
                                                List.of(Blocks.GRASS_BLOCK, ModBlocks.SCARLET_GRASS_BLOCK.get())));

                // Scarlet Lily Patch (on water)
                register(context, SCARLET_LILY_PATCH_KEY, Feature.RANDOM_PATCH,
                                new RandomPatchConfiguration(10, 7, 3,
                                                PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK,
                                                                new SimpleBlockConfiguration(BlockStateProvider.simple(
                                                                                ModBlocks.SCARLET_LILY.get())))));

                // Scarlet Ponds
                register(context, SCARLET_POND_PLAINS_KEY, ModFeatures.BLOOD_WATER_POND.get(),
                                new net.ganyusbathwater.oririmod.worldgen.feature.BloodWaterPondConfig(
                                                UniformInt.of(5, 12), UniformInt.of(2, 4)));

                register(context, SCARLET_POND_FOREST_KEY, ModFeatures.BLOOD_WATER_POND.get(),
                                new net.ganyusbathwater.oririmod.worldgen.feature.BloodWaterPondConfig(
                                                UniformInt.of(3, 6), UniformInt.of(1, 2)));

                // Dripstone Clusters
                // Elderwoods caves — vanilla dripstone blocks
                register(context, ELDERWOODS_DRIPSTONE_CLUSTER_KEY, ModFeatures.DRIPSTONE_CLUSTER.get(),
                                new ScarletDripstoneClusterConfig(
                                                UniformInt.of(5, 10), // cluster radius
                                                3, // max stalactite height
                                                3, // max stalagmite height
                                                12, // floor-to-ceiling search range
                                                false // use vanilla dripstone
                                ));

                // Scarlet caves — scarlet dripstone blocks
                register(context, SCARLET_DRIPSTONE_CLUSTER_KEY, ModFeatures.DRIPSTONE_CLUSTER.get(),
                                new ScarletDripstoneClusterConfig(
                                                UniformInt.of(5, 10), // cluster radius (slightly larger)
                                                3, // max stalactite height
                                                3, // max stalagmite height
                                                12, // floor-to-ceiling search range
                                                true // use scarlet dripstone
                                ));

                List<OreConfiguration.TargetBlockState> jadeOreTargets = List.of(
                                OreConfiguration.target(new BlockMatchTest(Blocks.STONE),
                                                ModBlocks.JADE_ORE.get().defaultBlockState()),
                                OreConfiguration.target(new BlockMatchTest(Blocks.DEEPSLATE),
                                                ModBlocks.DEEPSLATE_JADE_ORE.get().defaultBlockState()));

                register(context, JADE_ORE_KEY, Feature.ORE, new OreConfiguration(jadeOreTargets, 12));

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