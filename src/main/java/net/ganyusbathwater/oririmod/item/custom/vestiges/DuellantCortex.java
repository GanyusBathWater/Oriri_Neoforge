// language: java
package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.DuellantCortexEffects;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class DuellantCortex extends VestigeItem {

    public DuellantCortex(Item.Properties props) {
        super(props, List.of(
                // Level 1: 10 % Armor pro feindlichem Mob
                List.of(DuellantCortexEffects.nearbyHostileScaling(12)),
                // Level 2: + 5 % Damage pro feindlichem Mob zusätzlich
                List.of(DuellantCortexEffects.nearbyHostileScaling(12)),
                // Level 3: 15 % Armor / 7.5 % Damage pro feindlichem Mob
                List.of(DuellantCortexEffects.nearbyHostileScaling(12))
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
        return "tooltip.oririmod.vestige.duellant_cortex";
    }
}