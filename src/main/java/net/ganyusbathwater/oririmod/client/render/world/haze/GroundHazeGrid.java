package net.ganyusbathwater.oririmod.client.render.world.haze;

import net.ganyusbathwater.oririmod.block.custom.FogSourceBlock;
import net.ganyusbathwater.oririmod.block.entity.FogSourceBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Sliding-window terrain heightfield &amp; local {@link FogSourceBlock} patch cache.
 * <p>
 * Key behaviors:
 * <ul>
 *   <li>Uses client-synced {@link Heightmap.Types#MOTION_BLOCKING} with canopy/prop filtering so
 *       surface mist rests on true ground (ignoring leaves, floating branches, fences, lanterns,
 *       and non-full-block props).</li>
 *   <li>Applies edge-stopping bilateral smoothing so 45&deg; hills become smooth ramps while floor
 *       haze stays flat and hugs rising walls flush (filling tight alleyways cleanly).</li>
 *   <li>Computes a smooth distance-field {@code openEdgeFade} at open cliff rims, drop-offs, and
 *       unloaded boundaries so mist always dissolves to zero opacity before any open mesh edge.</li>
 *   <li>Tracks night-time and lingering post-rain moisture smoothly for natural biome haze.</li>
 *   <li>Discovers nearby {@link FogSourceBlockEntity} blocks via O(1) chunk BE maps and builds
 *       1-block-precision, wall-hugging, ledge-feathered BFS floor patches for structures &amp; dungeons.</li>
 * </ul>
 */
public final class GroundHazeGrid {

    public static final int CELL_STEP = 2;
    public static final int LOCAL_CELL_STEP = 1;
    public static final float WALL_HEIGHT_THRESHOLD = 3.0F;
    // On a 1-block local grid, 45-degree stairs climb <= 1.0 block per step; >= 1.75 in 1 block is a vertical wall/obstacle
    private static final float LOCAL_STEP_CLIMB_MAX = 1.35F;
    private static final float INVALID_Y = -99999.0F;
    private static final float DEEP_DROP_Y = -88888.0F;
    private static final int LIGHT_REFRESH_SLICES = 8;

    // Sliding surface grid state
    private int gridSize = 0;
    private int halfCells = 0;
    private int originCellX = Integer.MIN_VALUE;
    private int originCellZ = Integer.MIN_VALUE;
    private int anchorCamY = Integer.MIN_VALUE;
    private ClientLevel lastLevel = null;
    private int lightSliceCursor = 0;
    private boolean hasAnyActiveSurfaceHaze = false;

    // Per-vertex raw & smoothed buffers (size = gridSize * gridSize)
    float[] rawY = new float[0];
    float[] smoothY = new float[0];
    float[] openEdgeFade = new float[0]; // 1.0 inside & against rising walls; smoothly 0.0 at open cliff rims & borders
    boolean[] nearSurfaceWall = new boolean[0]; // true if adjacent to a >=3-block rising wall (keeps puffs compact so they don't poke through thin walls)
    GroundHazeProfile[] cellProfile = new GroundHazeProfile[0];

    float[] rawR = new float[0];
    float[] rawG = new float[0];
    float[] rawB = new float[0];
    float[] rawAlpha = new float[0];
    float[] rawLayer1 = new float[0];
    float[] rawLayer2 = new float[0];

    float[] smoothR = new float[0];
    float[] smoothG = new float[0];
    float[] smoothB = new float[0];
    float[] smoothAlpha = new float[0];
    float[] smoothLayer1 = new float[0];
    float[] smoothLayer2 = new float[0];

    int[] packedLight = new int[0];

    // Weather & time-of-day atmospheric modulation
    private float wetness = 0.0F;
    private float smoothNight = 0.0F;
    private int tickCounter = 0;

    // Local FogSourceBlock patches (keyed by BlockPos.asLong())
    private final Map<Long, LocalFogPatch> localPatches = new HashMap<>();
    private final List<LocalFogPatch> activeLocalPatches = new ArrayList<>();
    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();

    public static final class LocalFogPatch {
        public final BlockPos sourcePos;
        public final int density;
        public final FogSourceBlock.FogColor color;
        public final float radius;
        public final float baseAlpha;
        public final int gridSize;
        public final int originX;
        public final int originZ;
        public final float[] smoothY;
        public final float[] alphaMask;    // baseAlpha * smooth radial fade
        public final float[] openEdgeFade; // 1.0 inside & against solid walls; smoothly 0.0 at open drop-offs/edges
        public final boolean[] solidWall;  // true for unvisited rising walls (enables flush wall hugging)
        public final byte[] wallDist;      // 1 = adjacent to solid wall, 2 = 2 blocks away, 3 = open interior
        public final boolean[] narrowAlley; // true if walls flank opposite sides (ensures dense puff fill in 1-block alleys)
        public final int[] packedLight;
        long lastSeenTick;
        int lightRefreshTick;

        LocalFogPatch(BlockPos sourcePos, int density, FogSourceBlock.FogColor color, int gridSize, int originX, int originZ) {
            this.sourcePos = sourcePos.immutable();
            this.density = density;
            this.color = color;
            this.radius = FogSourceBlock.getRadiusForDensity(density);
            this.baseAlpha = FogSourceBlock.getAlphaForDensity(density);
            this.gridSize = gridSize;
            this.originX = originX;
            this.originZ = originZ;
            int n = gridSize * gridSize;
            this.smoothY = new float[n];
            this.alphaMask = new float[n];
            this.openEdgeFade = new float[n];
            this.solidWall = new boolean[n];
            this.wallDist = new byte[n];
            this.narrowAlley = new boolean[n];
            this.packedLight = new int[n];
        }
    }

    public void reset() {
        this.lastLevel = null;
        this.originCellX = Integer.MIN_VALUE;
        this.originCellZ = Integer.MIN_VALUE;
        this.anchorCamY = Integer.MIN_VALUE;
        this.hasAnyActiveSurfaceHaze = false;
        this.localPatches.clear();
        this.activeLocalPatches.clear();
    }

    public int getGridSize() {
        return gridSize;
    }

    public int getOriginWorldX() {
        return originCellX * CELL_STEP;
    }

    public int getOriginWorldZ() {
        return originCellZ * CELL_STEP;
    }

    public boolean hasActiveSurfaceHaze() {
        return hasAnyActiveSurfaceHaze;
    }

    public List<LocalFogPatch> getActiveLocalPatches() {
        return activeLocalPatches;
    }

    public float getNightFactor() {
        return smoothNight;
    }

    public float getPostRainFactor() {
        return wetness;
    }

    /**
     * Called once per client tick to advance weather/night atmosphere, shift the sliding heightfield
     * if the player moved across a 2-block boundary, retry newly loaded chunks, refresh light slices,
     * and discover nearby FogSource blocks.
     */
    public void tick(ClientLevel level, double camX, double camY, double camZ, int configRadius) {
        if (level != this.lastLevel) {
            reset();
            this.lastLevel = level;
            this.wetness = level.getRainLevel(1.0F);
            this.smoothNight = computeNightFactor(level);
        }

        tickCounter++;

        // 1. Track night & lingering post-rain atmospheric moisture
        float targetNight = computeNightFactor(level);
        this.smoothNight = Mth.lerp(0.04F, this.smoothNight, targetNight);

        float rainLevel = level.getRainLevel(1.0F);
        if (rainLevel > 0.05F) {
            this.wetness = Math.min(1.0F, this.wetness + 0.0035F * rainLevel);
        } else if (this.wetness > 0.0F) {
            this.wetness = Math.max(0.0F, this.wetness - (1.0F / 3000.0F));
        }

        // 2. Ensure surface grid allocation matches configured radius
        int clampedRadius = Mth.clamp(configRadius, 24, 64);
        int desiredHalfCells = (clampedRadius + CELL_STEP - 1) / CELL_STEP;
        int desiredSize = desiredHalfCells * 2 + 1;
        if (desiredSize != this.gridSize) {
            allocateSurfaceGrid(desiredSize, desiredHalfCells);
            this.originCellX = Integer.MIN_VALUE;
            this.originCellZ = Integer.MIN_VALUE;
        }

        int centerCellX = Math.floorDiv(Mth.floor(camX), CELL_STEP);
        int centerCellZ = Math.floorDiv(Mth.floor(camZ), CELL_STEP);
        int targetOriginX = centerCellX - halfCells;
        int targetOriginZ = centerCellZ - halfCells;
        int currentCamY = Mth.floor(camY);

        boolean geometryChanged = false;
        boolean bigVerticalMove = (anchorCamY == Integer.MIN_VALUE || Math.abs(currentCamY - anchorCamY) >= 8);
        if (originCellX == Integer.MIN_VALUE || bigVerticalMove
                || Math.abs(targetOriginX - originCellX) >= gridSize
                || Math.abs(targetOriginZ - originCellZ) >= gridSize) {
            originCellX = targetOriginX;
            originCellZ = targetOriginZ;
            anchorCamY = currentCamY;
            fillAllSurfaceColumns(level);
            geometryChanged = true;
        } else if (targetOriginX != originCellX || targetOriginZ != originCellZ) {
            shiftSurfaceGrid(level, targetOriginX - originCellX, targetOriginZ - originCellZ);
            originCellX = targetOriginX;
            originCellZ = targetOriginZ;
            geometryChanged = true;
        }

        // 3. Incremental lightmap refresh + retry any previously unloaded INVALID_Y cells whose chunk arrived
        if (refreshSurfaceSliceAndPendingChunks(level)) {
            geometryChanged = true;
        }

        if (geometryChanged) {
            if ((tickCounter % 10) == 0) {
                refreshRawAlphaFromProfiles();
            }
            rebuildSurfaceSmoothingAndEdges();
        } else if ((tickCounter % 10) == 0) {
            refreshRawAlphaFromProfiles();
            smoothBiomeColorsAndAlpha();
        }

        // 4. Discover & maintain local FogSourceBlock patches every 4 ticks (O(1) chunk BE scan)
        if ((tickCounter & 3) == 0 || geometryChanged) {
            updateLocalFogPatches(level, camX, camY, camZ, clampedRadius + 18);
        }
    }

    private static float computeNightFactor(ClientLevel level) {
        // getTimeOfDay: 0.0 = noon, 0.25 = sunset, 0.5 = midnight (Elderwoods fixed_time=18000), 0.75 = sunrise
        float timeOfDay = level.getTimeOfDay(1.0F);
        float sunCos = Mth.cos(timeOfDay * Mth.TWO_PI);
        float t = Mth.clamp((0.22F - sunCos) / 0.40F, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private void allocateSurfaceGrid(int size, int half) {
        this.gridSize = size;
        this.halfCells = half;
        int n = size * size;
        rawY = new float[n];
        smoothY = new float[n];
        openEdgeFade = new float[n];
        nearSurfaceWall = new boolean[n];
        cellProfile = new GroundHazeProfile[n];
        Arrays.fill(cellProfile, GroundHazeProfile.NONE);
        rawR = new float[n];
        rawG = new float[n];
        rawB = new float[n];
        rawAlpha = new float[n];
        rawLayer1 = new float[n];
        rawLayer2 = new float[n];
        smoothR = new float[n];
        smoothG = new float[n];
        smoothB = new float[n];
        smoothAlpha = new float[n];
        smoothLayer1 = new float[n];
        smoothLayer2 = new float[n];
        packedLight = new int[n];
    }

    private void fillAllSurfaceColumns(ClientLevel level) {
        for (int gz = 0; gz < gridSize; gz++) {
            for (int gx = 0; gx < gridSize; gx++) {
                sampleSurfaceCell(level, gx, gz);
            }
        }
    }

    private void shiftSurfaceGrid(ClientLevel level, int dx, int dz) {
        int xStart = dx > 0 ? 0 : gridSize - 1;
        int xEnd = dx > 0 ? gridSize : -1;
        int xStep = dx > 0 ? 1 : -1;

        int zStart = dz > 0 ? 0 : gridSize - 1;
        int zEnd = dz > 0 ? gridSize : -1;
        int zStep = dz > 0 ? 1 : -1;

        for (int gz = zStart; gz != zEnd; gz += zStep) {
            int srcZ = gz + dz;
            for (int gx = xStart; gx != xEnd; gx += xStep) {
                int srcX = gx + dx;
                int dstIdx = gz * gridSize + gx;
                if (srcX >= 0 && srcX < gridSize && srcZ >= 0 && srcZ < gridSize) {
                    int srcIdx = srcZ * gridSize + srcX;
                    rawY[dstIdx] = rawY[srcIdx];
                    cellProfile[dstIdx] = cellProfile[srcIdx];
                    rawR[dstIdx] = rawR[srcIdx];
                    rawG[dstIdx] = rawG[srcIdx];
                    rawB[dstIdx] = rawB[srcIdx];
                    rawAlpha[dstIdx] = rawAlpha[srcIdx];
                    rawLayer1[dstIdx] = rawLayer1[srcIdx];
                    rawLayer2[dstIdx] = rawLayer2[srcIdx];
                    packedLight[dstIdx] = packedLight[srcIdx];
                }
            }
        }

        originCellX += dx;
        originCellZ += dz;

        for (int gz = 0; gz < gridSize; gz++) {
            boolean newRow = (dz > 0 && gz >= gridSize - dz) || (dz < 0 && gz < -dz);
            for (int gx = 0; gx < gridSize; gx++) {
                boolean newCol = (dx > 0 && gx >= gridSize - dx) || (dx < 0 && gx < -dx);
                if (newRow || newCol) {
                    sampleSurfaceCell(level, gx, gz);
                }
            }
        }

        originCellX -= dx;
        originCellZ -= dz;
    }

    private void sampleSurfaceCell(ClientLevel level, int gx, int gz) {
        int idx = gz * gridSize + gx;
        int wx = (originCellX + gx) * CELL_STEP;
        int wz = (originCellZ + gz) * CELL_STEP;

        if (!level.getChunkSource().hasChunk(wx >> 4, wz >> 4)) {
            rawY[idx] = INVALID_Y;
            cellProfile[idx] = GroundHazeProfile.NONE;
            rawAlpha[idx] = 0.0F;
            packedLight[idx] = LightTexture.FULL_BRIGHT;
            return;
        }

        float groundY = sampleCanopySafeSurfaceY(level, wx, wz);
        rawY[idx] = groundY;

        if (groundY <= INVALID_Y + 1.0F) {
            cellProfile[idx] = GroundHazeProfile.NONE;
            rawAlpha[idx] = 0.0F;
            return;
        }

        int sampleY = Mth.floor(groundY);
        scratchPos.set(wx, sampleY, wz);
        Holder<Biome> biome = level.getBiome(scratchPos);
        GroundHazeProfile profile = GroundHazeRegistry.getProfile(biome);

        cellProfile[idx] = profile;
        rawR[idx] = profile.r();
        rawG[idx] = profile.g();
        rawB[idx] = profile.b();
        rawLayer1[idx] = profile.layer1Height();
        rawLayer2[idx] = profile.layer2Height();
        rawAlpha[idx] = computeProfileAlpha(profile);

        scratchPos.set(wx, sampleY + 1, wz);
        packedLight[idx] = sampleSurfacePackedLight(level, scratchPos);
    }

    private void refreshRawAlphaFromProfiles() {
        int n = gridSize * gridSize;
        for (int idx = 0; idx < n; idx++) {
            if (rawY[idx] <= INVALID_Y + 1.0F) {
                rawAlpha[idx] = 0.0F;
            } else {
                rawAlpha[idx] = computeProfileAlpha(cellProfile[idx]);
            }
        }
    }

    private float computeProfileAlpha(GroundHazeProfile profile) {
        if (profile == null || profile.maxAlpha() <= 0.001F) {
            return 0.0F;
        }
        float nightDriver = smoothNight * profile.nightWeight();
        float rainDriver = wetness * profile.postRainWeight();
        float combined = Math.max(nightDriver, rainDriver);
        if (nightDriver > 0.3F && rainDriver > 0.3F) {
            combined = Math.min(1.18F, combined + 0.18F * Math.min(nightDriver, rainDriver));
        }
        float ratio = Mth.lerp(Mth.clamp(combined, 0.0F, 1.0F), profile.dryDayRatio(), 1.0F);
        if (combined > 1.0F) {
            ratio *= combined;
        }
        return profile.maxAlpha() * ratio;
    }

    private void rebuildSurfaceSmoothingAndEdges() {
        int n = gridSize * gridSize;
        // 1. Two-pass edge-stopping (bilateral) 3x3 smoothing:
        //    Keeps continuous 45-degree hills faithful while leveling out isolated 1-block bumps
        //    so small rubble/curbs sit submerged inside the fog instead of launching layered ramps.
        float[] tmpY = new float[n];
        bilateralSmoothSurfacePass(rawY, tmpY, 1.0F);
        bilateralSmoothSurfacePass(tmpY, smoothY, 1.0F);

        // 2. Smooth distance-field feathering at open cliff rims, deep holes, abrupt descents,
        //    and unloaded chunk borders — checked on BOTH rawY and smoothY with threshold 2.2F
        //    (strictly below MAX_QUAD_DELTA_Y = 2.6F) so NO clamped or skipped rim ever stays opaque!
        float[] openDist = new float[n];
        for (int gz = 0; gz < gridSize; gz++) {
            for (int gx = 0; gx < gridSize; gx++) {
                int idx = gz * gridSize + gx;
                float cy = smoothY[idx];
                float rcy = rawY[idx];
                if (cy <= INVALID_Y + 1.0F || rcy <= INVALID_Y + 1.0F) {
                    openDist[idx] = 0.0F;
                    continue;
                }
                boolean isOpenRim = false;
                boolean touchesWall = false;
                for (int dz = -1; dz <= 1; dz++) {
                    int nz = gz + dz;
                    if (nz < 0 || nz >= gridSize) continue;
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dz == 0) continue;
                        int nx = gx + dx;
                        if (nx < 0 || nx >= gridSize) continue;
                        int nIdx = nz * gridSize + nx;
                        float ny = smoothY[nIdx];
                        float rny = rawY[nIdx];
                        if (ny <= INVALID_Y + 1.0F || rny <= INVALID_Y + 1.0F
                                || (cy - ny) >= 2.2F || (rcy - rny) >= 2.4F) {
                            // Looking down over a cliff rim, deep hole, or abrupt descent
                            isOpenRim = true;
                        } else if ((rny - rcy) >= WALL_HEIGHT_THRESHOLD) {
                            touchesWall = true;
                        }
                    }
                }
                openDist[idx] = isOpenRim ? 0.0F : 4.0F;
                nearSurfaceWall[idx] = touchesWall;
            }
        }

        // 3 relaxation passes (~6-7 blocks of silky feathering before deep holes & cliff rims)
        for (int pass = 0; pass < 3; pass++) {
            for (int gz = 0; gz < gridSize; gz++) {
                for (int gx = 0; gx < gridSize; gx++) {
                    int idx = gz * gridSize + gx;
                    float best = openDist[idx];
                    if (best <= 0.0F) continue;
                    float cy = smoothY[idx];
                    for (int dz = -1; dz <= 1; dz++) {
                        int nz = gz + dz;
                        if (nz < 0 || nz >= gridSize) continue;
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dz == 0) continue;
                            int nx = gx + dx;
                            if (nx < 0 || nx >= gridSize) continue;
                            int nIdx = nz * gridSize + nx;
                            float ny = smoothY[nIdx];
                            if (ny <= INVALID_Y + 1.0F || Math.abs(ny - cy) >= 2.4F) continue;
                            float step = (dx != 0 && dz != 0) ? 1.414F : 1.0F;
                            float cand = openDist[nIdx] + step;
                            if (cand < best) best = cand;
                        }
                    }
                    openDist[idx] = best;
                }
            }
        }

        for (int i = 0; i < n; i++) {
            float t = Mth.clamp(openDist[i] / 3.4F, 0.0F, 1.0F);
            openEdgeFade[i] = t * t * (3.0F - 2.0F * t);
        }

        // 3. Smooth biome colors & alpha across biome borders
        smoothBiomeColorsAndAlpha();
    }

    private void bilateralSmoothSurfacePass(float[] srcY, float[] dstY, float centerWeight) {
        for (int gz = 0; gz < gridSize; gz++) {
            for (int gx = 0; gx < gridSize; gx++) {
                int idx = gz * gridSize + gx;
                float cy = srcY[idx];
                if (cy <= INVALID_Y + 1.0F) {
                    dstY[idx] = INVALID_Y;
                    continue;
                }

                float sumY = cy * centerWeight;
                float weight = centerWeight;
                for (int dz = -1; dz <= 1; dz++) {
                    int nz = gz + dz;
                    if (nz < 0 || nz >= gridSize) continue;
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dz == 0) continue;
                        int nx = gx + dx;
                        if (nx < 0 || nx >= gridSize) continue;
                        float ny = srcY[nz * gridSize + nx];
                        if (ny <= INVALID_Y + 1.0F) continue;

                        boolean isDiag = (dx != 0 && dz != 0);
                        float maxStep = isDiag ? 3.2F : 2.2F;
                        if (Math.abs(ny - cy) <= maxStep) {
                            float w = isDiag ? 0.75F : 1.0F;
                            sumY += ny * w;
                            weight += w;
                        }
                    }
                }
                dstY[idx] = sumY / weight;
            }
        }
    }

    private void smoothBiomeColorsAndAlpha() {
        boolean any = false;
        for (int gz = 0; gz < gridSize; gz++) {
            for (int gx = 0; gx < gridSize; gx++) {
                int idx = gz * gridSize + gx;
                if (rawY[idx] <= INVALID_Y + 1.0F) {
                    smoothAlpha[idx] = 0.0F;
                    continue;
                }

                float sumA = 0.0F;
                float sumR = 0.0F, sumG = 0.0F, sumB = 0.0F;
                float sumL1 = 0.0F, sumL2 = 0.0F;
                float totalW = 0.0F;
                float colorW = 0.0F;

                for (int dz = -1; dz <= 1; dz++) {
                    int nz = gz + dz;
                    if (nz < 0 || nz >= gridSize) continue;
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = gx + dx;
                        if (nx < 0 || nx >= gridSize) continue;
                        int nIdx = nz * gridSize + nx;
                        if (rawY[nIdx] <= INVALID_Y + 1.0F) continue;
                        float w = (dx == 0 && dz == 0) ? 2.0F : ((dx != 0 && dz != 0) ? 0.7F : 1.0F);
                        float a = rawAlpha[nIdx];
                        sumA += a * w;
                        totalW += w;
                        if (a > 0.001F) {
                            float cw = a * w;
                            sumR += rawR[nIdx] * cw;
                            sumG += rawG[nIdx] * cw;
                            sumB += rawB[nIdx] * cw;
                            sumL1 += rawLayer1[nIdx] * cw;
                            sumL2 += rawLayer2[nIdx] * cw;
                            colorW += cw;
                        }
                    }
                }

                float avgA = totalW > 0.0F ? (sumA / totalW) : 0.0F;
                smoothAlpha[idx] = avgA;
                if (colorW > 0.0001F) {
                    smoothR[idx] = sumR / colorW;
                    smoothG[idx] = sumG / colorW;
                    smoothB[idx] = sumB / colorW;
                    smoothLayer1[idx] = sumL1 / colorW;
                    smoothLayer2[idx] = sumL2 / colorW;
                } else {
                    smoothR[idx] = rawR[idx];
                    smoothG[idx] = rawG[idx];
                    smoothB[idx] = rawB[idx];
                    smoothLayer1[idx] = rawLayer1[idx];
                    smoothLayer2[idx] = rawLayer2[idx];
                }
                if (avgA > 0.004F) {
                    any = true;
                }
            }
        }
        this.hasAnyActiveSurfaceHaze = any;
    }

    private boolean refreshSurfaceSliceAndPendingChunks(ClientLevel level) {
        if (gridSize <= 0) {
            return false;
        }
        int slice = lightSliceCursor;
        lightSliceCursor = (lightSliceCursor + 1) % LIGHT_REFRESH_SLICES;
        boolean newlyLoaded = false;

        for (int gz = slice; gz < gridSize; gz += LIGHT_REFRESH_SLICES) {
            int wz = (originCellZ + gz) * CELL_STEP;
            for (int gx = 0; gx < gridSize; gx++) {
                int idx = gz * gridSize + gx;
                int wx = (originCellX + gx) * CELL_STEP;
                if (rawY[idx] <= INVALID_Y + 1.0F) {
                    if (level.getChunkSource().hasChunk(wx >> 4, wz >> 4)) {
                        sampleSurfaceCell(level, gx, gz);
                        if (rawY[idx] > INVALID_Y + 1.0F) {
                            newlyLoaded = true;
                        }
                    }
                    continue;
                }
                if (smoothAlpha[idx] <= 0.004F) {
                    continue;
                }
                scratchPos.set(wx, Mth.floor(rawY[idx]) + 1, wz);
                packedLight[idx] = sampleSurfacePackedLight(level, scratchPos);
            }
        }
        return newlyLoaded;
    }

    /**
     * Finds the true floor height at (wx, wz) using client-synced {@link Heightmap.Types#MOTION_BLOCKING},
     * while ignoring blocks / floating islands / overhangs high above the player's elevation band.
     * Skips tree canopies (leaves &amp; floating branches), fences, lanterns, and narrow props,
     * while resting at the exact top of full blocks, farmland, dirt paths, mud, soul sand, slabs, stairs, and fluids.
     */
    private float sampleCanopySafeSurfaceY(ClientLevel level, int wx, int wz) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        // IMPORTANT: MOTION_BLOCKING is synced to ClientLevel (MOTION_BLOCKING_NO_LEAVES is server-only!)
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz);
        if (height <= minY) {
            return INVALID_Y;
        }
        int topY = Math.min(maxY, height - 1);

        // If there are blocks/floating islands/overhangs well above the camera, probe the open air band
        // around the camera first so high overhead structures (even 80+ blocks up) never cut out ground fog!
        if (anchorCamY != Integer.MIN_VALUE && topY > anchorCamY + 14) {
            int bandTop = Math.min(topY, anchorCamY + 16);
            int bandBottom = Math.max(minY, anchorCamY - 28);
            int airStart = Integer.MIN_VALUE;

            // Find open passable air inside the camera's vertical band (handles roofs/bridges/floating islands)
            for (int cy = bandTop; cy >= Math.max(minY, anchorCamY - 6); cy--) {
                scratchPos.set(wx, cy, wz);
                BlockState st = level.getBlockState(scratchPos);
                if (st.getFluidState().isEmpty() && getSolidFloorTopOffset(level, scratchPos, st) < 0.0F) {
                    airStart = cy;
                    break;
                }
            }

            if (airStart != Integer.MIN_VALUE) {
                for (int cy = airStart; cy >= bandBottom; cy--) {
                    scratchPos.set(wx, cy, wz);
                    BlockState state = level.getBlockState(scratchPos);
                    if (!state.getFluidState().isEmpty()) {
                        return cy + 0.88F;
                    }
                    float topOffset = getSolidFloorTopOffset(level, scratchPos, state);
                    if (topOffset < 0.0F) {
                        continue;
                    }
                    if (state.is(BlockTags.LOGS) && isFloatingCanopyLog(level, wx, cy, wz)) {
                        continue;
                    }
                    return cy + topOffset;
                }
                // Open air at camera band with no floor within 28 blocks down -> deep pit/void (open edge)
                return INVALID_Y;
            }
            // If the entire camera band is solid rock, this column is a real mountain/cliff wall -> fall through to topY
        }

        int minScan = Math.max(minY, topY - 56);
        for (int cy = topY; cy >= minScan; cy--) {
            scratchPos.set(wx, cy, wz);
            BlockState state = level.getBlockState(scratchPos);
            if (!state.getFluidState().isEmpty()) {
                return cy + 0.88F;
            }
            float topOffset = getSolidFloorTopOffset(level, scratchPos, state);
            if (topOffset < 0.0F) {
                continue;
            }
            // Skip floating tree branches/canopy logs that have air or leaves within 3 blocks underneath
            if (state.is(BlockTags.LOGS) && isFloatingCanopyLog(level, wx, cy, wz)) {
                continue;
            }
            return cy + topOffset;
        }
        return INVALID_Y;
    }

    private boolean isFloatingCanopyLog(ClientLevel level, int wx, int cy, int wz) {
        for (int step = 1; step <= 3; step++) {
            scratchPos.set(wx, cy - step, wz);
            BlockState below = level.getBlockState(scratchPos);
            if (below.isAir() || below.is(BlockTags.LEAVES)) {
                return true;
            }
            if (!below.is(BlockTags.LOGS)) {
                return false;
            }
        }
        return false;
    }

    /**
     * Scans around an indoor/dungeon seed Y to find the local room floor (works under low 2-block
     * archways, beams, overhangs, roofs &amp; caves), resting at the exact top of full blocks,
     * farmland, dirt paths, mud, soul sand, slabs, and stairs while ignoring narrow props.
     * <p>
     * Checks floor-level air ({@code seedY}) FIRST before stepping upward, so overhanging beams or
     * ceilings at {@code seedY + 1} or {@code seedY + 2} are NEVER misclassified as solid walls!
     */
    private float sampleLocalFloorY(ClientLevel level, int wx, int seedY, int wz) {
        int minY = Math.max(level.getMinBuildHeight(), seedY - 6);
        int maxY = level.getMaxBuildHeight() - 1;

        int feetY = Mth.clamp(seedY, minY, maxY);
        scratchPos.set(wx, feetY, wz);
        BlockState feetState = level.getBlockState(scratchPos);

        // Case 1: Floor-level air (or fluid) at seedY -> scan straight down (ignores any ceiling/overhang above!)
        if (!feetState.getFluidState().isEmpty()) {
            return feetY + 0.88F;
        }
        float feetTop = getSolidFloorTopOffset(level, scratchPos, feetState);
        if (feetTop < 0.0F) {
            for (int cy = feetY - 1; cy >= minY; cy--) {
                scratchPos.set(wx, cy, wz);
                BlockState state = level.getBlockState(scratchPos);
                if (!state.getFluidState().isEmpty()) {
                    return cy + 0.88F;
                }
                float topOffset = getSolidFloorTopOffset(level, scratchPos, state);
                if (topOffset >= 0.0F) {
                    return cy + topOffset;
                }
            }
            // Open air at floor level with >6-block drop below -> deep hole (NEVER a solid wall!)
            return DEEP_DROP_Y;
        }

        // Case 2: Solid block at seedY (e.g. a 1-block step/stair/curb) -> only walkable if seedY + 1 is passable!
        int headY = feetY + 1;
        if (headY <= maxY) {
            scratchPos.set(wx, headY, wz);
            BlockState headState = level.getBlockState(scratchPos);
            if (!headState.getFluidState().isEmpty()) {
                return headY + 0.88F;
            }
            if (getSolidFloorTopOffset(level, scratchPos, headState) < 0.0F) {
                return feetY + feetTop;
            }
        }

        // Both seedY and seedY + 1 are physically solid -> true wall (guaranteed to depth-clip all fog slices!)
        return INVALID_Y;
    }

    /**
     * Returns the exact top surface offset (0.35..1.0) for real floor/wall blocks — including
     * full blocks, Farmland (0.9375), Dirt Paths (0.9375), Soul Sand/Mud (0.875), Slabs (0.5/1.0),
     * and Stairs — or {@code -1.0F} for pass-through props (fences, walls, lanterns, chains, doors,
     * leaves, grass, flowers, thin snow).
     */
    private static float getSolidFloorTopOffset(ClientLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.REPLACEABLE)
                || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CLIMBABLE) || state.is(BlockTags.FENCES)
                || state.is(BlockTags.WALLS) || state.is(BlockTags.FENCE_GATES)
                || state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.BANNERS) || state.is(BlockTags.SIGNS)) {
            return -1.0F;
        }
        if (state.is(Blocks.SNOW) && state.hasProperty(SnowLayerBlock.LAYERS)
                && state.getValue(SnowLayerBlock.LAYERS) <= 4) {
            return -1.0F;
        }
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) {
            return -1.0F;
        }
        double widthX = shape.max(Direction.Axis.X) - shape.min(Direction.Axis.X);
        double widthZ = shape.max(Direction.Axis.Z) - shape.min(Direction.Axis.Z);
        double maxY = shape.max(Direction.Axis.Y);
        if (widthX >= 0.75D && widthZ >= 0.75D && maxY >= 0.35D) {
            return (float) maxY;
        }
        return -1.0F;
    }

    private static int sampleSurfacePackedLight(ClientLevel level, BlockPos pos) {
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int sky = level.getBrightness(LightLayer.SKY, pos);
        // Give outdoor moonlit nocturnal mist a soft ambient visibility floor so it doesn't vanish
        // in pitch-black fixed-night dimensions while keeping unlit indoor/shaded areas faithful
        if (sky >= 12 && block < 5) {
            block = 5;
        }
        return LightTexture.pack(block, sky);
    }

    private static int sampleLocalPackedLight(ClientLevel level, BlockPos pos) {
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int sky = level.getBrightness(LightLayer.SKY, pos);
        return LightTexture.pack(block, sky);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Local FogSourceBlock discovery & wall-bounded dungeon floor patch builder
    // ─────────────────────────────────────────────────────────────────────────────

    private void updateLocalFogPatches(ClientLevel level, double camX, double camY, double camZ, int scanRadius) {
        activeLocalPatches.clear();

        int minCx = (Mth.floor(camX) - scanRadius) >> 4;
        int maxCx = (Mth.floor(camX) + scanRadius) >> 4;
        int minCz = (Mth.floor(camZ) - scanRadius) >> 4;
        int maxCz = (Mth.floor(camZ) + scanRadius) >> 4;
        double maxDistSq = (double) scanRadius * scanRadius;

        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof FogSourceBlockEntity)) {
                        continue;
                    }
                    BlockPos pos = be.getBlockPos();
                    double dx = pos.getX() + 0.5 - camX;
                    double dy = pos.getY() + 0.5 - camY;
                    double dz = pos.getZ() + 0.5 - camZ;
                    if (dx * dx + dz * dz > maxDistSq || Math.abs(dy) > 48.0) {
                        continue;
                    }

                    BlockState state = be.getBlockState();
                    if (!(state.getBlock() instanceof FogSourceBlock)) {
                        continue;
                    }

                    int density = state.getValue(FogSourceBlock.DENSITY);
                    FogSourceBlock.FogColor color = state.getValue(FogSourceBlock.COLOR);
                    long key = pos.asLong();
                    LocalFogPatch patch = localPatches.get(key);
                    if (patch == null || patch.density != density || patch.color != color
                            || (tickCounter - patch.lightRefreshTick) >= 40) {
                        patch = buildLocalFogPatch(level, pos, density, color);
                        patch.lightRefreshTick = tickCounter;
                        localPatches.put(key, patch);
                    }
                    patch.lastSeenTick = tickCounter;
                    activeLocalPatches.add(patch);
                }
            }
        }

        if (localPatches.size() > activeLocalPatches.size() + 16) {
            Iterator<Map.Entry<Long, LocalFogPatch>> it = localPatches.entrySet().iterator();
            while (it.hasNext()) {
                if (tickCounter - it.next().getValue().lastSeenTick > 100) {
                    it.remove();
                }
            }
        }
    }

    private LocalFogPatch buildLocalFogPatch(ClientLevel level, BlockPos sourcePos, int density, FogSourceBlock.FogColor color) {
        float radius = FogSourceBlock.getRadiusForDensity(density);
        int half = Mth.ceil(radius / LOCAL_CELL_STEP) + 2;
        int size = half * 2 + 1;
        int ox = sourcePos.getX() - half * LOCAL_CELL_STEP;
        int oz = sourcePos.getZ() - half * LOCAL_CELL_STEP;

        LocalFogPatch patch = new LocalFogPatch(sourcePos, density, color, size, ox, oz);
        int n = size * size;
        float[] locRawY = new float[n];
        boolean[] visited = new boolean[n];
        Arrays.fill(locRawY, INVALID_Y);

        float centerFloor = sampleLocalFloorY(level, sourcePos.getX(), sourcePos.getY(), sourcePos.getZ());
        if (centerFloor <= DEEP_DROP_Y + 1.0F) {
            centerFloor = sourcePos.getY();
        }

        // 1-block-precision BFS: walks smoothly up/down 45-degree slopes (|dy| <= 1.35 per 1 block),
        // treats >=2-block sheer vertical rises (walls, log piles, pillars) as solidWall to hug flush,
        // and stops at sheer drop-offs (feathered smoothly to 0 alpha via openEdgeFade).
        int[] queueX = new int[n];
        int[] queueZ = new int[n];
        int qHead = 0, qTail = 0;

        int centerIdx = half * size + half;
        locRawY[centerIdx] = centerFloor;
        visited[centerIdx] = true;
        queueX[qTail] = half;
        queueZ[qTail] = half;
        qTail++;

        final int[] dirX = { 1, -1, 0, 0 };
        final int[] dirZ = { 0, 0, 1, -1 };

        while (qHead < qTail) {
            int cx = queueX[qHead];
            int cz = queueZ[qHead];
            qHead++;
            int cIdx = cz * size + cx;
            float cy = locRawY[cIdx];

            for (int d = 0; d < 4; d++) {
                int nx = cx + dirX[d];
                int nz = cz + dirZ[d];
                if (nx < 0 || nx >= size || nz < 0 || nz >= size) continue;
                int nIdx = nz * size + nx;
                if (visited[nIdx]) continue;

                int wx = ox + nx * LOCAL_CELL_STEP;
                int wz = oz + nz * LOCAL_CELL_STEP;
                float dist = Mth.sqrt((wx - sourcePos.getX()) * (wx - sourcePos.getX())
                        + (wz - sourcePos.getZ()) * (wz - sourcePos.getZ()));
                if (dist > radius + 1.0F) continue;

                float ny = sampleLocalFloorY(level, wx, Mth.floor(cy), wz);
                if (ny <= INVALID_Y + 1.0F || (ny > DEEP_DROP_Y + 1.0F && (ny - cy) > LOCAL_STEP_CLIMB_MAX)) {
                    // Physically solid rising wall (both floor & head solid, or >=2-block vertical step): hug flush
                    patch.solidWall[nIdx] = true;
                    continue;
                }
                if (ny <= DEEP_DROP_Y + 1.0F || (cy - ny) > LOCAL_STEP_CLIMB_MAX) {
                    // Sheer drop-off or deep (>6-block) hole: NEVER solidWall -> openEdgeFade dissolves to 0.0
                    continue;
                }

                visited[nIdx] = true;
                locRawY[nIdx] = ny;
                queueX[qTail] = nx;
                queueZ[qTail] = nz;
                qTail++;
            }
        }

        // Mark solid concave 90-degree inner-corner wall blocks so room corners fill flush with no notch
        for (int gz = 0; gz < size; gz++) {
            int wz = oz + gz * LOCAL_CELL_STEP;
            for (int gx = 0; gx < size; gx++) {
                int idx = gz * size + gx;
                if (visited[idx] || patch.solidWall[idx]) continue;

                int wx = ox + gx * LOCAL_CELL_STEP;
                for (int dz = -1; dz <= 1; dz += 2) {
                    int nz = gz + dz;
                    if (nz < 0 || nz >= size) continue;
                    for (int dx = -1; dx <= 1; dx += 2) {
                        int nx = gx + dx;
                        if (nx < 0 || nx >= size) continue;
                        int diagIdx = nz * size + nx;
                        if (!visited[diagIdx]) continue;
                        if (patch.solidWall[gz * size + nx] && patch.solidWall[nz * size + gx]) {
                            float ny = sampleLocalFloorY(level, wx, Mth.floor(locRawY[diagIdx]), wz);
                            if (ny <= INVALID_Y + 1.0F || (ny > DEEP_DROP_Y + 1.0F && (ny - locRawY[diagIdx]) > LOCAL_STEP_CLIMB_MAX)) {
                                patch.solidWall[idx] = true;
                            }
                            break;
                        }
                    }
                    if (patch.solidWall[idx]) break;
                }
            }
        }

        // Two-pass bilateral height smoothing FIRST so openDist can detect any sharp elevation
        // discontinuities (on both raw and smoothed heights) and fade them cleanly to 0 opacity
        float[] smoothPass1 = new float[n];
        Arrays.fill(smoothPass1, INVALID_Y);
        bilateralSmoothLocalPass(locRawY, smoothPass1, visited, size, 1.0F);
        bilateralSmoothLocalPass(smoothPass1, patch.smoothY, visited, size, 1.0F);

        // Distance-field openEdgeFade: 0.0 on open drop-offs, deep holes, abrupt descents, and any
        // height discontinuity >= 2.0 blocks, while keeping 1.0 right against physically solid walls!
        float[] openDist = new float[n];
        for (int gz = 0; gz < size; gz++) {
            for (int gx = 0; gx < size; gx++) {
                int idx = gz * size + gx;
                if (!visited[idx]) {
                    openDist[idx] = patch.solidWall[idx] ? 5.0F : 0.0F;
                    continue;
                }
                float cy = locRawY[idx];
                float sy = patch.smoothY[idx];
                boolean touchesOpenVoid = false;
                for (int dz = -1; dz <= 1 && !touchesOpenVoid; dz++) {
                    int nz = gz + dz;
                    if (nz < 0 || nz >= size) {
                        touchesOpenVoid = true;
                        break;
                    }
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dz == 0) continue;
                        int nx = gx + dx;
                        if (nx < 0 || nx >= size) {
                            touchesOpenVoid = true;
                            break;
                        }
                        int nIdx = nz * size + nx;
                        if (!visited[nIdx]) {
                            if (!patch.solidWall[nIdx]) {
                                touchesOpenVoid = true;
                                break;
                            }
                        } else if (Math.abs(locRawY[nIdx] - cy) >= 2.0F
                                || Math.abs(patch.smoothY[nIdx] - sy) >= 2.0F) {
                            // Height jump that would split a quad -> dissolve smoothly to 0 instead of hard edge!
                            touchesOpenVoid = true;
                            break;
                        }
                    }
                }
                openDist[idx] = touchesOpenVoid ? 0.0F : 5.0F;
            }
        }

        // 4 relaxation passes -> smooth ~4-block dissolution into nothingness before holes & ledges
        for (int pass = 0; pass < 4; pass++) {
            for (int gz = 0; gz < size; gz++) {
                for (int gx = 0; gx < size; gx++) {
                    int idx = gz * size + gx;
                    if (!visited[idx]) continue;
                    float best = openDist[idx];
                    if (best <= 0.0F) continue;
                    for (int dz = -1; dz <= 1; dz++) {
                        int nz = gz + dz;
                        if (nz < 0 || nz >= size) continue;
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dz == 0) continue;
                            int nx = gx + dx;
                            if (nx < 0 || nx >= size) continue;
                            int nIdx = nz * size + nx;
                            if (!visited[nIdx]) continue;
                            float step = (dx != 0 && dz != 0) ? 1.414F : 1.0F;
                            float cand = openDist[nIdx] + step;
                            if (cand < best) best = cand;
                        }
                    }
                    openDist[idx] = best;
                }
            }
        }

        // Radial fade + openEdgeFade + light sampling
        for (int gz = 0; gz < size; gz++) {
            int wz = oz + gz * LOCAL_CELL_STEP;
            for (int gx = 0; gx < size; gx++) {
                int idx = gz * size + gx;
                if (!visited[idx]) {
                    patch.smoothY[idx] = INVALID_Y;
                    patch.alphaMask[idx] = 0.0F;
                    patch.openEdgeFade[idx] = 0.0F;
                    continue;
                }
                float cy = locRawY[idx];
                int wx = ox + gx * LOCAL_CELL_STEP;
                float dist = Mth.sqrt((wx - sourcePos.getX()) * (wx - sourcePos.getX())
                        + (wz - sourcePos.getZ()) * (wz - sourcePos.getZ()));
                float radialT = Mth.clamp(1.0F - (dist / radius), 0.0F, 1.0F);
                float radialFade = radialT * radialT * (3.0F - 2.0F * radialT);
                patch.alphaMask[idx] = patch.baseAlpha * radialFade;

                float edgeT = Mth.clamp(openDist[idx] / 4.0F, 0.0F, 1.0F);
                patch.openEdgeFade[idx] = edgeT * edgeT * (3.0F - 2.0F * edgeT);

                // Compute distance to nearest solid wall so puffs right beside a 1-block wall stay compact
                // (hugging the wall flush without ever poking through into an adjacent room!)
                boolean wallN = gz > 0 && patch.solidWall[(gz - 1) * size + gx];
                boolean wallS = gz < size - 1 && patch.solidWall[(gz + 1) * size + gx];
                boolean wallW = gx > 0 && patch.solidWall[gz * size + (gx - 1)];
                boolean wallE = gx < size - 1 && patch.solidWall[gz * size + (gx + 1)];
                patch.narrowAlley[idx] = (wallN && wallS) || (wallW && wallE);

                int wDist = 3;
                if (wallN || wallS || wallW || wallE) {
                    wDist = 1;
                } else {
                    outerWallCheck:
                    for (int dz = -2; dz <= 2; dz++) {
                        int nz = gz + dz;
                        if (nz < 0 || nz >= size) continue;
                        for (int dx = -2; dx <= 2; dx++) {
                            int nx = gx + dx;
                            if (nx < 0 || nx >= size) continue;
                            if (patch.solidWall[nz * size + nx]) {
                                int cheb = Math.max(Math.abs(dx), Math.abs(dz));
                                if (cheb < wDist) wDist = cheb;
                                if (wDist == 1) break outerWallCheck;
                            }
                        }
                    }
                }
                patch.wallDist[idx] = (byte) wDist;

                scratchPos.set(wx, Mth.floor(cy) + 1, wz);
                patch.packedLight[idx] = sampleLocalPackedLight(level, scratchPos);
            }
        }

        return patch;
    }

    private static void bilateralSmoothLocalPass(float[] srcY, float[] dstY, boolean[] visited, int size, float centerWeight) {
        for (int gz = 0; gz < size; gz++) {
            for (int gx = 0; gx < size; gx++) {
                int idx = gz * size + gx;
                if (!visited[idx]) {
                    dstY[idx] = INVALID_Y;
                    continue;
                }
                float cy = srcY[idx];
                float sumY = cy * centerWeight;
                float weight = centerWeight;

                for (int dz = -1; dz <= 1; dz++) {
                    int nz = gz + dz;
                    if (nz < 0 || nz >= size) continue;
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dz == 0) continue;
                        int nx = gx + dx;
                        if (nx < 0 || nx >= size) continue;
                        int nIdx = nz * size + nx;
                        if (!visited[nIdx]) continue;
                        float ny = srcY[nIdx];
                        boolean isDiag = (dx != 0 && dz != 0);
                        if (Math.abs(ny - cy) <= (isDiag ? 2.1F : 1.35F)) {
                            float w = isDiag ? 0.75F : 1.0F;
                            sumY += ny * w;
                            weight += w;
                        }
                    }
                }
                dstY[idx] = sumY / weight;
            }
        }
    }
}
