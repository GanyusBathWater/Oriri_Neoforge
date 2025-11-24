// java
package net.ganyusbathwater.oririmod.util.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.menu.ExtraInventoryMenu;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class ExtraInventoryInit {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag root = player.getPersistentData();
        if (!root.contains(ExtraInventoryMenu.NBT_KEY)) {
            SimpleContainer cont = new SimpleContainer(ExtraInventoryMenu.SIZE);
            NonNullList<ItemStack> list = NonNullList.withSize(ExtraInventoryMenu.SIZE, ItemStack.EMPTY);
            for (int i = 0; i < ExtraInventoryMenu.SIZE; i++) {
                list.set(i, cont.getItem(i));
            }
            CompoundTag invTag = new CompoundTag();
            var lookup = player.level().registryAccess();
            ContainerHelper.saveAllItems(invTag, list, lookup);
            root.put(ExtraInventoryMenu.NBT_KEY, invTag);
        }

        NonNullList<ItemStack> current = ExtraInventoryUtil.readExtraInventory(player);
        NetworkHandler.sendExtraInventoryTo(player, current);
    }
}