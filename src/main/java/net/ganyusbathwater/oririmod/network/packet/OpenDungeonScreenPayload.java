package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server → Client: tells the client to open the Dungeon Keeper screen.
 * Carries all data the screen needs to display without additional queries.
 */
public record OpenDungeonScreenPayload(
        int npcEntityId,
        String dungeonId,
        String dungeonDisplayName,
        String dungeonDescription,
        UUID partyId,
        UUID leaderId,
        List<UUID> memberIds,
        List<String> memberNames,
        List<String> memberStatuses // "PENDING", "ACCEPTED", "DECLINED"
) implements CustomPacketPayload {

    public static final Type<OpenDungeonScreenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "open_dungeon_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenDungeonScreenPayload> STREAM_CODEC =
            StreamCodec.of(OpenDungeonScreenPayload::encode, OpenDungeonScreenPayload::decode);

    private static void encode(FriendlyByteBuf buf, OpenDungeonScreenPayload p) {
        buf.writeInt(p.npcEntityId());
        buf.writeUtf(p.dungeonId());
        buf.writeUtf(p.dungeonDisplayName());
        buf.writeUtf(p.dungeonDescription());
        buf.writeUUID(p.partyId());
        buf.writeUUID(p.leaderId());
        buf.writeInt(p.memberIds().size());
        for (UUID id : p.memberIds()) buf.writeUUID(id);
        for (String name : p.memberNames()) buf.writeUtf(name);
        for (String status : p.memberStatuses()) buf.writeUtf(status);
    }

    private static OpenDungeonScreenPayload decode(FriendlyByteBuf buf) {
        int npcId = buf.readInt();
        String dungeonId = buf.readUtf();
        String displayName = buf.readUtf();
        String description = buf.readUtf();
        UUID partyId = buf.readUUID();
        UUID leaderId = buf.readUUID();
        int count = buf.readInt();
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) ids.add(buf.readUUID());
        List<String> names = new ArrayList<>();
        for (int i = 0; i < count; i++) names.add(buf.readUtf());
        List<String> statuses = new ArrayList<>();
        for (int i = 0; i < count; i++) statuses.add(buf.readUtf());
        return new OpenDungeonScreenPayload(npcId, dungeonId, displayName, description, partyId, leaderId, ids, names, statuses);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
