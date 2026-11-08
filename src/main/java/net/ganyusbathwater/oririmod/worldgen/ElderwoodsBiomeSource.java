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

    // Elder Highlands biomes
    public static final ResourceKey<Biome> ELDER_HIGHLANDS_MEADOW_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highlands_meadow"));
    public static final ResourceKey<Biome> ELDER_HIGHLAND_FOREST_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highland_forest"));
    public static final ResourceKey<Biome> ELDER_HIGHLAND_SUMMIT_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highland_summit"));

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
    private double highlandOffsetX = 0;
    private double highlandOffsetZ = 0;
    private double ridgeOffsetX = 0;
    private double ridgeOffsetZ = 0;
    
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
            highlandOffsetX = random.nextDouble() * 10000.0;
            highlandOffsetZ = random.nextDouble() * 10000.0;
            ridgeOffsetX = random.nextDouble() * 10000.0;
            ridgeOffsetZ = random.nextDouble() * 10000.0;
            
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
    
    public double getHumOffsetX() {
        return humOffsetX;
    }
    
    public double getHumOffsetZ() {
        return humOffsetZ;
    }
    
    public double getTempOffsetX() {
        return tempOffsetX;
    }
    
    public double getTempOffsetZ() {
        return tempOffsetZ;
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
     * Evaluates the biome precisely at the given 1x1x1 block coordinate.
     * Prevents the 4x4 chunky block rendering caused by getNoiseBiome.
     */
    public Holder<Biome> getBlockBiome(int x, int y, int z) {
        if (!seedInitialized) {
            syncSeed(net.ganyusbathwater.oririmod.OririMod.globalWorldSeed);
        }

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
     * Enforces realistic climatic zonation and strict concentric biome containment:
     * Golden Desert (Hot) -> Lowlands (Temperate) -> Highland Meadow (Cool) -> Highland Forest (Cold) -> Highland Summit (Freezing Alpine Crest).
     */
    public Holder<Biome> getSurfaceBiome(int x, int z) {
        double tempNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((x + tempOffsetX) * 0.002), 0f, (float)((z + tempOffsetZ) * 0.002), 3);
        double humNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((x + humOffsetX) * 0.002), 0f, (float)((z + humOffsetZ) * 0.002), 3);

        // 1. Hot Climate: Golden Desert (T > 0.15)
        if (tempNoise > 0.15) {
            return findBiome(GOLDEN_DESERT_KEY);
        }

        // 2. Elder Highlands Continental System (strictly Cold/Temperate: T < -0.02, away from desert buffer)
        // Broad scale (0.0004) for massive mountain ranges
        double rawHighland = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((x + highlandOffsetX) * 0.0004f), 0f, (float)((z + highlandOffsetZ) * 0.0004f), 3);
        double highlandNoise = rawHighland / 0.32; // Normalized roughly to [-1.0, 1.0]

        // Highlands only generate in cold/dry-to-temperate regions (T < -0.02 and H < 0.28)
        // This guarantees an expansive lowland buffer (Elderwoods) between Desert and Highlands!
        if (tempNoise < -0.02 && humNoise < 0.28 && highlandNoise > 0.05) {
            // Multi-octave alpine ridge noise for razor-sharp arêtes and aiguilles
            double r1 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float)((x + ridgeOffsetX) * 0.0015f), 0f, (float)((z + ridgeOffsetZ) * 0.0015f), 2));
            double r2 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float)((x + ridgeOffsetX + 333) * 0.003f), 0f, (float)((z + ridgeOffsetZ + 333) * 0.003f), 2));
            double ridgeNoise = r1 * 0.72 + r2 * 0.28;

            // Concentric Containment:
            // - Summit peaks: ONLY in deep cold core (highlandNoise > 0.55, T < -0.10) AND along sharp ridge crests (ridgeNoise > 0.68)
            //   This makes summit peaks narrow, dramatic mountain crests instead of flat horizontal plateaus!
            if (highlandNoise > 0.55 && ridgeNoise > 0.68 && tempNoise < -0.10) {
                return findBiome(ELDER_HIGHLAND_SUMMIT_KEY);
            } else if (highlandNoise > 0.26) {
                // Mountain flanks and pine forest belt
                return findBiome(ELDER_HIGHLAND_FOREST_KEY);
            } else {
                // Outer foothills and meadows surrounding the entire mountain system
                return findBiome(ELDER_HIGHLANDS_MEADOW_KEY);
            }
        }

        // 3. Lowland Woodland Biomes (Humidity controlled)
        if (humNoise > 0.30) {
            return findBiome(SCARLET_SWAMP_KEY); // Wet
        } else if (humNoise > 0.18) {
            return findBiome(SCARLET_FOREST_KEY); // Moderate
        } else {
            return findBiome(ELDERWOODS_KEY); // Lowland ancient forest buffer
        }
    }

    /**
     * Unified Terrain Shaping: Uses domain warping, multi-octave alpine fractals, and continuous climate blending.
     * Eliminates artificial terracing steps and ensures mountains never form vertical cliffs into deserts.
     */
    public int computeSurfaceHeight(int x, int z, Holder<Biome> surfaceBiome) {
        // Sample MultiNoise for Biome Placement
        double tempNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((x + tempOffsetX) * 0.002), 0f, (float)((z + tempOffsetZ) * 0.002), 3);
        double humNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((x + humOffsetX) * 0.002), 0f, (float)((z + humOffsetZ) * 0.002), 3);

        // Blending weights centered on Biome thresholds
        // Desert threshold is tempNoise = 0.15. We blend from 0.05 to 0.25
        double desertWeight = net.minecraft.util.Mth.clamp((tempNoise - 0.05) / 0.20, 0.0, 1.0);
        // Swamp threshold is humNoise = 0.3. We blend from 0.2 to 0.40
        double swampWeight = net.minecraft.util.Mth.clamp((humNoise - 0.2) / 0.20, 0.0, 1.0);

        // Domain Warping for organic, non-symmetrical mountain and hill contours (breaks all stair terraces)
        double warpX = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((x + seedOffsetX) * 0.005f), 0f, (float)((z + seedOffsetZ) * 0.005f), 2) * 38.0;
        double warpZ = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((x + seedOffsetZ + 317) * 0.005f), 0f, (float)((z + seedOffsetX + 317) * 0.005f), 2) * 38.0;
        double wx = x + warpX;
        double wz = z + warpZ;

        // 1. Base Rolling Hills (Elderwoods / Scarlet Forest)
        double nx = (wx + seedOffsetX) * 0.003;
        double nz = (wz + seedOffsetZ) * 0.003;
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
        double desertHeight = (terrainHeight * 0.4) + duneNoise;

        // Blend Desert over base terrain
        double finalNoise = net.minecraft.util.Mth.lerp(desertWeight, terrainHeight, desertHeight);

        // 4. Elder Highlands Continental Mountain System (Massive ranges Y=80 to Y=210)
        // Climate gating: smoothly fades to 0 as temperature approaches desert or humidity approaches swamp
        double tempFade = net.minecraft.util.Mth.clamp((-tempNoise - 0.02) / 0.14, 0.0, 1.0);
        double humFade = net.minecraft.util.Mth.clamp((0.28 - humNoise) / 0.14, 0.0, 1.0);
        double climateSupport = tempFade * humFade;

        double rawHighland = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float)((wx + highlandOffsetX) * 0.0004f), 0f, (float)((wz + highlandOffsetZ) * 0.0004f), 3);
        double highlandNoise = rawHighland / 0.32;

        // Foothill ramp starts at highlandNoise = -0.02 so that by the meadow border (0.05),
        // the terrain is already elevated by +10 to +14 blocks above normal Elderwoods
        double highlandWeight = net.minecraft.util.Mth.clamp((highlandNoise - (-0.02)) / 0.18, 0.0, 1.0);

        double effectiveHighlandWeight = highlandWeight * climateSupport;

        if (effectiveHighlandWeight > 0.0) {
            // Smoothstep curve for seamless foothills transition
            double smoothT = effectiveHighlandWeight * effectiveHighlandWeight * (3.0 - 2.0 * effectiveHighlandWeight);

            // A. Base Elevation Lift: elevates Highlands Meadows noticeably above Elderwoods baseline (Y=78..90)
            double baseLift = 16.0 + smoothT * 14.0;

            // B. Asymmetrical Alpine Slopes & Glacial Valleys (eliminates terrace rings)
            double alpineSlope = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float)((wx + seedOffsetX) * 0.0025f), 0f, (float)((wz + seedOffsetZ) * 0.0025f), 3) * 26.0;
            double cragDetail = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float)(x * 0.015f), 0f, (float)(z * 0.015f), 2) * 7.0;
            double microRoughness = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float)(x * 0.04f), 0f, (float)(z * 0.04f), 2) * 3.0;

            // C. Mountain Flank / Pine Forest Lift (Y=115..155)
            double forestFactor = net.minecraft.util.Mth.clamp((highlandNoise - 0.24) / 0.28, 0.0, 1.0);
            double forestLift = forestFactor * 36.0;

            // D. Alpine Ridged Peak Formation (narrow razor-sharp crests thrusting to Y=180..215)
            double r1 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float)((wx + ridgeOffsetX) * 0.0015f), 0f, (float)((wz + ridgeOffsetZ) * 0.0015f), 2));
            double r2 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float)((wx + ridgeOffsetX + 333) * 0.003f), 0f, (float)((wz + ridgeOffsetZ + 333) * 0.003f), 2));
            double ridgeVal = Math.max(0.0, (r1 * 0.72 + r2 * 0.28) - 0.32) / 0.68;
            // Cubic steepness concentrates elevation tightly along the razor crest instead of a wide plateau
            double ridgePeak = Math.pow(ridgeVal, 3.0);

            double summitFactor = net.minecraft.util.Mth.clamp((highlandNoise - 0.50) / 0.26, 0.0, 1.0);
            double peakLift = ridgePeak * 88.0 * summitFactor;

            double totalHighlandBonus = (baseLift + alpineSlope + cragDetail + microRoughness + forestLift + peakLift) * smoothT;
            finalNoise = net.minecraft.util.Mth.lerp(smoothT, finalNoise, terrainHeight + totalHighlandBonus);
        }

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
