package net.ganyusbathwater.oririmod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
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
                if (sampleY <= 63) {
                    return Optional.empty();
                }

                // Reject if structure footprint crosses outside allowed biomes
                Holder<Biome> biome = context.biomeSource().getNoiseBiome(
                        sampleX >> 2, sampleY >> 2, sampleZ >> 2, context.randomState().sampler()
                );
                if (!this.biomes().contains(biome)) {
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
        List<StructurePiece> pieceList = pieces.pieces();
        if (pieceList.isEmpty()) {
            return;
        }

        int minX = chunkBox.minX();
        int maxX = chunkBox.maxX();
        int minZ = chunkBox.minZ();
        int maxZ = chunkBox.maxZ();
        int widthX = maxX - minX + 1;
        int widthZ = maxZ - minZ + 1;
        if (widthX <= 0 || widthZ <= 0 || widthX > 16 || widthZ > 16) {
            return;
        }

        // Pad by 1 block on each side so perimeter & stepped batter detection is seamless across chunk seams.
        int padW = widthX + 2;
        int padD = widthZ + 2;
        boolean[][] hasPlatform = new boolean[padW][padD];
        boolean[][] needsWall = new boolean[padW][padD];
        int[][] platformBottomY = new int[padW][padD];
        int[][] pieceMinYs = new int[padW][padD];
        int[][] terrainGroundY = new int[padW][padD];

        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        int worldMinY = level.getMinBuildHeight();

        for (int lx = 0; lx < padW; lx++) {
            int wx = minX - 1 + lx;
            for (int lz = 0; lz < padD; lz++) {
                int wz = minZ - 1 + lz;

                int pieceBaseY = Integer.MAX_VALUE;
                for (int p = 0; p < pieceList.size(); p++) {
                    BoundingBox box = pieceList.get(p).getBoundingBox();
                    if (wx >= box.minX() && wx <= box.maxX() && wz >= box.minZ() && wz <= box.maxZ()) {
                        if (box.minY() < pieceBaseY) {
                            pieceBaseY = box.minY();
                        }
                    }
                }

                if (pieceBaseY == Integer.MAX_VALUE) {
                    int surfY = Math.min(level.getMaxBuildHeight() - 1,
                            level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, wx, wz));
                    terrainGroundY[lx][lz] = findSolidTerrainBelow(level, mPos, wx, surfY, wz, worldMinY, 48);
                    continue;
                }

                pieceMinYs[lx][lz] = pieceBaseY;
                int bottomY = Integer.MIN_VALUE;
                int maxCheckY = Math.min(level.getMaxBuildHeight() - 1, pieceBaseY + 3);
                for (int y = pieceBaseY; y <= maxCheckY; y++) {
                    mPos.set(wx, y, wz);
                    BlockState st = level.getBlockState(mPos);
                    if (isOutpostFoundationBlock(level, mPos, st)) {
                        bottomY = y;
                        break;
                    }
                }

                if (bottomY == Integer.MIN_VALUE) {
                    int surfY = Math.min(level.getMaxBuildHeight() - 1, pieceBaseY + 4);
                    terrainGroundY[lx][lz] = findSolidTerrainBelow(level, mPos, wx, surfY, wz, worldMinY, 48);
                    continue;
                }

                hasPlatform[lx][lz] = true;
                platformBottomY[lx][lz] = bottomY;

                int groundY = findSolidTerrainBelow(level, mPos, wx, bottomY - 1, wz, worldMinY, 34);
                terrainGroundY[lx][lz] = groundY;
                needsWall[lx][lz] = (bottomY - groundY) >= 2;
            }
        }

        String path = this.basename.getPath();
        boolean isScarlet = path.contains("scarlet");
        boolean isRuins = path.contains("ruins");

        for (int lx = 1; lx <= widthX; lx++) {
            int wx = minX - 1 + lx;
            for (int lz = 1; lz <= widthZ; lz++) {
                int wz = minZ - 1 + lz;

                if (hasPlatform[lx][lz]) {
                    int bottomY = platformBottomY[lx][lz];
                    int pieceBaseY = pieceMinYs[lx][lz];
                    int groundY = terrainGroundY[lx][lz];

                    boolean isPerimeter = !hasPlatform[lx - 1][lz] || !hasPlatform[lx + 1][lz]
                            || !hasPlatform[lx][lz - 1] || !hasPlatform[lx][lz + 1]
                            || !hasPlatform[lx - 1][lz - 1] || !hasPlatform[lx + 1][lz - 1]
                            || !hasPlatform[lx - 1][lz + 1] || !hasPlatform[lx + 1][lz + 1];

                    if (isPerimeter) {
                        // Even at slight edges, replace exposed dirt sub-base (y0..y2) with stone masonry
                        // whenever an exterior neighbor sits below pieceBaseY + 2 so dirt never sticks out.
                        boolean exposedSide = needsWall[lx][lz]
                                || (!hasPlatform[lx - 1][lz] && terrainGroundY[lx - 1][lz] < pieceBaseY + 2)
                                || (!hasPlatform[lx + 1][lz] && terrainGroundY[lx + 1][lz] < pieceBaseY + 2)
                                || (!hasPlatform[lx][lz - 1] && terrainGroundY[lx][lz - 1] < pieceBaseY + 2)
                                || (!hasPlatform[lx][lz + 1] && terrainGroundY[lx][lz + 1] < pieceBaseY + 2);
                        if (exposedSide) {
                            int cladTopY = Math.min(level.getMaxBuildHeight() - 1, pieceBaseY + 2);
                            for (int y = cladTopY; y >= bottomY; y--) {
                                mPos.set(wx, y, wz);
                                BlockState cur = level.getBlockState(mPos);
                                if (cur.is(Blocks.DIRT) || cur.is(Blocks.COARSE_DIRT)) {
                                    level.setBlock(mPos, pickWallMasonry(wx, y, wz, bottomY, groundY, y == cladTopY, isScarlet, isRuins), 2);
                                }
                            }
                        }
                    }

                    if (!needsWall[lx][lz]) {
                        continue;
                    }

                    if (isPerimeter) {
                        // Build stone retaining wall downward & anchor 1 block into natural hillside soil
                        int embedY = Math.max(worldMinY + 2, groundY - 1);
                        for (int y = bottomY - 1; y >= embedY; y--) {
                            mPos.set(wx, y, wz);
                            BlockState cur = level.getBlockState(mPos);
                            if (y >= groundY || cur.isAir() || cur.liquid() || cur.canBeReplaced() || cur.is(Blocks.DIRT)) {
                                boolean topCourse = (y == bottomY - 1);
                                level.setBlock(mPos, pickWallMasonry(wx, y, wz, bottomY, groundY, topCourse, isScarlet, isRuins), 2);
                            }
                        }
                    } else {
                        // Interior underfill so courtyard & towers never hide hollow voids under the hill slope
                        int fillEndY = Math.max(worldMinY + 2, groundY);
                        for (int y = bottomY - 1; y >= fillEndY; y--) {
                            mPos.set(wx, y, wz);
                            BlockState cur = level.getBlockState(mPos);
                            if (cur.isAir() || cur.liquid() || cur.canBeReplaced() || cur.is(BlockTags.LEAVES)) {
                                BlockState fill = (bottomY - y <= 2)
                                        ? Blocks.DIRT.defaultBlockState()
                                        : ((y <= groundY + 1) ? Blocks.STONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                                level.setBlock(mPos, fill, 2);
                            }
                        }
                    }
                } else {
                    // Add stepped stone batter / buttress toes along tall downhill wall edges (>= 4 blocks drop)
                    int maxAdjDrop = 0;
                    int adjWallBottom = Integer.MIN_VALUE;
                    for (int dir = 0; dir < 4; dir++) {
                        int nlx = lx + (dir == 0 ? -1 : dir == 1 ? 1 : 0);
                        int nlz = lz + (dir == 2 ? -1 : dir == 3 ? 1 : 0);
                        if (hasPlatform[nlx][nlz] && needsWall[nlx][nlz]) {
                            int drop = platformBottomY[nlx][nlz] - terrainGroundY[nlx][nlz];
                            if (drop > maxAdjDrop) {
                                maxAdjDrop = drop;
                                adjWallBottom = platformBottomY[nlx][nlz];
                            }
                        }
                    }

                    if (maxAdjDrop >= 4 && adjWallBottom != Integer.MIN_VALUE) {
                        int localGroundY = terrainGroundY[lx][lz];
                        boolean isButtressRib = Math.floorMod(wx + wz, 4) == 0;
                        int stepHeight = isButtressRib
                                ? Math.min(adjWallBottom - 1 - localGroundY, Math.max(2, maxAdjDrop / 2))
                                : Math.min(adjWallBottom - 2 - localGroundY, Math.max(1, maxAdjDrop / 4));
                        if (stepHeight >= 1) {
                            int topStepY = localGroundY + stepHeight;
                            for (int y = localGroundY; y <= topStepY; y++) {
                                mPos.set(wx, y, wz);
                                BlockState cur = level.getBlockState(mPos);
                                if (y == localGroundY || cur.isAir() || cur.liquid() || cur.canBeReplaced()
                                        || cur.is(BlockTags.LEAVES) || cur.is(Blocks.DIRT)) {
                                    boolean topCourse = (y == topStepY);
                                    level.setBlock(mPos, pickWallMasonry(wx, y, wz, adjWallBottom, localGroundY, topCourse, isScarlet, isRuins), 2);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isOutpostFoundationBlock(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.liquid() || state.canBeReplaced()) return false;
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) return false;
        return state.isCollisionShapeFullBlock(level, pos);
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

    private static BlockState pickWallMasonry(int x, int y, int z, int topY, int groundY, boolean topCourse,
                                             boolean isScarlet, boolean isRuins) {
        int h = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791);
        int roll = Math.floorMod(h ^ (h >>> 13), 100);

        if (isScarlet) {
            if (y <= groundY + 1) {
                return roll < 50 ? ModBlocks.MOSSY_SCARLET_COBBLESTONE.get().defaultBlockState()
                        : ModBlocks.SCARLET_STONE.get().defaultBlockState();
            }
            if (topCourse) {
                return roll < (isRuins ? 35 : 15) ? ModBlocks.CRACKED_SCARLET_STONE_BRICKS.get().defaultBlockState()
                        : ModBlocks.SCARLET_STONE_BRICKS.get().defaultBlockState();
            }
            if (roll < (isRuins ? 30 : 14)) return ModBlocks.CRACKED_SCARLET_STONE_BRICKS.get().defaultBlockState();
            if (roll < (isRuins ? 55 : 26)) return ModBlocks.MOSSY_SCARLET_STONE_BRICKS.get().defaultBlockState();
            return ModBlocks.SCARLET_STONE_BRICKS.get().defaultBlockState();
        }

        // Damp lower footing transitions naturally into mossy/rough cobble near natural soil
        if (y <= groundY + 1) {
            if (roll < (isRuins ? 60 : 38)) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            if (roll < 85) return Blocks.COBBLESTONE.defaultBlockState();
            return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        }
        if (topCourse) {
            if (isRuins && roll < 30) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            return roll < (isRuins ? 45 : 12) ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                    : Blocks.STONE_BRICKS.defaultBlockState();
        }
        if (roll < (isRuins ? 28 : 14)) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        if (roll < (isRuins ? 52 : 28)) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        if (y <= groundY + 3 && roll < 42) return Blocks.COBBLESTONE.defaultBlockState();
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    private void generatePieces(StructurePiecesBuilder builder, GenerationContext context, BlockPos startPos, Rotation rotation) {
        StructureTemplateManager manager = context.structureTemplateManager();
        
        for (int gridX = 0; gridX < this.widthChunks; gridX++) {
            for (int gridZ = 0; gridZ < this.depthChunks; gridZ++) {
                
                int offsetX = gridX * this.gridSize;
                int offsetZ = gridZ * this.gridSize;
                
                BlockPos relativePos = new BlockPos(offsetX, 0, offsetZ).rotate(rotation);
                BlockPos finalPos = startPos.offset(relativePos);
                
                ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath(this.basename.getNamespace(), this.basename.getPath() + "_" + offsetX + "_" + offsetZ);
                
                StructurePoolElement element = SinglePoolElement.legacy(templateId.toString()).apply(StructureTemplatePool.Projection.RIGID);
                
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
