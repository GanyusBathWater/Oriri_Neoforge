package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.util.FastNoise;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic, thread-safe drainage network and stepped fluvial generator for
 * {@code oririmod:elder_valley}.
 * <p>
 * Traces continental drainage polylines downhill from foothill springs (outside
 * deserts/swamps/summits)
 * toward {@code the_silent_oceans}, assigns strictly non-increasing quantized
 * horizontal water reaches
 * ({@code waterY >= BASE_HEIGHT (72)}), and carves U-shaped alluvial valleys,
 * canyon gorges,
 * boulder-weir waterfall steps, gentle rapids, and coastal estuaries with solid
 * flood-contained levees.
 */
public final class ElderValleyTerrainHelper {

    public static final int SEA_LEVEL_Y = ElderwoodsChunkGenerator.BASE_HEIGHT; // 72
    public static final int MAX_HEADWATER_WATER_Y = 185;

    private static final int CELL_SIZE = 768;
    private static final double STEP_LEN = 26.0;
    private static final int MAX_TRACE_STEPS = 160; // ~4160 blocks max trace so rivers cross peninsulas to reach ocean
    private static final int MIN_VALID_STEPS = 12; // ~312 blocks minimum standalone river length
    private static final double CONFLUENCE_SNAP_DIST = 40.0;
    private static final int CONFLUENCE_FLAT_STEPS = 2; // prevent steps right at trunk confluence
    private static final int MAX_CACHE_ENTRIES = 2048;

    private static final int NUM_CANDIDATE_ANGLES = 9;
    private static final double MAX_TURN_RAD = Math.toRadians(32.0);

    public enum RiverStyle {
        /** Dramatic 2-block waterfall drops over rock weirs into deep plunge pools. */
        CASCADE,
        /** Broad calm boating pools joined by gentle 1-block gravel/boulder rapids. */
        RAPIDS,
        /**
         * Upper cascades transitioning to gentle meandering lowland pools toward the
         * estuary.
         */
        HYBRID
    }

    /**
     * Pure pre-river terrain & climate callback so path tracing never recurses into
     * river carving.
     */
    public interface BaseTerrainLookup {
        double sampleContinentalness(int x, int z);

        double sampleTemperature(int x, int z);

        double sampleHumidity(int x, int z);

        double sampleHighland(int x, int z);

        int sampleBaseHeight(int x, int z);

        double sampleIslandNoise(int x, int z);
    }

    /**
     * Reusable per-column output struct to keep hot inner chunk loops
     * allocation-free.
     */
    public static final class ValleyColumnInfo {
        public boolean hasValley;
        public boolean inWaterChannel;
        public boolean isProtectedRiverbed;
        public boolean isWaterfallStep;
        public boolean isWeirCrest;
        public boolean isCascadeTickCell;
        public boolean isPlungePool;
        public boolean isEstuary;
        public boolean inSourceLake;

        public int waterY;
        public int stepUpstreamWaterY;
        public int bedY;
        public int surfaceY;
        public int sourceLakeWaterY;

        public double valleyMask; // [0..1] terrain carving influence
        public double biomeMask; // [0..1] biome assignment strength
        public double distToWaterEdge; // signed distance (>0 on dry banks, <=0 inside channel)
        public double canopyDensity; // [0..1] riparian grove vs sunlit meadow variation
        public double sourceLakeX;
        public double sourceLakeZ;
        public double sourceLakeRadius;
        public long sourceLakeSeed;
        public RiverStyle style = RiverStyle.HYBRID;

        public void reset(int baseSurfaceY) {
            this.hasValley = false;
            this.inWaterChannel = false;
            this.isProtectedRiverbed = false;
            this.isWaterfallStep = false;
            this.isWeirCrest = false;
            this.isCascadeTickCell = false;
            this.isPlungePool = false;
            this.isEstuary = false;
            this.inSourceLake = false;
            this.waterY = SEA_LEVEL_Y;
            this.stepUpstreamWaterY = SEA_LEVEL_Y;
            this.bedY = baseSurfaceY;
            this.surfaceY = baseSurfaceY;
            this.sourceLakeWaterY = SEA_LEVEL_Y;
            this.valleyMask = 0.0;
            this.biomeMask = 0.0;
            this.distToWaterEdge = 999.0;
            this.canopyDensity = 0.0;
            this.sourceLakeX = 0.0;
            this.sourceLakeZ = 0.0;
            this.sourceLakeRadius = 0.0;
            this.sourceLakeSeed = 0L;
            this.style = RiverStyle.HYBRID;
        }

        public void copyFrom(ValleyColumnInfo src) {
            this.hasValley = src.hasValley;
            this.inWaterChannel = src.inWaterChannel;
            this.isProtectedRiverbed = src.isProtectedRiverbed;
            this.isWaterfallStep = src.isWaterfallStep;
            this.isWeirCrest = src.isWeirCrest;
            this.isCascadeTickCell = src.isCascadeTickCell;
            this.isPlungePool = src.isPlungePool;
            this.isEstuary = src.isEstuary;
            this.inSourceLake = src.inSourceLake;
            this.waterY = src.waterY;
            this.stepUpstreamWaterY = src.stepUpstreamWaterY;
            this.bedY = src.bedY;
            this.surfaceY = src.surfaceY;
            this.sourceLakeWaterY = src.sourceLakeWaterY;
            this.valleyMask = src.valleyMask;
            this.biomeMask = src.biomeMask;
            this.distToWaterEdge = src.distToWaterEdge;
            this.canopyDensity = src.canopyDensity;
            this.sourceLakeX = src.sourceLakeX;
            this.sourceLakeZ = src.sourceLakeZ;
            this.sourceLakeRadius = src.sourceLakeRadius;
            this.sourceLakeSeed = src.sourceLakeSeed;
            this.style = src.style;
        }
    }

    private static final class RiverNode {
        final double x;
        final double z;
        final double tx; // unit downstream tangent X
        final double tz; // unit downstream tangent Z
        final double arcDist;
        final double waterHalfWidth;
        final double valleyHalfWidth;
        final double baseDepth;
        final double estuaryFactor;
        final int waterY;
        final int stepDownAmount; // >0 when a step drop occurs immediately downstream of this segment
        final double canopyBias;

        RiverNode(double x, double z, double tx, double tz, double arcDist,
                double waterHalfWidth, double valleyHalfWidth, double baseDepth,
                double estuaryFactor, int waterY, int stepDownAmount, double canopyBias) {
            this.x = x;
            this.z = z;
            this.tx = tx;
            this.tz = tz;
            this.arcDist = arcDist;
            this.waterHalfWidth = waterHalfWidth;
            this.valleyHalfWidth = valleyHalfWidth;
            this.baseDepth = baseDepth;
            this.estuaryFactor = estuaryFactor;
            this.waterY = waterY;
            this.stepDownAmount = stepDownAmount;
            this.canopyBias = canopyBias;
        }
    }

    private static final class StepMarker {
        final double arcS;
        final int upstreamY;
        final int downstreamY;
        final int dropBlocks;

        StepMarker(double arcS, int upstreamY, int downstreamY, int dropBlocks) {
            this.arcS = arcS;
            this.upstreamY = upstreamY;
            this.downstreamY = downstreamY;
            this.dropBlocks = dropBlocks;
        }
    }

    private static final class RiverPolyline {
        final RiverNode[] nodes;
        final StepMarker[] steps;
        final RiverStyle style;
        final boolean mergedAtTail;
        final boolean endedInOcean;
        final double lakeX, lakeZ, lakeRadius, lakeRimSpan;
        final long lakeSeed;
        final double minX, maxX, minZ, maxZ;

        // 8-segment chunk bounds for O(1) segment skipping inside long polylines
        final double[] groupMinX, groupMaxX, groupMinZ, groupMaxZ;

        RiverPolyline(RiverNode[] nodes, StepMarker[] steps, RiverStyle style, boolean mergedAtTail,
                boolean endedInOcean, double lakeX, double lakeZ, double lakeRadius, double lakeRimSpan,
                long lakeSeed) {
            this.nodes = nodes;
            this.steps = steps;
            this.style = style;
            this.mergedAtTail = mergedAtTail;
            this.endedInOcean = endedInOcean;
            this.lakeX = lakeX;
            this.lakeZ = lakeZ;
            this.lakeRadius = lakeRadius;
            this.lakeRimSpan = lakeRimSpan;
            this.lakeSeed = lakeSeed;

            double lakePad = lakeRadius + lakeRimSpan + 24.0;
            double x0 = lakeX - lakePad;
            double x1 = lakeX + lakePad;
            double z0 = lakeZ - lakePad;
            double z1 = lakeZ + lakePad;
            for (RiverNode n : nodes) {
                double pad = n.valleyHalfWidth + 24.0;
                if (n.x - pad < x0)
                    x0 = n.x - pad;
                if (n.x + pad > x1)
                    x1 = n.x + pad;
                if (n.z - pad < z0)
                    z0 = n.z - pad;
                if (n.z + pad > z1)
                    z1 = n.z + pad;
            }
            this.minX = x0;
            this.maxX = x1;
            this.minZ = z0;
            this.maxZ = z1;

            int segCount = Math.max(1, nodes.length - 1);
            int groups = (segCount + 7) >> 3;
            this.groupMinX = new double[groups];
            this.groupMaxX = new double[groups];
            this.groupMinZ = new double[groups];
            this.groupMaxZ = new double[groups];
            for (int g = 0; g < groups; g++) {
                double gx0 = Double.POSITIVE_INFINITY, gx1 = Double.NEGATIVE_INFINITY;
                double gz0 = Double.POSITIVE_INFINITY, gz1 = Double.NEGATIVE_INFINITY;
                int end = Math.min(segCount, (g + 1) * 8);
                for (int i = g * 8; i < end; i++) {
                    RiverNode a = nodes[i];
                    RiverNode b = nodes[i + 1];
                    double pad = Math.max(a.valleyHalfWidth, b.valleyHalfWidth) + 24.0;
                    gx0 = Math.min(gx0, Math.min(a.x, b.x) - pad);
                    gx1 = Math.max(gx1, Math.max(a.x, b.x) + pad);
                    gz0 = Math.min(gz0, Math.min(a.z, b.z) - pad);
                    gz1 = Math.max(gz1, Math.max(a.z, b.z) + pad);
                }
                if (g == 0 && lakeRadius > 0.0) {
                    gx0 = Math.min(gx0, lakeX - lakePad);
                    gx1 = Math.max(gx1, lakeX + lakePad);
                    gz0 = Math.min(gz0, lakeZ - lakePad);
                    gz1 = Math.max(gz1, lakeZ + lakePad);
                }
                groupMinX[g] = gx0;
                groupMaxX[g] = gx1;
                groupMinZ[g] = gz0;
                groupMaxZ[g] = gz1;
            }
        }

        boolean boundsContain(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    private static final class RiverRegion {
        final RiverPolyline river; // null if no valid river originates in this macro cell

        RiverRegion(RiverPolyline river) {
            this.river = river;
        }
    }

    private static final RiverRegion EMPTY_REGION = new RiverRegion(null);

    private volatile long cachedSeed = Long.MIN_VALUE;
    private final ConcurrentHashMap<Long, RiverRegion> regionCache = new ConcurrentHashMap<>(512);

    private final ThreadLocal<ValleyColumnInfo> threadColumnScratch = ThreadLocal.withInitial(ValleyColumnInfo::new);
    private final ThreadLocal<List<RiverPolyline>> nearbyScratch = ThreadLocal.withInitial(() -> new ArrayList<>(16));

    public ValleyColumnInfo getScratchInfo() {
        return threadColumnScratch.get();
    }

    public void resetForSeed(long seed) {
        if (this.cachedSeed != seed) {
            synchronized (this) {
                if (this.cachedSeed != seed) {
                    this.regionCache.clear();
                    this.cachedSeed = seed;
                }
            }
        }
    }

    /**
     * Evaluates the Elder Valley hydrological channel, waterfall steps, and
     * U-shaped valley carving
     * on top of a pre-river terrain elevation {@code baseSurfaceY}.
     */
    public ValleyColumnInfo evaluateColumn(
            long seed, int x, int z, int baseSurfaceY,
            double contNoise, double tempNoise, double humNoise,
            BaseTerrainLookup lookup, ValleyColumnInfo out) {

        resetForSeed(seed);
        out.reset(baseSurfaceY);

        // Quick global exclusion: open deep abyss never hosts Elder Valleys
        if (contNoise < -0.16) {
            return out;
        }

        List<RiverPolyline> nearby = nearbyScratch.get();
        nearby.clear();

        int centerGx = Math.floorDiv(x, CELL_SIZE);
        int centerGz = Math.floorDiv(z, CELL_SIZE);

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                RiverRegion region = getOrBuildRegion(seed, centerGx + dx, centerGz + dz, lookup);
                if (region.river != null && region.river.boundsContain(x, z)) {
                    nearby.add(region.river);
                }
            }
        }

        if (nearby.isEmpty()) {
            return out;
        }

        // Subtly warp query coordinate for natural local sinuosity; zero inside
        // estuaries to preserve smooth mouths
        double lateralWarpX = (FastNoise.fbm2D((float) ((x + 713) * 0.011f), (float) ((z - 941) * 0.011f), 2) / 0.32)
                * 3.8;
        double lateralWarpZ = (FastNoise.fbm2D((float) ((x - 521) * 0.011f), (float) ((z + 419) * 0.011f), 2) / 0.32)
                * 3.8;

        // Protect ONLY detached pelagic islands well out in open sea with a wide gentle erosion ramp
        // hitting full protection smoothly by cont <= -0.032 & island > 0.28 (zero sharp estuary walls, zero sliced islands!)
        double islandProtect = 0.0;
        if (contNoise < -0.018) {
            double islandNoise = lookup.sampleIslandNoise(x, z);
            if (islandNoise > 0.18) {
                islandProtect = hermite01((-0.018 - contNoise) / 0.014) * hermite01((islandNoise - 0.18) / 0.10);
            }
        }
        double islandFade = 1.0 - islandProtect;
        if (contNoise < -0.050 && baseSurfaceY <= SEA_LEVEL_Y - 5) {
            return out;
        }

        boolean hasAnyValley = false;
        boolean anyWaterChannel = false;
        int bestWaterLocalY = Integer.MAX_VALUE;
        int bestWaterUpstreamY = SEA_LEVEL_Y;
        double bestWaterTargetY = baseSurfaceY;
        boolean bestInCascade = false;
        boolean bestInWeir = false;
        boolean bestCascadeTick = false;
        boolean bestInPlunge = false;

        boolean hasAnyDry = false;
        double combinedDryY = baseSurfaceY;
        double dryShoreFloor = -999.0;

        double maxVNorm = 0.0;
        double maxBNorm = 0.0;
        double minSignedEdgeDist = Double.MAX_VALUE;
        double bestEstuary = 0.0;
        double bestCanopyBias = 0.0;
        RiverStyle bestStyle = RiverStyle.HYBRID;
        double bestScore = Double.MAX_VALUE;

        boolean bestInSourceLake = false;
        double bestSourceLakeX = 0.0;
        double bestSourceLakeZ = 0.0;
        double bestSourceLakeRadius = 0.0;
        int bestSourceLakeWaterY = SEA_LEVEL_Y;
        long bestSourceLakeSeed = 0L;
        double bestSourceLakeDist = Double.MAX_VALUE;

        int dryCurveCount = 0;

        for (int rIdx = 0; rIdx < nearby.size(); rIdx++) {
            RiverPolyline river = nearby.get(rIdx);
            RiverNode[] nodes = river.nodes;
            int segCount = nodes.length - 1;
            int groups = river.groupMinX.length;

            double localMinDistSq = Double.MAX_VALUE;
            int localSegIdx = 0;
            double localSegT = 0.0;

            for (int g = 0; g < groups; g++) {
                if (x < river.groupMinX[g] || x > river.groupMaxX[g]
                        || z < river.groupMinZ[g] || z > river.groupMaxZ[g]) {
                    continue;
                }
                int segStart = g * 8;
                int segEnd = Math.min(segCount, segStart + 8);
                for (int i = segStart; i < segEnd; i++) {
                    RiverNode a = nodes[i];
                    RiverNode b = nodes[i + 1];

                    double wx = x + lateralWarpX * (1.0 - 0.65 * a.estuaryFactor);
                    double wz = z + lateralWarpZ * (1.0 - 0.65 * a.estuaryFactor);

                    double abx = b.x - a.x;
                    double abz = b.z - a.z;
                    double lenSq = abx * abx + abz * abz;
                    double t = (lenSq > 1e-6)
                            ? Mth.clamp(((wx - a.x) * abx + (wz - a.z) * abz) / lenSq, 0.0, 1.0)
                            : 0.0;
                    double px = a.x + t * abx;
                    double pz = a.z + t * abz;
                    double dSq = (wx - px) * (wx - px) + (wz - pz) * (wz - pz);

                    if (dSq < localMinDistSq) {
                        localMinDistSq = dSq;
                        localSegIdx = i;
                        localSegT = t;
                    }
                }
            }

            if (localMinDistSq == Double.MAX_VALUE) {
                continue;
            }

            // Find secondary non-adjacent reach around horseshoe U-bends (arc separation >= 110 blocks)
            // minimizing signed distance inside its valley so opposite arms never clip discontinuously!
            double primaryArcS = Mth.lerp(localSegT, nodes[localSegIdx].arcDist, nodes[localSegIdx + 1].arcDist);
            int altSegIdx = -1;
            double altSegT = 0.0;
            double altDist = Double.MAX_VALUE;
            double altBestScore = 0.0;
            if (segCount >= 8) {
                for (int g = 0; g < groups; g++) {
                    if (x < river.groupMinX[g] || x > river.groupMaxX[g]
                            || z < river.groupMinZ[g] || z > river.groupMaxZ[g]) {
                        continue;
                    }
                    int segStart = g * 8;
                    int segEnd = Math.min(segCount, segStart + 8);
                    for (int i = segStart; i < segEnd; i++) {
                        RiverNode a = nodes[i];
                        RiverNode b = nodes[i + 1];
                        double arcSep = Math.abs(a.arcDist - primaryArcS);
                        if (arcSep < 140.0
                                || Math.hypot(a.x - nodes[localSegIdx].x, a.z - nodes[localSegIdx].z) >= 0.72 * arcSep) {
                            continue;
                        }
                        double wx = x + lateralWarpX * (1.0 - 0.65 * a.estuaryFactor);
                        double wz = z + lateralWarpZ * (1.0 - 0.65 * a.estuaryFactor);
                        double abx = b.x - a.x;
                        double abz = b.z - a.z;
                        double lenSq = abx * abx + abz * abz;
                        double t = (lenSq > 1e-6)
                                ? Mth.clamp(((wx - a.x) * abx + (wz - a.z) * abz) / lenSq, 0.0, 1.0)
                                : 0.0;
                        double px = a.x + t * abx;
                        double pz = a.z + t * abz;
                        double d = Math.hypot(wx - px, wz - pz);
                        double vh = Mth.lerp(t, a.valleyHalfWidth, b.valleyHalfWidth);
                        double signed = d - vh;
                        if (signed < altBestScore) {
                            altBestScore = signed;
                            altSegIdx = i;
                            altSegT = t;
                            altDist = d;
                        }
                    }
                }
            }

            RiverNode n0 = nodes[localSegIdx];
            RiverNode n1 = nodes[localSegIdx + 1];
            double dist = Math.sqrt(localMinDistSq);
            double valleyHalfW = Mth.lerp(localSegT, n0.valleyHalfWidth, n1.valleyHalfWidth);
            if (dist >= valleyHalfW && altSegIdx >= 0) {
                localSegIdx = altSegIdx;
                localSegT = altSegT;
                dist = altDist;
                n0 = nodes[localSegIdx];
                n1 = nodes[localSegIdx + 1];
                valleyHalfW = Mth.lerp(localSegT, n0.valleyHalfWidth, n1.valleyHalfWidth);
                altSegIdx = -1;
            }
            double waterHalfW = Mth.lerp(localSegT, n0.waterHalfWidth, n1.waterHalfWidth);
            double baseDepth = Mth.lerp(localSegT, n0.baseDepth, n1.baseDepth);
            double estuary = Mth.lerp(localSegT, n0.estuaryFactor, n1.estuaryFactor);
            double arcS = Mth.lerp(localSegT, n0.arcDist, n1.arcDist);

            double widthNoise = (FastNoise.fbm2D((float) (x * 0.018f), (float) (z * 0.018f), 2) / 0.32) * 1.6;
            double effWaterHalfW = Math.max(3.2, waterHalfW + widthNoise * (1.0 - 0.5 * estuary));
            double riverPastWater = dist - effWaterHalfW;

            double distLake = 0.0;
            double effLakeWaterR = waterHalfW;
            double lakePastWater = Double.MAX_VALUE;
            boolean hasLakeInfluence = false;
            double lakeVNorm = 0.0;
            double lakeBNorm = 0.0;
            if (river.lakeRadius > 0.0) {
                double wxLake = x + lateralWarpX * 0.55;
                double wzLake = z + lateralWarpZ * 0.55;
                distLake = Math.hypot(wxLake - river.lakeX, wzLake - river.lakeZ);
                double lakeRimNoise = (FastNoise.fbm2D((float) (x * 0.025f), (float) (z * 0.025f), 2) / 0.32) * 2.5;
                effLakeWaterR = Math.max(8.0, river.lakeRadius + lakeRimNoise);
                double effLakeValleyR = effLakeWaterR + river.lakeRimSpan;
                lakePastWater = distLake - effLakeWaterR;
                if (distLake < effLakeValleyR) {
                    hasLakeInfluence = true;
                    lakeVNorm = 1.0 - Mth.clamp(lakePastWater / river.lakeRimSpan, 0.0, 1.0);
                    double lakeBRad = Math.max(effLakeWaterR + 10.0, effLakeValleyR * 0.78);
                    lakeBNorm = 1.0 - Mth.clamp((distLake - (effLakeWaterR + 4.0)) / Math.max(1.0, lakeBRad - (effLakeWaterR + 4.0)), 0.0, 1.0);
                }
            }

            if (dist >= valleyHalfW && !hasLakeInfluence) {
                continue;
            }

            double oceanFade = 1.0;
            if (contNoise < -0.025 && baseSurfaceY <= SEA_LEVEL_Y - 3) {
                oceanFade = hermite01((contNoise - (-0.060)) / 0.035);
                RiverNode lastNode = nodes[nodes.length - 1];
                double remS = lastNode.arcDist - arcS;
                if (river.endedInOcean && remS < 26.0) {
                    oceanFade *= hermite01(remS / 26.0);
                }
            }
            double regionFade = oceanFade * islandFade;
            if (regionFade <= 0.005) {
                continue;
            }

            double midX = Mth.lerp(localSegT, n0.x, n1.x);
            double midZ = Mth.lerp(localSegT, n0.z, n1.z);
            double crossProj = (x - midX) * (-n0.tz) + (z - midZ) * n0.tx;
            double crossU = Mth.clamp(crossProj / Math.max(1.0, waterHalfW), -1.0, 1.0);

            double signedEdge = hasLakeInfluence ? smoothMin(riverPastWater, lakePastWater, 6.0) : riverPastWater;
            boolean lakeDominatesChannel = hasLakeInfluence && (lakePastWater <= 0.5 && lakePastWater < riverPastWater);
            if (lakeDominatesChannel) {
                baseDepth = Math.max(baseDepth, 3.8);
            }

            int localWaterY;
            int upstreamStepY;
            boolean inWeirCrest = false;
            boolean inCascadeDrop = false;
            boolean inPlungePool = false;
            boolean cascadeTick = false;
            double plungeDepthBonus = 0.0;
            double weirLiftOverride = -999.0;
            int stepDropAmt = 0;

            double archCurve = (crossU * crossU) * 1.15;
            double weirJitter = (FastNoise.noise2D((float) (x * 0.16f), (float) (z * 0.16f))) * 0.45;
            double archedS = arcS - archCurve + weirJitter;

            StepMarker nearestStep = null;
            double nearestAbsDs = Double.MAX_VALUE;
            for (StepMarker sm : river.steps) {
                double ds = archedS - sm.arcS;
                double absDs = Math.abs(ds);
                if (absDs < nearestAbsDs) {
                    nearestAbsDs = absDs;
                    nearestStep = sm;
                }
            }

            if (nearestStep != null && nearestAbsDs < 22.0 && !lakeDominatesChannel) {
                double ds = archedS - nearestStep.arcS;
                stepDropAmt = nearestStep.dropBlocks;
                double cascadeRunLen = (stepDropAmt <= 1) ? 1.2 : Math.min(9.5, 1.2 + (stepDropAmt - 1) * 0.65);
                if (ds < 0.0) {
                    localWaterY = nearestStep.upstreamY;
                    upstreamStepY = nearestStep.upstreamY;
                    if (ds >= -2.6) {
                        inWeirCrest = true;
                        double crestT = hermite01((ds + 2.6) / 2.6);
                        weirLiftOverride = Mth.lerp(crestT, nearestStep.upstreamY - 2.0, nearestStep.upstreamY - 1.0);
                        if (ds >= -1.1) {
                            cascadeTick = true;
                        }
                    }
                } else if (ds < cascadeRunLen) {
                    upstreamStepY = nearestStep.upstreamY;
                    double prevFrac = Mth.clamp((ds - 0.9) / cascadeRunLen, 0.0, 1.0);
                    double curFrac = Mth.clamp(ds / cascadeRunLen, 0.0, 1.0);
                    int prevDropped = (ds <= 0.9) ? 0
                            : ((stepDropAmt <= 1) ? 1
                                    : Math.min(stepDropAmt - 1, (int) Math.floor(prevFrac * stepDropAmt)));
                    int curDropped = (stepDropAmt <= 1) ? 1
                            : Math.min(stepDropAmt - 1, 1 + (int) Math.floor(curFrac * (stepDropAmt - 1)));
                    localWaterY = Math.max(nearestStep.downstreamY, nearestStep.upstreamY - prevDropped);
                    inCascadeDrop = true;
                    cascadeTick = true;
                    weirLiftOverride = Math.max(nearestStep.downstreamY, nearestStep.upstreamY - 1 - curDropped);
                } else {
                    upstreamStepY = nearestStep.upstreamY;
                    localWaterY = nearestStep.downstreamY;
                    if (ds < cascadeRunLen + 8.0) {
                        inPlungePool = true;
                        double pFrac = (ds - cascadeRunLen) / 8.0;
                        plungeDepthBonus = Math.sin(pFrac * Math.PI) * Math.min(4.5, 1.3 + stepDropAmt * 0.45);
                    }
                }
            } else {
                localWaterY = lakeDominatesChannel ? nodes[0].waterY
                        : ((localSegT < 0.5) ? n0.waterY : n1.waterY);
                upstreamStepY = localWaterY;
            }

            // Start bank descent slightly past weir crest (+0.65) so jittered cascade lips NEVER leak laterally
            double smoothArchedS = arcS - archCurve;
            double smoothBankWaterY = nodes[0].waterY;
            for (StepMarker sm : river.steps) {
                double bankTransitionLen = Math.max(10.0, sm.dropBlocks * 2.8 + 8.5);
                double tDrop = hermite01((smoothArchedS - (sm.arcS + 0.65)) / bankTransitionLen);
                smoothBankWaterY -= sm.dropBlocks * tDrop;
            }

            boolean inRiverWater = riverPastWater <= 0.0;
            boolean inLakeWater = hasLakeInfluence && lakePastWater <= 0.0;
            boolean thisWater = (inRiverWater || inLakeWater)
                    && !(contNoise < -0.030 && baseSurfaceY < localWaterY - 2);

            double thisTargetY;
            if (thisWater) {
                double riverU = inRiverWater ? (dist / Math.max(1.0, effWaterHalfW)) : 1.0;
                double lakeU = inLakeWater ? (distLake / Math.max(1.0, effLakeWaterR)) : 1.0;
                double u = Mth.clamp(Math.min(riverU, lakeU), 0.0, 1.0);
                double channelDepth;
                if (u >= 0.64) {
                    double shelfT = (1.0 - u) / 0.36;
                    channelDepth = 0.45 + hermite01(shelfT) * 1.15;
                } else {
                    double coreT = (0.64 - u) / 0.64;
                    double deepMax = Math.max(2.6, baseDepth + plungeDepthBonus);
                    channelDepth = Mth.lerp(hermite01(coreT), 1.60, deepMax);
                }
                double bedTarget = localWaterY - channelDepth;
                if (weirLiftOverride > -900.0 && u <= 0.94 && !inLakeWater) {
                    double rockShoulder = (Math.abs(crossU) > 0.72) ? 0.35 : 0.0;
                    bedTarget = Mth.lerp(hermite01((0.94 - u) / 0.22), bedTarget,
                            Math.max(bedTarget, weirLiftOverride + rockShoulder));
                    bedTarget = Math.min(bedTarget, localWaterY - 0.85);
                }
                thisTargetY = Mth.lerp(regionFade, baseSurfaceY, bedTarget);
                if (contNoise < 0.015 && baseSurfaceY < thisTargetY) {
                    thisTargetY = baseSurfaceY;
                }
                if (thisTargetY >= localWaterY) {
                    thisWater = false;
                }
            } else {
                double valleySpan = Math.max(1.0, valleyHalfW - effWaterHalfW);
                double carvedY = (riverPastWater < valleySpan)
                        ? evaluateDryValleyY(x, z, baseSurfaceY, Math.max(0.0, riverPastWater), valleySpan, smoothBankWaterY, estuary)
                        : (double) baseSurfaceY;
                if (hasLakeInfluence) {
                    double lakeCarvedY = evaluateDryValleyY(x, z, baseSurfaceY, Math.max(0.0, lakePastWater),
                            river.lakeRimSpan, nodes[0].waterY, 0.0);
                    double neckFloor = Math.min(baseSurfaceY, smoothBankWaterY + 0.35);
                    carvedY = Math.max(neckFloor, smoothCarveMin(carvedY, lakeCarvedY, baseSurfaceY, 6.0));
                }
                if (altSegIdx >= 0) {
                    RiverNode an0 = nodes[altSegIdx];
                    RiverNode an1 = nodes[altSegIdx + 1];
                    double aVH = Mth.lerp(altSegT, an0.valleyHalfWidth, an1.valleyHalfWidth);
                    double aWH = Mth.lerp(altSegT, an0.waterHalfWidth, an1.waterHalfWidth);
                    double aEst = Mth.lerp(altSegT, an0.estuaryFactor, an1.estuaryFactor);
                    double aArcS = Mth.lerp(altSegT, an0.arcDist, an1.arcDist);
                    double aEffWH = Math.max(3.2, aWH + widthNoise * (1.0 - 0.5 * aEst));
                    double aPastW = Math.max(0.0, altDist - aEffWH);
                    double aSpan = Math.max(1.0, aVH - aEffWH);
                    if (aPastW < aSpan) {
                        double aSmoothW = nodes[0].waterY;
                        for (StepMarker sm : river.steps) {
                            double len = Math.max(10.0, sm.dropBlocks * 2.8 + 8.5);
                            aSmoothW -= sm.dropBlocks * hermite01((aArcS - (sm.arcS + 0.65)) / len);
                        }
                        double altCarvedY = evaluateDryValleyY(x, z, baseSurfaceY, aPastW, aSpan, aSmoothW, aEst);
                        carvedY = smoothCarveMin(carvedY, altCarvedY, baseSurfaceY, 5.5);
                        double aHillDepth = Math.max(0.0, (double) baseSurfaceY - aSmoothW);
                        double aCanyonFactor = hermite01((aHillDepth - 11.0) / 14.0);
                        double aEffSpan = Math.max(Math.min(10.0, aSpan), aSpan * (1.0 - 0.48 * aCanyonFactor));
                        double aEffVH = aEffWH + aEffSpan;
                        double altV = 1.0 - Mth.clamp(aPastW / aEffSpan, 0.0, 1.0);
                        maxVNorm = Math.max(maxVNorm, altV);
                        double aBRad = Math.max(aEffWH + 10.0, aEffVH * 0.78);
                        maxBNorm = Math.max(maxBNorm, 1.0 - Mth.clamp((altDist - (aEffWH + 4.0)) / Math.max(1.0, aBRad - (aEffWH + 4.0)), 0.0, 1.0));
                    }
                }
                thisTargetY = Mth.lerp(regionFade, baseSurfaceY, carvedY);
            }

            double hillDepth = Math.max(0.0, (double) baseSurfaceY - smoothBankWaterY);
            double canyonFactor = hermite01((hillDepth - 11.0) / 14.0);
            double rawValleySpan = Math.max(1.0, valleyHalfW - effWaterHalfW);
            double effValleySpan = Math.max(Math.min(10.0, rawValleySpan), rawValleySpan * (1.0 - 0.48 * canyonFactor));
            double effValleyHalfW = effWaterHalfW + effValleySpan;
            hasAnyValley = true;
            double riverVNorm = 1.0 - Mth.clamp(riverPastWater / effValleySpan, 0.0, 1.0);
            maxVNorm = Math.max(maxVNorm, Math.max(riverVNorm, lakeVNorm));
            double bRad = Math.max(effWaterHalfW + 10.0, effValleyHalfW * 0.78);
            double riverBNorm = 1.0
                    - Mth.clamp((dist - (effWaterHalfW + 4.0)) / Math.max(1.0, bRad - (effWaterHalfW + 4.0)), 0.0, 1.0);
            maxBNorm = Math.max(maxBNorm, Math.max(riverBNorm, lakeBNorm));
            if (signedEdge < minSignedEdgeDist) {
                minSignedEdgeDist = signedEdge;
            }

            if (hasLakeInfluence && river.lakeRadius > 0.0) {
                double rawDistLake = Math.hypot(x - river.lakeX, z - river.lakeZ);
                if (rawDistLake <= river.lakeRadius * 0.88 && rawDistLake < bestSourceLakeDist) {
                    bestInSourceLake = true;
                    bestSourceLakeDist = rawDistLake;
                    bestSourceLakeX = river.lakeX;
                    bestSourceLakeZ = river.lakeZ;
                    bestSourceLakeRadius = river.lakeRadius;
                    bestSourceLakeWaterY = nodes[0].waterY;
                    bestSourceLakeSeed = river.lakeSeed;
                }
            }

            if (thisWater) {
                if (!anyWaterChannel || localWaterY < bestWaterLocalY || (localWaterY == bestWaterLocalY && thisTargetY < bestWaterTargetY)) {
                    anyWaterChannel = true;
                    bestWaterLocalY = localWaterY;
                    bestWaterUpstreamY = upstreamStepY;
                    bestWaterTargetY = thisTargetY;
                    bestInCascade = inCascadeDrop;
                    bestInWeir = inWeirCrest;
                    bestCascadeTick = cascadeTick;
                    bestInPlunge = inPlungePool;
                    bestEstuary = estuary;
                    bestCanopyBias = n0.canopyBias;
                    bestStyle = river.style;
                }
            } else {
                combinedDryY = hasAnyDry ? smoothCarveMin(combinedDryY, thisTargetY, baseSurfaceY, 5.5) : thisTargetY;
                hasAnyDry = true;
                dryCurveCount++;
                double bankNearWaterRef = (hasLakeInfluence && lakePastWater < riverPastWater) ? nodes[0].waterY : smoothBankWaterY;
                double minEdge = hasLakeInfluence ? Math.min(riverPastWater, lakePastWater) : riverPastWater;
                if (minEdge >= -0.25 && minEdge < 3.5) {
                    dryShoreFloor = Math.max(dryShoreFloor, Math.min(baseSurfaceY, bankNearWaterRef + 0.35));
                }
                if (!anyWaterChannel && thisTargetY < bestScore) {
                    bestScore = thisTargetY;
                    bestWaterLocalY = localWaterY;
                    bestWaterUpstreamY = upstreamStepY;
                    bestInWeir = inWeirCrest;
                    bestInPlunge = inPlungePool;
                    bestEstuary = estuary;
                    bestCanopyBias = n0.canopyBias;
                    bestStyle = river.style;
                }
            }
        }

        if (!hasAnyValley) {
            return out;
        }

        double dryFinal = (dryCurveCount > 1 && dryShoreFloor > -900.0)
                ? Math.max(dryShoreFloor, combinedDryY)
                : combinedDryY;
        double targetY = anyWaterChannel ? bestWaterTargetY : Math.min(baseSurfaceY, dryFinal);
        boolean insideWater = anyWaterChannel;
        int localWaterY = bestWaterLocalY;

        double canopyMacro = FastNoise.fbm2D((float) ((x + 1907) * 0.0042f), (float) ((z - 2803) * 0.0042f), 2) / 0.32;
        double canopyDensity = Mth.clamp((Mth.lerp(0.55, canopyMacro, bestCanopyBias) + 1.0) * 0.5, 0.0, 1.0);

        out.hasValley = true;
        out.inWaterChannel = insideWater;
        out.isProtectedRiverbed = minSignedEdgeDist <= 6.0;
        out.isWaterfallStep = bestInCascade && insideWater;
        out.isWeirCrest = bestInWeir && (insideWater || (minSignedEdgeDist <= 1.1 && targetY <= localWaterY + 0.5));
        out.isCascadeTickCell = bestCascadeTick && insideWater;
        out.isPlungePool = bestInPlunge;
        out.isEstuary = bestEstuary > 0.45;
        out.inSourceLake = bestInSourceLake;
        out.waterY = localWaterY;
        out.stepUpstreamWaterY = bestWaterUpstreamY;
        out.bedY = insideWater ? (int) Math.floor(targetY) : (int) Math.round(targetY);
        out.surfaceY = insideWater ? Math.min((int) Math.floor(targetY), localWaterY - 1) : (int) Math.round(targetY);
        out.sourceLakeWaterY = bestSourceLakeWaterY;
        out.valleyMask = hermite01(maxVNorm);
        out.biomeMask = hermite01(maxBNorm * 1.15);
        out.distToWaterEdge = minSignedEdgeDist;
        out.canopyDensity = canopyDensity;
        out.sourceLakeX = bestSourceLakeX;
        out.sourceLakeZ = bestSourceLakeZ;
        out.sourceLakeRadius = bestSourceLakeRadius;
        out.sourceLakeSeed = bestSourceLakeSeed;
        out.style = bestStyle;
        return out;
    }

    private static double evaluateDryValleyY(int x, int z, int baseSurfaceY,
            double dPastWater, double valleySpan, double smoothWaterY, double estuary) {
        double hillDepth = Math.max(0.0, (double) baseSurfaceY - smoothWaterY);
        double canyonFactor = hermite01((hillDepth - 11.0) / 14.0);
        double effSpan = Math.max(Math.min(10.0, valleySpan), valleySpan * (1.0 - 0.48 * canyonFactor));
        if (dPastWater >= effSpan) {
            return baseSurfaceY;
        }

        double wFrac = Mth.clamp(dPastWater / effSpan, 0.0, 1.0);
        double bankLipY = smoothWaterY + 0.35;
        double floodUndul = (FastNoise.fbm2D((float) (x * 0.022f), (float) (z * 0.022f), 2) / 0.32 + 1.0) * 1.15;
        double lowValleyTarget = bankLipY + 2.5 + floodUndul * (1.0 - 0.65 * estuary);

        double bankCurve = hermite01(dPastWater / 12.0);
        double innerShelfY = Mth.lerp(bankCurve, bankLipY, lowValleyTarget);
        double smoothWall = Math.pow(hermite01(wFrac), 1.35);
        double canyonWall = wFrac * wFrac * (2.0 - wFrac * wFrac);
        double outerBlend = Mth.lerp(canyonFactor, smoothWall, canyonWall);
        double rawProfileY = Mth.lerp(outerBlend, innerShelfY, baseSurfaceY);

        double carvedY = Math.min(baseSurfaceY, rawProfileY);
        double distFromRim = Math.max(0.0, effSpan - dPastWater);
        double safeSlope = Math.max(0.65, (baseSurfaceY - bankLipY) / Math.max(16.0, effSpan) * 1.15);
        double maxSafeDrop = distFromRim * safeSlope * hermite01(distFromRim / 12.0);
        return Math.max(carvedY, baseSurfaceY - maxSafeDrop);
    }

    private static void inWaterfallStepSet(ValleyColumnInfo out) {
        out.isWaterfallStep = true;
    }

    /**
     * Thread-safe region lookup with acyclic chessboard pass resolution (0a -> 0b
     * -> 1 -> 2 -> 3).
     */
    private RiverRegion getOrBuildRegion(long seed, int gx, int gz, BaseTerrainLookup lookup) {
        long key = packRegionKey(gx, gz);
        RiverRegion cached = regionCache.get(key);
        if (cached != null) {
            return cached;
        }

        if (regionCache.size() > MAX_CACHE_ENTRIES) {
            regionCache.clear();
        }

        RiverRegion built = buildRegion(seed, gx, gz, lookup);
        RiverRegion prev = regionCache.putIfAbsent(key, built);
        return prev != null ? prev : built;
    }

    private RiverRegion buildRegion(long seed, int gx, int gz, BaseTerrainLookup lookup) {
        int pass = getCellPass(gx, gz);
        long cellHash = mix64(seed ^ (gx * 0x9E3779B97F4A7C15L) ^ (gz * 0xBF58476D1CE4E5B9L));

        // Keep tributary density natural (odd checkerboard diagonals sparse by 28%)
        // while higher passes always see lower passes
        boolean isTributaryParity = ((gx + gz) & 1) != 0;
        if (isTributaryParity && (Math.floorMod(cellHash >>> 8, 100L) < 28)) {
            return EMPTY_REGION;
        }

        // Gather ALL already-built lower-pass rivers (strictly acyclic 3x3 ordering
        // across 9 passes)
        // so tributaries merge cleanly and non-merging channels steer clear instead of
        // colliding at mismatched Y!
        List<RiverPolyline> lowerPassTrunks = (pass > 0) ? new ArrayList<>(16) : null;
        if (pass > 0) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    int ngx = gx + dx;
                    int ngz = gz + dz;
                    if (getCellPass(ngx, ngz) < pass) {
                        RiverRegion nr = getOrBuildRegion(seed, ngx, ngz, lookup);
                        if (nr.river != null) {
                            lowerPassTrunks.add(nr.river);
                        }
                    }
                }
            }
        }

        // Stratified 8x8 / 5x5 jittered grid inside cell core: locate high Elder
        // Highlands plateau bowls
        // suitable for hosting scenic Highland headwater lakes feeding mountain
        // waterfalls downhill.
        double bestSpringX = 0.0;
        double bestSpringZ = 0.0;
        double bestScore = Double.NEGATIVE_INFINITY;
        double bestLakeRadius = 0.0;
        int bestRimFloor = 0;
        boolean foundSpring = false;

        long probeHash = cellHash;
        boolean isTrunkPass = !isTributaryParity;
        int gridN = isTrunkPass ? 8 : 5;
        double margin = 64.0;
        double stepCell = (CELL_SIZE - 2.0 * margin) / gridN;
        for (int gxIdx = 0; gxIdx < gridN; gxIdx++) {
            for (int gzIdx = 0; gzIdx < gridN; gzIdx++) {
                probeHash = mix64(probeHash ^ 0x94D049BB133111EBL);
                double jx = Math.floorMod(probeHash, 1000L) / 1000.0;
                double jz = Math.floorMod(probeHash >>> 16, 1000L) / 1000.0;
                double sx = gx * (double) CELL_SIZE + margin + (gxIdx + jx) * stepCell;
                double sz = gz * (double) CELL_SIZE + margin + (gzIdx + jz) * stepCell;
                int ix = (int) Math.round(sx);
                int iz = (int) Math.round(sz);

                double cont = lookup.sampleContinentalness(ix, iz);
                double temp = lookup.sampleTemperature(ix, iz);
                double hum = lookup.sampleHumidity(ix, iz);
                double high = lookup.sampleHighland(ix, iz);
                int h = lookup.sampleBaseHeight(ix, iz);

                // Strictly originate on Elder Highlands Meadow / Forest mountain benches (never on sheer Summit spires)
                if (isTrunkPass) {
                    if (cont < 0.30 || cont > 0.80 || temp > -0.02 || hum > 0.42
                            || high < 0.14 || high > 0.50 || h < 92 || h > 144) {
                        continue;
                    }
                } else {
                    if (cont < 0.28 || cont > 0.80 || temp > -0.02 || hum > 0.42
                            || high < 0.11 || high > 0.48 || h < 88 || h > 140) {
                        continue;
                    }
                }

                // Require surrounding highland basin support on >= 5 of 8 directions (allow 1
                // outlet notch,
                // never place a lake balanced on top of a narrow spike/cliff tip)
                double candLakeR = isTrunkPass ? 22.0 : 14.0;
                int[] rimHeights = new int[8];
                int supportedDirs = 0;
                for (int a = 0; a < 8; a++) {
                    double ang = a * (Math.PI * 2.0 / 8.0);
                    int rx = (int) Math.round(sx + Math.cos(ang) * (candLakeR + 6.0));
                    int rz = (int) Math.round(sz + Math.sin(ang) * (candLakeR + 6.0));
                    int rh = lookup.sampleBaseHeight(rx, rz);
                    rimHeights[a] = rh;
                    if (rh >= h - 4) {
                        supportedDirs++;
                    }
                }
                if (supportedDirs < 5) {
                    continue;
                }
                java.util.Arrays.sort(rimHeights);
                // 3rd-lowest rim direction allows a wide natural outflow notch while securing
                // remaining 270 deg shores
                int stableRimFloor = rimHeights[2];
                if (stableRimFloor < h - 5 || stableRimFloor < SEA_LEVEL_Y + 12) {
                    continue;
                }

                double score = (h - 92.0) * 0.42 + (supportedDirs * 3.5)
                        + ((high >= 0.20 && high <= 0.54) ? 18.0 : 4.0) + (cont * 10.0);
                if (score > bestScore) {
                    bestScore = score;
                    bestSpringX = sx;
                    bestSpringZ = sz;
                    bestLakeRadius = candLakeR + (Math.floorMod(cellHash >>> 40, 6L));
                    bestRimFloor = stableRimFloor;
                    foundSpring = true;
                }
            }
        }

        if (!foundSpring) {
            return EMPTY_REGION;
        }

        // Choose style variation for this river basin
        int styleRoll = (int) Math.floorMod(cellHash >>> 24, 100L);
        RiverStyle style;
        if (isTributaryParity) {
            style = (styleRoll < 50) ? RiverStyle.CASCADE : RiverStyle.HYBRID;
        } else if (styleRoll < 32) {
            style = RiverStyle.CASCADE;
        } else if (styleRoll < 68) {
            style = RiverStyle.RAPIDS;
        } else {
            style = RiverStyle.HYBRID;
        }

        // Find initial downhill direction across 12 compass headings
        double bestDirAngle = 0.0;
        double lowestAheadPot = Double.POSITIVE_INFINITY;
        for (int a = 0; a < 12; a++) {
            double ang = (a * Math.PI * 2.0) / 12.0;
            int tx = (int) Math.round(bestSpringX + Math.cos(ang) * 88.0);
            int tz = (int) Math.round(bestSpringZ + Math.sin(ang) * 88.0);
            double pot = evaluateDrainagePotential(tx, tz, lookup);
            if (pot < lowestAheadPot) {
                lowestAheadPot = pot;
                bestDirAngle = ang;
            }
        }

        List<double[]> pts = new ArrayList<>(MAX_TRACE_STEPS + 1);
        pts.add(new double[] { bestSpringX, bestSpringZ });

        double curX = bestSpringX;
        double curZ = bestSpringZ;
        double heading = bestDirAngle;
        boolean reachedOcean = false;
        boolean mergedIntoTrunk = false;
        int mergeTrunkWaterY = SEA_LEVEL_Y;
        double mergeTrunkHalfW = 7.0;
        int oceanExtensionSteps = 0;

        for (int step = 0; step < MAX_TRACE_STEPS; step++) {
            double bestCandX = curX;
            double bestCandZ = curZ;
            double bestCandAng = heading;
            double bestCandScore = Double.POSITIVE_INFINITY;

            for (int a = 0; a < NUM_CANDIDATE_ANGLES; a++) {
                double frac = (NUM_CANDIDATE_ANGLES == 1) ? 0.0 : ((double) a / (NUM_CANDIDATE_ANGLES - 1)) * 2.0 - 1.0;
                double candAng = heading + frac * MAX_TURN_RAD;
                double nx = curX + Math.cos(candAng) * STEP_LEN;
                double nz = curZ + Math.sin(candAng) * STEP_LEN;

                // Self-loop guard (keep spacious bend radius so opposing banks never collide)
                boolean tooCloseToSelf = false;
                for (int prev = 0; prev < pts.size() - 4; prev++) {
                    double[] pp = pts.get(prev);
                    double dx = nx - pp[0];
                    double dz = nz - pp[1];
                    if (dx * dx + dz * dz < 64.0 * 64.0) {
                        tooCloseToSelf = true;
                        break;
                    }
                }
                if (tooCloseToSelf) {
                    continue;
                }

                // Multi-distance lookahead drives rivers cleanly into open ocean at all costs
                int lx1 = (int) Math.round(nx);
                int lz1 = (int) Math.round(nz);
                int lx2 = (int) Math.round(curX + Math.cos(candAng) * 90.0);
                int lz2 = (int) Math.round(curZ + Math.sin(candAng) * 90.0);
                int lx3 = (int) Math.round(curX + Math.cos(candAng) * 260.0);
                int lz3 = (int) Math.round(curZ + Math.sin(candAng) * 260.0);

                // Gradually boost seaward continental pull later in long traces so no coastal
                // ridge or peninsula traps a river
                double coastUrgency = (step > 45) ? Mth.clamp((step - 45) / 40.0, 0.0, 1.0) : 0.0;
                double potNear = evaluateDrainagePotential(lx1, lz1, lookup, coastUrgency, lowerPassTrunks, nx, nz,
                        heading);
                double potMid = evaluateDrainagePotential(lx2, lz2, lookup, coastUrgency);
                double potFar = evaluateDrainagePotential(lx3, lz3, lookup, coastUrgency);

                // Smooth meander oscillation bends rivers across broad flats without abrupt
                // zigzags
                double meander = FastNoise.noise2D((float) (nx * 0.0035f + 91.0f), (float) (nz * 0.0035f - 47.0f))
                        * 0.012 * (1.0 - coastUrgency);
                double turnCost = (frac * frac) * 0.009;
                double candScore = potNear * 0.42 + potMid * 0.30 + potFar * 0.28 + meander * frac + turnCost;

                if (candScore < bestCandScore) {
                    bestCandScore = candScore;
                    bestCandX = nx;
                    bestCandZ = nz;
                    bestCandAng = candAng;
                }
            }

            if (bestCandScore == Double.POSITIVE_INFINITY) {
                break;
            }

            curX = bestCandX;
            curZ = bestCandZ;
            heading = bestCandAng;

            int icx = (int) Math.round(curX);
            int icz = (int) Math.round(curZ);

            // Allow rivers crossing warm fringes or coastal peninsulas cleanly without
            // trapping; reject only extreme desert cores
            if (step < 14 && lookup.sampleTemperature(icx, icz) > 0.26) {
                return EMPTY_REGION;
            }

            double curCont = lookup.sampleContinentalness(icx, icz);
            int ch = (curCont <= 0.02) ? lookup.sampleBaseHeight(icx, icz) : 80;

            // Only stop before detached offshore pelagic islands AFTER we have cleared the
            // mainland into open deep sea
            if (reachedOcean
                    && (ch >= SEA_LEVEL_Y - 1 || (curCont < -0.030 && lookup.sampleIslandNoise(icx, icz) > 0.28))) {
                break;
            }

            pts.add(new double[] { curX, curZ });

            // Check tributary confluence with lower-pass trunk rivers (strict downstream
            // alignment so streams never fuse head-on!)
            if (lowerPassTrunks != null && step >= 5) {
                RiverNode snapNode = findNearestTrunkNode(lowerPassTrunks, curX, curZ, heading, CONFLUENCE_SNAP_DIST);
                if (snapNode != null) {
                    pts.add(new double[] { snapNode.x, snapNode.z });
                    mergedIntoTrunk = true;
                    mergeTrunkWaterY = snapNode.waterY;
                    mergeTrunkHalfW = snapNode.waterHalfWidth;
                    break;
                }
            }

            // Open-ocean check: extend cleanly out into verified submerged sea before
            // terminating!
            if (reachedOcean) {
                oceanExtensionSteps++;
                if (oceanExtensionSteps >= 3) {
                    break;
                }
            } else if (curCont <= -0.022 && ch <= SEA_LEVEL_Y - 2 && isOpenSeaAhead(curX, curZ, heading, lookup)) {
                reachedOcean = true;
                oceanExtensionSteps = 1;
            }
        }

        if (isTributaryParity && !mergedIntoTrunk) {
            return EMPTY_REGION;
        }
        if ((!reachedOcean && !mergedIntoTrunk) || pts.size() < MIN_VALID_STEPS) {
            return EMPTY_REGION;
        }

        // Smooth polyline vertices once via Chaikin / midpoint averaging (keeps
        // anchors) for liquid-smooth curves
        List<double[]> smoothPts = subdivideAndRelax(pts);
        int count = smoothPts.size();

        // Sample pre-river ground heights along smoothed centerline
        int[] baseHeights = new int[count];
        double[] contVals = new double[count];
        double[] highVals = new double[count];
        double[] arcDists = new double[count];

        double cumArc = 0.0;
        for (int i = 0; i < count; i++) {
            double[] p = smoothPts.get(i);
            if (i > 0) {
                double[] prev = smoothPts.get(i - 1);
                cumArc += Math.hypot(p[0] - prev[0], p[1] - prev[1]);
            }
            arcDists[i] = cumArc;
            int ix = (int) Math.round(p[0]);
            int iz = (int) Math.round(p[1]);
            baseHeights[i] = lookup.sampleBaseHeight(ix, iz);
            contVals[i] = lookup.sampleContinentalness(ix, iz);
            highVals[i] = lookup.sampleHighland(ix, iz);
        }

        // Compute exact lowest natural bank ground floor along the channel width;
        // use tight offsets in the mountains so steep hillsides never artificially depress safeCeiling.
        int[] corridorMinGround = new int[count];
        int[] lateralOffsWide = { -24, -16, -9, -4, 4, 9, 16, 24 };
        int[] lateralOffsTight = { -8, -4, 4, 8 };
        for (int i = 0; i < count; i++) {
            double[] p = smoothPts.get(i);
            double[] pPrev = smoothPts.get(Math.max(0, i - 1));
            double[] pNext = smoothPts.get(Math.min(count - 1, i + 1));
            double tdx = pNext[0] - pPrev[0];
            double tdz = pNext[1] - pPrev[1];
            double tLen = Math.hypot(tdx, tdz);
            double nx = (tLen > 1e-6) ? (-tdz / tLen) : 0.0;
            double nz = (tLen > 1e-6) ? (tdx / tLen) : 1.0;
            int[] offs = (i == 0 || highVals[i] >= 0.12) ? lateralOffsTight : lateralOffsWide;

            int minFloor = baseHeights[i];
            for (int lat : offs) {
                int lh = lookup.sampleBaseHeight((int) Math.round(p[0] + nx * lat), (int) Math.round(p[1] + nz * lat));
                if (lh < minFloor) {
                    minFloor = lh;
                }
            }
            if (i + 1 < count) {
                double[] pMid = smoothPts.get(i + 1);
                double mx = 0.5 * (p[0] + pMid[0]);
                double mz = 0.5 * (p[1] + pMid[1]);
                int mh = lookup.sampleBaseHeight((int) Math.round(mx), (int) Math.round(mz));
                if (mh < minFloor) {
                    minFloor = mh;
                }
                for (int lat : offs) {
                    int lh = lookup.sampleBaseHeight((int) Math.round(mx + nx * lat), (int) Math.round(mz + nz * lat));
                    if (lh < minFloor) {
                        minFloor = lh;
                    }
                }
            }
            corridorMinGround[i] = minFloor;
        }

        int targetLakeWaterY = Mth.clamp(Math.min(baseHeights[0], bestRimFloor) - 2, SEA_LEVEL_Y + 6,
                MAX_HEADWATER_WATER_Y);
        int lakeExitIdx = 1;
        for (int i = 1; i < count - 2; i++) {
            if (arcDists[i] <= bestLakeRadius + 4.0) {
                lakeExitIdx = i;
            } else {
                break;
            }
        }

        int[] safeCeiling = new int[count];
        int runningMin = corridorMinGround[lakeExitIdx];
        for (int i = lakeExitIdx; i < count; i++) {
            if (corridorMinGround[i] < runningMin) {
                runningMin = corridorMinGround[i];
            }
            safeCeiling[i] = Math.max(SEA_LEVEL_Y, Math.min(targetLakeWaterY, runningMin - 2));
        }
        for (int i = 0; i <= lakeExitIdx; i++) {
            safeCeiling[i] = Math.max(SEA_LEVEL_Y, targetLakeWaterY);
        }

        // Walk strictly UPSTREAM from mouth/confluence (count - 1) to Highland Lake (0)
        // Guarantees 100% monotonic downward flow, steep waterfall drops down mountain walls,
        // and sets the source lake level at the mountain height (targetLakeWaterY).
        int[] waterYs = new int[count];
        int[] stepDrops = new int[count]; // drop at node i -> i+1
        List<StepMarker> stepMarkers = new ArrayList<>();

        int curWaterY = mergedIntoTrunk ? mergeTrunkWaterY : SEA_LEVEL_Y;
        waterYs[count - 1] = curWaterY;
        double distSinceLastStep = 0.0;
        int lastStepIdx = -1;

        double sizeScale = (pass <= 1) ? 1.10 : 0.90;
        int widthBaseSeed = (int) Math.floorMod(cellHash >>> 32, 3L); // 0=medium, 1=broad, 2=grand

        for (int i = count - 2; i >= 0; i--) {
            double segLen = arcDists[i + 1] - arcDists[i];
            distSinceLastStep += segLen;

            if (i < lakeExitIdx) {
                waterYs[i] = targetLakeWaterY;
                continue;
            }
            if (i == lakeExitIdx) {
                int remaining = targetLakeWaterY - curWaterY;
                if (remaining > 0) {
                    if (lastStepIdx >= 0 && !stepMarkers.isEmpty() && distSinceLastStep < 14.0
                            && targetLakeWaterY <= corridorMinGround[lastStepIdx] - 1) {
                        for (int k = lakeExitIdx; k <= lastStepIdx; k++) {
                            waterYs[k] = targetLakeWaterY;
                        }
                        StepMarker prev = stepMarkers.get(stepMarkers.size() - 1);
                        int combinedDrop = targetLakeWaterY - prev.downstreamY;
                        stepMarkers.set(stepMarkers.size() - 1,
                                new StepMarker(prev.arcS, targetLakeWaterY, prev.downstreamY, combinedDrop));
                        stepDrops[lastStepIdx] = combinedDrop;
                    } else {
                        int oldY = curWaterY;
                        stepDrops[i] = remaining;
                        stepMarkers.add(new StepMarker((arcDists[i] + arcDists[i + 1]) * 0.5,
                                targetLakeWaterY, oldY, remaining));
                    }
                    curWaterY = targetLakeWaterY;
                    distSinceLastStep = 0.0;
                }
                waterYs[i] = curWaterY;
                continue;
            }

            int headroom = safeCeiling[i] - curWaterY;
            int plannedDrop = 0;
            boolean allowStepHere = i < count - (mergedIntoTrunk ? CONFLUENCE_FLAT_STEPS + 2 : 6)
                    && contVals[i] > 0.05 && headroom > 0;

            if (allowStepHere) {
                boolean mountainWaterfallZone = (highVals[i] >= 0.14 || curWaterY >= 82 || headroom >= 4);
                if (mountainWaterfallZone && distSinceLastStep >= 14.0) {
                    // Mountainside waterfall cascades spilling down from the Highland source lake
                    int maxWaterfallDrop = (headroom >= 8) ? 12 : (headroom >= 4 ? 5 : 2);
                    plannedDrop = Math.min(headroom, maxWaterfallDrop);
                } else {
                    int candidateStep = chooseStepSize(style, curWaterY, highVals[i], cellHash, i);
                    double requiredPoolLen = (candidateStep >= 2 ? 92.0 : 68.0) * sizeScale;
                    if (distSinceLastStep >= requiredPoolLen && headroom >= candidateStep) {
                        plannedDrop = candidateStep;
                    }
                }
            }

            if (plannedDrop > 0) {
                int oldY = curWaterY;
                curWaterY += plannedDrop;
                stepDrops[i] = plannedDrop;
                stepMarkers.add(new StepMarker((arcDists[i] + arcDists[i + 1]) * 0.5, curWaterY, oldY, plannedDrop));
                lastStepIdx = i;
                distSinceLastStep = 0.0;
            }
            waterYs[i] = curWaterY;
        }

        // Reject rivers whose non-confluence trace collides within 60 blocks of an
        // existing river at a different height!
        if (lowerPassTrunks != null && hasCorridorCollision(smoothPts, waterYs, mergedIntoTrunk, lowerPassTrunks)) {
            return EMPTY_REGION;
        }

        // Build final RiverNode array (order steps upstream -> downstream)
        java.util.Collections.reverse(stepMarkers);

        // Sample lateral hilltops to compute smooth highland-aware valley widths
        // (prevents cliff walls around deep canyons & alpine lakes)
        int[] rawHillY = new int[count];
        int[] hillScanOffs = { -88, -56, -28, 28, 56, 88 };
        for (int i = 0; i < count; i++) {
            double[] p = smoothPts.get(i);
            double[] pPrev = smoothPts.get(Math.max(0, i - 1));
            double[] pNext = smoothPts.get(Math.min(count - 1, i + 1));
            double tdx = pNext[0] - pPrev[0];
            double tdz = pNext[1] - pPrev[1];
            double tLen = Math.hypot(tdx, tdz);
            double nx = (tLen > 1e-6) ? (-tdz / tLen) : 0.0;
            double nz = (tLen > 1e-6) ? (tdx / tLen) : 1.0;
            int peak = baseHeights[i];
            for (int lat : hillScanOffs) {
                int h = lookup.sampleBaseHeight((int) Math.round(p[0] + nx * lat), (int) Math.round(p[1] + nz * lat));
                if (h > peak) {
                    peak = h;
                }
            }
            rawHillY[i] = peak;
        }
        double[] smoothHillY = new double[count];
        for (int i = 0; i < count; i++) {
            int maxW = rawHillY[i];
            double sumW = 0.0;
            double wTot = 0.0;
            int lo = Math.max(0, i - 4);
            int hi = Math.min(count - 1, i + 4);
            for (int k = lo; k <= hi; k++) {
                double wt = 5.0 - Math.abs(k - i);
                if (rawHillY[k] > maxW) {
                    maxW = rawHillY[k];
                }
                sumW += rawHillY[k] * wt;
                wTot += wt;
            }
            smoothHillY[i] = 0.65 * maxW + 0.35 * (sumW / Math.max(1.0, wTot));
        }
        double[] smoothedTemp = new double[count];
        for (int passIdx = 0; passIdx < 2; passIdx++) {
            for (int i = 0; i < count; i++) {
                double left = smoothHillY[Math.max(0, i - 1)];
                double right = smoothHillY[Math.min(count - 1, i + 1)];
                smoothedTemp[i] = 0.25 * left + 0.50 * smoothHillY[i] + 0.25 * right;
            }
            System.arraycopy(smoothedTemp, 0, smoothHillY, 0, count);
        }

        double lakeRimSpan = 16.0;

        // Precompute smooth highland-aware valley half-widths + Lipschitz envelope so
        // widths taper smoothly around bends & lakes
        double[] rawVHalf = new double[count];
        double[] rawWHalf = new double[count];
        double[] rawDepth = new double[count];
        double[] rawEstuary = new double[count];
        double[] rawCanopy = new double[count];
        double[] rawTx = new double[count];
        double[] rawTz = new double[count];
        double totalLen = Math.max(1.0, arcDists[count - 1]);
        double widthScale = isTributaryParity ? 0.68 : (widthBaseSeed == 2 ? 1.25 : 0.98);

        for (int i = 0; i < count; i++) {
            double[] p = smoothPts.get(i);
            double[] pPrev = smoothPts.get(Math.max(0, i - 1));
            double[] pNext = smoothPts.get(Math.min(count - 1, i + 1));
            double tdx = pNext[0] - pPrev[0];
            double tdz = pNext[1] - pPrev[1];
            double tLen = Math.hypot(tdx, tdz);
            rawTx[i] = (tLen > 1e-6) ? tdx / tLen : 1.0;
            rawTz[i] = (tLen > 1e-6) ? tdz / tLen : 0.0;

            double prog = arcDists[i] / totalLen;
            double estuary = (!mergedIntoTrunk && contVals[i] < 0.07)
                    ? hermite01((0.07 - contVals[i]) / 0.09)
                    : 0.0;
            rawEstuary[i] = estuary;

            double baseW = (6.8 + prog * 9.0) * widthScale;
            if (mergedIntoTrunk && prog > 0.85) {
                double mergeT = hermite01((prog - 0.85) / 0.15);
                baseW = Mth.lerp(mergeT, baseW, Math.min(mergeTrunkHalfW, baseW * 1.25));
            }
            double wHalf = baseW * (1.0 + 1.05 * estuary);
            double hillHeadroom = Math.max(0.0, smoothHillY[i] - waterYs[i]);
            double slopeSpan = Math.min(48.0, Math.max(24.0, hillHeadroom * 0.85 + 14.0));
            rawWHalf[i] = wHalf;
            rawVHalf[i] = Math.max(wHalf + slopeSpan, (32.0 + prog * 24.0) * widthScale * (1.0 + 0.40 * estuary));
            rawDepth[i] = (2.8 + prog * 2.1) * (isTributaryParity ? 0.85 : 1.0) * (1.0 + 0.15 * estuary);
            rawCanopy[i] = (FastNoise.noise2D((float) (p[0] * 0.0019f + 17.0f), (float) (p[1] * 0.0019f - 31.0f)));
        }

        // Backward + forward 1-Lipschitz cone sweep AND 2D Euclidean cone envelope guarantee no wide segment
        // or lake mouth can ever swallow a narrower downstream arm across U-bends
        for (int i = 1; i < count; i++) {
            double ds = Math.max(1.0, arcDists[i] - arcDists[i - 1]);
            rawVHalf[i] = Math.max(rawVHalf[i], rawVHalf[i - 1] - 0.32 * ds);
        }
        for (int i = count - 2; i >= 0; i--) {
            double ds = Math.max(1.0, arcDists[i + 1] - arcDists[i]);
            rawVHalf[i] = Math.max(rawVHalf[i], rawVHalf[i + 1] - 0.32 * ds);
        }
        for (int i = 0; i < count; i++) {
            double[] pi = smoothPts.get(i);
            for (int j = 0; j < count; j++) {
                if (i != j && rawVHalf[j] > rawVHalf[i]) {
                    double[] pj = smoothPts.get(j);
                    double sep = Math.hypot(pi[0] - pj[0], pi[1] - pj[1]);
                    rawVHalf[i] = Math.max(rawVHalf[i], rawVHalf[j] - 0.45 * sep);
                }
            }
        }
        // Keep the Highland source lake compact at its natural mountain shelf without flattening surrounding terrain
        for (int i = 0; i < count; i++) {
            if (arcDists[i] < bestLakeRadius + 64.0) {
                double maxHeadVHalf = rawWHalf[i] + Mth.lerp(
                        hermite01(Math.max(0.0, arcDists[i] - bestLakeRadius) / 64.0),
                        14.0, 44.0);
                rawVHalf[i] = Math.min(rawVHalf[i], maxHeadVHalf);
            }
        }

        RiverNode[] finalNodes = new RiverNode[count];
        for (int i = 0; i < count; i++) {
            double[] p = smoothPts.get(i);
            finalNodes[i] = new RiverNode(
                    p[0], p[1], rawTx[i], rawTz[i], arcDists[i],
                    rawWHalf[i], rawVHalf[i], rawDepth[i], rawEstuary[i], waterYs[i], stepDrops[i], rawCanopy[i]);
        }

        return new RiverRegion(new RiverPolyline(
                finalNodes,
                stepMarkers.toArray(new StepMarker[0]),
                style,
                mergedIntoTrunk,
                reachedOcean && !mergedIntoTrunk,
                bestSpringX,
                bestSpringZ,
                bestLakeRadius,
                lakeRimSpan,
                cellHash));
    }

    private static int chooseStepSize(RiverStyle style, int currentWaterY, double highlandVal, long seedHash,
            int index) {
        if (style == RiverStyle.CASCADE) {
            return (currentWaterY >= 78 || (Math.floorMod(seedHash + index * 31L, 10L) < 7)) ? 2 : 1;
        }
        if (style == RiverStyle.RAPIDS) {
            return 1;
        }
        // HYBRID: scenic 2-block waterfall cascades in higher hills/foothills, gentle
        // 1-block rapids lower down
        if (currentWaterY >= 78 || highlandVal > 0.20) {
            return (Math.floorMod(seedHash + index * 17L, 3L) != 0) ? 2 : 1;
        }
        return 1;
    }

    private static RiverNode findNearestTrunkNode(List<RiverPolyline> trunks, double x, double z, double heading,
            double maxDist) {
        double maxDistSq = maxDist * maxDist;
        double hx = Math.cos(heading);
        double hz = Math.sin(heading);
        RiverNode best = null;
        double bestDistSq = maxDistSq;
        for (RiverPolyline trunk : trunks) {
            if (!trunk.boundsContain((int) x, (int) z)) {
                continue;
            }
            // Only snap safely upstream of the mouth/tail and along compatible downstream
            // alignment (no head-on dead-end fusion!)
            int limit = trunk.nodes.length - 6;
            for (int i = 4; i < limit; i++) {
                RiverNode n = trunk.nodes[i];
                double dot = hx * n.tx + hz * n.tz;
                if (dot < -0.10) {
                    continue;
                }
                double dx = n.x - x;
                double dz = n.z - z;
                double d2 = dx * dx + dz * dz;
                if (d2 < bestDistSq) {
                    bestDistSq = d2;
                    best = n;
                }
            }
        }
        return best;
    }

    private static boolean isOpenSeaAhead(double curX, double curZ, double heading, BaseTerrainLookup lookup) {
        // Dense 10-block ray check out to 250 blocks: prevents ever skipping across
        // narrow 15-40 block
        // coastal sandbars/peninsulas and guarantees unobstructed water straight into
        // deep Silent Oceans.
        double[] offsets = { 0.0, -0.35, 0.35 };
        for (double off : offsets) {
            double ang = heading + off;
            double cos = Math.cos(ang);
            double sin = Math.sin(ang);
            boolean unobstructed = true;
            boolean reachedDeepSea = false;
            for (int dist = 10; dist <= 250; dist += 10) {
                int px = (int) Math.round(curX + cos * dist);
                int pz = (int) Math.round(curZ + sin * dist);
                double pc = lookup.sampleContinentalness(px, pz);
                int ph = lookup.sampleBaseHeight(px, pz);
                if (pc >= -0.008 || ph >= SEA_LEVEL_Y - 1) {
                    unobstructed = false;
                    break;
                }
                if (pc <= -0.038 && ph <= SEA_LEVEL_Y - 5) {
                    reachedDeepSea = true;
                }
            }
            if (unobstructed && reachedDeepSea) {
                return true;
            }
        }
        return false;
    }

    private static List<double[]> subdivideAndRelax(List<double[]> raw) {
        if (raw.size() < 3) {
            return raw;
        }
        List<double[]> out = new ArrayList<>(raw.size() * 2);
        out.add(raw.get(0));
        for (int i = 0; i < raw.size() - 1; i++) {
            double[] p0 = raw.get(i);
            double[] p1 = raw.get(i + 1);
            if (i > 0) {
                out.add(new double[] {
                        0.75 * p0[0] + 0.25 * p1[0],
                        0.75 * p0[1] + 0.25 * p1[1]
                });
            }
            if (i < raw.size() - 2) {
                out.add(new double[] {
                        0.25 * p0[0] + 0.75 * p1[0],
                        0.25 * p0[1] + 0.75 * p1[1]
                });
            }
        }
        out.add(raw.get(raw.size() - 1));
        return out;
    }

    private static double evaluateDrainagePotential(int x, int z, BaseTerrainLookup lookup) {
        return evaluateDrainagePotential(x, z, lookup, 0.0);
    }

    private static double evaluateDrainagePotential(int x, int z, BaseTerrainLookup lookup, double coastUrgency) {
        return evaluateDrainagePotential(x, z, lookup, coastUrgency, null, x, z, 0.0);
    }

    private static double evaluateDrainagePotential(int x, int z, BaseTerrainLookup lookup, double coastUrgency,
            List<RiverPolyline> existingRivers, double stepX, double stepZ, double heading) {
        double cont = lookup.sampleContinentalness(x, z);
        double high = lookup.sampleHighland(x, z);
        double temp = lookup.sampleTemperature(x, z);
        double hum = lookup.sampleHumidity(x, z);

        // Gently prefer mountain valleys over tall spires, fading off if river urgently
        // needs to breach to coast
        double avoidScale = 1.0 - 0.85 * coastUrgency;
        double highPenalty = Math.max(0.0, high - 0.16) * 0.50 * avoidScale;
        double desertPenalty = Math.max(0.0, temp - 0.03) * 1.15 * avoidScale;
        double scarletPenalty = Math.max(0.0, hum - 0.42) * 0.75 * avoidScale;
        // Pull rivers aggressively seaward past beaches and peninsulas into open deep
        // ocean
        double seawardCont = Math.max(-0.14, cont) * (1.0 + 1.2 * coastUrgency);
        // Only steer around detached pelagic offshore islands well out in open sea
        // (cont < -0.032)
        double islandPenalty = 0.0;
        if (cont < -0.032) {
            double island = lookup.sampleIslandNoise(x, z);
            if (island > 0.28) {
                islandPenalty += (island - 0.28) * 0.60;
            }
        }
        // Repel non-confluence passes away from existing lower-pass channels so
        // mismatched rivers never collide
        double collisionPenalty = 0.0;
        if (existingRivers != null && !existingRivers.isEmpty()) {
            double hx = Math.cos(heading);
            double hz = Math.sin(heading);
            for (RiverPolyline trunk : existingRivers) {
                if (!trunk.boundsContain((int) stepX, (int) stepZ)) {
                    continue;
                }
                for (int i = 0; i < trunk.nodes.length; i += 2) {
                    RiverNode n = trunk.nodes[i];
                    double dx = n.x - stepX;
                    double dz = n.z - stepZ;
                    double d2 = dx * dx + dz * dz;
                    if (d2 < 96.0 * 96.0) {
                        double dot = hx * n.tx + hz * n.tz;
                        // Allow clean downstream confluence approach only near trunk middle/downstream
                        boolean allowConfluence = (i >= 4 && i < trunk.nodes.length - 6 && dot >= 0.05
                                && d2 < 48.0 * 48.0);
                        if (!allowConfluence) {
                            double d = Math.sqrt(d2);
                            collisionPenalty += (96.0 - d) * 0.008;
                        }
                    }
                }
            }
        }
        return seawardCont + highPenalty + desertPenalty + scarletPenalty + islandPenalty + collisionPenalty;
    }

    private static boolean hasCorridorCollision(List<double[]> smoothPts, int[] waterYs,
            boolean mergedIntoTrunk, List<RiverPolyline> existingRivers) {
        int checkLimit = mergedIntoTrunk ? Math.max(0, smoothPts.size() - 2) : smoothPts.size();
        double minSepSq = 68.0 * 68.0;
        for (int i = 0; i < checkLimit; i++) {
            double[] p = smoothPts.get(i);
            int wy = waterYs[i];
            for (RiverPolyline other : existingRivers) {
                if (p[0] < other.minX - 72.0 || p[0] > other.maxX + 72.0
                        || p[1] < other.minZ - 72.0 || p[1] > other.maxZ + 72.0) {
                    continue;
                }
                for (int j = 0; j < other.nodes.length; j++) {
                    RiverNode on = other.nodes[j];
                    double dx = p[0] - on.x;
                    double dz = p[1] - on.z;
                    if (dx * dx + dz * dz < minSepSq && Math.abs(wy - on.waterY) > 1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static double smoothMin(double a, double b, double k) {
        if (k <= 0.0) {
            return Math.min(a, b);
        }
        double diff = b - a;
        if (diff >= k) {
            return a;
        }
        if (diff <= -k) {
            return b;
        }
        double h = 0.5 + 0.5 * diff / k;
        return Mth.lerp(h, b, a) - k * h * (1.0 - h);
    }

    /**
     * Rim-safe smooth minimum for carved dry valley surfaces: tapers smoothing radius to zero
     * whenever either profile approaches {@code baseSurfaceY} so overlapping outer rims never
     * depress ground below natural height and create a perimeter step wall.
     */
    private static double smoothCarveMin(double yA, double yB, int baseSurfaceY, double kMax) {
        double dropA = Math.max(0.0, baseSurfaceY - yA);
        double dropB = Math.max(0.0, baseSurfaceY - yB);
        double minDrop = Math.min(dropA, dropB);
        if (minDrop <= 1e-4) {
            return Math.min(yA, yB);
        }
        double effK = kMax * hermite01(minDrop / 4.0);
        return smoothMin(yA, yB, effK);
    }

    private static int getCellPass(int gx, int gz) {
        return Math.floorMod(gx, 5) + 5 * Math.floorMod(gz, 5);
    }

    private static long packRegionKey(int gx, int gz) {
        return (((long) gx) << 32) ^ (gz & 0xFFFFFFFFL);
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double hermite01(double v) {
        double c = Mth.clamp(v, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }
}
