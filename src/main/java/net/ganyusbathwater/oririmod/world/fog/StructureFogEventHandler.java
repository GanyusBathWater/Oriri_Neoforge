package net.ganyusbathwater.oririmod.world.fog;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.attachment.ModAttachments;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles syncing chunk-attached {@link FogAnchor}s to watching clients and event-driven
 * structural demolition cleanup (zero per-tick polling overhead).
 * <p>
 * When players place blocks/torches on top of a structure fog anchor, nothing is disturbed.
 * When players demolish the surrounding structure (breaking the floor and &ge;70% of the anchor's
 * captured structural support blocks, or &ge;80% of supports overall), the anchor automatically
 * deletes itself and syncs the removal to nearby players.
 */
@EventBusSubscriber(modid = OririMod.MOD_ID)
public final class StructureFogEventHandler {

    private static final int SUPPORT_CHECK_MARGIN = 5;

    private StructureFogEventHandler() {
    }

    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        LevelChunk chunk = event.getChunk();
        if (!chunk.hasData(ModAttachments.FOG_ANCHORS)) {
            return;
        }
        ChunkFogAnchors data = chunk.getData(ModAttachments.FOG_ANCHORS);
        if (!data.isEmpty()) {
            ChunkPos pos = chunk.getPos();
            NetworkHandler.sendChunkFogAnchorsToPlayer(event.getPlayer(), pos.x, pos.z, data.toClientViews());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ChunkPos pos = event.getChunk().getPos();
            ClientFogAnchorCache.removeChunk(pos.x, pos.z);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos brokenPos = event.getPos();
        Set<BlockPos> brokenSet = Set.of(brokenPos);
        int minCx = (brokenPos.getX() - SUPPORT_CHECK_MARGIN) >> 4;
        int maxCx = (brokenPos.getX() + SUPPORT_CHECK_MARGIN) >> 4;
        int minCz = (brokenPos.getZ() - SUPPORT_CHECK_MARGIN) >> 4;
        int maxCz = (brokenPos.getZ() + SUPPORT_CHECK_MARGIN) >> 4;

        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                validateChunkAnchors(serverLevel, cx, cz, brokenSet);
            }
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        List<BlockPos> affected = event.getAffectedBlocks();
        if (affected.isEmpty()) {
            return;
        }

        Set<BlockPos> brokenSet = new HashSet<>(affected);
        Set<Long> chunksToCheck = new HashSet<>();
        for (int i = 0; i < affected.size(); i++) {
            BlockPos p = affected.get(i);
            int minCx = (p.getX() - SUPPORT_CHECK_MARGIN) >> 4;
            int maxCx = (p.getX() + SUPPORT_CHECK_MARGIN) >> 4;
            int minCz = (p.getZ() - SUPPORT_CHECK_MARGIN) >> 4;
            int maxCz = (p.getZ() + SUPPORT_CHECK_MARGIN) >> 4;
            for (int cx = minCx; cx <= maxCx; cx++) {
                for (int cz = minCz; cz <= maxCz; cz++) {
                    chunksToCheck.add(ChunkPos.asLong(cx, cz));
                }
            }
        }

        for (long key : chunksToCheck) {
            validateChunkAnchors(serverLevel, ChunkPos.getX(key), ChunkPos.getZ(key), brokenSet);
        }
    }

    private static void validateChunkAnchors(ServerLevel level, int cx, int cz, Set<BlockPos> newlyBroken) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null || !chunk.hasData(ModAttachments.FOG_ANCHORS)) {
            return;
        }
        ChunkFogAnchors data = chunk.getData(ModAttachments.FOG_ANCHORS);
        if (data.isEmpty()) {
            return;
        }

        List<FogAnchor> anchors = data.getAnchors();
        List<FogAnchor> demolished = null;

        for (int i = 0; i < anchors.size(); i++) {
            FogAnchor anchor = anchors.get(i);
            List<BlockPos> supports = anchor.supportBlocks();
            if (supports.isEmpty()) {
                continue;
            }

            // Quick relevance check: only re-evaluate if a broken block is one of this anchor's supports
            // or directly underneath its origin
            BlockPos floorPos = anchor.origin().below();
            boolean touched = newlyBroken.contains(floorPos);
            if (!touched) {
                for (int s = 0; s < supports.size(); s++) {
                    if (newlyBroken.contains(supports.get(s))) {
                        touched = true;
                        break;
                    }
                }
            }
            if (!touched) {
                continue;
            }

            int total = supports.size();
            int intact = 0;
            for (int s = 0; s < total; s++) {
                BlockPos sp = supports.get(s);
                if (!newlyBroken.contains(sp) && isIntactStructureSupport(level, sp)) {
                    intact++;
                }
            }

            boolean floorIntact = !newlyBroken.contains(floorPos) && isIntactStructureSupport(level, floorPos);
            float intactRatio = (float) intact / (float) total;

            // Demolished when the structure around this emitter is torn down:
            // - Either floor directly beneath is gone AND <= 30% of support blocks remain
            // - Or <= 20% of support blocks remain overall (even if a single floor block is left)
            boolean isDemolished = (!floorIntact && intactRatio <= 0.30F) || intactRatio <= 0.20F;
            if (isDemolished) {
                if (demolished == null) {
                    demolished = new ArrayList<>();
                }
                demolished.add(anchor);
            }
        }

        if (demolished != null && data.removeAll(demolished)) {
            chunk.setUnsaved(true);
            NetworkHandler.sendChunkFogAnchorsToTracking(level, new ChunkPos(cx, cz), data.toClientViews());
        }
    }

    private static boolean isIntactStructureSupport(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return true; // Assume intact if neighbor chunk isn't loaded yet
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty() || state.is(BlockTags.REPLACEABLE)) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }
}
