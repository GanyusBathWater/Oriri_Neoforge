package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffects;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class RelicOfThePast extends VestigeItem {

    public RelicOfThePast(Properties props) {
        super(props, List.of(
                List.of(VestigeEffects.relicOfThePastReactive()),
                List.of(VestigeEffects.relicOfThePastReactive()),
                List.of(VestigeEffects.relicOfThePastReactive())
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

    @Override
    public String getTranslationKeyBase() {
        return "tooltip.oririmod.vestige.relic_of_the_past";
    }
}