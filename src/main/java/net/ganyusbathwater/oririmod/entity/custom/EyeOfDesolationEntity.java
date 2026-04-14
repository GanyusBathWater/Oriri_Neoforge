package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Eye of Desolation — a stationary flying robotic eye.
 *
 * Key behaviours:
 *  - Never moves; position is locked to its spawn point.
 *  - Charges a guardian-style laser beam over 60 ticks, then fires every 20 ticks.
 *  - Against players: applies / extends "Threat Detection" (300 ticks per hit, stackable).
 *  - Against other mobs: basic beam damage only.
 *  - Targets players first (priority 1), any mob second (priority 2).
 *  - Uses GeckoLib for animated model (idle + hover simultaneous, hurt, death).
 */
public class EyeOfDesolationEntity extends Monster implements GeoEntity {

    // ─── Synced data ──────────────────────────────────────────────────────────
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID =
            SynchedEntityData.defineId(EyeOfDesolationEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LASER_TICK =
            SynchedEntityData.defineId(EyeOfDesolationEntity.class, EntityDataSerializers.INT);

    // ─── GeckoLib ─────────────────────────────────────────────────────────────
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // ─── Spawn-lock ───────────────────────────────────────────────────────────
    private double spawnX, spawnY, spawnZ;
    private boolean hasSpawnPos = false;

    // ─── Constructor ──────────────────────────────────────────────────────────
    public EyeOfDesolationEntity(EntityType<? extends EyeOfDesolationEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 15;
    }

    // ─── Attributes ───────────────────────────────────────────────────────────
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,    40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE,  32.0D)
                .add(Attributes.ATTACK_DAMAGE,  4.0D)
                .add(Attributes.ARMOR,          8.0D);
    }

    /** 
     * Refined Hitbox: Moves the physical bounding box 0.5 blocks up 
     * to make the entity hover, without moving the AI position base.
     */
    @Override
    protected AABB makeBoundingBox() {
        return super.makeBoundingBox().move(0, 0.5, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TARGET_ID, -1);
        builder.define(DATA_LASER_TICK, 0);
    }

    // ─── AI ───────────────────────────────────────────────────────────────────
    @Override
    protected void registerGoals() {
        // No movement goals. Target selector only.
        // Ignore creative/spectator players. Visibility check disabled for stationary turret stability.
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false,
                p -> p instanceof Player player && !player.isCreative() && !player.isSpectator()));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, false,
                mob -> !(mob instanceof EyeOfDesolationEntity) && mob.isAlive()));
    }

    // ─── Tick ─────────────────────────────────────────────────────────────────
    @Override
    public void tick() {
        // Record spawn position on the very first tick
        if (!this.hasSpawnPos) {
            // Aggressively snap to the mathematical center of the Block Grid voxel.
            // This firmly pushes the 1.5-wide hitbox out of ANY adjacent walls 
            // and elevates it exactly half a block upwards as requested!
            net.minecraft.core.BlockPos pos = this.blockPosition();
            this.spawnX = pos.getX() + 0.5;
            this.spawnY = pos.getY() + 0.5; 
            this.spawnZ = pos.getZ() + 0.5;
            
            this.setPos(this.spawnX, this.spawnY, this.spawnZ);
            this.hasSpawnPos = true;
        }

        // Lock position BEFORE super.tick() so AI/physics cannot move the entity
        this.setPos(spawnX, spawnY, spawnZ);
        this.setDeltaMovement(Vec3.ZERO);

        super.tick();

        // Re-lock AFTER super.tick() to undo any residual drift
        this.setPos(spawnX, spawnY, spawnZ);
        this.setDeltaMovement(Vec3.ZERO);

        // Beam logic runs server-side only
        if (!this.level().isClientSide) {
            updateBeam();
        }
    }

    /** Prevent any movement delta from being applied. */
    @Override
    public void travel(Vec3 travelVector) {
        this.setDeltaMovement(Vec3.ZERO);
    }

    // ─── Beam logic ───────────────────────────────────────────────────────────
    /**
     * Beam logic — Handles a cycle of:
     * 1. Charging (0-60 ticks): Beam grows in intensity.
     * 2. Firing (Tick 60): Target is hit once.
     * 3. Cooldown (61-160 ticks): Beam is hidden while the eye recharges.
     */
    private void updateBeam() {
        LivingEntity target = this.getTarget();

        // Validate target (including creative/spectator check)
        if (target == null || !target.isAlive()
                || (target instanceof Player p && (p.isCreative() || p.isSpectator()))
                || this.distanceToSqr(target) > 15.0 * 15.0) {
            // Reset if we had a target
            if (this.entityData.get(DATA_TARGET_ID) != -1) {
                this.entityData.set(DATA_TARGET_ID, -1);
                this.entityData.set(DATA_LASER_TICK, 0);
            }
            return;
        }

        // Increment cycle tick
        int tick = this.entityData.get(DATA_LASER_TICK) + 1;
        
        // Reset cycle if it exceeds total time (60 charge + 100 wait)
        if (tick > 160) {
            tick = 0;
        }
        this.entityData.set(DATA_LASER_TICK, tick);

        // Track target only during charge or hit (ticks 0-60)
        // This makes the beam vanish during the cooldown ticks
        if (tick <= 60) {
            this.entityData.set(DATA_TARGET_ID, target.getId());
        } else {
            this.entityData.set(DATA_TARGET_ID, -1);
        }

        // Look at the target
        Vec3 eyePos = this.getPosition(1.0f).add(0, 1.5, 0); // World height: 1.5 blocks above entity base
        Vec3 look = target.getEyePosition().subtract(eyePos).normalize();
        this.setYRot((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
        this.setXRot((float) Math.toDegrees(Math.asin(-look.y)));
        this.setYHeadRot(this.getYRot());

        // Firing Condition: Hit exactly once at tick 60
        if (tick == 60) {
            boolean hit = target.hurt(
                    this.damageSources().mobAttack(this),
                    (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));

            if (hit && target instanceof Player player) {
                // Extend Threat Detection — add 15 s (300 t) on top of remaining time
                MobEffectInstance existing = player.getEffect(ModEffects.THREAT_DETECTION_EFFECT);
                int existing_duration = (existing != null) ? existing.getDuration() : 0;
                player.addEffect(new MobEffectInstance(
                        ModEffects.THREAT_DETECTION_EFFECT,
                        existing_duration + 300, // +15 seconds each hit
                        0,
                        false, true, true));
            }
        }
    }

    // ─── Client helpers ────────────────────────────────────────────────────────
    /** Entity ID of the current beam target, or -1 if none. */
    public int getTargetId() {
        return this.entityData.get(DATA_TARGET_ID);
    }

    /**
     * 0.0 → beam is starting to charge, 1.0 → beam is fully charged.
     * Only returns a progress > 0 during the charging phase (0-60).
     */
    public float getLaserProgress(float partialTick) {
        int tick = this.entityData.get(DATA_LASER_TICK);
        if (tick > 60) return 0.0f;
        return Math.min(1.0f, tick / 60.0f);
    }

    // ─── Persistence ──────────────────────────────────────────────────────────
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("SpawnX", this.spawnX);
        tag.putDouble("SpawnY", this.spawnY);
        tag.putDouble("SpawnZ", this.spawnZ);
        tag.putBoolean("HasSpawnPos", this.hasSpawnPos);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SpawnX")) {
            this.spawnX = tag.getDouble("SpawnX");
            this.spawnY = tag.getDouble("SpawnY");
            this.spawnZ = tag.getDouble("SpawnZ");
            this.hasSpawnPos = tag.getBoolean("HasSpawnPos");
        }
    }

    // ─── GeckoLib ─────────────────────────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Idle rotation — always looping
        controllers.add(new AnimationController<>(this, "idle_controller", 0, state -> {
            state.getController().setAnimation(
                    RawAnimation.begin().thenLoop("eye_of_desolation_idle"));
            return PlayState.CONTINUE;
        }));

        // Hover bob — always looping, plays simultaneously with idle via separate controller
        controllers.add(new AnimationController<>(this, "hover_controller", 0, state -> {
            state.getController().setAnimation(
                    RawAnimation.begin().thenLoop("eye_of_desolation_hover"));
            return PlayState.CONTINUE;
        }));

        // Hurt flash — plays once when the "hurt" trigger is fired
        controllers.add(new AnimationController<>(this, "hurt_controller", 2, state -> PlayState.STOP)
                .triggerableAnim("hurt", RawAnimation.begin().then("eye_of_desolation_hurt", Animation.LoopType.PLAY_ONCE)));

        // Death — plays once and holds on last frame
        controllers.add(new AnimationController<>(this, "death_controller", 0, state -> {
            if (this.isDeadOrDying()) {
                state.getController().setAnimation(
                        RawAnimation.begin().then("eye_of_desolation_death", Animation.LoopType.HOLD_ON_LAST_FRAME));
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide()) {
            triggerAnim("hurt_controller", "hurt");
        }
        return result;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }
}
