package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.util.FastNoise;
import net.minecraft.util.Mth;

/**
 * Pure mathematical helper encapsulating Coastal Lagoon generation, smooth 1x1 morphological biome borders,
 * and natural coastal soil/sand strand transitions so BiomeSource, ChunkGenerator, and offline verifiers
 * share a single source of truth.
 */
public final class ElderCoastalHelper {

    public enum StrandSurface {
        KEEP_BASE,
        SAND,
        COARSE_DIRT,
        DIRT
    }

    @FunctionalInterface
    public interface QuartCorePredicate {
        boolean isCoreQuartCenter(int cx, int cz);
    }

    @FunctionalInterface
    public interface OffsetBiomePredicate {
        boolean matchesOffset(int dx, int dz);
    }

    /**
     * Exact filled disk of (dx, dz, dist) offsets within radius sqrt(40) = 6.32 blocks,
     * sorted ascending by Euclidean distance so coastal checks terminate early with zero blind holes.
     */
    public static final int[][] SPIRAL_DISK_R6;
    public static final double[] SPIRAL_DIST_R6;

    static {
        java.util.List<int[]> pts = new java.util.ArrayList<>();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > 0 && d2 <= 40) {
                    pts.add(new int[] { dx, dz, d2 });
                }
            }
        }
        pts.sort(java.util.Comparator.comparingInt(a -> a[2]));
        SPIRAL_DISK_R6 = new int[pts.size()][2];
        SPIRAL_DIST_R6 = new double[pts.size()];
        for (int i = 0; i < pts.size(); i++) {
            SPIRAL_DISK_R6[i][0] = pts.get(i)[0];
            SPIRAL_DISK_R6[i][1] = pts.get(i)[1];
            SPIRAL_DIST_R6[i] = Math.sqrt(pts.get(i)[2]);
        }
    }

    public static double findMinBeachDist(OffsetBiomePredicate isBeachAtOffset) {
        for (int i = 0; i < SPIRAL_DISK_R6.length; i++) {
            if (isBeachAtOffset.matchesOffset(SPIRAL_DISK_R6[i][0], SPIRAL_DISK_R6[i][1])) {
                return SPIRAL_DIST_R6[i];
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    private ElderCoastalHelper() {
    }

    public static double hermite01(double v) {
        double c = Mth.clamp(v, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }

    /**
     * Computes the sheltered coastal lagoon envelope [0..1], strictly excluded from Scarlet humidity
     * (humNoise >= 0.24 guarantees >= 0.22 humidity buffer before scarlet_forest at 0.46),
     * Golden Desert (tempNoise >= 0.06), and open pelagic/deep inland terrain.
     */
    public static double computeLagoonEnvelope(double wx, double wz, double contNoise, double tempNoise,
            double humNoise, double islandOffsetX, double islandOffsetZ) {
        double humFade = hermite01((0.24 - humNoise) / 0.10);
        if (humFade <= 0.0) {
            return 0.0;
        }
        double tempFade = hermite01((0.06 - tempNoise) / 0.06)
                * hermite01((tempNoise + 0.22) / 0.08);
        if (tempFade <= 0.0) {
            return 0.0;
        }
        double coastEnvelope = hermite01((contNoise + 0.024) / 0.022)
                * hermite01((0.042 - contNoise) / 0.024);
        if (coastEnvelope <= 0.0) {
            return 0.0;
        }
        double rawLagoonZone = FastNoise.fbm2D(
                (float) ((wx + islandOffsetX + 5209) * 0.0017f),
                (float) ((wz + islandOffsetZ - 4127) * 0.0017f), 2) / 0.32;
        double lagoonZone = hermite01((rawLagoonZone - 0.12) / 0.16);
        return lagoonZone * coastEnvelope * humFade * tempFade;
    }

    public static double sampleLagoonField(double wx, double wz, double seedOffsetX, double seedOffsetZ) {
        return FastNoise.fbm2D(
                (float) ((wx + seedOffsetX + 7919) * 0.0038f),
                (float) ((wz + seedOffsetZ - 6131) * 0.0038f), 2) / 0.32;
    }

    public static double applyLagoonCarving(int x, int z, double wx, double wz,
            double contNoise, double tempNoise, double humNoise,
            double islandOffsetX, double islandOffsetZ, double seedOffsetX, double seedOffsetZ,
            double finalTerrainY) {
        double lagoonEnv = computeLagoonEnvelope(wx, wz, contNoise, tempNoise, humNoise, islandOffsetX, islandOffsetZ);
        if (lagoonEnv <= 0.0 || finalTerrainY <= 58.0 || finalTerrainY >= 75.4) {
            return finalTerrainY;
        }
        double bandWeight = hermite01((finalTerrainY - 58.0) / 4.0)
                * hermite01((75.4 - finalTerrainY) / 1.8);
        double basinMask = lagoonEnv * bandWeight;
        if (basinMask <= 0.36) {
            return finalTerrainY;
        }
        double lagoonField = sampleLagoonField(wx, wz, seedOffsetX, seedOffsetZ);
        double activeT = hermite01((basinMask - 0.36) / 0.24);
        if (lagoonField < 0.26 && finalTerrainY > 73.2) {
            double bankFlatT = hermite01((0.26 - lagoonField) / 0.18) * activeT * 0.85;
            finalTerrainY = Mth.lerp(bankFlatT, finalTerrainY, 72.8);
        }
        if (lagoonField < -0.10) {
            double channelCurve = hermite01((-0.10 - lagoonField) / 0.26) * activeT;
            double bedUndulation = (FastNoise.fbm2D((float) (x * 0.038f), (float) (z * 0.038f), 2) / 0.32) * 1.1;
            double lagoonBedY = 64.5 + bedUndulation;
            finalTerrainY = Mth.lerp(channelCurve, finalTerrainY, Math.min(finalTerrainY, lagoonBedY));
        }
        return finalTerrainY;
    }

    public static double computeLagoonBiomeWeight(int x, int z, double wx, double wz,
            double contNoise, double tempNoise, double humNoise,
            double islandOffsetX, double islandOffsetZ, double seedOffsetX, double seedOffsetZ,
            int surfaceY) {
        if (surfaceY < 58 || surfaceY > 74) {
            return 0.0;
        }
        double lagoonEnv = computeLagoonEnvelope(wx, wz, contNoise, tempNoise, humNoise, islandOffsetX, islandOffsetZ);
        if (lagoonEnv <= 0.0) {
            return 0.0;
        }
        double bandWeight = hermite01((surfaceY - 58.0) / 3.0)
                * hermite01((74.8 - surfaceY) / 1.6);
        double lagoonField = sampleLagoonField(wx, wz, seedOffsetX, seedOffsetZ);
        double fieldExtent = hermite01((0.24 - lagoonField) / 0.20);
        double edgeRipple = (FastNoise.fbm2D((float) (x * 0.045f), (float) (z * 0.045f), 2) / 0.32) * 0.06;
        return Mth.clamp(lagoonEnv * bandWeight * fieldExtent + edgeRipple, 0.0, 1.0);
    }

    public static double computeLagoonFeatureFactor(double lagoonBiomeWeight) {
        return hermite01((lagoonBiomeWeight - 0.40) / 0.30);
    }

    /**
     * Evaluates morphological core membership on 1x1 block contours using organic FBM domain warping
     * and bilinear interpolation across the 4 nearest quart centers, eliminating 4x4 staircase artifacts.
     */
    public static boolean hasSmoothMorphCore(int x, int z, int quartStep, float warpFreq, QuartCorePredicate coreAt) {
        double warpX = (FastNoise.fbm2D((float) (x * warpFreq), (float) (z * warpFreq), 2) / 0.32) * 4.5;
        double warpZ = (FastNoise.fbm2D((float) ((x + 317) * warpFreq), (float) ((z - 521) * warpFreq), 2) / 0.32) * 4.5;
        double wx = x + warpX;
        double wz = z + warpZ;

        int cX0 = Math.floorDiv((int) Math.floor(wx) - 2, quartStep) * quartStep + 2;
        int cZ0 = Math.floorDiv((int) Math.floor(wz) - 2, quartStep) * quartStep + 2;
        int cX1 = cX0 + quartStep;
        int cZ1 = cZ0 + quartStep;

        boolean b00 = coreAt.isCoreQuartCenter(cX0, cZ0);
        boolean b10 = coreAt.isCoreQuartCenter(cX1, cZ0);
        boolean b01 = coreAt.isCoreQuartCenter(cX0, cZ1);
        boolean b11 = coreAt.isCoreQuartCenter(cX1, cZ1);

        if (b00 == b10 && b00 == b01 && b00 == b11) {
            return b00;
        }

        double fx = Mth.clamp((wx - cX0) / (double) quartStep, 0.0, 1.0);
        double fz = Mth.clamp((wz - cZ0) / (double) quartStep, 0.0, 1.0);
        double blend = (1.0 - fx) * (1.0 - fz) * (b00 ? 1.0 : 0.0)
                + fx * (1.0 - fz) * (b10 ? 1.0 : 0.0)
                + (1.0 - fx) * fz * (b01 ? 1.0 : 0.0)
                + fx * fz * (b11 ? 1.0 : 0.0);
        return blend >= 0.5;
    }

    public static StrandSurface evaluateUplandCoastalStrand(int worldX, int worldZ, int surfaceY, int baseHeight,
            double minBeachDist, int outerHits) {
        boolean nearBeachPerimeter = minBeachDist <= 6.35;
        if (!nearBeachPerimeter && outerHits == 0 && surfaceY > baseHeight + 2) {
            return StrandSurface.KEEP_BASE;
        }

        double wave = FastNoise.fbm2D((float) (worldX * 0.055f), (float) (worldZ * 0.055f), 2) / 0.32;
        double fine = FastNoise.noise2D((float) (worldX * 0.24f), (float) (worldZ * 0.24f));

        // Inside 6.3-block perimeter of elder_beach (or <= Y=73): keep Grass Block completely clear
        // so client biome blending never tints crimson Scarlet grass with coastal beach grass tones.
        if (nearBeachPerimeter || surfaceY <= baseHeight + 1) {
            double distFactor = nearBeachPerimeter ? hermite01((5.0 - minBeachDist) / 4.0) : 0.35;
            double heightFactor = Mth.clamp((74.8 - surfaceY) / 3.0, 0.0, 1.0);
            double sandBias = distFactor * 0.62
                    + heightFactor * 0.24
                    + (outerHits / 8.0) * 0.14
                    + wave * 0.28 + fine * 0.16;
            if (sandBias > 0.56) {
                return StrandSurface.SAND;
            }
            return (wave * 0.55 + fine * 0.45 > 0.24) ? StrandSurface.DIRT : StrandSurface.COARSE_DIRT;
        }

        double fringe = (outerHits / 8.0) * 0.72
                + Mth.clamp((76.5 - surfaceY) / 3.5, 0.0, 1.0) * 0.28
                + wave * 0.32 + fine * 0.17;
        if (fringe > 0.64) {
            return StrandSurface.COARSE_DIRT;
        }
        if (fringe > 0.46) {
            return StrandSurface.DIRT;
        }
        return StrandSurface.KEEP_BASE;
    }
}
