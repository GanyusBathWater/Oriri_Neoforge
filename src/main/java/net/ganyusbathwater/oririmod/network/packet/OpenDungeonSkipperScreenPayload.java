package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → Client: Opens or refreshes the Dungeon Skipper debug screen.
 */
public record OpenDungeonSkipperScreenPayload(
        boolean inDungeon,
        String dungeonId,
        int currentStageIndex,
        int totalStages,
        String currentStageId,
        String currentStageType,
        String currentStageState,
        List<String> allStageIds,
        boolean hasRespawnPos,
        BlockPos respawnPos,
        boolean infiniteLives,
        boolean debugLogging,
        boolean showDebugZones,
        boolean spawnAllSecretsCurrent,
        boolean spawnAllSecretsNext,
        int playerLives,
        String secretsStatusSummary
) implements CustomPacketPayload {

    public static final Type<OpenDungeonSkipperScreenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "open_dungeon_skipper_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenDungeonSkipperScreenPayload> STREAM_CODEC =
            StreamCodec.of(OpenDungeonSkipperScreenPayload::encode, OpenDungeonSkipperScreenPayload::decode);

    private static void encode(FriendlyByteBuf buf, OpenDungeonSkipperScreenPayload p) {
        buf.writeBoolean(p.inDungeon());
        buf.writeUtf(p.dungeonId());
        buf.writeInt(p.currentStageIndex());
        buf.writeInt(p.totalStages());
        buf.writeUtf(p.currentStageId());
        buf.writeUtf(p.currentStageType());
        buf.writeUtf(p.currentStageState());
        buf.writeInt(p.allStageIds().size());
        for (String id : p.allStageIds()) {
            buf.writeUtf(id);
        }
        buf.writeBoolean(p.hasRespawnPos());
        buf.writeBlockPos(p.respawnPos());
        buf.writeBoolean(p.infiniteLives());
        buf.writeBoolean(p.debugLogging());
        buf.writeBoolean(p.showDebugZones());
        buf.writeBoolean(p.spawnAllSecretsCurrent());
        buf.writeBoolean(p.spawnAllSecretsNext());
        buf.writeInt(p.playerLives());
        buf.writeUtf(p.secretsStatusSummary());
    }

    private static OpenDungeonSkipperScreenPayload decode(FriendlyByteBuf buf) {
        boolean inDungeon = buf.readBoolean();
        String dungeonId = buf.readUtf();
        int currentStageIndex = buf.readInt();
        int totalStages = buf.readInt();
        String currentStageId = buf.readUtf();
        String currentStageType = buf.readUtf();
        String currentStageState = buf.readUtf();
        int size = buf.readInt();
        List<String> allStageIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            allStageIds.add(buf.readUtf());
        }
        boolean hasRespawnPos = buf.readBoolean();
        BlockPos respawnPos = buf.readBlockPos();
        boolean infiniteLives = buf.readBoolean();
        boolean debugLogging = buf.readBoolean();
        boolean showDebugZones = buf.readBoolean();
        boolean spawnAllSecretsCurrent = buf.readBoolean();
        boolean spawnAllSecretsNext = buf.readBoolean();
        int playerLives = buf.readInt();
        String secretsStatusSummary = buf.readUtf();
        return new OpenDungeonSkipperScreenPayload(
                inDungeon, dungeonId, currentStageIndex, totalStages,
                currentStageId, currentStageType, currentStageState, allStageIds,
                hasRespawnPos, respawnPos, infiniteLives, debugLogging,
                showDebugZones, spawnAllSecretsCurrent, spawnAllSecretsNext,
                playerLives, secretsStatusSummary
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

