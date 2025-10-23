package net.ganyusbathwater.oririmod.worldgen;

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
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;

public class ModConfiguredFeatures {
    //How the Tree/Block/Ore/Structure will look like, basically the shape and all

    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_MANA_GEODE_KEY = registerKey("mana_geode");

    public static final ResourceKey<ConfiguredFeature<?, ?>> ELDER_TREE_KEY = registerKey("elder_tree");

    //here will be the Features be defined and later turned into json files
    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
    //there need to exist rules for the feature, for example which Blocks it may override to place itself

        //Layer (filling, inner, alternateInner, middle, outer, inner_placements, cannotReplaceTag, invalidBlocksTag)
        GeodeBlockSettings layerConfig = new GeodeBlockSettings(

                BlockStateProvider.simple(Blocks.AIR.defaultBlockState()),
                BlockStateProvider.simple(ModBlocks.MANA_CRYSTAL_BLOCK.get().defaultBlockState()),
                BlockStateProvider.simple(ModBlocks.MANA_CRYSTAL_BLOCK.get().defaultBlockState()),
                BlockStateProvider.simple(Blocks.CALCITE.defaultBlockState()),
                BlockStateProvider.simple(Blocks.SMOOTH_BASALT.defaultBlockState()),

                // inner placements (Clusters)
                List.<BlockState>of(
                        ModBlocks.MANA_CRYSTAL_CLUSTER.get().defaultBlockState()
                ),
                BlockTags.FEATURES_CANNOT_REPLACE,
                BlockTags.GEODE_INVALID_BLOCKS
        );

        GeodeLayerSettings thickness = new GeodeLayerSettings(
                1.7D, // filling radius
                2.2D,       // inner radius
                3.2D,       // middle radius
                4.2D        // outer radius
        );

        GeodeCrackSettings crack = new GeodeCrackSettings(
                0.95D, // Crack Chance
                1.75D,                    // Crack Size
                2                        // Crack Offset
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
                16,              // maxGenOffset
                0.05D,          // noiseMultiplier
                1              // invalidBlocksThreshold
        );
        

        register(context, OVERWORLD_MANA_GEODE_KEY, Feature.GEODE, geodeConfig);

        var elderCfg = new ElderGiantTreeConfig(
                UniformInt.of(15, 35), // Stammhöhe
                UniformInt.of(2, 5),   // Stammradius
                UniformInt.of(6, 12),   // Kronenradius
                UniformInt.of(1, 4),   // Astlänge
                BlockStateProvider.simple(net.ganyusbathwater.oririmod.block.ModBlocks.ELDER_LOG_BLOCK.get().defaultBlockState()),
                BlockStateProvider.simple(net.ganyusbathwater.oririmod.block.ModBlocks.ELDER_LEAVES.get().defaultBlockState()),
                BlockStateProvider.simple(net.ganyusbathwater.oririmod.block.ModBlocks.ELDER_LEAVES_FLOWERING.get().defaultBlockState()),
                0.25f
        );
        register(context, ELDER_TREE_KEY, ModFeatures.ELDER_GIANT_TREE.get(), elderCfg);
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}