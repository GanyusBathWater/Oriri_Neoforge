package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncDialoguesPayload(String dialoguesJson) implements CustomPacketPayload {
    public static final Type<SyncDialoguesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "sync_dialogues"));

    public static final StreamCodec<FriendlyByteBuf, SyncDialoguesPayload> STREAM_CODEC = StreamCodec.of(
            SyncDialoguesPayload::encode,
            SyncDialoguesPayload::decode
    );

    private static void encode(FriendlyByteBuf buf, SyncDialoguesPayload p) {
        buf.writeUtf(p.dialoguesJson(), 1048576);
    }

    private static SyncDialoguesPayload decode(FriendlyByteBuf buf) {
        return new SyncDialoguesPayload(buf.readUtf(1048576));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
