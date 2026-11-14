package net.ganyusbathwater.oririmod.worldgen.feature;

import net.minecraft.util.Mth;

/**
 * Stateless, seed-deterministic marine ecology and rock-cluster zoning for
 * Silent Oceans and Elderwood beaches. Breaks up uniform seafloor generation
 * into organic habitats separated by barren sediment channels.
 */
public final class OceanEcologyHelper {

    public enum SilentOceanHabitat {
        BARREN_SEABED,
        SEAGRASS_MEADOW,
        KELP_FOREST,
        CORAL_GARDEN,
        MIXED_REEF
    }

    // 16 normalized 2D gradient directions for isotropic noise without axis-aligned bias
    private static final float[] GRAD_X = new float[16];
    private static final float[] GRAD_Z = new float[16];

    static {
        for (int i = 0; i < 16; i++) {
            double angle = (i * Math.PI * 2.0) / 16.0;
            GRAD_X[i] = (float) Math.cos(angle);
            GRAD_Z[i] = (float) Math.sin(angle);
        }
    }

    private OceanEcologyHelper() {
    }

    /**
     * Classifies a Silent Ocean column into an irregular biological zone using
     * domain-warped multi-octave Perlin fields (~32% barren seabed, ~20% seagrass,
     * ~16% kelp forest, ~16% coral garden, ~16% mixed lush reef).
     */
    public static SilentOceanHabitat getSilentOceanHabitat(long seed, int x, int z) {
        double warpX = fbm2D(x * 0.0075, z * 0.0075, seed ^ 0x3A51B9D74C81E2L, 2) * 38.0;
        double warpZ = fbm2D(x * 0.0075 + 211.3, z * 0.0075 - 173.9, seed ^ 0x6F93D1A8E5024BL, 2) * 38.0;
        double wx = x + warpX;
        double wz = z + warpZ;

        double richness = fbm2D(wx * 0.0046, wz * 0.0046, seed ^ 0x4F1BBCDCBA74A1L, 3);
        if (richness < -0.22) {
            return SilentOceanHabitat.BARREN_SEABED;
        }

        if (richness >= 0.42) {
            return SilentOceanHabitat.MIXED_REEF;
        }

        double bias = fbm2D(wx * 0.0058 + 413.0, wz * 0.0058 - 289.0, seed ^ 0x9E3779B97F4A7CL, 3);
        if (bias > 0.19) {
            return SilentOceanHabitat.CORAL_GARDEN;
        }
        if (bias < -0.19) {
            return SilentOceanHabitat.KELP_FOREST;
        }
        return SilentOceanHabitat.SEAGRASS_MEADOW;
    }

    /**
     * Meso-scale density modulation inside habitats [0..1] so kelp forests,
     * meadows, and coral beds naturally open into sandy clearings instead of
     * carpeting every block inside a zone.
     */
    public static float getFloraCanopyMask(long seed, int x, int z) {
        double warpX = fbm2D(x * 0.014, z * 0.014, seed ^ 0x1149C27B9D3A1EL, 2) * 14.0;
        double warpZ = fbm2D(x * 0.014 - 91.0, z * 0.014 + 67.0, seed ^ 0x72E4F5B81C392AL, 2) * 14.0;
        double mask = fbm2D((x + warpX) * 0.022, (z + warpZ) * 0.022, seed ^ 0x2545F4914F6CDDL, 2);
        return Mth.clamp((float) ((mask + 0.55) / 1.15), 0.0f, 1.0f);
    }

    /**
     * Determines whether a natural stone formation belongs at (x, z).
     * Creates wide rock-free stretches paired with tight grouped outcrops:
     * very rare in the Silent Ocean (~2-4% shoal clusters + monument halos),
     * occasional in clustered coastal coves along Elder Beaches (~20-26%).
     */
    public static boolean shouldSpawnRockFormation(long seed, int x, int z,
            boolean isSilentOcean, boolean inMonumentHalo) {
        double warpX = fbm2D(x * 0.009, z * 0.009, seed ^ 0xC13FA9A902A632L, 2) * 26.0;
        double warpZ = fbm2D(x * 0.009 + 149.0, z * 0.009 - 307.0, seed ^ 0x91E10DA5C79E7BL, 2) * 26.0;
        double wx = x + warpX;
        double wz = z + warpZ;

        // Broad shoal region (~90-140 block stretch) + tighter group pocket (~24-36 block cluster)
        double shoalMacro = fbm2D(wx * 0.0072, wz * 0.0072, seed ^ 0xA3B195354A39B7L, 3);
        double pocketCluster = fbm2D(x * 0.032 - 310.0, z * 0.032 + 480.0, seed ^ 0x1B873593D16549L, 2);

        if (isSilentOcean) {
            if (inMonumentHalo) {
                return pocketCluster > -0.15;
            }
            return shoalMacro > 0.78 && pocketCluster > -0.05;
        }

        // Elder Beach shallow waters: occasional grouped headland/cove clusters
        return shoalMacro > 0.30 && pocketCluster > 0.02;
    }

    /**
     * Controls coral presence along Elder Beach waters so shallow coral gardens
     * cluster in sheltered coves rather than lining every beach uniformly.
     */
    public static boolean isBeachCoralCove(long seed, int x, int z) {
        double warpX = fbm2D(x * 0.008, z * 0.008, seed ^ 0x517CC1B727220AL, 2) * 22.0;
        double warpZ = fbm2D(x * 0.008 - 83.0, z * 0.008 + 197.0, seed ^ 0x85EBCA6B1C4E2DL, 2) * 22.0;
        double cove = fbm2D((x + warpX) * 0.0068, (z + warpZ) * 0.0068, seed ^ 0xC2B2AE3D27D4EBL, 3);
        return cove > 0.14;
    }

    /**
     * 3D/2D deterministic Perlin gradient FBM with practical range roughly [-1.2, +1.2]
     * (standard deviation ~0.46 for 3 octaves, ~0.42 for 2 octaves).
     */
    public static double fbm2D(double x, double z, long seed, int octaves) {
        double sum = 0.0;
        double amp = 1.0;
        double freq = 1.0;
        for (int i = 0; i < octaves; i++) {
            sum += perlin2D(x * freq, z * freq, seed + i * 0x9E3779B97F4A7C15L) * amp;
            freq *= 2.03;
            amp *= 0.5;
        }
        return sum;
    }

    public static double perlin2D(double x, double z, long seed) {
        int x0 = Mth.floor(x);
        int z0 = Mth.floor(z);
        double fx = x - x0;
        double fz = z - z0;

        double u = fade(fx);
        double v = fade(fz);

        int g00 = hashCorner(x0, z0, seed);
        int g10 = hashCorner(x0 + 1, z0, seed);
        int g01 = hashCorner(x0, z0 + 1, seed);
        int g11 = hashCorner(x0 + 1, z0 + 1, seed);

        double n00 = GRAD_X[g00] * fx + GRAD_Z[g00] * fz;
        double n10 = GRAD_X[g10] * (fx - 1.0) + GRAD_Z[g10] * fz;
        double n01 = GRAD_X[g01] * fx + GRAD_Z[g01] * (fz - 1.0);
        double n11 = GRAD_X[g11] * (fx - 1.0) + GRAD_Z[g11] * (fz - 1.0);

        double nx0 = Mth.lerp(u, n00, n10);
        double nx1 = Mth.lerp(u, n01, n11);
        // Scale by sqrt(2) so peak value reaches [-1, 1]
        return Mth.lerp(v, nx0, nx1) * 1.41421356;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static int hashCorner(int x, int z, long seed) {
        long h = seed;
        h ^= x * 0xBF58476D1CE4E5B9L;
        h ^= z * 0x94D049BB133111EBL;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= (h >>> 31);
        return (int) (h & 15);
    }
}

