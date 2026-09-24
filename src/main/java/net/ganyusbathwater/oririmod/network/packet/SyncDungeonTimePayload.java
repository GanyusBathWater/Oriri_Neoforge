package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncDungeonTimePayload(int ticksActive, boolean isComplete, String objectiveText, String progressText) implements CustomPacketPayload {
    public static final Type<SyncDungeonTimePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "sync_dungeon_time"));

    public static final StreamCodec<FriendlyByteBuf, SyncDungeonTimePayload> STREAM_CODEC =
            StreamCodec.of(SyncDungeonTimePayload::encode, SyncDungeonTimePayload::decode);

    private static void encode(FriendlyByteBuf buf, SyncDungeonTimePayload p) {
        buf.writeInt(p.ticksActive());
        buf.writeBoolean(p.isComplete());
        buf.writeUtf(p.objectiveText() != null ? p.objectiveText() : "");
        buf.writeUtf(p.progressText() != null ? p.progressText() : "");
    }

    private static SyncDungeonTimePayload decode(FriendlyByteBuf buf) {
        return new SyncDungeonTimePayload(buf.readInt(), buf.readBoolean(), buf.readUtf(), buf.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
