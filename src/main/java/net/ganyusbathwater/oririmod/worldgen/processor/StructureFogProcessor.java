package net.ganyusbathwater.oririmod.worldgen.processor;

import com.mojang.serialization.MapCodec;
import net.ganyusbathwater.oririmod.attachment.ModAttachments;
import net.ganyusbathwater.oririmod.block.custom.FogSourceBlock;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.ganyusbathwater.oririmod.world.fog.ChunkFogAnchors;
import net.ganyusbathwater.oririmod.world.fog.FogAnchor;
import net.ganyusbathwater.oririmod.worldgen.ModStructureProcessors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Structure processor that converts creative-placed {@link FogSourceBlock}s in structure templates
 * into invisible, non-blocking chunk-attached {@link FogAnchor}s during worldgen.
 * <p>
 * Benefits:
 * <ul>
 *   <li>Replaces each {@code FogSourceBlock} with air (or water if waterlogged), so survival players
 *       can freely place torches, chests, beds, or blocks on that tile without breaking the fog.</li>
 *   <li>Snapshots up to 8 nearby solid structure blocks directly from the template piece as structural
 *       supports, so {@link net.ganyusbathwater.oririmod.world.fog.StructureFogEventHandler} automatically
 *       clears the fog if the player demolishes the structure.</li>
 *   <li>Works cleanly in both async worldgen ({@code WorldGenRegion} / {@code ProtoChunk}) and live
 *       server placement ({@code /place structure}).</li>
 * </ul>
 */
public class StructureFogProcessor extends StructureProcessor {

    public static final StructureFogProcessor INSTANCE = new StructureFogProcessor();
    public static final MapCodec<StructureFogProcessor> CODEC = MapCodec.unit(INSTANCE);

    private static final int MAX_SUPPORTS = 8;
    private static final int SUPPORT_RADIUS_XZ = 4;

    private StructureFogProcessor() {
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            ServerLevelAccessor level,
            BlockPos offset,
            BlockPos pos,
            List<StructureTemplate.StructureBlockInfo> localInfos,
            List<StructureTemplate.StructureBlockInfo> globalInfos,
            StructurePlaceSettings settings) {

        // Fast check: skip allocating anything if this structure piece contains no FogSourceBlocks
        boolean hasFogBlock = false;
        for (int i = 0; i < globalInfos.size(); i++) {
            if (globalInfos.get(i).state().getBlock() instanceof FogSourceBlock) {
                hasFogBlock = true;
                break;
            }
        }
        if (!hasFogBlock) {
            return globalInfos;
        }

        // Index solid template blocks for fast structural-support selection
        List<StructureTemplate.StructureBlockInfo> solidBlocks = new ArrayList<>();
        Map<BlockPos, BlockState> solidMap = new HashMap<>();
        for (int i = 0; i < globalInfos.size(); i++) {
            StructureTemplate.StructureBlockInfo info = globalInfos.get(i);
            BlockState st = info.state();
            if (isCandidateSupportBlock(st)) {
                solidBlocks.add(info);
                solidMap.put(info.pos(), st);
            }
        }

        List<StructureTemplate.StructureBlockInfo> output = new ArrayList<>(globalInfos.size());
        Set<ChunkPos> dirtyChunks = new HashSet<>();

        for (int i = 0; i < globalInfos.size(); i++) {
            StructureTemplate.StructureBlockInfo info = globalInfos.get(i);
            BlockState state = info.state();
            if (!(state.getBlock() instanceof FogSourceBlock)) {
                output.add(info);
                continue;
            }

            BlockPos origin = info.pos().immutable();
            int density = state.getValue(FogSourceBlock.DENSITY);
            FogSourceBlock.FogColor color = state.getValue(FogSourceBlock.COLOR);
            boolean waterlogged = state.getValue(FogSourceBlock.WATERLOGGED);

            List<BlockPos> supports = pickStructuralSupports(origin, solidBlocks, solidMap);
            FogAnchor anchor = new FogAnchor(origin, density, color, supports);

            ChunkAccess chunk = level.getChunk(origin);
            ChunkFogAnchors chunkAnchors = chunk.getData(ModAttachments.FOG_ANCHORS);
            chunkAnchors.putAnchor(anchor);
            chunk.setUnsaved(true);
            dirtyChunks.add(chunk.getPos());

            // Replace FogSourceBlock with clean AIR (or WATER) so survival players can build freely here
            BlockState replacement = waterlogged
                    ? Blocks.WATER.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
            output.add(new StructureTemplate.StructureBlockInfo(origin, replacement, null));
        }

        // If placed live (e.g. /place structure on an active ServerLevel), immediately sync to tracking clients
        if (level instanceof ServerLevel serverLevel && !dirtyChunks.isEmpty()) {
            for (ChunkPos chunkPos : dirtyChunks) {
                ChunkAccess chunk = serverLevel.getChunk(chunkPos.x, chunkPos.z);
                if (chunk.hasData(ModAttachments.FOG_ANCHORS)) {
                    NetworkHandler.sendChunkFogAnchorsToTracking(
                            serverLevel, chunkPos, chunk.getData(ModAttachments.FOG_ANCHORS).toClientViews());
                }
            }
        }

        return output;
    }

    /**
     * Picks up to {@value MAX_SUPPORTS} well-distributed solid structure blocks around {@code origin}:
     * first the floor block directly beneath {@code origin}, then up to 7 surrounding non-terrain
     * structure blocks spread across all 4 quadrants (falling back to any solid template blocks).
     */
    private static List<BlockPos> pickStructuralSupports(
            BlockPos origin,
            List<StructureTemplate.StructureBlockInfo> solidBlocks,
            Map<BlockPos, BlockState> solidMap) {
        List<BlockPos> result = new ArrayList<>(MAX_SUPPORTS);
        Set<BlockPos> chosen = new HashSet<>();

        // 1. Always prioritize the floor directly beneath origin if present in the template
        BlockPos floorPos = origin.below();
        if (solidMap.containsKey(floorPos)) {
            result.add(floorPos.immutable());
            chosen.add(floorPos);
        }

        // Collect nearby candidates separated into preferred architectural blocks vs natural soil/grass
        List<BlockPos> preferred = new ArrayList<>();
        List<BlockPos> fallback = new ArrayList<>();

        for (int i = 0; i < solidBlocks.size(); i++) {
            StructureTemplate.StructureBlockInfo cand = solidBlocks.get(i);
            BlockPos p = cand.pos();
            if (chosen.contains(p)) continue;
            int dx = p.getX() - origin.getX();
            int dy = p.getY() - origin.getY();
            int dz = p.getZ() - origin.getZ();
            if (Math.abs(dx) > SUPPORT_RADIUS_XZ || Math.abs(dz) > SUPPORT_RADIUS_XZ || dy < -2 || dy > 3) {
                continue;
            }
            if (isNaturalDirtOrGrass(cand.state())) {
                fallback.add(p);
            } else {
                preferred.add(p);
            }
        }

        // 2. Pick 1 closest block from each of the 4 quadrants first so supports span the room evenly
        addSpreadCandidates(origin, preferred, result, chosen);
        if (result.size() < MAX_SUPPORTS) {
            addSpreadCandidates(origin, fallback, result, chosen);
        }

        // 3. Fill any remaining slots with nearest preferred then fallback candidates
        fillNearest(origin, preferred, result, chosen);
        if (result.size() < MAX_SUPPORTS) {
            fillNearest(origin, fallback, result, chosen);
        }

        return result;
    }

    private static void addSpreadCandidates(
            BlockPos origin,
            List<BlockPos> pool,
            List<BlockPos> result,
            Set<BlockPos> chosen) {
        BlockPos[] bestPerQuadrant = new BlockPos[4];
        int[] bestDistSq = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };

        for (int i = 0; i < pool.size(); i++) {
            BlockPos p = pool.get(i);
            if (chosen.contains(p)) continue;
            int dx = p.getX() - origin.getX();
            int dz = p.getZ() - origin.getZ();
            int dy = p.getY() - origin.getY();
            int q = (dx >= 0 ? 1 : 0) | (dz >= 0 ? 2 : 0);
            int distSq = dx * dx + dz * dz + dy * dy * 2;
            // Prefer blocks 2..4 blocks out over blocks stacked right next to floorPos
            int score = Math.abs(distSq - 6);
            if (score < bestDistSq[q]) {
                bestDistSq[q] = score;
                bestPerQuadrant[q] = p;
            }
        }

        for (int q = 0; q < 4 && result.size() < MAX_SUPPORTS; q++) {
            BlockPos p = bestPerQuadrant[q];
            if (p != null && chosen.add(p)) {
                result.add(p.immutable());
            }
        }
    }

    private static void fillNearest(
            BlockPos origin,
            List<BlockPos> pool,
            List<BlockPos> result,
            Set<BlockPos> chosen) {
        while (result.size() < MAX_SUPPORTS) {
            BlockPos best = null;
            int bestDistSq = Integer.MAX_VALUE;
            for (int i = 0; i < pool.size(); i++) {
                BlockPos p = pool.get(i);
                if (chosen.contains(p)) continue;
                int distSq = (int) p.distSqr(origin);
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = p;
                }
            }
            if (best == null) {
                break;
            }
            chosen.add(best);
            result.add(best.immutable());
        }
    }

    private static boolean isCandidateSupportBlock(BlockState state) {
        if (state.isAir()
                || !state.getFluidState().isEmpty()
                || state.getBlock() instanceof FogSourceBlock
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.REPLACEABLE)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CLIMBABLE)
                || state.is(BlockTags.DOORS)
                || state.is(BlockTags.BANNERS)
                || state.is(BlockTags.SIGNS)
                || state.is(Blocks.JIGSAW)
                || state.is(Blocks.STRUCTURE_BLOCK)
                || state.is(Blocks.STRUCTURE_VOID)) {
            return false;
        }
        return state.canOcclude() || state.is(BlockTags.STAIRS) || state.is(BlockTags.SLABS);
    }

    private static boolean isNaturalDirtOrGrass(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.ROOTED_DIRT);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructureProcessors.STRUCTURE_FOG.get();
    }
}
