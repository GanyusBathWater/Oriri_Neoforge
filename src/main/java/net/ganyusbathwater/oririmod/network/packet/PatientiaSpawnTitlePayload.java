package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PatientiaSpawnTitlePayload implements CustomPacketPayload {

    public static final Type<PatientiaSpawnTitlePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "patientia_spawn_title"));

    public static final StreamCodec<FriendlyByteBuf, PatientiaSpawnTitlePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {},
                    buf -> new PatientiaSpawnTitlePayload());

    public PatientiaSpawnTitlePayload() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            net.ganyusbathwater.oririmod.events.ClientEvents.triggerPatientiaTitle();
        });
    }
}
