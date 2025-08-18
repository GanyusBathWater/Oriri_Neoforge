package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.*;

import java.util.List;

public class CustomSwordItem extends SwordItem implements ModRarityCarrier {
    private final ModRarity rarity;

    //Konstruktor Mathode
    public CustomSwordItem(Tier tier, Properties properties, ModRarity rarity) {
        super(tier, properties);
        this.rarity = rarity;
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }
}
