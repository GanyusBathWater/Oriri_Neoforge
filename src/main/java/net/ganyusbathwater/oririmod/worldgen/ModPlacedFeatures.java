package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

import static net.minecraft.world.level.levelgen.VerticalAnchor.aboveBottom;
import static net.minecraft.world.level.levelgen.VerticalAnchor.absolute;

public class ModPlacedFeatures {
        // How many Trees/Blocks/Ores/Structures will spawn or where they spawn

        public static final ResourceKey<PlacedFeature> OVERWORLD_MANA_GEODE_PLACED_KEY = registerKey(
                        "mana_geode_placed");

        public static final ResourceKey<PlacedFeature> ELDER_TREE_PLACED_KEY = registerKey("elder_tree_placed");
        public static final ResourceKey<PlacedFeature> SCARLET_TREE_PLACED_KEY = registerKey("scarlet_tree_placed");
        public static final ResourceKey<PlacedFeature> SCARLET_BOULDER_PLACED_KEY = registerKey(
                        "scarlet_boulder_placed");
        public static final ResourceKey<PlacedFeature> SCARLET_GRASS_PATCH_PLACED_KEY = registerKey(
                        "scarlet_grass_patch_placed");
        public static final ResourceKey<PlacedFeature> SCARLET_TOOTH_LEAVES_PATCH_PLACED_KEY = registerKey(
                        "scarlet_tooth_leaves_patch_placed");
        public static final ResourceKey<PlacedFeature> SCARLET_LILY_PATCH_PLACED_KEY = registerKey(
                        "scarlet_lily_patch_placed");
        public static final ResourceKey<PlacedFeature> SCARLET_POND_PLAINS_PLACED_KEY = registerKey(
                        "scarlet_pond_plains_placed");
        public static final ResourceKey<PlacedFeature> SCARLET_POND_FOREST_PLACED_KEY = registerKey(
                        "scarlet_pond_forest_placed");

        public static final ResourceKey<PlacedFeature> ELDERWOODS_DRIPSTONE_CLUSTER_PLACED_KEY = registerKey(
                        "elderwoods_dripstone_cluster_placed");
        public static final ResourceKey<PlacedFeature> SCARLET_DRIPSTONE_CLUSTER_PLACED_KEY = registerKey(
                        "scarlet_dripstone_cluster_placed");

        // here will be the Features be defined and later turned into json files
        public static void bootstrap(BootstrapContext<PlacedFeature> context) {
                var configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

                Holder.Reference<ConfiguredFeature<?, ?>> configuredGeode = context
                                .lookup(Registries.CONFIGURED_FEATURE)
                                .getOrThrow(ModConfiguredFeatures.OVERWORLD_MANA_GEODE_KEY);
                List geodeModifier = List.of(
                                RarityFilter.onAverageOnceEvery(48), // 1 in 24 chunks (like a normal geode)
                                InSquarePlacement.spread(), // "in_square" (scatters the attempts within the chunk area)
                                HeightRangePlacement.uniform(aboveBottom(6), absolute(30)) // min and max heights where
                                                                                           // the geode will
                                                                                           // generate
                );

                register(context, OVERWORLD_MANA_GEODE_PLACED_KEY, configuredGeode, geodeModifier);

                var configuredElderTree = configuredFeatures.getOrThrow(ModConfiguredFeatures.ELDER_TREE_KEY);
                List<PlacementModifier> elderTreeModifiers = List.of(
                                CountPlacement.of(2),
                                InSquarePlacement.spread(),
                                HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES));
                register(context, ELDER_TREE_PLACED_KEY, configuredElderTree, elderTreeModifiers);

                // Scarlet tree - sparser than elder trees
                var configuredScarletTree = configuredFeatures.getOrThrow(ModConfiguredFeatures.SCARLET_TREE_KEY);
                List<PlacementModifier> scarletTreeModifiers = List.of(
                                CountPlacement.of(1), // Sparser than elder trees
                                InSquarePlacement.spread(),
                                HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES));
                register(context, SCARLET_TREE_PLACED_KEY, configuredScarletTree, scarletTreeModifiers);

                // Scarlet Boulder (Plains)
                var configuredScarletBoulder = configuredFeatures
                                .getOrThrow(ModConfiguredFeatures.SCARLET_BOULDER_KEY);
                List<PlacementModifier> scarletBoulderModifiers = List.of(
                                CountPlacement.of(1),
                                RarityFilter.onAverageOnceEvery(60), // Rare generation
                                InSquarePlacement.spread(),
                                HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR_WG)); // Land on ground/under
                                                                                                 // water
                register(context, SCARLET_BOULDER_PLACED_KEY, configuredScarletBoulder, scarletBoulderModifiers);

                // Scarlet Vegetation Placements

                // Grass Patch - very common
                var configuredScarletGrass = configuredFeatures
                                .getOrThrow(ModConfiguredFeatures.SCARLET_GRASS_PATCH_KEY);
                register(context, SCARLET_GRASS_PATCH_PLACED_KEY, configuredScarletGrass, List.of(
                                CountPlacement.of(16),
                                InSquarePlacement.spread(),
                                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                                BiomeFilter.biome()));

                // Tooth Leaves Patch - scattered
                var configuredScarletToothLeaves = configuredFeatures
                                .getOrThrow(ModConfiguredFeatures.SCARLET_TOOTH_LEAVES_PATCH_KEY);
                register(context, SCARLET_TOOTH_LEAVES_PATCH_PLACED_KEY, configuredScarletToothLeaves, List.of(
                                RarityFilter.onAverageOnceEvery(4),
                                InSquarePlacement.spread(),
                                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                                BiomeFilter.biome()));

                // Scarlet Lily Patch - only on water
                // Note: The configured feature uses random patch, which tries to place on
                // surface.
                // We need to ensure it only places if there is water.
                // The BlockPredicate in configured feature should handle "only when empty" but
                // for lilies we want "only when water".
                // Actually, the block itself (ScarletLilyBlock) checks for water in canSurvive.
                // But we need to place it ON the water surface.

                var configuredScarletLily = configuredFeatures.getOrThrow(ModConfiguredFeatures.SCARLET_LILY_PATCH_KEY);
                register(context, SCARLET_LILY_PATCH_PLACED_KEY, configuredScarletLily, List.of(
                                CountPlacement.of(2), // 2 attempts per chunk
                                InSquarePlacement.spread(),
                                // Place on water surface (WORLD_SURFACE_WG gives the highest non-air block,
                                // which includes water)
                                // But we want to place it *in* the water block (top of water).
                                // HEIGHTMAP_WORLD_SURFACE gives the top of water if fluid is present.
                                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                                BiomeFilter.biome()));

                // Scarlet Ponds
                var configuredScarletPondPlains = configuredFeatures
                                .getOrThrow(ModConfiguredFeatures.SCARLET_POND_PLAINS_KEY);
                register(context, SCARLET_POND_PLAINS_PLACED_KEY, configuredScarletPondPlains, List.of(
                                RarityFilter.onAverageOnceEvery(12), // Reduced frequency (was 2)
                                InSquarePlacement.spread(),
                                HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR_WG), // Use OCEAN_FLOOR to
                                                                                                // avoid trees
                                BiomeFilter.biome()));

                var configuredScarletPondForest = configuredFeatures
                                .getOrThrow(ModConfiguredFeatures.SCARLET_POND_FOREST_KEY);
                register(context, SCARLET_POND_FOREST_PLACED_KEY, configuredScarletPondForest, List.of(
                                RarityFilter.onAverageOnceEvery(6), // Reduced frequency (was 1)
                                InSquarePlacement.spread(),
                                HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR_WG), // Use OCEAN_FLOOR to
                                                                                                // avoid trees
                                BiomeFilter.biome()));

                // Dripstone Clusters (underground)
                var configuredElderwoodsDripstone = configuredFeatures
                                .getOrThrow(ModConfiguredFeatures.ELDERWOODS_DRIPSTONE_CLUSTER_KEY);
                register(context, ELDERWOODS_DRIPSTONE_CLUSTER_PLACED_KEY, configuredElderwoodsDripstone, List.of(
                                CountPlacement.of(4), // Reduced from 16 (rarer)
                                InSquarePlacement.spread(),
                                HeightRangePlacement.uniform(aboveBottom(12), absolute(60)),
                                BiomeFilter.biome()));

                var configuredScarletDripstone = configuredFeatures
                                .getOrThrow(ModConfiguredFeatures.SCARLET_DRIPSTONE_CLUSTER_KEY);
                register(context, SCARLET_DRIPSTONE_CLUSTER_PLACED_KEY, configuredScarletDripstone, List.of(
                                CountPlacement.of(4), // Reduced from 20 (rarer)
                                InSquarePlacement.spread(),
                                HeightRangePlacement.uniform(aboveBottom(12), absolute(60)),
                                BiomeFilter.biome()));

        }

        private static ResourceKey<PlacedFeature> registerKey(String name) {
                return ResourceKey.create(Registries.PLACED_FEATURE,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
        }

        private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
                        Holder<ConfiguredFeature<?, ?>> configuration, List<PlacementModifier> modifiers) {
                context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
        }
}