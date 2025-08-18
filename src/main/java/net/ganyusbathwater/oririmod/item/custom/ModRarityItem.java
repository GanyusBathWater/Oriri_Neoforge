package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ModRarityItem extends Item {
    private final ModRarity rarity;

    public ModRarityItem(Properties properties, ModRarity rarity) {
        super(properties);
        this.rarity = rarity;
    }

    public ModRarity getModRarity() {
        return rarity;
    }
}
