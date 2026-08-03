package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Server → Client: instructs the client to play/stop dungeon music.
 */
public record PlayDungeonMusicPayload(
        @Nullable ResourceLocation track, // The track to play. If null, stops dungeon music.
        boolean stopAll, // If true, forces all other currently playing tracks to stop.
        boolean looping  // If true, the track will loop.
) implements CustomPacketPayload {

    public static final Type<PlayDungeonMusicPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "play_dungeon_music"));

    public static final StreamCodec<FriendlyByteBuf, PlayDungeonMusicPayload> STREAM_CODEC =
            StreamCodec.of(PlayDungeonMusicPayload::encode, PlayDungeonMusicPayload::decode);

    private static void encode(FriendlyByteBuf buf, PlayDungeonMusicPayload p) {
        if (p.track() == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeResourceLocation(p.track());
        }
        buf.writeBoolean(p.stopAll());
        buf.writeBoolean(p.looping());
    }

    private static PlayDungeonMusicPayload decode(FriendlyByteBuf buf) {
        boolean hasTrack = buf.readBoolean();
        ResourceLocation track = hasTrack ? buf.readResourceLocation() : null;
        boolean stopAll = buf.readBoolean();
        boolean looping = buf.readBoolean();
        return new PlayDungeonMusicPayload(track, stopAll, looping);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
