package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.entity.MagicProjectileEntity;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.Optional;
import java.util.List;

public class NoxusCultistEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> DATA_ELEMENT = SynchedEntityData.defineId(NoxusCultistEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DATA_ATTACKING = SynchedEntityData.defineId(NoxusCultistEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    
    public int attackTick = 0; // Tracks attack animation progress

    public NoxusCultistEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 5.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ELEMENT, Element.DARKNESS.ordinal());
        builder.define(DATA_ATTACKING, false);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        Element[] possibleElements = {Element.FIRE, Element.NATURE, Element.EARTH, Element.WATER, Element.LIGHT, Element.DARKNESS};
        Element chosen = possibleElements[this.random.nextInt(possibleElements.length)];
        this.setElement(chosen);
        this.getPersistentData().putBoolean("IsNoxusMob", true);
        return super.finalizeSpawn(level, difficulty, reason, spawnData);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(this, Player.class, 4.0F, 0.6D, 0.8D)); // Flee if player gets within 4 blocks
        this.goalSelector.addGoal(2, new CultistRangedAttackGoal(this, 1.0D, 80, 16.0F)); // 4s cooldown
        this.goalSelector.addGoal(3, new CultistWorkstationStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, true, false, 
            (entity) -> {
                return entity instanceof Monster 
                    && !entity.getType().is(net.ganyusbathwater.oririmod.entity.custom.NoxusKnightEntity.NOXUS_MOBS)
                    && !entity.getPersistentData().getBoolean("IsNoxusMob");
            }
        ));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.entityData.get(DATA_ATTACKING)) {
            attackTick++;
            this.yBodyRot = this.yHeadRot; // Force the entire body to face the target so arms and magic circle align
        } else {
            attackTick = 0;
        }
    }

    // --- GETTERS & SETTERS ---
    public Element getElement() {
        return Element.values()[this.entityData.get(DATA_ELEMENT)];
    }

    public void setElement(Element element) {
        this.entityData.set(DATA_ELEMENT, element.ordinal());
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(DATA_ATTACKING, attacking);
    }

    public boolean isAttacking() {
        return this.entityData.get(DATA_ATTACKING);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CultistElement", this.entityData.get(DATA_ELEMENT));
        tag.putBoolean("IsNoxusMob", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CultistElement")) {
            this.entityData.set(DATA_ELEMENT, tag.getInt("CultistElement"));
        }
        this.getPersistentData().putBoolean("IsNoxusMob", true);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target.getType().is(net.ganyusbathwater.oririmod.entity.custom.NoxusKnightEntity.NOXUS_MOBS) 
            || target.getPersistentData().getBoolean("IsNoxusMob")) {
            return false;
        }
        return super.canAttack(target);
    }

    // Removed placeholder vanilla sounds. Add your custom sounds here when they are ready.
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, state -> {
            if (this.isAttacking()) {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("noxus_cultist_attacking"));
                return PlayState.CONTINUE;
            } else if (state.isMoving()) {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("noxus_cultist_walk"));
                return PlayState.CONTINUE;
            } else {
                state.getController().forceAnimationReset();
                return PlayState.STOP;
            }
        }));
        
        // Separated hurt controller to play concurrently
        controllers.add(new AnimationController<>(this, "hurt_controller", 0, state -> {
            if (this.hurtTime > 0) {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("noxus_cultist_hurt"));
                return PlayState.CONTINUE;
            }
            state.getController().forceAnimationReset();
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }

    // --- CUSTOM GOALS ---

    static class CultistRangedAttackGoal extends Goal {
        private final NoxusCultistEntity mob;
        private final double speedModifier;
        private final int attackInterval;
        private final float attackRadius;
        private int attackTime = -1;
        private int seeTime;
        private boolean strafingClockwise;
        private boolean strafingBackwards;
        private int strafingTime = -1;

        public CultistRangedAttackGoal(NoxusCultistEntity mob, double speedModifier, int attackInterval, float attackRadius) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.attackInterval = attackInterval;
            this.attackRadius = attackRadius;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.mob.getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return (this.canUse() || !this.mob.getNavigation().isDone());
        }

        @Override
        public void start() {
            super.start();
            this.mob.setAggressive(true);
        }

        @Override
        public void stop() {
            super.stop();
            this.mob.setAggressive(false);
            this.seeTime = 0;
            this.attackTime = -1;
            this.mob.setAttacking(false);
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target != null) {
                double distanceSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
                boolean canSee = this.mob.getSensing().hasLineOfSight(target);
                boolean hasSeen = this.seeTime > 0;
                if (canSee != hasSeen) {
                    this.seeTime = 0;
                }
                if (canSee) {
                    ++this.seeTime;
                } else {
                    --this.seeTime;
                }

                if (this.mob.isAttacking()) {
                    // Lock movement and wait for animation (20 ticks)
                    this.mob.getNavigation().stop();
                    this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                    
                    if (this.mob.attackTick >= 20) {
                        // Fire projectile!
                        fireProjectile(target);
                        this.mob.setAttacking(false);
                        this.attackTime = this.attackInterval; // Start cooldown
                    }
                    return;
                }

                // Movement logic when not attacking
                if (distanceSqr > (double)(this.attackRadius * this.attackRadius) || !canSee) {
                    this.mob.getNavigation().moveTo(target, this.speedModifier);
                } else {
                    this.mob.getNavigation().stop();
                }

                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

                if (canSee) {
                    if (--this.attackTime <= 0) {
                        this.mob.setAttacking(true); // Start the attack windup
                    }
                }
            }
        }

        private void fireProjectile(LivingEntity target) {
            // Magic circle is at [0, 19, -16] in Blockbench, which is 1.0 block forward and 1.1875 blocks high
            net.minecraft.world.phys.Vec3 forward = net.minecraft.world.phys.Vec3.directionFromRotation(0, this.mob.yBodyRot).normalize();
            Vec3 spawnPos = this.mob.position().add(forward.x * 1.0, 1.1875, forward.z * 1.0);
            
            Vec3 targetPos = target.getEyePosition().subtract(0, 0.5, 0);
            Vec3 direction = targetPos.subtract(spawnPos).normalize();
            
            double speed = 1.0D;
            MagicProjectileEntity proj = new MagicProjectileEntity(this.mob.level(), this.mob, direction.x * speed, direction.y * speed, direction.z * speed);
            proj.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            proj.setElement(this.mob.getElement());
            proj.setDamage(10.0F); // 5 hearts
            this.mob.level().addFreshEntity(proj);
        }
    }

    static class CultistWorkstationStrollGoal extends RandomStrollGoal {
        public CultistWorkstationStrollGoal(NoxusCultistEntity mob, double speedModifier) {
            super(mob, speedModifier, 40); // Decreased from 120 to 40 so he wanders much more frequently
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            // 20% chance to specifically look for a workstation nearby
            if (this.mob.getRandom().nextFloat() < 0.2F) {
                BlockPos pos = this.mob.blockPosition();
                for (int i = 0; i < 15; i++) {
                    BlockPos target = pos.offset(
                            this.mob.getRandom().nextInt(17) - 8,
                            this.mob.getRandom().nextInt(7) - 3,
                            this.mob.getRandom().nextInt(17) - 8
                    );
                    net.minecraft.world.level.block.state.BlockState state = this.mob.level().getBlockState(target);
                    if (state.is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE) || state.is(net.minecraft.world.level.block.Blocks.FURNACE)) {
                        return Vec3.atBottomCenterOf(target);
                    }
                }
            }
            return super.getPosition();
        }
    }
}
