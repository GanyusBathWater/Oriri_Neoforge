package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.entity.ThornProjectileEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Venomous Plant — a stationary turret that fires Thorn Projectiles.
 *
 * Key behaviours:
 *  - Cannot move; position is locked to its spawn point.
 *  - Targets only players (non-creative, non-spectator) within 16 blocks.
 *  - Every 60 ticks (3 s) it plays the attack animation and spawns a
 *    ThornProjectile at exactly tick 10 (0.5 s into the animation).
 *  - Spawn invincibility: the entity cannot take damage during the first
 *    20 ticks (1 s) after it enters the world.
 *  - Death animation overrides vanilla fall-over rotation via the renderer.
 *    The entity is kept alive for 30 ticks so the 1.0 s animation finishes.
 *
 * Animation controllers (GeckoLib):
 *  spawn_controller   — plays "spawning" once during the first 40 ticks (2 s).
 *  idle_controller    — loops "plant_turret_idle" after the spawn animation.
 *  attack_controller  — triggerable "plant_turret_attack" (PLAY_ONCE).
 *  hurt_controller    — triggerable "plant_turret_hurt"   (PLAY_ONCE).
 *  death_controller   — holds "plant_turret_death" when dead.
 */
public class VenomousPlantEntity extends Monster implements GeoEntity {

    // ─── Animation lengths (in ticks) ────────────────────────────────────────
    /** Duration of the spawn animation (2 s). */
    private static final int SPAWN_ANIM_TICKS  = 40;
    /** Tick at which the thorn is fired within the attack animation (0.5 s). */
    private static final int PROJECTILE_FIRE_TICK = 10;
    /** Total tracked ticks for the attack animation (0.75 s). */
    private static final int ATTACK_ANIM_TICKS = 15;
    /** Attack cooldown after firing (3 s). */
    private static final int ATTACK_COOLDOWN   = 60;
    /** Ticks of spawn invincibility (1 s). */
    private static final int SPAWN_INVINCIBILITY_TICKS = 20;
    /** Max range for attacking (blocks). */
    private static final double ATTACK_RANGE_SQ = 16.0 * 16.0;

    // ─── GeckoLib ─────────────────────────────────────────────────────────────
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // ─── Spawn-lock ───────────────────────────────────────────────────────────
    private double  spawnX, spawnY, spawnZ;
    private float   spawnYRot;
    private boolean hasSpawnPos = false;

    // ─── Server-only attack tracking (not synced; client uses triggerAnim) ───
    private int attackCooldown = 60; // Start with cooldown so it doesn't fire instantly
    private int attackAnimTick = 0;  // 0 = idle, 1-15 = mid-attack

    // ─── Constructor ──────────────────────────────────────────────────────────
    public VenomousPlantEntity(EntityType<? extends VenomousPlantEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    // ─── Attributes ───────────────────────────────────────────────────────────
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,     30.0D)
                .add(Attributes.MOVEMENT_SPEED,  0.0D)
                .add(Attributes.FOLLOW_RANGE,   16.0D)
                .add(Attributes.ATTACK_DAMAGE,   5.0D)
                .add(Attributes.ARMOR,           4.0D);
    }

    // ─── AI ───────────────────────────────────────────────────────────────────
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        
        // Target only non-creative, non-spectator players
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false,
                p -> p instanceof Player player
                        && !player.isCreative()
                        && !player.isSpectator()));
    }

    // ─── Tick ─────────────────────────────────────────────────────────────────
    @Override
    public void tick() {
        // Record and lock spawn position on first tick
        if (!this.hasSpawnPos) {
            this.spawnX = this.getX();
            this.spawnY = this.getY();
            this.spawnZ = this.getZ();
            this.spawnYRot = this.getYRot();
            this.hasSpawnPos = true;
        }

        // Lock position and rotation BEFORE super.tick()
        this.setPos(spawnX, spawnY, spawnZ);
        this.setDeltaMovement(Vec3.ZERO);
        this.setYRot(spawnYRot);
        this.yRotO = spawnYRot;
        this.setYBodyRot(spawnYRot);
        this.yBodyRotO = spawnYRot;

        super.tick();

        // Re-lock AFTER super.tick()
        this.setPos(spawnX, spawnY, spawnZ);
        this.setDeltaMovement(Vec3.ZERO);
        this.setYRot(spawnYRot);
        this.yRotO = spawnYRot;
        this.setYBodyRot(spawnYRot);
        this.yBodyRotO = spawnYRot;

        // Spawn dirt particles during the spawn animation (first 2 seconds)
        if (this.level().isClientSide && this.tickCount <= SPAWN_ANIM_TICKS) {
            if (this.random.nextInt(2) == 0) { // High frequency for nice effect
                double px = this.getX() + (this.random.nextDouble() - 0.5) * 1.5;
                double py = this.getY();
                double pz = this.getZ() + (this.random.nextDouble() - 0.5) * 1.5;
                this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), 
                                         px, py, pz, 0, 0.1, 0);
            }
        }

        // Server-side attack logic
        if (!this.level().isClientSide) {
            tickAttack();
        }
    }

    /** Prevent movement from being applied. */
    @Override
    public void travel(Vec3 travelVector) {
        this.setDeltaMovement(Vec3.ZERO);
    }

    // ─── Attack logic ─────────────────────────────────────────────────────────
    private void tickAttack() {
        // Decrement attack cooldown
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        // Advance in-progress attack animation
        if (attackAnimTick > 0) {
            attackAnimTick++;

            // Fire projectile at exactly 0.5 s (tick 10)
            if (attackAnimTick == PROJECTILE_FIRE_TICK) {
                LivingEntity target = this.getTarget();
                if (target != null && target.isAlive()) {
                    spawnThornProjectile(target);
                }
            }

            // Reset once the animation window is over
            if (attackAnimTick > ATTACK_ANIM_TICKS) {
                attackAnimTick = 0;
            }
        }

        // Start a new attack if ready
        if (attackCooldown == 0 && attackAnimTick == 0) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()
                    && this.distanceToSqr(target) <= ATTACK_RANGE_SQ
                    && !(target instanceof Player p && (p.isCreative() || p.isSpectator()))) {

                triggerAnim("attack_controller", "attack");
                attackCooldown = ATTACK_COOLDOWN;
                attackAnimTick = 1;
            }
        }
        
        // Ensure the entity looks at the target while attacking
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    /**
     * Spawns a ThornProjectileEntity aimed at the target.
     * Spawn position is approximated at the plant's mouth height (+1.2 blocks).
     */
    private void spawnThornProjectile(LivingEntity target) {
        Vec3 spawnPos  = this.position().add(0, 1.2, 0);
        Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 direction = targetPos.subtract(spawnPos).normalize();

        ThornProjectileEntity thorn = new ThornProjectileEntity(this.level(), this, direction);
        thorn.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        this.level().addFreshEntity(thorn);
    }

    // ─── Damage / death ───────────────────────────────────────────────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Spawn invincibility for the first second
        if (this.tickCount <= SPAWN_INVINCIBILITY_TICKS) {
            return false;
        }

        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide()) {
            triggerAnim("hurt_controller", "hurt");
        }
        return result;
    }

    /**
     * Extends the entity lifetime after death so the 1.0 s death animation
     * can finish before the entity is removed (default vanilla threshold = 20 t).
     */
    @Override
    protected void tickDeath() {
        ++this.deathTime;
        // Remove after 30 ticks (1.5 s) — gives the 1.0 s animation time to complete
        if (this.deathTime >= 30) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    // ─── Persistence ──────────────────────────────────────────────────────────
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("SpawnX", this.spawnX);
        tag.putDouble("SpawnY", this.spawnY);
        tag.putDouble("SpawnZ", this.spawnZ);
        tag.putFloat("SpawnYRot", this.spawnYRot);
        tag.putBoolean("HasSpawnPos", this.hasSpawnPos);
        tag.putInt("AttackCooldown", this.attackCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SpawnX")) {
            this.spawnX      = tag.getDouble("SpawnX");
            this.spawnY      = tag.getDouble("SpawnY");
            this.spawnZ      = tag.getDouble("SpawnZ");
            this.spawnYRot   = tag.getFloat("SpawnYRot");
            this.hasSpawnPos = tag.getBoolean("HasSpawnPos");
        }
        if (tag.contains("AttackCooldown")) {
            this.attackCooldown = tag.getInt("AttackCooldown");
        }
    }

    // ─── GeckoLib ─────────────────────────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        // ── Spawn animation — plays once during the first 40 ticks (2 s) ──────
        controllers.add(new AnimationController<>(this, "spawn_controller", 0, state -> {
            if (this.tickCount <= SPAWN_ANIM_TICKS) {
                state.getController().setAnimation(
                        RawAnimation.begin().then("spawning", Animation.LoopType.PLAY_ONCE));
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));

        // ── Idle — loops after spawn animation finishes ────────────────────────
        controllers.add(new AnimationController<>(this, "idle_controller", 0, state -> {
            if (this.tickCount > SPAWN_ANIM_TICKS && !this.isDeadOrDying()) {
                state.getController().setAnimation(
                        RawAnimation.begin().thenLoop("plant_turret_idle"));
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));

        // ── Attack — triggered by server via triggerAnim ───────────────────────
        controllers.add(new AnimationController<>(this, "attack_controller", 2, state -> PlayState.STOP)
                .triggerableAnim("attack",
                        RawAnimation.begin().then("plant_turret_attack", Animation.LoopType.PLAY_ONCE)));

        // ── Hurt — triggered by server via triggerAnim ────────────────────────
        controllers.add(new AnimationController<>(this, "hurt_controller", 2, state -> PlayState.STOP)
                .triggerableAnim("hurt",
                        RawAnimation.begin().then("plant_turret_hurt", Animation.LoopType.PLAY_ONCE)));

        // ── Death — holds last frame so the plant stays "dead" on screen ──────
        controllers.add(new AnimationController<>(this, "death_controller", 0, state -> {
            if (this.isDeadOrDying()) {
                state.getController().setAnimation(
                        RawAnimation.begin().then("plant_turret_death", Animation.LoopType.HOLD_ON_LAST_FRAME));
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}
