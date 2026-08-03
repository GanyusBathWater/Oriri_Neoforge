package net.ganyusbathwater.oririmod.worldgen.dimension;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

import java.util.Collections;
import java.util.Optional;

public class ModDimensions {

    public static final ResourceKey<LevelStem> VOLCANO_STEM = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "dungeon_volcano"));
    public static final ResourceKey<LevelStem> MOUNTAIN_STEM = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "dungeon_mountain"));
    public static final ResourceKey<LevelStem> UNDERWATER_STEM = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "dungeon_underwater"));

    public static void bootstrap(BootstrapContext<LevelStem> context) {
        HolderGetter<Biome> biomeRegistry = context.lookup(Registries.BIOME);
        HolderGetter<DimensionType> dimTypes = context.lookup(Registries.DIMENSION_TYPE);

        // We use the 'the_void' biome for our void dimension. It is purely empty.
        FlatLevelGeneratorSettings flatSettings = new FlatLevelGeneratorSettings(
                Optional.empty(),
                biomeRegistry.getOrThrow(Biomes.THE_VOID),
                Collections.emptyList()
        );

        FlatLevelSource voidSource = new FlatLevelSource(flatSettings);

        context.register(VOLCANO_STEM, new LevelStem(dimTypes.getOrThrow(ModDimensionTypes.DUNGEON), voidSource));
        context.register(MOUNTAIN_STEM, new LevelStem(dimTypes.getOrThrow(ModDimensionTypes.DUNGEON), voidSource));
        context.register(UNDERWATER_STEM, new LevelStem(dimTypes.getOrThrow(ModDimensionTypes.DUNGEON), voidSource));
    }
}
