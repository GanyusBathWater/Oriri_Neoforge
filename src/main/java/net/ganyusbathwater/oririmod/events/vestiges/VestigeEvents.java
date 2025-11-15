// java
package net.ganyusbathwater.oririmod.events.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.menu.ExtraInventoryMenu;
import net.ganyusbathwater.oririmod.util.vestiges.VestigeManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class VestigeEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        VestigeManager.tick(sp);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        if (VestigeManager.hasKeepInventory(sp)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getOriginal() instanceof ServerPlayer oldSp)) return;
        if (!(event.getEntity() instanceof ServerPlayer newSp)) return;

        if (!VestigeManager.hasKeepInventory(oldSp)) return;

        var oldInv = oldSp.getInventory();
        var newInv = newSp.getInventory();

        for (int i = 0; i < oldInv.items.size(); i++) {
            ItemStack it = oldInv.items.get(i);
            newInv.items.set(i, it.isEmpty() ? ItemStack.EMPTY : it.copy());
        }
        for (int i = 0; i < oldInv.armor.size(); i++) {
            ItemStack it = oldInv.armor.get(i);
            newInv.armor.set(i, it.isEmpty() ? ItemStack.EMPTY : it.copy());
        }
        for (int i = 0; i < oldInv.offhand.size(); i++) {
            ItemStack it = oldInv.offhand.get(i);
            newInv.offhand.set(i, it.isEmpty() ? ItemStack.EMPTY : it.copy());
        }

        var oldRoot = oldSp.getPersistentData();
        var newRoot = newSp.getPersistentData();
        if (oldRoot.contains(ExtraInventoryMenu.NBT_KEY)) {
            newRoot.put(ExtraInventoryMenu.NBT_KEY, oldRoot.getCompound(ExtraInventoryMenu.NBT_KEY).copy());
        }
    }
}