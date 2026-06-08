package net.ganyusbathwater.oririmod.entity.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class LoadedBlazeEntity extends Blaze {

    public LoadedBlazeEntity(EntityType<? extends Blaze> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.xpReward = 15; // More XP than normal blaze
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.MAX_HEALTH, 30.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new LoadedBlazeAttackGoal(this));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // Use soul fire particles instead of regular fire particles
    @Override
    public void aiStep() {
        if (!this.onGround() && this.getDeltaMovement().y < 0.0D) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
        }

        if (this.level().isClientSide) {

            for(int i = 0; i < 2; ++i) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
            }
        }

        super.aiStep();
    }

    static class LoadedBlazeAttackGoal extends Goal {
        private final LoadedBlazeEntity blaze;
        private int attackStep;
        private int attackTime;
        private int lastSeen;

        public LoadedBlazeAttackGoal(LoadedBlazeEntity blaze) {
            this.blaze = blaze;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        public boolean canUse() {
            LivingEntity target = this.blaze.getTarget();
            return target != null && target.isAlive() && this.blaze.canAttack(target);
        }

        public void start() {
            this.attackStep = 0;
        }

        public void stop() {
            this.lastSeen = 0;
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        public void tick() {
            --this.attackTime;
            LivingEntity target = this.blaze.getTarget();
            if (target != null) {
                boolean hasLineOfSight = this.blaze.getSensing().hasLineOfSight(target);
                if (hasLineOfSight) {
                    this.lastSeen = 0;
                } else {
                    ++this.lastSeen;
                }

                double distanceSq = this.blaze.distanceToSqr(target);
                if (distanceSq < 4.0D) {
                    if (!hasLineOfSight) {
                        return;
                    }
                    if (this.attackTime <= 0) {
                        this.attackTime = 20;
                        this.blaze.doHurtTarget(target);
                    }
                    this.blaze.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0D);
                } else if (distanceSq < this.getFollowDistance() * this.getFollowDistance() && hasLineOfSight) {
                    double dx = target.getX() - this.blaze.getX();
                    double dy = target.getY(0.5D) - this.blaze.getY(0.5D);
                    double dz = target.getZ() - this.blaze.getZ();
                    if (this.attackTime <= 0) {
                        ++this.attackStep;
                        if (this.attackStep == 1) {
                            this.attackTime = 60;
                        } else if (this.attackStep <= 4) {
                            this.attackTime = 6;
                        } else {
                            this.attackTime = 100;
                            this.attackStep = 0;
                        }

                        if (this.attackStep > 1) {
                            double sqrtDistance = Math.sqrt(Math.sqrt(distanceSq)) * 0.5D;
                            if (!this.blaze.isSilent()) {
                                this.blaze.level().levelEvent(null, 1018, this.blaze.blockPosition(), 0);
                            }

                            for(int i = 0; i < 1; ++i) {
                                Vec3 aim = new Vec3(dx + this.blaze.getRandom().nextGaussian() * sqrtDistance, 
                                                    dy, 
                                                    dz + this.blaze.getRandom().nextGaussian() * sqrtDistance);
                                
                                AetherChargeEntity charge = new AetherChargeEntity(this.blaze.level(), this.blaze, aim.x, aim.y, aim.z);
                                charge.setPos(charge.getX(), this.blaze.getY(0.5D) + 0.5D, charge.getZ());
                                this.blaze.level().addFreshEntity(charge);
                            }
                        }
                    }

                    this.blaze.getLookControl().setLookAt(target, 10.0F, 10.0F);
                } else if (this.lastSeen < 5) {
                    this.blaze.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0D);
                }

                super.tick();
            }
        }

        private double getFollowDistance() {
            return this.blaze.getAttributeValue(Attributes.FOLLOW_RANGE);
        }
    }
}
