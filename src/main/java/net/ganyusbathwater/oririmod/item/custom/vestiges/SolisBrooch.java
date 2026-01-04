package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.ManaModifierEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;

import java.util.List;

public class SolisBrooch extends VestigeItem {

    public SolisBrooch(Properties props) {
        super(props, List.of(
                List.of(ManaModifierEffect.bonusMaxMana(50)),
                List.of(ManaModifierEffect.bonusMaxMana(50)),
                List.of(ManaModifierEffect.regenIntervalSeconds(1))
        ), ModRarity.MYTHIC);
    }
}