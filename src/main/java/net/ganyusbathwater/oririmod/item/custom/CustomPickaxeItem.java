package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.*;

import java.util.List;

public class CustomPickaxeItem extends PickaxeItem implements ModRarityCarrier {
    private final ModRarity rarity;

    public CustomPickaxeItem(Tier tier, Properties properties, ModRarity rarity) {
        super(tier, properties);
        this.rarity = rarity;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        Component displayName = stack.getHoverName()
                .copy()
                .setStyle(Style.EMPTY.withColor(rarity.getColor()));

        tooltipComponents.add(displayName);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy()
                .setStyle(Style.EMPTY.withColor(rarity.getColor()));
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }
}
