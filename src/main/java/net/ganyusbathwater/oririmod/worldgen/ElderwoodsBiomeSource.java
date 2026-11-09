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

        if (contNoise < 0.02) {
            if (!isBelowSurfaceLayer) {
                return surfaceBiome;
            }
            if (contNoise < -0.04) {
                if (surfaceY <= -116) {
                    return findBiome(THE_SUNKEN_TRENCHES_KEY);
                } else if (surfaceY <= -85 && contNoise < -0.42) {
                    return findBiome(THE_SILENT_DEPTHS_KEY);
                } else if (surfaceY >= 74) {
                    return findBiome(ELDERWOODS_CAVE_KEY);
                } else {
                    return surfaceBiome;
                }
            } else {
                return findBiome(ELDERWOODS_CAVE_KEY);
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

        if (contNoise < 0.02) {
            if (!isBelowSurfaceLayer) {
                return surfaceBiome;
            }
            if (contNoise < -0.04) {
                if (surfaceY <= -116) {
                    return findBiome(THE_SUNKEN_TRENCHES_KEY);
                } else if (surfaceY <= -85 && contNoise < -0.42) {
                    return findBiome(THE_SILENT_DEPTHS_KEY);
                } else if (surfaceY >= 74) {
                    return findBiome(ELDERWOODS_CAVE_KEY);
                } else {
                    return surfaceBiome;
                }
            } else {
                return findBiome(ELDERWOODS_CAVE_KEY);
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
        // 0. Master Continentalness Axis: Oceanic Domain vs Coastal Beach vs
        // Continental Landmass
        double rawCont = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + contOffsetX) * 0.00035f), 0f, (float) ((z + contOffsetZ) * 0.00035f), 3);
        double contNoise = rawCont / 0.32;

        // Oceanic Domain (contNoise < -0.04)
        if (contNoise < -0.04) {
            int surfaceY = computeSurfaceHeight(x, z, null);
            if (surfaceY >= 74) {
                // Island Interior (Lush Elderwoods or Golden Desert)
                double tempNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                        (float) ((x + tempOffsetX) * 0.002), 0f, (float) ((z + tempOffsetZ) * 0.002), 3);
                return findBiome(tempNoise > 0.15 ? GOLDEN_DESERT_KEY : ELDERWOODS_KEY);
            } else if (surfaceY >= 66) {
                // Island Shore / Beach
                return findBiome(ELDER_BEACH_KEY);
            } else if (surfaceY >= -20) {
                // Photic Ocean Shelf / Underwater Mountain Flank / Upper Sea
                return findBiome(THE_SILENT_OCEANS_KEY);
            } else {
                // Deep Abyssal Domain
                double crackNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                        (float) ((x + trenchOffsetX) * 0.012f), 0f, (float) ((z + trenchOffsetZ) * 0.012f), 2);
                boolean isCrack = Math.abs(crackNoise) < 0.038;
                if (isCrack && surfaceY <= -116) {
                    return findBiome(THE_SUNKEN_TRENCHES_KEY);
                } else if (surfaceY <= -85 && contNoise < -0.42) {
                    // Strictly deep abyssal floors: reduced size so Silent Ocean has expansive space
                    return findBiome(THE_SILENT_DEPTHS_KEY);
                } else {
                    return findBiome(THE_SILENT_OCEANS_KEY);
                }
            }
        } else if (contNoise < 0.02) {
            int surfaceY = computeSurfaceHeight(x, z, null);
            if (surfaceY >= 74) {
                // Peninsula interior connecting island to mainland
                double tempNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                        (float) ((x + tempOffsetX) * 0.002), 0f, (float) ((z + tempOffsetZ) * 0.002), 3);
                return findBiome(tempNoise > 0.15 ? GOLDEN_DESERT_KEY : ELDERWOODS_KEY);
            }
            return findBiome(ELDER_BEACH_KEY);
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

        // 5. Continental Protection & Bathymetric Carving
        // Dynamic Relative Shoreline: Beach seamlessly inherits the actual incoming inland elevation
        // and slopes dynamically into shallow water (-5.0, Y=57.0).
        // Zero hardcoded fixed-height plateau, zero exposed sandstone cliff wall!

        // Sample Island Noise globally
        double rawIsland = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) ((x + islandOffsetX) * 0.0014f), 0f, (float) ((z + islandOffsetZ) * 0.0014f), 3);
        double islandNoise = rawIsland / 0.32;

        double islandTargetY = -999.0;
        if (islandNoise > -0.05) {
            if (islandNoise > 0.52) {
                // Island Interior (Y=74 to 88)
                double t = net.minecraft.util.Mth.clamp((islandNoise - 0.52) / 0.18, 0.0, 1.0);
                double smoothT = t * t * (3.0 - 2.0 * t);
                double peak = 85.0 + Math.sin(x * 0.02) * Math.cos(z * 0.02) * 3.5;
                islandTargetY = net.minecraft.util.Mth.lerp(smoothT, 74.0, peak);
            } else if (islandNoise > 0.42) {
                // Beach Zone (Y=66 to 74) - wide, gentle sandy shore
                double t = (islandNoise - 0.42) / 0.10;
                double smoothT = t * t * (3.0 - 2.0 * t);
                islandTargetY = net.minecraft.util.Mth.lerp(smoothT, 66.0, 74.0);
            } else if (islandNoise > 0.15) {
                // Photic Silent Ocean Shelf (Y=42 to 66) - expansive photic ring around the island
                double t = (islandNoise - 0.15) / 0.27;
                double smoothT = t * t * (3.0 - 2.0 * t);
                islandTargetY = net.minecraft.util.Mth.lerp(smoothT, 42.0, 66.0);
            }
        }

        if (contNoise >= -0.04) {
            // Soft curve: ensures land valleys naturally curve above water level
            if (finalNoise < 1.0) {
                finalNoise = 1.0 + Math.exp(finalNoise - 1.0) * 0.5;
            }

            // Inland-to-Coast Smooth Taper (contNoise from 0.10 down to 0.02):
            // Gently eases inland elevations toward the coastal grade
            if (contNoise < 0.10) {
                double landCoastT = net.minecraft.util.Mth.clamp((contNoise - 0.02) / 0.08, 0.0, 1.0);
                double smoothLandCoast = landCoastT * landCoastT * (3.0 - 2.0 * landCoastT);
                double targetCoastalBase = Math.min(finalNoise, 3.0);
                finalNoise = net.minecraft.util.Mth.lerp(smoothLandCoast, targetCoastalBase, finalNoise);
            }

            // Coastal Beach (contNoise from 0.02 down to -0.04):
            // Dynamically slopes from the actual terrain elevation down to shallow water (-5.0, Y=57.0)
            if (contNoise < 0.02) {
                double beachT = (0.02 - contNoise) / 0.06;
                double smoothBeach = beachT * beachT * (3.0 - 2.0 * beachT);
                finalNoise = net.minecraft.util.Mth.lerp(smoothBeach, finalNoise, -5.0);
            }

            // Island-Mainland Collision Peninsula Merging:
            // When an ocean island seamount collides with or approaches the coast,
            // smoothly blend and elevate the terrain into a natural peninsula, headland, or isthmus!
            // Prevents unnatural water moats, trenches, or abrupt cliffs along the coastal boundary.
            if (islandTargetY > -900.0) {
                double islandDelta = islandTargetY - ElderwoodsChunkGenerator.BASE_HEIGHT;
                if (islandNoise > 0.42) {
                    // Island interior or high beach: seamlessly bridges across the water into a solid peninsula!
                    finalNoise = Math.max(finalNoise, islandDelta);
                } else if (islandNoise > 0.15) {
                    // Photic shelf / tombolo: smoothly raises the seabed, creating a shallow sandbar
                    finalNoise = Math.max(finalNoise, islandDelta);
                }
            }
        } else {
            // Oceanic Domain (contNoise < -0.04)
            // 1. Photic Continental Shelf (Silent Oceans): vast majority of the ocean domain (-0.04 to -0.30)
            // Slopes gently from coast Y=67 down to normal expansive floor Y=40..44.
            // 2. Continental Slope (contNoise < -0.30): sharp dramatic plunge down to deep abyssal floor Y=-50.
            double u = net.minecraft.util.Mth.clamp((-contNoise - 0.04) / 0.42, 0.0, 1.0);

            double baseOceanY;
            if (u < 0.75) {
                // Photic Continental Shelf (Silent Oceans): vast majority of ocean domain (-0.04 to -0.36)
                double shelfFrac = u / 0.75;
                double s = shelfFrac * shelfFrac * (3.0 - 2.0 * shelfFrac);
                baseOceanY = net.minecraft.util.Mth.lerp(s, 67.0, 40.0);
                // Rolling sandbanks and dunes across the expansive ocean floor
                baseOceanY += Math.sin(x * 0.012) * Math.cos(z * 0.012) * 2.8 * (1.0 - shelfFrac);
            } else {
                // Continental Slope: plunge from Y=40 down to deep abyssal floor Y=-106
                double slopeFrac = (u - 0.75) / 0.25;
                double s = slopeFrac * slopeFrac * (3.0 - 2.0 * slopeFrac);
                baseOceanY = net.minecraft.util.Mth.lerp(s, 40.0, -106.0);
            }

            // 3. Deep Abyssal Plains & Underwater Mountain Ranges (Silent Depths):
            if (u > 0.75) {
                double abyssalFrac = (u - 0.75) / 0.25;
                double smoothAbyssal = abyssalFrac * abyssalFrac * (3.0 - 2.0 * abyssalFrac);

                // Gentle rolling curvature of the seabed floor between mountains
                double valleyCurvature = (Math.sin(x * 0.007) * Math.cos(z * 0.007)
                        + Math.cos(x * 0.014) * Math.sin(z * 0.014) * 0.4) * 3.5;
                baseOceanY += valleyCurvature * smoothAbyssal;

                // Underwater Mountain Ranges (seamounts) rising between the deep abyssal floors:
                // Abundant seamounts rising up to 150 blocks above the floor (reaching Y=35..48 into the photic Silent Ocean)!
                double m1 = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                        (float) (x * 0.0022f), 0f, (float) (z * 0.0022f), 3) / 0.32;
                double m2 = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                        (float) ((x + 719) * 0.0045f), 0f, (float) ((z + 719) * 0.0045f), 2) / 0.32;
                double combinedMount = m1 * 0.75 + m2 * 0.25;

                if (combinedMount > -0.15) {
                    double mtT = net.minecraft.util.Mth.clamp((combinedMount - (-0.15)) / 0.85, 0.0, 1.0);
                    double mtCurve = Math.pow(mtT, 1.25);
                    double mtLift = mtCurve * 150.0;
                    baseOceanY += mtLift * smoothAbyssal;
                }

                // 4. Dedicated Ocean Fissures & Bedrock Fault Cracks (Sunken Trenches):
                // Narrow cracks (3-5 blocks wide) cracking the floor open, dropping 10-12 blocks down
                // to a partially fixed height right above bedrock (-118, bedrock is -124)
                double crackNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                        (float) ((x + trenchOffsetX) * 0.012f), 0f, (float) ((z + trenchOffsetZ) * 0.012f), 2);
                double absCrack = Math.abs(crackNoise);
                if (absCrack < 0.038 && baseOceanY <= -90.0) {
                    double crackT = absCrack / 0.038;
                    double dropFactor = Math.pow(1.0 - crackT, 0.25); // steep ravine profile
                    double crackTargetY = -118.0; // slightly above bedrock layer (-128..-124)
                    baseOceanY = net.minecraft.util.Mth.lerp(dropFactor, baseOceanY, crackTargetY);
                }
            }

            // 5. Ocean Mountain Islands & Expansive Island Shelves:
            if (islandTargetY > -900.0) {
                baseOceanY = Math.max(baseOceanY, islandTargetY);
            } else if (islandNoise > -0.05) {
                // Gentle Submarine Base Slope (Y=baseOceanY to 42)
                double t = (islandNoise - (-0.05)) / 0.20;
                double smoothT = t * t * (3.0 - 2.0 * t);
                baseOceanY = net.minecraft.util.Mth.lerp(smoothT, baseOceanY, 42.0);
            }

            double oceanDelta = baseOceanY - ElderwoodsChunkGenerator.BASE_HEIGHT;
            finalNoise = oceanDelta;
        }

        // --- DYNAMIC SWAMP SHORELINE (SPILL PREVENTION) ---
        // Blood water spawns at Y=61 (BASE_HEIGHT - 2) in the Scarlet Swamp.
        // If an adjacent Desert or Forest dune drops lower than Y=61, the water will
        // form a sheer vertical cliff.
        // We dynamically calculate distance to the exact Swamp border and build a
        // natural retaining ridge!
        if (contNoise > -0.06) {
            double distToHumBorder = Math.abs(swampWeight - 0.5) * 2.0; // 0 at Hum border, 1 inside/outside
            double distToTempBorder = Math.abs(desertWeight - 0.5) * 2.0; // 0 at Temp border, 1 inside/outside

            double ridgeForce = 0.0;
            if (swampWeight > 0.5) {
                // Inside the Swamp's humidity zone.
                // The border is either the edge of the humidity zone (Forest) or the edge of
                // the temperature zone (Desert).
                double closestEdge = Math.min(distToHumBorder, distToTempBorder);
                ridgeForce = Math.max(0.0, 1.0 - (closestEdge * closestEdge));
            } else {
                // Outside the humidity zone, the only relevant border is the hum border.
                ridgeForce = Math.max(0.0, 1.0 - (distToHumBorder * distToHumBorder));
            }

            if (ridgeForce > 0.0) {
                // Force the surface up to at least -1.0 (Y=62) to physically contain the Y=61
                // water!
                double ridgeMinimum = -1.0;
                finalNoise = net.minecraft.util.Mth.lerp(ridgeForce, finalNoise, Math.max(finalNoise, ridgeMinimum));
            }
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
