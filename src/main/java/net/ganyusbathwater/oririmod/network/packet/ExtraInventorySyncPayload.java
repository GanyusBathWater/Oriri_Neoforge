// java
package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record ExtraInventorySyncPayload(NonNullList<ItemStack> items) implements CustomPacketPayload {

    public static final Type<ExtraInventorySyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "extra_inventory_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtraInventorySyncPayload> STREAM_CODEC =
            StreamCodec.of(ExtraInventorySyncPayload::encode, ExtraInventorySyncPayload::decode);

    @Override
    public Type<ExtraInventorySyncPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buf, ExtraInventorySyncPayload payload) {
        buf.writeVarInt(payload.items.size());
        for (ItemStack stack : payload.items) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }

    private static ExtraInventorySyncPayload decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        NonNullList<ItemStack> list = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < size; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            list.set(i, stack);
        }
        return new ExtraInventorySyncPayload(list);
    }
}