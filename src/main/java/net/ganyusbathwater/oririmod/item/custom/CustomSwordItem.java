package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

public class CustomSwordItem extends SwordItem implements ModRarityCarrier {
    private final ModRarity rarity;

    public CustomSwordItem(Tier tier, Properties properties, ModRarity rarity) {
        super(tier, properties);
        this.rarity = rarity;
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }
}
