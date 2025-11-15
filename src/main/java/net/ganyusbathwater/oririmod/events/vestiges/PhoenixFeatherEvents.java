// java
package net.ganyusbathwater.oririmod.events.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.custom.vestiges.PhoenixFeather;
import net.ganyusbathwater.oririmod.util.vestiges.ExtraInventoryUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class PhoenixFeatherEvents {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;

        float damage = event.getAmount();
        if (damage <= 0F) return;

        float healthAfter = sp.getHealth() - damage;
        if (healthAfter > 0F) return; // Spieler würde nicht sterben

        for (ItemStack stack : ExtraInventoryUtil.readExtraInventory(sp)) {
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof PhoenixFeather feather)) continue;

            int unlocked = feather.getUnlockedLevel(stack);

            double chance = switch (unlocked) {
                case 1 -> 0.20D;
                case 2 -> 0.35D;
                case 3 -> 0.50D;
                default -> 0.0D;
            };
            if (chance <= 0.0D) continue;

            double roll = ThreadLocalRandom.current().nextDouble();
            if (roll > chance) continue;

            float newHealth = Math.max(1.0F, sp.getMaxHealth() * 0.25F);
            sp.setHealth(newHealth);
            sp.clearFire();

            event.setAmount(0); // Schaden komplett verhindern
            return;
        }
    }
}