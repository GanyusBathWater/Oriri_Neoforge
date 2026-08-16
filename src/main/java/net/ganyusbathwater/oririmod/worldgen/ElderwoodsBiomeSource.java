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
    private static final ResourceKey<Biome> SCARLET_SWAMP_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_swamp"));
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
    private static final ResourceKey<Biome> GOLDEN_DESERT_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "golden_desert"));

    private final java.util.List<Holder<Biome>> biomes;

    // Seed offsets - injected by the chunk generator when it learns the world seed
    private double seedOffsetX = 0;
    private double seedOffsetZ = 0;
    private double seedOffsetCave = 0;
    
    // MultiNoise Offsets
    private double tempOffsetX = 0;
    private double tempOffsetZ = 0;
    private double humOffsetX = 0;
    private double humOffsetZ = 0;
    
    private boolean seedInitialized = false;

    public ElderwoodsBiomeSource(java.util.List<Holder<Biome>> biomes) {
        this.biomes = biomes;
    }

    /**
     * Called by ElderwoodsChunkGenerator to perfectly sync the true world seed.
     */
    public void syncSeed(long seed) {
        if (!seedInitialized) {
            net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(seed);
            seedOffsetX = random.nextDouble() * 10000.0;
            seedOffsetZ = random.nextDouble() * 10000.0;
            seedOffsetCave = random.nextDouble() * 10000.0;
            
            tempOffsetX = random.nextDouble() * 10000.0;
            tempOffsetZ = random.nextDouble() * 10000.0;
            humOffsetX = random.nextDouble() * 10000.0;
            humOffsetZ = random.nextDouble() * 10000.0;
            
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
            syncSeed(net.ganyusbathwater.oririmod.OririMod.globalWorldSeed);
        }
        
        // Convert quart coordinates to block coordinates
        int x = quartX * 4;
        int y = quartY * 4;
        int z = quartZ * 4;

        Holder<Biome> surfaceBiome = getSurfaceBiome(x, z);

        // Surface height check for caves
        int surfaceY = computeSurfaceHeight(x, z, surfaceBiome);
        boolean isBelowSurfaceLayer = y < surfaceY - 16;

        if (isBelowSurfaceLayer) {
            // Cave biomes
            float caveScale = 0.004f;
            double caveNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float)((x + seedOffsetCave) * caveScale),
                    0f,
                    (float)((z + seedOffsetCave) * caveScale),
                    3
            );

            // Scarlet Caves cleanly spawn under Scarlet Swamps and Scarlet Forests
            if (surfaceBiome.is(SCARLET_SWAMP_KEY) || surfaceBiome.is(SCARLET_FOREST_KEY)) {
                return findBiome(SCARLET_CAVES_KEY);
            }
            if (caveNoise > 0.20) {
                return findBiome(ELYSIAN_ABYSS_KEY);
            } else if (caveNoise < -0.3) {
                return findBiome(ELDERWOODS_CAVE_KEY);
            } else if (caveNoise < -0.15 && caveNoise > -0.2) {
                return findBiome(CRYSTAL_CAVES_KEY);
            } else {
                return findBiome(ELDERWOODS_CAVE_KEY);
            }
        }

        return surfaceBiome;
    }

    /**
     * Unified 2D MultiNoise map for surface biomes (Temperature/Humidity).
     * Prevents overlapping rules and forces continuous borders.
     */
    public Holder<Biome> getSurfaceBiome(int x, int z) {
        double tempNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((x + tempOffsetX) * 0.002), 0f, (float)((z + tempOffsetZ) * 0.002), 3);
        double humNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((x + humOffsetX) * 0.002), 0f, (float)((z + humOffsetZ) * 0.002), 3);

        // Temp/Hum Grid:
        // Temperature controls Desert vs Everything Else
        // Hot (> 0.15) -> Golden Desert
        // Normal (<= 0.15) -> Check Humidity for Forest/Swamp/Elderwoods
        
        if (tempNoise > 0.15) {
            return findBiome(GOLDEN_DESERT_KEY);
        } else {
            // Humidity controls the woodland biomes
            if (humNoise > 0.1) {
                return findBiome(SCARLET_SWAMP_KEY); // Wet
            } else if (humNoise > -0.1) {
                return findBiome(SCARLET_FOREST_KEY); // Normal
            } else {
                return findBiome(ELDERWOODS_KEY); // Dry
            }
        }
    }

    /**
     * Unified Terrain Shaping: Uses continuous noise weights to blend terrain across biome borders seamlessly.
     */
    public int computeSurfaceHeight(int x, int z, Holder<Biome> surfaceBiome) {
        // Sample the exact same MultiNoise used for Biome Placement
        double tempNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((x + tempOffsetX) * 0.002), 0f, (float)((z + tempOffsetZ) * 0.002), 3);
        double humNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((x + humOffsetX) * 0.002), 0f, (float)((z + humOffsetZ) * 0.002), 3);

        // Calculate continuous blending weights centered exactly on the Biome thresholds!
        // Desert threshold is tempNoise = 0.15. We blend from 0.05 to 0.25
        double desertWeight = net.minecraft.util.Mth.clamp((tempNoise - 0.05) / 0.20, 0.0, 1.0);
        
        // Swamp threshold is humNoise = 0.1. We blend from 0.0 to 0.20
        double swampWeight = net.minecraft.util.Mth.clamp((humNoise - 0.0) / 0.20, 0.0, 1.0);

        // 1. Base Rolling Hills (Elderwoods / Scarlet Forest)
        double nx = (x + seedOffsetX) * 0.003;
        double nz = (z + seedOffsetZ) * 0.003;
        double baseNoise = Math.sin(nx) * Math.cos(nz) * 12.0;
        baseNoise += Math.sin(nx * 0.5 + 2.0) * Math.cos(nz * 0.6 + 1.1) * 6.0;

        // 2. Swamp Basin
        double bumpyNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((x + seedOffsetX) * 0.05), 0f, (float)((z + seedOffsetZ) * 0.05), 2) * 3.0;
        double swampHeight = -3.0 + bumpyNoise;

        // Blend Swamp into Base Hills
        double terrainHeight = net.minecraft.util.Mth.lerp(swampWeight, baseNoise, swampHeight);

        // 3. Golden Desert Dunes
        double duneNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((x + seedOffsetX) * 0.03), 0f, (float)((z + seedOffsetZ) * 0.03), 2) * 5.0;
        
        // Dunes ride on a smoother base terrain
        double desertHeight = (terrainHeight * 0.4) + duneNoise;

        // Blend Desert over everything else
        double finalNoise = net.minecraft.util.Mth.lerp(desertWeight, terrainHeight, desertHeight);

        // --- DYNAMIC SWAMP SHORELINE (SPILL PREVENTION) ---
        // Blood water spawns at Y=61 (BASE_HEIGHT - 2) in the Scarlet Swamp.
        // If an adjacent Desert or Forest dune drops lower than Y=61, the water will form a sheer vertical cliff.
        // We dynamically calculate distance to the exact Swamp border and build a natural retaining ridge!
        
        double distToHumBorder = Math.abs(swampWeight - 0.5) * 2.0; // 0 at Hum border, 1 inside/outside
        double distToTempBorder = Math.abs(desertWeight - 0.5) * 2.0; // 0 at Temp border, 1 inside/outside
        
        double ridgeForce = 0.0;
        if (swampWeight > 0.5) {
            // Inside the Swamp's humidity zone. 
            // The border is either the edge of the humidity zone (Forest) or the edge of the temperature zone (Desert).
            double closestEdge = Math.min(distToHumBorder, distToTempBorder);
            ridgeForce = Math.max(0.0, 1.0 - (closestEdge * closestEdge)); 
        } else {
            // Outside the humidity zone, the only relevant border is the hum border.
            ridgeForce = Math.max(0.0, 1.0 - (distToHumBorder * distToHumBorder));
        }
        
        if (ridgeForce > 0.0) {
            // Force the surface up to at least -1.0 (Y=62) to physically contain the Y=61 water!
            double ridgeMinimum = -1.0; 
            finalNoise = net.minecraft.util.Mth.lerp(ridgeForce, finalNoise, Math.max(finalNoise, ridgeMinimum));
        }

        return ElderwoodsChunkGenerator.BASE_HEIGHT + (int) Math.round(finalNoise);
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
