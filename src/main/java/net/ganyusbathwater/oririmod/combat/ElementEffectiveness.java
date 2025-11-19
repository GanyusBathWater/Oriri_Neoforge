package net.ganyusbathwater.oririmod.combat;

import java.util.EnumMap;
import java.util.Map;

public final class ElementEffectiveness {

    private static final Map<Element, Map<Element, Double>> MULTIPLIERS = new EnumMap<>(Element.class);

    static {
        // Standard: 1.0 (wird benutzt, wenn nichts eingetragen ist)

        // Fire -> Nature
        addMultiplier(Element.FIRE, Element.NATURE, 1.5);

        // Nature -> Earth
        addMultiplier(Element.NATURE, Element.EARTH, 1.5);

        // Earth -> Water
        addMultiplier(Element.EARTH, Element.WATER, 1.5);

        // Water -> Fire
        addMultiplier(Element.WATER, Element.FIRE, 1.5);

        // Light <-> Darkness
        addMultiplier(Element.LIGHT, Element.DARKNESS, 1.5);
        addMultiplier(Element.DARKNESS, Element.LIGHT, 1.5);
    }

    private ElementEffectiveness() {}

    private static void addMultiplier(Element attacker, Element defender, double value) {
        MULTIPLIERS
                .computeIfAbsent(attacker, k -> new EnumMap<>(Element.class))
                .put(defender, value);
    }

    public static double getMultiplier(Element attacker, Element defender) {
        if (attacker == null || defender == null) {
            return 1.0;
        }
        Map<Element, Double> map = MULTIPLIERS.get(attacker);
        if (map == null) {
            return 1.0;
        }
        return map.getOrDefault(defender, 1.0);
    }
}