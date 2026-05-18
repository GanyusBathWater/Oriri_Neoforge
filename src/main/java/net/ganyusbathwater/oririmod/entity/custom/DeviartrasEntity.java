package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.entity.ai.DeviartrasAttackGoal;
import net.ganyusbathwater.oririmod.entity.ai.DeviartrasFleeGoal;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Deviartras — Nature Boss
 *
 * ─── Stats ──────────────────────────────────────────────────────────────────
 *   HP            : 600
 *   Armor         : 10
 *   Speed         : 0.23 (standard zombie, can sprint)
 *   Knockback Res : 1.0 (immune)
 *
 * ─── Phase System ───────────────────────────────────────────────────────────
 *   Phase 2 triggers at ≤ 300 HP (50%).
 *   All mob-summon skills then spawn +1 extra unit.
 *
 * ─── Attack Roster ──────────────────────────────────────────────────────────
 *   Melee         – 10 dmg + Poison 5s  (no global CD gate)
 *   Passive Vex   – on-hit, own 600-t CD, spawns 1 (+1 in P2) Vex
 *   Skill 1 – Overgrowth  : summons plant_turret(s), dirt particles ticks 15-20
 *   Skill 2 – Vine Lock   : placeholder mechanic (see vineLockMechanic())
 *   Skill 3 – Spore Blossom: summons eye_of_desolation(s) at exact tick 20
 *
 * ─── Death Sequence ─────────────────────────────────────────────────────────
 *   Step 1: Play deviartras_defeat (40 t)
 *   Step 2: Play deviartras_defeat_idle (loop)
 *   Step 3: Hold idle for 100 t
 *   Step 4: Drop 10-32 ancient_ingot, discard()
 *
 * ─── GeckoLib Controllers ───────────────────────────────────────────────────
 *   movement_controller – idle / walk
 *   attack_controller   – all attack animations (triggerable)
 *   hurt_controller     – hurt flash (triggerable)
 *   death_controller    – defeat sequence (triggerable then state-driven)
 */
public class DeviartrasEntity extends Monster implements GeoEntity {

    // ── Attack-type constants (used by movement/attack controller) ─────────────
    public static final int ATTACK_NONE         = 0;
    public static final int ATTACK_MELEE        = 1;
    public static final int ATTACK_OVERGROWTH   = 2;
    public static final int ATTACK_VINE_LOCK    = 3;
    public static final int ATTACK_SPORE_BLOSSOM = 4;

    // ── Synced data ────────────────────────────────────────────────────────────
    /** Current attack type, synced so client-side animation controller can branch. */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TYPE =
            SynchedEntityData.defineId(DeviartrasEntity.class, EntityDataSerializers.INT);
    /** Health fraction [0,1] — synced for HUD rendering. */
    public static final EntityDataAccessor<Float> DATA_HEALTH_SYNC =
            SynchedEntityData.defineId(DeviartrasEntity.class, EntityDataSerializers.FLOAT);

    // ── GeckoLib ──────────────────────────────────────────────────────────────
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // ── Boss-bar ───────────────────────────────────────────────────────────────
    /**
     * ServerBossEvent drives the vanilla boss-bar HUD.
     * The two custom textures ("deviartras_progress" and "deviartrass_boss_bar")
     * are supplied via the BossEvent.BarColor / BarOverlay pairs that Forge
     * maps to the texture atlas names.  As NeoForge 1.21.1 uses the vanilla
     * boss-bar rendering pipeline, the matching bar colour is chosen by name
     * convention: PURPLE maps to the "deviartrass_boss_bar" atlas entry and
     * NOTCHED_20 overlay maps to "deviartras_progress" detail strip.
     */
    // Removed ServerBossEvent because we render it entirely on the client side like Blizza.

    // ── Phase & defeat state ───────────────────────────────────────────────────
    private boolean isPhase2      = false;
    private boolean isDefeated    = false;
    /**
     * Counts ticks since defeat was triggered.
     *
     *   0  – 40  → deviartras_defeat animation plays
     *  40  – 140 → deviartras_defeat_idle loops (100 t hold)
     *  > 140     → drop loot, discard()
     */
    private int     defeatTicks   = 0;
    private boolean lootDropped   = false;

    // ── Passive Vex cooldown ───────────────────────────────────────────────────
    /** Independent passive cooldown – 600 t (30 s). Does NOT share the global CD. */
    private int vexPassiveCooldown = 0;

    // ── Hit & Run mechanic ─────────────────────────────────────────────────────
    /**
     * Counts consecutive ticks the target spends within MELEE_FLEE_RADIUS.
     * When this reaches FLEE_TRIGGER_TICKS the boss sprints away.
     */
    private int meleeProximityTicks = 0;
    private static final double MELEE_FLEE_RADIUS_SQ = 9.0;  // 3 blocks²
    private static final int    FLEE_TRIGGER_TICKS   = 100;  // 5 s

    // ── Concurrent Melee State ─────────────────────────────────────────────────
    private int meleeCooldown = 0;
    private int meleeAnimTimer = 0;

    // ── References to AI goals (for coordinating cooldowns) ───────────────────
    private DeviartrasAttackGoal attackGoal;
    private DeviartrasFleeGoal   fleeGoal;

    // ── Constructor ───────────────────────────────────────────────────────────
    public DeviartrasEntity(EntityType<? extends DeviartrasEntity> type, Level level) {
        super(type, level);
        this.xpReward = 150;
    }

    // ── Attributes ────────────────────────────────────────────────────────────
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,          600.0D)
                .add(Attributes.ARMOR,                10.0D)
                .add(Attributes.MOVEMENT_SPEED,        0.23D)
                .add(Attributes.FOLLOW_RANGE,          48.0D)
                .add(Attributes.ATTACK_DAMAGE,         10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE,   1.0D);
    }

    // ── Synced data init ───────────────────────────────────────────────────────
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_TYPE,  ATTACK_NONE);
        builder.define(DATA_HEALTH_SYNC,  1.0f);
    }

    // ── AI ────────────────────────────────────────────────────────────────────
    @Override
    protected void registerGoals() {
        attackGoal = new DeviartrasAttackGoal(this);
        fleeGoal   = new DeviartrasFleeGoal(this);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, fleeGoal);
        this.goalSelector.addGoal(3, attackGoal);
        // Keep distance – prefers staying 8+ blocks from target for ranged attacks
        this.goalSelector.addGoal(4, new MoveTowardsTargetGoal(this, 0.9D, 32f));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 20f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Target nearest player OR OririSummoned allies
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                target -> target.getPersistentData().getBoolean("OririSummoned")));
    }


    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {

            float maxHp = (float) this.getAttributeValue(Attributes.MAX_HEALTH);
            float fraction = this.getHealth() / maxHp;
            this.entityData.set(DATA_HEALTH_SYNC, fraction);

            // ── Phase 2 trigger ───────────────────────────────────────────────
            if (!isDefeated && !isPhase2 && this.getHealth() <= 300.0f) {
                isPhase2 = true;
            }

            // ── Hit & Run mechanic ────────────────────────────────────────────
            // If target camps within 3 blocks for 100 consecutive ticks (5 s)
            // the FleeGoal receives a kick-start signal.
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && !isDefeated) {
                if (this.distanceToSqr(target) <= MELEE_FLEE_RADIUS_SQ) {
                    meleeProximityTicks++;
                    if (meleeProximityTicks >= FLEE_TRIGGER_TICKS) {
                        meleeProximityTicks = 0;
                        fleeGoal.triggerFlee(); // see DeviartrasFleeGoal
                    }
                } else {
                    if (meleeProximityTicks > 0) meleeProximityTicks--;
                }
            } else {
                meleeProximityTicks = 0;
            }

            // ── Passive Vex cooldown tick ─────────────────────────────────────
            if (vexPassiveCooldown > 0) vexPassiveCooldown--;

            // ── Concurrent Melee Logic ────────────────────────────────────────
            if (meleeCooldown > 0) meleeCooldown--;
            if (meleeAnimTimer > 0) {
                meleeAnimTimer--;
                if (meleeAnimTimer == 15 - 7) { // Hit frame 7 (of 15)
                    if (target != null && target.isAlive() && this.distanceToSqr(target) <= 16.0) {
                        target.hurt(this.damageSources().mobAttack(this), 10.0f);
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true));
                    }
                }
            } else if (target != null && target.isAlive() && !isDefeated) {
                if (this.distanceToSqr(target) <= 16.0 && meleeCooldown <= 0) {
                    meleeAnimTimer = 15;
                    meleeCooldown = 40; // 2 seconds between melee swings
                    triggerAnim("melee_controller", "deviartras_normal_attack");
                }
            }

            // ── Defeat sequence ───────────────────────────────────────────────
            if (isDefeated) {
                tickDefeatSequence();
            }

            // ── Delegate attack goal cooldown tick ────────────────────────────
            if (attackGoal != null) attackGoal.tickGlobalCooldown();
        }
    }

    // ── Boss-bar lifecycle ─────────────────────────────────────────────────────
    // Removed startSeenByPlayer and stopSeenByPlayer to prevent the vanilla boss bar from rendering.

    // ── Spawn intro title ──────────────────────────────────────────────────────
    /**
     * When Deviartras enters the world, every player within 64 blocks receives
     * the boss intro title — identical to the Blizza pattern.
     */
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                if (this.distanceTo(player) <= 64.0f) {
                    NetworkHandler.sendDeviartrasTitle(player);
                }
            }
        }
    }

    // ── Resistances & Weaknesses ──────────────────────────────────────────────
    /**
     * Intercepts all incoming damage before it is applied.
     *
     * Immunities  : Poison, Fatal Poison, Slowness, Blindness, Weakness, Levitation
     *               → enforced via removeMobEffect() in the effect-application
     *                 callback at the bottom of this method and via the
     *                 canBeAffectedBy() override below.
     *
     * Fire weakness: 1.5× multiplier applied here.
     * Wither weakness: 1.5× multiplier applied here (damage type tag check).
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isDefeated) return false; // Block all damage during death sequence

        // ── Apply weaknesses ──────────────────────────────────────────────────
        boolean isFire   = source.is(DamageTypes.ON_FIRE)
                        || source.is(DamageTypes.IN_FIRE)
                        || source.is(DamageTypes.LAVA)
                        || source.is(DamageTypes.FIREBALL);
        boolean isWither = source.is(DamageTypes.WITHER)
                        || source.is(DamageTypes.WITHER_SKULL);

        if (isFire || isWither) {
            amount *= 1.5f;
        }

        boolean result = super.hurt(source, amount);

        if (result && !this.level().isClientSide) {
            // ── Trigger hurt animation ────────────────────────────────────────
            triggerAnim("hurt_controller", "deviartras_hurt");

            // ── Passive: Vex spawn on any hit ─────────────────────────────────
            // Independent 600-tick cooldown (30 s), no animation required.
            if (vexPassiveCooldown <= 0) {
                vexPassiveCooldown = 600;
                spawnVex();
            }
        }
        return result;
    }

    /**
     * Prevents immune mob effects from being applied to Deviartras.
     * Overriding addEffect() is the correct NeoForge 1.21.1 hook — canBeAffectedBy
     * does not exist on LivingEntity in this mapping.
     */
    @Override
    public boolean addEffect(MobEffectInstance effectInstance, @javax.annotation.Nullable net.minecraft.world.entity.Entity entity) {
        var type = effectInstance.getEffect();
        if (type == MobEffects.POISON
         || type == MobEffects.WITHER
         || type == MobEffects.MOVEMENT_SLOWDOWN
         || type == MobEffects.BLINDNESS
         || type == MobEffects.WEAKNESS
         || type == MobEffects.LEVITATION) {
            return false; // silently reject — immunity
        }
        return super.addEffect(effectInstance, entity);
    }

    // ── Death override ────────────────────────────────────────────────────────
    @Override
    public void die(DamageSource source) {
        // Do NOT call super.die() — we intercept and run our custom sequence.
        if (!isDefeated) {
            startDefeatSequence();
        }
    }

    // ── Defeat sequence internals ─────────────────────────────────────────────

    /** Kick off the defeat sequence: freeze AI, lock HP at 1, play defeat anim. */
    private void startDefeatSequence() {
        isDefeated  = true;
        defeatTicks = 0;
        lootDropped = false;
        // Lock health at 1 so vanilla entity cleanup never fires during the sequence
        this.setHealth(1.0f);
        this.setAttackType(ATTACK_NONE);
        // Step 1: play the defeat animation (40 t)
        triggerAnim("death_controller", "deviartras_defeat");
    }

    /**
     * Called every server tick while isDefeated == true.
     *
     * Timeline:
     *   tick  0 – 40  → deviartras_defeat animation (triggered in startDefeatSequence)
     *   tick 40       → switch to deviartras_defeat_idle (loop)
     *   tick 40 – 140 → hold in defeat_idle for 100 ticks (5 s)
     *   tick 140+     → drop loot and discard()
     */
    private void tickDefeatSequence() {
        defeatTicks++;

        // Step 2: at exactly tick 40, switch to the looping idle death anim
        if (defeatTicks == 40) {
            triggerAnim("death_controller", "deviartras_defeat_idle");
        }

        // Step 4: after 40 (defeat) + 100 (idle hold) = 140 ticks, drop loot & remove
        if (defeatTicks >= 140) {
            if (!lootDropped) {
                lootDropped = true;
                dropDeviartrasLoot();
            }
            this.discard();
        }
    }

    /**
     * Drops between 10 and 32 stacks of oririmod:ancient_ingot at the boss's
     * position. Each stack contains a single item so the player sees a spread
     * of pickups rather than one huge pile.
     */
    private void dropDeviartrasLoot() {
        int drops = 10 + this.random.nextInt(23); // [10, 32]
        for (int i = 0; i < drops; i++) {
            this.spawnAtLocation(new ItemStack(ModItems.ANCIENT_INGOT.get()));
        }
    }

    // ── Mob summon helpers (used by DeviartrasAttackGoal) ─────────────────────

    /**
     * Passive skill: spawns 1 Vex (Phase 2: 2).
     * Positions are randomised ±2 blocks around the boss.
     */
    public void spawnVex() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        int count = isPhase2 ? 2 : 1;
        for (int i = 0; i < count; i++) {
            Vex vex = EntityType.VEX.create(serverLevel);
            if (vex == null) continue;
            double ox = (random.nextDouble() - 0.5) * 4.0;
            double oz = (random.nextDouble() - 0.5) * 4.0;
            vex.setPos(this.getX() + ox, this.getY() + 1.0, this.getZ() + oz);
            vex.finalizeSpawn(serverLevel,
                    serverLevel.getCurrentDifficultyAt(this.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            vex.setOwner(this);
            serverLevel.addFreshEntity(vex);
        }
    }

    /**
     * Skill 1 – Overgrowth: spawns plant_turret(s) within a 16-block radius.
     * The actual particle burst at ticks 15-20 is driven by DeviartrasAttackGoal.
     */
    public void spawnPlantTurrets() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        int count = isPhase2 ? 2 : 1;
        for (int i = 0; i < count; i++) {
            Entity turret = ModEntities.VENOMOUS_PLANT.get().create(serverLevel);
            if (turret == null) continue;

            // Random XZ within 4-16 block radius
            double angle  = random.nextDouble() * Math.PI * 2;
            double radius = 4.0 + random.nextDouble() * 12.0;
            double tx = this.getX() + Math.cos(angle) * radius;
            double tz = this.getZ() + Math.sin(angle) * radius;

            // ── Ground-snap: find solid surface so the plant never spawns inside a block
            // Start from the boss's Y and scan up to 16 blocks down for ground,
            // then step up past any solid blocks to land on top of them.
            BlockPos candidate = BlockPos.containing(tx, this.getY() + 1, tz);
            // Step downward until we hit something solid (or reach world bottom)
            while (candidate.getY() > serverLevel.getMinBuildHeight()
                    && serverLevel.getBlockState(candidate.below()).getCollisionShape(serverLevel, candidate.below()).isEmpty()) {
                candidate = candidate.below();
            }
            // Step upward to clear any solid blocks at the candidate position
            while (!serverLevel.getBlockState(candidate).getCollisionShape(serverLevel, candidate).isEmpty()) {
                candidate = candidate.above();
            }

            turret.setPos(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5);
            serverLevel.addFreshEntity(turret);
        }
    }

    /**
     * Skill 3 – Spore Blossom: spawns spore_blossom(s).
     * Called by DeviartrasAttackGoal exactly at animation tick 20.
     */
    public void spawnSporeBlossom() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        int count = isPhase2 ? 2 : 1;
        for (int i = 0; i < count; i++) {
            Entity eye = ModEntities.SPORE_BLOSSOM.get().create(serverLevel);
            if (eye == null) continue;

            double ox = (random.nextDouble() - 0.5) * 6.0;
            double oz = (random.nextDouble() - 0.5) * 6.0;

            // ── Ground-snap: eyes spawn on the ground, never inside terrain
            BlockPos candidate = BlockPos.containing(this.getX() + ox, this.getY() + 1, this.getZ() + oz);
            while (candidate.getY() > serverLevel.getMinBuildHeight()
                    && serverLevel.getBlockState(candidate.below()).getCollisionShape(serverLevel, candidate.below()).isEmpty()) {
                candidate = candidate.below();
            }
            while (!serverLevel.getBlockState(candidate).getCollisionShape(serverLevel, candidate).isEmpty()) {
                candidate = candidate.above();
            }

            eye.setPos(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5);
            serverLevel.addFreshEntity(eye);
        }
    }

    // ── Helpers for AI goals ───────────────────────────────────────────────────
    public int  getAttackType()        { return this.entityData.get(DATA_ATTACK_TYPE); }
    public void setAttackType(int type){ this.entityData.set(DATA_ATTACK_TYPE, type); }
    public float getHealthFraction()   { return this.entityData.get(DATA_HEALTH_SYNC); }
    public boolean isPhase2()          { return isPhase2; }
    public boolean isDefeated()        { return isDefeated; }

    /** Suppress all AI goals while the defeat sequence is running. */
    @Override
    public boolean isNoAi() {
        return isDefeated || super.isNoAi();
    }

    // ── NBT persistence ────────────────────────────────────────────────────────
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsPhase2",        isPhase2);
        tag.putBoolean("IsDefeated",      isDefeated);
        tag.putInt("DefeatTicks",         defeatTicks);
        tag.putBoolean("LootDropped",     lootDropped);
        tag.putInt("VexPassiveCooldown",  vexPassiveCooldown);
        tag.putInt("MeleeProximityTicks", meleeProximityTicks);
        tag.putInt("MeleeCooldown",       meleeCooldown);
        tag.putInt("MeleeAnimTimer",      meleeAnimTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        isPhase2           = tag.getBoolean("IsPhase2");
        isDefeated         = tag.getBoolean("IsDefeated");
        defeatTicks        = tag.getInt("DefeatTicks");
        lootDropped        = tag.getBoolean("LootDropped");
        vexPassiveCooldown = tag.getInt("VexPassiveCooldown");
        meleeProximityTicks = tag.getInt("MeleeProximityTicks");
        meleeCooldown      = tag.getInt("MeleeCooldown");
        meleeAnimTimer     = tag.getInt("MeleeAnimTimer");
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        // ── Movement controller: idle / walk ──────────────────────────────────
        // Stops during any attack or defeat state so attack_controller can take over.
        controllers.add(new AnimationController<>(this, "movement_controller", 5, state -> {
            if (getAttackType() != ATTACK_NONE || isDefeated) return PlayState.STOP;
            if (state.isMoving()) {
                state.getController().setAnimation(
                        RawAnimation.begin().thenLoop("deviartras_walk"));
                return PlayState.CONTINUE;
            }
            state.getController().setAnimation(
                    RawAnimation.begin().thenLoop("deviartras_idle"));
            return PlayState.CONTINUE;
        }));

        // ── Melee controller (concurrent) ─────────────────────────────────────
        // This controller runs concurrently with the movement and attack controllers,
        // allowing melee swings even while casting a special.
        controllers.add(new AnimationController<>(this, "melee_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("deviartras_normal_attack",
                        RawAnimation.begin().then("deviartras_normal_attack", Animation.LoopType.PLAY_ONCE)));

        // ── Attack controller (specials only) ─────────────────────────────────
        // All special attack animations are registered as triggerable animations.
        // The goal calls triggerAnim("attack_controller", animName) to start them.
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("deviartras_overgrowth",
                        RawAnimation.begin().then("deviartras_overgrowth", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("deviartras_vine_lock",
                        RawAnimation.begin().then("deviartras_vine_lock", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("deviartras_spore_blossom",
                        RawAnimation.begin().then("deviartras_spore_blossom", Animation.LoopType.PLAY_ONCE)));

        // ── Hurt controller ───────────────────────────────────────────────────
        controllers.add(new AnimationController<>(this, "hurt_controller", 2, state -> PlayState.STOP)
                .triggerableAnim("deviartras_hurt",
                        RawAnimation.begin().then("deviartras_hurt", Animation.LoopType.PLAY_ONCE)));

        // ── Death / defeat controller ─────────────────────────────────────────
        // Step 1 (defeat anim) is triggered via triggerAnim from startDefeatSequence().
        // Step 2 (defeat_idle loop) is triggered via triggerAnim from tickDefeatSequence().
        controllers.add(new AnimationController<>(this, "death_controller", 0, state -> PlayState.STOP)
                // Play once, hold on last frame — boss lies frozen until idle kicks in
                .triggerableAnim("deviartras_defeat",
                        RawAnimation.begin().then("deviartras_defeat", Animation.LoopType.HOLD_ON_LAST_FRAME))
                // Loops indefinitely; the 100-tick hold is tracked in defeatTicks (server-side)
                .triggerableAnim("deviartras_defeat_idle",
                        RawAnimation.begin().then("deviartras_defeat_idle", Animation.LoopType.LOOP)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}
