package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.ChatFormatting;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ChargedProjectiles;

public class ArbiterCrossbowItem extends CustomCrossbowItem {
    public ArbiterCrossbowItem(Properties properties, ModRarity rarity) {
        super(properties, rarity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        int currentLevel = data.copyTag().contains("oriri_level") ? data.copyTag().getInt("oriri_level") : 1;
        net.ganyusbathwater.oririmod.util.TooltipHelper.addAbility(tooltipComponents, "tooltip.oririmod.arbiter_crossbow.ability");
        if (currentLevel > 1) {
            net.ganyusbathwater.oririmod.util.TooltipHelper.addEmptyLine(tooltipComponents);
            net.ganyusbathwater.oririmod.util.TooltipHelper.addLevelInfo(tooltipComponents, "item.oririmod.arbiter_crossbow", currentLevel);
        }
        net.ganyusbathwater.oririmod.util.TooltipHelper.addLore(tooltipComponents, "tooltip.oririmod.arbiter_crossbow.lore");
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        super.releaseUsing(stack, level, entity, timeLeft);
        
        ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
        if (charged != null && !charged.isEmpty()) {
            CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            int currentLevel = data.copyTag().contains("oriri_level") ? data.copyTag().getInt("oriri_level") : 1;
            
            int targetArrows = 1;
            if (currentLevel == 2) targetArrows = 2;
            else if (currentLevel == 3) targetArrows = 3;
            
            if (targetArrows > 1) {
                ItemStack firstProjectile = charged.getItems().get(0);
                List<ItemStack> newProjectiles = new java.util.ArrayList<>();
                for (int i = 0; i < targetArrows; i++) {
                    newProjectiles.add(firstProjectile.copy());
                }
                stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(newProjectiles));
            }
        }
    }
}
