package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → Client: Opens the Dungeon Selection Screen.
 * Contains a list of all available dungeons with their display data.
 */
public record OpenDungeonSelectionPayload(
        List<String> dungeonIds,
        List<String> displayNames,
        List<String> descriptions,
        List<String> loreTexts,
        List<String> previewTextures // ResourceLocation string or empty
) implements CustomPacketPayload {

    public static final Type<OpenDungeonSelectionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "open_dungeon_selection"));

    public static final StreamCodec<FriendlyByteBuf, OpenDungeonSelectionPayload> STREAM_CODEC =
            StreamCodec.of(OpenDungeonSelectionPayload::encode, OpenDungeonSelectionPayload::decode);

    private static void encode(FriendlyByteBuf buf, OpenDungeonSelectionPayload p) {
        buf.writeInt(p.dungeonIds().size());
        for (int i = 0; i < p.dungeonIds().size(); i++) {
            buf.writeUtf(p.dungeonIds().get(i));
            buf.writeUtf(p.displayNames().get(i));
            buf.writeUtf(p.descriptions().get(i));
            buf.writeUtf(p.loreTexts().get(i));
            buf.writeUtf(p.previewTextures().get(i));
        }
    }

    private static OpenDungeonSelectionPayload decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<String> ids = new ArrayList<>(size);
        List<String> names = new ArrayList<>(size);
        List<String> descs = new ArrayList<>(size);
        List<String> lores = new ArrayList<>(size);
        List<String> previews = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buf.readUtf());
            names.add(buf.readUtf());
            descs.add(buf.readUtf());
            lores.add(buf.readUtf());
            previews.add(buf.readUtf());
        }
        return new OpenDungeonSelectionPayload(ids, names, descs, lores, previews);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
