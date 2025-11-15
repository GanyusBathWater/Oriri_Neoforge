// java
package net.ganyusbathwater.oririmod.util.vestiges;

import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class VestigeManager {

    public static void tick(ServerPlayer player) {
        // 1) Boni berechnen
        VestigeBonusSnapshot bonuses = VestigeBonusCalculator.calculate(player);

        // 2) Attribute anwenden
        VestigeAttributeApplier.apply(player, bonuses);
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