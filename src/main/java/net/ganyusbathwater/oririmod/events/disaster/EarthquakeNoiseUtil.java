package net.ganyusbathwater.oririmod.events.disaster;

/**
 * Fast, seedable, allocation-free 2D and 3D gradient noise for organic terrain carving
 * and domain warping.
 */
public final class EarthquakeNoiseUtil {

    private final int[] perm = new int[512];

    public EarthquakeNoiseUtil(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }
        // Shuffle using LCG seeded with disaster seed
        long s = seed ^ 0x5DEECE66DL;
        for (int i = 255; i > 0; i--) {
            s = (s * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
            int j = (int) ((s >>> 16) % (i + 1));
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) {
            perm[i] = p[i & 255];
        }
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad2D(int hash, double x, double z) {
        int h = hash & 7;
        double u = h < 4 ? x : z;
        double v = h < 4 ? z : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    private static double grad3D(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    /** 2D Perlin gradient noise in range [-1.0, 1.0]. */
    public double noise2D(double x, double z) {
        int xi = (int) Math.floor(x) & 255;
        int zi = (int) Math.floor(z) & 255;

        double xf = x - Math.floor(x);
        double zf = z - Math.floor(z);

        double u = fade(xf);
        double v = fade(zf);

        int aa = perm[perm[xi] + zi];
        int ab = perm[perm[xi] + zi + 1];
        int ba = perm[perm[xi + 1] + zi];
        int bb = perm[perm[xi + 1] + zi + 1];

        double x1 = lerp(u, grad2D(aa, xf, zf), grad2D(ba, xf - 1.0, zf));
        double x2 = lerp(u, grad2D(ab, xf, zf - 1.0), grad2D(bb, xf - 1.0, zf - 1.0));

        return lerp(v, x1, x2);
    }

    /** 3D Perlin gradient noise in range [-1.0, 1.0]. */
    public double noise3D(double x, double y, double z) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;
        int zi = (int) Math.floor(z) & 255;

        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);
        double zf = z - Math.floor(z);

        double u = fade(xf);
        double v = fade(yf);
        double w = fade(zf);

        int a = perm[xi] + yi;
        int aa = perm[a] + zi;
        int ab = perm[a + 1] + zi;
        int b = perm[xi + 1] + yi;
        int ba = perm[b] + zi;
        int bb = perm[b + 1] + zi;

        double x1 = lerp(u, grad3D(perm[aa], xf, yf, zf), grad3D(perm[ba], xf - 1.0, yf, zf));
        double x2 = lerp(u, grad3D(perm[ab], xf, yf - 1.0, zf), grad3D(perm[bb], xf - 1.0, yf - 1.0, zf));
        double y1 = lerp(v, x1, x2);

        double x3 = lerp(u, grad3D(perm[aa + 1], xf, yf, zf - 1.0), grad3D(perm[ba + 1], xf - 1.0, yf, zf - 1.0));
        double x4 = lerp(u, grad3D(perm[ab + 1], xf, yf - 1.0, zf - 1.0), grad3D(perm[bb + 1], xf - 1.0, yf - 1.0, zf - 1.0));
        double y2 = lerp(v, x3, x4);

        return lerp(w, y1, y2);
    }

    /** 2-octave fractional Brownian motion for organic crack heading. */
    public double fbm2D(double x, double z) {
        return noise2D(x, z) * 0.7 + noise2D(x * 2.3 + 17.1, z * 2.3 + 31.7) * 0.3;
    }

    /** 2-octave 3D noise for organic wall skew. */
    public double fbm3D(double x, double y, double z) {
        return noise3D(x, y, z) * 0.75 + noise3D(x * 2.1, y * 2.1, z * 2.1) * 0.25;
    }
}

