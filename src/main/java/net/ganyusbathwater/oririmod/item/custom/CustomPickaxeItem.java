package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

import java.util.List;

public class CustomPickaxeItem extends PickaxeItem implements ModRarityCarrier {
    private final ModRarity rarity;

    public CustomPickaxeItem(Tier tier, Properties properties, ModRarity rarity) {
        super(tier, properties);
        this.rarity = rarity;
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }
}
