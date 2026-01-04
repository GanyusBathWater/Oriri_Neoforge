package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.AttributeVestigeEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.ImmunityEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.PotionVestigeEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

public class MinersLantern extends VestigeItem {

    public MinersLantern(Properties props) {
        super(props, List.of(
                List.of(new PotionVestigeEffect(MobEffects.DIG_SPEED, 0)),
                List.of(new ImmunityEffect(MobEffects.BLINDNESS)),
                List.of(new AttributeVestigeEffect(Attributes.LUCK, 5))
        ), ModRarity.UNCOMMON);
    }
}

