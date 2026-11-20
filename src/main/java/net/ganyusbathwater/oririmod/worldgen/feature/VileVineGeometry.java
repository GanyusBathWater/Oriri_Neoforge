package net.ganyusbathwater.oririmod.worldgen.feature;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared 3D geometry builder & placer for giant twisting Vile Vines and their curling offshoot tendrils.
 * <p>
 * Each vine is composed of 2–3 braided helical strands orbiting a gently meandering tilted 3D spine,
 * producing an organic, rope-like twisted vine (width 2..6 blocks) rather than a smooth tree log.
 * Supports both chunk-clipped worldgen placement (zero far-chunk writes) and direct sapling placement.
 */
public final class VileVineGeometry {

    @FunctionalInterface
    public interface ReplacePredicate {
        boolean canReplace(WorldGenLevel level, BlockPos pos, BlockState existing, boolean aboveBase);
    }

    public static final class TendrilSpec {
        public final double startX, startY, startZ;
        public final double dirX, dirY, dirZ;
        public final double length;
        public final double baseWidth;
        public final double curlRate;
        public final double curlAmp;
        public final double curlPhase;
        public final double minX, maxX, minZ, maxZ;

        public TendrilSpec(double startX, double startY, double startZ,
                double dirX, double dirY, double dirZ,
                double length, double baseWidth,
                double curlRate, double curlAmp, double curlPhase) {
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
            double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            if (len < 1e-4) {
                this.dirX = 0.7;
                this.dirY = 0.7;
                this.dirZ = 0.0;
            } else {
                this.dirX = dirX / len;
                this.dirY = dirY / len;
                this.dirZ = dirZ / len;
            }
            this.length = length;
            this.baseWidth = baseWidth;
            this.curlRate = curlRate;
            this.curlAmp = curlAmp;
            this.curlPhase = curlPhase;

            double endX = startX + this.dirX * length;
            double endZ = startZ + this.dirZ * length;
            double pad = curlAmp + baseWidth + 3.0;
            this.minX = Math.min(startX, endX) - pad;
            this.maxX = Math.max(startX, endX) + pad;
            this.minZ = Math.min(startZ, endZ) - pad;
            this.maxZ = Math.max(startZ, endZ) + pad;
        }

        /**
         * Samples a point along this curling tendril at distance {@code d in [0, length]}.
         */
        public void samplePoint(double d, double[] outXYZ) {
            double refX = Math.abs(dirX) < 0.85 ? 1.0 : 0.0;
            double refZ = Math.abs(dirX) < 0.85 ? 0.0 : 1.0;
            double rx = dirY * refZ;
            double ry = dirZ * refX - dirX * refZ;
            double rz = -dirY * refX;
            double rLen = Math.sqrt(rx * rx + ry * ry + rz * rz);
            if (rLen < 1e-4) {
                rx = 1; ry = 0; rz = 0;
            } else {
                rx /= rLen; ry /= rLen; rz /= rLen;
            }
            double fx = dirY * rz - dirZ * ry;
            double fy = dirZ * rx - dirX * rz;
            double fz = dirX * ry - dirY * rx;

            double frac = Mth.clamp(d / Math.max(1.0, length), 0.0, 1.0);
            double curlGrowth = frac * curlAmp;
            double ang = curlPhase + Math.max(0.0, d) * curlRate;
            double ca = Math.cos(ang);
            double sa = Math.sin(ang);
            double hookY = frac * frac * 2.5;

            outXYZ[0] = startX + dirX * d + (rx * ca + fx * sa) * curlGrowth;
            outXYZ[1] = startY + dirY * d + (ry * ca + fy * sa) * curlGrowth + hookY;
            outXYZ[2] = startZ + dirZ * d + (rz * ca + fz * sa) * curlGrowth;
        }
    }

    /**
     * Parametric 3D giant botanical leaf blade (ovate-lanceolate silhouette with longitudinal arch,
     * graceful tip droop, and subtle V-rib cross-sectional cupping) built out of {@code Vile Leaves}.
     */
    public static final class LeafSpec {
        public final double ox, oy, oz;
        public final double fx, fy, fz; // unit forward axis (base -> pointed tip)
        public final double rx, ry, rz; // unit lateral axis (left -> right across blade)
        public final double ux, uy, uz; // unit upward normal (for arch & V-cup)
        public final double length;
        public final double maxHalfWidth;
        public final double archAmp;
        public final double droopAmp;
        public final double cupAmp;
        public final double wavePhase;
        public final double minX, maxX, minZ, maxZ;

        public LeafSpec(double ox, double oy, double oz,
                double dirX, double dirY, double dirZ,
                double rollRad,
                double length, double maxHalfWidth,
                double archAmp, double droopAmp, double cupAmp,
                double wavePhase) {
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;

            double fLen = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            if (fLen < 1e-4) {
                this.fx = 1.0; this.fy = 0.0; this.fz = 0.0;
            } else {
                this.fx = dirX / fLen;
                this.fy = dirY / fLen;
                this.fz = dirZ / fLen;
            }

            // Build horizontal right vector R0 perpendicular to F, then upward normal U0 = R0 x F
            double horiz = Math.sqrt(this.fx * this.fx + this.fz * this.fz);
            double r0x, r0y, r0z;
            if (horiz < 1e-4) {
                r0x = 1.0; r0y = 0.0; r0z = 0.0;
            } else {
                r0x = -this.fz / horiz;
                r0y = 0.0;
                r0z = this.fx / horiz;
            }
            double u0x = r0y * this.fz - r0z * this.fy;
            double u0y = r0z * this.fx - r0x * this.fz;
            double u0z = r0x * this.fy - r0y * this.fx;
            double uLen = Math.sqrt(u0x * u0x + u0y * u0y + u0z * u0z);
            if (uLen > 1e-4) {
                u0x /= uLen; u0y /= uLen; u0z /= uLen;
            } else {
                u0x = 0.0; u0y = 1.0; u0z = 0.0;
            }

            double cr = Math.cos(rollRad);
            double sr = Math.sin(rollRad);
            this.rx = r0x * cr + u0x * sr;
            this.ry = r0y * cr + u0y * sr;
            this.rz = r0z * cr + u0z * sr;
            this.ux = -r0x * sr + u0x * cr;
            this.uy = -r0y * sr + u0y * cr;
            this.uz = -r0z * sr + u0z * cr;

            this.length = length;
            this.maxHalfWidth = maxHalfWidth;
            this.archAmp = archAmp;
            this.droopAmp = droopAmp;
            this.cupAmp = cupAmp;
            this.wavePhase = wavePhase;

            // Conservative XZ AABB for chunk-clipped placement (zero cross-chunk cutoffs)
            double tipX = ox + this.fx * length;
            double tipZ = oz + this.fz * length;
            double pad = maxHalfWidth * 1.15 + Math.max(archAmp, droopAmp) + cupAmp + 2.0;
            this.minX = Math.min(ox, tipX) - pad;
            this.maxX = Math.max(ox, tipX) + pad;
            this.minZ = Math.min(oz, tipZ) - pad;
            this.maxZ = Math.max(oz, tipZ) + pad;
        }
    }

    public static final class VineSpec {
        public final double baseX, baseY, baseZ;
        public final double heightAbove;
        public final double depthBelow;
        public final double tiltRad;
        public final double azimuth;
        public final double baseWidth;
        public final int strandCount;
        public final double twistRate;
        public final double twistPhase;
        public final double swayAmp1, swayAmp2;
        public final double swayFreq1, swayFreq2;
        public final double swayPhase1, swayPhase2;
        public final double rootTiltScale;
        public final List<TendrilSpec> tendrils;
        public final List<LeafSpec> leaves;
        public final double minX, maxX, minZ, maxZ;

        public VineSpec(double baseX, double baseY, double baseZ,
                double heightAbove, double depthBelow,
                double tiltRad, double azimuth, double baseWidth,
                double twistRate, double twistPhase,
                double swayAmp1, double swayAmp2,
                double swayFreq1, double swayFreq2,
                double swayPhase1, double swayPhase2,
                double rootTiltScale,
                List<TendrilSpec> tendrils) {
            this.baseX = baseX;
            this.baseY = baseY;
            this.baseZ = baseZ;
            this.heightAbove = heightAbove;
            this.depthBelow = depthBelow;
            this.tiltRad = tiltRad;
            this.azimuth = azimuth;
            this.baseWidth = Mth.clamp(baseWidth, 2.0, 6.0);
            this.strandCount = this.baseWidth >= 3.6 ? 3 : 2;
            this.twistRate = twistRate;
            this.twistPhase = twistPhase;
            this.swayAmp1 = swayAmp1;
            this.swayAmp2 = swayAmp2;
            this.swayFreq1 = swayFreq1;
            this.swayFreq2 = swayFreq2;
            this.swayPhase1 = swayPhase1;
            this.swayPhase2 = swayPhase2;
            this.rootTiltScale = rootTiltScale;
            this.tendrils = tendrils;
            this.leaves = buildLeaves(this);

            double[] p = new double[3];
            double mnX = baseX, mxX = baseX, mnZ = baseZ, mxZ = baseZ;
            int samples = 16;
            for (int i = 0; i <= samples; i++) {
                double s = -depthBelow + (depthBelow + heightAbove) * ((double) i / samples);
                sampleSpine(s, p);
                if (p[0] < mnX) mnX = p[0];
                if (p[0] > mxX) mxX = p[0];
                if (p[2] < mnZ) mnZ = p[2];
                if (p[2] > mxZ) mxZ = p[2];
            }
            double pad = this.baseWidth + swayAmp1 + swayAmp2 + 4.0;
            mnX -= pad;
            mxX += pad;
            mnZ -= pad;
            mxZ += pad;
            for (TendrilSpec t : tendrils) {
                if (t.minX < mnX) mnX = t.minX;
                if (t.maxX > mxX) mxX = t.maxX;
                if (t.minZ < mnZ) mnZ = t.minZ;
                if (t.maxZ > mxZ) mxZ = t.maxZ;
            }
            for (LeafSpec leaf : this.leaves) {
                if (leaf.minX < mnX) mnX = leaf.minX;
                if (leaf.maxX > mxX) mxX = leaf.maxX;
                if (leaf.minZ < mnZ) mnZ = leaf.minZ;
                if (leaf.maxZ > mxZ) mxZ = leaf.maxZ;
            }
            this.minX = mnX;
            this.maxX = mxX;
            this.minZ = mnZ;
            this.maxZ = mxZ;
        }

        /**
         * Samples the 3D centerline at signed vertical offset {@code s} relative to {@code baseY}:
         * {@code s in [-depthBelow, +heightAbove]}.
         */
        public void sampleSpine(double s, double[] outXYZ) {
            double tanTilt = Math.tan(tiltRad);
            double dirX = Math.cos(azimuth);
            double dirZ = Math.sin(azimuth);
            double perpX = -dirZ;
            double perpZ = dirX;

            double effSlope = s >= 0.0 ? tanTilt : tanTilt * rootTiltScale;
            double drift = s * effSlope;

            // Gentle organic S-curve sway along and perpendicular to the tilt plane
            double swayPerp = Math.sin(s * swayFreq1 + swayPhase1) * swayAmp1
                    + Math.cos(s * swayFreq2 + swayPhase2) * swayAmp2;
            double swayAlong = Math.cos(s * swayFreq1 * 0.78 + swayPhase2) * (swayAmp1 * 0.55);

            // Pin smoothly to exact base at s = 0 so roots/saplings anchor cleanly
            double pin = 1.0 - Math.exp(-(s * s) / 140.0);
            swayPerp *= pin;
            swayAlong *= pin;

            outXYZ[0] = baseX + dirX * (drift + swayAlong) + perpX * swayPerp;
            outXYZ[1] = baseY + s;
            outXYZ[2] = baseZ + dirZ * (drift + swayAlong) + perpZ * swayPerp;
        }

        /**
         * Computes the effective vine diameter [1.5 .. baseWidth] at signed vertical offset {@code s}.
         */
        public double widthAt(double s) {
            if (s >= 0.0) {
                double frac = heightAbove > 1.0 ? Mth.clamp(s / heightAbove, 0.0, 1.0) : 0.0;
                // Slight root flare near ground, full thickness through mid-sky, taper in upper 30%
                double flare = 1.0 + 0.14 * Math.exp(-(s * s) / 80.0);
                double taper = frac > 0.70 ? Mth.lerp((frac - 0.70) / 0.30, 1.0, 0.42) : 1.0;
                return Math.max(1.6, baseWidth * flare * taper);
            } else {
                double frac = depthBelow > 1.0 ? Mth.clamp((-s) / depthBelow, 0.0, 1.0) : 0.0;
                double taper = frac > 0.72 ? Mth.lerp((frac - 0.72) / 0.28, 1.0, 0.48) : 1.0;
                return Math.max(1.6, baseWidth * taper);
            }
        }
    }

    private VileVineGeometry() {
    }

    /**
     * Builds a randomized twisting VineSpec anchored at {@code (baseX, baseY, baseZ)}.
     *
     * @param minWidth minimum vine diameter (2.0..6.0)
     * @param maxWidth maximum vine diameter (2.0..6.0)
     * @param heightAbove height into the sky (e.g., 80..120)
     * @param depthBelow depth burrowing down into stone/deepslate
     * @param maxTiltDeg max angle from vertical in degrees (e.g., 60.0)
     */
    public static VineSpec createVine(
            double baseX, double baseY, double baseZ,
            double minWidth, double maxWidth,
            double heightAbove, double depthBelow,
            double maxTiltDeg, double fixedAzimuth,
            int tendrilCount, RandomSource rnd) {

        double width = minWidth + rnd.nextDouble() * Math.max(0.0, maxWidth - minWidth);
        width = Mth.clamp(width, 2.0, 6.0);

        // Angle from 0 (straight up) up to maxTiltDeg (~60 degrees), mildly weighted toward dramatic angles
        double tiltDeg = Math.pow(rnd.nextDouble(), 0.82) * maxTiltDeg;
        double tiltRad = Math.toRadians(tiltDeg);
        double azimuth = Double.isNaN(fixedAzimuth) ? (rnd.nextDouble() * Math.PI * 2.0) : fixedAzimuth;

        double twistDir = rnd.nextBoolean() ? 1.0 : -1.0;
        double twistRate = twistDir * (0.17 + rnd.nextDouble() * 0.09);
        double twistPhase = rnd.nextDouble() * Math.PI * 2.0;

        double swayAmp1 = 1.6 + rnd.nextDouble() * 2.8;
        double swayAmp2 = 0.7 + rnd.nextDouble() * 1.4;
        double swayFreq1 = 0.045 + rnd.nextDouble() * 0.032;
        double swayFreq2 = 0.11 + rnd.nextDouble() * 0.055;
        double swayPhase1 = rnd.nextDouble() * Math.PI * 2.0;
        double swayPhase2 = rnd.nextDouble() * Math.PI * 2.0;
        double rootTiltScale = 0.35 + rnd.nextDouble() * 0.35;

        List<TendrilSpec> tendrils = new ArrayList<>(tendrilCount);
        VineSpec baseSpec = new VineSpec(
                baseX, baseY, baseZ,
                heightAbove, depthBelow,
                tiltRad, azimuth, width,
                twistRate, twistPhase,
                swayAmp1, swayAmp2, swayFreq1, swayFreq2, swayPhase1, swayPhase2,
                rootTiltScale,
                tendrils);

        double[] p = new double[3];
        for (int i = 0; i < tendrilCount; i++) {
            double s;
            if (i < tendrilCount / 3 && depthBelow > 12.0) {
                // Underground cave curling root tendril
                s = -(6.0 + rnd.nextDouble() * (depthBelow * 0.70));
            } else {
                // Surface / sky curling offshoot tendril
                s = 6.0 + rnd.nextDouble() * Math.max(10.0, heightAbove * 0.82);
            }
            baseSpec.sampleSpine(s, p);

            double tAz = rnd.nextDouble() * Math.PI * 2.0;
            double upBias = s >= 0.0 ? (0.25 + rnd.nextDouble() * 0.65) : (-0.35 + rnd.nextDouble() * 0.70);
            double horiz = Math.sqrt(Math.max(0.05, 1.0 - upBias * upBias));
            double dx = Math.cos(tAz) * horiz;
            double dz = Math.sin(tAz) * horiz;

            double tLen = 9.0 + rnd.nextDouble() * 17.0;
            double tWidth = 1.1 + rnd.nextDouble() * Math.min(1.4, width * 0.32);
            double cRate = (rnd.nextBoolean() ? 1.0 : -1.0) * (0.28 + rnd.nextDouble() * 0.18);
            double cAmp = 1.2 + rnd.nextDouble() * 2.2;
            double cPhase = rnd.nextDouble() * Math.PI * 2.0;

            tendrils.add(new TendrilSpec(p[0], p[1], p[2], dx, upBias, dz, tLen, tWidth, cRate, cAmp, cPhase));
        }

        // Recompute AABB with tendrils included
        return new VineSpec(
                baseX, baseY, baseZ,
                heightAbove, depthBelow,
                tiltRad, azimuth, width,
                twistRate, twistPhase,
                swayAmp1, swayAmp2, swayFreq1, swayFreq2, swayPhase1, swayPhase2,
                rootTiltScale,
                tendrils);
    }

    /**
     * Adds an extra curling tendril that emerges from the vine near a point and wraps outward.
     */
    public static TendrilSpec createIslandWrapTendril(
            double startX, double startY, double startZ,
            double targetX, double targetY, double targetZ,
            double width, RandomSource rnd) {
        double dx = targetX - startX;
        double dy = targetY - startY;
        double dz = targetZ - startZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double len = Math.max(8.0, dist + 4.0 + rnd.nextDouble() * 6.0);
        return new TendrilSpec(
                startX, startY, startZ,
                dx, dy, dz,
                len, Mth.clamp(width, 1.2, 2.4),
                (rnd.nextBoolean() ? 1.0 : -1.0) * (0.26 + rnd.nextDouble() * 0.14),
                1.4 + rnd.nextDouble() * 1.8,
                rnd.nextDouble() * Math.PI * 2.0);
    }

    /**
     * Places a VineSpec clipped to {@code [clipMinX..clipMaxX, clipMinZ..clipMaxZ]} (inclusive).
     * Pass {@code Integer.MIN_VALUE / Integer.MAX_VALUE} to disable XZ clipping (e.g., sapling growth).
     */
    public static void placeVine(
            WorldGenLevel level,
            VineSpec vine,
            int clipMinX, int clipMaxX,
            int clipMinZ, int clipMaxZ,
            int flags,
            ReplacePredicate replacePredicate) {

        if (vine.maxX < clipMinX || vine.minX > clipMaxX || vine.maxZ < clipMinZ || vine.minZ > clipMaxZ) {
            return;
        }

        BlockState vineState = ModBlocks.VILE_VINE_BLOCK.get().defaultBlockState();
        int worldMinY = level.getMinBuildHeight() + 5; // strictly protect bedrock floor
        int worldMaxY = level.getMaxBuildHeight() - 2;
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

        double[] pCurr = new double[3];
        double[] pNext = new double[3];

        // Adaptive step along Y so even steep 60-degree vines remain 100% solid with zero gaps
        double stepY = 0.45 / Math.max(1.0, Math.tan(vine.tiltRad));
        stepY = Mth.clamp(stepY, 0.22, 0.55);

        double startS = -vine.depthBelow;
        double endS = vine.heightAbove;
        double maxPad = vine.baseWidth + 3.0;

        for (double s = startS; s <= endS; s += stepY) {
            double y = vine.baseY + s;
            if (y < worldMinY - 4 || y > worldMaxY + 4) {
                continue;
            }

            vine.sampleSpine(s, pCurr);
            if (pCurr[0] + maxPad < clipMinX || pCurr[0] - maxPad > clipMaxX
                    || pCurr[2] + maxPad < clipMinZ || pCurr[2] - maxPad > clipMaxZ) {
                continue;
            }

            vine.sampleSpine(s + 0.5, pNext);
            double tx = pNext[0] - pCurr[0];
            double ty = pNext[1] - pCurr[1];
            double tz = pNext[2] - pCurr[2];
            double tLen = Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (tLen < 1e-4) {
                tx = 0; ty = 1; tz = 0;
            } else {
                tx /= tLen; ty /= tLen; tz /= tLen;
            }

            // Build orthonormal basis (rx, ry, rz) and (fx, fy, fz) perpendicular to tangent T
            double refX = Math.abs(tx) < 0.85 ? 1.0 : 0.0;
            double refY = 0.0;
            double refZ = Math.abs(tx) < 0.85 ? 0.0 : 1.0;
            // R = normalize(T x ref)
            double rx = ty * refZ - tz * refY;
            double ry = tz * refX - tx * refZ;
            double rz = tx * refY - ty * refX;
            double rLen = Math.sqrt(rx * rx + ry * ry + rz * rz);
            rx /= rLen; ry /= rLen; rz /= rLen;
            // F = T x R
            double fx = ty * rz - tz * ry;
            double fy = tz * rx - tx * rz;
            double fz = tx * ry - ty * rx;

            double localWidth = vine.widthAt(s);
            double orbitR = localWidth * 0.21;
            double strandR = Math.max(0.88, localWidth * 0.29);
            boolean aboveBase = s >= 0.0;

            // Solid central tie-in so strands never split apart with gaps
            if (localWidth >= 2.6) {
                stampSphereClipped(level, mPos, vineState,
                        pCurr[0], pCurr[1], pCurr[2],
                        Math.max(0.72, localWidth * 0.24),
                        clipMinX, clipMaxX, clipMinZ, clipMaxZ,
                        worldMinY, worldMaxY, flags, aboveBase, replacePredicate);
            }

            // Braided twisting helical rope strands
            double baseAngle = vine.twistPhase + s * vine.twistRate;
            for (int k = 0; k < vine.strandCount; k++) {
                double ang = baseAngle + (2.0 * Math.PI * k) / vine.strandCount;
                double ca = Math.cos(ang);
                double sa = Math.sin(ang);

                // Subtle organic ridge swell along each strand
                double gnarl = 1.0 + 0.14 * Math.sin(s * 0.45 + k * 2.1 + vine.twistPhase);
                double cx = pCurr[0] + (rx * ca + fx * sa) * orbitR;
                double cy = pCurr[1] + (ry * ca + fy * sa) * orbitR;
                double cz = pCurr[2] + (rz * ca + fz * sa) * orbitR;

                stampSphereClipped(level, mPos, vineState,
                        cx, cy, cz,
                        strandR * gnarl,
                        clipMinX, clipMaxX, clipMinZ, clipMaxZ,
                        worldMinY, worldMaxY, flags, aboveBase, replacePredicate);
            }
        }

        // Curling offshoot tendrils
        for (TendrilSpec tendril : vine.tendrils) {
            placeTendril(level, mPos, vineState, tendril, vine.baseY,
                    clipMinX, clipMaxX, clipMinZ, clipMaxZ,
                    worldMinY, worldMaxY, flags, replacePredicate);
        }

        // Giant sculpted Vile Leaves (mostly concentrated on tendrils/branches, with a few along the main vine)
        if (!vine.leaves.isEmpty()) {
            BlockState leafState = ModBlocks.VILE_LEAVES.get().defaultBlockState()
                    .setValue(LeavesBlock.PERSISTENT, true);
            for (LeafSpec leaf : vine.leaves) {
                placeLeaf(level, mPos, leafState, leaf,
                        clipMinX, clipMaxX, clipMinZ, clipMaxZ,
                        worldMinY, worldMaxY, flags);
            }
        }
    }

    /**
     * Deterministically generates giant botanical leaf blades for a vine, placing ~80-85% along its
     * curling branches/tendrils and a few accent leaves on the main trunk &amp; upper crown.
     */
    private static List<LeafSpec> buildLeaves(VineSpec vine) {
        if (vine.tendrils.isEmpty() && vine.heightAbove < 12.0) {
            return List.of();
        }

        long seed = Double.doubleToLongBits(vine.baseX) * 341873128712L
                ^ Double.doubleToLongBits(vine.baseY) * 132897987541L
                ^ Double.doubleToLongBits(vine.baseZ) * 42317861L
                ^ Double.doubleToLongBits(vine.twistPhase) * 918273645L
                ^ Double.doubleToLongBits(vine.azimuth) * 2654435761L;
        RandomSource rnd = RandomSource.create(seed);

        List<LeafSpec> leaves = new ArrayList<>();
        double[] p0 = new double[3];
        double[] p1 = new double[3];

        // 1. Primary foliage: 3-5 giant leaves per tendril/branch (alternating side leaves + terminal tip leaves)
        for (TendrilSpec t : vine.tendrils) {
            int sideLeafCount = 2 + rnd.nextInt(2); // 2..3 along the branch body
            int sideSign = rnd.nextBoolean() ? 1 : -1;

            for (int i = 0; i < sideLeafCount; i++) {
                double frac = 0.28 + ((double) i / Math.max(1, sideLeafCount)) * 0.52 + (rnd.nextDouble() - 0.5) * 0.10;
                frac = Mth.clamp(frac, 0.22, 0.84);
                double d = frac * t.length;
                t.samplePoint(d, p0);
                t.samplePoint(Math.min(t.length, d + 0.6), p1);

                double tx = p1[0] - p0[0];
                double tz = p1[2] - p0[2];
                double baseAz = (tx * tx + tz * tz) > 1e-4 ? Math.atan2(tz, tx) : Math.atan2(t.dirZ, t.dirX);
                double fanAngle = sideSign * Math.toRadians(42.0 + rnd.nextDouble() * 34.0);
                sideSign = -sideSign;
                double leafAz = baseAz + fanAngle;
                double pitchRad = Math.toRadians(-6.0 + rnd.nextDouble() * 34.0);

                double cp = Math.cos(pitchRad);
                double dx = Math.cos(leafAz) * cp;
                double dy = Math.sin(pitchRad);
                double dz = Math.sin(leafAz) * cp;

                double length = 7.0 + rnd.nextDouble() * 5.2;
                double halfW = length * (0.31 + rnd.nextDouble() * 0.08);
                double roll = Math.toRadians((rnd.nextDouble() - 0.5) * 44.0);
                double arch = length * (0.12 + rnd.nextDouble() * 0.07);
                double droop = length * (0.18 + rnd.nextDouble() * 0.11);
                double cup = 0.8 + rnd.nextDouble() * 0.7;

                leaves.add(new LeafSpec(
                        p0[0], p0[1], p0[2],
                        dx, dy, dz,
                        roll, length, halfW, arch, droop, cup,
                        rnd.nextDouble() * Math.PI * 2.0));
            }

            // 1-2 terminal leaves blooming out from the curling tendril tip
            int tipLeaves = rnd.nextFloat() < 0.65F ? 2 : 1;
            t.samplePoint(t.length * 0.90, p0);
            t.samplePoint(t.length, p1);
            double tx = p1[0] - p0[0];
            double tz = p1[2] - p0[2];
            double tipAz = (tx * tx + tz * tz) > 1e-4 ? Math.atan2(tz, tx) : Math.atan2(t.dirZ, t.dirX);

            for (int k = 0; k < tipLeaves; k++) {
                double spread = tipLeaves == 1
                        ? (rnd.nextDouble() - 0.5) * 0.45
                        : ((k == 0 ? -1.0 : 1.0) * Math.toRadians(28.0 + rnd.nextDouble() * 24.0));
                double leafAz = tipAz + spread;
                double pitchRad = Math.toRadians(2.0 + rnd.nextDouble() * 30.0);
                double cp = Math.cos(pitchRad);

                double length = 7.5 + rnd.nextDouble() * 5.0;
                double halfW = length * (0.30 + rnd.nextDouble() * 0.08);
                double roll = Math.toRadians((rnd.nextDouble() - 0.5) * 38.0);
                double arch = length * (0.13 + rnd.nextDouble() * 0.07);
                double droop = length * (0.20 + rnd.nextDouble() * 0.10);
                double cup = 0.8 + rnd.nextDouble() * 0.7;

                leaves.add(new LeafSpec(
                        p1[0], p1[1], p1[2],
                        Math.cos(leafAz) * cp, Math.sin(pitchRad), Math.sin(leafAz) * cp,
                        roll, length, halfW, arch, droop, cup,
                        rnd.nextDouble() * Math.PI * 2.0));
            }
        }

        // 2. Sparse accent leaves along the upper main vine & crown (~15-20% of total foliage)
        if (vine.heightAbove >= 18.0) {
            int trunkLeaves = 2 + rnd.nextInt(3); // 2..4 scattered accent leaves
            for (int i = 0; i < trunkLeaves; i++) {
                double frac = 0.24 + ((double) i / trunkLeaves) * 0.68 + (rnd.nextDouble() - 0.5) * 0.10;
                double s = Mth.clamp(frac, 0.20, 0.94) * vine.heightAbove;
                vine.sampleSpine(s, p0);

                double leafAz = rnd.nextDouble() * Math.PI * 2.0;
                double pitchRad = Math.toRadians(-4.0 + rnd.nextDouble() * 30.0);
                double cp = Math.cos(pitchRad);

                double length = 8.5 + rnd.nextDouble() * 5.5;
                double halfW = length * (0.31 + rnd.nextDouble() * 0.07);
                double roll = Math.toRadians((rnd.nextDouble() - 0.5) * 40.0);
                double arch = length * (0.12 + rnd.nextDouble() * 0.06);
                double droop = length * (0.20 + rnd.nextDouble() * 0.10);
                double cup = 0.9 + rnd.nextDouble() * 0.7;

                // Anchor near the outer edge of the braided trunk so the full blade emerges cleanly
                double rOut = vine.widthAt(s) * 0.35;
                leaves.add(new LeafSpec(
                        p0[0] + Math.cos(leafAz) * rOut, p0[1], p0[2] + Math.sin(leafAz) * rOut,
                        Math.cos(leafAz) * cp, Math.sin(pitchRad), Math.sin(leafAz) * cp,
                        roll, length, halfW, arch, droop, cup,
                        rnd.nextDouble() * Math.PI * 2.0));
            }

            // 1-2 crown leaves fanning from the top tip of the vine
            int crownLeaves = 1 + rnd.nextInt(2);
            vine.sampleSpine(vine.heightAbove * 0.96, p0);
            double crownBaseAz = rnd.nextDouble() * Math.PI * 2.0;
            for (int k = 0; k < crownLeaves; k++) {
                double leafAz = crownBaseAz + (Math.PI * 2.0 * k / crownLeaves) + (rnd.nextDouble() - 0.5) * 0.6;
                double pitchRad = Math.toRadians(8.0 + rnd.nextDouble() * 24.0);
                double cp = Math.cos(pitchRad);

                double length = 8.0 + rnd.nextDouble() * 5.0;
                double halfW = length * (0.30 + rnd.nextDouble() * 0.07);
                leaves.add(new LeafSpec(
                        p0[0], p0[1], p0[2],
                        Math.cos(leafAz) * cp, Math.sin(pitchRad), Math.sin(leafAz) * cp,
                        Math.toRadians((rnd.nextDouble() - 0.5) * 32.0),
                        length, halfW,
                        length * 0.14, length * 0.22, 0.9 + rnd.nextDouble() * 0.6,
                        rnd.nextDouble() * Math.PI * 2.0));
            }
        }

        return leaves;
    }

    private static void placeLeaf(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mPos,
            BlockState leafState,
            LeafSpec leaf,
            int clipMinX, int clipMaxX,
            int clipMinZ, int clipMaxZ,
            int worldMinY, int worldMaxY,
            int flags) {

        if (leaf.maxX < clipMinX || leaf.minX > clipMaxX || leaf.maxZ < clipMinZ || leaf.minZ > clipMaxZ) {
            return;
        }

        int uSteps = Math.max(12, Mth.ceil(leaf.length / 0.38));
        for (int ui = 0; ui <= uSteps; ui++) {
            double u = (double) ui / uSteps;
            // Ovate-lanceolate botanical profile: widens quickly near base (peak ~u=0.35), tapers to sharp tip
            double envelope = Math.sin(Math.pow(u, 0.72) * Math.PI);
            double serration = 1.0 + 0.07 * Math.sin(u * 12.0 + leaf.wavePhase);
            double halfW = leaf.maxHalfWidth * envelope * serration;
            if (u < 0.09) {
                halfW = Math.max(halfW, 0.55); // solid petiole attachment into branch
            }

            double distAlong = u * leaf.length;
            double archOffset = leaf.archAmp * Math.sin(u * Math.PI) - leaf.droopAmp * (u * u);
            double cupScale = leaf.cupAmp * Math.sin(u * Math.PI);

            int vSteps = Math.max(1, Mth.ceil(halfW / 0.42));
            for (int vi = -vSteps; vi <= vSteps; vi++) {
                double v = (double) vi / vSteps; // -1 .. +1
                double distAcross = v * halfW;
                double distUp = archOffset + cupScale * Math.abs(v);

                double px = leaf.ox + leaf.fx * distAlong + leaf.rx * distAcross + leaf.ux * distUp;
                double py = leaf.oy + leaf.fy * distAlong + leaf.ry * distAcross + leaf.uy * distUp;
                double pz = leaf.oz + leaf.fz * distAlong + leaf.rz * distAcross + leaf.uz * distUp;

                tryPlaceLeafBlock(level, mPos, leafState,
                        Mth.floor(px), Mth.floor(py), Mth.floor(pz),
                        clipMinX, clipMaxX, clipMinZ, clipMaxZ,
                        worldMinY, worldMaxY, flags);

                // Subtle 1-block underside reinforcement along the central midrib near the base for gap-free curvature
                if (u < 0.65 && Math.abs(distAcross) < 0.85) {
                    tryPlaceLeafBlock(level, mPos, leafState,
                            Mth.floor(px), Mth.floor(py - 0.75), Mth.floor(pz),
                            clipMinX, clipMaxX, clipMinZ, clipMaxZ,
                            worldMinY, worldMaxY, flags);
                }
            }
        }
    }

    private static void tryPlaceLeafBlock(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mPos,
            BlockState leafState,
            int x, int y, int z,
            int clipMinX, int clipMaxX,
            int clipMinZ, int clipMaxZ,
            int worldMinY, int worldMaxY,
            int flags) {
        if (x < clipMinX || x > clipMaxX || z < clipMinZ || z > clipMaxZ || y < worldMinY || y > worldMaxY) {
            return;
        }
        mPos.set(x, y, z);
        BlockState existing = level.getBlockState(mPos);
        // Never overwrite vine wood, terrain, or structures — only fill open air / replaceable foliage
        if (existing.isAir() || existing.canBeReplaced()) {
            level.setBlock(mPos, leafState, flags);
        }
    }

    private static void placeTendril(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mPos,
            BlockState vineState,
            TendrilSpec t,
            double vineBaseY,
            int clipMinX, int clipMaxX,
            int clipMinZ, int clipMaxZ,
            int worldMinY, int worldMaxY,
            int flags,
            ReplacePredicate replacePredicate) {

        if (t.maxX < clipMinX || t.minX > clipMaxX || t.maxZ < clipMinZ || t.minZ > clipMaxZ) {
            return;
        }

        // Orthonormal basis perpendicular to tendril direction
        double refX = Math.abs(t.dirX) < 0.85 ? 1.0 : 0.0;
        double refZ = Math.abs(t.dirX) < 0.85 ? 0.0 : 1.0;
        double rx = t.dirY * refZ;
        double ry = t.dirZ * refX - t.dirX * refZ;
        double rz = -t.dirY * refX;
        double rLen = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rLen < 1e-4) {
            rx = 1; ry = 0; rz = 0;
        } else {
            rx /= rLen; ry /= rLen; rz /= rLen;
        }
        double fx = t.dirY * rz - t.dirZ * ry;
        double fy = t.dirZ * rx - t.dirX * rz;
        double fz = t.dirX * ry - t.dirY * rx;

        double step = 0.25;
        boolean hasPrev = false;
        int prevX = 0, prevY = 0, prevZ = 0;

        // Start slightly embedded (-1.0) inside the main vine so the branch root is firmly fused
        for (double d = -1.0; d <= t.length; d += step) {
            double frac = Mth.clamp(d / Math.max(1.0, t.length), 0.0, 1.0);
            double curlGrowth = frac * t.curlAmp;
            double ang = t.curlPhase + Math.max(0.0, d) * t.curlRate;
            double ca = Math.cos(ang);
            double sa = Math.sin(ang);

            // Slight upward hook at the curling tip
            double hookY = frac * frac * 2.5;
            double cx = t.startX + t.dirX * d + (rx * ca + fx * sa) * curlGrowth;
            double cy = t.startY + t.dirY * d + (ry * ca + fy * sa) * curlGrowth + hookY;
            double cz = t.startZ + t.dirZ * d + (rz * ca + fz * sa) * curlGrowth;

            // Flared base tapering smoothly while staying >= 0.85 so branches never break into floating blocks
            double baseFlare = d < 2.5 ? Mth.lerp(Mth.clamp((d + 1.0) / 3.5, 0.0, 1.0), 1.30, 1.0) : 1.0;
            double radius = Math.max(0.84, (t.baseWidth * 0.55) * baseFlare * Mth.lerp(frac, 1.0, 0.58));
            boolean aboveBase = cy >= vineBaseY;

            stampSphereClipped(level, mPos, vineState,
                    cx, cy, cz, radius,
                    clipMinX, clipMaxX, clipMinZ, clipMaxZ,
                    worldMinY, worldMaxY, flags, aboveBase, replacePredicate);

            // 6-connected orthogonal bridge between consecutive voxel centers so thin tips never touch only by corners
            int vx = Mth.floor(cx);
            int vy = Mth.floor(cy);
            int vz = Mth.floor(cz);
            if (hasPrev) {
                int bx = prevX, by = prevY, bz = prevZ;
                while (bx != vx || by != vy || bz != vz) {
                    if (bx != vx) {
                        bx += Integer.compare(vx, bx);
                    } else if (bz != vz) {
                        bz += Integer.compare(vz, bz);
                    } else {
                        by += Integer.compare(vy, by);
                    }
                    if (bx >= clipMinX && bx <= clipMaxX && bz >= clipMinZ && bz <= clipMaxZ
                            && by >= worldMinY && by <= worldMaxY) {
                        mPos.set(bx, by, bz);
                        BlockState existing = level.getBlockState(mPos);
                        if (!existing.is(vineState.getBlock()) && !existing.is(Blocks.BEDROCK)
                                && replacePredicate.canReplace(level, mPos, existing, aboveBase)) {
                            level.setBlock(mPos, vineState, flags);
                        }
                    }
                }
            }
            prevX = vx;
            prevY = vy;
            prevZ = vz;
            hasPrev = true;
        }
    }

    private static void stampSphereClipped(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mPos,
            BlockState state,
            double cx, double cy, double cz,
            double radius,
            int clipMinX, int clipMaxX,
            int clipMinZ, int clipMaxZ,
            int worldMinY, int worldMaxY,
            int flags,
            boolean aboveBase,
            ReplacePredicate replacePredicate) {

        int rCeil = Mth.ceil(radius);
        int x0 = Math.max(clipMinX, Mth.floor(cx) - rCeil);
        int x1 = Math.min(clipMaxX, Mth.floor(cx) + rCeil);
        int z0 = Math.max(clipMinZ, Mth.floor(cz) - rCeil);
        int z1 = Math.min(clipMaxZ, Mth.floor(cz) + rCeil);
        if (x0 > x1 || z0 > z1) {
            return;
        }
        int y0 = Math.max(worldMinY, Mth.floor(cy) - rCeil);
        int y1 = Math.min(worldMaxY, Mth.floor(cy) + rCeil);
        if (y0 > y1) {
            return;
        }

        double rSq = radius * radius;
        for (int x = x0; x <= x1; x++) {
            double dx = (x + 0.5) - cx;
            double dxSq = dx * dx;
            if (dxSq > rSq) continue;
            for (int z = z0; z <= z1; z++) {
                double dz = (z + 0.5) - cz;
                double dXZSq = dxSq + dz * dz;
                if (dXZSq > rSq) continue;
                for (int y = y0; y <= y1; y++) {
                    double dy = (y + 0.5) - cy;
                    if (dXZSq + dy * dy > rSq) continue;

                    mPos.set(x, y, z);
                    BlockState existing = level.getBlockState(mPos);
                    if (existing.is(state.getBlock()) || existing.is(Blocks.BEDROCK)) {
                        continue;
                    }
                    if (replacePredicate.canReplace(level, mPos, existing, aboveBase)) {
                        level.setBlock(mPos, state, flags);
                    }
                }
            }
        }
    }
}
