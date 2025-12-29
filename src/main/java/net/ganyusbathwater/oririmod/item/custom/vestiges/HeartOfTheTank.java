package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.AttributeVestigeEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.MobSenseEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.PotionVestigeEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

public class HeartOfTheTank extends VestigeItem {
    public HeartOfTheTank(Properties props) {
        super(props,
                List.of(List.of(new AttributeVestigeEffect(Attributes.MAX_HEALTH, 8.0)),
                        List.of(new AttributeVestigeEffect(Attributes.MAX_HEALTH, 12.0)),
                        List.of(new AttributeVestigeEffect(Attributes.MAX_HEALTH, 20.0))
                ), ModRarity.UNCOMMON);
    }
}
