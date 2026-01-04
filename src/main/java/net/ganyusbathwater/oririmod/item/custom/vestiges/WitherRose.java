package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.ImmunityEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.WitherRoseEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

public class WitherRose extends VestigeItem {

    public WitherRose(Properties props) {
        super(props, List.of(
                List.of(new ImmunityEffect(MobEffects.WITHER)),
                List.of(new WitherRoseEffect(2)),
                List.of(new WitherRoseEffect(3))
        ), ModRarity.MYTHIC);
    }
}