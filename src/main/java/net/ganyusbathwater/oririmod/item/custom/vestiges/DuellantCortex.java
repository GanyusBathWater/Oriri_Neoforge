package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.DuellantCortextEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;

import java.util.List;

public class DuellantCortex extends VestigeItem {

    public DuellantCortex(Properties props) {
        super(props, List.of(
                List.of(new DuellantCortextEffect(0.05f, 0.0f)),
                // Bei einem höheren Level muss die Modifikation des vorherigen Levels gleich sein oder höher sonst wird der Wert verringert, weil nur das aktuelle Level mit den darin bestehenden Werten als "aktiv" genommen wird.
                List.of(new DuellantCortextEffect(0.005f, 0.025f)),
                List.of(new DuellantCortextEffect(0.075f, 0.05f))
        ), ModRarity.RARE);
    }
}