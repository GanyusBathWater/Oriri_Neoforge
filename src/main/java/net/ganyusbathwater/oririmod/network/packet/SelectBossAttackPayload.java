package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.item.custom.magic.BossAttackDebugWandItem.BossAttackType;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SelectBossAttackPayload(BossAttackType attackType) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectBossAttackPayload> TYPE = new CustomPacketPayload.Type<>(NetworkHandler.SELECT_BOSS_ATTACK);

    public static final StreamCodec<FriendlyByteBuf, SelectBossAttackPayload> STREAM_CODEC = StreamCodec.of(
            SelectBossAttackPayload::encode,
            SelectBossAttackPayload::decode
    );

    private static void encode(FriendlyByteBuf buf, SelectBossAttackPayload payload) {
        buf.writeEnum(payload.attackType());
    }

    private static SelectBossAttackPayload decode(FriendlyByteBuf buf) {
        BossAttackType attackType = buf.readEnum(BossAttackType.class);
        return new SelectBossAttackPayload(attackType);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
