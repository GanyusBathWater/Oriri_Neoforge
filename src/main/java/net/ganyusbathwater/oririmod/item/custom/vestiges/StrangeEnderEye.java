// java
package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.MobSenseEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.PotionVestigeEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

public class StrangeEnderEye extends VestigeItem {

    public StrangeEnderEye(Properties props) {
        super(props, List.of(
                List.of(new PotionVestigeEffect(MobEffects.NIGHT_VISION, 0)),
                List.of(new MobSenseEffect(12)),
                List.of(new MobSenseEffect(24))
        ), ModRarity.RARE);
    }
}