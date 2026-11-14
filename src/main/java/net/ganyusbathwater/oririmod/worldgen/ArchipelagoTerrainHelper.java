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

        // Stay cleanly inside the photic shelf away from the steep continental drop-off.
        double shelfLimit = dynamicShelfEdge - 0.12;
        if (oceanProgress <= 0.06 || oceanProgress >= shelfLimit || currentTerrainY < 34.0) {
            return currentTerrainY;
        }

        double shoreFade = hermite01((oceanProgress - 0.06) / 0.14);
        double slopeFade = hermite01((shelfLimit - oceanProgress) / 0.16);
        double floorFade = hermite01((currentTerrainY - 34.0) / 10.0);

        // Macro province selector keeps large open-ocean expanses free while creating distinct archipelago regions (~28%).
        double provinceNoise = FastNoise.fbm2D(
                (float) ((x + 31415) * 0.00045f), (float) ((z - 27182) * 0.00045f), 2) / 0.32;
        double provinceMask = hermite01((provinceNoise - 0.04) / 0.30);

        double archWeight = shoreFade * slopeFade * floorFade * provinceMask;
        if (archWeight <= 0.001) {
            return currentTerrainY;
        }

        // Restrained domain warp curves tectonic chains into smooth arcs without folding contours into shear cliffs.
        double warpX = FastNoise.fbm2D((float) ((x + 9103) * 0.0018f), (float) ((z - 4211) * 0.0018f), 2) * 48.0;
        double warpZ = FastNoise.fbm2D((float) ((x - 6317) * 0.0018f), (float) ((z + 8843) * 0.0018f), 2) * 48.0;
        double dx = x + warpX;
        double dz = z + warpZ;

        // Sinuous arc belt concentrates islets into smooth dome chains with navigable deep straits in between.
        double spineRaw = FastNoise.fbm2D((float) (dx * 0.00145f), (float) (dz * 0.00145f), 2) / 0.32;
        double arcU = Mth.clamp(Math.abs(spineRaw) * 1.35, 0.0, 1.0);
        double arcRidge = 1.0 - (arcU * arcU * (3.0 - 2.0 * arcU));

        // Break the spine into smooth rounded islands, sandbars, and partial coral atoll rings without high-freq cuts.
        double clusterRaw = FastNoise.fbm2D(
                (float) ((dx + 17321) * 0.0045f), (float) ((dz - 11903) * 0.0045f), 2) / 0.32;
        double shoreJitter = (FastNoise.fbm2D((float) (x * 0.0085f), (float) (z * 0.0085f), 2) / 0.32) * 0.040;
        double ringRaw = FastNoise.fbm2D(
                (float) ((dx - 4512) * 0.0032f), (float) ((dz + 14821) * 0.0032f), 2) / 0.32;
        double ringSmoothAbs = Math.sqrt(ringRaw * ringRaw + 0.015);
        double ringV = Mth.clamp(Math.abs(ringSmoothAbs - 0.38) / 0.26, 0.0, 1.0);
        double atollRing = 0.12 * (1.0 - (ringV * ringV * (3.0 - 2.0 * ringV)));

        double baseSignal = (clusterRaw * 0.85) + (arcRidge * 0.44 - 0.18) + atollRing;
        // Confine small bay jitter to lower shores so island interiors never get pinched or corrugated
        double crownFade = 1.0 - hermite01((baseSignal - 0.20) / 0.25);
        double rawChainSignal = baseSignal + shoreJitter * crownFade;
        if (rawChainSignal <= -0.32) {
            return currentTerrainY;
        }

        // Compute target height BEFORE blending by archWeight so boundary fade lowers elevation smoothly
        // rather than compressing the nonlinear beach slope into an abrupt drop-off.
        double archTargetY = computeArchipelagoProfileY(x, z, rawChainSignal);
        double fillet = 6.0 * hermite01((rawChainSignal + 0.32) / 0.20);
        double raisedY = ElderwoodsBiomeSource.smoothMax(currentTerrainY, archTargetY, fillet);
        return Mth.lerp(archWeight, currentTerrainY, raisedY);
    }

    private static double computeArchipelagoProfileY(int x, int z, double signal) {
        if (signal < 0.06) {
            // Broad submarine coral & seagrass shoal skirt rising gently from shelf floor (Y=38..58)
            double t = hermite01((signal + 0.32) / 0.38);
            return Mth.lerp(t, 38.0, 58.0);
        }
        if (signal < 0.34) {
            // Wide shallow reef flats (Y=58..69) tapering softly into sandy bars & beaches (Y=70..73.4)
            double t = hermite01((signal - 0.06) / 0.28);
            return Mth.lerp(t, 58.0, 73.4);
        }

        // Smooth C1 Hermite vegetated islet dome (Y=73.4..80.4) with zero shoreline derivative kink
        double softDome = hermite01((signal - 0.34) / 0.72);
        double relief = (FastNoise.fbm3D((float) ((x + 511) * 0.0075f), 0f, (float) ((z - 917) * 0.0075f), 2) / 0.32)
                * 0.9 * softDome;
        return 73.4 + 7.0 * softDome + relief;
    }

    static double hermite01(double t) {
        double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }
}
