package net.ganyusbathwater.oririmod.entity.ai;

import net.ganyusbathwater.oririmod.entity.custom.PatientiaEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class PatientiaMeleeAttackGoal extends Goal {

    private final PatientiaEntity boss;
    private int timer = 0;
    private int chosenAttack = PatientiaEntity.ATTACK_NONE;
    private boolean actionFired = false;
    private long nextMeleeTime = 0;

    public PatientiaMeleeAttackGoal(PatientiaEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (boss.isDefeated() || boss.getAttackType() != PatientiaEntity.ATTACK_NONE) return false;
        if (boss.level().getGameTime() < nextMeleeTime) return false;

        LivingEntity target = boss.getTarget();
        if (target == null || !target.isAlive()) return false;
        
        // Melee only if target is close (8 blocks)
        if (boss.distanceToSqr(target) > 64.0) return false;

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (boss.isDefeated() || boss.getAttackType() == PatientiaEntity.ATTACK_DEFENSIVE) return false;
        // Melee animation is 20 ticks
        return timer < 20;
    }

    @Override
    public void start() {
        timer = 0;
        actionFired = false;
        
        chosenAttack = boss.getRandom().nextBoolean() ? PatientiaEntity.ATTACK_MELEE_RIGHT : PatientiaEntity.ATTACK_MELEE_LEFT;
        boss.setAttackType(chosenAttack);
        
        String animName = chosenAttack == PatientiaEntity.ATTACK_MELEE_RIGHT ? "patientia_normal_attack_right" : "patientia_normal_attack_left";
        boss.triggerAnim("attack_controller", animName);
    }

    @Override
    public void tick() {
        timer++;
        LivingEntity target = boss.getTarget();
        if (target != null && target.isAlive()) {
            boss.getLookControl().setLookAt(target, 60.0f, 60.0f);
        }

        // Animation is 10 ticks (0.5s). Hit frame around tick 5.
        if (!actionFired && timer == 5) {
            actionFired = true;
            if (target != null && target.isAlive() && boss.distanceToSqr(target) <= 100.0) {
                // Wide sweep, hit everyone nearby
                List<Player> hitPlayers = boss.level().getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(6.0));
                for (Player p : hitPlayers) {
                    if (boss.distanceToSqr(p) <= 64.0) { 
                        p.hurt(boss.damageSources().mobAttack(boss), (float)boss.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
                        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true));
                    }
                }
            }
        }
    }

    @Override
    public void stop() {
        if (boss.getAttackType() != PatientiaEntity.ATTACK_DEFENSIVE) {
            boss.setAttackType(PatientiaEntity.ATTACK_NONE);
            boss.triggerAnim("attack_controller", "stop");
        }
        chosenAttack = PatientiaEntity.ATTACK_NONE;
        timer = 0;
        actionFired = false;
        
        nextMeleeTime = boss.level().getGameTime() + 40; // 2 seconds cooldown
    }
}
