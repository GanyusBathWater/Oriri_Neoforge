package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.AttributeVestigeEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;
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
