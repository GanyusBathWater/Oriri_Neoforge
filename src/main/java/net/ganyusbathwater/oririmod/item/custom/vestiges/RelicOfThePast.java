package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.RelicOfThePastEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;

import java.util.List;

public class RelicOfThePast extends VestigeItem {

    public RelicOfThePast(Properties props) {
        super(props, List.of(
                List.of(new RelicOfThePastEffect(RelicOfThePastEffect.COOLDOWN_SECONDS)),
                List.of(new RelicOfThePastEffect(RelicOfThePastEffect.COOLDOWN_SECONDS)),
                List.of(new RelicOfThePastEffect(RelicOfThePastEffect.COOLDOWN_SECONDS))
        ), ModRarity.RARE);
    }
}