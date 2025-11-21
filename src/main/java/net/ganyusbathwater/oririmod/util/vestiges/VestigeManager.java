package net.ganyusbathwater.oririmod.util.vestiges;

import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

public class VestigeManager {

    private static final Map<ServerPlayer, NonNullList<ItemStack>> LAST_EXTRA_INVENTORY =
            new IdentityHashMap<>();

    public static void tick(ServerPlayer player) {
        NonNullList<ItemStack> last = LAST_EXTRA_INVENTORY.get(player);
        NonNullList<ItemStack> current = ExtraInventoryUtil.readExtraInventory(player);

        if (last != null) {
            for (int i = 0; i < last.size(); i++) {
                ItemStack oldStack = last.get(i);
                if (oldStack.isEmpty()) continue;

                if (!(oldStack.getItem() instanceof VestigeItem vestigeItem)) continue;

                ItemStack newStack = current.get(i);
                boolean removed =
                        newStack.isEmpty()
                                || !ItemStack.isSameItemSameComponents(oldStack, newStack);

                if (removed) {
                    vestigeItem.applyRemovedFromExtraInventory(player, oldStack);
                }
            }
        }

        VestigeBonusSnapshot bonuses = VestigeBonusCalculator.calculate(player);
        VestigeAttributeApplier.apply(player, bonuses);

        NonNullList<ItemStack> copy = NonNullList.withSize(current.size(), ItemStack.EMPTY);
        for (int i = 0; i < current.size(); i++) {
            ItemStack s = current.get(i);
            copy.set(i, s.isEmpty() ? ItemStack.EMPTY : s.copy());
        }
        LAST_EXTRA_INVENTORY.put(player, copy);
    }

    public static boolean hasKeepInventory(ServerPlayer player) {
        for (ItemStack stack : ExtraInventoryUtil.readExtraInventory(player)) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof VestigeItem vestige) {
                if (vestige.grantsKeepInventory(player, stack)) {
                    return true;
                }
            }
        }
        return false;
    }
}