package net.ganyusbathwater.oririmod.worldgen.processor;

import com.mojang.serialization.MapCodec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.worldgen.ModStructureProcessors;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * Blends the earthen skirt and leftover surface grass baked into GridStructure outpost templates
 * seamlessly into natural surrounding terrain (stone, andesite, podzol, dirt, grass, sand) when
 * a structure generates partially buried inside a slope or rocky hillside.
 */
public class OutpostTerrainBlendProcessor extends StructureProcessor {
    public static final OutpostTerrainBlendProcessor INSTANCE = new OutpostTerrainBlendProcessor();
    public static final MapCodec<OutpostTerrainBlendProcessor> CODEC = MapCodec.unit(INSTANCE);

    private OutpostTerrainBlendProcessor() {
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos offset,
            BlockPos pos,
            StructureTemplate.StructureBlockInfo blockInfoLocal,
            StructureTemplate.StructureBlockInfo blockInfoGlobal,
            StructurePlaceSettings settings) {

        BlockState templateState = blockInfoGlobal.state();
        BlockPos worldPos = blockInfoGlobal.pos();

        boolean isFoliagePlant = templateState.is(Blocks.SHORT_GRASS)
                || templateState.is(Blocks.TALL_GRASS)
                || templateState.is(Blocks.FERN)
                || templateState.is(Blocks.LARGE_FERN)
                || templateState.is(Blocks.DEAD_BUSH);

        boolean isGrassBlock = templateState.is(Blocks.GRASS_BLOCK);
        boolean isFoundationDirt = templateState.is(Blocks.DIRT) || templateState.is(Blocks.COARSE_DIRT);

        if (!isFoliagePlant && !isGrassBlock && !isFoundationDirt) {
            return blockInfoGlobal;
        }

        BlockState worldCur = level.getBlockState(worldPos);
        BlockState worldAbove = level.getBlockState(worldPos.above());
        boolean worldCurNaturalGrassOrPlant = isNaturalGrassOrPlant(worldCur);
        boolean worldCurNaturalSolid = isNaturalSolidTerrain(worldCur);
        boolean coveredAbove = !worldAbove.isAir()
                && !worldAbove.canBeReplaced()
                && !worldAbove.liquid()
                && !worldAbove.is(BlockTags.LEAVES);

        // 1. Template foliage plants: prioritize structure placement over natural grass plants,
        // while still preventing 1x1 pits inside deep natural stone/dirt slopes.
        if (isFoliagePlant) {
            if (coveredAbove || (worldCurNaturalSolid && !worldCur.is(Blocks.GRASS_BLOCK))) {
                return null; // keep buried natural rock/dirt untouched
            }
            if (worldCur.is(Blocks.GRASS_BLOCK)) {
                return null;
            }
            BlockState worldBelow = level.getBlockState(worldPos.below());
            if (!worldBelow.is(BlockTags.DIRT)) {
                return null;
            }
            return blockInfoGlobal;
        }

        // 2. Template Grass Block skirt: unconditionally replace natural grass blocks & foliage,
        // and convert buried grass blocks cleanly to dirt when covered by structure blocks.
        if (isGrassBlock) {
            if (coveredAbove) {
                if (worldCurNaturalSolid && !worldCur.is(Blocks.GRASS_BLOCK)) {
                    return null; // preserve existing natural stone/dirt/podzol/andesite
                }
                return new StructureTemplate.StructureBlockInfo(worldPos, Blocks.DIRT.defaultBlockState(), null);
            }
            if (worldCurNaturalGrassOrPlant) {
                return blockInfoGlobal; // always prioritize structure over natural grass
            }
            if (worldCurNaturalSolid) {
                // Keep non-grass natural terrain surface (podzol, stone, andesite, sand, scarlet stone, etc.)
                return null;
            }
            return blockInfoGlobal;
        }

        // 3. Template Foundation Dirt: unconditionally replace natural grass blocks & foliage,
        // while preserving solid rock/sand/soil if already occupied by non-grass ground.
        if (worldCurNaturalGrassOrPlant) {
            return blockInfoGlobal; // always prioritize structure dirt over natural grass
        }
        if (worldCurNaturalSolid) {
            return null; // keep natural stone/andesite/podzol/sandstone/dirt/scarlet stone
        }

        return blockInfoGlobal;
    }

    private static boolean isNaturalGrassOrPlant(BlockState st) {
        return st.is(Blocks.GRASS_BLOCK)
                || st.is(Blocks.SHORT_GRASS)
                || st.is(Blocks.TALL_GRASS)
                || st.is(Blocks.FERN)
                || st.is(Blocks.LARGE_FERN)
                || st.is(BlockTags.FLOWERS);
    }

    private static boolean isNaturalSolidTerrain(BlockState st) {
        if (st.isAir() || st.liquid() || st.canBeReplaced() || st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS)) {
            return false;
        }
        return st.is(BlockTags.DIRT)
                || st.is(BlockTags.SAND)
                || st.is(Blocks.STONE)
                || st.is(Blocks.ANDESITE)
                || st.is(Blocks.GRANITE)
                || st.is(Blocks.DIORITE)
                || st.is(Blocks.DEEPSLATE)
                || st.is(Blocks.COBBLESTONE)
                || st.is(Blocks.MOSSY_COBBLESTONE)
                || st.is(Blocks.GRAVEL)
                || st.is(Blocks.SANDSTONE)
                || st.is(Blocks.CLAY)
                || st.is(Blocks.SNOW_BLOCK)
                || st.is(ModBlocks.SCARLET_STONE.get())
                || st.is(ModBlocks.SCARLET_DEEPSLATE.get())
                || st.is(ModBlocks.SCARLET_COBBLESTONE.get())
                || st.is(ModBlocks.SOL_SAND.get())
                || st.is(ModBlocks.SOL_SANDSTONE.get())
                || st.is(ModBlocks.ABYSS_SAND.get());
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructureProcessors.OUTPOST_TERRAIN_BLEND.get();
    }
}
