package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → Client: Syncs 3D wireframe debug zones for the active dungeon stage & global zones.
 */
public record SyncDungeonDebugZonesPayload(
        boolean enabled,
        List<DebugZoneEntry> zones
) implements CustomPacketPayload {

    public record DebugZoneEntry(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            int argbColor,
            String label
    ) {}

    public static final Type<SyncDungeonDebugZonesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "sync_dungeon_debug_zones"));

    public static final StreamCodec<FriendlyByteBuf, SyncDungeonDebugZonesPayload> STREAM_CODEC =
            StreamCodec.of(SyncDungeonDebugZonesPayload::encode, SyncDungeonDebugZonesPayload::decode);

    private static void encode(FriendlyByteBuf buf, SyncDungeonDebugZonesPayload p) {
        buf.writeBoolean(p.enabled());
        buf.writeInt(p.zones().size());
        for (DebugZoneEntry z : p.zones()) {
            buf.writeDouble(z.minX());
            buf.writeDouble(z.minY());
            buf.writeDouble(z.minZ());
            buf.writeDouble(z.maxX());
            buf.writeDouble(z.maxY());
            buf.writeDouble(z.maxZ());
            buf.writeInt(z.argbColor());
            buf.writeUtf(z.label());
        }
    }

    private static SyncDungeonDebugZonesPayload decode(FriendlyByteBuf buf) {
        boolean enabled = buf.readBoolean();
        int size = buf.readInt();
        List<DebugZoneEntry> zones = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            double minX = buf.readDouble();
            double minY = buf.readDouble();
            double minZ = buf.readDouble();
            double maxX = buf.readDouble();
            double maxY = buf.readDouble();
            double maxZ = buf.readDouble();
            int color = buf.readInt();
            String label = buf.readUtf();
            zones.add(new DebugZoneEntry(minX, minY, minZ, maxX, maxY, maxZ, color, label));
        }
        return new SyncDungeonDebugZonesPayload(enabled, zones);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

