// language: java
package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public final class BoundCelestialEffects {

    // --- NBT-Schlüssel für interne Speicherung ---
    private static final String NBT_BASE_MAX_MANA = "OririBaseMaxManaBoundCelestial";
    private static final String NBT_MANA_REGEN_HALVED = "OririManaRegenHalvedBoundCelestial";
    private static final String NBT_ELEMENT_RES_LIGHT = "OririElementResLightBoundCelestial";
    private static final String NBT_ELEMENT_RES_DARK = "OririElementResDarkBoundCelestial";
    private static final String NBT_LAST_IS_DAY = "OririBoundCelestialLastIsDay";

    // Feste IDs für Attribut-Modifikatoren (damit sie nicht stapeln)
    private static final ResourceLocation DEFENSE_DAY_ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "bound_celestial_defense_day");
    private static final ResourceLocation DAMAGE_NIGHT_ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "bound_celestial_damage_night");

    private BoundCelestialEffects() {}

    private static boolean isDay(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        return level.isDay() && !level.isThundering();
    }

    // --- Hilfsfunktionen für Element-Resistenz (Level 1) ---

    private static void setElementResistance(ServerPlayer player, Element element, double value) {
        var tag = player.getPersistentData();
        if (element == Element.LIGHT) {
            tag.putDouble(NBT_ELEMENT_RES_LIGHT, value);
        } else if (element == Element.DARKNESS) {
            tag.putDouble(NBT_ELEMENT_RES_DARK, value);
        }
    }

    public static double getElementResistance(ServerPlayer player, Element element) {
        var tag = player.getPersistentData();
        if (element == Element.LIGHT) {
            return tag.contains(NBT_ELEMENT_RES_LIGHT) ? tag.getDouble(NBT_ELEMENT_RES_LIGHT) : 0.0D;
        }
        if (element == Element.DARKNESS) {
            return tag.contains(NBT_ELEMENT_RES_DARK) ? tag.getDouble(NBT_ELEMENT_RES_DARK) : 0.0D;
        }
        return 0.0D;
    }

    // --- Level 1: Elementare Resistenz Tag/Nacht ---

    public static VestigeEffect elementalDayNightResistance() {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int level) {
                boolean day = isDay(player);

                if (day) {
                    // Tag: 25 % Resistenz gegen DARKNESS, keine gegen LIGHT
                    setElementResistance(player, Element.DARKNESS, 0.25D);
                    setElementResistance(player, Element.LIGHT, 0.0D);
                } else {
                    // Nacht: 25 % Resistenz gegen LIGHT, keine gegen DARKNESS
                    setElementResistance(player, Element.LIGHT, 0.25D);
                    setElementResistance(player, Element.DARKNESS, 0.0D);
                }
            }
        };
    }

    // --- Level 2: Mana-Boni Tag/Nacht ---
    public static VestigeEffect manaDayNightBonus() {
        return new VestigeEffect() {

            private static final int BONUS_MAX_MANA = 50;
            private static final String NBT_LAST_IS_DAY = "OririBoundCelestialLastIsDay";

            @Override
            public void tick(ServerPlayer player, ItemStack stack, int level) {
                boolean dayNow = isDay(player);
                var data = player.getPersistentData();

                // Erstes Mal: nur Zustand merken, nichts ändern
                if (!data.contains(NBT_LAST_IS_DAY)) {
                    data.putBoolean(NBT_LAST_IS_DAY, dayNow);
                    return;
                }

                boolean lastWasDay = data.getBoolean(NBT_LAST_IS_DAY);

                // Kein Wechsel -> nichts tun
                if (lastWasDay == dayNow) {
                    return;
                }

                // Wechsel Tag <-> Nacht
                data.putBoolean(NBT_LAST_IS_DAY, dayNow);

                int currentMax = ModManaUtil.getMaxMana(player);

                if (dayNow) {
                    // Nacht -> Tag: +50
                    ModManaUtil.setMaxMana(player, currentMax + BONUS_MAX_MANA);
                    // Regeneration auf aktuellen Basiswert zurücksetzen
                    ModManaUtil.resetRegenIntervalSeconds(player);
                } else {
                    // Tag -> Nacht: -50
                    ModManaUtil.setMaxMana(player, currentMax - BONUS_MAX_MANA);
                    // Nachts: Regeneration beschleunigen (Intervall halbieren)
                    int baseInterval = ModManaUtil.getRegenIntervalSeconds(player);
                    int faster = Math.max(1, baseInterval / 2);
                    ModManaUtil.setRegenIntervalSeconds(player, faster);
                }
            }

            @Override
            public void onRemovedFromExtraInventory(ServerPlayer player, ItemStack stack, int level) {
                var data = player.getPersistentData();

                if (!data.contains(NBT_LAST_IS_DAY)) {
                    return;
                }

                boolean lastWasDay = data.getBoolean(NBT_LAST_IS_DAY);
                int currentMax = ModManaUtil.getMaxMana(player);

                // Wenn wir zuletzt am Tag waren, war der letzte Schritt "+50".
                // Beim Entfernen ziehen wir wieder 50 ab.
                // Wenn wir zuletzt in der Nacht waren, war der letzte Schritt "-50".
                // Beim Entfernen geben wir wieder 50 dazu.
                if (lastWasDay) {
                    ModManaUtil.setMaxMana(player, currentMax - BONUS_MAX_MANA);
                } else {
                    ModManaUtil.setMaxMana(player, currentMax + BONUS_MAX_MANA);
                }

                // Regenerationsintervall auf Basis zurücksetzen
                ModManaUtil.resetRegenIntervalSeconds(player);

                // Interne Marker optional löschen
                data.remove(NBT_LAST_IS_DAY);
            }
        };
    }

    // --- Level 3: Verteidigung/Schaden Tag/Nacht ---

    public static VestigeEffect combatDayNightBonus() {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int level) {
                boolean day = isDay(player);

                AttributeInstance armorInst = player.getAttribute(Attributes.ARMOR);
                AttributeInstance attackInst = player.getAttribute(Attributes.ATTACK_DAMAGE);

                if (armorInst == null || attackInst == null) {
                    return;
                }

                // Alte Modifikatoren immer entfernen, damit nichts stapelt
                AttributeModifier oldDefense = armorInst.getModifier(DEFENSE_DAY_ID);
                if (oldDefense != null) {
                    armorInst.removeModifier(oldDefense);
                }
                AttributeModifier oldDamage = attackInst.getModifier(DAMAGE_NIGHT_ID);
                if (oldDamage != null) {
                    attackInst.removeModifier(oldDamage);
                }

                if (day) {
                    // Tag: +25 % Verteidigung (Armor)
                    double baseArmor = armorInst.getBaseValue();
                    double bonusArmor = baseArmor * 0.25D;
                    AttributeModifier defMod = new AttributeModifier(
                            DEFENSE_DAY_ID,
                            bonusArmor,
                            AttributeModifier.Operation.ADD_VALUE
                    );
                    armorInst.addTransientModifier(defMod);
                } else {
                    // Nacht: +25 % Schaden
                    double baseDamage = attackInst.getBaseValue();
                    double bonusDamage = baseDamage * 0.25D;
                    AttributeModifier dmgMod = new AttributeModifier(
                            DAMAGE_NIGHT_ID,
                            bonusDamage,
                            AttributeModifier.Operation.ADD_VALUE
                    );
                    attackInst.addTransientModifier(dmgMod);
                }
            }
        };
    }
}