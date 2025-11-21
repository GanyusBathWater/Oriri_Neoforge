package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.BoundCelestialEffects;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class BoundOfTheCelestialSisters extends VestigeItem {

    public BoundOfTheCelestialSisters(Item.Properties props) {
        super(props, List.of(
                // Level 1: Tag/Nacht-Elementarresistenz
                List.of(BoundCelestialEffects.elementalDayNightResistance()),
                // Level 2: Tag/Nacht-Mana-Bonus
                List.of(BoundCelestialEffects.manaDayNightBonus()),
                // Level 3: Tag/Nacht-Kampf-Bonus
                List.of(BoundCelestialEffects.combatDayNightBonus())
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
        return "tooltip.oririmod.vestige.bound_of_the_celestial_sisters";
    }
}