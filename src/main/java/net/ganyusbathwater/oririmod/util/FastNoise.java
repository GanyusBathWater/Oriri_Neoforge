package net.ganyusbathwater.oririmod.util;

public class FastNoise {
    private static final int[] P = new int[512];
    
    // Deterministic seed generation
    public static void init(long seed) {
        java.util.Random random = new java.util.Random(seed);
        int[] p = new int[256];
        for (int i=0; i<256; i++) p[i] = i;
        for (int i=0; i<256; i++) {
            int j = random.nextInt(256);
            int swap = p[i];
            p[i] = p[j];
            p[j] = swap;
        }
        for (int i=0; i<256; i++) {
            P[i] = p[i];
            P[i + 256] = p[i];
        }
    }
    
    // Initialize with default seed
    static {
        init(42069L);
    }
    
    private static float fade(float t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static float lerp(float t, float a, float b) { return a + t * (b - a); }
    private static float grad(int hash, float x, float y, float z) {
        int h = hash & 15;
        float u = h < 8 ? x : y;
        float v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
    
    public static float noise3D(float x, float y, float z) {
        double fx = Math.floor(x);
        double fy = Math.floor(y);
        double fz = Math.floor(z);
        int X = ((int) fx) & 255;
        int Y = ((int) fy) & 255;
        int Z = ((int) fz) & 255;
        x = (float) (x - fx);
        y = (float) (y - fy);
        z = (float) (z - fz);
        float u = fade(x);
        float v = fade(y);
        float w = fade(z);
        int A = P[X] + Y, AA = P[A] + Z, AB = P[A + 1] + Z;
        int B = P[X + 1] + Y, BA = P[B] + Z, BB = P[B + 1] + Z;

        return lerp(w, lerp(v, lerp(u, grad(P[AA], x, y, z), grad(P[BA], x - 1, y, z)),
                               lerp(u, grad(P[AB], x, y - 1, z), grad(P[BB], x - 1, y - 1, z))),
                       lerp(v, lerp(u, grad(P[AA + 1], x, y, z - 1), grad(P[BA + 1], x - 1, y, z - 1)),
                               lerp(u, grad(P[AB + 1], x, y - 1, z - 1), grad(P[BB + 1], x - 1, y - 1, z - 1))));
    }

    /**
     * Exact 2D fast path matching {@code noise3D(x, 0f, z)} while skipping the unused Y=1 cube plane.
     */
    public static float noise2D(float x, float z) {
        double fx = Math.floor(x);
        double fz = Math.floor(z);
        int X = ((int) fx) & 255;
        int Z = ((int) fz) & 255;
        x = (float) (x - fx);
        z = (float) (z - fz);
        float u = fade(x);
        float w = fade(z);
        int AA = P[P[X]] + Z;
        int BA = P[P[X + 1]] + Z;

        return lerp(w,
                lerp(u, grad(P[AA], x, 0f, z), grad(P[BA], x - 1, 0f, z)),
                lerp(u, grad(P[AA + 1], x, 0f, z - 1), grad(P[BA + 1], x - 1, 0f, z - 1)));
    }

    public static float fbm3D(float x, float y, float z, int octaves) {
        float total = 0;
        float frequency = 1;
        float amplitude = 1;
        float maxValue = 0;
        for (int i = 0; i < octaves; i++) {
            total += noise3D(x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= 0.5f;
            frequency *= 2.0f;
        }
        return total / maxValue;
    }

    /**
     * Exact 2D multi-octave Perlin FBM matching {@code fbm3D(x, 0f, z, octaves)} at ~2x speed.
     */
    public static float fbm2D(float x, float z, int octaves) {
        float total = 0;
        float frequency = 1;
        float amplitude = 1;
        float maxValue = 0;
        for (int i = 0; i < octaves; i++) {
            total += noise2D(x * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= 0.5f;
            frequency *= 2.0f;
        }
        return total / maxValue;
    }
}
