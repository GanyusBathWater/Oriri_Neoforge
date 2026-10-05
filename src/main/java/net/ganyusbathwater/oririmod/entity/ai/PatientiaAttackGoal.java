package net.ganyusbathwater.oririmod.entity.ai;

import net.ganyusbathwater.oririmod.entity.custom.PatientiaEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class PatientiaAttackGoal extends Goal {

    private final PatientiaEntity boss;
    private int timer = 0;
    private int chosenAttack = PatientiaEntity.ATTACK_NONE;
    private boolean actionFired = false;
    
    // Projectile state
    private int projectileCount = 0;

    public PatientiaAttackGoal(PatientiaEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (boss.isDefeated() || boss.getAttackType() == PatientiaEntity.ATTACK_DEFENSIVE) return false;
        
        // Wait for global cooldown (stored in boss, managed here for simplicity, but let's just use a local one for now)
        // We will just let the goal tick natively.
        LivingEntity target = boss.getTarget();
        if (target == null || !target.isAlive()) return false;
        
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (boss.isDefeated() || boss.getAttackType() == PatientiaEntity.ATTACK_DEFENSIVE) return false;
        return timer < animDuration(chosenAttack);
    }

    @Override
    public void start() {
        timer = 0;
        actionFired = false;
        projectileCount = 0;

        LivingEntity target = boss.getTarget();
        double distSq = target != null ? boss.distanceToSqr(target) : 100.0;
        
        // Choose attack
        if (distSq <= 16.0 && boss.getRandom().nextBoolean()) {
            chosenAttack = boss.getRandom().nextBoolean() ? PatientiaEntity.ATTACK_MELEE_RIGHT : PatientiaEntity.ATTACK_MELEE_LEFT;
        } else {
            int r = boss.getRandom().nextInt(100);
            if (r < 25) {
                chosenAttack = PatientiaEntity.ATTACK_MAGIC_PROJECTILE;
            } else if (r < 50) {
                chosenAttack = PatientiaEntity.ATTACK_MAGIC_CIRCLES;
            } else if (r < 75) {
                chosenAttack = PatientiaEntity.ATTACK_GIANT_SLICE;
            } else {
                if (boss.getHealthFraction() <= 0.5f) {
                    chosenAttack = PatientiaEntity.ATTACK_HEAVENLY_EXEC;
                } else {
                    chosenAttack = PatientiaEntity.ATTACK_MAGIC_PROJECTILE; // Fallback
                }
            }
        }
        
        boss.setAttackType(chosenAttack);
        triggerAnimation(chosenAttack);
    }

    @Override
    public void tick() {
        timer++;
        
        LivingEntity target = boss.getTarget();
        if (target != null && target.isAlive()) {
            boss.getLookControl().setLookAt(target, 60.0f, 60.0f);
        }

        switch (chosenAttack) {
            case PatientiaEntity.ATTACK_MELEE_RIGHT:
            case PatientiaEntity.ATTACK_MELEE_LEFT:
                tickMelee(target);
                break;
            case PatientiaEntity.ATTACK_MAGIC_PROJECTILE:
                tickProjectiles(target);
                break;
            case PatientiaEntity.ATTACK_MAGIC_CIRCLES:
                tickMagicCircles(target);
                break;
            case PatientiaEntity.ATTACK_GIANT_SLICE:
                tickGiantSlice();
                break;
            case PatientiaEntity.ATTACK_HEAVENLY_EXEC:
                tickHeavenlyExec(target);
                break;
        }
    }

    @Override
    public void stop() {
        boss.setAttackType(PatientiaEntity.ATTACK_NONE);
        chosenAttack = PatientiaEntity.ATTACK_NONE;
        timer = 0;
        actionFired = false;
        boss.triggerAnim("attack_controller", "stop");
    }
    
    // ── Attack Ticks ─────────────────────────────────────────────────────────

    private void tickMelee(LivingEntity target) {
        // Animation is 10 ticks (0.5s). Hit frame around tick 5.
        if (!actionFired && timer == 5) {
            actionFired = true;
            if (target != null && target.isAlive() && boss.distanceToSqr(target) <= 16.0) {
                // 120-degree cone sweep using 2D Vector math
                Vec3 look = boss.getLookAngle();
                Vec3 look2d = new Vec3(look.x, 0, look.z).normalize();
                
                List<Player> hitPlayers = boss.level().getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(4.0));
                for (Player p : hitPlayers) {
                    Vec3 toTarget = p.position().subtract(boss.position());
                    Vec3 toTarget2d = new Vec3(toTarget.x, 0, toTarget.z).normalize();
                    double dot = look2d.dot(toTarget2d);
                    
                    if (dot >= 0.5D) { // 120 degree cone (cos(60) = 0.5)
                        p.hurt(boss.damageSources().mobAttack(boss), (float)boss.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
                        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true));
                    }
                }
            }
        }
    }

    private void tickProjectiles(LivingEntity target) {
        // Fire 6 projectiles. 1 every 5 ticks.
        if (timer % 5 == 0 && projectileCount < 6) {
            projectileCount++;
            if (boss.level() instanceof ServerLevel sl && target != null) {
                net.ganyusbathwater.oririmod.entity.SwordProjectileEntity sword = new net.ganyusbathwater.oririmod.entity.SwordProjectileEntity(sl, boss);
                Vec3 look = boss.getLookAngle();
                
                double offsetX = (sl.random.nextDouble() - 0.5) * 6.0;
                double offsetY = 2.0 + (sl.random.nextDouble() * 3.0);
                double offsetZ = (sl.random.nextDouble() - 0.5) * 6.0;

                sword.setPos(boss.getX() + offsetX, boss.getY() + offsetY, boss.getZ() + offsetZ);
                sword.setTargetId(target.getUUID());
                sword.setOwner(boss);
                
                // Initialize direction towards target
                Vec3 toTarget = target.getEyePosition().subtract(sword.position()).normalize();
                sword.shoot(toTarget.x, toTarget.y, toTarget.z, 2.0f, 1.0f);
                sl.addFreshEntity(sword);
            }
        }
    }

    private void tickMagicCircles(LivingEntity target) {
        if (!actionFired && timer == 10) {
            actionFired = true;
            if (boss.level() instanceof ServerLevel sl) {
                net.ganyusbathwater.oririmod.entity.SwordCircleEntity circle = new net.ganyusbathwater.oririmod.entity.SwordCircleEntity(
                        net.ganyusbathwater.oririmod.entity.ModEntities.SWORD_CIRCLE.get(), sl);
                
                if (target != null) {
                    circle.setTargetId(target.getUUID());
                } else {
                    circle.setTargetId(boss.getUUID()); 
                }
                circle.setPos(boss.getX(), boss.getY() + 3.0, boss.getZ());
                sl.addFreshEntity(circle);
            }
        }
    }

    private void tickGiantSlice() {
        if (!actionFired && timer == 10) {
            actionFired = true;
            if (boss.level() instanceof ServerLevel sl && boss.arenaCenter != null) {
                boolean isGodsTrial = net.ganyusbathwater.oririmod.world.GodsTrialData.get(sl).isActive();
                boolean isHard = sl.getDifficulty() == net.minecraft.world.Difficulty.HARD;
                
                if (boss.getRandom().nextBoolean()) {
                    // GIANT SWORD SLICE GRID
                    double radius = 20.0;
                    double gap = 4.0; 
                    double yLevel = boss.arenaCenter.getY();
                    int gridInterval = isGodsTrial ? 20 : (isHard ? 40 : 60);
                    
                    for (double x = -radius; x <= radius; x += gap) {
                        spawnSlice(sl, boss.arenaCenter.getX() + x + 0.5, yLevel, boss.arenaCenter.getZ() + radius + 0.5, 0, new Vec3(0, 0, -1));
                    }
                    for (double z = -radius; z <= radius; z += gap) {
                        spawnSlice(sl, boss.arenaCenter.getX() - radius + 0.5, yLevel, boss.arenaCenter.getZ() + z + 0.5, gridInterval, new Vec3(1, 0, 0));
                    }
                    for (double x = -radius; x <= radius; x += gap) {
                        spawnSlice(sl, boss.arenaCenter.getX() + x + 0.5, yLevel, boss.arenaCenter.getZ() - radius + 0.5, gridInterval * 2, new Vec3(0, 0, 1));
                    }
                    for (double z = -radius; z <= radius; z += gap) {
                        spawnSlice(sl, boss.arenaCenter.getX() + radius + 0.5, yLevel, boss.arenaCenter.getZ() + z + 0.5, gridInterval * 3, new Vec3(-1, 0, 0));
                    }
                } else {
                    // GIANT SWORD SLICE CIRCLE
                    double radius = 20.0;
                    double yLevel = boss.arenaCenter.getY();
                    int swordsPerWave = isGodsTrial ? 4 : (isHard ? 3 : 2);
                    int totalSwords = 15 * swordsPerWave;
                    int waves = totalSwords / swordsPerWave;
                    
                    for (int i = 0; i < totalSwords; i++) {
                        int waveIndex = i / swordsPerWave;
                        int swordInWave = i % swordsPerWave;
                        
                        double angle = (2 * Math.PI * i) / totalSwords;
                        double x = boss.arenaCenter.getX() + 0.5 + Math.cos(angle) * radius;
                        double z = boss.arenaCenter.getZ() + 0.5 + Math.sin(angle) * radius;
                        
                        Vec3 dir = new Vec3(boss.arenaCenter.getX() + 0.5 - x, 0, boss.arenaCenter.getZ() + 0.5 - z).normalize();
                        spawnSlice(sl, x, yLevel, z, waveIndex * 15, dir);
                    }
                }
            }
        }
    }

    private void spawnSlice(ServerLevel sl, double x, double y, double z, int delay, Vec3 dir) {
        net.ganyusbathwater.oririmod.entity.custom.GiantSwordSliceEntity sword = new net.ganyusbathwater.oririmod.entity.custom.GiantSwordSliceEntity(net.ganyusbathwater.oririmod.entity.ModEntities.GIANT_SWORD_SLICE.get(), sl);
        sword.setPos(x, y, z);
        sword.ownerId = boss.getUUID();
        sword.initialDelay = delay;
        sword.setDirection(dir);
        sl.addFreshEntity(sword);
    }

    private void tickHeavenlyExec(LivingEntity target) {
        if (!actionFired && timer == 20) {
            actionFired = true;
            if (boss.level() instanceof ServerLevel sl) {
                BlockPos targetPos = target != null ? target.blockPosition() : boss.blockPosition();
                net.ganyusbathwater.oririmod.entity.custom.GiantSwordEntity sword = new net.ganyusbathwater.oririmod.entity.custom.GiantSwordEntity(
                        net.ganyusbathwater.oririmod.entity.ModEntities.GIANT_SWORD.get(), sl
                );
                sword.setPos(targetPos.getX() + 0.5, targetPos.getY() + 15.0, targetPos.getZ() + 0.5);
                sword.ownerId = boss.getUUID();
                
                sword.delayTicks = 60; 
                sword.fallSpeed = -3.5f;
                sword.impactDamage = 20.0f;
                sword.explosionDamage = 10.0f;
                sword.impactRadius = 7.0f;
                sword.explosionRadius = 5.0f;
                sword.embeddedTicks = 80;
                
                sl.addFreshEntity(sword);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int animDuration(int attack) {
        return switch (attack) {
            case PatientiaEntity.ATTACK_MELEE_RIGHT, PatientiaEntity.ATTACK_MELEE_LEFT -> 20; // 1s
            case PatientiaEntity.ATTACK_MAGIC_PROJECTILE -> 40; // 2s
            case PatientiaEntity.ATTACK_MAGIC_CIRCLES -> 30; // 1.5s
            case PatientiaEntity.ATTACK_GIANT_SLICE -> 30; // 1.5s
            case PatientiaEntity.ATTACK_HEAVENLY_EXEC -> 40; // 2s
            default -> 20;
        };
    }

    private void triggerAnimation(int attack) {
        String animName = switch (attack) {
            case PatientiaEntity.ATTACK_MELEE_RIGHT -> "patientia_normal_attack_right";
            case PatientiaEntity.ATTACK_MELEE_LEFT -> "patientia_normal_attack_left";
            case PatientiaEntity.ATTACK_MAGIC_PROJECTILE, PatientiaEntity.ATTACK_MAGIC_CIRCLES, 
                 PatientiaEntity.ATTACK_GIANT_SLICE, PatientiaEntity.ATTACK_HEAVENLY_EXEC -> "patientia_defensive";
            default -> "patientia_idle";
        };
        boss.triggerAnim("attack_controller", animName);
    }
}
