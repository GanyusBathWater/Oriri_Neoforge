package net.ganyusbathwater.oririmod.client.render.world.haze;

import net.ganyusbathwater.oririmod.block.custom.FogSourceBlock;
import net.ganyusbathwater.oririmod.block.custom.VolumeFogSourceBlock;
import net.ganyusbathwater.oririmod.block.entity.FogSourceBlockEntity;
import net.ganyusbathwater.oririmod.world.fog.ClientFogAnchorCache;
import net.ganyusbathwater.oririmod.world.fog.FogAnchor;
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
    public static final float INVALID_Y_CUTOFF = -90000.0F;
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
    private int surfaceMeshVersion = 1;
    private int localMeshVersion = 1;
    private int volumeMeshVersion = 1;

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

    // Local FogSourceBlock & chunk-attached FogAnchor patches (keyed by BlockPos.asLong())
    private final Map<Long, LocalFogPatch> localPatches = new HashMap<>();
    private final List<LocalFogPatch> activeLocalPatches = new ArrayList<>();
    private final Map<Long, VolumeFogPatch> volumePatches = new HashMap<>();
    private final List<VolumeFogPatch> activeVolumePatches = new ArrayList<>();
    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
    private long lastAnchorCacheVersion = -1L;
    private int localRefreshCursor = 0;
    private int volumeRefreshCursor = 0;
    private boolean localRenderDirty = true;
    private boolean volumeRenderDirty = true;
    private int lastLocalBakeCellX = Integer.MIN_VALUE;
    private int lastLocalBakeCellZ = Integer.MIN_VALUE;
    private int lastVolumeBakeCellX = Integer.MIN_VALUE;
    private int lastVolumeBakeCellZ = Integer.MIN_VALUE;

    // Reusable unified heightfield composite grid (radius 144 -> 289x289, covers full ~140-block crater!)
    public static final int COMP_RADIUS = 144;
    public static final int COMP_SIZE = COMP_RADIUS * 2 + 1;
    private static final int COMP_CELLS = COMP_SIZE * COMP_SIZE;
    private final LocalCompositeGrid localCompositeGrid = new LocalCompositeGrid();
    private final VolumeCompositeGrid volumeCompositeGrid = new VolumeCompositeGrid();
    private float[] compColorW;
    private float[] volColorW;

    /**
     * Deduplicated 1-block-precision composite grid merging all overlapping {@code FogSourceBlock}s
     * and chunk-attached {@code FogAnchor}s across a 289x289 area around the camera (144-block radius),
     * ready for direct upload into the GPU volumetric heightfield textures.
     */
    public static final class LocalCompositeGrid {
        public int originX;
        public int originZ;
        public final int size = COMP_SIZE;
        public int dirtyMinX;
        public int dirtyMaxX = -1;
        public int dirtyMinZ;
        public int dirtyMaxZ = -1;
        public boolean hasData;
        public float minFloorY;
        public float maxFogTopY;

        public final float[] smoothY = new float[COMP_CELLS];
        public final float[] fogHeight = new float[COMP_CELLS];
        public final float[] maskAlpha = new float[COMP_CELLS];
        public final float[] edgeFade = new float[COMP_CELLS];
        public final float[] r = new float[COMP_CELLS];
        public final float[] g = new float[COMP_CELLS];
        public final float[] b = new float[COMP_CELLS];
        public final int[] packedLight = new int[COMP_CELLS];
        public final boolean[] solidWall = new boolean[COMP_CELLS];
        public final List<LocalFogPatch> overflowPatches = new ArrayList<>();

        void clear() {
            hasData = false;
            dirtyMinX = COMP_SIZE;
            dirtyMaxX = -1;
            dirtyMinZ = COMP_SIZE;
            dirtyMaxZ = -1;
            minFloorY = Float.MAX_VALUE;
            maxFogTopY = -Float.MAX_VALUE;
            overflowPatches.clear();
        }
    }

    /**
     * Deduplicated 1-block-precision 3D floating room-volume composite grid merging all nearby
     * {@link VolumeFogSourceBlock}s and volume {@link FogAnchor}s. Arrays are allocated lazily on
     * first volume emitter discovery so zero extra memory is used when no volume fog is present.
     */
    public static final class VolumeCompositeGrid {
        public int originX;
        public int originZ;
        public final int size = COMP_SIZE;
        public int dirtyMinX;
        public int dirtyMaxX = -1;
        public int dirtyMinZ;
        public int dirtyMaxZ = -1;
        public boolean hasData;
        public float minBottomY;
        public float maxTopY;

        public float[] bottomY;
        public float[] topY;
        public float[] density;
        public float[] mask;
        public float[] r;
        public float[] g;
        public float[] b;
        public int[] packedLight;

        void ensureAllocated() {
            if (bottomY != null) return;
            bottomY = new float[COMP_CELLS];
            topY = new float[COMP_CELLS];
            density = new float[COMP_CELLS];
            mask = new float[COMP_CELLS];
            r = new float[COMP_CELLS];
            g = new float[COMP_CELLS];
            b = new float[COMP_CELLS];
            packedLight = new int[COMP_CELLS];
        }

        void clear() {
            hasData = false;
            dirtyMinX = COMP_SIZE;
            dirtyMaxX = -1;
            dirtyMinZ = COMP_SIZE;
            dirtyMaxZ = -1;
            minBottomY = Float.MAX_VALUE;
            maxTopY = -Float.MAX_VALUE;
        }
    }

    /**
     * Wall-bounded 3D floating room-filling fog patch centered on a {@link VolumeFogSourceBlock}.
     */
    public static final class VolumeFogPatch {
        public final BlockPos sourcePos;
        public final int density;
        public final FogSourceBlock.FogColor color;
        public final float radiusXZ;
        public final float halfHeight;
        public final float baseAlpha;
        public final int gridSize;
        public final int originX;
        public final int originZ;
        public final float[] bottomY;
        public final float[] topY;
        public final float[] alphaMask;
        public final int[] packedLight;
        long lastSeenTick;
        int lightRefreshTick;
        int geoRefreshTick;

        VolumeFogPatch(BlockPos sourcePos, int density, FogSourceBlock.FogColor color, int gridSize, int originX, int originZ) {
            this.sourcePos = sourcePos.immutable();
            this.density = density;
            this.color = color;
            this.radiusXZ = VolumeFogSourceBlock.getVolumeRadiusXZ(density);
            this.halfHeight = VolumeFogSourceBlock.getVolumeHalfHeight(density);
            this.baseAlpha = VolumeFogSourceBlock.getVolumeAlphaForDensity(density);
            this.gridSize = gridSize;
            this.originX = originX;
            this.originZ = originZ;
            int n = gridSize * gridSize;
            this.bottomY = new float[n];
            this.topY = new float[n];
            this.alphaMask = new float[n];
            this.packedLight = new int[n];
        }
    }

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
        int geoRefreshTick;

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
        this.volumePatches.clear();
        this.activeVolumePatches.clear();
        this.localCompositeGrid.clear();
        this.volumeCompositeGrid.clear();
        this.localRenderDirty = true;
        this.volumeRenderDirty = true;
        this.lastLocalBakeCellX = Integer.MIN_VALUE;
        this.lastLocalBakeCellZ = Integer.MIN_VALUE;
        this.lastVolumeBakeCellX = Integer.MIN_VALUE;
        this.lastVolumeBakeCellZ = Integer.MIN_VALUE;
        this.lastAnchorCacheVersion = -1L;
        this.surfaceMeshVersion++;
        this.localMeshVersion++;
        this.volumeMeshVersion++;
    }

    public LocalCompositeGrid getLocalCompositeGrid() {
        return localCompositeGrid;
    }

    public VolumeCompositeGrid getVolumeCompositeGrid() {
        return volumeCompositeGrid;
    }

    public int getSurfaceMeshVersion() {
        return surfaceMeshVersion;
    }

    public int getLocalMeshVersion() {
        return localMeshVersion;
    }

    public int getVolumeMeshVersion() {
        return volumeMeshVersion;
    }

    public List<VolumeFogPatch> getActiveVolumePatches() {
        return activeVolumePatches;
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
            surfaceMeshVersion++;
        } else if ((tickCounter % 10) == 0) {
            refreshRawAlphaFromProfiles();
            smoothBiomeColorsAndAlpha();
            surfaceMeshVersion++;
        } else if (hasAnyActiveSurfaceHaze && lightSliceCursor == 0) {
            // Full light-slice cycle completed -> refresh surface VBO lighting cleanly
            surfaceMeshVersion++;
        }

        // 4. Discover & maintain local FogSourceBlock + VolumeFogSourceBlock + chunk-attached FogAnchors
        int localScanRadius = Math.max(clampedRadius + 28, COMP_RADIUS + 8);
        long anchorVer = ClientFogAnchorCache.getVersion();
        if ((tickCounter & 3) == 0 || geometryChanged || anchorVer != lastAnchorCacheVersion) {
            lastAnchorCacheVersion = anchorVer;
            updateLocalFogPatches(level, camX, camY, camZ, localScanRadius);
        } else {
            if (!activeLocalPatches.isEmpty()) {
                refreshOneStaleLocalPatch(level);
            }
            if (!activeVolumePatches.isEmpty()) {
                refreshOneStaleVolumePatch(level);
            }
        }

        // 5. Rebuild deduplicated ground & volume composite grids only when patches change or player moves >= 8 blocks
        int bakeCellX = Math.floorDiv(Mth.floor(camX), 8);
        int bakeCellZ = Math.floorDiv(Mth.floor(camZ), 8);
        if (localRenderDirty || bakeCellX != lastLocalBakeCellX || bakeCellZ != lastLocalBakeCellZ) {
            lastLocalBakeCellX = bakeCellX;
            lastLocalBakeCellZ = bakeCellZ;
            localRenderDirty = false;
            rebuildDeduplicatedLocalBuffer(Mth.floor(camX), Mth.floor(camZ));
        }
        if (volumeRenderDirty || (!activeVolumePatches.isEmpty()
                && (bakeCellX != lastVolumeBakeCellX || bakeCellZ != lastVolumeBakeCellZ))) {
            lastVolumeBakeCellX = bakeCellX;
            lastVolumeBakeCellZ = bakeCellZ;
            volumeRenderDirty = false;
            rebuildDeduplicatedVolumeGrid(Mth.floor(camX), Mth.floor(camZ));
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
        int prevLocalCount = activeLocalPatches.size();
        long prevLocalChecksum = 0L;
        for (int i = 0; i < prevLocalCount; i++) {
            prevLocalChecksum = prevLocalChecksum * 31L + activeLocalPatches.get(i).sourcePos.asLong();
        }
        activeLocalPatches.clear();

        int prevVolCount = activeVolumePatches.size();
        long prevVolChecksum = 0L;
        for (int i = 0; i < prevVolCount; i++) {
            prevVolChecksum = prevVolChecksum * 31L + activeVolumePatches.get(i).sourcePos.asLong();
        }
        activeVolumePatches.clear();

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
                    if (dx * dx + dz * dz > maxDistSq || Math.abs(dy) > 148.0) {
                        continue;
                    }

                    BlockState state = be.getBlockState();
                    if (!(state.getBlock() instanceof FogSourceBlock)) {
                        continue;
                    }

                    int density = state.getValue(FogSourceBlock.DENSITY);
                    FogSourceBlock.FogColor color = state.getValue(FogSourceBlock.COLOR);
                    if (state.getBlock() instanceof VolumeFogSourceBlock) {
                        registerActiveVolumeEmitter(level, pos, density, color);
                    } else {
                        registerActiveLocalEmitter(level, pos, density, color);
                    }
                }

                // Also include chunk-attached structure FogAnchors (converted from FogSourceBlocks on worldgen)
                List<FogAnchor.ClientView> chunkAnchors = ClientFogAnchorCache.getChunkAnchors(cx, cz);
                for (int i = 0; i < chunkAnchors.size(); i++) {
                    FogAnchor.ClientView anchor = chunkAnchors.get(i);
                    BlockPos pos = anchor.origin();
                    double dx = pos.getX() + 0.5 - camX;
                    double dy = pos.getY() + 0.5 - camY;
                    double dz = pos.getZ() + 0.5 - camZ;
                    if (dx * dx + dz * dz > maxDistSq || Math.abs(dy) > 148.0) {
                        continue;
                    }
                    if (anchor.isVolume()) {
                        registerActiveVolumeEmitter(level, pos, anchor.density(), anchor.color());
                    } else {
                        registerActiveLocalEmitter(level, pos, anchor.density(), anchor.color());
                    }
                }
            }
        }

        long newLocalChecksum = 0L;
        for (int i = 0; i < activeLocalPatches.size(); i++) {
            newLocalChecksum = newLocalChecksum * 31L + activeLocalPatches.get(i).sourcePos.asLong();
        }
        if (activeLocalPatches.size() != prevLocalCount || newLocalChecksum != prevLocalChecksum) {
            localRenderDirty = true;
        }

        long newVolChecksum = 0L;
        for (int i = 0; i < activeVolumePatches.size(); i++) {
            newVolChecksum = newVolChecksum * 31L + activeVolumePatches.get(i).sourcePos.asLong();
        }
        if (activeVolumePatches.size() != prevVolCount || newVolChecksum != prevVolChecksum) {
            volumeRenderDirty = true;
        }

        if (localPatches.size() > activeLocalPatches.size() + 24) {
            Iterator<Map.Entry<Long, LocalFogPatch>> it = localPatches.entrySet().iterator();
            while (it.hasNext()) {
                if (tickCounter - it.next().getValue().lastSeenTick > 120) {
                    it.remove();
                }
            }
        }
        if (volumePatches.size() > activeVolumePatches.size() + 24) {
            Iterator<Map.Entry<Long, VolumeFogPatch>> it = volumePatches.entrySet().iterator();
            while (it.hasNext()) {
                if (tickCounter - it.next().getValue().lastSeenTick > 120) {
                    it.remove();
                }
            }
        }
    }

    private void registerActiveVolumeEmitter(ClientLevel level, BlockPos pos, int density, FogSourceBlock.FogColor color) {
        long key = pos.asLong();
        VolumeFogPatch patch = volumePatches.get(key);
        if (patch == null || patch.density != density || patch.color != color) {
            patch = buildVolumeFogPatch(level, pos, density, color);
            int stagger = Math.floorMod(pos.hashCode(), 40);
            patch.lightRefreshTick = tickCounter - stagger;
            patch.geoRefreshTick = tickCounter - stagger * 3;
            volumePatches.put(key, patch);
            volumeRenderDirty = true;
        }
        patch.lastSeenTick = tickCounter;
        activeVolumePatches.add(patch);
    }

    private void refreshOneStaleVolumePatch(ClientLevel level) {
        int count = activeVolumePatches.size();
        if (count == 0) return;

        int checked = Math.min(count, 4);
        for (int i = 0; i < checked; i++) {
            if (volumeRefreshCursor >= count) {
                volumeRefreshCursor = 0;
            }
            VolumeFogPatch patch = activeVolumePatches.get(volumeRefreshCursor++);
            if ((tickCounter - patch.geoRefreshTick) >= 240) {
                VolumeFogPatch rebuilt = buildVolumeFogPatch(level, patch.sourcePos, patch.density, patch.color);
                rebuilt.lastSeenTick = tickCounter;
                rebuilt.lightRefreshTick = tickCounter;
                rebuilt.geoRefreshTick = tickCounter;
                volumePatches.put(patch.sourcePos.asLong(), rebuilt);
                activeVolumePatches.set(volumeRefreshCursor - 1, rebuilt);
                volumeRenderDirty = true;
                return;
            }
            if ((tickCounter - patch.lightRefreshTick) >= 80) {
                refreshVolumePatchLightsOnly(level, patch);
                patch.lightRefreshTick = tickCounter;
                volumeRenderDirty = true;
                return;
            }
        }
    }

    private void refreshVolumePatchLightsOnly(ClientLevel level, VolumeFogPatch patch) {
        int size = patch.gridSize;
        for (int gz = 0; gz < size; gz++) {
            int wz = patch.originZ + gz;
            int row = gz * size;
            for (int gx = 0; gx < size; gx++) {
                int idx = row + gx;
                if (patch.alphaMask[idx] <= 0.004F) continue;
                int wx = patch.originX + gx;
                int midY = Mth.floor((patch.bottomY[idx] + patch.topY[idx]) * 0.5F);
                scratchPos.set(wx, midY, wz);
                patch.packedLight[idx] = sampleLocalPackedLight(level, scratchPos);
            }
        }
    }

    /**
     * Merges all active 3D floating room-volume patches into {@link VolumeCompositeGrid} (289x289,
     * 144-block radius), allowing simultaneous rendering alongside ground-hugging fog in the same column.
     */
    private void rebuildDeduplicatedVolumeGrid(int centerX, int centerZ) {
        VolumeCompositeGrid vol = this.volumeCompositeGrid;
        vol.clear();
        volumeMeshVersion++;
        if (activeVolumePatches.isEmpty()) {
            return;
        }

        vol.ensureAllocated();
        if (volColorW == null) {
            volColorW = new float[COMP_CELLS];
        }

        int originX = centerX - COMP_RADIUS;
        int originZ = centerZ - COMP_RADIUS;
        vol.originX = originX;
        vol.originZ = originZ;

        Arrays.fill(vol.mask, 0.0F);
        Arrays.fill(vol.density, 0.0F);

        int dirtyMinX = COMP_SIZE, dirtyMaxX = -1;
        int dirtyMinZ = COMP_SIZE, dirtyMaxZ = -1;

        for (int p = 0; p < activeVolumePatches.size(); p++) {
            VolumeFogPatch patch = activeVolumePatches.get(p);
            int pSize = patch.gridSize;
            float pr = patch.color.r;
            float pg = patch.color.g;
            float pb = patch.color.b;

            for (int gz = 0; gz < pSize; gz++) {
                int wz = patch.originZ + gz;
                int cz = wz - originZ;
                if (cz < 0 || cz >= COMP_SIZE) continue;
                int pRow = gz * pSize;
                int cRow = cz * COMP_SIZE;

                for (int gx = 0; gx < pSize; gx++) {
                    int pIdx = pRow + gx;
                    float m = patch.alphaMask[pIdx];
                    if (m <= 0.004F) continue;
                    float bY = patch.bottomY[pIdx];
                    float tY = patch.topY[pIdx];
                    if (tY <= bY + 0.1F) continue;

                    int wx = patch.originX + gx;
                    int cx = wx - originX;
                    if (cx < 0 || cx >= COMP_SIZE) continue;
                    int cIdx = cRow + cx;

                    float existingM = vol.mask[cIdx];
                    if (existingM <= 0.0F) {
                        vol.bottomY[cIdx] = bY;
                        vol.topY[cIdx] = tY;
                        vol.density[cIdx] = patch.baseAlpha;
                        vol.mask[cIdx] = m;
                        vol.r[cIdx] = pr * m;
                        vol.g[cIdx] = pg * m;
                        vol.b[cIdx] = pb * m;
                        volColorW[cIdx] = m;
                        vol.packedLight[cIdx] = patch.packedLight[pIdx];
                        if (cx < dirtyMinX) dirtyMinX = cx;
                        if (cx > dirtyMaxX) dirtyMaxX = cx;
                        if (cz < dirtyMinZ) dirtyMinZ = cz;
                        if (cz > dirtyMaxZ) dirtyMaxZ = cz;
                    } else {
                        if (bY < vol.bottomY[cIdx]) vol.bottomY[cIdx] = bY;
                        if (tY > vol.topY[cIdx]) vol.topY[cIdx] = tY;
                        if (m > existingM) {
                            vol.mask[cIdx] = m;
                            vol.density[cIdx] = Math.max(vol.density[cIdx], patch.baseAlpha);
                            vol.packedLight[cIdx] = patch.packedLight[pIdx];
                        }
                        vol.r[cIdx] += pr * m;
                        vol.g[cIdx] += pg * m;
                        vol.b[cIdx] += pb * m;
                        volColorW[cIdx] += m;
                    }
                }
            }
        }

        if (dirtyMaxX < dirtyMinX) {
            return;
        }

        float minB = Float.MAX_VALUE;
        float maxT = -Float.MAX_VALUE;
        for (int cz = dirtyMinZ; cz <= dirtyMaxZ; cz++) {
            int cRow = cz * COMP_SIZE;
            for (int cx = dirtyMinX; cx <= dirtyMaxX; cx++) {
                int cIdx = cRow + cx;
                if (vol.mask[cIdx] <= 0.004F) continue;
                float invW = volColorW[cIdx] > 0.0001F ? (1.0F / volColorW[cIdx]) : 1.0F;
                vol.r[cIdx] *= invW;
                vol.g[cIdx] *= invW;
                vol.b[cIdx] *= invW;
                if (vol.bottomY[cIdx] < minB) minB = vol.bottomY[cIdx];
                if (vol.topY[cIdx] > maxT) maxT = vol.topY[cIdx];
            }
        }

        vol.dirtyMinX = Math.max(0, dirtyMinX - 1);
        vol.dirtyMaxX = Math.min(COMP_SIZE - 1, dirtyMaxX + 1);
        vol.dirtyMinZ = Math.max(0, dirtyMinZ - 1);
        vol.dirtyMaxZ = Math.min(COMP_SIZE - 1, dirtyMaxZ + 1);
        vol.minBottomY = minB;
        vol.maxTopY = maxT;
        vol.hasData = true;
    }

    /**
     * Builds a 3D wall-bounded, floating room-filling fog patch centered vertically &amp; horizontally
     * on {@code sourcePos}. Works both inside enclosed rooms (filling cleanly floor-to-ceiling and
     * stopping at 1-block walls) and floating mid-air (soft 3D ellipsoid cloud with feathered top/bottom).
     */
    private VolumeFogPatch buildVolumeFogPatch(ClientLevel level, BlockPos sourcePos, int density, FogSourceBlock.FogColor color) {
        float radiusXZ = VolumeFogSourceBlock.getVolumeRadiusXZ(density);
        float halfH = VolumeFogSourceBlock.getVolumeHalfHeight(density);
        int half = Mth.ceil(radiusXZ) + 1;
        int size = half * 2 + 1;
        int ox = sourcePos.getX() - half;
        int oz = sourcePos.getZ() - half;

        VolumeFogPatch patch = new VolumeFogPatch(sourcePos, density, color, size, ox, oz);
        int n = size * size;
        boolean[] visited = new boolean[n];
        int[] airMinArr = new int[n];
        int[] airMaxArr = new int[n];
        float[] rawBottom = new float[n];
        float[] rawTop = new float[n];

        int minBuild = level.getMinBuildHeight();
        int maxBuild = level.getMaxBuildHeight() - 1;
        int srcY = Mth.clamp(sourcePos.getY(), minBuild, maxBuild);
        float centerY = srcY + 0.5F;

        int[] queueX = new int[n];
        int[] queueZ = new int[n];
        int qHead = 0, qTail = 0;

        float[] spanOut = new float[4]; // [0]=bottomY, [1]=topY, [2]=airMinInt, [3]=airMaxInt
        float seedAllowMin = centerY - halfH;
        float seedAllowMax = centerY + halfH;
        if (!probeVolumeColumnSpan(level, sourcePos.getX(), sourcePos.getZ(), srcY, srcY, seedAllowMin, seedAllowMax, srcY, minBuild, maxBuild, spanOut)) {
            // Even if emitter sits in a weird state, seed a small air span right at sourcePos
            spanOut[0] = seedAllowMin;
            spanOut[1] = seedAllowMax;
            spanOut[2] = Mth.floor(seedAllowMin);
            spanOut[3] = Mth.ceil(seedAllowMax);
        }

        int centerIdx = half * size + half;
        visited[centerIdx] = true;
        rawBottom[centerIdx] = spanOut[0];
        rawTop[centerIdx] = spanOut[1];
        airMinArr[centerIdx] = (int) spanOut[2];
        airMaxArr[centerIdx] = (int) spanOut[3];
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
            int prevMin = airMinArr[cIdx];
            int prevMax = airMaxArr[cIdx];

            for (int d = 0; d < 4; d++) {
                int nx = cx + dirX[d];
                int nz = cz + dirZ[d];
                if (nx < 0 || nx >= size || nz < 0 || nz >= size) continue;
                int nIdx = nz * size + nx;
                if (visited[nIdx]) continue;

                int wx = ox + nx;
                int wz = oz + nz;
                float dist = Mth.sqrt((wx - sourcePos.getX()) * (wx - sourcePos.getX())
                        + (wz - sourcePos.getZ()) * (wz - sourcePos.getZ()));
                if (dist > radiusXZ) continue;

                float normR = dist / radiusXZ;
                float localHalfH = halfH * Mth.sqrt(Math.max(0.08F, 1.0F - normR * normR * 0.85F));
                float allowMin = centerY - localHalfH;
                float allowMax = centerY + localHalfH;

                if (!probeVolumeColumnSpan(level, wx, wz, prevMin, prevMax, allowMin, allowMax, srcY, minBuild, maxBuild, spanOut)) {
                    // Completely blocked by a solid wall across the overlapping vertical band -> never leak through walls!
                    continue;
                }

                visited[nIdx] = true;
                rawBottom[nIdx] = spanOut[0];
                rawTop[nIdx] = spanOut[1];
                airMinArr[nIdx] = (int) spanOut[2];
                airMaxArr[nIdx] = (int) spanOut[3];
                queueX[qTail] = nx;
                queueZ[qTail] = nz;
                qTail++;
            }
        }

        // Light 1-pass smoothing on bottomY/topY across visited neighbors for silky arches & stairs,
        // plus smooth radial fade and midpoint lighting
        for (int gz = 0; gz < size; gz++) {
            int wz = oz + gz;
            int row = gz * size;
            for (int gx = 0; gx < size; gx++) {
                int idx = row + gx;
                if (!visited[idx]) continue;

                float sumB = rawBottom[idx] * 2.0F;
                float sumT = rawTop[idx] * 2.0F;
                float w = 2.0F;
                for (int dz = -1; dz <= 1; dz++) {
                    int nz = gz + dz;
                    if (nz < 0 || nz >= size) continue;
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dz == 0) continue;
                        int nx = gx + dx;
                        if (nx < 0 || nx >= size) continue;
                        int nIdx = nz * size + nx;
                        if (!visited[nIdx]) continue;
                        if (Math.abs(rawBottom[nIdx] - rawBottom[idx]) <= 3.0F
                                && Math.abs(rawTop[nIdx] - rawTop[idx]) <= 3.0F) {
                            sumB += rawBottom[nIdx];
                            sumT += rawTop[nIdx];
                            w += 1.0F;
                        }
                    }
                }
                patch.bottomY[idx] = sumB / w;
                patch.topY[idx] = sumT / w;

                int wx = ox + gx;
                float dist = Mth.sqrt((wx - sourcePos.getX()) * (wx - sourcePos.getX())
                        + (wz - sourcePos.getZ()) * (wz - sourcePos.getZ()));
                float radialT = Mth.clamp(1.0F - (dist / radiusXZ), 0.0F, 1.0F);
                patch.alphaMask[idx] = radialT * radialT * (3.0F - 2.0F * radialT);

                int midY = Mth.clamp(Mth.floor((airMinArr[idx] + airMaxArr[idx]) * 0.5F), minBuild, maxBuild);
                scratchPos.set(wx, midY, wz);
                patch.packedLight[idx] = sampleLocalPackedLight(level, scratchPos);
            }
        }

        return patch;
    }

    /**
     * Finds the vertical passable air span in column {@code (wx, wz)} connected to the neighbor's
     * {@code [prevMin, prevMax]} band within {@code [allowMin, allowMax]}.
     * Solid floors/ceilings extend 1.1 blocks into the solid surface so the shader's vertical
     * feather fills enclosed rooms flush, while open mid-air bounds feather softly into empty air.
     */
    private boolean probeVolumeColumnSpan(
            ClientLevel level,
            int wx,
            int wz,
            int prevMin,
            int prevMax,
            float allowMin,
            float allowMax,
            int preferY,
            int minBuild,
            int maxBuild,
            float[] out) {
        int lo = Math.max(minBuild, Math.max(prevMin, Mth.floor(allowMin)));
        int hi = Math.min(maxBuild, Math.min(prevMax, Mth.ceil(allowMax)));
        if (lo > hi) {
            return false;
        }

        // Find a passable seed Y inside the overlap band, starting closest to emitter's center Y
        int seedY = Integer.MIN_VALUE;
        int startY = Mth.clamp(preferY, lo, hi);
        int maxOffset = Math.max(startY - lo, hi - startY);
        for (int off = 0; off <= maxOffset; off++) {
            int up = startY + off;
            if (up <= hi && isVolumePassable(level, wx, up, wz)) {
                seedY = up;
                break;
            }
            if (off > 0) {
                int dn = startY - off;
                if (dn >= lo && isVolumePassable(level, wx, dn, wz)) {
                    seedY = dn;
                    break;
                }
            }
        }
        if (seedY == Integer.MIN_VALUE) {
            return false;
        }

        int minScan = Math.max(minBuild, Mth.floor(allowMin));
        int maxScan = Math.min(maxBuild, Mth.ceil(allowMax));

        // Scan downward to floor or open mid-air bottom
        float bottomY = allowMin;
        int airMin = minScan;
        for (int cy = seedY - 1; cy >= minScan; cy--) {
            scratchPos.set(wx, cy, wz);
            BlockState st = level.getBlockState(scratchPos);
            if (!st.getFluidState().isEmpty()) {
                bottomY = (cy + 0.88F) - 1.1F;
                airMin = cy + 1;
                break;
            }
            float topOff = getSolidFloorTopOffset(level, scratchPos, st);
            if (topOff >= 0.0F) {
                bottomY = (cy + topOff) - 1.1F;
                airMin = cy + 1;
                break;
            }
        }

        // Scan upward to ceiling or open mid-air top
        float topY = allowMax;
        int airMax = maxScan;
        for (int cy = seedY + 1; cy <= maxScan; cy++) {
            scratchPos.set(wx, cy, wz);
            BlockState st = level.getBlockState(scratchPos);
            if (!isVolumePassableState(level, scratchPos, st)) {
                topY = cy + 1.1F;
                airMax = cy - 1;
                break;
            }
        }

        out[0] = bottomY;
        out[1] = topY;
        out[2] = airMin;
        out[3] = airMax;
        return true;
    }

    private boolean isVolumePassable(ClientLevel level, int wx, int wy, int wz) {
        scratchPos.set(wx, wy, wz);
        return isVolumePassableState(level, scratchPos, level.getBlockState(scratchPos));
    }

    private static boolean isVolumePassableState(ClientLevel level, BlockPos pos, BlockState state) {
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        return getSolidFloorTopOffset(level, pos, state) < 0.0F;
    }

    private void registerActiveLocalEmitter(ClientLevel level, BlockPos pos, int density, FogSourceBlock.FogColor color) {
        long key = pos.asLong();
        LocalFogPatch patch = localPatches.get(key);
        if (patch == null || patch.density != density || patch.color != color) {
            patch = buildLocalFogPatch(level, pos, density, color);
            // Stagger initial refresh timers by position hash so large dungeons never refresh in lockstep
            int stagger = Math.floorMod(pos.hashCode(), 40);
            patch.lightRefreshTick = tickCounter - stagger;
            patch.geoRefreshTick = tickCounter - stagger * 3;
            localPatches.put(key, patch);
            localRenderDirty = true;
        }
        patch.lastSeenTick = tickCounter;
        activeLocalPatches.add(patch);
    }

    /**
     * Refreshes at most ONE stale patch per tick: fast light-only refresh every 80 ticks,
     * and full BFS geometry rebuild every 240 ticks (catches newly broken/placed blocks with zero stutter).
     */
    private void refreshOneStaleLocalPatch(ClientLevel level) {
        int count = activeLocalPatches.size();
        if (count == 0) return;

        int checked = Math.min(count, 4);
        for (int i = 0; i < checked; i++) {
            if (localRefreshCursor >= count) {
                localRefreshCursor = 0;
            }
            LocalFogPatch patch = activeLocalPatches.get(localRefreshCursor++);
            if ((tickCounter - patch.geoRefreshTick) >= 240) {
                LocalFogPatch rebuilt = buildLocalFogPatch(level, patch.sourcePos, patch.density, patch.color);
                rebuilt.lastSeenTick = tickCounter;
                rebuilt.lightRefreshTick = tickCounter;
                rebuilt.geoRefreshTick = tickCounter;
                localPatches.put(patch.sourcePos.asLong(), rebuilt);
                activeLocalPatches.set(localRefreshCursor - 1, rebuilt);
                localRenderDirty = true;
                return;
            }
            if ((tickCounter - patch.lightRefreshTick) >= 80) {
                refreshLocalPatchLightsOnly(level, patch);
                patch.lightRefreshTick = tickCounter;
                localRenderDirty = true;
                return;
            }
        }
    }

    private void refreshLocalPatchLightsOnly(ClientLevel level, LocalFogPatch patch) {
        int size = patch.gridSize;
        for (int gz = 0; gz < size; gz++) {
            int wz = patch.originZ + gz * LOCAL_CELL_STEP;
            int row = gz * size;
            for (int gx = 0; gx < size; gx++) {
                int idx = row + gx;
                float sy = patch.smoothY[idx];
                if (sy <= INVALID_Y + 1.0F) continue;
                int wx = patch.originX + gx * LOCAL_CELL_STEP;
                scratchPos.set(wx, Mth.floor(sy) + 1, wz);
                patch.packedLight[idx] = sampleLocalPackedLight(level, scratchPos);
            }
        }
    }

    /**
     * Merges all active local fog patches into ONE deduplicated {@link LocalCompositeGrid} around the camera.
     * Overlapping {@code FogSourceBlock}s fuse seamlessly into a single non-overlapping volumetric surface
     * (1x GPU overdraw even in massive 140-block dungeons), while multi-story floors (|dy| >= 3.0) are
     * tracked in {@code overflowPatches} so tall towers and bridges above a foggy floor still render cleanly.
     */
    private void rebuildDeduplicatedLocalBuffer(int centerX, int centerZ) {
        LocalCompositeGrid comp = this.localCompositeGrid;
        comp.clear();
        localMeshVersion++;
        if (activeLocalPatches.isEmpty()) {
            return;
        }

        if (compColorW == null) {
            compColorW = new float[COMP_CELLS];
        }

        int originX = centerX - COMP_RADIUS;
        int originZ = centerZ - COMP_RADIUS;
        comp.originX = originX;
        comp.originZ = originZ;

        Arrays.fill(comp.smoothY, INVALID_Y);
        Arrays.fill(comp.maskAlpha, 0.0F);
        Arrays.fill(comp.edgeFade, 0.0F);
        Arrays.fill(comp.solidWall, false);

        int dirtyMinX = COMP_SIZE, dirtyMaxX = -1;
        int dirtyMinZ = COMP_SIZE, dirtyMaxZ = -1;

        for (int p = 0; p < activeLocalPatches.size(); p++) {
            LocalFogPatch patch = activeLocalPatches.get(p);
            int pSize = patch.gridSize;
            float pr = patch.color.r;
            float pg = patch.color.g;
            float pb = patch.color.b;
            // Tall ~2.1-2.55 block soft volumetric height band: comfortably ABOVE standing eye height (1.62)
            // so standing/walking on the floor uses true 3D per-pixel depth-slab integration (zero flat
            // canopy plane under the eyes, zero horizon beam!) and wraps smoothly up the bottom 2 blocks of walls.
            float pHeight = 2.05F + 0.10F * patch.density; // 2.15 .. 2.55 blocks
            boolean patchHasOverflow = false;

            for (int gz = 0; gz < pSize; gz++) {
                int wz = patch.originZ + gz * LOCAL_CELL_STEP;
                int cz = wz - originZ;
                if (cz < 0 || cz >= COMP_SIZE) continue;
                int pRow = gz * pSize;
                int cRow = cz * COMP_SIZE;

                for (int gx = 0; gx < pSize; gx++) {
                    int pIdx = pRow + gx;
                    int wx = patch.originX + gx * LOCAL_CELL_STEP;
                    int cx = wx - originX;
                    if (cx < 0 || cx >= COMP_SIZE) continue;
                    int cIdx = cRow + cx;

                    float sy = patch.smoothY[pIdx];
                    if (sy <= INVALID_Y + 1.0F) {
                        if (patch.solidWall[pIdx] && comp.maskAlpha[cIdx] <= 0.0F) {
                            comp.solidWall[cIdx] = true;
                        }
                        continue;
                    }
                    float m = patch.alphaMask[pIdx];
                    float e = patch.openEdgeFade[pIdx];
                    if (m <= 0.004F || e <= 0.004F) continue;

                    float existingM = comp.maskAlpha[cIdx];
                    if (existingM <= 0.0F) {
                        comp.smoothY[cIdx] = sy;
                        comp.fogHeight[cIdx] = pHeight;
                        comp.maskAlpha[cIdx] = m;
                        comp.edgeFade[cIdx] = e;
                        comp.solidWall[cIdx] = false;
                        float w = m * e;
                        comp.r[cIdx] = pr * w;
                        comp.g[cIdx] = pg * w;
                        comp.b[cIdx] = pb * w;
                        compColorW[cIdx] = w;
                        comp.packedLight[cIdx] = patch.packedLight[pIdx];
                        if (cx < dirtyMinX) dirtyMinX = cx;
                        if (cx > dirtyMaxX) dirtyMaxX = cx;
                        if (cz < dirtyMinZ) dirtyMinZ = cz;
                        if (cz > dirtyMaxZ) dirtyMaxZ = cz;
                    } else if (Math.abs(sy - comp.smoothY[cIdx]) >= 3.0F) {
                        patchHasOverflow = true;
                    } else {
                        float w = m * e;
                        if (m > existingM) {
                            comp.maskAlpha[cIdx] = m;
                            comp.smoothY[cIdx] = sy;
                            comp.fogHeight[cIdx] = Math.max(comp.fogHeight[cIdx], pHeight);
                            comp.packedLight[cIdx] = patch.packedLight[pIdx];
                        }
                        if (e > comp.edgeFade[cIdx]) {
                            comp.edgeFade[cIdx] = e;
                        }
                        comp.r[cIdx] += pr * w;
                        comp.g[cIdx] += pg * w;
                        comp.b[cIdx] += pb * w;
                        compColorW[cIdx] += w;
                    }
                }
            }

            if (patchHasOverflow) {
                comp.overflowPatches.add(patch);
            }
        }

        if (dirtyMaxX < dirtyMinX) {
            return;
        }

        // Normalize accumulated color weights inside dirty bounds & track vertical Y bounds
        float minY = Float.MAX_VALUE;
        float maxTopY = -Float.MAX_VALUE;
        for (int cz = dirtyMinZ; cz <= dirtyMaxZ; cz++) {
            int cRow = cz * COMP_SIZE;
            for (int cx = dirtyMinX; cx <= dirtyMaxX; cx++) {
                int cIdx = cRow + cx;
                if (comp.maskAlpha[cIdx] <= 0.004F) continue;
                float invW = compColorW[cIdx] > 0.0001F ? (1.0F / compColorW[cIdx]) : 1.0F;
                comp.r[cIdx] *= invW;
                comp.g[cIdx] *= invW;
                comp.b[cIdx] *= invW;
                float sy = comp.smoothY[cIdx];
                float topY = sy + comp.fogHeight[cIdx];
                if (sy < minY) minY = sy;
                if (topY > maxTopY) maxTopY = topY;
            }
        }

        // Expand dirty bounds by 1 cell so 1-texel dilation covers perimeter neighbors cleanly
        comp.dirtyMinX = Math.max(0, dirtyMinX - 1);
        comp.dirtyMaxX = Math.min(COMP_SIZE - 1, dirtyMaxX + 1);
        comp.dirtyMinZ = Math.max(0, dirtyMinZ - 1);
        comp.dirtyMaxZ = Math.min(COMP_SIZE - 1, dirtyMaxZ + 1);
        comp.minFloorY = minY;
        comp.maxFogTopY = maxTopY;
        comp.hasData = true;
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
