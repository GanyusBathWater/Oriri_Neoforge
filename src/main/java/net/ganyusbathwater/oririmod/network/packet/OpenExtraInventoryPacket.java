package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.menu.ExtraInventoryMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public record OpenExtraInventoryPacket() implements CustomPacketPayload {
    public static final Type<OpenExtraInventoryPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "open_extra_inventory"));
    public static final StreamCodec<FriendlyByteBuf, OpenExtraInventoryPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenExtraInventoryPacket());

    @Override
    public Type<OpenExtraInventoryPacket> type() {
        return TYPE;
    }

    public static void handle(OpenExtraInventoryPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new ExtraInventoryMenu(id, inv),
                        Component.translatable("screen.oririmod.extra_inventory")));
            }
        });
    }
}
