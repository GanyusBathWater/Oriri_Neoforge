package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → Client: Opens the conversation screen.
 * Contains the NPC entity ID and the dialogue tree ID.
 */
public record OpenConversationPayload(
        int entityId,
        String treeId
) implements CustomPacketPayload {

    public static final Type<OpenConversationPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "open_conversation"));

    public static final StreamCodec<FriendlyByteBuf, OpenConversationPayload> STREAM_CODEC =
            StreamCodec.of(OpenConversationPayload::encode, OpenConversationPayload::decode);

    private static void encode(FriendlyByteBuf buf, OpenConversationPayload p) {
        buf.writeVarInt(p.entityId());
        buf.writeUtf(p.treeId(), 256); // 256 chars max for tree ID
    }

    private static OpenConversationPayload decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        String treeId = buf.readUtf(256);
        return new OpenConversationPayload(entityId, treeId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
