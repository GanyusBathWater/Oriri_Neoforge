package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.DamageModifierEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.DuellantCortextEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.PhoenixFeatherEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;

import java.util.List;

public class PhoenixFeather extends VestigeItem {

    public PhoenixFeather(Properties props) {
        super(props, List.of(
                List.of(new PhoenixFeatherEffect(0.1f)),
                List.of(new PhoenixFeatherEffect(0.2f)),
                List.of(new PhoenixFeatherEffect(0.33f))
        ), ModRarity.RARE);
    }
}
