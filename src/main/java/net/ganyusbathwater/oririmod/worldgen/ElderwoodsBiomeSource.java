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
    private static final ResourceKey<Biome> ELYSIAN_ABYSS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elysian_abyss"));

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

    public boolean isSeedInitialized() {
        return seedInitialized;
    }

    public double getSeedOffsetX() {
        return seedOffsetX;
    }

    public double getSeedOffsetZ() {
        return seedOffsetZ;
    }

    public double getSeedOffsetCave() {
        return seedOffsetCave;
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
        if (!seedInitialized) {
            net.minecraft.world.level.biome.Climate.TargetPoint sample = sampler.sample(0, 0, 0);
            long derivedSeed = 1L 
                    ^ (Double.doubleToRawLongBits(sample.humidity()) * 6364136223846793005L)
                    ^ (Double.doubleToRawLongBits(sample.continentalness()) * 1442695040888963407L);
            initSeed(derivedSeed);
        }

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
        float caveScale = 0.004f;
        double caveNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((x + seedOffsetCave) * caveScale),
                0f,
                (float)((z + seedOffsetCave) * caveScale),
                3
        );

        // Surface height (same as ElderwoodsChunkGenerator.getSurfaceHeight)
        int surfaceY = computeSurfaceHeight(x, z);

        // Surface biomes only apply within 16 blocks of the surface to prevent cave features (like geodes) from breaking through
        boolean isBelowSurfaceLayer = y < surfaceY - 16;

        boolean isScarletSurface = (surfaceNoise > 0.5) || (surfaceNoise < -0.5);

        // Find the biome holder from our biome list
        if (isBelowSurfaceLayer) {
            // Cave biomes
            if (isScarletSurface) {
                return findBiome(SCARLET_CAVES_KEY);
            }
            if (caveNoise > 0.20) {
                // Elysian Abyss carved zone — noise band synchronized with generator (> 0.20)
                return findBiome(ELYSIAN_ABYSS_KEY);
            }
            else if (caveNoise < -0.3) {
                return findBiome(ELDERWOODS_CAVE_KEY);
            } else if (caveNoise < -0.15 && caveNoise > -0.2) {
                return findBiome(CRYSTAL_CAVES_KEY);
            } else {
                return findBiome(ELDERWOODS_CAVE_KEY);
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
        // Synchronized with ElderwoodsChunkGenerator
        double nx = (x + seedOffsetX) * 0.003;
        double nz = (z + seedOffsetZ) * 0.003;

        double noise = Math.sin(nx) * Math.cos(nz) * 12.0; 
        noise += Math.sin(nx * 0.5 + 2.0) * Math.cos(nz * 0.6 + 1.1) * 6.0;

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
