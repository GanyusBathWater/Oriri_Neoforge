package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.ganyusbathwater.oririmod.effect.vestiges.DuellantCortextEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.ImmunityEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;

import java.util.List;

public class StigmaOfDarkness extends VestigeItem {

    public StigmaOfDarkness(Properties props) {
        super(props, List.of(
                List.of(new ImmunityEffect(ModEffects.BROKEN_EFFECT)),
                List.of(new ImmunityEffect(ModEffects.CHARMED_EFFECT)),
                List.of(new ImmunityEffect(ModEffects.STUNNED_EFFECT))
        ), ModRarity.RARE);
    }
}

