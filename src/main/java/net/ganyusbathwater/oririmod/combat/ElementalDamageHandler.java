package net.ganyusbathwater.oririmod.combat;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.effect.vestiges.BoundCelestialEffects;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class ElementalDamageHandler {

    private ElementalDamageHandler() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(ElementalDamageHandler.class);
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();

        Element defenderElement = EntityElementRegistry.getElement(target);
        Element attackerElement = Element.PHYSICAL;

        Entity direct = event.getSource().getDirectEntity();   // z\.B. Pfeil, Fireball
        Entity owner  = event.getSource().getEntity();         // z\.B. Skelett, Spieler

        // 1\. Versuch: Element vom Projektil\-EntityType
        if (direct != null) {
            attackerElement = EntityElementRegistry.getElement(direct);
        }

        // 2\. Wenn Projektil kein spezielles Element hat \-\> Angreifer benutzen
        if (attackerElement == Element.PHYSICAL && owner instanceof LivingEntity attacker) {
            // Versuch über Item
            ItemStack mainHand = attacker.getMainHandItem();
            attackerElement = ItemElementRegistry.getElement(mainHand);

            // Wenn Item auch kein Element hat \-\> Entity\-Element des Angreifers
            if (attackerElement == Element.PHYSICAL) {
                attackerElement = EntityElementRegistry.getElement(attacker);
            }
        }

        float baseDamage = event.getAmount();
        double multiplier = ElementEffectiveness.getMultiplier(attackerElement, defenderElement);
        float newDamage = (float) (baseDamage * multiplier);

        if (target instanceof ServerPlayer sp) {
            double resist = BoundCelestialEffects.getElementResistance(sp, attackerElement);
            if (resist > 0.0D) {
                newDamage *= (1.0D - resist);
            }
        }
        event.setAmount(newDamage);

        OririMod.LOGGER.debug("Element damage: {} -> {} ({} vs {})",
                baseDamage, newDamage, attackerElement, defenderElement);
    }
}