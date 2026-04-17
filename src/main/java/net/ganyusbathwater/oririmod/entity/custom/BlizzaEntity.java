package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.entity.EyeOfTheStormEntity;
import net.ganyusbathwater.oririmod.entity.ai.BlizzaMagicAttackGoal;
import net.ganyusbathwater.oririmod.entity.ai.BlizzaMeleeAttackGoal;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * Blizza — "Choir of Frozen Waves, Blizza the Sage of Water"
 *
 * A humanoid ice-sage boss with 4 attacks, a 2-phase system, a dramatic
 * delayed death sequence, and a custom HUD boss bar.
 */
public class BlizzaEntity extends Monster implements GeoEntity {

    // ── Attack type constants ──────────────────────────────────────────────
    public static final int ATTACK_NONE    = 0;
    public static final int ATTACK_MELEE   = 1;
    public static final int ATTACK_ICICLE  = 2;
    public static final int ATTACK_STORM   = 3;
    public static final int ATTACK_ILLAGER = 4;

    // ── Synced data ────────────────────────────────────────────────────────
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TYPE =
            SynchedEntityData.defineId(BlizzaEntity.class, EntityDataSerializers.INT);
    /** Health fraction synced to client for the custom HUD boss bar. */
    public static final EntityDataAccessor<Float> DATA_HEALTH_SYNC =
            SynchedEntityData.defineId(BlizzaEntity.class, EntityDataSerializers.FLOAT);
    /**
     * True while an Eye of the Storm is active near Blizza.
     * Synced so the renderer can spawn hand particles.
     */
    public static final EntityDataAccessor<Boolean> DATA_CASTING =
            SynchedEntityData.defineId(BlizzaEntity.class, EntityDataSerializers.BOOLEAN);

    // ── GeckoLib ──────────────────────────────────────────────────────────
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // ── State ─────────────────────────────────────────────────────────────
    private boolean isPhase2         = false;
    private boolean phase2Spawned    = false;
    /**
     * Set to true when HP hits 0 — blocks further damage, runs defeat sequence.
     * Once set, we also lock health at 1 so vanilla cleanup code never fires.
     */
    private boolean isDefeated       = false;
    /** Counts ticks AFTER defeat is triggered (animation + hold + explosion). */
    private int defeatHoldTicks      = 0;
    private boolean lootDropped      = false;

    private int lastAttack           = ATTACK_NONE;

    // Idle animation inter-play cooldown
    private int idleCooldown         = 0;
    private static final int IDLE_PAUSE = 30; // 1.5 s pause between idle loops

    // Anti-camp mechanic tracking
    private int meleeProximityTicks  = 0;
    private static final int MELEE_PUNISH_THRESHOLD = 80; // 4 seconds

    // Reference to the magic goal so we can call tickCooldown() each tick
    private BlizzaMagicAttackGoal magicGoal;

    // ── Constructor ───────────────────────────────────────────────────────
    public BlizzaEntity(EntityType<? extends BlizzaEntity> type, Level level) {
        super(type, level);
        this.xpReward = 100;
    }

    // ── Attributes ────────────────────────────────────────────────────────
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,     500.0D)
                .add(Attributes.ARMOR,           15.0D)
                .add(Attributes.MOVEMENT_SPEED,  0.23D)
                .add(Attributes.FOLLOW_RANGE,    48.0D)
                .add(Attributes.ATTACK_DAMAGE,   12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_TYPE, ATTACK_NONE);
        builder.define(DATA_HEALTH_SYNC, 1.0f);
        builder.define(DATA_CASTING, false);
    }

    // ── AI ────────────────────────────────────────────────────────────────
    @Override
    protected void registerGoals() {
        magicGoal = new BlizzaMagicAttackGoal(this);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new BlizzaMeleeAttackGoal(this));
        this.goalSelector.addGoal(3, magicGoal);
        this.goalSelector.addGoal(4, new MoveTowardsTargetGoal(this, 0.9D, 32f));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // Issue #6: target player summons too (identified by OririSummoned NBT flag)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, 
                (target) -> target.getPersistentData().getBoolean("OririSummoned")));
    }

    // ── Tick ──────────────────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // Issue #5: permanent fire/frost/cold immunity — keep it re-applied every second
            if (this.tickCount % 20 == 0 && !isDefeated) {
                this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
                this.setTicksFrozen(0);
            }

            // Sync health fraction every tick for HUD
            float maxHp = (float) this.getAttributeValue(Attributes.MAX_HEALTH);
            this.entityData.set(DATA_HEALTH_SYNC, this.getHealth() / maxHp);

            // Phase 2 trigger
            if (!isDefeated && !isPhase2 && this.getHealth() <= maxHp * 0.5f) {
                triggerPhase2();
            }

            // Issue #7: check if an Eye of the Storm is near Blizza
            boolean stormNearby = isEyeOfStormActive();
            this.entityData.set(DATA_CASTING, stormNearby);

            // Anti-camp mechanic: If a player stays within 5 blocks for 4 seconds, hit them with Slowness III for 3s
            LivingEntity target = getTarget();
            if (target instanceof Player p && !p.isCreative() && !p.isSpectator() && this.distanceToSqr(p) <= 25.0) {
                meleeProximityTicks++;
                if (meleeProximityTicks >= MELEE_PUNISH_THRESHOLD) {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2)); // 3 seconds, Slowness 3
                    meleeProximityTicks = 0;
                }
            } else {
                if (meleeProximityTicks > 0) meleeProximityTicks--;
            }

            // Tick magic goal cooldown
            if (magicGoal != null) magicGoal.tickCooldown();

            // Defeat sequence
            if (isDefeated) {
                tickDefeatSequence();
            }
        }

        // Idle cooldown tick (both sides – purely cosmetic)
        if (idleCooldown > 0) idleCooldown--;
    }

    /** Checks if an active EyeOfTheStormEntity exists within 16 blocks of Blizza. */
    public boolean isEyeOfStormActive() {
        AABB box = this.getBoundingBox().inflate(16.0);
        return !this.level().getEntitiesOfClass(EyeOfTheStormEntity.class, box, e -> e.isAlive()).isEmpty();
    }

    /** For the goalSelector – don't run AI goals while in defeat sequence. */
    @Override
    public boolean isNoAi() {
        return isDefeated || super.isNoAi();
    }

    // ── Phase 2 ───────────────────────────────────────────────────────────
    private void triggerPhase2() {
        isPhase2 = true;
        if (!level().isClientSide && !phase2Spawned) {
            phase2Spawned = true;
            spawnStrays();
        }
    }

    private void spawnStrays() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        int[][] offsets = {{3, 0, 0}, {-3, 0, 0}, {0, 0, 3}, {0, 0, -3}};
        for (int[] off : offsets) {
            Stray stray = EntityType.STRAY.create(serverLevel);
            if (stray == null) continue;
            stray.setPos(this.getX() + off[0], this.getY() + off[1], this.getZ() + off[2]);
            stray.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(this.blockPosition()),
                    net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
            serverLevel.addFreshEntity(stray);
        }
    }

    // ── Defeat sequence ───────────────────────────────────────────────────

    /** Trigger the defeat — locks health at 1, blocks AI, starts explosion show. */
    private void startDefeated() {
        isDefeated = true;
        defeatHoldTicks = 0;
        lootDropped = false;
        // Lock health at 1 so vanilla entity cleanup never fires during the sequence
        this.setHealth(1.0f);
        this.setAttackType(ATTACK_NONE);
        triggerAnim("death_controller", "blizza_defeat");
    }

    private void tickDefeatSequence() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        defeatHoldTicks++;

        // Defeat animation runs for ~15 ticks, then we hold and explode for 100 ticks
        if (defeatHoldTicks > 15 && defeatHoldTicks <= 115) {
            if (defeatHoldTicks % 10 == 0) {
                double rx = this.random.nextGaussian() * 0.8;
                double ry = this.random.nextDouble() * 2.5;
                double rz = this.random.nextGaussian() * 0.8;
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        this.getX() + rx, this.getY() + ry, this.getZ() + rz,
                        1, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        this.getX() + rx, this.getY() + ry, this.getZ() + rz,
                        1, 0, 0, 0, 0);
                serverLevel.playSound(null,
                        this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_EXPLODE,
                        SoundSource.HOSTILE,
                        0.6f + this.random.nextFloat() * 0.3f,
                        0.8f + this.random.nextFloat() * 0.4f);
            }

            if (defeatHoldTicks == 16 && !lootDropped) {
                lootDropped = true;
                dropBlizzaLoot(serverLevel);
            }
        }

        if (defeatHoldTicks > 115) {
            this.discard();
        }
    }

    private void dropBlizzaLoot(ServerLevel serverLevel) {
        net.minecraft.resources.ResourceLocation lootId =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        net.ganyusbathwater.oririmod.OririMod.MOD_ID, "entities/blizza");

        var registries = serverLevel.getServer().reloadableRegistries();
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootKey =
                net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.LOOT_TABLE, lootId);
        net.minecraft.world.level.storage.loot.LootTable lootTable =
                registries.getLootTable(lootKey);
        if (lootTable == net.minecraft.world.level.storage.loot.LootTable.EMPTY) return;

        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, this.damageSources().genericKill())
                .create(LootContextParamSets.ENTITY);

        List<ItemStack> drops = lootTable.getRandomItems(params);
        for (ItemStack stack : drops) {
            this.spawnAtLocation(stack);
        }
    }

    // ── Damage / death overrides ──────────────────────────────────────────
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Block ALL damage while running the defeat sequence
        if (isDefeated) return false;

        // Issue #5: ignore own EvokerFangs / cold aura damage
        if (source.getEntity() == this || source.getDirectEntity() == this) return false;

        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide()) {
            // Trigger hurt animation only if still alive (not freshly defeated)
            if (!isDefeated) {
                triggerAnim("hurt_controller", "blizza_hurt");
            }
        }
        return result;
    }

    @Override
    public void die(DamageSource source) {
        // Issue #2 fix: Don't call super.die(), instead start our custom defeat sequence.
        // Setting health to 1 in startDefeated() prevents vanilla from re-triggering die().
        if (!isDefeated) {
            startDefeated();
        }
    }

    /** Public accessor used by AI goals. */
    public boolean isDefeated() {
        return isDefeated;
    }

    // ── Phase-2 damage multiplier ─────────────────────────────────────────
    /** Called by the melee goal to apply damage with the phase 2 multiplier. */
    public void doMeleeHit(LivingEntity target) {
        float base = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = isPhase2 ? base * 1.25f : base;
        target.hurt(this.damageSources().mobAttack(this), damage);
    }

    // ── Spawn title ───────────────────────────────────────────────────────
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                if (this.distanceTo(player) <= 64.0) {
                    NetworkHandler.sendBlizzaTitleToPlayer(player);
                }
            }
        }
    }

    // ── Idle cooldown helpers (issue #1) ──────────────────────────────────
    public boolean isIdlePaused() { return idleCooldown > 0; }
    public void resetIdleCooldown() { idleCooldown = IDLE_PAUSE; }

    // ── Getters / setters used by AI goals ─────────────────────────────────
    public int getAttackType()  { return this.entityData.get(DATA_ATTACK_TYPE); }
    public void setAttackType(int type) { this.entityData.set(DATA_ATTACK_TYPE, type); }

    public int  getLastAttack() { return lastAttack; }
    public void setLastAttack(int a) { lastAttack = a; }

    public boolean isPhase2() { return isPhase2; }
    public boolean isCasting() { return this.entityData.get(DATA_CASTING); }

    /** Returns synced health fraction [0,1] – safe to call on client. */
    public float getHealthFraction() { return this.entityData.get(DATA_HEALTH_SYNC); }

    // ── Persistence ───────────────────────────────────────────────────────
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsPhase2",      isPhase2);
        tag.putBoolean("Phase2Spawned", phase2Spawned);
        tag.putBoolean("IsDefeated",    isDefeated);
        tag.putInt("DefeatHoldTicks",   defeatHoldTicks);
        tag.putBoolean("LootDropped",   lootDropped);
        tag.putInt("LastAttack",        lastAttack);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        isPhase2      = tag.getBoolean("IsPhase2");
        phase2Spawned = tag.getBoolean("Phase2Spawned");
        isDefeated    = tag.getBoolean("IsDefeated");
        defeatHoldTicks = tag.getInt("DefeatHoldTicks");
        lootDropped   = tag.getBoolean("LootDropped");
        lastAttack    = tag.getInt("LastAttack");
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        // Issue #1: idle arms with a pause between loops
        controllers.add(new AnimationController<>(this, "idle_arms_controller", 5, state -> {
            if (getAttackType() != ATTACK_NONE || isDefeated) return PlayState.STOP;
            if (isIdlePaused()) return PlayState.STOP;
            if (!state.getController().isPlayingTriggeredAnimation()) {
                resetIdleCooldown();
            }
            state.getController().setAnimation(RawAnimation.begin().thenPlayAndHold("blizza_idle_arms"));
            return PlayState.CONTINUE;
        }));

        // Issue #1: idle nose with a pause between loops
        controllers.add(new AnimationController<>(this, "idle_nose_controller", 5, state -> {
            if (getAttackType() != ATTACK_NONE || isDefeated) return PlayState.STOP;
            if (isIdlePaused()) return PlayState.STOP;
            state.getController().setAnimation(RawAnimation.begin().thenPlayAndHold("blizza_idle_nose"));
            return PlayState.CONTINUE;
        }));

        // Walk — plays while moving, stops when attacking
        controllers.add(new AnimationController<>(this, "walk_controller", 5, state -> {
            if (getAttackType() != ATTACK_NONE || isDefeated) return PlayState.STOP;
            if (state.isMoving()) {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("blizza_walk"));
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));

        // Attack — four triggerable animations
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("blizza_normal_attack",
                        RawAnimation.begin().then("blizza_normal_attack", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("blizza_magic_1",
                        RawAnimation.begin().then("blizza_magic_1", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("blizza_magic_2",
                        RawAnimation.begin().then("blizza_magic_2", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("blizza_magic_3",
                        RawAnimation.begin().then("blizza_magic_3", Animation.LoopType.PLAY_ONCE)));

        // Hurt — triggerable flash
        controllers.add(new AnimationController<>(this, "hurt_controller", 2, state -> PlayState.STOP)
                .triggerableAnim("blizza_hurt",
                        RawAnimation.begin().then("blizza_hurt", Animation.LoopType.PLAY_ONCE)));

        // Death — plays on defeat, holds last frame
        controllers.add(new AnimationController<>(this, "death_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("blizza_defeat",
                        RawAnimation.begin().then("blizza_defeat", Animation.LoopType.HOLD_ON_LAST_FRAME)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}
