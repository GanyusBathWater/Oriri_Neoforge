package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client → Server: The player reached an ACTION node in the dialogue.
 * Contains the NPC entity ID and the action to execute.
 * The server validates proximity before performing the action.
 */
public record ConversationActionPayload(
        int entityId,
        String action // DialogueAction name string
) implements CustomPacketPayload {

    public static final Type<ConversationActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "conversation_action"));

    public static final StreamCodec<FriendlyByteBuf, ConversationActionPayload> STREAM_CODEC =
            StreamCodec.of(ConversationActionPayload::encode, ConversationActionPayload::decode);

    private static void encode(FriendlyByteBuf buf, ConversationActionPayload p) {
        buf.writeVarInt(p.entityId());
        buf.writeUtf(p.action());
    }

    private static ConversationActionPayload decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        String action = buf.readUtf();
        return new ConversationActionPayload(entityId, action);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
