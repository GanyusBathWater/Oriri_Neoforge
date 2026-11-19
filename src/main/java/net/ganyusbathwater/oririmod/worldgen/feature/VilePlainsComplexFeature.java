package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.util.FastNoise;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsBiomeSource;
import net.ganyusbathwater.oririmod.worldgen.processor.OutpostTerrainBlendProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Deterministic multi-chunk ecosystem feature for the paired {@code vile_plains} and {@code vile_caves} biomes.
 * <p>
 * Runs once per chunk and clips all placements strictly to the current chunk's 16x16 bounds,
 * eliminating cross-chunk cutoffs and far-chunk warnings even for 120-block tilted vines and
 * 59x59 floating Outpost Ruins islands.
 * <p>
 * Strict per-chunk ordering:
 * <ol>
 *   <li>Carve chaotic surface craters sized proportionally to the floating islands pulled from below.</li>
 *   <li>Build floating rocky islands (purple grass + dirt + stone on surface; strictly stone-only inside Vile Caves).</li>
 *   <li>Place {@code elderwoods_outpost_ruins} on the largest floating surface islands.</li>
 *   <li>Grow all twisting Vile Vines &amp; curling tendrils LAST so they wrap islands and burrow straight through
 *       Outpost Ruins with zero regard for what lies in their path.</li>
 * </ol>
 */
public class VilePlainsComplexFeature extends Feature<NoneFeatureConfiguration> {

    private static final int SURFACE_CELL = 80;
    private static final int CAVE_CELL = 44;
    private static final int MAX_SURFACE_SEARCH_RADIUS = 2; // +-2 cells covers 160+ block vine/island reach
    private static final int MAX_CAVE_SEARCH_RADIUS = 1;

    private static final ResourceLocation[] OUTPOST_RUINS_TEMPLATES = {
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods_outpost_ruins_0_0"),
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods_outpost_ruins_0_48"),
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods_outpost_ruins_48_0"),
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods_outpost_ruins_48_48")
    };
    private static final int[][] OUTPOST_GRID_OFFSETS = {
            { 0, 0 }, { 0, 48 }, { 48, 0 }, { 48, 48 }
    };

    private static final class CraterSpec {
        final double centerX, centerZ;
        final int surfaceY;
        final double radius;
        final double depth;
        final double phase1, phase2, phase3;
        final double minX, maxX, minZ, maxZ;

        CraterSpec(double centerX, double centerZ, int surfaceY, double radius, double depth, RandomSource rnd) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.surfaceY = surfaceY;
            this.radius = radius;
            this.depth = depth;
            this.phase1 = rnd.nextDouble() * Math.PI * 2.0;
            this.phase2 = rnd.nextDouble() * Math.PI * 2.0;
            this.phase3 = rnd.nextDouble() * Math.PI * 2.0;
            double pad = radius * 1.45 + 3.0;
            this.minX = centerX - pad;
            this.maxX = centerX + pad;
            this.minZ = centerZ - pad;
            this.maxZ = centerZ + pad;
        }
    }

    private static final class IslandSpec {
        final double centerX, centerZ;
        final int topY;
        final double radius;
        final double thickness;
        final boolean surfaceGrass; // true = grass/dirt/stone surface island, false = stone-only cave island
        final boolean hasOutpost;
        final Rotation outpostRotation;
        final double phase1, phase2;
        final double minX, maxX, minZ, maxZ;

        IslandSpec(double centerX, double centerZ, int topY, double radius, double thickness,
                boolean surfaceGrass, boolean hasOutpost, Rotation outpostRotation, RandomSource rnd) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.topY = topY;
            this.radius = radius;
            this.thickness = thickness;
            this.surfaceGrass = surfaceGrass;
            this.hasOutpost = hasOutpost;
            this.outpostRotation = outpostRotation;
            this.phase1 = rnd.nextDouble() * Math.PI * 2.0;
            this.phase2 = rnd.nextDouble() * Math.PI * 2.0;
            double pad = radius * 1.30 + (hasOutpost ? 12.0 : 3.0);
            this.minX = centerX - pad;
            this.maxX = centerX + pad;
            this.minZ = centerZ - pad;
            this.maxZ = centerZ + pad;
        }
    }

    private static final class SitePlan {
        final boolean valid;
        final List<CraterSpec> craters;
        final List<IslandSpec> islands;
        final List<VileVineGeometry.VineSpec> vines;
        final double minX, maxX, minZ, maxZ;

        static final SitePlan EMPTY = new SitePlan(false, List.of(), List.of(), List.of());

        SitePlan(boolean valid, List<CraterSpec> craters, List<IslandSpec> islands,
                List<VileVineGeometry.VineSpec> vines) {
            this.valid = valid;
            this.craters = craters;
            this.islands = islands;
            this.vines = vines;

            double mnX = Double.POSITIVE_INFINITY, mxX = Double.NEGATIVE_INFINITY;
            double mnZ = Double.POSITIVE_INFINITY, mxZ = Double.NEGATIVE_INFINITY;
            for (CraterSpec c : craters) {
                if (c.minX < mnX) mnX = c.minX;
                if (c.maxX > mxX) mxX = c.maxX;
                if (c.minZ < mnZ) mnZ = c.minZ;
                if (c.maxZ > mxZ) mxZ = c.maxZ;
            }
            for (IslandSpec isl : islands) {
                if (isl.minX < mnX) mnX = isl.minX;
                if (isl.maxX > mxX) mxX = isl.maxX;
                if (isl.minZ < mnZ) mnZ = isl.minZ;
                if (isl.maxZ > mxZ) mxZ = isl.maxZ;
            }
            for (VileVineGeometry.VineSpec v : vines) {
                if (v.minX < mnX) mnX = v.minX;
                if (v.maxX > mxX) mxX = v.maxX;
                if (v.minZ < mnZ) mnZ = v.minZ;
                if (v.maxZ > mxZ) mxZ = v.maxZ;
            }
            this.minX = mnX;
            this.maxX = mxX;
            this.minZ = mnZ;
            this.maxZ = mxZ;
        }

        boolean overlaps(int x0, int x1, int z0, int z1) {
            return valid && maxX >= x0 && minX <= x1 && maxZ >= z0 && minZ <= z1;
        }
    }

    private static final class SiteCache {
        long seed = Long.MIN_VALUE;
        final LinkedHashMap<Long, SitePlan> surface = new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, SitePlan> eldest) {
                return size() > 256;
            }
        };
        final LinkedHashMap<Long, SitePlan> cave = new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, SitePlan> eldest) {
                return size() > 256;
            }
        };
    }

    private static final ThreadLocal<SiteCache> SITE_CACHE = ThreadLocal.withInitial(SiteCache::new);

    public VilePlainsComplexFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkGenerator generator = context.chunkGenerator();
        if (!(generator.getBiomeSource() instanceof ElderwoodsBiomeSource ebs)) {
            return false;
        }

        BlockPos origin = context.origin();
        int chunkMinX = (origin.getX() >> 4) << 4;
        int chunkMinZ = (origin.getZ() >> 4) << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;
        long worldSeed = level.getSeed();

        SiteCache cache = SITE_CACHE.get();
        if (cache.seed != worldSeed) {
            cache.surface.clear();
            cache.cave.clear();
            cache.seed = worldSeed;
        }

        int centerSurfaceCellX = Math.floorDiv(chunkMinX + 8, SURFACE_CELL);
        int centerSurfaceCellZ = Math.floorDiv(chunkMinZ + 8, SURFACE_CELL);

        List<SitePlan> activeSites = new ArrayList<>(8);

        for (int dx = -MAX_SURFACE_SEARCH_RADIUS; dx <= MAX_SURFACE_SEARCH_RADIUS; dx++) {
            for (int dz = -MAX_SURFACE_SEARCH_RADIUS; dz <= MAX_SURFACE_SEARCH_RADIUS; dz++) {
                int cx = centerSurfaceCellX + dx;
                int cz = centerSurfaceCellZ + dz;
                SitePlan plan = getOrBuildSurfaceSite(cache, worldSeed, cx, cz, ebs, level.getMaxBuildHeight());
                if (plan.overlaps(chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ)) {
                    activeSites.add(plan);
                }
            }
        }

        int centerCaveCellX = Math.floorDiv(chunkMinX + 8, CAVE_CELL);
        int centerCaveCellZ = Math.floorDiv(chunkMinZ + 8, CAVE_CELL);
        for (int dx = -MAX_CAVE_SEARCH_RADIUS; dx <= MAX_CAVE_SEARCH_RADIUS; dx++) {
            for (int dz = -MAX_CAVE_SEARCH_RADIUS; dz <= MAX_CAVE_SEARCH_RADIUS; dz++) {
                int cx = centerCaveCellX + dx;
                int cz = centerCaveCellZ + dz;
                SitePlan plan = getOrBuildCaveSite(cache, worldSeed, cx, cz, ebs);
                if (plan.overlaps(chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ)) {
                    activeSites.add(plan);
                }
            }
        }

        boolean chunkHasVilePlains = ebs.getSurfaceBiome(chunkMinX + 8, chunkMinZ + 8).is(ElderwoodsBiomeSource.VILE_PLAINS_KEY)
                || ebs.getSurfaceBiome(chunkMinX, chunkMinZ).is(ElderwoodsBiomeSource.VILE_PLAINS_KEY)
                || ebs.getSurfaceBiome(chunkMaxX, chunkMinZ).is(ElderwoodsBiomeSource.VILE_PLAINS_KEY)
                || ebs.getSurfaceBiome(chunkMinX, chunkMaxZ).is(ElderwoodsBiomeSource.VILE_PLAINS_KEY)
                || ebs.getSurfaceBiome(chunkMaxX, chunkMaxZ).is(ElderwoodsBiomeSource.VILE_PLAINS_KEY);

        if (activeSites.isEmpty() && !chunkHasVilePlains) {
            return false;
        }

        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        BoundingBox chunkBox = new BoundingBox(
                chunkMinX, level.getMinBuildHeight(), chunkMinZ,
                chunkMaxX, level.getMaxBuildHeight() - 1, chunkMaxZ);

        // STEP 1: Carve chaotic surface craters
        for (SitePlan site : activeSites) {
            for (CraterSpec crater : site.craters) {
                carveCraterClipped(level, mPos, crater, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
            }
        }

        // STEP 2: Build floating rocky islands (surface purple-grass islands + stone-only cave islands)
        for (SitePlan site : activeSites) {
            for (IslandSpec island : site.islands) {
                placeIslandClipped(level, mPos, island, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
            }
        }

        // STEP 3: Place Outpost Ruins on the largest floating islands
        StructureTemplateManager templateManager = level.getLevel().getStructureManager();
        for (SitePlan site : activeSites) {
            for (IslandSpec island : site.islands) {
                if (island.hasOutpost) {
                    placeOutpostRuinsClipped(level, templateManager, island, chunkBox, context.random());
                }
            }
        }

        // STEP 4: Grow ALL twisting Vile Vines & curling tendrils LAST so they wrap islands
        // and burrow straight through Outpost Ruins regardless of what lies in their way!
        VileVineGeometry.ReplacePredicate burrowAllExceptBedrock = (lvl, pos, existing, aboveBase) ->
                !existing.is(Blocks.BEDROCK) && existing.getDestroySpeed(lvl, pos) >= 0.0F;

        for (SitePlan site : activeSites) {
            for (VileVineGeometry.VineSpec vine : site.vines) {
                VileVineGeometry.placeVine(
                        level, vine,
                        chunkMinX, chunkMaxX,
                        chunkMinZ, chunkMaxZ,
                        2, burrowAllExceptBedrock);
            }
        }

        // STEP 5: Populate natural short & tall grass on both floating surface islands AND Vile Plains ground
        populateGrassClipped(level, mPos, ebs, activeSites, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, worldSeed);

        return true;
    }

    private static long packCellKey(int cx, int cz) {
        return (((long) cx) << 32) ^ (cz & 0xFFFFFFFFL);
    }

    private static RandomSource createCellRandom(long worldSeed, int cx, int cz, long salt) {
        long h = worldSeed ^ salt;
        h ^= cx * 341873128712L;
        h ^= cz * 132897987541L;
        h = (h ^ (h >>> 30)) * 0xbf58476d1ce4e5b9L;
        h = (h ^ (h >>> 27)) * 0x94d049bb133111ebL;
        h = h ^ (h >>> 31);
        return RandomSource.create(h);
    }

    private SitePlan getOrBuildSurfaceSite(SiteCache cache, long worldSeed, int cx, int cz,
            ElderwoodsBiomeSource ebs, int maxBuildHeight) {
        long key = packCellKey(cx, cz);
        SitePlan cached = cache.surface.get(key);
        if (cached != null) {
            return cached;
        }
        SitePlan built = buildSurfaceSite(worldSeed, cx, cz, ebs, maxBuildHeight);
        cache.surface.put(key, built);
        return built;
    }

    private SitePlan buildSurfaceSite(long worldSeed, int cx, int cz, ElderwoodsBiomeSource ebs, int maxBuildHeight) {
        RandomSource rnd = createCellRandom(worldSeed, cx, cz, 0x56494C4553555246L);

        int anchorX = cx * SURFACE_CELL + 14 + rnd.nextInt(SURFACE_CELL - 28);
        int anchorZ = cz * SURFACE_CELL + 14 + rnd.nextInt(SURFACE_CELL - 28);

        if (!ebs.getSurfaceBiome(anchorX, anchorZ).is(ElderwoodsBiomeSource.VILE_PLAINS_KEY)) {
            return SitePlan.EMPTY;
        }
        int surfaceY = ebs.computeSurfaceHeight(anchorX, anchorZ, null);
        if (surfaceY < 66) {
            return SitePlan.EMPTY;
        }

        // Check whether a generous 38-block interior ring is Vile Plains (eligible for an Outpost Ruins floating island)
        boolean deepInterior = true;
        int[][] probes = {
                { 38, 0 }, { -38, 0 }, { 0, 38 }, { 0, -38 },
                { 27, 27 }, { -27, 27 }, { 27, -27 }, { -27, -27 }
        };
        for (int[] p : probes) {
            if (!ebs.getSurfaceBiome(anchorX + p[0], anchorZ + p[1]).is(ElderwoodsBiomeSource.VILE_PLAINS_KEY)) {
                deepInterior = false;
                break;
            }
        }

        // 1. Giant Floating Island Site (even parity keeps spacing >= 160 blocks; 40% have Outpost Ruins, 60% natural)
        if (deepInterior && (cx & 1) == 0 && (cz & 1) == 0 && rnd.nextFloat() < 0.28F) {
            return buildGiantFloatingIslandSite(anchorX, surfaceY, anchorZ, ebs, maxBuildHeight, rnd);
        }

        List<CraterSpec> craters = new ArrayList<>(2);
        List<IslandSpec> islands = new ArrayList<>(4);
        List<VileVineGeometry.VineSpec> vines = new ArrayList<>(4);

        // Decide whether this cell has floating islands (~65%) or solo/duo sky vines (~35%)
        boolean hasIslands = rnd.nextFloat() < 0.68F;
        int islandCount = hasIslands ? (1 + rnd.nextInt(3)) : 0; // 1..3 islands (can sit on same vine!)

        // Pre-roll island radii so we know the required vine strength & crater size
        double[] plannedRadii = new double[islandCount];
        double maxIslandR = 0.0;
        double totalIslandAreaSq = 0.0;
        for (int i = 0; i < islandCount; i++) {
            float roll = rnd.nextFloat();
            double r;
            if (roll < 0.45F) {
                r = 5.5 + rnd.nextDouble() * 3.5; // Small (5.5..9)
            } else if (roll < 0.82F) {
                r = 9.5 + rnd.nextDouble() * 5.0; // Medium (9.5..14.5)
            } else {
                r = 15.0 + rnd.nextDouble() * 6.0; // Large (15..21)
            }
            plannedRadii[i] = r;
            if (r > maxIslandR) maxIslandR = r;
            totalIslandAreaSq += r * r;
        }

        // Match vine thickness and quantity to the largest island carried
        double minWidth, maxWidth;
        int extraAnchorVines = 0;
        if (islandCount == 0) {
            minWidth = 2.0;
            maxWidth = 6.0;
        } else if (maxIslandR <= 9.0) {
            minWidth = 2.6;
            maxWidth = 4.4;
        } else if (maxIslandR <= 14.5) {
            minWidth = 4.2;
            maxWidth = 5.8;
            extraAnchorVines = rnd.nextBoolean() ? 1 : 0;
        } else {
            minWidth = 5.2;
            maxWidth = 6.0;
            extraAnchorVines = 1 + rnd.nextInt(2); // 1..2 coiling support vines for big islands
        }

        double maxHeightAllowed = Math.max(30.0, (maxBuildHeight - 10) - surfaceY);
        double primaryHeight = Math.min(80.0 + rnd.nextDouble() * 40.0, maxHeightAllowed);
        int vileBottom = ElderwoodsBiomeSource.getVileCaveBottomY(anchorX, anchorZ);
        double primaryDepth = Math.max(25.0, surfaceY - (vileBottom - (8 + rnd.nextInt(14))));

        // Create primary twisting Vile Vine
        VileVineGeometry.VineSpec primaryVine = VileVineGeometry.createVine(
                anchorX + 0.5, surfaceY, anchorZ + 0.5,
                minWidth, maxWidth,
                primaryHeight, primaryDepth,
                60.0, Double.NaN,
                6 + rnd.nextInt(5), rnd);

        // Place multiple islands directly along the primary vine at distinct heights
        double[] spinePt = new double[3];
        List<VileVineGeometry.TendrilSpec> wrapTendrils = new ArrayList<>();
        for (int i = 0; i < islandCount; i++) {
            double r = plannedRadii[i];
            // Spread islands across heights (22%..78% of vine height) so multiple islands on one vine don't collide
            double bandMin = 0.20 + (0.58 * i) / Math.max(1, islandCount);
            double bandMax = Math.min(0.82, bandMin + 0.45 / Math.max(1, islandCount));
            double frac = bandMin + rnd.nextDouble() * Math.max(0.05, bandMax - bandMin);
            double s = Mth.clamp(primaryHeight * frac, 18.0, primaryHeight - 12.0);
            primaryVine.sampleSpine(s, spinePt);

            // Offset slightly so the vine pierces or hugs one flank organically
            double offsetAng = rnd.nextDouble() * Math.PI * 2.0;
            double offsetDist = r * (0.15 + rnd.nextDouble() * 0.45);
            double ix = spinePt[0] + Math.cos(offsetAng) * offsetDist;
            double iz = spinePt[2] + Math.sin(offsetAng) * offsetDist;
            int topY = Mth.clamp((int) Math.round(spinePt[1] + r * 0.35), surfaceY + 14, maxBuildHeight - 12);
            double thickness = r * (0.85 + rnd.nextDouble() * 0.45);

            islands.add(new IslandSpec(ix, iz, topY, r, thickness, true, false, Rotation.NONE, rnd));

            // Add 2-3 curling tendrils wrapping from the vine around the island's rocky underside
            int wraps = r >= 12.0 ? 3 : 2;
            for (int w = 0; w < wraps; w++) {
                double wAng = rnd.nextDouble() * Math.PI * 2.0;
                double tx = ix + Math.cos(wAng) * (r * 0.82);
                double ty = topY - thickness * (0.25 + rnd.nextDouble() * 0.45);
                double tz = iz + Math.sin(wAng) * (r * 0.82);
                primaryVine.sampleSpine(Math.max(4.0, s - thickness * 0.65), spinePt);
                wrapTendrils.add(VileVineGeometry.createIslandWrapTendril(
                        spinePt[0], spinePt[1], spinePt[2],
                        tx, ty, tz,
                        1.4 + rnd.nextDouble() * 0.9, rnd));
            }
        }

        // Attach extra island-wrapping tendrils to the primary vine
        if (!wrapTendrils.isEmpty()) {
            List<VileVineGeometry.TendrilSpec> allTendrils = new ArrayList<>(primaryVine.tendrils);
            allTendrils.addAll(wrapTendrils);
            primaryVine = new VileVineGeometry.VineSpec(
                    primaryVine.baseX, primaryVine.baseY, primaryVine.baseZ,
                    primaryVine.heightAbove, primaryVine.depthBelow,
                    primaryVine.tiltRad, primaryVine.azimuth, primaryVine.baseWidth,
                    primaryVine.twistRate, primaryVine.twistPhase,
                    primaryVine.swayAmp1, primaryVine.swayAmp2,
                    primaryVine.swayFreq1, primaryVine.swayFreq2,
                    primaryVine.swayPhase1, primaryVine.swayPhase2,
                    primaryVine.rootTiltScale,
                    allTendrils);
        }
        vines.add(primaryVine);

        // Extra anchor vines for medium/large islands (emerge from the same crater and rise toward the largest island)
        IslandSpec biggestIsland = null;
        for (IslandSpec isl : islands) {
            if (biggestIsland == null || isl.radius > biggestIsland.radius) {
                biggestIsland = isl;
            }
        }
        for (int a = 0; a < extraAnchorVines && biggestIsland != null; a++) {
            double ang = (Math.PI * 2.0 * a / extraAnchorVines) + rnd.nextDouble() * 1.4;
            double startDist = 5.0 + rnd.nextDouble() * 8.0;
            double bx = anchorX + Math.cos(ang) * startDist;
            double bz = anchorZ + Math.sin(ang) * startDist;
            int bSurfY = ebs.computeSurfaceHeight((int) bx, (int) bz, null);

            // Aim toward the flank of the biggest island
            double targetAng = ang + 0.6;
            double tx = biggestIsland.centerX + Math.cos(targetAng) * (biggestIsland.radius * 0.55);
            double tz = biggestIsland.centerZ + Math.sin(targetAng) * (biggestIsland.radius * 0.55);
            double dy = Math.max(16.0, (biggestIsland.topY - biggestIsland.thickness * 0.35) - bSurfY);
            double dx = tx - bx;
            double dz = tz - bz;
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            double tiltRad = Math.min(Math.toRadians(58.0), Math.atan2(horizDist, dy));
            double az = Math.atan2(dz, dx);

            double h = Math.min(80.0 + rnd.nextDouble() * 38.0, maxHeightAllowed);
            double d = Math.max(25.0, bSurfY - (vileBottom - (6 + rnd.nextInt(14))));
            vines.add(createAimedVine(bx, bSurfY, bz, 3.2 + rnd.nextDouble() * 2.0, h, d, tiltRad, az, 4 + rnd.nextInt(3), rnd));
        }

        // Occasional companion solo vine nearby (no extra island) to create tangled skyline silhouettes
        if (islandCount == 0 && rnd.nextFloat() < 0.50F) {
            double cAng = rnd.nextDouble() * Math.PI * 2.0;
            double cDist = 12.0 + rnd.nextDouble() * 14.0;
            double bx = anchorX + Math.cos(cAng) * cDist;
            double bz = anchorZ + Math.sin(cAng) * cDist;
            if (ebs.getSurfaceBiome((int) bx, (int) bz).is(ElderwoodsBiomeSource.VILE_PLAINS_KEY)) {
                int bSurfY = ebs.computeSurfaceHeight((int) bx, (int) bz, null);
                double h = Math.min(80.0 + rnd.nextDouble() * 35.0, Math.max(30.0, (maxBuildHeight - 10) - bSurfY));
                double d = Math.max(25.0, bSurfY - (vileBottom - (6 + rnd.nextInt(12))));
                vines.add(VileVineGeometry.createVine(bx, bSurfY, bz, 2.0, 4.8, h, d, 60.0, Double.NaN, 4 + rnd.nextInt(4), rnd));
            }
        }

        // Surface crater sized directly by the total island mass pulled up from this site!
        if (islandCount > 0) {
            double craterRadius = Mth.clamp(Math.sqrt(totalIslandAreaSq) * 1.12, 6.5, 25.0);
            double craterDepth = Mth.clamp(craterRadius * (0.58 + rnd.nextDouble() * 0.28), 5.0, 22.0);
            // Highest chance directly under the main vine, occasionally slightly offset near it
            double offset = rnd.nextFloat() < 0.72F ? (rnd.nextDouble() * 3.0) : (4.0 + rnd.nextDouble() * 6.0);
            double oAng = rnd.nextDouble() * Math.PI * 2.0;
            double crX = anchorX + Math.cos(oAng) * offset;
            double crZ = anchorZ + Math.sin(oAng) * offset;
            int crSurfY = ebs.computeSurfaceHeight((int) crX, (int) crZ, null);
            craters.add(new CraterSpec(crX, crZ, crSurfY, craterRadius, craterDepth, rnd));
        } else if (rnd.nextFloat() < 0.45F) {
            // Small chaotic root tear crater beneath solo vines
            double crR = 4.0 + rnd.nextDouble() * 3.5;
            double crD = 3.0 + rnd.nextDouble() * 3.0;
            craters.add(new CraterSpec(anchorX + 0.5, anchorZ + 0.5, surfaceY, crR, crD, rnd));
        }

        return new SitePlan(true, craters, islands, vines);
    }

    /**
     * Builds a monumental Giant Floating Island site:
     * 40% chance to feature {@code elderwoods_outpost_ruins} on its plateau, 60% chance to be a wild
     * purple-grass floating island — both held up by 5–6 massive anchor/piercing vines above a huge crater.
     */
    private SitePlan buildGiantFloatingIslandSite(int anchorX, int surfaceY, int anchorZ,
            ElderwoodsBiomeSource ebs, int maxBuildHeight, RandomSource rnd) {
        List<CraterSpec> craters = new ArrayList<>(1);
        List<IslandSpec> islands = new ArrayList<>(2);
        List<VileVineGeometry.VineSpec> vines = new ArrayList<>(6);

        boolean hasOutpost = rnd.nextFloat() < 0.40F; // 40% with Outpost Ruins, 60% without
        double islandRadius = hasOutpost
                ? (35.0 + rnd.nextDouble() * 3.0) // comfortably supports the 59x59 Outpost Ruins
                : (30.0 + rnd.nextDouble() * 7.0); // massive natural floating island
        int islandTopY = Math.min(maxBuildHeight - 60, surfaceY + 46 + rnd.nextInt(16));
        double islandThickness = 25.0 + rnd.nextDouble() * 6.0;
        Rotation rot = Rotation.values()[rnd.nextInt(4)];

        islands.add(new IslandSpec(anchorX + 0.5, anchorZ + 0.5, islandTopY,
                islandRadius, islandThickness, true, hasOutpost, rot, rnd));

        // Huge chaotic crater directly beneath the ripped-out island
        double craterRadius = 29.0 + rnd.nextDouble() * 5.0;
        double craterDepth = 19.0 + rnd.nextDouble() * 9.0;
        craters.add(new CraterSpec(anchorX + 0.5, anchorZ + 0.5, surfaceY, craterRadius, craterDepth, rnd));

        int vileBottom = ElderwoodsBiomeSource.getVileCaveBottomY(anchorX, anchorZ);
        double maxHeightAllowed = Math.max(40.0, (maxBuildHeight - 10) - surfaceY);

        // 3–4 outer anchor vines rising from the crater floor, coiling around the island rim and reaching skyward
        int outerVines = 3 + rnd.nextInt(2);
        for (int i = 0; i < outerVines; i++) {
            double ang = (Math.PI * 2.0 * i / outerVines) + (rnd.nextDouble() - 0.5) * 0.45;
            double baseDist = 14.0 + rnd.nextDouble() * 10.0;
            double bx = anchorX + Math.cos(ang) * baseDist;
            double bz = anchorZ + Math.sin(ang) * baseDist;
            int bSurfY = ebs.computeSurfaceHeight((int) bx, (int) bz, null);

            // Aim outward slightly toward the island rim
            double rimDist = islandRadius * (0.78 + rnd.nextDouble() * 0.18);
            double tx = anchorX + Math.cos(ang + 0.25) * rimDist;
            double tz = anchorZ + Math.sin(ang + 0.25) * rimDist;
            double dy = Math.max(20.0, islandTopY - bSurfY);
            double dx = tx - bx;
            double dz = tz - bz;
            double tiltRad = Math.min(Math.toRadians(54.0), Math.atan2(Math.sqrt(dx * dx + dz * dz), dy));
            double az = Math.atan2(dz, dx);

            double h = Math.min(92.0 + rnd.nextDouble() * 28.0, maxHeightAllowed);
            double d = Math.max(30.0, bSurfY - (vileBottom - (10 + rnd.nextInt(14))));
            vines.add(createAimedVine(bx, bSurfY, bz, 4.8 + rnd.nextDouble() * 1.2, h, d, tiltRad, az, 6 + rnd.nextInt(4), rnd));
        }

        // 2–3 piercing vines that burrow STRAIGHT THROUGH the floating island & the Outpost Ruins structure!
        int piercingVines = 2 + rnd.nextInt(2);
        for (int i = 0; i < piercingVines; i++) {
            double ang = (Math.PI * 2.0 * i / piercingVines) + rnd.nextDouble() * 1.2;
            double baseDist = 3.0 + rnd.nextDouble() * 9.0;
            double bx = anchorX + Math.cos(ang) * baseDist;
            double bz = anchorZ + Math.sin(ang) * baseDist;
            int bSurfY = ebs.computeSurfaceHeight((int) bx, (int) bz, null);

            // Aim right through the interior of the 59x59 Outpost Ruins (within +-18 blocks of island center!)
            double ruinTargetDist = 5.0 + rnd.nextDouble() * 14.0;
            double tAng = ang + Math.PI * (0.4 + rnd.nextDouble() * 0.9);
            double tx = anchorX + Math.cos(tAng) * ruinTargetDist;
            double tz = anchorZ + Math.sin(tAng) * ruinTargetDist;
            double dy = Math.max(20.0, (islandTopY + 10.0) - bSurfY);
            double dx = tx - bx;
            double dz = tz - bz;
            double tiltRad = Math.min(Math.toRadians(45.0), Math.atan2(Math.sqrt(dx * dx + dz * dz), dy));
            double az = Math.atan2(dz, dx);

            double h = Math.min(96.0 + rnd.nextDouble() * 24.0, maxHeightAllowed);
            double d = Math.max(30.0, bSurfY - (vileBottom - (12 + rnd.nextInt(14))));
            vines.add(createAimedVine(bx, bSurfY, bz, 4.6 + rnd.nextDouble() * 1.4, h, d, tiltRad, az, 5 + rnd.nextInt(4), rnd));
        }

        return new SitePlan(true, craters, islands, vines);
    }

    private static VileVineGeometry.VineSpec createAimedVine(
            double bx, double by, double bz,
            double width, double heightAbove, double depthBelow,
            double tiltRad, double azimuth,
            int tendrilCount, RandomSource rnd) {
        double twistDir = rnd.nextBoolean() ? 1.0 : -1.0;
        double twistRate = twistDir * (0.17 + rnd.nextDouble() * 0.08);
        double twistPhase = rnd.nextDouble() * Math.PI * 2.0;
        double swayAmp1 = 1.4 + rnd.nextDouble() * 2.2;
        double swayAmp2 = 0.6 + rnd.nextDouble() * 1.1;
        double swayFreq1 = 0.045 + rnd.nextDouble() * 0.03;
        double swayFreq2 = 0.11 + rnd.nextDouble() * 0.05;
        double swayPhase1 = rnd.nextDouble() * Math.PI * 2.0;
        double swayPhase2 = rnd.nextDouble() * Math.PI * 2.0;
        double rootTiltScale = 0.35 + rnd.nextDouble() * 0.30;

        List<VileVineGeometry.TendrilSpec> tendrils = new ArrayList<>(tendrilCount);
        VileVineGeometry.VineSpec tmp = new VileVineGeometry.VineSpec(
                bx, by, bz, heightAbove, depthBelow, tiltRad, azimuth, width,
                twistRate, twistPhase, swayAmp1, swayAmp2, swayFreq1, swayFreq2, swayPhase1, swayPhase2,
                rootTiltScale, tendrils);

        double[] p = new double[3];
        for (int i = 0; i < tendrilCount; i++) {
            double s = (i < tendrilCount / 3 && depthBelow > 12.0)
                    ? -(6.0 + rnd.nextDouble() * (depthBelow * 0.65))
                    : (10.0 + rnd.nextDouble() * Math.max(12.0, heightAbove * 0.80));
            tmp.sampleSpine(s, p);
            double tAz = rnd.nextDouble() * Math.PI * 2.0;
            double upBias = s >= 0.0 ? (0.2 + rnd.nextDouble() * 0.6) : (-0.3 + rnd.nextDouble() * 0.6);
            double horiz = Math.sqrt(Math.max(0.05, 1.0 - upBias * upBias));
            tendrils.add(new VileVineGeometry.TendrilSpec(
                    p[0], p[1], p[2],
                    Math.cos(tAz) * horiz, upBias, Math.sin(tAz) * horiz,
                    9.0 + rnd.nextDouble() * 15.0,
                    1.2 + rnd.nextDouble() * 1.2,
                    (rnd.nextBoolean() ? 1.0 : -1.0) * (0.28 + rnd.nextDouble() * 0.16),
                    1.3 + rnd.nextDouble() * 2.0,
                    rnd.nextDouble() * Math.PI * 2.0));
        }

        return new VileVineGeometry.VineSpec(
                bx, by, bz, heightAbove, depthBelow, tiltRad, azimuth, width,
                twistRate, twistPhase, swayAmp1, swayAmp2, swayFreq1, swayFreq2, swayPhase1, swayPhase2,
                rootTiltScale, tendrils);
    }

    private SitePlan getOrBuildCaveSite(SiteCache cache, long worldSeed, int cx, int cz, ElderwoodsBiomeSource ebs) {
        long key = packCellKey(cx, cz);
        SitePlan cached = cache.cave.get(key);
        if (cached != null) {
            return cached;
        }
        SitePlan built = buildCaveSite(worldSeed, cx, cz, ebs);
        cache.cave.put(key, built);
        return built;
    }

    /**
     * Builds underground Vile Caves floor-to-ceiling twisting vines and suspended STONE-ONLY islands
     * (never grass/dirt).
     */
    private SitePlan buildCaveSite(long worldSeed, int cx, int cz, ElderwoodsBiomeSource ebs) {
        RandomSource rnd = createCellRandom(worldSeed, cx, cz, 0x56494C4543415645L);
        int anchorX = cx * CAVE_CELL + 8 + rnd.nextInt(CAVE_CELL - 16);
        int anchorZ = cz * CAVE_CELL + 8 + rnd.nextInt(CAVE_CELL - 16);

        if (!ebs.getSurfaceBiome(anchorX, anchorZ).is(ElderwoodsBiomeSource.VILE_PLAINS_KEY)) {
            return SitePlan.EMPTY;
        }
        int surfaceY = ebs.computeSurfaceHeight(anchorX, anchorZ, null);
        if (surfaceY < 66) {
            return SitePlan.EMPTY;
        }

        int caveBottom = ElderwoodsBiomeSource.getVileCaveBottomY(anchorX, anchorZ);
        int caveCeiling = surfaceY - 18;
        if (caveCeiling - caveBottom < 24) {
            return SitePlan.EMPTY;
        }

        List<IslandSpec> islands = new ArrayList<>(2);
        List<VileVineGeometry.VineSpec> vines = new ArrayList<>(2);

        int midY = (caveBottom + caveCeiling) / 2;
        // Vine spans from deep deepslate below the cave floor up into the cave roof stone
        double heightAbove = (caveCeiling + 10) - midY;
        double depthBelow = midY - (caveBottom - (10 + rnd.nextInt(14)));

        VileVineGeometry.VineSpec caveVine = VileVineGeometry.createVine(
                anchorX + 0.5, midY, anchorZ + 0.5,
                2.2, 5.2,
                heightAbove, depthBelow,
                42.0, Double.NaN,
                4 + rnd.nextInt(4), rnd);
        vines.add(caveVine);

        // Attach 1–2 suspended STONE-ONLY islands inside the cavern wrapped by the vine
        if (rnd.nextFloat() < 0.70F) {
            int count = rnd.nextFloat() < 0.35F ? 2 : 1;
            double[] pt = new double[3];
            for (int i = 0; i < count; i++) {
                double s = -((caveCeiling - caveBottom) * 0.25) + rnd.nextDouble() * ((caveCeiling - caveBottom) * 0.50);
                caveVine.sampleSpine(s, pt);
                double r = 4.2 + rnd.nextDouble() * 5.2; // 4.2 .. 9.4 stone boulder / suspended ledge
                double thickness = r * (0.65 + rnd.nextDouble() * 0.45);
                int topY = Mth.clamp((int) Math.round(pt[1] + thickness * 0.35), caveBottom + 10, caveCeiling - 6);
                double ang = rnd.nextDouble() * Math.PI * 2.0;
                double ix = pt[0] + Math.cos(ang) * (r * 0.35);
                double iz = pt[2] + Math.sin(ang) * (r * 0.35);
                // surfaceGrass = false -> strictly stone-only cave island!
                islands.add(new IslandSpec(ix, iz, topY, r, thickness, false, false, Rotation.NONE, rnd));
            }
        }

        return new SitePlan(true, List.of(), islands, vines);
    }

    /**
     * Carves an irregular, jagged chaotic crater clipped to the current chunk.
     */
    private static void carveCraterClipped(WorldGenLevel level, BlockPos.MutableBlockPos mPos,
            CraterSpec c, int clipMinX, int clipMaxX, int clipMinZ, int clipMaxZ) {
        if (c.maxX < clipMinX || c.minX > clipMaxX || c.maxZ < clipMinZ || c.minZ > clipMaxZ) {
            return;
        }

        int x0 = Math.max(clipMinX, Mth.floor(c.minX));
        int x1 = Math.min(clipMaxX, Mth.ceil(c.maxX));
        int z0 = Math.max(clipMinZ, Mth.floor(c.minZ));
        int z1 = Math.min(clipMaxZ, Mth.ceil(c.maxZ));
        int minSafeY = level.getMinBuildHeight() + 8;

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState andesite = Blocks.ANDESITE.defaultBlockState();

        for (int x = x0; x <= x1; x++) {
            double dx = (x + 0.5) - c.centerX;
            for (int z = z0; z <= z1; z++) {
                double dz = (z + 0.5) - c.centerZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                double angle = Math.atan2(dz, dx);

                // Chaotic multi-harmonic jagged perimeter (NOT a smooth circle!)
                double angularWarp = 0.22 * Math.sin(angle * 3.0 + c.phase1)
                        + 0.15 * Math.cos(angle * 5.0 + c.phase2)
                        + 0.09 * Math.sin(angle * 7.0 + c.phase3);
                double fbmWarp = (FastNoise.fbm2D((float) (x * 0.075f), (float) (z * 0.075f), 2) / 0.32) * 0.24;
                double effRadius = c.radius * Mth.clamp(1.0 + angularWarp + fbmWarp, 0.52, 1.42);
                if (dist >= effRadius) {
                    continue;
                }

                double norm = dist / effRadius; // 0 at center -> 1 at jagged rim
                // Stepped, fractured profile rather than a smooth parabola
                double baseBowl = Math.pow(1.0 - norm * norm, 0.72);
                double fractureNoise = FastNoise.fbm2D((float) (x * 0.14f + 137.0f), (float) (z * 0.14f - 91.0f), 2) / 0.32;
                double jaggedTerrace = Math.floor(baseBowl * 4.5 + fractureNoise * 0.7) / 4.5;
                double localDepth = c.depth * Mth.clamp(Mth.lerp(0.45, baseBowl, jaggedTerrace) + fractureNoise * 0.18, 0.08, 1.35);

                // Deep central fissure on big craters that can punch straight down into Vile Caves!
                if (c.radius >= 16.0 && norm < 0.38 && fractureNoise > -0.15) {
                    localDepth += (1.0 - norm / 0.38) * (10.0 + fractureNoise * 8.0);
                }

                int bottomY = Math.max(minSafeY, c.surfaceY - Mth.floor(localDepth));
                int topCarveY = Math.min(level.getMaxBuildHeight() - 2, c.surfaceY + 12);

                for (int y = topCarveY; y >= bottomY; y--) {
                    mPos.set(x, y, z);
                    BlockState cur = level.getBlockState(mPos);
                    if (cur.isAir() || cur.is(Blocks.BEDROCK)) {
                        continue;
                    }
                    // Preserve water if crater somehow grazed a coastline
                    if (!cur.getFluidState().isEmpty()) {
                        continue;
                    }
                    level.setBlock(mPos, air, 2);
                }

                // Roughen exposed crater floor with fractured stone / cobblestone rubble
                mPos.set(x, bottomY - 1, z);
                BlockState floorCur = level.getBlockState(mPos);
                if (!floorCur.isAir() && !floorCur.is(Blocks.BEDROCK) && floorCur.getFluidState().isEmpty()) {
                    if (fractureNoise > 0.25) {
                        level.setBlock(mPos, cobble, 2);
                    } else if (fractureNoise < -0.25) {
                        level.setBlock(mPos, andesite, 2);
                    } else if (floorCur.is(Blocks.DIRT) || floorCur.is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(mPos, stone, 2);
                    }
                }
            }
        }
    }

    /**
     * Builds a jagged floating rocky island clipped to the current chunk.
     * Surface islands have a purple-tinted grass + dirt cap over shallow-crust stone variants
     * (stone, andesite, diorite, granite, cobblestone — never deepslate);
     * Cave islands are strictly stone-only (zero grass or dirt).
     */
    private static void placeIslandClipped(WorldGenLevel level, BlockPos.MutableBlockPos mPos,
            IslandSpec isl, int clipMinX, int clipMaxX, int clipMinZ, int clipMaxZ) {
        if (isl.maxX < clipMinX || isl.minX > clipMaxX || isl.maxZ < clipMinZ || isl.minZ > clipMaxZ) {
            return;
        }

        int x0 = Math.max(clipMinX, Mth.floor(isl.minX));
        int x1 = Math.min(clipMaxX, Mth.ceil(isl.maxX));
        int z0 = Math.max(clipMinZ, Mth.floor(isl.minZ));
        int z1 = Math.min(clipMaxZ, Mth.ceil(isl.maxZ));
        int minSafeY = level.getMinBuildHeight() + 6;
        int maxSafeY = level.getMaxBuildHeight() - 2;

        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState andesite = Blocks.ANDESITE.defaultBlockState();
        BlockState diorite = Blocks.DIORITE.defaultBlockState();
        BlockState granite = Blocks.GRANITE.defaultBlockState();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                double norm = computeIslandNormDist(isl, x, z);
                if (norm > 1.0) {
                    continue;
                }

                // Smoothed stirred-up top surface (eliminates 1x1 and 1x2 pinholes cleanly across chunk seams)
                int colTopY = Mth.clamp(computeSmoothedIslandTopY(isl, x, z), minSafeY, maxSafeY);

                // Tapered jagged stalactite-like rocky underside cone
                double coneProfile = Math.pow(Math.max(0.0, 1.0 - norm * norm), 0.78);
                double spikeNoise = FastNoise.fbm2D((float) (x * 0.13f - 210.0f), (float) (z * 0.13f + 170.0f), 2) / 0.32;
                double localThickness = Math.max(1.5, isl.thickness * coneProfile * (0.85 + spikeNoise * 0.45));

                int colBotY = Mth.clamp(isl.topY - Mth.floor(localThickness), minSafeY, colTopY);

                // Broad rock-stratum noise for natural shallow-crust stone pockets (andesite/diorite/granite/cobble)
                double rockPatchA = FastNoise.fbm2D((float) (x * 0.085f + 310.0f), (float) (z * 0.085f - 140.0f), 2) / 0.32;
                double rockPatchB = FastNoise.fbm2D((float) (x * 0.095f - 520.0f), (float) (z * 0.095f + 290.0f), 2) / 0.32;

                for (int y = colBotY; y <= colTopY; y++) {
                    mPos.set(x, y, z);
                    BlockState existing = level.getBlockState(mPos);
                    if (existing.is(Blocks.BEDROCK)) {
                        continue;
                    }

                    BlockState toPlace;
                    int depthFromTop = colTopY - y;
                    double yShift = Math.sin(y * 0.35 + x * 0.12 - z * 0.12) * 0.18;
                    double rA = rockPatchA + yShift;
                    double rB = rockPatchB - yShift;

                    if (isl.surfaceGrass) {
                        // Surface chunks lifted from upper terrain: grass -> dirt -> shallow stone variants (NO deepslate!)
                        if (depthFromTop == 0) {
                            toPlace = grass;
                        } else if (depthFromTop <= 2) {
                            toPlace = dirt;
                        } else if (rA > 0.36) {
                            toPlace = andesite;
                        } else if (rA < -0.40) {
                            toPlace = diorite;
                        } else if (rB > 0.42) {
                            toPlace = granite;
                        } else if (spikeNoise > 0.28 || rB < -0.34) {
                            toPlace = cobble;
                        } else {
                            toPlace = stone;
                        }
                    } else {
                        // Cave floating island: strictly stone-only (no grass, no dirt!)
                        if (y < 0 && (y < colBotY + 2 || spikeNoise < -0.25)) {
                            toPlace = deepslate;
                        } else if (rA > 0.35) {
                            toPlace = andesite;
                        } else if (rB > 0.44) {
                            toPlace = diorite;
                        } else if (spikeNoise > 0.28) {
                            toPlace = cobble;
                        } else {
                            toPlace = stone;
                        }
                    }

                    level.setBlock(mPos, toPlace, 2);
                }
            }
        }
    }

    private static double computeIslandNormDist(IslandSpec isl, int x, int z) {
        double dx = (x + 0.5) - isl.centerX;
        double dz = (z + 0.5) - isl.centerZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double angle = Math.atan2(dz, dx);

        double warpScale = isl.hasOutpost ? 0.12 : 0.24;
        double angWarp = warpScale * (0.65 * Math.sin(angle * 3.0 + isl.phase1)
                + 0.35 * Math.cos(angle * 5.0 + isl.phase2));
        double noiseWarp = (FastNoise.fbm2D((float) (x * 0.08f), (float) (z * 0.08f), 2) / 0.32) * warpScale;
        double effRadius = isl.radius * Mth.clamp(1.0 + angWarp + noiseWarp, 0.72, 1.28);
        return dist / Math.max(1.0, effRadius);
    }

    /**
     * Evaluates raw stirred-up island top elevation using broad geological noise (no tiny pinholes).
     */
    private static int computeRawIslandTopY(IslandSpec isl, int x, int z) {
        double norm = Math.min(1.0, computeIslandNormDist(isl, x, z));
        double topOffset;
        if (isl.hasOutpost) {
            topOffset = norm <= 0.84 ? 0.0 : -Math.pow((norm - 0.84) / 0.16, 1.5) * 3.0;
        } else {
            double dome = Math.cos(norm * Math.PI * 0.5) * (isl.surfaceGrass ? 2.4 : 3.0);
            // Gentle deterministic upheaval tilt so torn-up chunks look naturally canted
            double dx = (x + 0.5) - isl.centerX;
            double dz = (z + 0.5) - isl.centerZ;
            double tilt = (Math.cos(isl.phase1) * dx + Math.sin(isl.phase1) * dz)
                    / Math.max(5.0, isl.radius) * (isl.surfaceGrass ? 1.5 : 1.8);
            // Broad undulation (wavelength ~16 blocks) instead of high-frequency 1-block pits
            double broadCrag = (FastNoise.fbm2D((float) (x * 0.062f + 41.0f), (float) (z * 0.062f - 63.0f), 2) / 0.32)
                    * (isl.surfaceGrass ? 1.65 : 2.1);
            topOffset = dome + tilt + broadCrag;
        }
        return isl.topY + (int) Math.round(topOffset);
    }

    /**
     * Pure deterministic morphological pit-fill: preserves stirred-up ridges and mounds while
     * filling any 1x1 or 1x2 recessed holes seamlessly across chunk boundaries.
     */
    private static int computeSmoothedIslandTopY(IslandSpec isl, int x, int z) {
        if (isl.hasOutpost) {
            return computeRawIslandTopY(isl, x, z);
        }
        int c = computeRawIslandTopY(isl, x, z);
        int n = computeRawIslandTopY(isl, x, z - 1);
        int s = computeRawIslandTopY(isl, x, z + 1);
        int e = computeRawIslandTopY(isl, x + 1, z);
        int w = computeRawIslandTopY(isl, x - 1, z);

        // 1. Fill 1x1 pits (lower than at least 3 of 4 cardinal neighbors)
        int higherCount = (n > c ? 1 : 0) + (s > c ? 1 : 0) + (e > c ? 1 : 0) + (w > c ? 1 : 0);
        if (higherCount >= 3) {
            int minHigher = Integer.MAX_VALUE;
            if (n > c && n < minHigher) minHigher = n;
            if (s > c && s < minHigher) minHigher = s;
            if (e > c && e < minHigher) minHigher = e;
            if (w > c && w < minHigher) minHigher = w;
            if (minHigher != Integer.MAX_VALUE) {
                return minHigher;
            }
        }

        // 2. Fill 1x2 pockets (enclosed along one axis and lower than outer neighbors on the other axis)
        if (n > c && s > c) {
            int e2 = computeRawIslandTopY(isl, x + 2, z);
            int w2 = computeRawIslandTopY(isl, x - 2, z);
            if ((e > c || e2 > c) && (w > c || w2 > c)) {
                return Math.min(n, s);
            }
        }
        if (e > c && w > c) {
            int n2 = computeRawIslandTopY(isl, x, z - 2);
            int s2 = computeRawIslandTopY(isl, x, z + 2);
            if ((n > c || n2 > c) && (s > c || s2 > c)) {
                return Math.min(e, w);
            }
        }

        return c;
    }

    /**
     * Runtime processor ensuring floating-island Outpost Ruins NEVER destroy or replace
     * any pre-existing solid blocks (such as adjacent floating islands or terrain).
     */
    private static final StructureProcessor PRESERVE_EXISTING_BLOCKS_PROCESSOR = new StructureProcessor() {
        @Nullable
        @Override
        public StructureTemplate.StructureBlockInfo processBlock(
                net.minecraft.world.level.LevelReader level,
                BlockPos offset,
                BlockPos pos,
                StructureTemplate.StructureBlockInfo blockInfoLocal,
                StructureTemplate.StructureBlockInfo blockInfoGlobal,
                StructurePlaceSettings settings) {
            BlockState existing = level.getBlockState(blockInfoGlobal.pos());
            if (!existing.isAir() && !existing.canBeReplaced()) {
                return null;
            }
            return blockInfoGlobal;
        }

        @Override
        protected StructureProcessorType<?> getType() {
            return net.ganyusbathwater.oririmod.worldgen.ModStructureProcessors.OUTPOST_TERRAIN_BLEND.get();
        }
    };

    /**
     * Places the 4-piece {@code elderwoods_outpost_ruins} structure centered on a giant floating island,
     * clipped cleanly to {@code chunkBox} without destroying or replacing any existing blocks.
     */
    private static void placeOutpostRuinsClipped(WorldGenLevel level, StructureTemplateManager manager,
            IslandSpec island, BoundingBox chunkBox, RandomSource random) {
        Rotation rot = island.outpostRotation;

        // First calculate the rotated footprint bounding box at origin (0, 0, 0) so we can center it on the island
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        StructureTemplate[] loaded = new StructureTemplate[4];

        StructurePlaceSettings measureSettings = new StructurePlaceSettings().setRotation(rot);
        for (int i = 0; i < 4; i++) {
            Optional<StructureTemplate> opt = manager.get(OUTPOST_RUINS_TEMPLATES[i]);
            if (opt.isEmpty()) {
                return;
            }
            loaded[i] = opt.get();
            BlockPos pieceOffset = new BlockPos(OUTPOST_GRID_OFFSETS[i][0], 0, OUTPOST_GRID_OFFSETS[i][1]).rotate(rot);
            BoundingBox b = loaded[i].getBoundingBox(measureSettings, pieceOffset);
            if (b.minX() < minX) minX = b.minX();
            if (b.maxX() > maxX) maxX = b.maxX();
            if (b.minZ() < minZ) minZ = b.minZ();
            if (b.maxZ() > maxZ) maxZ = b.maxZ();
        }

        int centerOffsetX = (minX + maxX) / 2;
        int centerOffsetZ = (minZ + maxZ) / 2;
        // Outpost template y=0..1 is dirt, y=2 is outer grass apron -> place at topY - 2 so walls sit right on top
        // while BlockIgnoreProcessor.STRUCTURE_AND_AIR + PRESERVE_EXISTING_BLOCKS_PROCESSOR never carve or replace existing blocks!
        BlockPos startPos = new BlockPos(
                Mth.floor(island.centerX) - centerOffsetX,
                island.topY - 2,
                Mth.floor(island.centerZ) - centerOffsetZ);

        StructurePlaceSettings placeSettings = new StructurePlaceSettings()
                .setRotation(rot)
                .setBoundingBox(chunkBox)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
                .addProcessor(PRESERVE_EXISTING_BLOCKS_PROCESSOR)
                .addProcessor(OutpostTerrainBlendProcessor.INSTANCE);

        for (int i = 0; i < 4; i++) {
            BlockPos pieceOffset = new BlockPos(OUTPOST_GRID_OFFSETS[i][0], 0, OUTPOST_GRID_OFFSETS[i][1]).rotate(rot);
            BlockPos piecePos = startPos.offset(pieceOffset);
            BoundingBox pieceBox = loaded[i].getBoundingBox(measureSettings, piecePos);
            if (!pieceBox.intersects(chunkBox)) {
                continue;
            }
            loaded[i].placeInWorld(level, piecePos, piecePos, placeSettings, random, 2);
        }
    }

    /**
     * Scatters lush purple-tinted short grass & tall grass on both floating surface islands
     * and the Vile Plains ground beneath (works even directly under overhanging islands and vines).
     */
    private static void populateGrassClipped(WorldGenLevel level, BlockPos.MutableBlockPos mPos,
            ElderwoodsBiomeSource ebs, List<SitePlan> activeSites,
            int clipMinX, int clipMaxX, int clipMinZ, int clipMaxZ, long worldSeed) {
        BlockState shortGrass = Blocks.SHORT_GRASS.defaultBlockState();
        BlockState tallGrassLower = Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState tallGrassUpper = Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
        int maxY = level.getMaxBuildHeight() - 2;

        // 1. Floating surface islands grass
        for (SitePlan site : activeSites) {
            for (IslandSpec isl : site.islands) {
                if (!isl.surfaceGrass) continue;
                if (isl.maxX < clipMinX || isl.minX > clipMaxX || isl.maxZ < clipMinZ || isl.minZ > clipMaxZ) {
                    continue;
                }
                int x0 = Math.max(clipMinX, Mth.floor(isl.minX));
                int x1 = Math.min(clipMaxX, Mth.ceil(isl.maxX));
                int z0 = Math.max(clipMinZ, Mth.floor(isl.minZ));
                int z1 = Math.min(clipMaxZ, Mth.ceil(isl.maxZ));

                for (int x = x0; x <= x1; x++) {
                    for (int z = z0; z <= z1; z++) {
                        if (computeIslandNormDist(isl, x, z) > 1.0) continue;
                        int topY = computeSmoothedIslandTopY(isl, x, z);
                        if (topY + 1 >= maxY) continue;
                        tryPlaceGrassBlade(level, mPos, x, topY, z, worldSeed, shortGrass, tallGrassLower, tallGrassUpper);
                    }
                }
            }
        }

        // 2. Vile Plains ground grass (works even beneath overhanging floating islands & vines)
        for (int x = clipMinX; x <= clipMaxX; x++) {
            for (int z = clipMinZ; z <= clipMaxZ; z++) {
                if (!ebs.getSurfaceBiome(x, z).is(ElderwoodsBiomeSource.VILE_PLAINS_KEY)) {
                    continue;
                }
                int surfaceY = ebs.computeSurfaceHeight(x, z, null);
                if (surfaceY < 62 || surfaceY + 1 >= maxY) continue;
                tryPlaceGrassBlade(level, mPos, x, surfaceY, z, worldSeed ^ 0x47524153L, shortGrass, tallGrassLower, tallGrassUpper);
            }
        }
    }

    private static void tryPlaceGrassBlade(WorldGenLevel level, BlockPos.MutableBlockPos mPos,
            int x, int groundY, int z, long seed,
            BlockState shortGrass, BlockState tallGrassLower, BlockState tallGrassUpper) {
        mPos.set(x, groundY, z);
        if (!level.getBlockState(mPos).is(Blocks.GRASS_BLOCK)) {
            return;
        }
        mPos.set(x, groundY + 1, z);
        if (!level.getBlockState(mPos).isAir()) {
            return;
        }

        // Clumpy meadow density via low-frequency FBM + fast coordinate hash
        double clump = (FastNoise.fbm2D((float) (x * 0.055f), (float) (z * 0.055f), 2) / 0.32) * 0.5 + 0.5;
        long h = seed ^ (x * 73856093L) ^ (groundY * 19349663L) ^ (z * 83492791L);
        h = (h ^ (h >>> 30)) * 0xbf58476d1ce4e5b9L;
        h ^= (h >>> 27);
        double roll = (h & 0xFFFFL) / 65536.0;

        double grassChance = 0.22 + clump * 0.34; // ~22% in sparse patches up to ~56% in lush meadow patches
        if (roll >= grassChance) {
            return;
        }

        // ~18% of placed grass blades are tall grass when 2 blocks of air are clear
        if (roll < grassChance * 0.18) {
            mPos.set(x, groundY + 2, z);
            if (level.getBlockState(mPos).isAir()) {
                mPos.set(x, groundY + 1, z);
                level.setBlock(mPos, tallGrassLower, 2);
                mPos.set(x, groundY + 2, z);
                level.setBlock(mPos, tallGrassUpper, 2);
                return;
            }
        }

        mPos.set(x, groundY + 1, z);
        level.setBlock(mPos, shortGrass, 2);
    }
}
