package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.PotionVestigeEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.StriderScaleEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

public class StriderScale extends VestigeItem {

    public StriderScale(Properties props) {
        super(props, List.of(
                List.of(new StriderScaleEffect(1)),
                List.of(new StriderScaleEffect(2)),
                List.of(new PotionVestigeEffect(MobEffects.FIRE_RESISTANCE, 0))
        ), ModRarity.RARE);
    }
}