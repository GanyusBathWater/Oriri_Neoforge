package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.util.FastNoise;
import net.minecraft.util.Mth;

/**
 * Computes C1-smooth photic-shelf archipelago chains, sandy cays, and reef lagoons
 * integrated into {@link ElderwoodsBiomeSource#computeRawSurfaceHeight(int, int)}.
 */
final class ArchipelagoTerrainHelper {

    private ArchipelagoTerrainHelper() {
    }

    /**
     * Raises organic curved island chains and shallow sandy atolls on the photic ocean shelf
     * while leaving mainland coasts, open abyssal basins, and ocean floor slopes undisturbed.
     */
    static double applyArchipelagoTerrain(
            int x,
            int z,
            double contNoise,
            double oceanProgress,
            double dynamicShelfEdge,
            double currentTerrainY) {

        if (contNoise >= 0.0) {
            return currentTerrainY;
        }

        double shelfLimit = dynamicShelfEdge - 0.08;
        if (oceanProgress <= 0.06 || oceanProgress >= shelfLimit || currentTerrainY < 36.0) {
            return currentTerrainY;
        }

        double shoreFade = hermite01((oceanProgress - 0.06) / 0.12);
        double slopeFade = hermite01((shelfLimit - oceanProgress) / 0.14);
        double floorFade = hermite01((currentTerrainY - 36.0) / 8.0);

        // Macro province selector keeps large open-ocean expanses free while creating distinct archipelago regions (~28%).
        double provinceNoise = FastNoise.fbm3D(
                (float) ((x + 31415) * 0.00045f), 0f, (float) ((z - 27182) * 0.00045f), 2) / 0.32;
        double provinceMask = hermite01((provinceNoise - 0.04) / 0.30);

        double archWeight = shoreFade * slopeFade * floorFade * provinceMask;
        if (archWeight <= 0.001) {
            return currentTerrainY;
        }

        // Domain warp curves tectonic chains into sweeping natural arcs instead of noisy blobs.
        double warpX = FastNoise.fbm3D((float) ((x + 9103) * 0.0020f), 0f, (float) ((z - 4211) * 0.0020f), 2) * 88.0;
        double warpZ = FastNoise.fbm3D((float) ((x - 6317) * 0.0020f), 0f, (float) ((z + 8843) * 0.0020f), 2) * 88.0;
        double dx = x + warpX;
        double dz = z + warpZ;

        // Sinuous arc belt concentrates islets into chains with navigable deep straits in between.
        double spineRaw = FastNoise.fbm3D((float) (dx * 0.00145f), 0f, (float) (dz * 0.00145f), 2) / 0.32;
        double arcRidge = 1.0 - Math.min(1.0, Math.abs(spineRaw) * 1.40);

        // Break the spine into discrete small islands, sandbars, and partial coral atoll rings.
        double clusterRaw = FastNoise.fbm3D(
                (float) ((dx + 17321) * 0.0064f), 0f, (float) ((dz - 11903) * 0.0064f), 3) / 0.32;
        double shoreJitter = (FastNoise.fbm3D((float) (x * 0.022f), 0f, (float) (z * 0.022f), 2) / 0.32) * 0.13;
        double ringRaw = FastNoise.fbm3D(
                (float) ((dx - 4512) * 0.0032f), 0f, (float) ((dz + 14821) * 0.0032f), 2) / 0.32;
        double atollRing = 0.15 * (1.0 - Math.min(1.0, Math.abs(Math.abs(ringRaw) - 0.38) / 0.24));

        double rawChainSignal = (clusterRaw * 0.85 + shoreJitter) + (arcRidge * 0.44 - 0.18) + atollRing;
        double signal = Mth.lerp(archWeight, -0.75, rawChainSignal);

        if (signal <= -0.24) {
            return currentTerrainY;
        }

        double archTargetY = computeArchipelagoProfileY(x, z, signal);
        double fillet = 4.5 * archWeight * hermite01((signal + 0.24) / 0.16);
        return ElderwoodsBiomeSource.smoothMax(currentTerrainY, archTargetY, fillet);
    }

    private static double computeArchipelagoProfileY(int x, int z, double signal) {
        if (signal < 0.06) {
            // Submarine coral & seagrass shoal bank rising from deep shelf floor (Y=45..61)
            double t = hermite01((signal + 0.24) / 0.30);
            return Mth.lerp(t, 45.0, 61.0);
        }
        if (signal < 0.30) {
            // Shallow reef flats (Y=61..69) tapering into warm sandy bars & beaches (Y=70..73.4)
            double t = hermite01((signal - 0.06) / 0.24);
            return Mth.lerp(t, 61.0, 73.4);
        }

        // Gently rounded vegetated islet crown (Y=73.4..80.6) with subtle terrain micro-relief
        double peakT = Mth.clamp((signal - 0.30) / 0.65, 0.0, 1.0);
        double softDome = Math.sqrt(peakT);
        double relief = (FastNoise.fbm3D((float) ((x + 511) * 0.015f), 0f, (float) ((z - 917) * 0.015f), 2) / 0.32)
                * 1.1 * softDome;
        return 73.4 + 7.2 * softDome + relief;
    }

    static double hermite01(double t) {
        double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }
}

