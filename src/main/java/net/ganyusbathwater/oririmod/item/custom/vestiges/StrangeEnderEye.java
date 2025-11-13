package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.KeepInventoryOnDeathEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.MobEffectBonus;
import net.ganyusbathwater.oririmod.effect.vestiges.StepHeightBonusEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffects;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Stufe 1: permanente Nachtsicht
 * Stufe 2: Erz-Erkennung (Partikel) im Radius 16
 * Stufe 3: Mob-Erkennung (Glühen) im Radius 16
 */
public class StrangeEnderEye extends VestigeItem {
    public StrangeEnderEye(Properties props) {
        super(props, List.of(
                List.of(VestigeEffects.mobEffect(MobEffects.NIGHT_VISION, 0, 260)),
                List.of(VestigeEffects.oreSense(16)),
                List.of(VestigeEffects.mobSense(16))
        ));
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        this.setUnlockedLevel(stack, this.getMaxLevel());
        return stack;
    }

    // Bestehende Stacks: einmalig nachziehen (serverseitig)
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
        if (level.isClientSide) return;
        if (this.getUnlockedLevel(stack) < this.getMaxLevel()) {
            this.setUnlockedLevel(stack, this.getMaxLevel());
        }
    }
}