package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.item.custom.magic.ParticleDebugWandItem.ParticleEffectType;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * SelectParticleEffectPayload — sent from client → server when the player
 * picks an effect in {@link net.ganyusbathwater.oririmod.client.screen.ParticleSelectionScreen}.
 *
 * <p>The server handler writes the chosen {@link ParticleEffectType} into the
 * wand's CustomData component so it persists through save/load and syncs to
 * other clients via normal ItemStack synchronisation.</p>
 */
public record SelectParticleEffectPayload(ParticleEffectType effectType)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectParticleEffectPayload> TYPE =
            new CustomPacketPayload.Type<>(NetworkHandler.SELECT_PARTICLE_EFFECT);

    public static final StreamCodec<FriendlyByteBuf, SelectParticleEffectPayload> STREAM_CODEC =
            StreamCodec.of(
                    SelectParticleEffectPayload::encode,
                    SelectParticleEffectPayload::decode
            );

    private static void encode(FriendlyByteBuf buf, SelectParticleEffectPayload payload) {
        buf.writeEnum(payload.effectType());
    }

    private static SelectParticleEffectPayload decode(FriendlyByteBuf buf) {
        return new SelectParticleEffectPayload(buf.readEnum(ParticleEffectType.class));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
