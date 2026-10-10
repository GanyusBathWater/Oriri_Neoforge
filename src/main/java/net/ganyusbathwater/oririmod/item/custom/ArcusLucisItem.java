package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.ChatFormatting;
import java.util.List;

public class ArcusLucisItem extends CustomBowItemClass {
    public ArcusLucisItem(Properties properties, ModRarity rarity) {
        super(properties, rarity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        net.ganyusbathwater.oririmod.util.TooltipHelper.addAbility(tooltipComponents, "tooltip.oririmod.arcus_lucis.homing");
        net.ganyusbathwater.oririmod.util.TooltipHelper.addAbility(tooltipComponents, "tooltip.oririmod.arcus_lucis.conversion");
        net.ganyusbathwater.oririmod.util.TooltipHelper.addLore(tooltipComponents, "tooltip.oririmod.arcus_lucis.lore");
    }

}
