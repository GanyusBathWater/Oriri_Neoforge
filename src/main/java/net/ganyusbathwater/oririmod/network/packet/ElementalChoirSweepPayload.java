package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ElementalChoirSweepPayload() implements CustomPacketPayload {
    public static final Type<ElementalChoirSweepPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elemental_choir_sweep"));
    
    public static final StreamCodec<FriendlyByteBuf, ElementalChoirSweepPayload> STREAM_CODEC = StreamCodec.ofMember(
        ElementalChoirSweepPayload::write,
        ElementalChoirSweepPayload::new
    );

    public ElementalChoirSweepPayload(FriendlyByteBuf buf) {
        this();
    }

    public void write(FriendlyByteBuf buf) {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
