package net.ganyusbathwater.oririmod.entity.ai;

import net.ganyusbathwater.oririmod.entity.custom.IOririBoss;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

public class BossSwarmPunishGoal extends Goal {
    private final Mob mob;
    private final IOririBoss boss;
    private int cooldown = 0;

    public BossSwarmPunishGoal(Mob mob) {
        this.mob = mob;
        this.boss = (IOririBoss) mob;
        // No flags means it runs concurrently with their other attacks!
        // This is exactly the "extra attack" in quick succession mechanics.
        this.setFlags(EnumSet.noneOf(Goal.Flag.class)); 
    }

    @Override
    public boolean canUse() {
        if (boss.isDefeated()) return false;
        
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        // Trigger Swarm Punish if 3 or more player-summoned mobs are within 16 blocks
        long summonCount = mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(16.0), 
            e -> e.getPersistentData().getBoolean("OririSummoned")).size();

        return summonCount >= 3;
    }

    @Override
    public void start() {
        cooldown = 100; // 5-second cooldown for the AoE Swarm blast
        
        if (mob.level() instanceof ServerLevel sl) {
            // Massive Swarm Punish AoE
            List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(12.0),
                e -> e != mob && !e.getPersistentData().getBoolean("IsNoxusMob"));
            
            for (LivingEntity e : targets) {
                // If it's a summon, obliterate it (stacks with the 3.0x global damage multiplier!)
                float damage = e.getPersistentData().getBoolean("OririSummoned") ? 20.0f : 10.0f;
                e.hurt(mob.damageSources().mobAttack(mob), damage);
                
                // Add crippling debuffs
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));

                // Visual sonic blast on each target
                sl.sendParticles(ParticleTypes.SONIC_BOOM, e.getX(), e.getY() + e.getBbHeight() / 2.0, e.getZ(), 1, 0, 0, 0, 0);
            }
            
            // Visual for the boss
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, mob.getX(), mob.getY() + mob.getBbHeight() / 2.0, mob.getZ(), 2, 1.0, 1.0, 1.0, 0.0);
            sl.playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM, net.minecraft.sounds.SoundSource.HOSTILE, 3.0f, 1.0f);
        }
    }
}
