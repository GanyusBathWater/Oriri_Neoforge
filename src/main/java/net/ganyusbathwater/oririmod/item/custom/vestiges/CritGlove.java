package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.AttributeVestigeEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.DuellantCortextEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

public class CritGlove extends VestigeItem {

    public CritGlove(Properties props) {
        super(props, List.of(
                List.of(new AttributeVestigeEffect(Attributes.ATTACK_SPEED, 0.25)),
                List.of(new AttributeVestigeEffect(Attributes.ATTACK_SPEED, 0.5)),
                List.of(new AttributeVestigeEffect(Attributes.ATTACK_SPEED, 0.75))
        ), ModRarity.RARE);
    }
}
