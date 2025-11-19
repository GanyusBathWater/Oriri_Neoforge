// language: java
package net.ganyusbathwater.oririmod.events.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.menu.ExtraInventoryMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class KeepInventoryEvents {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getOriginal() instanceof Player original)) return;
        if (!(event.getEntity() instanceof Player clone)) return;

        boolean keepRule = original.level()
                .getGameRules()
                .getBoolean(GameRules.RULE_KEEPINVENTORY);

        if (keepRule) {
            // keepInventory == true -> Extra-Inventar übernehmen
            copyExtraInventory(original, clone);
        } else {
            // keepInventory == false -> Extra-Inventar des alten Spielers löschen
            removeExtraInventory(original);
        }
    }

    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        boolean keepRule = player.level()
                .getGameRules()
                .getBoolean(GameRules.RULE_KEEPINVENTORY);

        if (keepRule) {
            // keepInventory == true -> Extra-Inventar nicht droppen
            return;
        }

        // keepInventory == false -> Extra-Inventar droppen und löschen
        dropExtraInventory(player, event);
    }

    private static void copyExtraInventory(Player from, Player to) {
        CompoundTag fromRoot = from.getPersistentData();
        if (!fromRoot.contains(ExtraInventoryMenu.NBT_KEY)) return;

        CompoundTag invTag = fromRoot.getCompound(ExtraInventoryMenu.NBT_KEY);
        CompoundTag toRoot = to.getPersistentData();
        toRoot.put(ExtraInventoryMenu.NBT_KEY, invTag.copy());
    }

    private static void removeExtraInventory(Player player) {
        player.getPersistentData().remove(ExtraInventoryMenu.NBT_KEY);
    }

    private static void dropExtraInventory(Player player, LivingDropsEvent event) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(ExtraInventoryMenu.NBT_KEY)) return;

        CompoundTag invTag = root.getCompound(ExtraInventoryMenu.NBT_KEY);
        NonNullList<ItemStack> list = NonNullList.withSize(ExtraInventoryMenu.SIZE, ItemStack.EMPTY);
        var lookup = player.level().registryAccess();
        ContainerHelper.loadAllItems(invTag, list, lookup);

        for (ItemStack stack : list) {
            if (stack.isEmpty()) continue;
            ItemEntity entity = new ItemEntity(
                    player.level(),
                    player.getX(),
                    player.getY() + 0.5D,
                    player.getZ(),
                    stack
            );
            event.getDrops().add(entity);
        }

        // Nach dem Droppen Extra-Inventar aus NBT entfernen
        root.remove(ExtraInventoryMenu.NBT_KEY);
    }
}