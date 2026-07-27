package net.ganyusbathwater.oririmod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

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
        int y = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        
        // Add the startHeight offset
        y += this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        
        BlockPos startPos = new BlockPos(x, y, z);
        
        Rotation rotation = Rotation.getRandom(context.random());

        return Optional.of(new GenerationStub(startPos, builder -> this.generatePieces(builder, context, startPos, rotation)));
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
