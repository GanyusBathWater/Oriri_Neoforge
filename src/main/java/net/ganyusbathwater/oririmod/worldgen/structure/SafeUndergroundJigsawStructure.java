package net.ganyusbathwater.oririmod.worldgen.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

import java.util.List;
import java.util.Optional;

/**
 * Underground Jigsaw structure with two-stage terrain bounds validation:
 * <ol>
 *   <li>Stage 1 pre-filters low surface/ocean starts before Jigsaw expansion runs.</li>
 *   <li>Stage 2 inspects every generated {@link StructurePiece} bounding box against the
 *       solid surface height and dimension bedrock floor, rejecting the structure if any
 *       room lacks sufficient rock cover or bedrock clearance.</li>
 * </ol>
 */
public final class SafeUndergroundJigsawStructure extends Structure {
    public static final int DEFAULT_MIN_SURFACE_COVER = 14;
    public static final int DEFAULT_MIN_BEDROCK_CLEARANCE = 8;
    public static final int DEFAULT_CORE_CHECK_RADIUS = 48;
    public static final int DEFAULT_CORE_MIN_SURFACE_Y = 58;

    private static final int MAX_TOTAL_STRUCTURE_RANGE = 128;
    private static final int CORE_SAMPLE_STEP = 16;
    private static final int PIECE_SAMPLE_STEP = 8;

    public static final MapCodec<SafeUndergroundJigsawStructure> CODEC = RecordCodecBuilder.<SafeUndergroundJigsawStructure>mapCodec(
                    instance -> instance.group(
                                    settingsCodec(instance),
                                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                                    ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
                                    Codec.intRange(0, 20).fieldOf("size").forGetter(s -> s.maxDepth),
                                    HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
                                    Codec.BOOL.fieldOf("use_expansion_hack").forGetter(s -> s.useExpansionHack),
                                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
                                    Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
                                    Codec.list(PoolAliasBinding.CODEC).optionalFieldOf("pool_aliases", List.of()).forGetter(s -> s.poolAliases),
                                    DimensionPadding.CODEC.optionalFieldOf("dimension_padding", DimensionPadding.ZERO).forGetter(s -> s.dimensionPadding),
                                    LiquidSettings.CODEC.optionalFieldOf("liquid_settings", LiquidSettings.APPLY_WATERLOGGING).forGetter(s -> s.liquidSettings),
                                    Codec.intRange(0, 128).optionalFieldOf("min_surface_cover", DEFAULT_MIN_SURFACE_COVER).forGetter(s -> s.minSurfaceCover),
                                    Codec.intRange(0, 128).optionalFieldOf("min_bedrock_clearance", DEFAULT_MIN_BEDROCK_CLEARANCE).forGetter(s -> s.minBedrockClearance),
                                    Codec.intRange(0, 128).optionalFieldOf("core_check_radius", DEFAULT_CORE_CHECK_RADIUS).forGetter(s -> s.coreCheckRadius),
                                    Codec.INT.optionalFieldOf("core_min_surface_y", DEFAULT_CORE_MIN_SURFACE_Y).forGetter(s -> s.coreMinSurfaceY)
                            )
                            .apply(instance, SafeUndergroundJigsawStructure::new)
            )
            .validate(SafeUndergroundJigsawStructure::verifyRange);

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final List<PoolAliasBinding> poolAliases;
    private final DimensionPadding dimensionPadding;
    private final LiquidSettings liquidSettings;
    private final int minSurfaceCover;
    private final int minBedrockClearance;
    private final int coreCheckRadius;
    private final int coreMinSurfaceY;

    public SafeUndergroundJigsawStructure(
            Structure.StructureSettings settings,
            Holder<StructureTemplatePool> startPool,
            Optional<ResourceLocation> startJigsawName,
            int maxDepth,
            HeightProvider startHeight,
            boolean useExpansionHack,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            List<PoolAliasBinding> poolAliases,
            DimensionPadding dimensionPadding,
            LiquidSettings liquidSettings,
            int minSurfaceCover,
            int minBedrockClearance,
            int coreCheckRadius,
            int coreMinSurfaceY
    ) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.maxDepth = maxDepth;
        this.startHeight = startHeight;
        this.useExpansionHack = useExpansionHack;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.poolAliases = poolAliases;
        this.dimensionPadding = dimensionPadding;
        this.liquidSettings = liquidSettings;
        this.minSurfaceCover = minSurfaceCover;
        this.minBedrockClearance = minBedrockClearance;
        this.coreCheckRadius = coreCheckRadius;
        this.coreMinSurfaceY = coreMinSurfaceY;
    }

    private static DataResult<SafeUndergroundJigsawStructure> verifyRange(SafeUndergroundJigsawStructure structure) {
        int adaptationPadding = switch (structure.terrainAdaptation()) {
            case NONE -> 0;
            case BURY, BEARD_THIN, BEARD_BOX, ENCAPSULATE -> 12;
        };
        return structure.maxDistanceFromCenter + adaptationPadding > MAX_TOTAL_STRUCTURE_RANGE
                ? DataResult.error(() -> "Structure size including terrain adaptation must not exceed " + MAX_TOTAL_STRUCTURE_RANGE)
                : DataResult.success(structure);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int startY = this.startHeight.sample(
                context.random(),
                new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor())
        );
        BlockPos startPos = new BlockPos(chunkPos.getMinBlockX(), startY, chunkPos.getMinBlockZ());

        if (!this.hasSafeSurfaceCore(context, startPos)) {
            return Optional.empty();
        }

        Optional<Structure.GenerationStub> generated = JigsawPlacement.addPieces(
                context,
                this.startPool,
                this.startJigsawName,
                this.maxDepth,
                startPos,
                this.useExpansionHack,
                this.projectStartToHeightmap,
                this.maxDistanceFromCenter,
                PoolAliasLookup.create(this.poolAliases, startPos, context.seed()),
                this.dimensionPadding,
                this.liquidSettings
        );
        if (generated.isEmpty()) {
            return Optional.empty();
        }

        Structure.GenerationStub stub = generated.get();
        StructurePiecesBuilder piecesBuilder = stub.getPiecesBuilder();
        if (!this.allPiecesSafelyBuried(context, piecesBuilder.build())) {
            return Optional.empty();
        }

        // Reuse the already-built pieces directly so JigsawPlacement is never evaluated twice.
        return Optional.of(new Structure.GenerationStub(stub.position(), Either.right(piecesBuilder)));
    }

    private boolean hasSafeSurfaceCore(Structure.GenerationContext context, BlockPos startPos) {
        if (this.coreCheckRadius <= 0) {
            return true;
        }
        int originX = startPos.getX();
        int originZ = startPos.getZ();
        if (getSolidSurfaceY(context, originX, originZ) < this.coreMinSurfaceY) {
            return false;
        }

        int radiusSq = this.coreCheckRadius * this.coreCheckRadius;
        for (int dx = -this.coreCheckRadius; ; dx += CORE_SAMPLE_STEP) {
            int offsetX = Math.min(dx, this.coreCheckRadius);
            for (int dz = -this.coreCheckRadius; ; dz += CORE_SAMPLE_STEP) {
                int offsetZ = Math.min(dz, this.coreCheckRadius);
                if (offsetX * offsetX + offsetZ * offsetZ <= radiusSq) {
                    if (getSolidSurfaceY(context, originX + offsetX, originZ + offsetZ) < this.coreMinSurfaceY) {
                        return false;
                    }
                }
                if (offsetZ >= this.coreCheckRadius) {
                    break;
                }
            }
            if (offsetX >= this.coreCheckRadius) {
                break;
            }
        }
        return true;
    }

    private boolean allPiecesSafelyBuried(Structure.GenerationContext context, PiecesContainer piecesContainer) {
        List<StructurePiece> pieces = piecesContainer.pieces();
        if (pieces.isEmpty()) {
            return false;
        }
        int minAllowedY = context.chunkGenerator().getMinY() + this.minBedrockClearance;
        for (StructurePiece piece : pieces) {
            if (!this.isPieceSafelyBuried(context, piece.getBoundingBox(), minAllowedY)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPieceSafelyBuried(Structure.GenerationContext context, BoundingBox box, int minAllowedY) {
        if (box.minY() < minAllowedY) {
            return false;
        }
        for (int x = box.minX(); ; x += PIECE_SAMPLE_STEP) {
            int sampleX = Math.min(x, box.maxX());
            for (int z = box.minZ(); ; z += PIECE_SAMPLE_STEP) {
                int sampleZ = Math.min(z, box.maxZ());
                int surfaceY = getSolidSurfaceY(context, sampleX, sampleZ);
                if (box.maxY() + this.minSurfaceCover > surfaceY) {
                    return false;
                }
                if (sampleZ >= box.maxZ()) {
                    break;
                }
            }
            if (sampleX >= box.maxX()) {
                break;
            }
        }
        return true;
    }

    private static int getSolidSurfaceY(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(
                x,
                z,
                Heightmap.Types.OCEAN_FLOOR_WG,
                context.heightAccessor(),
                context.randomState()
        );
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.SAFE_UNDERGROUND_JIGSAW.get();
    }
}

