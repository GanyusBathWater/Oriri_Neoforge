package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client → Server: Performs a debugging action from the Dungeon Skipper screen.
 */
public record DungeonSkipperActionPayload(
        String action,
        int targetStageIndex
) implements CustomPacketPayload {

    public static final Type<DungeonSkipperActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "dungeon_skipper_action"));

    public static final StreamCodec<FriendlyByteBuf, DungeonSkipperActionPayload> STREAM_CODEC =
            StreamCodec.of(DungeonSkipperActionPayload::encode, DungeonSkipperActionPayload::decode);

    private static void encode(FriendlyByteBuf buf, DungeonSkipperActionPayload p) {
        buf.writeUtf(p.action());
        buf.writeInt(p.targetStageIndex());
    }

    private static DungeonSkipperActionPayload decode(FriendlyByteBuf buf) {
        String action = buf.readUtf();
        int targetStageIndex = buf.readInt();
        return new DungeonSkipperActionPayload(action, targetStageIndex);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

