// java
package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class HeartOfTheTank extends VestigeItem {

    public HeartOfTheTank(Properties props) {
        super(props, List.of(
                List.of(totalHealthBonus()),
                List.of(totalHealthBonus()),
                List.of(totalHealthBonus())
        ));
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        this.setUnlockedLevel(stack, this.getMaxLevel());
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
        if (level.isClientSide) return;
        if (this.getUnlockedLevel(stack) < this.getMaxLevel()) {
            this.setUnlockedLevel(stack, this.getMaxLevel());
        }
    }

    private static VestigeEffect totalHealthBonus() {
        return new VestigeEffect() {
            // Kein Tick mehr nötig – Health-Bonus wird zentral im VestigeManager gesetzt
            @Override
            public double healthBonus(net.minecraft.server.level.ServerPlayer player,
                                      ItemStack stack,
                                      int lvl) {
                // lvl entspricht hier der freigeschalteten Stufe (1..3)
                return switch (lvl) {
                    case 1 -> 4.0D;  // 4 Herzen
                    case 2 -> 6.0D; // 6 Herzen
                    case 3 -> 10.0D; // 10 Herzen
                    default -> 0.0D;
                };
            }
        };
    }

    @Override
    public String getTranslationKeyBase() {
        return "tooltip.oririmod.vestige.heart_of_the_tank";
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
    }
}