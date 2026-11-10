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

    // Deep Ocean & Coastal Biomes
    public static final ResourceKey<Biome> ELDER_BEACH_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_beach"));
    public static final ResourceKey<Biome> THE_SILENT_OCEANS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_silent_oceans"));
    public static final ResourceKey<Biome> THE_SILENT_DEPTHS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_silent_depths"));
    public static final ResourceKey<Biome> THE_SUNKEN_TRENCHES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_sunken_trenches"));

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
    private double contOffsetX = 0;
    private double contOffsetZ = 0;
    private double trenchOffsetX = 0;
    private double trenchOffsetZ = 0;
    private double islandOffsetX = 0;
    private double islandOffsetZ = 0;

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
            contOffsetX = random.nextDouble() * 10000.0;
            contOffsetZ = random.nextDouble() * 10000.0;
            trenchOffsetX = random.nextDouble() * 10000.0;
            trenchOffsetZ = random.nextDouble() * 10000.0;
            islandOffsetX = random.nextDouble() * 10000.0;
            islandOffsetZ = random.nextDouble() * 10000.0;

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

    public double getContOffsetX() {
        return contOffsetX;
    }

    public double getContOffsetZ() {
        return contOffsetZ;
    }

    public boolean isMainland(int x, int z) {
        if (!seedInitialized) {
            syncSeed(net.ganyusbathwater.oririmod.OririMod.globalWorldSeed);
        }
        double rawCont = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + contOffsetX) * 0.00035f), 0f, (float) ((z + contOffsetZ) * 0.00035f), 3);
        return (rawCont / 0.32) >= 0.02;
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

        double rawCont = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + contOffsetX) * 0.00035f), 0f, (float) ((z + contOffsetZ) * 0.00035f), 3);
        double contNoise = rawCont / 0.32;

        int surfaceY = computeSurfaceHeight(x, z, surfaceBiome);
        boolean isBelowSurfaceLayer = y < surfaceY - 16;

        if (surfaceY < 58) {
            if (!isBelowSurfaceLayer) {
                return surfaceBiome;
            }
            if (surfaceY <= -112) {
                return findBiome(THE_SUNKEN_TRENCHES_KEY);
            } else if (contNoise < getDynamicAbyssThreshold(x, z) && surfaceY < 35) {
                return findBiome(THE_SILENT_DEPTHS_KEY);
            } else {
                return surfaceBiome;
            }
        }

        if (isBelowSurfaceLayer) {
            // Cave biomes - strictly deep inland continental landmass
            float caveScale = 0.004f;
            double caveNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((x + seedOffsetCave) * caveScale),
                    0f,
                    (float) ((z + seedOffsetCave) * caveScale),
                    3);

            // Scarlet Caves cleanly spawn under Scarlet Swamps and Scarlet Forests strictly on land
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

        double rawCont = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + contOffsetX) * 0.00035f), 0f, (float) ((z + contOffsetZ) * 0.00035f), 3);
        double contNoise = rawCont / 0.32;

        int surfaceY = computeSurfaceHeight(x, z, surfaceBiome);
        boolean isBelowSurfaceLayer = y < surfaceY - 16;

        if (surfaceY < 58) {
            if (!isBelowSurfaceLayer) {
                return surfaceBiome;
            }
            if (surfaceY <= -112) {
                return findBiome(THE_SUNKEN_TRENCHES_KEY);
            } else if (contNoise < getDynamicAbyssThreshold(x, z) && surfaceY < 35) {
                return findBiome(THE_SILENT_DEPTHS_KEY);
            } else {
                return surfaceBiome;
            }
        }

        if (isBelowSurfaceLayer) {
            // Cave biomes - strictly deep inland continental landmass
            float caveScale = 0.004f;
            double caveNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((x + seedOffsetCave) * caveScale),
                    0f,
                    (float) ((z + seedOffsetCave) * caveScale),
                    3);

            // Scarlet Caves cleanly spawn under Scarlet Swamps and Scarlet Forests strictly on land
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
     * Golden Desert (Hot) -> Lowlands (Temperate) -> Highland Meadow (Cool) ->
     * Highland Forest (Cold) -> Highland Summit (Freezing Alpine Crest).
     */
    public Holder<Biome> getSurfaceBiome(int x, int z) {
        // 0. Master Continentalness Axis: Oceanic Domain vs Coastal Beach vs Continental Landmass
        double rawCont = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + contOffsetX) * 0.00035f), 0f, (float) ((z + contOffsetZ) * 0.00035f), 3);
        double contNoise = rawCont / 0.32;
        int surfaceY = computeSurfaceHeight(x, z, null);

        // 1. High Landmass / Island Interiors:
        if (surfaceY >= 74) {
            double tempNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((x + tempOffsetX) * 0.002), 0f, (float) ((z + tempOffsetZ) * 0.002), 3);
            if (contNoise < 0.02) {
                // Island Interior or Coastal Peninsula: Lush Elderwoods or Golden Desert
                return findBiome(tempNoise > 0.15 ? GOLDEN_DESERT_KEY : ELDERWOODS_KEY);
            }
            // Mainland interior biomes handled below
        }

        // 2. Beach Zone: All shorelines, island perimeters, and shallow water bands (Y=58 to 74)
        if (surfaceY >= 58 && surfaceY < 74) {
            return findBiome(ELDER_BEACH_KEY);
        }

        // 3. Oceanic Domain (Y < 58)
        if (surfaceY < 58) {
            double abyssThreshold = getDynamicAbyssThreshold(x, z);
            boolean isAbyssalDomain = contNoise < abyssThreshold;

            if (isAbyssalDomain && surfaceY < 35) {
                // Inside the Deep Abyssal Domain:
                // Valley floor, deep rifts, and all spiked mountains belong to the Silent Depths!
                double crackNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                        (float) ((x + trenchOffsetX) * 0.012f), 0f, (float) ((z + trenchOffsetZ) * 0.012f), 2);
                boolean isCrack = Math.abs(crackNoise) < 0.038;
                if (isCrack && surfaceY <= -112) {
                    return findBiome(THE_SUNKEN_TRENCHES_KEY);
                } else {
                    return findBiome(THE_SILENT_DEPTHS_KEY);
                }
            } else if (surfaceY >= -45) {
                // Photic Ocean Shelf / Upper Continental Slope / Seamounts
                return findBiome(THE_SILENT_OCEANS_KEY);
            } else {
                return findBiome(THE_SILENT_OCEANS_KEY);
            }
        }

        double tempNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + tempOffsetX) * 0.002), 0f, (float) ((z + tempOffsetZ) * 0.002), 3);
        double humNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + humOffsetX) * 0.002), 0f, (float) ((z + humOffsetZ) * 0.002), 3);

        // 1. Hot Climate: Golden Desert (T > 0.15)
        if (tempNoise > 0.15) {
            return findBiome(GOLDEN_DESERT_KEY);
        }

        // 2. Elder Highlands Continental System (strictly deep continental interior:
        // contNoise > 0.20, Cold/Temperate: T < -0.02, away from desert buffer)
        // Broad scale (0.0004) for massive mountain ranges
        double rawHighland = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + highlandOffsetX) * 0.0004f), 0f, (float) ((z + highlandOffsetZ) * 0.0004f), 3);
        double highlandNoise = rawHighland / 0.32; // Normalized roughly to [-1.0, 1.0]

        // Highlands ONLY generate deep inland on high continentalness (contNoise >
        // 0.20)
        // This guarantees an expansive lowland buffer (Elderwoods) and beach between
        // Ocean and Highlands!
        if (contNoise > 0.20 && tempNoise < -0.02 && humNoise < 0.28 && highlandNoise > 0.05) {
            // Multi-octave alpine ridge noise for razor-sharp arêtes and aiguilles
            double r1 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((x + ridgeOffsetX) * 0.0015f), 0f, (float) ((z + ridgeOffsetZ) * 0.0015f), 2));
            double r2 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((x + ridgeOffsetX + 333) * 0.003f), 0f, (float) ((z + ridgeOffsetZ + 333) * 0.003f), 2));
            double ridgeNoise = r1 * 0.72 + r2 * 0.28;

            // Concentric Containment & Balanced Zonation:
            // - Summit peaks: STRICTLY compressed to apex mountain crests (highlandNoise >
            // 0.70, ridgeNoise > 0.75, T < -0.12)
            // - Forest flanks: Expanded to cover highlandNoise 0.34 to 0.70.
            // - Foothill meadows: Expanded to cover highlandNoise 0.05 to 0.34.
            if (highlandNoise > 0.70 && ridgeNoise > 0.75 && tempNoise < -0.12) {
                return findBiome(ELDER_HIGHLAND_SUMMIT_KEY);
            } else if (highlandNoise > 0.34) {
                return findBiome(ELDER_HIGHLAND_FOREST_KEY);
            } else {
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
     * Unified Terrain Shaping: Uses domain warping, multi-octave alpine fractals,
     * and continuous climate blending.
     * Eliminates artificial terracing steps and ensures mountains never form
     * vertical cliffs into deserts or oceans.
     */
    public int computeSurfaceHeight(int x, int z, Holder<Biome> surfaceBiome) {
        // Sample MultiNoise for Biome Placement
        double tempNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + tempOffsetX) * 0.002), 0f, (float) ((z + tempOffsetZ) * 0.002), 3);
        double humNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + humOffsetX) * 0.002), 0f, (float) ((z + humOffsetZ) * 0.002), 3);

        // Blending weights centered on Biome thresholds
        double desertWeight = net.minecraft.util.Mth.clamp((tempNoise - 0.05) / 0.20, 0.0, 1.0);
        double swampWeight = net.minecraft.util.Mth.clamp((humNoise - 0.2) / 0.20, 0.0, 1.0);

        // Domain Warping for organic, non-symmetrical mountain and hill contours
        double warpX = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + seedOffsetX) * 0.005f), 0f, (float) ((z + seedOffsetZ) * 0.005f), 2) * 38.0;
        double warpZ = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + seedOffsetZ + 317) * 0.005f), 0f, (float) ((z + seedOffsetX + 317) * 0.005f), 2) * 38.0;
        double wx = x + warpX;
        double wz = z + warpZ;

        // 1. Base Rolling Hills (Elderwoods / Scarlet Forest)
        double nx = (wx + seedOffsetX) * 0.003;
        double nz = (wz + seedOffsetZ) * 0.003;
        // Elevated baseline (centered at +8.0) ensures valleys naturally curve smoothly
        // above water level
        double baseNoise = 8.0 + Math.sin(nx) * Math.cos(nz) * 9.0;
        baseNoise += Math.sin(nx * 0.5 + 2.0) * Math.cos(nz * 0.6 + 1.1) * 4.5;

        // 2. Swamp Basin
        double bumpyNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + seedOffsetX) * 0.05), 0f, (float) ((z + seedOffsetZ) * 0.05), 2) * 3.0;
        double swampHeight = -3.0 + bumpyNoise;

        // Blend Swamp into Base Hills
        double terrainHeight = net.minecraft.util.Mth.lerp(swampWeight, baseNoise, swampHeight);

        // 3. Golden Desert Dunes
        double duneNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + seedOffsetX) * 0.03), 0f, (float) ((z + seedOffsetZ) * 0.03), 2) * 5.0;
        double desertHeight = (terrainHeight * 0.4) + duneNoise;

        // Blend Desert over base terrain
        double finalNoise = net.minecraft.util.Mth.lerp(desertWeight, terrainHeight, desertHeight);

        // Master Continentalness Sampling (strictly unwarped x, z for 100% synchronization with getSurfaceBiome and getNoiseBiome)
        double rawCont = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + contOffsetX) * 0.00035f), 0f, (float) ((z + contOffsetZ) * 0.00035f), 3);
        double contNoise = rawCont / 0.32;

        // 4. Elder Highlands Continental Mountain System (Massive ranges Y=80 to Y=210)
        // Climate & Continental gating: Highlands are strictly inland (contNoise >
        // 0.15)
        double tempFade = net.minecraft.util.Mth.clamp((-tempNoise - 0.02) / 0.14, 0.0, 1.0);
        double humFade = net.minecraft.util.Mth.clamp((0.28 - humNoise) / 0.14, 0.0, 1.0);
        double contFade = net.minecraft.util.Mth.clamp((contNoise - 0.15) / 0.10, 0.0, 1.0);
        double climateSupport = tempFade * humFade * contFade;

        double rawHighland = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((wx + highlandOffsetX) * 0.0004f), 0f, (float) ((wz + highlandOffsetZ) * 0.0004f), 3);
        double highlandNoise = rawHighland / 0.32;

        // Foothill ramp starts at highlandNoise = -0.05 so that by the meadow border
        // (0.05),
        // the terrain is already elevated by +12 to +16 blocks above normal Elderwoods
        double highlandWeight = net.minecraft.util.Mth.clamp((highlandNoise - (-0.05)) / 0.20, 0.0, 1.0);
        double effectiveHighlandWeight = highlandWeight * climateSupport;

        if (effectiveHighlandWeight > 0.0) {
            double smoothT = effectiveHighlandWeight * effectiveHighlandWeight * (3.0 - 2.0 * effectiveHighlandWeight);

            // A. Base Elevation Lift: elevates Highlands Meadows noticeably above
            // Elderwoods baseline (Y=78..92)
            double baseLift = 18.0 + smoothT * 16.0;

            // B. Asymmetrical Alpine Slopes & Glacial Valleys
            double alpineSlope = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((wx + seedOffsetX) * 0.0025f), 0f, (float) ((wz + seedOffsetZ) * 0.0025f), 3) * 26.0;
            double cragDetail = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) (x * 0.015f), 0f, (float) (z * 0.015f), 2) * 7.0;
            double microRoughness = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) (x * 0.04f), 0f, (float) (z * 0.04f), 2) * 3.0;

            // C. Mountain Flank / Pine Forest Lift (Y=115..155)
            double forestFactor = net.minecraft.util.Mth.clamp((highlandNoise - 0.30) / 0.35, 0.0, 1.0);
            double forestLift = forestFactor * 38.0;

            // D. Alpine Ridged Peak Formation (narrow razor-sharp crests thrusting to
            // Y=180..215)
            double r1 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((wx + ridgeOffsetX) * 0.0015f), 0f, (float) ((wz + ridgeOffsetZ) * 0.0015f), 2));
            double r2 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((wx + ridgeOffsetX + 333) * 0.003f), 0f, (float) ((wz + ridgeOffsetZ + 333) * 0.003f), 2));
            double ridgeVal = Math.max(0.0, (r1 * 0.72 + r2 * 0.28) - 0.32) / 0.68;
            double ridgePeak = Math.pow(ridgeVal, 3.0);

            double summitFactor = net.minecraft.util.Mth.clamp((highlandNoise - 0.65) / 0.25, 0.0, 1.0);
            double peakLift = ridgePeak * 88.0 * summitFactor;

            double totalHighlandBonus = (baseLift + alpineSlope + cragDetail + microRoughness + forestLift + peakLift)
                    * smoothT;
            finalNoise = net.minecraft.util.Mth.lerp(smoothT, finalNoise, terrainHeight + totalHighlandBonus);
        }

        // 5. Continental Protection, Coastline Unification & Bathymetric Carving
        // Sample Island Noise globally with broader wavelength (0.00095) for expansive multi-hundred-block marine domains
        double rawIsland = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + islandOffsetX) * 0.00095f), 0f, (float) ((z + islandOffsetZ) * 0.00095f), 3);
        double islandNoise = rawIsland / 0.32;

        // A. Mainland Coastal Slope & Shoreline (continuous inland elevation to water grade)
        double mainlandY = ElderwoodsChunkGenerator.BASE_HEIGHT + finalNoise;

        // Inland-to-Coast Smooth Taper (contNoise from 0.10 down to 0.02):
        double landCoastT = net.minecraft.util.Mth.clamp((contNoise - 0.02) / 0.08, 0.0, 1.0);
        double smoothLandCoast = landCoastT * landCoastT * (3.0 - 2.0 * landCoastT);
        double targetCoastY = ElderwoodsChunkGenerator.BASE_HEIGHT + Math.min(finalNoise, 3.5);
        double taperedMainlandY = net.minecraft.util.Mth.lerp(smoothLandCoast, targetCoastY, mainlandY);

        // Coastal Beach (contNoise from 0.02 down to -0.04):
        // Slopes smoothly from coastal grade (Y=65.5) through waterline (Y=62) down to shallow water (Y=58.0)
        double beachT = net.minecraft.util.Mth.clamp((0.02 - contNoise) / 0.06, 0.0, 1.0);
        double smoothBeach = beachT * beachT * (3.0 - 2.0 * beachT);
        double mainlandShoreY = net.minecraft.util.Mth.lerp(smoothBeach, taperedMainlandY, ElderwoodsChunkGenerator.BASE_HEIGHT - 4.0);

        // B. Ocean Bathymetry (Shallow photic shelf -> Continental slope -> Deep abyssal plains)
        // oceanProgress: 0.0 at coast (contNoise = 0.0), 1.0 at deep abyss (contNoise = -0.72)
        // Scaled to 0.72 (doubled continental span) to ensure vast Silent Ocean expanses before abyssal drop
        double oceanProgress = net.minecraft.util.Mth.clamp((-contNoise) / 0.72, 0.0, 1.0);

        // Continental Shelf Dynamic Modulation:
        // Doubled continental shelf length across coasts: from 400 blocks to over 1,500+ blocks of shallow seas!
        double rawShelfMod = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + contOffsetX + 4391) * 0.00045f), 0f, (float) ((z + contOffsetZ + 4391) * 0.00045f), 2);
        double shelfModNoise = rawShelfMod / 0.32;
        double dynamicShelfEdge = 0.66 + 0.22 * shelfModNoise;
        double dynamicAbyssEdge = net.minecraft.util.Mth.clamp(dynamicShelfEdge + 0.16, 0.75, 0.96);

        double oceanY;
        if (oceanProgress < dynamicShelfEdge) {
            // Photic Continental Shelf (Silent Oceans): slopes gently from shallow water (Y=58) to shelf floor (Y=42)
            double shelfFrac = oceanProgress / dynamicShelfEdge;
            double s = shelfFrac * shelfFrac * (3.0 - 2.0 * shelfFrac);
            oceanY = net.minecraft.util.Mth.lerp(s, ElderwoodsChunkGenerator.BASE_HEIGHT - 4.0, 42.0);
            // Rolling sandbanks and dunes across the expansive ocean floor
            oceanY += Math.sin(x * 0.012) * Math.cos(z * 0.012) * 2.5 * (1.0 - shelfFrac);
        } else if (oceanProgress < dynamicAbyssEdge) {
            // Continental Slope: descent from photic shelf floor (Y=42) down to abyssal floor (Y=-98)
            // Added subtle jagged crag roughness and rocky ledges along the descent
            double slopeFrac = (oceanProgress - dynamicShelfEdge) / (dynamicAbyssEdge - dynamicShelfEdge);
            double s = slopeFrac * slopeFrac * (3.0 - 2.0 * slopeFrac);

            // Jaggedness envelope: smoothly 0.0 at top (Y=42) and bottom (Y=-98) to ensure zero boundary steps
            double slopeEnvelope = Math.sin(slopeFrac * Math.PI);
            double j1 = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) (x * 0.025f), 0f, (float) (z * 0.025f), 2) * 5.5;
            double j2 = (1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((x + 417) * 0.055f), 0f, (float) ((z + 417) * 0.055f), 1))) * 3.5;
            double slopeJagged = (j1 + j2) * slopeEnvelope;

            oceanY = net.minecraft.util.Mth.lerp(s, 42.0, -98.0) + slopeJagged;
        } else {
            // Deep Abyssal Rift Valleys & Spiked Mountain Chains (Silent Depths):
            // Forms a dramatic underwater valley-shaped biome with deep canyon rifts (Y=-105..-98)
            // bordered by high-density razor-sharp spiked mountain ridges and needle pinnacles (Y=-50..-32)!
            double abyssalFrac = (oceanProgress - dynamicAbyssEdge) / Math.max(0.01, 1.0 - dynamicAbyssEdge);
            abyssalFrac = net.minecraft.util.Mth.clamp(abyssalFrac, 0.0, 1.0);
            double smoothAbyssal = abyssalFrac * abyssalFrac * (3.0 - 2.0 * abyssalFrac);

            // 1. Deep Rift Valley Basin Axis:
            double valleyBaseY = -104.0;
            double vNoise1 = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((x + trenchOffsetX + 1983) * 0.0028f), 0f, (float) ((z + trenchOffsetZ + 1983) * 0.0028f), 2);
            double valleyAxis = Math.abs(vNoise1); // 0 along central rift floor, > 0.18 at valley walls

            // 2. Dense Spiked Mountain Ridges & Needle Pinnacles:
            double r1 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((x + seedOffsetX + 555) * 0.0055f), 0f, (float) ((z + seedOffsetZ + 555) * 0.0055f), 2));
            double r2 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((x + seedOffsetZ + 999) * 0.0110f), 0f, (float) ((z + seedOffsetX + 999) * 0.0110f), 2));
            double r3 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((x + 333) * 0.0180f), 0f, (float) ((z + 333) * 0.0180f), 1));
            double combinedRidge = r1 * 0.55 + r2 * 0.32 + r3 * 0.13;

            // Power exponent creates needle-sharp mountain crests and steep pyramid flanks
            double spikeFactor = Math.pow(Math.max(0.0, (combinedRidge - 0.28) / 0.72), 2.8);

            // Spiked mountains flank the rift valleys and rise in high amounts: up to 72 blocks above valley floor!
            double valleyWallBoost = net.minecraft.util.Mth.clamp(valleyAxis / 0.18, 0.25, 1.0);
            double spikedMountainLift = spikeFactor * 72.0 * valleyWallBoost;

            // Gentle floor undulation in deep valley troughs
            double valleyFloorCurve = Math.sin(x * 0.009) * Math.cos(z * 0.009) * 4.0;

            // Combine into full abyssal valley topography
            double abyssalTerrainY = valleyBaseY + valleyFloorCurve + spikedMountainLift;
            oceanY = net.minecraft.util.Mth.lerp(smoothAbyssal, -98.0, abyssalTerrainY);

            // Seamounts in the open ocean (Underwater Mountain Ranges rising to photic zone)
            double m1 = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) (x * 0.0014f), 0f, (float) (z * 0.0014f), 3) / 0.32;
            double m2 = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((x + 719) * 0.0030f), 0f, (float) ((z + 719) * 0.0030f), 2) / 0.32;
            double combinedMount = m1 * 0.75 + m2 * 0.25;

            if (combinedMount > -0.22) {
                double mtT = net.minecraft.util.Mth.clamp((combinedMount - (-0.22)) / 0.85, 0.0, 1.0);
                double mtCurve = mtT * mtT * (3.0 - 2.0 * mtT);
                double mtLift = mtCurve * 142.0; // rises up to Y=44!
                oceanY += mtLift * smoothAbyssal;
            }

            // Sunken Trenches / Bedrock fault cracks: smooth Hermite drop down to bedrock layer (-118)
            double crackNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float) ((x + trenchOffsetX) * 0.012f), 0f, (float) ((z + trenchOffsetZ) * 0.012f), 2);
            double absCrack = Math.abs(crackNoise);
            if (absCrack < 0.038) {
                double crackT = absCrack / 0.038;
                double dropFactor = (1.0 - crackT) * (1.0 - crackT) * (3.0 - 2.0 * (1.0 - crackT));
                double depthFade = net.minecraft.util.Mth.clamp((-75.0 - oceanY) / 20.0, 0.0, 1.0);
                double crackTargetY = -118.0;
                oceanY = net.minecraft.util.Mth.lerp(dropFactor * depthFade, oceanY, crackTargetY);
            }
        }

        // C. Continuous Coastal Blending (Seamless unification of mainland and ocean)
        // Coastline transition zone: contNoise between -0.04 and 0.04 (centered at contNoise = 0.0, sea level)
        double coastWeight = net.minecraft.util.Mth.clamp((contNoise - (-0.04)) / 0.08, 0.0, 1.0);
        double smoothCoast = coastWeight * coastWeight * (3.0 - 2.0 * coastWeight);
        double baseTerrainY = net.minecraft.util.Mth.lerp(smoothCoast, oceanY, mainlandShoreY);

        // D. Island Seamounts & Dynamic Expansive Island Marine Shelves (Doubled Marine Footprint)
        // Samples regional marine shelf noise to give each island an expansive Silent Ocean footprint:
        // Wide lagoon/atoll shelves (hundreds of blocks wide) vs balanced shelves vs compact volcanic slopes!
        double rawIslandShelfMod = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + islandOffsetX + 7821) * 0.00065f), 0f, (float) ((z + islandOffsetZ + 7821) * 0.00065f), 2);
        double islandShelfMod = rawIslandShelfMod / 0.32;

        // Doubled shelf boundaries for each island: spans from vast lagoons (-0.56) to compact shelves (-0.08)
        double shelfNoiseStart = -0.32 - 0.24 * islandShelfMod;
        double apronNoiseStart = shelfNoiseStart - (0.28 + 0.10 * net.minecraft.util.Mth.clamp(islandShelfMod, -0.6, 1.0));

        double finalTerrainY = baseTerrainY;
        if (islandNoise > apronNoiseStart) {
            double islandTargetY;
            if (islandNoise > 0.50) {
                // Island Interior (Y=74 to 88)
                double t = net.minecraft.util.Mth.clamp((islandNoise - 0.50) / 0.20, 0.0, 1.0);
                double smoothT = t * t * (3.0 - 2.0 * t);
                double peak = 85.0 + Math.sin(x * 0.02) * Math.cos(z * 0.02) * 3.5;
                islandTargetY = net.minecraft.util.Mth.lerp(smoothT, 74.0, peak);
            } else if (islandNoise > 0.38) {
                // Beach Zone (Y=58 to 74) - gentle sandy shore passing through sea level (Y=62)
                double t = (islandNoise - 0.38) / 0.12;
                double smoothT = t * t * (3.0 - 2.0 * t);
                islandTargetY = net.minecraft.util.Mth.lerp(smoothT, 58.0, 74.0);
            } else if (islandNoise > shelfNoiseStart) {
                // Photic Silent Ocean Shelf (Y=42 to 58) - vast, doubled-size photic marine shelf around the island!
                double shelfT = (islandNoise - shelfNoiseStart) / (0.38 - shelfNoiseStart);
                double smoothShelf = shelfT * shelfT * (3.0 - 2.0 * shelfT);
                double shelfRoll = Math.sin(x * 0.015) * Math.cos(z * 0.015) * 4.0 * (shelfT * (1.0 - shelfT));
                islandTargetY = net.minecraft.util.Mth.lerp(smoothShelf, 42.0, 58.0) + shelfRoll;
            } else {
                // Submarine apron / ramp (sloping smoothly from surrounding seafloor baseTerrainY up to Y=42)
                double apronT = (islandNoise - apronNoiseStart) / (shelfNoiseStart - apronNoiseStart);
                double smoothApron = apronT * apronT * (3.0 - 2.0 * apronT);
                islandTargetY = net.minecraft.util.Mth.lerp(smoothApron, baseTerrainY, 42.0);
            }

            // Smooth fillet radius k smoothly fades to 0 at the outer submarine boundary
            double k = 5.0 * net.minecraft.util.Mth.clamp((islandNoise - apronNoiseStart) / 0.10, 0.0, 1.0);
            finalTerrainY = smoothMax(baseTerrainY, islandTargetY, k);
        }

        // --- DYNAMIC SWAMP SHORELINE (SPILL PREVENTION) ---
        // Strictly applies only on deep inland mainland where Scarlet Swamp meets adjacent biomes
        if (contNoise > 0.05) {
            double landFade = net.minecraft.util.Mth.clamp((contNoise - 0.05) / 0.08, 0.0, 1.0);
            double distToHumBorder = Math.abs(swampWeight - 0.5) * 2.0;
            double distToTempBorder = Math.abs(desertWeight - 0.5) * 2.0;

            double ridgeForce = 0.0;
            if (swampWeight > 0.5) {
                double closestEdge = Math.min(distToHumBorder, distToTempBorder);
                ridgeForce = Math.max(0.0, 1.0 - (closestEdge * closestEdge));
            } else {
                ridgeForce = Math.max(0.0, 1.0 - (distToHumBorder * distToHumBorder));
            }

            if (ridgeForce > 0.0) {
                double ridgeMinimum = ElderwoodsChunkGenerator.BASE_HEIGHT - 1.0;
                finalTerrainY = net.minecraft.util.Mth.lerp(ridgeForce * landFade, finalTerrainY, Math.max(finalTerrainY, ridgeMinimum));
            }
        }

        return (int) Math.round(finalTerrainY);
    }

    /**
     * Smooth Polynomial Maximum (smax) using a quadratic fillet.
     * Guarantees C1 continuity when blending intersecting terrain surfaces,
     * eliminating hard cut-offs, sharp creases, and stepped cliffs.
     */
    public static double smoothMax(double a, double b, double k) {
        if (k <= 0.0) {
            return Math.max(a, b);
        }
        double diff = a - b;
        if (diff >= k) {
            return a;
        }
        if (diff <= -k) {
            return b;
        }
        double h = 0.5 + 0.5 * diff / k;
        return net.minecraft.util.Mth.lerp(h, b, a) + k * h * (1.0 - h);
    }

    /**
     * Computes the dynamic continentalness threshold for the deep abyssal floor (Silent Depths),
     * dynamically varying based on regional continental shelf width.
     */
    public double getDynamicAbyssThreshold(int x, int z) {
        double rawShelfMod = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + contOffsetX + 4391) * 0.00045f), 0f, (float) ((z + contOffsetZ + 4391) * 0.00045f), 2);
        double shelfModNoise = rawShelfMod / 0.32;
        double dynamicShelfEdge = 0.66 + 0.22 * shelfModNoise;
        double dynamicAbyssEdge = net.minecraft.util.Mth.clamp(dynamicShelfEdge + 0.16, 0.75, 0.96);
        return -(dynamicAbyssEdge * 0.72);
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
