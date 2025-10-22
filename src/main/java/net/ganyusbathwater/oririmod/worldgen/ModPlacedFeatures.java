package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.Holder;
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
    //How many Trees/Blocks/Ores/Structures will spawn or where they spawn

    public static final ResourceKey<PlacedFeature> OVERWORLD_MANA_GEODE_PLACED_KEY = registerKey("mana_geode_placed");

    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature> ELDER_TREE_PLACED_KEY = registerKey("elder_tree_placed");

    //here will be the Features be defined and later turned into json files
    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        Holder.Reference<ConfiguredFeature<?, ?>> configuredGeode = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(ModConfiguredFeatures.OVERWORLD_MANA_GEODE_KEY);
        List geodeModifier = List.of(
                RarityFilter.onAverageOnceEvery(48),                               // 1 in 24 chunks (like a normal geode)
                InSquarePlacement.spread(),                                               // "in_square" (scatters the attempts within the chunk area)
                HeightRangePlacement.uniform(aboveBottom(6), absolute(30))    // min and max heights where the geode will generate
        );

        register(context, OVERWORLD_MANA_GEODE_PLACED_KEY, configuredGeode, geodeModifier);

        var configuredElderTree = configuredFeatures.getOrThrow(ModConfiguredFeatures.ELDER_TREE_KEY);
        List<PlacementModifier> elderTreeModifiers = List.of(
                CountPlacement.of(2),
                InSquarePlacement.spread(),
                HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)
        );
        register(context, ELDER_TREE_PLACED_KEY, configuredElderTree, elderTreeModifiers);
    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration, List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}