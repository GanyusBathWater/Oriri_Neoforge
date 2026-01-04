// file: src/main/java/net/ganyusbathwater/oririmod/events/vestiges/WitherRoseEntityDamageEvents.java
package net.ganyusbathwater.oririmod.events.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.effect.vestiges.WitherRoseEffect;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public final class WitherRoseEntityDamageEvents {

    private WitherRoseEntityDamageEvents() {}

    // Schadensverringerung: Spieler bekommt Schaden
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target instanceof Player player)) return;

        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();

        float original = event.getAmount();
        float modified = WitherRoseEffect.modifyIncomingDamage(player, source, original);

        if (modified != original) {
            event.setAmount(modified);
        }
    }

    // Schadenserhöhung: Spieler verursacht Schaden
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player player)) return;

        LivingEntity target = event.getEntity();

        float original = event.getNewDamage();
        float modified = WitherRoseEffect.modifyOutgoingDamage(player, target, original);

        if (modified != original) {
            event.setNewDamage(modified);
        }
    }
}