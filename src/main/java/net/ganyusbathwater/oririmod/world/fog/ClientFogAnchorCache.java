package net.ganyusbathwater.oririmod.world.fog;

import net.minecraft.world.level.ChunkPos;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of chunk-attached structure fog anchors synced from the server.
 * Read by {@code GroundHazeGrid} alongside placed {@code FogSourceBlockEntity} blocks.
 */
public final class ClientFogAnchorCache {

    private static final Map<Long, List<FogAnchor.ClientView>> BY_CHUNK = new ConcurrentHashMap<>();
    private static volatile long version = 0L;

    private ClientFogAnchorCache() {
    }

    public static void updateChunk(int chunkX, int chunkZ, List<FogAnchor.ClientView> anchors) {
        long key = ChunkPos.asLong(chunkX, chunkZ);
        if (anchors == null || anchors.isEmpty()) {
            if (BY_CHUNK.remove(key) != null) {
                version++;
            }
        } else {
            BY_CHUNK.put(key, List.copyOf(anchors));
            version++;
        }
    }

    public static void removeChunk(int chunkX, int chunkZ) {
        if (BY_CHUNK.remove(ChunkPos.asLong(chunkX, chunkZ)) != null) {
            version++;
        }
    }

    public static List<FogAnchor.ClientView> getChunkAnchors(int chunkX, int chunkZ) {
        List<FogAnchor.ClientView> list = BY_CHUNK.get(ChunkPos.asLong(chunkX, chunkZ));
        return list != null ? list : Collections.emptyList();
    }

    public static long getVersion() {
        return version;
    }

    public static void clear() {
        BY_CHUNK.clear();
        version++;
    }
}
