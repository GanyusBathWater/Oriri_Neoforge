package net.ganyusbathwater.oririmod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsBiomeSource;
import net.ganyusbathwater.oririmod.worldgen.ModStructureProcessorLists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GridStructure extends Structure {

    public static final MapCodec<GridStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("basename").forGetter(s -> s.basename),
            Codec.INT.fieldOf("width_chunks").forGetter(s -> s.widthChunks),
            Codec.INT.fieldOf("depth_chunks").forGetter(s -> s.depthChunks),
            Codec.INT.fieldOf("grid_size").forGetter(s -> s.gridSize),
            HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight)
        ).apply(instance, GridStructure::new)
    );

    private final ResourceLocation basename;
    private final int widthChunks;
    private final int depthChunks;
    private final int gridSize;
    private final HeightProvider startHeight;

    public GridStructure(StructureSettings settings, ResourceLocation basename, int widthChunks, int depthChunks, int gridSize, HeightProvider startHeight) {
        super(settings);
        this.basename = basename;
        this.widthChunks = widthChunks;
        this.depthChunks = depthChunks;
        this.gridSize = gridSize;
        this.startHeight = startHeight;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMinBlockX();
        int z = chunkPos.getMinBlockZ();

        Rotation rotation = Rotation.getRandom(context.random());
        StructureTemplateManager manager = context.structureTemplateManager();

        // Compute the true rotated XZ bounding box of the grid pieces so non-square edge pieces
        // (e.g. 59x59 outposts stored across 48/11 templates) are sampled accurately.
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        BlockPos originFlat = new BlockPos(x, 0, z);
        for (int gridX = 0; gridX < this.widthChunks; gridX++) {
            for (int gridZ = 0; gridZ < this.depthChunks; gridZ++) {
                int offsetX = gridX * this.gridSize;
                int offsetZ = gridZ * this.gridSize;
                ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath(
                        this.basename.getNamespace(), this.basename.getPath() + "_" + offsetX + "_" + offsetZ);
                StructurePoolElement element = SinglePoolElement.legacy(templateId.toString())
                        .apply(StructureTemplatePool.Projection.RIGID);
                BlockPos piecePos = originFlat.offset(new BlockPos(offsetX, 0, offsetZ).rotate(rotation));
                BoundingBox box = element.getBoundingBox(manager, piecePos, rotation);
                if (box.minX() < minX) minX = box.minX();
                if (box.maxX() > maxX) maxX = box.maxX();
                if (box.minZ() < minZ) minZ = box.minZ();
                if (box.maxZ() > maxZ) maxZ = box.maxZ();
            }
        }

        int spanX = Math.max(1, maxX - minX);
        int spanZ = Math.max(1, maxZ - minZ);

        // Sample a 5x5 grid across the actual rotated footprint of the structure
        List<Integer> heights = new ArrayList<>(25);
        int sampleGridPoints = 5;
        for (int i = 0; i < sampleGridPoints; i++) {
            for (int j = 0; j < sampleGridPoints; j++) {
                int sampleX = minX + (i * spanX) / (sampleGridPoints - 1);
                int sampleZ = minZ + (j * spanZ) / (sampleGridPoints - 1);

                int sampleY = context.chunkGenerator().getBaseHeight(
                        sampleX, sampleZ, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()
                );

                // Prevent structures from encroaching on oceans, lakes, or river channels
                if (sampleY <= 72) {
                    return Optional.empty();
                }

                // Reject if structure footprint crosses outside allowed biomes or touches an Elder Valley river channel
                Holder<Biome> biome = context.biomeSource().getNoiseBiome(
                        sampleX >> 2, sampleY >> 2, sampleZ >> 2, context.randomState().sampler()
                );
                if (!this.biomes().contains(biome) || biome.is(ElderwoodsBiomeSource.ELDER_VALLEY_KEY)) {
                    return Optional.empty();
                }
                if (context.biomeSource() instanceof ElderwoodsBiomeSource ebs
                        && ebs.isInRiverChannel(sampleX, sampleZ)) {
                    return Optional.empty();
                }

                heights.add(sampleY);
            }
        }

        int minY = Collections.min(heights);
        int maxY = Collections.max(heights);
        int delta = maxY - minY;

        // Reject sheer cliff faces, while allowing Elder Highlands slopes supported by a retaining wall
        if (delta > 28) {
            return Optional.empty();
        }

        Collections.sort(heights);
        // Flat areas keep median placement; hillside slopes anchor near upper crest (~70th pct)
        // so buildings sit clear uphill while the downhill side rests on a terraced retaining wall.
        int anchorY = delta <= 4
                ? heights.get(heights.size() / 2)
                : heights.get((heights.size() * 7) / 10);

        int y = anchorY + this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        BlockPos startPos = new BlockPos(x, y, z);

        return Optional.of(new GenerationStub(startPos, builder -> this.generatePieces(builder, context, startPos, rotation)));
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator,
                           RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, PiecesContainer pieces) {
        String path = this.basename.getPath();
        boolean isScarlet = path.contains("scarlet");
        boolean isRuins = path.contains("ruins");
        applyRetainingWallAndFoundation(level, chunkGenerator, chunkBox, pieces, isScarlet, isRuins, true);
    }

    /**
     * Builds perimeter retaining walls, corner wing walls, under-structure stone/dirt foundation
     * fill, and grass apron leveling across a placed structure's bounding box.
     */
    public static void applyRetainingWallAndFoundation(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            BoundingBox chunkBox,
            PiecesContainer pieces,
            boolean isScarlet,
            boolean isRuins,
            boolean hasRaisedInnerTerrace) {
        List<StructurePiece> pieceList = pieces.pieces();
        if (pieceList.isEmpty()) {
            return;
        }

        // Derive the exact global structure square & base elevation from ALL pieces in the start so
        // retaining walls, corner infills, and wing walls stay 100% seamless across chunk seams.
        int structMinX = Integer.MAX_VALUE;
        int structMaxX = Integer.MIN_VALUE;
        int structMinZ = Integer.MAX_VALUE;
        int structMaxZ = Integer.MIN_VALUE;
        int structMinY = Integer.MAX_VALUE;
        for (int p = 0; p < pieceList.size(); p++) {
            BoundingBox b = pieceList.get(p).getBoundingBox();
            if (b.minX() < structMinX) structMinX = b.minX();
            if (b.maxX() > structMaxX) structMaxX = b.maxX();
            if (b.minZ() < structMinZ) structMinZ = b.minZ();
            if (b.maxZ() > structMaxZ) structMaxZ = b.maxZ();
            if (b.minY() < structMinY) structMinY = b.minY();
        }

        int x0 = Math.max(chunkBox.minX(), structMinX);
        int x1 = Math.min(chunkBox.maxX(), structMaxX);
        int z0 = Math.max(chunkBox.minZ(), structMinZ);
        int z1 = Math.min(chunkBox.maxZ(), structMaxZ);
        if (x0 > x1 || z0 > z1) {
            return;
        }

        int worldMinY = level.getMinBuildHeight();
        int worldMaxY = level.getMaxBuildHeight() - 1;
        // Outposts build y=0..1 dirt, y=2 outer grass apron, and y=3 inner grass terrace/courtyard walls.
        int lawnY = Math.min(worldMaxY, structMinY + 2);
        int wallTopY = hasRaisedInnerTerrace ? Math.min(worldMaxY, structMinY + 3) : lawnY;

        int width = structMaxX - structMinX + 1;
        int depth = structMaxZ - structMinZ + 1;
        if (width < 2 || depth < 2) {
            return;
        }

        ElderwoodsBiomeSource biomeSource = chunkGenerator.getBiomeSource() instanceof ElderwoodsBiomeSource ebs
                ? ebs : null;
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

        PerimeterWallData wallData = buildPerimeterWallGrid(
                biomeSource, level, mPos, chunkBox, structMinX, structMinZ, width, depth,
                structMinY, wallTopY, worldMinY);
        int[][] wallCapGrid = wallData.wallCapGrid();
        int[][] outsideYGrid = wallData.outsideYGrid();

        boolean hasAnyWall = false;
        for (int lx = 0; lx < width && !hasAnyWall; lx++) {
            for (int lz = 0; lz < depth; lz++) {
                if (wallCapGrid[lx][lz] != Integer.MIN_VALUE) {
                    hasAnyWall = true;
                    break;
                }
            }
        }

        int[][] naturalYGrid = hasAnyWall
                ? buildCornerSurfaceGrid(biomeSource, level, mPos, chunkBox, structMinX, structMinZ,
                        width, depth, wallTopY, worldMinY, lawnY)
                : null;
        boolean[][] cornerActive = hasAnyWall
                ? buildCornerActiveGrid(wallCapGrid, naturalYGrid, width, depth, lawnY)
                : null;

        for (int wx = x0; wx <= x1; wx++) {
            int lx = wx - structMinX;
            for (int wz = z0; wz <= z1; wz++) {
                int lz = wz - structMinZ;
                int subGroundY = findSolidFoundationBelow(level, mPos, wx, structMinY - 1, wz, worldMinY, 44);

                int perimCapY = wallCapGrid[lx][lz];
                if (perimCapY != Integer.MIN_VALUE) {
                    int outY = outsideYGrid[lx][lz];
                    int wallFootY = (outY != Integer.MIN_VALUE) ? Math.min(subGroundY, outY) : subGroundY;
                    placeRetainingWallColumn(level, mPos, wx, wz, structMinY, perimCapY, wallFootY,
                            worldMinY, isScarlet, isRuins, true);
                    continue;
                }

                boolean hasUpperBuilding = hasStructureBuildingAt(level, mPos, wx, wallTopY, wz);
                boolean hasLawnBuilding = hasStructureBuildingAt(level, mPos, wx, lawnY, wz);
                boolean hasLowerBuilding = hasStructureBuildingAt(level, mPos, wx, structMinY, wz)
                        || hasStructureBuildingAt(level, mPos, wx, Math.min(worldMaxY, structMinY + 1), wz);
                boolean hasAnyBuilding = hasUpperBuilding || hasLawnBuilding || hasLowerBuilding;

                int apronTargetY = hasAnyWall
                        ? getWallApronTargetY(wallCapGrid, width, depth, lx, lz, lawnY, wallTopY)
                        : Integer.MIN_VALUE;
                boolean inCorner = cornerActive != null && cornerActive[lx][lz];
                boolean behindFullWall = apronTargetY >= wallTopY;
                boolean behindWall = apronTargetY >= lawnY || inCorner;

                int fillEndY = Math.max(worldMinY + 2, subGroundY);

                if (!hasAnyBuilding && behindWall) {
                    int targetSurfY = behindFullWall ? wallTopY : Math.max(lawnY, apronTargetY);
                    mPos.set(wx, targetSurfY + 1, wz);
                    BlockState aboveTarget = level.getBlockState(mPos);
                    boolean buriedBelowTerrain = !aboveTarget.isAir()
                            && !aboveTarget.liquid()
                            && !aboveTarget.canBeReplaced()
                            && !aboveTarget.is(BlockTags.LEAVES)
                            && !aboveTarget.is(BlockTags.LOGS)
                            && !isNaturalGrassOrFoliage(aboveTarget);

                    // Unconditionally replace natural grass/foliage and fill behind the visible wall with dirt.
                    for (int y = structMinY; y < targetSurfY; y++) {
                        mPos.set(wx, y, wz);
                        BlockState cur = level.getBlockState(mPos);
                        if (isReplaceableForStructureWall(cur) || isNaturalGrassOrFoliage(cur)) {
                            level.setBlock(mPos, Blocks.DIRT.defaultBlockState(), 2);
                        }
                    }

                    mPos.set(wx, targetSurfY, wz);
                    BlockState atTarget = level.getBlockState(mPos);
                    if (isReplaceableForStructureWall(atTarget) || isNaturalGrassOrFoliage(atTarget)) {
                        level.setBlock(mPos,
                                buriedBelowTerrain ? Blocks.DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState(),
                                2);
                        if (aboveTarget.getBlock() instanceof DoublePlantBlock
                                && aboveTarget.hasProperty(DoublePlantBlock.HALF)
                                && aboveTarget.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
                            mPos.set(wx, targetSurfY + 1, wz);
                            level.setBlock(mPos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }

                    for (int y = structMinY - 1; y >= fillEndY; y--) {
                        mPos.set(wx, y, wz);
                        BlockState cur = level.getBlockState(mPos);
                        if (isReplaceableForStructureWall(cur) || isNaturalGrassOrFoliage(cur)) {
                            level.setBlock(mPos, Blocks.DIRT.defaultBlockState(), 2);
                        }
                    }
                } else {
                    boolean hasSurfacePlatform = hasAnyBuilding
                            || hasSolidSurfaceAt(level, mPos, wx, wallTopY, wz)
                            || hasSolidSurfaceAt(level, mPos, wx, lawnY, wz);
                    if (!hasSurfacePlatform) {
                        continue;
                    }

                    for (int y = structMinY - 1; y >= fillEndY; y--) {
                        mPos.set(wx, y, wz);
                        BlockState cur = level.getBlockState(mPos);
                        if (isReplaceableForStructureWall(cur) || isNaturalGrassOrFoliage(cur)) {
                            level.setBlock(mPos, Blocks.DIRT.defaultBlockState(), 2);
                        }
                    }
                }

                // Clean buried grass blocks and trapped plants inside the structure footprint unconditionally.
                int cleanTopY = Math.min(worldMaxY - 1, structMinY + 10);
                for (int y = fillEndY; y <= cleanTopY; y++) {
                    mPos.set(wx, y, wz);
                    BlockState cur = level.getBlockState(mPos);
                    if (cur.is(Blocks.GRASS_BLOCK)) {
                        mPos.set(wx, y + 1, wz);
                        BlockState above = level.getBlockState(mPos);
                        boolean buried = !above.isAir()
                                && !above.liquid()
                                && !above.is(BlockTags.LEAVES)
                                && !isNaturalGrassOrFoliage(above)
                                && (!above.canBeReplaced() || above.isCollisionShapeFullBlock(level, mPos));
                        if (buried) {
                            mPos.set(wx, y, wz);
                            level.setBlock(mPos, Blocks.DIRT.defaultBlockState(), 2);
                        }
                    } else if (cur.is(Blocks.SHORT_GRASS) || cur.is(Blocks.TALL_GRASS)
                            || cur.is(Blocks.FERN) || cur.is(Blocks.LARGE_FERN) || cur.is(BlockTags.FLOWERS)) {
                        mPos.set(wx, y - 1, wz);
                        if (!level.getBlockState(mPos).is(BlockTags.DIRT)) {
                            mPos.set(wx, y, wz);
                            level.setBlock(mPos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }

        if (hasAnyWall) {
            fillSingleBlockApronHoles(level, mPos, wallCapGrid, structMinX, structMinZ, width, depth,
                    x0, x1, z0, z1, lawnY, wallTopY);
        }
    }

    private static final int CORNER_EXTENT = 15;

    private record PerimeterWallData(int[][] wallCapGrid, int[][] outsideYGrid) {
    }

    private static PerimeterWallData buildPerimeterWallGrid(ElderwoodsBiomeSource biomeSource, WorldGenLevel level,
                                                            BlockPos.MutableBlockPos mPos, BoundingBox chunkBox,
                                                            int minX, int minZ, int w, int d,
                                                            int structMinY, int wallTopY, int worldMinY) {
        int perimLen = 2 * (w - 1) + 2 * (d - 1);
        int[] px = new int[perimLen];
        int[] pz = new int[perimLen];
        int[] outsideY = new int[perimLen];
        boolean[] wall = new boolean[perimLen];

        int idx = 0;
        // Segment 0: North edge (lz = 0, lx = 0 .. w - 2)
        for (int lx = 0; lx < w - 1; lx++, idx++) {
            px[idx] = lx;
            pz[idx] = 0;
            int wx = minX + lx;
            int hN = sampleSurfaceY(biomeSource, level, mPos, chunkBox, wx, minZ - 1, wallTopY, worldMinY);
            if (lx == 0) {
                int hW = sampleSurfaceY(biomeSource, level, mPos, chunkBox, wx - 1, minZ, wallTopY, worldMinY);
                int hD = sampleSurfaceY(biomeSource, level, mPos, chunkBox, wx - 1, minZ - 1, wallTopY, worldMinY);
                outsideY[idx] = Math.min(hN, Math.min(hW, hD));
            } else {
                outsideY[idx] = hN;
            }
        }
        // Segment 1: East edge (lx = w - 1, lz = 0 .. d - 2)
        for (int lz = 0; lz < d - 1; lz++, idx++) {
            px[idx] = w - 1;
            pz[idx] = lz;
            int wz = minZ + lz;
            int hE = sampleSurfaceY(biomeSource, level, mPos, chunkBox, minX + w, wz, wallTopY, worldMinY);
            if (lz == 0) {
                int hN = sampleSurfaceY(biomeSource, level, mPos, chunkBox, minX + w - 1, wz - 1, wallTopY, worldMinY);
                int hD = sampleSurfaceY(biomeSource, level, mPos, chunkBox, minX + w, wz - 1, wallTopY, worldMinY);
                outsideY[idx] = Math.min(hE, Math.min(hN, hD));
            } else {
                outsideY[idx] = hE;
            }
        }
        // Segment 2: South edge (lz = d - 1, lx = w - 1 .. 1)
        for (int k = 0; k < w - 1; k++, idx++) {
            int lx = w - 1 - k;
            px[idx] = lx;
            pz[idx] = d - 1;
            int wx = minX + lx;
            int hS = sampleSurfaceY(biomeSource, level, mPos, chunkBox, wx, minZ + d, wallTopY, worldMinY);
            if (k == 0) {
                int hE = sampleSurfaceY(biomeSource, level, mPos, chunkBox, wx + 1, minZ + d - 1, wallTopY, worldMinY);
                int hD = sampleSurfaceY(biomeSource, level, mPos, chunkBox, wx + 1, minZ + d, wallTopY, worldMinY);
                outsideY[idx] = Math.min(hS, Math.min(hE, hD));
            } else {
                outsideY[idx] = hS;
            }
        }
        // Segment 3: West edge (lx = 0, lz = d - 1 .. 1)
        for (int k = 0; k < d - 1; k++, idx++) {
            int lz = d - 1 - k;
            px[idx] = 0;
            pz[idx] = lz;
            int wz = minZ + lz;
            int hW = sampleSurfaceY(biomeSource, level, mPos, chunkBox, minX - 1, wz, wallTopY, worldMinY);
            if (k == 0) {
                int hS = sampleSurfaceY(biomeSource, level, mPos, chunkBox, minX, wz + 1, wallTopY, worldMinY);
                int hD = sampleSurfaceY(biomeSource, level, mPos, chunkBox, minX - 1, wz + 1, wallTopY, worldMinY);
                outsideY[idx] = Math.min(hW, Math.min(hS, hD));
            } else {
                outsideY[idx] = hW;
            }
        }

        // Only place retaining walls where terrain drops far enough below the base that leaving
        // it unwalled looks high/floating; flat or high sides keep natural ground transitions.
        int hardTriggerMaxY = structMinY - 2;
        int softTriggerMaxY = structMinY - 1;
        int bridgeMaxY = structMinY;

        int highAnchor = -1;
        for (int i = 0; i < perimLen; i++) {
            if (outsideY[i] <= hardTriggerMaxY) {
                wall[i] = true;
            }
            if (outsideY[i] > softTriggerMaxY && highAnchor < 0) {
                highAnchor = i;
            }
        }

        if (highAnchor < 0) {
            for (int i = 0; i < perimLen; i++) {
                wall[i] = true;
            }
        } else {
            // Trigger any sustained (>= 2 blocks) drop below lawnY so no gap opens beside backfill
            int runStart = -1;
            int runLen = 0;
            for (int step = 1; step <= perimLen; step++) {
                int cur = (highAnchor + step) % perimLen;
                if (outsideY[cur] <= softTriggerMaxY) {
                    if (runLen == 0) runStart = step;
                    runLen++;
                } else {
                    if (runLen >= 2) {
                        for (int s = runStart; s < runStart + runLen; s++) {
                            wall[(highAnchor + s) % perimLen] = true;
                        }
                    }
                    runLen = 0;
                }
            }

            // Expand active walls seamlessly through connected slope cells (<= bridgeMaxY)
            for (int pass = 0; pass < 2; pass++) {
                for (int step = 0; step < perimLen * 2; step++) {
                    int a = step % perimLen;
                    int b = (step + 1) % perimLen;
                    if (wall[a] && outsideY[b] <= bridgeMaxY) {
                        wall[b] = true;
                    }
                }
                for (int step = perimLen * 2 - 1; step >= 0; step--) {
                    int a = (step + 1) % perimLen;
                    int b = step % perimLen;
                    if (wall[a] && outsideY[b] <= bridgeMaxY) {
                        wall[b] = true;
                    }
                }
            }

            // Bridge narrow 1-4 block rock/grass humps (outsideY <= wallTopY) between two wall segments
            for (int i = 0; i < perimLen; i++) {
                if (!wall[i]) continue;
                int bridgeEnd = -1;
                for (int span = 1; span <= 4; span++) {
                    int far = (i + span + 1) % perimLen;
                    boolean lowSpan = true;
                    for (int k = 1; k <= span; k++) {
                        if (outsideY[(i + k) % perimLen] > wallTopY) {
                            lowSpan = false;
                            break;
                        }
                    }
                    if (!lowSpan) break;
                    if (wall[far]) {
                        bridgeEnd = span;
                        break;
                    }
                }
                if (bridgeEnd > 0) {
                    for (int k = 1; k <= bridgeEnd; k++) {
                        wall[(i + k) % perimLen] = true;
                    }
                }
            }
        }

        int[] wallCap = new int[perimLen];
        Arrays.fill(wallCap, Integer.MIN_VALUE);
        for (int i = 0; i < perimLen; i++) {
            if (wall[i]) {
                wallCap[i] = wallTopY;
            }
        }
        if (highAnchor >= 0) {
            // Gradually descend the retaining wall 1 block per step past each active wall end
            // until the lowered cap no longer sits above the outside ground level.
            for (int i = 0; i < perimLen; i++) {
                if (!wall[i]) continue;
                for (int dir = -1; dir <= 1; dir += 2) {
                    if (wall[Math.floorMod(i + dir, perimLen)]) continue;
                    for (int step = 1; step <= 16; step++) {
                        int cur = Math.floorMod(i + dir * step, perimLen);
                        if (wall[cur]) break;
                        int capY = wallTopY - step;
                        if (capY <= outsideY[cur]) break;
                        if (capY > wallCap[cur]) {
                            wallCap[cur] = capY;
                        }
                    }
                }
            }
        }

        int[][] wallCapGrid = new int[w][d];
        int[][] outsideYGrid = new int[w][d];
        for (int x = 0; x < w; x++) {
            Arrays.fill(wallCapGrid[x], Integer.MIN_VALUE);
            Arrays.fill(outsideYGrid[x], Integer.MIN_VALUE);
        }
        for (int i = 0; i < perimLen; i++) {
            wallCapGrid[px[i]][pz[i]] = wallCap[i];
            outsideYGrid[px[i]][pz[i]] = outsideY[i];
        }
        return new PerimeterWallData(wallCapGrid, outsideYGrid);
    }

    private static int[][] buildCornerSurfaceGrid(ElderwoodsBiomeSource biomeSource, WorldGenLevel level,
                                                  BlockPos.MutableBlockPos mPos, BoundingBox chunkBox,
                                                  int minX, int minZ, int w, int d,
                                                  int wallTopY, int worldMinY, int defaultY) {
        int[][] surfaceY = new int[w][d];
        int extentX = Math.min(CORNER_EXTENT + 2, w);
        int extentZ = Math.min(CORNER_EXTENT + 2, d);
        for (int lx = 0; lx < w; lx++) {
            boolean nearX = (lx < extentX) || (lx >= w - extentX);
            for (int lz = 0; lz < d; lz++) {
                boolean nearZ = (lz < extentZ) || (lz >= d - extentZ);
                if (nearX && nearZ) {
                    surfaceY[lx][lz] = sampleSurfaceY(
                            biomeSource, level, mPos, chunkBox, minX + lx, minZ + lz, wallTopY, worldMinY);
                } else {
                    surfaceY[lx][lz] = defaultY;
                }
            }
        }
        return surfaceY;
    }

    private static boolean[][] buildCornerActiveGrid(int[][] wallCapGrid, int[][] naturalYGrid,
                                                       int w, int d, int lawnY) {
        boolean[][] active = new boolean[w][d];
        int extX = Math.min(CORNER_EXTENT, w - 1);
        int extZ = Math.min(CORNER_EXTENT, d - 1);
        for (int lx = 1; lx < w - 1; lx++) {
            boolean nearW = lx < extX;
            boolean nearE = lx >= w - extX;
            if (!nearW && !nearE) continue;

            for (int lz = 1; lz < d - 1; lz++) {
                boolean nearN = lz < extZ;
                boolean nearS = lz >= d - extZ;
                if (!nearN && !nearS) continue;

                boolean behindWall;
                if (nearW && nearN) {
                    behindWall = wallCapGrid[0][lz] >= lawnY || wallCapGrid[lx][0] >= lawnY;
                } else if (nearE && nearN) {
                    behindWall = wallCapGrid[w - 1][lz] >= lawnY || wallCapGrid[lx][0] >= lawnY;
                } else if (nearW) {
                    behindWall = wallCapGrid[0][lz] >= lawnY || wallCapGrid[lx][d - 1] >= lawnY;
                } else {
                    behindWall = wallCapGrid[w - 1][lz] >= lawnY || wallCapGrid[lx][d - 1] >= lawnY;
                }

                if (behindWall && naturalYGrid[lx][lz] < lawnY) {
                    active[lx][lz] = true;
                }
            }
        }
        return active;
    }

    private static int getWallApronTargetY(int[][] wallCapGrid, int w, int d, int lx, int lz, int lawnY, int wallTopY) {
        int distW = lx;
        int distE = w - 1 - lx;
        int distN = lz;
        int distS = d - 1 - lz;
        int bestCap = Integer.MIN_VALUE;
        if (distW <= CORNER_EXTENT && wallCapGrid[0][lz] >= lawnY) {
            bestCap = Math.max(bestCap, wallCapGrid[0][lz]);
        }
        if (distE <= CORNER_EXTENT && wallCapGrid[w - 1][lz] >= lawnY) {
            bestCap = Math.max(bestCap, wallCapGrid[w - 1][lz]);
        }
        if (distN <= CORNER_EXTENT && wallCapGrid[lx][0] >= lawnY) {
            bestCap = Math.max(bestCap, wallCapGrid[lx][0]);
        }
        if (distS <= CORNER_EXTENT && wallCapGrid[lx][d - 1] >= lawnY) {
            bestCap = Math.max(bestCap, wallCapGrid[lx][d - 1]);
        }
        return Math.min(wallTopY, bestCap);
    }

    private static void fillSingleBlockApronHoles(WorldGenLevel level, BlockPos.MutableBlockPos mPos,
                                                  int[][] wallCapGrid, int structMinX, int structMinZ,
                                                  int w, int d, int x0, int x1, int z0, int z1,
                                                  int lawnY, int wallTopY) {
        for (int wx = x0; wx <= x1; wx++) {
            int lx = wx - structMinX;
            for (int wz = z0; wz <= z1; wz++) {
                int lz = wz - structMinZ;
                if (lx <= 0 || lx >= w - 1 || lz <= 0 || lz >= d - 1) continue;
                int targetY = getWallApronTargetY(wallCapGrid, w, d, lx, lz, lawnY, wallTopY);
                if (targetY < wallTopY) continue;

                mPos.set(wx, wallTopY, wz);
                BlockState atTop = level.getBlockState(mPos);
                if (!atTop.isAir() && !atTop.canBeReplaced() && !atTop.is(BlockTags.FLOWERS)) continue;

                mPos.set(wx, wallTopY + 1, wz);
                BlockState above = level.getBlockState(mPos);
                if (!above.isAir() && !above.canBeReplaced()) continue;

                mPos.set(wx, wallTopY - 1, wz);
                BlockState below = level.getBlockState(mPos);
                boolean safeBase = below.is(BlockTags.DIRT) || below.is(Blocks.STONE_BRICKS)
                        || below.is(Blocks.MOSSY_STONE_BRICKS) || below.is(Blocks.CRACKED_STONE_BRICKS)
                        || below.is(ModBlocks.SCARLET_STONE_BRICKS.get())
                        || below.is(ModBlocks.MOSSY_SCARLET_STONE_BRICKS.get())
                        || below.is(ModBlocks.CRACKED_SCARLET_STONE_BRICKS.get());
                if (!safeBase) continue;

                int solidHigherNeighbors = 0;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    mPos.set(wx + dir.getStepX(), wallTopY, wz + dir.getStepZ());
                    BlockState nSt = level.getBlockState(mPos);
                    if (nSt.is(BlockTags.DIRT) || nSt.is(Blocks.STONE_BRICKS)
                            || nSt.is(Blocks.MOSSY_STONE_BRICKS) || nSt.is(Blocks.CRACKED_STONE_BRICKS)
                            || nSt.is(ModBlocks.SCARLET_STONE_BRICKS.get())
                            || nSt.is(ModBlocks.MOSSY_SCARLET_STONE_BRICKS.get())
                            || nSt.is(ModBlocks.CRACKED_SCARLET_STONE_BRICKS.get())) {
                        solidHigherNeighbors++;
                    }
                }
                if (solidHigherNeighbors >= 3) {
                    if (below.is(Blocks.GRASS_BLOCK)) {
                        mPos.set(wx, wallTopY - 1, wz);
                        level.setBlock(mPos, Blocks.DIRT.defaultBlockState(), 2);
                    }
                    mPos.set(wx, wallTopY, wz);
                    level.setBlock(mPos, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                }
            }
        }
    }

    private static int sampleSurfaceY(ElderwoodsBiomeSource biomeSource, WorldGenLevel level,
                                      BlockPos.MutableBlockPos mPos, BoundingBox chunkBox,
                                      int wx, int wz, int wallTopY, int worldMinY) {
        int theoreticalY = (biomeSource != null)
                ? biomeSource.computeSurfaceHeight(wx, wz, null)
                : Integer.MAX_VALUE;
        boolean inSafeHalo = wx >= chunkBox.minX() - 4 && wx <= chunkBox.maxX() + 4
                && wz >= chunkBox.minZ() - 4 && wz <= chunkBox.maxZ() + 4;
        if (inSafeHalo) {
            int actualY = findSolidTerrainBelow(level, mPos, wx, wallTopY + 12, wz, worldMinY, 56);
            return (theoreticalY == Integer.MAX_VALUE) ? actualY : Math.min(theoreticalY, actualY);
        }
        if (theoreticalY != Integer.MAX_VALUE) {
            return theoreticalY;
        }
        int cx = Math.max(chunkBox.minX() - 1, Math.min(chunkBox.maxX() + 1, wx));
        int cz = Math.max(chunkBox.minZ() - 1, Math.min(chunkBox.maxZ() + 1, wz));
        return findSolidTerrainBelow(level, mPos, cx, wallTopY, cz, worldMinY, 44);
    }

    private static boolean hasSolidSurfaceAt(WorldGenLevel level, BlockPos.MutableBlockPos mPos,
                                             int wx, int y, int wz) {
        mPos.set(wx, y, wz);
        BlockState st = level.getBlockState(mPos);
        return !st.isAir() && !st.liquid() && !st.canBeReplaced() && !st.is(BlockTags.LEAVES);
    }

    private static boolean hasStructureBuildingAt(WorldGenLevel level, BlockPos.MutableBlockPos mPos,
                                                  int wx, int y, int wz) {
        mPos.set(wx, y, wz);
        return isStructureBuildingBlock(level.getBlockState(mPos));
    }

    private static boolean isStructureBuildingBlock(BlockState state) {
        if (state.isAir()
                || state.liquid()
                || state.canBeReplaced()
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)) {
            return false;
        }
        return !state.is(BlockTags.DIRT)
                && !state.is(BlockTags.SAND)
                && !state.is(Blocks.SANDSTONE)
                && !state.is(Blocks.GRAVEL)
                && !state.is(Blocks.CLAY)
                && !state.is(Blocks.SNOW)
                && !state.is(Blocks.SNOW_BLOCK)
                && !state.is(Blocks.STONE)
                && !state.is(Blocks.ANDESITE)
                && !state.is(Blocks.GRANITE)
                && !state.is(Blocks.DIORITE)
                && !state.is(Blocks.DEEPSLATE)
                && !state.is(ModBlocks.SCARLET_STONE.get())
                && !state.is(ModBlocks.SCARLET_DEEPSLATE.get())
                && !state.is(ModBlocks.SOL_SAND.get())
                && !state.is(ModBlocks.SOL_SANDSTONE.get())
                && !state.is(ModBlocks.ABYSS_SAND.get());
    }

    private static boolean isNaturalGrassOrFoliage(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(BlockTags.FLOWERS);
    }

    private static boolean isReplaceableForStructureWall(BlockState cur) {
        return cur.isAir()
                || cur.liquid()
                || cur.canBeReplaced()
                || cur.is(BlockTags.FLOWERS)
                || cur.is(BlockTags.LEAVES)
                || cur.is(BlockTags.LOGS)
                || cur.is(BlockTags.DIRT)
                || cur.is(BlockTags.SAND)
                || cur.is(Blocks.SANDSTONE)
                || cur.is(Blocks.GRAVEL)
                || cur.is(Blocks.CLAY)
                || cur.is(Blocks.SNOW)
                || cur.is(Blocks.SNOW_BLOCK)
                || cur.is(Blocks.STONE)
                || cur.is(Blocks.ANDESITE)
                || cur.is(Blocks.GRANITE)
                || cur.is(Blocks.DIORITE)
                || cur.is(Blocks.DEEPSLATE)
                || cur.is(Blocks.COBBLESTONE)
                || cur.is(Blocks.MOSSY_COBBLESTONE)
                || cur.is(ModBlocks.SCARLET_STONE.get())
                || cur.is(ModBlocks.SCARLET_DEEPSLATE.get())
                || cur.is(ModBlocks.SCARLET_COBBLESTONE.get())
                || cur.is(ModBlocks.SOL_SAND.get())
                || cur.is(ModBlocks.SOL_SANDSTONE.get())
                || cur.is(ModBlocks.ABYSS_SAND.get());
    }

    private static void placeRetainingWallColumn(WorldGenLevel level, BlockPos.MutableBlockPos mPos,
                                                 int wx, int wz, int structMinY, int wallCapY, int subGroundY,
                                                 int worldMinY, boolean isScarlet, boolean isRuins,
                                                 boolean allowTerrainTrim) {
        int embedY = Math.max(worldMinY + 2, subGroundY - 2);
        boolean placedTop = false;
        for (int y = wallCapY; y >= embedY; y--) {
            mPos.set(wx, y, wz);
            BlockState cur = level.getBlockState(mPos);
            boolean grassOrNatural = isNaturalGrassOrFoliage(cur) || isReplaceableForStructureWall(cur);
            boolean shouldReplace = (y < structMinY)
                    ? (y >= subGroundY || grassOrNatural)
                    : (isNaturalGrassOrFoliage(cur)
                            || (grassOrNatural && (allowTerrainTrim || (!cur.is(Blocks.STONE) && !cur.is(ModBlocks.SCARLET_STONE.get())))));
            if (shouldReplace) {
                boolean topCourse = (y == wallCapY);
                level.setBlock(mPos, pickWallMasonry(wx, y, wz, subGroundY, topCourse, isScarlet, isRuins), 2);
                if (topCourse) {
                    placedTop = true;
                }
            }
        }
        if (placedTop && wallCapY + 1 < level.getMaxBuildHeight()) {
            mPos.set(wx, wallCapY + 1, wz);
            BlockState above = level.getBlockState(mPos);
            if (above.is(Blocks.SHORT_GRASS) || above.is(Blocks.FERN)
                    || above.is(Blocks.TALL_GRASS) || above.is(Blocks.LARGE_FERN)
                    || above.is(BlockTags.FLOWERS)) {
                level.setBlock(mPos, Blocks.AIR.defaultBlockState(), 2);
                if (above.getBlock() instanceof DoublePlantBlock && wallCapY + 2 < level.getMaxBuildHeight()) {
                    mPos.set(wx, wallCapY + 2, wz);
                    if (level.getBlockState(mPos).getBlock() == above.getBlock()) {
                        level.setBlock(mPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static int findSolidTerrainBelow(WorldGenLevel level, BlockPos.MutableBlockPos mPos, int x, int startY, int z, int worldMinY, int maxSearch) {
        int minSearchY = Math.max(worldMinY + 2, startY - maxSearch);
        for (int y = startY; y >= minSearchY; y--) {
            mPos.set(x, y, z);
            BlockState st = level.getBlockState(mPos);
            if (!st.isAir() && !st.liquid() && !st.canBeReplaced()
                    && !st.is(BlockTags.LEAVES) && !st.is(BlockTags.LOGS)
                    && st.isCollisionShapeFullBlock(level, mPos)) {
                return y;
            }
        }
        return minSearchY;
    }

    private static int findSolidFoundationBelow(WorldGenLevel level, BlockPos.MutableBlockPos mPos, int x, int startY, int z, int worldMinY, int maxSearch) {
        int minSearchY = Math.max(worldMinY + 2, startY - maxSearch);
        for (int y = startY; y >= minSearchY; y--) {
            mPos.set(x, y, z);
            BlockState st = level.getBlockState(mPos);
            if (!st.isAir() && !st.liquid() && !st.canBeReplaced()
                    && !st.is(BlockTags.LEAVES) && !st.is(BlockTags.LOGS)
                    && !st.is(Blocks.GRASS_BLOCK)
                    && st.isCollisionShapeFullBlock(level, mPos)) {
                // Skip thin 1-block hanging crusts over caves/overhangs so foundation dirt reaches solid ground
                mPos.set(x, y - 1, z);
                BlockState below = level.getBlockState(mPos);
                if (st.is(Blocks.BEDROCK) || y - 1 <= minSearchY
                        || (!below.isAir() && !below.liquid() && !below.canBeReplaced())) {
                    return y;
                }
            }
        }
        return minSearchY;
    }

    private static BlockState pickWallMasonry(int x, int y, int z, int groundY, boolean topCourse,
                                             boolean isScarlet, boolean isRuins) {
        int h = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791);
        int roll = Math.floorMod(h ^ (h >>> 13), 100);
        boolean nearFooting = (y <= groundY + 1);

        if (isScarlet) {
            if (topCourse) {
                return roll < (isRuins ? 35 : 15)
                        ? ModBlocks.CRACKED_SCARLET_STONE_BRICKS.get().defaultBlockState()
                        : ModBlocks.SCARLET_STONE_BRICKS.get().defaultBlockState();
            }
            int crackedPct = isRuins ? 30 : 14;
            int mossyPct = nearFooting ? (isRuins ? 75 : 55) : (isRuins ? 55 : 26);
            if (roll < crackedPct) return ModBlocks.CRACKED_SCARLET_STONE_BRICKS.get().defaultBlockState();
            if (roll < mossyPct) return ModBlocks.MOSSY_SCARLET_STONE_BRICKS.get().defaultBlockState();
            return ModBlocks.SCARLET_STONE_BRICKS.get().defaultBlockState();
        }

        if (topCourse) {
            if (isRuins && roll < 30) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            return roll < (isRuins ? 45 : 12)
                    ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                    : Blocks.STONE_BRICKS.defaultBlockState();
        }
        int crackedPct = isRuins ? 28 : 14;
        int mossyPct = nearFooting ? (isRuins ? 75 : 55) : (isRuins ? 52 : 28);
        if (roll < crackedPct) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        if (roll < mossyPct) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    private void generatePieces(StructurePiecesBuilder builder, GenerationContext context, BlockPos startPos, Rotation rotation) {
        StructureTemplateManager manager = context.structureTemplateManager();
        Holder<StructureProcessorList> terrainBlendProcessors = context.registryAccess()
                .registry(Registries.PROCESSOR_LIST)
                .flatMap(reg -> reg.getHolder(ModStructureProcessorLists.OUTPOST_TERRAIN_BLEND_KEY))
                .orElse(null);

        for (int gridX = 0; gridX < this.widthChunks; gridX++) {
            for (int gridZ = 0; gridZ < this.depthChunks; gridZ++) {
                
                int offsetX = gridX * this.gridSize;
                int offsetZ = gridZ * this.gridSize;
                
                BlockPos relativePos = new BlockPos(offsetX, 0, offsetZ).rotate(rotation);
                BlockPos finalPos = startPos.offset(relativePos);
                
                ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath(this.basename.getNamespace(), this.basename.getPath() + "_" + offsetX + "_" + offsetZ);
                
                StructurePoolElement element = (terrainBlendProcessors != null
                        ? SinglePoolElement.legacy(templateId.toString(), terrainBlendProcessors)
                        : SinglePoolElement.legacy(templateId.toString()))
                        .apply(StructureTemplatePool.Projection.RIGID);
                
                BoundingBox boundingBox = element.getBoundingBox(manager, finalPos, rotation);
                
                PoolElementStructurePiece piece = new PoolElementStructurePiece(
                    manager,
                    element,
                    finalPos,
                    element.getGroundLevelDelta(),
                    rotation,
                    boundingBox,
                    LiquidSettings.IGNORE_WATERLOGGING
                );
                
                builder.addPiece(piece);
            }
        }
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.GRID.get();
    }
}
