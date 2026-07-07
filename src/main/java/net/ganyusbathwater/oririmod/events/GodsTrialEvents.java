package net.ganyusbathwater.oririmod.events;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.world.GodsTrialData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class GodsTrialEvents {

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Level level = event.getLevel().getLevel();
        if (level instanceof ServerLevel serverLevel && event.getEntity() instanceof Monster monster) {
            GodsTrialData data = GodsTrialData.get(serverLevel);
            if (data.isActive()) {
                // Buff Health by 50%
                var maxHealth = monster.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.setBaseValue(maxHealth.getBaseValue() * 1.5);
                    monster.setHealth(monster.getMaxHealth());
                }

                // Buff Attack Damage by 75%
                var attackDamage = monster.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attackDamage != null) {
                    attackDamage.setBaseValue(attackDamage.getBaseValue() * 1.75);
                }

                // Add 0.25 Knockback Resistance
                var knockbackResistance = monster.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (knockbackResistance != null) {
                    knockbackResistance.setBaseValue(knockbackResistance.getBaseValue() + 0.25);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        if (!(target instanceof Monster monster)) return;
        if (!(monster.level() instanceof ServerLevel serverLevel)) return;

        GodsTrialData data = GodsTrialData.get(serverLevel);
        if (data.isActive()) {
            // Anti-Kill command & Bypasses Invulnerability immunity
            if (event.getSource().is(DamageTypes.GENERIC_KILL) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                event.setNewDamage(0.0f);
                return;
            }

            // One-Shot Protection from players
            if (event.getSource().getEntity() instanceof Player) {
                if (event.getNewDamage() > monster.getMaxHealth()) {
                    event.setNewDamage(1.0f);
                }
            }
        }
    }
}
