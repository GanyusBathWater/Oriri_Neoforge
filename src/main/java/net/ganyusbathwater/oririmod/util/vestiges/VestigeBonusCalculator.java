package net.ganyusbathwater.oririmod.util.vestiges;

import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class VestigeBonusCalculator {

    private VestigeBonusCalculator() {}

    public static VestigeBonusSnapshot calculate(ServerPlayer player) {
        double healthBonus = 0.0D;
        float stepBonus = 0.0F;
        float LuckBonus = 0.0F;

        for (ItemStack stack : ExtraInventoryUtil.readExtraInventory(player)) {
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof VestigeItem vestige)) continue;

            // Tick-Effekte ausführen (Potions, OreSense, usw.)
            vestige.applyTick(player, stack);

            // Boni summieren
            healthBonus += vestige.sumHealthBonus(player, stack);
            stepBonus += vestige.sumStepHeightBonus(player, stack);
            LuckBonus += vestige.sumLuckBonus(player, stack);
        }

        if (healthBonus == 0.0D && stepBonus == 0.0F && LuckBonus == 0.0F) {
            return VestigeBonusSnapshot.empty();
        }
        return new VestigeBonusSnapshot(healthBonus, stepBonus, LuckBonus);
    }
}
