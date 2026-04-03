package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

public class MoltenScytheItem extends CustomScytheItem {
    public MoltenScytheItem(Tier tier, Properties properties, ModRarity rarity) {
        super(tier, properties, rarity);
    }

    @Override
    public void applyScytheEffects(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Ignite for 5 seconds
        target.igniteForSeconds(5.0f);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltipComponents,
            net.minecraft.world.item.TooltipFlag tooltipFlag) {
        tooltipComponents.add(net.minecraft.network.chat.Component.translatable("tooltip.oririmod.scythe.ignite"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
