// java
package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DuellantCortexHostileSyncPayload(int hostileCount) implements CustomPacketPayload {

    public static final Type<DuellantCortexHostileSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "duellant_cortex_hostile_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DuellantCortexHostileSyncPayload> STREAM_CODEC =
            StreamCodec.of(DuellantCortexHostileSyncPayload::encode, DuellantCortexHostileSyncPayload::decode);

    @Override
    public Type<DuellantCortexHostileSyncPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buf, DuellantCortexHostileSyncPayload payload) {
        buf.writeVarInt(payload.hostileCount());
    }

    private static DuellantCortexHostileSyncPayload decode(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        return new DuellantCortexHostileSyncPayload(count);
    }
}