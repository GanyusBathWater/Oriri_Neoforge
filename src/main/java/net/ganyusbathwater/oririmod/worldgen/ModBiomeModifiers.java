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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import java.util.List;

public class ModBiomeModifiers {
        // Which Biome should it spawn in or in which "stage" it should generate

        public static final ResourceKey<BiomeModifier> ADD_MANA_GEODE = registerKey("add_mana_geode");
        public static final ResourceKey<BiomeModifier> ADD_MANA_GEODE_DIMENSION = registerKey(
                        "add_mana_geode_dimension");
        public static final ResourceKey<BiomeModifier> ADD_SEA_URCHIN_FEATURES = registerKey("add_sea_urchin_features");

        public static final ResourceKey<BiomeModifier> ADD_ELDER_TREES = registerKey("add_elder_trees");
        public static final ResourceKey<BiomeModifier> ADD_ELDERWOODS_OVERGROWTH = registerKey(
                        "add_elderwoods_overgrowth");
        public static final ResourceKey<BiomeModifier> ADD_STAR_HERB = registerKey("add_star_herb");
        public static final ResourceKey<BiomeModifier> ADD_JADE_ORE = registerKey("add_jade_ore");
        public static final ResourceKey<BiomeModifier> ADD_DRAGON_IRON_ORE = registerKey("add_dragon_iron_ore");

        // Elysian Abyss
        public static final ResourceKey<BiomeModifier> ADD_ELYSIAN_FEATURES = registerKey("add_elysian_features");
        public static final ResourceKey<BiomeModifier> ADD_ELYSIAN_DECORATION = registerKey("add_elysian_decoration");

        public static final ResourceKey<BiomeModifier> ADD_GLOBAL_AETHER_RIVER = registerKey("add_global_aether_river");

        // Scarlet Forest & Plains
        public static final ResourceKey<BiomeModifier> ADD_SCARLET_FOREST_FEATURES = registerKey(
                        "add_scarlet_forest_features");
        public static final ResourceKey<BiomeModifier> ADD_SCARLET_SWAMP_FEATURES = registerKey(
                        "add_scarlet_swamp_features");

        // Elder Highlands Features
        public static final ResourceKey<BiomeModifier> ADD_HIGHLANDS_FOREST_FEATURES = registerKey(
                        "add_highlands_forest_features");
        public static final ResourceKey<BiomeModifier> ADD_HIGHLANDS_MEADOW_FEATURES = registerKey(
                        "add_highlands_meadow_features");
        public static final ResourceKey<BiomeModifier> ADD_HIGHLANDS_SUMMIT_FEATURES = registerKey(
                        "add_highlands_summit_features");

        // Cave Decorations
        public static final ResourceKey<BiomeModifier> ADD_SCARLET_CAVE_FEATURES = registerKey(
                        "add_scarlet_cave_features");
        public static final ResourceKey<BiomeModifier> ADD_ELDERWOODS_CAVE_FEATURES = registerKey(
                        "add_elderwoods_cave_features");

        // Deep Ocean & Abyssal Features
        public static final ResourceKey<BiomeModifier> ADD_SILENT_OCEAN_FLORA = registerKey(
                        "add_silent_ocean_flora");
        public static final ResourceKey<BiomeModifier> ADD_OCEAN_BENTHIC_FEATURES = registerKey(
                        "add_ocean_benthic_features");
        public static final ResourceKey<BiomeModifier> ADD_BEACH_FEATURES = registerKey(
                        "add_beach_features");

        public static final ResourceKey<BiomeModifier> ADD_GLOWLINGS = registerKey("add_glowlings");

        // Spawns
        public static final ResourceKey<BiomeModifier> ADD_SPAWNS_FIRE_ZOMBIE = registerKey("add_spawns_fire_zombie");
        public static final ResourceKey<BiomeModifier> ADD_SPAWNS_SPORE_ZOMBIE = registerKey("add_spawns_spore_zombie");
        public static final ResourceKey<BiomeModifier> ADD_SPAWNS_SPORE_BLOSSOM = registerKey(
                        "add_spawns_spore_blossom");
        public static final ResourceKey<BiomeModifier> ADD_SPAWNS_SPLINTER_SPIDER = registerKey(
                        "add_spawns_splinter_spider");
        public static final ResourceKey<BiomeModifier> ADD_SPAWNS_LOADED_BLAZE = registerKey("add_spawns_loaded_blaze");

        public static final ResourceKey<BiomeModifier> ADD_ELDERWOODS_FOREST_SPAWNS = registerKey(
                        "add_elderwoods_forest_spawns");
        public static final ResourceKey<BiomeModifier> ADD_SCARLET_SWAMP_SPAWNS = registerKey(
                        "add_scarlet_swamp_spawns");
        public static final ResourceKey<BiomeModifier> ADD_SCARLET_FOREST_SPAWNS = registerKey(
                        "add_scarlet_forest_spawns");
        public static final ResourceKey<BiomeModifier> ADD_CAVE_SPAWNS = registerKey("add_cave_spawns");

        public static final ResourceKey<BiomeModifier> ADD_SPAWNS_FAIRY_COMMON_FORESTS = registerKey(
                        "add_spawns_fairy_common_forests");
        public static final ResourceKey<BiomeModifier> ADD_SPAWNS_FAIRY_FLOWER_FOREST = registerKey(
                        "add_spawns_fairy_flower_forest");
        public static final ResourceKey<BiomeModifier> ADD_SPAWNS_FAIRY_TAIGAS = registerKey("add_spawns_fairy_taigas");

        public static final ResourceKey<BiomeModifier> ADD_SPAWNS_DRYAD_COMMON_FORESTS = registerKey(
                        "add_spawns_dryad_common_forests");
        public static final ResourceKey<BiomeModifier> ADD_SPAWNS_DRYAD_FLOWER_FOREST = registerKey(
                        "add_spawns_dryad_flower_forest");

        public static final ResourceKey<BiomeModifier> ADD_SPAWNS_LAMIA_SWAMPS = registerKey("add_spawns_lamia_swamps");
        public static final ResourceKey<BiomeModifier> ADD_SPAWNS_LAMIA_FORESTS = registerKey(
                        "add_spawns_lamia_forests");
        public static final ResourceKey<BiomeModifier> ADD_SPAWNS_LAMIA_LUSH_CAVES = registerKey(
                        "add_spawns_lamia_lush_caves");
        public static final ResourceKey<BiomeModifier> ADD_SPAWNS_SILENT_NEREID = registerKey(
                        "add_spawns_silent_nereid");
        public static final ResourceKey<BiomeModifier> ADD_SILENT_OCEAN_WILDLIFE = registerKey(
                        "add_silent_ocean_wildlife");
        public static final ResourceKey<BiomeModifier> ADD_SILENT_DEPTHS_WILDLIFE = registerKey(
                        "add_silent_depths_wildlife");

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

                // Scarlet Swamp Features
                var scarletPlainsBiomes = HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_swamp"))));

                // Elder Highlands Forest Features
                var highlandsForestBiomes = HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highland_forest"))));
                context.register(ADD_HIGHLANDS_FOREST_FEATURES,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                highlandsForestBiomes,
                                                HolderSet.direct(
                                                                placedFeatures.getOrThrow(ModPlacedFeatures.ELDER_PINE_DENSE_PLACED_KEY),
                                                                placedFeatures.getOrThrow(ModPlacedFeatures.FALLEN_LOGS_PLACED_KEY),
                                                                placedFeatures.getOrThrow(ModPlacedFeatures.LEAF_PILE_PATCH_PLACED_KEY),
                                                                placedFeatures.getOrThrow(ModPlacedFeatures.HIGHLANDS_GRASS_PATCH_PLACED_KEY)),
                                                GenerationStep.Decoration.VEGETAL_DECORATION));

                // Elder Highlands Meadow Features
                var highlandsMeadowBiomes = HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highlands_meadow"))));
                context.register(ADD_HIGHLANDS_MEADOW_FEATURES,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                highlandsMeadowBiomes,
                                                HolderSet.direct(
                                                                placedFeatures.getOrThrow(ModPlacedFeatures.HIGHLANDS_BOULDER_PLACED_KEY),
                                                                placedFeatures.getOrThrow(ModPlacedFeatures.ELDER_PINE_SPARSE_PLACED_KEY),
                                                                placedFeatures.getOrThrow(ModPlacedFeatures.HIGHLANDS_MEADOW_FLOWERS_PLACED_KEY),
                                                                placedFeatures.getOrThrow(ModPlacedFeatures.HIGHLANDS_GRASS_PATCH_PLACED_KEY)),
                                                GenerationStep.Decoration.VEGETAL_DECORATION));

                // Elder Highland Summit Features
                var highlandsSummitBiomes = HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highland_summit"))));
                context.register(ADD_HIGHLANDS_SUMMIT_FEATURES,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                highlandsSummitBiomes,
                                                HolderSet.direct(
                                                                placedFeatures.getOrThrow(ModPlacedFeatures.HIGHLAND_SUMMIT_POWDER_SNOW_PLACED_KEY)),
                                                GenerationStep.Decoration.TOP_LAYER_MODIFICATION));
                context.register(ADD_SCARLET_SWAMP_FEATURES,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                scarletPlainsBiomes,
                                                HolderSet.direct(
                                                                placedFeatures.getOrThrow(
                                                                                ModPlacedFeatures.SCARLET_TREE_PLACED_KEY),
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

                var nonScarletCaveBiomes = HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                                "elysian_abyss"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                                "scarlet_forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                                "scarlet_swamp"))),
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

                // Photic Marine Biomes (Shared Flora & Coral Reefs: the_silent_oceans + elder_beach)
                var photicMarineBiomes = HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_silent_oceans"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_beach"))));

                // 1. Aquatic Flora (Seagrass meadows, Kelp forests, Sea Pickles) across both Silent Oceans and Beaches
                context.register(ADD_SILENT_OCEAN_FLORA,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                photicMarineBiomes,
                                                HolderSet.direct(placedFeatures
                                                                .getOrThrow(ModPlacedFeatures.SILENT_OCEAN_FLORA_PLACED_KEY)),
                                                GenerationStep.Decoration.VEGETAL_DECORATION));

                // Deep Ocean & Abyssal Floor Features (Strictly restricted to Silent Depths & Sunken Trenches - ZERO in Silent Oceans)
                var oceanBiomes = HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_silent_depths"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME,
                                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_sunken_trenches"))));
                context.register(ADD_OCEAN_BENTHIC_FEATURES,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                oceanBiomes,
                                                HolderSet.direct(placedFeatures
                                                                .getOrThrow(ModPlacedFeatures.BENTHIC_OCEAN_PLACED_KEY)),
                                                GenerationStep.Decoration.VEGETAL_DECORATION));

                // 2. Coral Reefs, Stone Spikes & Sea Stacks across both Beaches and Silent Oceans
                context.register(ADD_BEACH_FEATURES,
                                new BiomeModifiers.AddFeaturesBiomeModifier(
                                                photicMarineBiomes,
                                                HolderSet.direct(
                                                                placedFeatures.getOrThrow(ModPlacedFeatures.BEACH_UNDERWATER_REEF_PLACED_KEY)),
                                                GenerationStep.Decoration.VEGETAL_DECORATION));

                // Custom Mob Spawns
                context.register(ADD_SPAWNS_FIRE_ZOMBIE, new BiomeModifiers.AddSpawnsBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_NETHER),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.FIRE_ZOMBIE.get(), 50, 1, 4))
                ));
                context.register(registerKey("add_spawns_fire_zombie_overworld"), new BiomeModifiers.AddSpawnsBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.FIRE_ZOMBIE.get(), 50, 1, 4))
                ));
                
                context.register(ADD_SPAWNS_SPORE_ZOMBIE, new BiomeModifiers.AddSpawnsBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_JUNGLE),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.SPORE_ZOMBIE.get(), 80, 2, 4))
                ));
                context.register(registerKey("add_spawns_spore_zombie_lush_caves"), new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("lush_caves")))),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.SPORE_ZOMBIE.get(), 80, 2, 4))
                ));
                
                context.register(ADD_SPAWNS_SPORE_BLOSSOM, new BiomeModifiers.AddSpawnsBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_JUNGLE),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.SPORE_BLOSSOM.get(), 20, 1, 2))
                ));
                context.register(registerKey("add_spawns_spore_blossom_lush_caves"), new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("lush_caves")))),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.SPORE_BLOSSOM.get(), 20, 1, 2))
                ));
                
                context.register(ADD_SPAWNS_SPLINTER_SPIDER, new BiomeModifiers.AddSpawnsBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.SPLINTER_SPIDER.get(), 20, 4, 4))
                ));
                
                context.register(ADD_SPAWNS_LOADED_BLAZE, new BiomeModifiers.AddSpawnsBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_NETHER),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.LOADED_BLAZE.get(), 5, 1, 1))
                ));
                context.register(registerKey("add_spawns_loaded_blaze_elysian"), new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elysian_abyss")))),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.LOADED_BLAZE.get(), 5, 1, 1))
                ));


                // Dimension Vanilla Spawns
                List<MobSpawnSettings.SpawnerData> forestSpawns = List.of(
                        new MobSpawnSettings.SpawnerData(EntityType.SPIDER, 100, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.ZOMBIE, 95, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.ZOMBIE_VILLAGER, 5, 1, 1),
                        new MobSpawnSettings.SpawnerData(EntityType.SKELETON, 100, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.CREEPER, 100, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.SLIME, 100, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.ENDERMAN, 10, 1, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.WITCH, 5, 1, 1),
                        new MobSpawnSettings.SpawnerData(EntityType.SHEEP, 12, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.PIG, 10, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.CHICKEN, 10, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.COW, 8, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.BAT, 10, 8, 8)
                );
                context.register(ADD_ELDERWOODS_FOREST_SPAWNS, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods")))),
                        forestSpawns
                ));
                
                List<MobSpawnSettings.SpawnerData> monsterSpawns = List.of(
                        new MobSpawnSettings.SpawnerData(EntityType.SPIDER, 100, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.ZOMBIE, 95, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.ZOMBIE_VILLAGER, 5, 1, 1),
                        new MobSpawnSettings.SpawnerData(EntityType.SKELETON, 100, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.CREEPER, 100, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.SLIME, 100, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.ENDERMAN, 10, 1, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.WITCH, 5, 1, 1)
                );
                context.register(ADD_SCARLET_SWAMP_SPAWNS, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_swamp")))),
                        monsterSpawns
                ));
                context.register(ADD_SCARLET_FOREST_SPAWNS, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_forest")))),
                        monsterSpawns
                ));

                List<MobSpawnSettings.SpawnerData> caveSpawns = List.of(
                        new MobSpawnSettings.SpawnerData(EntityType.SPIDER, 100, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.ZOMBIE, 95, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.ZOMBIE_VILLAGER, 5, 1, 1),
                        new MobSpawnSettings.SpawnerData(EntityType.SKELETON, 100, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.CREEPER, 100, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.SLIME, 100, 4, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.ENDERMAN, 10, 1, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.WITCH, 5, 1, 1),
                        new MobSpawnSettings.SpawnerData(EntityType.BAT, 10, 8, 8)
                );
                context.register(ADD_CAVE_SPAWNS, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods_cave"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "crystal_caves"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_caves"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elysian_abyss")))
                        ),
                        caveSpawns
                ));

                context.register(ADD_SPAWNS_FAIRY_COMMON_FORESTS, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("birch_forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("old_growth_birch_forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("dark_forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("windswept_forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("cherry_grove")))
                        ),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.FAIRY.get(), 3, 1, 3))
                ));

                context.register(ADD_SPAWNS_FAIRY_FLOWER_FOREST, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("flower_forest")))
                        ),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.FAIRY.get(), 5, 1, 4))
                ));

                context.register(ADD_SPAWNS_FAIRY_TAIGAS, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("taiga"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("old_growth_pine_taiga"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("old_growth_spruce_taiga"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("grove")))
                        ),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.FAIRY.get(), 1, 1, 2))
                ));

                context.register(ADD_SPAWNS_DRYAD_COMMON_FORESTS, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("birch_forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("old_growth_birch_forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("dark_forest")))
                        ),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.DRYAD.get(), 15, 1, 2))
                ));

                context.register(ADD_SPAWNS_DRYAD_FLOWER_FOREST, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("flower_forest")))
                        ),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.DRYAD.get(), 25, 1, 3))
                ));

                // Lamia Spawns: Swampy / moist environments
                context.register(ADD_SPAWNS_LAMIA_SWAMPS, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("swamp"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("mangrove_swamp"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_swamp")))
                        ),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.LAMIA.get(), 25, 1, 2))
                ));

                // Lamia Spawns: Warm/temperate forests (excluding cold taigas)
                context.register(ADD_SPAWNS_LAMIA_FORESTS, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("flower_forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("birch_forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("old_growth_birch_forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("dark_forest"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("jungle"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("sparse_jungle"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_forest")))
                        ),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.LAMIA.get(), 15, 1, 2))
                ));

                // Lamia Spawns: Lush Caves
                context.register(ADD_SPAWNS_LAMIA_LUSH_CAVES, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("lush_caves")))
                        ),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.LAMIA.get(), 35, 1, 3))
                ));

                // Silent Nereid Spawns: The Silent Depths & The Sunken Trenches
                context.register(ADD_SPAWNS_SILENT_NEREID, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_silent_depths"))),
                                biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_sunken_trenches")))
                        ),
                        List.of(new MobSpawnSettings.SpawnerData(ModEntities.SILENT_NEREID.get(), 1, 1, 1))
                ));

                // Silent Ocean Underwater Wildlife Spawns
                List<MobSpawnSettings.SpawnerData> oceanWildlifeSpawns = List.of(
                        new MobSpawnSettings.SpawnerData(EntityType.COD, 15, 3, 6),
                        new MobSpawnSettings.SpawnerData(EntityType.SALMON, 15, 1, 5),
                        new MobSpawnSettings.SpawnerData(EntityType.TROPICAL_FISH, 25, 4, 8),
                        new MobSpawnSettings.SpawnerData(EntityType.PUFFERFISH, 10, 1, 3),
                        new MobSpawnSettings.SpawnerData(EntityType.SQUID, 20, 1, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.GLOW_SQUID, 10, 2, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.DOLPHIN, 6, 1, 2),
                        new MobSpawnSettings.SpawnerData(EntityType.TURTLE, 8, 2, 5),
                        new MobSpawnSettings.SpawnerData(EntityType.DROWNED, 5, 1, 1)
                );
                context.register(ADD_SILENT_OCEAN_WILDLIFE, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_silent_oceans")))),
                        oceanWildlifeSpawns
                ));

                // Abyssal Wildlife Spawns disabled (Both Silent Depths and Sunken Trenches are strictly exclusive to Silent Nereids)
                context.register(ADD_SILENT_DEPTHS_WILDLIFE, new BiomeModifiers.AddSpawnsBiomeModifier(
                        HolderSet.direct(),
                        List.of()
                ));
        }

        private static ResourceKey<BiomeModifier> registerKey(String name) {
                return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
        }
}
