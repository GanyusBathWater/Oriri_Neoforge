package net.ganyusbathwater.oririmod.world.fog;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Per-chunk attachment container storing all structure-generated {@link FogAnchor}s in a chunk.
 * Serialized natively by NeoForge's {@code AttachmentType} on both {@code ProtoChunk} (worldgen)
 * and {@code LevelChunk} (saved chunks).
 */
public final class ChunkFogAnchors {

    public static final Codec<ChunkFogAnchors> CODEC = FogAnchor.CODEC.listOf()
            .xmap(ChunkFogAnchors::new, ChunkFogAnchors::getAnchors);

    private final List<FogAnchor> anchors;

    public ChunkFogAnchors() {
        this.anchors = new ArrayList<>();
    }

    public ChunkFogAnchors(List<FogAnchor> initial) {
        this.anchors = new ArrayList<>(initial);
    }

    public List<FogAnchor> getAnchors() {
        return Collections.unmodifiableList(anchors);
    }

    public boolean isEmpty() {
        return anchors.isEmpty();
    }

    /**
     * Adds or replaces any existing anchor at the same origin position.
     */
    public void putAnchor(FogAnchor anchor) {
        BlockPos origin = anchor.origin();
        for (int i = 0; i < anchors.size(); i++) {
            if (anchors.get(i).origin().equals(origin)) {
                anchors.set(i, anchor);
                return;
            }
        }
        anchors.add(anchor);
    }

    public boolean removeAt(BlockPos origin) {
        return anchors.removeIf(a -> a.origin().equals(origin));
    }

    public boolean removeAll(List<FogAnchor> toRemove) {
        return anchors.removeAll(toRemove);
    }

    public List<FogAnchor.ClientView> toClientViews() {
        if (anchors.isEmpty()) {
            return List.of();
        }
        List<FogAnchor.ClientView> views = new ArrayList<>(anchors.size());
        for (int i = 0; i < anchors.size(); i++) {
            views.add(anchors.get(i).toClientView());
        }
        return views;
    }
}
