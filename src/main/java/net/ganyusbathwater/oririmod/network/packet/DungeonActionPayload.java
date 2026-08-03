package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Client → Server: player performs an action from the Dungeon Keeper screen.
 *
 * Actions:
 *  - "INVITE"   : leader invites targetPlayerName
 *  - "START"    : leader wants to start the dungeon
 *  - "LEAVE"    : any player leaves the party
 *  - "ACCEPT"   : invited player accepts the invite
 *  - "DECLINE"  : invited player declines the invite
 */
public record DungeonActionPayload(
        String action,
        UUID partyId,
        String targetPlayerName // used for INVITE only, otherwise empty
) implements CustomPacketPayload {

    public static final Type<DungeonActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "dungeon_action"));

    public static final StreamCodec<FriendlyByteBuf, DungeonActionPayload> STREAM_CODEC =
            StreamCodec.of(DungeonActionPayload::encode, DungeonActionPayload::decode);

    private static void encode(FriendlyByteBuf buf, DungeonActionPayload p) {
        buf.writeUtf(p.action());
        buf.writeUUID(p.partyId());
        buf.writeUtf(p.targetPlayerName());
    }

    private static DungeonActionPayload decode(FriendlyByteBuf buf) {
        String action = buf.readUtf();
        UUID partyId = buf.readUUID();
        String targetName = buf.readUtf();
        return new DungeonActionPayload(action, partyId, targetName);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
