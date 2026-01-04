package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.AttributeVestigeEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.PotionVestigeEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

public class Spring extends VestigeItem {

    public Spring(Properties props) {
        super(props, List.of(
                List.of(new PotionVestigeEffect(MobEffects.JUMP, 0)),
                List.of(new AttributeVestigeEffect(Attributes.STEP_HEIGHT, 1)),
                List.of(new AttributeVestigeEffect(Attributes.SAFE_FALL_DISTANCE, 999999999))
        ), ModRarity.UNCOMMON);
    }
}
