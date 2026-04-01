package net.ganyusbathwater.oririmod.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

public class BlackIceScytheItem extends CustomScytheItem {
    public BlackIceScytheItem(Tier tier, Properties properties, net.ganyusbathwater.oririmod.util.ModRarity rarity) {
        super(tier, properties, rarity);
    }

    @Override
    public void applyScytheEffects(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Applies Frost Effect (freezes the entity)
        int minFreezeTicks = 140; // Approx 7 seconds, required to start taking freeze damage is 140 default
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), minFreezeTicks + 100));
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltipComponents, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        tooltipComponents.add(net.minecraft.network.chat.Component.translatable("tooltip.oririmod.scythe.frost"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}

