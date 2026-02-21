package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class JadeShieldItem extends ShieldItem implements ModRarityCarrier {
    private final ModRarity rarity;

    public JadeShieldItem(Properties properties, ModRarity rarity) {
        super(properties);
        this.rarity = rarity;
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }

    @Override
    public boolean isValidRepairItem(ItemStack pStack, ItemStack pRepairCandidate) {
        return pRepairCandidate.is(ModBlocks.JADE_BLOCK.get().asItem())
                || super.isValidRepairItem(pStack, pRepairCandidate);
    }

    @Override
    public List<Component> buildModTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag flag) {
        List<Component> list = ModRarityCarrier.super.buildModTooltip(stack, context, flag);
        list.add(Component.translatable("item.oririmod.jade_shield.tooltip")
                .withStyle(net.minecraft.ChatFormatting.GREEN));
        return list;
    }

    @Override
    public String getDescriptionId(ItemStack pStack) {
        return "item.oririmod.jade_shield";
    }

}
