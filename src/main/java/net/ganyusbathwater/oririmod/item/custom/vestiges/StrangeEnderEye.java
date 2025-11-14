// java
package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffects;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

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
        return "tooltip.oririmod.vestige.strange_ender_eye";
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Delegiere an die Implementierung in VestigeItem (die bereits zur ModRarityCarrier-Defaultmethode weiterleitet)
        super.appendHoverText(stack, context, tooltip, flag);
    }
}