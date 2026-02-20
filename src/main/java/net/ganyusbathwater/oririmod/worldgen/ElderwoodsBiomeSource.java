package net.ganyusbathwater.oririmod.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * Custom BiomeSource for the Elderwoods dimension.
 * Implements the same noise logic as ElderwoodsChunkGenerator.getComputedBiome
 * so that /locate biome works correctly.
 */
public class ElderwoodsBiomeSource extends BiomeSource {

    public static final MapCodec<ElderwoodsBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.CODEC.listOf().fieldOf("biomes").forGetter(src -> src.biomes))
            .apply(instance, ElderwoodsBiomeSource::new));

    // Biome resource keys (same as in ElderwoodsChunkGenerator)
    private static final ResourceKey<Biome> ELDERWOODS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods"));
    private static final ResourceKey<Biome> SCARLET_PLAINS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_plains"));
    private static final ResourceKey<Biome> SCARLET_FOREST_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_forest"));
    private static final ResourceKey<Biome> SCARLET_CAVES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_caves"));
    private static final ResourceKey<Biome> CRYSTAL_CAVES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "crystal_caves"));
    private static final ResourceKey<Biome> ELDERWOODS_CAVE_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods_cave"));

    private final java.util.List<Holder<Biome>> biomes;

    // Seed offsets - injected by the chunk generator when it learns the world seed
    private double seedOffsetX = 0;
    private double seedOffsetZ = 0;
    private double seedOffsetCave = 0;
    private boolean seedInitialized = false;

    public ElderwoodsBiomeSource(java.util.List<Holder<Biome>> biomes) {
        this.biomes = biomes;
    }

    /**
     * Called by ElderwoodsChunkGenerator to sync seed offsets.
     */
    public void initSeed(long seed) {
        if (!seedInitialized) {
            net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(seed);
            seedOffsetX = random.nextDouble() * 10000.0;
            seedOffsetZ = random.nextDouble() * 10000.0;
            seedOffsetCave = random.nextDouble() * 10000.0;
            seedInitialized = true;
        }
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        // Convert quart coordinates to block coordinates
        int x = quartX * 4;
        int y = quartY * 4;
        int z = quartZ * 4;

        // Same noise logic as ElderwoodsChunkGenerator.getComputedBiome
        double nx = x + seedOffsetX;
        double nz = z + seedOffsetZ;

        // Surface Noise
        double surfaceNoise = Math.sin(nx * 0.002) * Math.cos(nz * 0.003) +
                0.5 * Math.cos(nx * 0.005 + 2.0) * Math.sin(nz * 0.005 + 1.0);

        // Cave Noise
        double caveNoise = Math.sin(nx * 0.01 + seedOffsetCave) * Math.cos(nz * 0.01 + seedOffsetCave);

        // Surface height (same as ElderwoodsChunkGenerator.getSurfaceHeight)
        int surfaceY = computeSurfaceHeight(x, z);

        // Surface biomes only apply within 4 blocks of the surface
        boolean isBelowSurfaceLayer = y < surfaceY - 4;

        // Find the biome holder from our biome list
        if (isBelowSurfaceLayer) {
            // Cave biomes
            if (caveNoise > 0.3) {
                return findBiome(SCARLET_CAVES_KEY);
            } else if (caveNoise < -0.3) {
                return findBiome(ELDERWOODS_CAVE_KEY);
            } else {
                return findBiome(CRYSTAL_CAVES_KEY);
            }
        } else {
            // Surface biomes
            if (surfaceNoise > 0.5) {
                return findBiome(SCARLET_FOREST_KEY);
            } else if (surfaceNoise < -0.5) {
                return findBiome(SCARLET_PLAINS_KEY);
            } else {
                return findBiome(ELDERWOODS_KEY);
            }
        }
    }

    /**
     * Same surface height calculation as ElderwoodsChunkGenerator.getSurfaceHeight
     */
    private int computeSurfaceHeight(int x, int z) {
        double nx = x + seedOffsetX;
        double nz = z + seedOffsetZ;

        double noise = 0.0;
        noise += Math.sin(nx * 0.0025) * Math.cos(nz * 0.0025) * 10.0;
        noise += Math.cos(nx * 0.002 + 2.0) * Math.sin(nz * 0.003) * 8.0;
        noise += Math.sin(nx * 0.007 + 20) * Math.cos(nz * 0.006 + 20) * 5.0;
        noise += Math.sin(nx * 0.02) * Math.cos(nz * 0.015) * 2.5;
        noise += Math.sin(nx * 0.05 + 7.0) * Math.cos(nz * 0.04 + 3.0) * 0.8;

        return ElderwoodsChunkGenerator.BASE_HEIGHT + (int) Math.round(noise);
    }

    /**
     * Find a biome holder by resource key from our cached list.
     */
    private Holder<Biome> findBiome(ResourceKey<Biome> key) {
        for (Holder<Biome> holder : biomes) {
            if (holder.is(key)) {
                return holder;
            }
        }
        // Fallback to first biome if not found
        return biomes.get(0);
    }
}
