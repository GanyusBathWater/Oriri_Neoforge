package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.BoundOfTheCelestialSistersEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.DamageModifierEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.ManaModifierEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;

import java.util.List;

public final class BoundOfTheCelestialSisters extends VestigeItem {

    private ModRarity rarity;

    public BoundOfTheCelestialSisters(Properties props) {
        super(props, List.of(
                // 1) Elementare Resistenz
                List.of(BoundOfTheCelestialSistersEffect.elementalResistance()),

                // 2) Mana Modifikation
                List.of(ManaModifierEffect.boundCelestialSistersMana()),

                // 3) Schaden- und Verteidigungsmodifikation
                List.of(DamageModifierEffect.boundCelestialSistersCombat())
        ), ModRarity.LEGENDARY);
    }
}