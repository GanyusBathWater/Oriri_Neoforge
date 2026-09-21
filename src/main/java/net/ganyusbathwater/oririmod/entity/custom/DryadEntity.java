package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.entity.ai.DryadAttackGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

public class DryadEntity extends Monster implements GeoEntity, NeutralMob {
    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(DryadEntity.class, EntityDataSerializers.BOOLEAN);
    
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    
    public int attackAnimTimer = 0;
    private int fairySpawnCooldown = 0;
    
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;

    public DryadEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D) // 15 Hearts
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D) // 3 Hearts
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACKING, false);
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(ATTACKING, attacking);
    }

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new DryadAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(2, new ResetUniversalAngerTargetGoal<>(this, true));
    }

    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide) {
            this.updatePersistentAnger((ServerLevel)this.level(), true);
            
            if (fairySpawnCooldown > 0) {
                fairySpawnCooldown--;
            }
        }
        
        if (isAttacking()) {
            attackAnimTimer++;
        } else {
            attackAnimTimer = 0;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean wasHurt = super.hurt(source, amount);
        if (wasHurt && !this.level().isClientSide && fairySpawnCooldown <= 0) {
            triggerAnim("hurt_controller", "dryad_hurt");
            spawnFairy();
            fairySpawnCooldown = 300; // 15 seconds cooldown
        }
        return wasHurt;
    }

    private void spawnFairy() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        FairyEntity fairy = ModEntities.FAIRY.get().create(serverLevel);
        if (fairy != null) {
            double ox = (random.nextDouble() - 0.5) * 4.0;
            double oz = (random.nextDouble() - 0.5) * 4.0;
            fairy.setPos(this.getX() + ox, this.getY() + 1.0, this.getZ() + oz);
            fairy.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            fairy.setOwner(this);
            serverLevel.addFreshEntity(fairy);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 5, state -> {
            if (isAttacking()) {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("dryad_attack"));
                return PlayState.CONTINUE;
            }
            if (state.isMoving()) {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("dryad_walk"));
                return PlayState.CONTINUE;
            }
            state.getController().setAnimation(RawAnimation.begin().thenLoop("dryad_idle"));
            return PlayState.CONTINUE;
        }));
        
        controllers.add(new AnimationController<>(this, "hurt_controller", 2, state -> PlayState.STOP)
                .triggerableAnim("dryad_hurt", RawAnimation.begin().thenPlay("dryad_hurt")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FairySpawnCooldown", fairySpawnCooldown);
        this.addPersistentAngerSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FairySpawnCooldown")) {
            fairySpawnCooldown = tag.getInt("FairySpawnCooldown");
        }
        this.readPersistentAngerSaveData(this.level(), tag);
    }

    // --- NeutralMob implementation ---
    @Override
    public int getRemainingPersistentAngerTime() {
        return this.remainingPersistentAngerTime;
    }

    @Override
    public void setRemainingPersistentAngerTime(int time) {
        this.remainingPersistentAngerTime = time;
    }

    @Nullable
    @Override
    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID target) {
        this.persistentAngerTarget = target;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(20 * 30); // 30 seconds of anger
    }
}
