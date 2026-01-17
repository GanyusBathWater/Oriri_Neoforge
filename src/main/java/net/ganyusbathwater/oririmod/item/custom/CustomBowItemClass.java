package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.world.item.BowItem;

public class CustomBowItemClass extends BowItem implements ModRarityCarrier {
    private final ModRarity rarity;

    public CustomBowItemClass(Properties properties, ModRarity rarity) {
        super(properties);
        this.rarity = rarity;
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }
}
