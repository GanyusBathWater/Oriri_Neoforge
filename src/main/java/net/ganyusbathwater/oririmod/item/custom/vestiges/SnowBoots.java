package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.SnowBootsEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;

import java.util.List;

public class SnowBoots extends VestigeItem {

    public SnowBoots(Properties props) {
        super(props, List.of(
                List.of(new SnowBootsEffect(1)),
                List.of(new SnowBootsEffect(2)),
                List.of(new SnowBootsEffect(3))
        ), ModRarity.UNCOMMON);
    }
}