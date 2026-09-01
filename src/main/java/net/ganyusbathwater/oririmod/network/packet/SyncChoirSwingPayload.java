package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncChoirSwingPayload(int playerId) implements CustomPacketPayload {
    public static final Type<SyncChoirSwingPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "sync_choir_swing"));

    public static final StreamCodec<FriendlyByteBuf, SyncChoirSwingPayload> STREAM_CODEC = StreamCodec.ofMember(
            SyncChoirSwingPayload::write,
            SyncChoirSwingPayload::new
    );

    public SyncChoirSwingPayload(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.playerId());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
