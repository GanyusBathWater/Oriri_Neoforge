package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.BoundOfTheCelestialSistersEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.DamageModifierEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.ManaModifierEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;

import java.util.List;

public final class BoundOfTheCelestialSisters extends VestigeItem {

    public BoundOfTheCelestialSisters(Properties props) {
        super(props, List.of(
                List.of(BoundOfTheCelestialSistersEffect.elementalResistance()),
                List.of(ManaModifierEffect.boundCelestialSistersMana()),
                List.of(DamageModifierEffect.boundCelestialSistersCombat())
        ), ModRarity.LEGENDARY);
    }
}