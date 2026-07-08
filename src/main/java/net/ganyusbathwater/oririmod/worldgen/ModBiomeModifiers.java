package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModBiomeModifiers {
        // Which Biome should it spawn in or in which "stage" it should generate

        public static final ResourceKey<BiomeModifier> ADD_MANA_GEODE = registerKey("add_mana_geode");
        public static final ResourceKey<BiomeModifier> ADD_MANA_GEODE_DIMENSION = registerKey(
                        "add_mana_geode_dimension");
        public static final ResourceKey<BiomeModifier> ADD_SEA_URCHIN_FEATURES = registerKey("add_sea_urchin_features");

        public static final ResourceKey<BiomeModifier> ADD_ELDER_TREES = registerKey("add_elder_trees");
        public static final ResourceKey<BiomeModifier> ADD_ELDERWOODS_OVERGROWTH = registerKey("add_elderwoods_overgrowth");
        public static final ResourceKey<BiomeModifier> ADD_STAR_HERB = registerKey("add_star_herb");
        public static final ResourceKey<BiomeModifier> ADD_JADE_ORE = registerKey("add_jade_ore");
        public static final ResourceKey<BiomeModifier> ADD_DRAGON_IRON_ORE = registerKey("add_dragon_iron_ore");

        // Elysian Abyss
        public static final ResourceKey<BiomeModifier> ADD_ELYSIAN_FEATURES = registerKey("add_elysian_features");
        public static final ResourceKey<BiomeModifier> ADD_ELYSIAN_DECORATION = registerKey("add_elysian_decoration");

        public static final ResourceKey<BiomeModifier> ADD_GLOBAL_AETHER_RIVER = registerKey("add_global_aether_river");
        public static final ResourceKey<BiomeModifier> ADD_ABYSS_RAVINE = registerKey("add_abyss_ravine");

        // Scarlet Forest & Plains
        public static final ResourceKey<BiomeModifier> ADD_SCARLET_FOREST_FEATURES = registerKey(
                        "add_scarlet_forest_features");
        public static final ResourceKey<BiomeModifier> ADD_SCARLET_PLAINS_FEATURES = registerKey(
                        "add_scarlet_plains_features");

        // Cave Decorations
        public static final ResourceKey<BiomeModifier> ADD_SCARLET_CAVE_FEATURES = registerKey(
                        "add_scarlet_cave_features");
        public static final ResourceKey<BiomeModifier> ADD_ELDERWOODS_CAVE_FEATURES = registerKey(
                        "add_elderwoods_cave_features");

        public static final ResourceKey<BiomeModifier> ADD_GLOWLINGS = registerKey("add_glowlings");

        // here will be the Features defined and later turned into json files
        public static void bootstrap(BootstrapContext<BiomeModifier> context) {
                var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
                var biomes = context.lookup(Registries.BIOME);

                context.register(ADD_MANA_GEODE,
                                new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                                                HolderSet.direct(placedFeatures.getOrThrow(
                                                                ModPlacedFeatures.OVERWORLD_MANA_GEODE_PLACED_KEY)),
                                                GenerationStep.Decoration.LOCAL_MODIFICATIONS));

                var elderBiomesTag = TagKey.create(
                                Registries.BIOME,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "has_elder_trees"));
                context.register(
                                ADD_ELDER_TREES,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                biomes.getOrThrow(elderBiomesTag),
                                                HolderSet.direct(
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.ELDER_TREE_PLACED_KEY)),
                                                GenerationStep.Decoration.VEGETAL_DECORATION));

                context.register(
                                ADD_ELDERWOODS_OVERGROWTH,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                biomes.getOrThrow(elderBiomesTag),
                                                HolderSet.direct(
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.FALLEN_LOGS_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.LEAF_PILE_PATCH_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.ELDERWOODS_GRASS_PATCH_PLACED_KEY)),
                                                GenerationStep.Decoration.VEGETAL_DECORATION));

                context.register(
                                ADD_STAR_HERB,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                biomes.getOrThrow(elderBiomesTag),
                                                HolderSet.direct(
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.STAR_HERB_PATCH_PLACED_KEY)),
                                                GenerationStep.Decoration.VEGETAL_DECORATION));

                context.register(
                                ADD_JADE_ORE,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                biomes.getOrThrow(BiomeTags.IS_JUNGLE),
                                                HolderSet.direct(
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.JADE_ORE_PLACED_KEY)),
                                                GenerationStep.Decoration.UNDERGROUND_ORES));

                var dragonIronBiomes = HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                                "crystal_caves"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                                "elderwoods_cave"))));

                context.register(ADD_DRAGON_IRON_ORE,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                dragonIronBiomes,
                                                HolderSet.direct(placedFeatures.getOrThrow(
                                                                ModPlacedFeatures.DRAGON_IRON_ORE_PLACED_KEY)),
                                                GenerationStep.Decoration.UNDERGROUND_ORES));

                // Elysian Abyss — vegetal: trees + mushrooms
                var elysianBiomes = HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elysian_abyss"))));
                context.register(ADD_ELYSIAN_FEATURES,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                elysianBiomes,
                                                HolderSet.direct(
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.ELYSIAN_ABYSS_CROWN_TREE_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.ELYSIAN_STONE_MUSHROOM_PLACED_KEY)),
                                                GenerationStep.Decoration.VEGETAL_DECORATION));

                // Elysian Abyss — underground decoration: moss patches + glow lichen + pillars
                context.register(ADD_ELYSIAN_DECORATION,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                elysianBiomes,
                                                HolderSet.direct(
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.ELYSIAN_FLOOR_MOSS_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.ELYSIAN_LICHEN_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.ELYSIAN_ABYSS_PILLAR_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.ELYSIAN_FLOOR_SPIKE_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.ELYSIAN_CEILING_SPIKE_PLACED_KEY)),
                                                GenerationStep.Decoration.UNDERGROUND_DECORATION));

                // Scarlet Forest Features
                var scarletForestBiomes = HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_forest"))));
                context.register(ADD_SCARLET_FOREST_FEATURES,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                scarletForestBiomes,
                                                HolderSet.direct(
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.SCARLET_POND_FOREST_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.SCARLET_TREE_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.SCARLET_GRASS_PATCH_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.SCARLET_TOOTH_LEAVES_PATCH_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.SCARLET_LILY_PATCH_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.BLOOD_CAP_PLACED_KEY)),
                                                GenerationStep.Decoration.VEGETAL_DECORATION));

                // Scarlet Plains Features
                var scarletPlainsBiomes = HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_plains"))));
                context.register(ADD_SCARLET_PLAINS_FEATURES,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                scarletPlainsBiomes,
                                                HolderSet.direct(
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.SCARLET_POND_PLAINS_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.SCARLET_BOULDER_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.SCARLET_GRASS_PATCH_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.SCARLET_TOOTH_LEAVES_PATCH_PLACED_KEY),
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.SCARLET_LILY_PATCH_PLACED_KEY)),
                                                GenerationStep.Decoration.VEGETAL_DECORATION));

                // Cave Decorations
                var scarletCaveBiomes = HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_caves"))));
                context.register(ADD_SCARLET_CAVE_FEATURES,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                scarletCaveBiomes,
                                                HolderSet.direct(placedFeatures.getOrThrow(
                                                                ModPlacedFeatures.SCARLET_DRIPSTONE_CLUSTER_PLACED_KEY)),
                                                GenerationStep.Decoration.UNDERGROUND_DECORATION));

                var elderwoodsCaveBiomes = HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods_cave"))));
                context.register(ADD_ELDERWOODS_CAVE_FEATURES,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                elderwoodsCaveBiomes,
                                                HolderSet.direct(placedFeatures.getOrThrow(
                                                                ModPlacedFeatures.ELDERWOODS_DRIPSTONE_CLUSTER_PLACED_KEY)),
                                                GenerationStep.Decoration.UNDERGROUND_DECORATION));

                var aetherRiverBiomes = HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                                "elysian_abyss"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                                "elderwoods_cave"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation
                                                .fromNamespaceAndPath(OririMod.MOD_ID, "crystal_caves"))));

                context.register(ADD_GLOBAL_AETHER_RIVER,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                aetherRiverBiomes,
                                                HolderSet.direct(placedFeatures.getOrThrow(
                                                                ModPlacedFeatures.GLOBAL_AETHER_RIVER_PLACED_KEY)),
                                                GenerationStep.Decoration.LOCAL_MODIFICATIONS));

                context.register(ADD_ABYSS_RAVINE,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                elysianBiomes,
                                                HolderSet.direct(placedFeatures
                                                                .getOrThrow(ModPlacedFeatures.ABYSS_RAVINE_PLACED_KEY)),
                                                GenerationStep.Decoration.RAW_GENERATION));

                var nonScarletCaveBiomes = HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                                "elysian_abyss"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                                "scarlet_forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                                "scarlet_plains"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                                "elderwoods_cave"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation
                                                .fromNamespaceAndPath(OririMod.MOD_ID, "crystal_caves"))));

                context.register(ADD_MANA_GEODE_DIMENSION,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                nonScarletCaveBiomes,
                                                HolderSet.direct(placedFeatures.getOrThrow(
                                                                ModPlacedFeatures.OVERWORLD_MANA_GEODE_PLACED_KEY)),
                                                GenerationStep.Decoration.LOCAL_MODIFICATIONS));

                var crystalCaveBiomes = HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "crystal_caves"))));
                context.register(ADD_SEA_URCHIN_FEATURES,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                crystalCaveBiomes,
                                                HolderSet.direct(placedFeatures
                                                                .getOrThrow(ModPlacedFeatures.SEA_URCHIN_PLACED_KEY)),
                                                GenerationStep.Decoration.UNDERGROUND_DECORATION));

                context.register(ADD_GLOWLINGS,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                crystalCaveBiomes,
                                                HolderSet.direct(placedFeatures
                                                                .getOrThrow(ModPlacedFeatures.GLOWLINGS_PLACED_KEY)),
                                                GenerationStep.Decoration.VEGETAL_DECORATION));
        }

        private static ResourceKey<BiomeModifier> registerKey(String name) {
                return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
        }
}
