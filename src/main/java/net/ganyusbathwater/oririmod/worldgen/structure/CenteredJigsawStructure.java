package net.ganyusbathwater.oririmod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CenteredJigsawStructure extends Structure {
    
    public static final MapCodec<CenteredJigsawStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            settingsCodec(instance),
            JigsawStructure.CODEC.fieldOf("jigsaw_config").forGetter(structure -> structure.jigsawStructure)
        ).apply(instance, CenteredJigsawStructure::new)
    );

    private final JigsawStructure jigsawStructure;

    public CenteredJigsawStructure(StructureSettings settings, JigsawStructure jigsawStructure) {
        super(settings);
        this.jigsawStructure = jigsawStructure;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        int radius = 16; // strict 1 chunk radius check
        
        Set<Holder<Biome>> biomesWithin = context.biomeSource().getBiomesWithin(x, context.chunkGenerator().getSeaLevel(), z, radius, context.randomState().sampler());
        for (Holder<Biome> biome : biomesWithin) {
            if (!this.biomes().contains(biome)) {
                return Optional.empty(); // Cancel generation if too close to border
            }
        }
        
        return this.jigsawStructure.findGenerationPoint(context);
    }
    
    @Override
    public StructureType<?> type() {
        return ModStructureTypes.CENTERED_JIGSAW.get();
    }
}
