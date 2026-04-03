package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.worldgen.tree.ElderGiantTreeConfig;
import net.ganyusbathwater.oririmod.worldgen.feature.ScarletDripstoneClusterConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
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
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_GRASS_PATCH_KEY = registerKey("scarlet_grass_patch");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_TOOTH_LEAVES_PATCH_KEY = registerKey("scarlet_tooth_leaves_patch");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_LILY_PATCH_KEY = registerKey("scarlet_lily_patch");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_POND_PLAINS_KEY = registerKey("scarlet_pond_plains");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_POND_FOREST_KEY = registerKey("scarlet_pond_forest");
        public static final ResourceKey<ConfiguredFeature<?, ?>> ELDERWOODS_DRIPSTONE_CLUSTER_KEY = registerKey("elderwoods_dripstone_cluster");
        public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_DRIPSTONE_CLUSTER_KEY = registerKey("scarlet_dripstone_cluster");
        public static final ResourceKey<ConfiguredFeature<?, ?>> JADE_ORE_KEY = registerKey("jade_ore");
        public static final ResourceKey<ConfiguredFeature<?, ?>> DRAGON_IRON_ORE_KEY = registerKey("dragon_iron_ore");

        // Elysian Abyss features
        public static final ResourceKey<ConfiguredFeature<?, ?>> ELYSIAN_STONE_MUSHROOM_KEY = registerKey("elysian_stone_mushroom");
        public static final ResourceKey<ConfiguredFeature<?, ?>> ELYSIAN_ABYSS_CROWN_TREE_KEY = registerKey("elysian_abyss_crown_tree");

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
                GeodeConfiguration geodeConfig = new GeodeConfiguration(layerConfig, thickness, crack, 0.1D, 0.083D, false, UniformInt.of(4, 7), UniformInt.of(3, 5), UniformInt.of(1, 3), -16, 16, 0.05D, 1);
                register(context, OVERWORLD_MANA_GEODE_KEY, Feature.GEODE, geodeConfig);

                var elderCfg = new ElderGiantTreeConfig(UniformInt.of(10, 20), UniformInt.of(1, 3), UniformInt.of(3, 5), UniformInt.of(2, 4), BlockStateProvider.simple(ModBlocks.ELDER_LOG_BLOCK.get().defaultBlockState()), BlockStateProvider.simple(ModBlocks.ELDER_STEM_BLOCK.get().defaultBlockState()), BlockStateProvider.simple(ModBlocks.ELDER_STEM_BLOCK.get().defaultBlockState()), BlockStateProvider.simple(ModBlocks.ELDER_LEAVES.get().defaultBlockState()), BlockStateProvider.simple(ModBlocks.ELDER_LEAVES_FLOWERING.get().defaultBlockState()), 0.2f, true);
                register(context, ELDER_TREE_KEY, ModFeatures.ELDER_GIANT_TREE.get(), elderCfg);

                var scarletCfg = new ElderGiantTreeConfig(UniformInt.of(8, 15), UniformInt.of(1, 2), UniformInt.of(2, 4), UniformInt.of(2, 4), BlockStateProvider.simple(ModBlocks.SCARLET_LOG.get().defaultBlockState()), BlockStateProvider.simple(ModBlocks.SCARLET_STEM.get().defaultBlockState()), BlockStateProvider.simple(ModBlocks.SCARLET_STEM.get().defaultBlockState()), BlockStateProvider.simple(ModBlocks.SCARLET_LEAVES.get().defaultBlockState()), BlockStateProvider.simple(ModBlocks.SCARLET_LEAVES.get().defaultBlockState()), 0.0f, false);
                register(context, SCARLET_TREE_KEY, ModFeatures.ELDER_GIANT_TREE.get(), scarletCfg);

                var abyssCrownCfg = new net.ganyusbathwater.oririmod.worldgen.tree.AbyssCrownTreeConfig(UniformInt.of(6, 20), UniformInt.of(1, 2), UniformInt.of(5, 12), BlockStateProvider.simple(ModBlocks.ABYSS_CROWN_LOG.get().defaultBlockState()), BlockStateProvider.simple(ModBlocks.ABYSS_CROWN_STEM.get().defaultBlockState()), BlockStateProvider.simple(ModBlocks.ABYSS_CROWN_LEAVES.get().defaultBlockState()));
                register(context, ABYSS_CROWN_TREE_KEY, ModFeatures.ABYSS_CROWN_TREE_FEATURE.get(), abyssCrownCfg);

                register(context, SCARLET_BOULDER_KEY, ModFeatures.SCARLET_BOULDER.get(), NoneFeatureConfiguration.INSTANCE);

                register(context, SCARLET_GRASS_PATCH_KEY, Feature.RANDOM_PATCH, FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.SCARLET_GRASS.get())), List.of(Blocks.GRASS_BLOCK, ModBlocks.SCARLET_GRASS_BLOCK.get())));
                register(context, SCARLET_TOOTH_LEAVES_PATCH_KEY, Feature.RANDOM_PATCH, FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.SCARLET_TOOTH_LEAVES.get())), List.of(Blocks.GRASS_BLOCK, ModBlocks.SCARLET_GRASS_BLOCK.get())));
                register(context, SCARLET_LILY_PATCH_KEY, Feature.RANDOM_PATCH, new RandomPatchConfiguration(10, 7, 3, PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.SCARLET_LILY.get())))));

                register(context, SCARLET_POND_PLAINS_KEY, ModFeatures.BLOOD_WATER_POND.get(), new net.ganyusbathwater.oririmod.worldgen.feature.BloodWaterPondConfig(UniformInt.of(5, 12), UniformInt.of(2, 4)));
                register(context, SCARLET_POND_FOREST_KEY, ModFeatures.BLOOD_WATER_POND.get(), new net.ganyusbathwater.oririmod.worldgen.feature.BloodWaterPondConfig(UniformInt.of(3, 6), UniformInt.of(1, 2)));

                register(context, ELDERWOODS_DRIPSTONE_CLUSTER_KEY, ModFeatures.DRIPSTONE_CLUSTER.get(), new ScarletDripstoneClusterConfig(UniformInt.of(5, 10), 3, 3, 12, false));
                register(context, SCARLET_DRIPSTONE_CLUSTER_KEY, ModFeatures.DRIPSTONE_CLUSTER.get(), new ScarletDripstoneClusterConfig(UniformInt.of(5, 10), 3, 3, 12, true));

                List<OreConfiguration.TargetBlockState> jadeOreTargets = List.of(OreConfiguration.target(new BlockMatchTest(Blocks.STONE), ModBlocks.JADE_ORE.get().defaultBlockState()), OreConfiguration.target(new BlockMatchTest(Blocks.DEEPSLATE), ModBlocks.DEEPSLATE_JADE_ORE.get().defaultBlockState()));
                register(context, JADE_ORE_KEY, Feature.ORE, new OreConfiguration(jadeOreTargets, 12));

                List<OreConfiguration.TargetBlockState> dragonIronOreTargets = List.of(OreConfiguration.target(new BlockMatchTest(Blocks.STONE), ModBlocks.DRAGON_IRON_ORE.get().defaultBlockState()), OreConfiguration.target(new BlockMatchTest(Blocks.DEEPSLATE), ModBlocks.DEEPSLATE_DRAGON_IRON_ORE.get().defaultBlockState()));
                register(context, DRAGON_IRON_ORE_KEY, Feature.ORE, new OreConfiguration(dragonIronOreTargets, 8));

                register(context, ELYSIAN_STONE_MUSHROOM_KEY, ModFeatures.STONE_MUSHROOM.get(), new net.ganyusbathwater.oririmod.worldgen.feature.StoneMushRoomConfig(15, 35));

                var abyssElysianCfg = new net.ganyusbathwater.oririmod.worldgen.tree.AbyssCrownTreeConfig(UniformInt.of(8, 20), UniformInt.of(1, 2), UniformInt.of(5, 10), BlockStateProvider.simple(ModBlocks.ABYSS_CROWN_LOG.get().defaultBlockState()), BlockStateProvider.simple(ModBlocks.ABYSS_CROWN_STEM.get().defaultBlockState()), BlockStateProvider.simple(ModBlocks.ABYSS_CROWN_LEAVES.get().defaultBlockState()));
                register(context, ELYSIAN_ABYSS_CROWN_TREE_KEY, ModFeatures.ABYSS_CROWN_TREE_FEATURE.get(), abyssElysianCfg);
        }

        public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
                return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
        }

        private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
                context.register(key, new ConfiguredFeature<>(feature, configuration));
        }
}