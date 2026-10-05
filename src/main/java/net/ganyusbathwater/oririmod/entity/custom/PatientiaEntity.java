package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PatientiaEntity extends Monster implements GeoEntity, IOririBoss {

    // ── Attack-type constants ─────────────────────────────────────────────
    public static final int ATTACK_NONE             = 0;
    public static final int ATTACK_MELEE_RIGHT      = 1;
    public static final int ATTACK_MELEE_LEFT       = 2;
    public static final int ATTACK_DEFENSIVE        = 3;
    public static final int ATTACK_MAGIC_PROJECTILE = 4;
    public static final int ATTACK_MAGIC_CIRCLES    = 5;
    public static final int ATTACK_GIANT_SLICE      = 6;
    public static final int ATTACK_HEAVENLY_EXEC    = 7;

    // ── Synced data ────────────────────────────────────────────────────────────
    public static final EntityDataAccessor<Integer> DATA_ATTACK_TYPE =
            SynchedEntityData.defineId(PatientiaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> DATA_HEALTH_SYNC =
            SynchedEntityData.defineId(PatientiaEntity.class, EntityDataSerializers.FLOAT);

    // ── GeckoLib ──────────────────────────────────────────────────────────────
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // ── State variables ───────────────────────────────────────────────────────
    private boolean isDefeated = false;
    private int defeatTicks = 0;
    private boolean lootDropped = false;

    // ── Defensive Buzzsaw State ─────────────────────────────────────────────
    private int defensiveTimer = 0;
    private int defensiveCooldown = 400; // 20s
    private int consecutiveMeleeHits = 0;
    private int hitDecayTimer = 0;
    private int buzzsawTickTimer = 0;

    // ── Combat AI Variables ──────────────────────────────────────────────────
    private int globalAttackCooldown = 60; // 3 seconds before first attack
    public net.minecraft.core.BlockPos arenaCenter = null;
    
    // ── Constructor ───────────────────────────────────────────────────────────
    public PatientiaEntity(EntityType<? extends PatientiaEntity> type, Level level) {
        super(type, level);
        this.xpReward = 200;
        this.getPersistentData().putBoolean("IsNoxusMob", true);
    }

    // ── Attributes ────────────────────────────────────────────────────────────
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,          700.0D)
                .add(Attributes.ARMOR,                15.0D)
                .add(Attributes.MOVEMENT_SPEED,        0.23D)
                .add(Attributes.FOLLOW_RANGE,          64.0D)
                .add(Attributes.ATTACK_DAMAGE,         12.5D)
                .add(Attributes.KNOCKBACK_RESISTANCE,   1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_TYPE,  ATTACK_NONE);
        builder.define(DATA_HEALTH_SYNC,  1.0f);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new net.ganyusbathwater.oririmod.entity.ai.PatientiaAttackGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 20f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Target nearest player OR non-Noxus summoned mobs (Identical to Deviartras)
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                target -> target.getPersistentData().getBoolean("OririSummoned")));
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

        if (this.isDefeated) {
            this.setYRot(this.yRotO);
            this.setXRot(this.xRotO);
            this.yBodyRot = this.yBodyRotO;
            this.yHeadRot = this.yHeadRotO;
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        }

        if (!this.level().isClientSide) {
            float maxHp = (float) this.getAttributeValue(Attributes.MAX_HEALTH);
            float fraction = this.getHealth() / maxHp;
            this.entityData.set(DATA_HEALTH_SYNC, fraction);

            // ── Defensive Buzzsaw State ──────────────────────────────────────────
            if (hitDecayTimer > 0) {
                hitDecayTimer--;
                if (hitDecayTimer == 0) consecutiveMeleeHits = 0;
            }

            if (defensiveCooldown > 0) {
                defensiveCooldown--;
            }

            if (defensiveTimer > 0) {
                defensiveTimer--;
                this.setDeltaMovement(0, this.getDeltaMovement().y, 0); // Stationary while defending
                this.setYRot(this.yRotO);
                
                // Buzzsaw damage tick every 10 ticks (0.5s)
                buzzsawTickTimer++;
                if (buzzsawTickTimer >= 10) {
                    buzzsawTickTimer = 0;
                    if (this.level() instanceof ServerLevel sl) {
                        net.minecraft.world.phys.AABB sweepBox = this.getBoundingBox().inflate(3.0);
                        java.util.List<Player> hitPlayers = sl.getEntitiesOfClass(Player.class, sweepBox);
                        for (Player hit : hitPlayers) {
                            hit.hurt(this.damageSources().mobAttack(this), 8.0f);
                            hit.invulnerableTime = 0; // Bypass i-frames
                            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, hit.getX(), hit.getY() + 1.0, hit.getZ(), 1, 0, 0, 0, 0);
                        }
                    }
                }

                if (defensiveTimer == 0) {
                    this.setAttackType(ATTACK_NONE);
                    this.triggerAnim("attack_controller", "stop");
                }
            }

            // ── Defeat sequence ───────────────────────────────────────────────
            if (isDefeated) {
                tickDefeatSequence();
            }
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (this.arenaCenter == null) {
            this.arenaCenter = this.blockPosition();
        }
        if (!this.level().isClientSide && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            long playersCount = 0;
            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                if (this.distanceTo(player) <= 64.0f) {
                    net.ganyusbathwater.oririmod.network.NetworkHandler.sendPatientiaTitle(player);
                    playersCount++;
                }
            }
            if (playersCount > 1) {
                double baseHp = this.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                double scalar = 1.0 + (0.1 * (playersCount - 1));
                this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(baseHp * scalar);
                this.setHealth((float)(baseHp * scalar));
            }
        }
    }

    // ── Resistances & Defense Triggers ────────────────────────────────────────
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (isDefeated) {
                this.discard();
                return true;
            }
        } else {
            if (isDefeated || defensiveTimer > 0) return false; // Immune during death or buzzsaw
        }

        // Immune to own magic
        if (source.getEntity() == this || source.getDirectEntity() == this) return false;

        boolean result = super.hurt(source, amount);

        if (result && !this.level().isClientSide) {
            // Track consecutive melee hits for Defensive Buzzsaw
            if (source.getEntity() instanceof LivingEntity && !source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) && source.getDirectEntity() == source.getEntity()) {
                consecutiveMeleeHits++;
                hitDecayTimer = 60; // 3 seconds to keep hitting
                
                if (consecutiveMeleeHits >= 4 && defensiveCooldown <= 0 && defensiveTimer <= 0) {
                    consecutiveMeleeHits = 0;
                    defensiveTimer = 100; // 5 seconds invincible
                    defensiveCooldown = 400; // 20 seconds cooldown
                    buzzsawTickTimer = 0;
                    this.setAttackType(ATTACK_DEFENSIVE);
                    triggerAnim("attack_controller", "patientia_defensive");
                }
            }
            // Trigger hurt animation
            triggerAnim("hurt_controller", "patientia_hurt");
        }
        return result;
    }

    // ── Defeat sequence ───────────────────────────────────────────────
    @Override
    public void die(DamageSource source) {
        if (!isDefeated) {
            isDefeated = true;
            this.triggerAnim("attack_controller", "stop");
            this.triggerAnim("weapons_controller", "stop");
            this.triggerAnim("death_controller", "patientia_defeat");
            defeatTicks = 0;
            lootDropped = false;
            this.setHealth(1.0f);
            this.setAttackType(ATTACK_NONE);
            triggerAnim("death_controller", "patientia_defeat");
        }
    }

    private void tickDefeatSequence() {
        defeatTicks++;
        // Animation length should dictate when loot drops. Let's assume 100 ticks for now.
        if (defeatTicks >= 100) {
            if (!lootDropped) {
                lootDropped = true;
                dropLoot();
            }
            this.discard();
        }
    }

    private void dropLoot() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        
        long playersCount = sl.players().stream().filter(p -> p.distanceTo(this) <= 64.0).count();
        if (playersCount < 1) playersCount = 1;
        
        int drops = 10 + this.random.nextInt(23); // [10, 32]
        drops *= playersCount; 
        
        for (int i = 0; i < drops; i++) {
            this.spawnAtLocation(new ItemStack(net.ganyusbathwater.oririmod.item.ModItems.CRYSTAL_INGOT.get()));
        }
        
        if (net.ganyusbathwater.oririmod.world.GodsTrialData.get(sl).isActive()) {
            this.spawnAtLocation(new ItemStack(net.ganyusbathwater.oririmod.item.ModItems.ORAPHIM_BOW.get()));
        }
    }

    // ── AI States ─────────────────────────────────────────────────────────────
    public int getAttackType() { return this.entityData.get(DATA_ATTACK_TYPE); }
    public void setAttackType(int type) { this.entityData.set(DATA_ATTACK_TYPE, type); }
    public float getHealthFraction() { return this.entityData.get(DATA_HEALTH_SYNC); }
    public boolean isDefeated() { return isDefeated; }

    @Override
    public boolean isNoAi() {
        return isDefeated || defensiveTimer > 0 || super.isNoAi();
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target.getType().is(net.ganyusbathwater.oririmod.entity.custom.NoxusKnightEntity.NOXUS_MOBS) || target.getPersistentData().getBoolean("IsNoxusMob")) {
            return false;
        }
        return super.canAttack(target);
    }
    
    // ── Multiplayer Boss Scaling ──────────────────────────────────────────
    @Override
    public void healFromSoulTithe() {
        if (this.isAlive() && !isDefeated) {
            this.heal(this.getMaxHealth() * 0.1f);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + this.getBbHeight() / 2.0, this.getZ(), 30, 0.5, 0.5, 0.5, 0.05);
            }
        }
    }

    // ── Save Data ─────────────────────────────────────────────────────────────
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsDefeated", isDefeated);
        tag.putInt("DefeatTicks", defeatTicks);
        tag.putBoolean("LootDropped", lootDropped);
        tag.putInt("DefensiveTimer", defensiveTimer);
        tag.putInt("DefensiveCooldown", defensiveCooldown);
        if (arenaCenter != null) {
            tag.putLong("ArenaCenter", arenaCenter.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        isDefeated = tag.getBoolean("IsDefeated");
        defeatTicks = tag.getInt("DefeatTicks");
        lootDropped = tag.getBoolean("LootDropped");
        defensiveTimer = tag.getInt("DefensiveTimer");
        defensiveCooldown = tag.getInt("DefensiveCooldown");
        if (tag.contains("ArenaCenter")) {
            arenaCenter = net.minecraft.core.BlockPos.of(tag.getLong("ArenaCenter"));
        }
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Movement controller
        controllers.add(new AnimationController<>(this, "movement_controller", 5, state -> {
            if (getAttackType() != ATTACK_NONE || isDefeated) return PlayState.STOP;
            if (state.isMoving()) {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("patientia_moving"));
                return PlayState.CONTINUE;
            }
            state.getController().setAnimation(RawAnimation.begin().thenLoop("patientia_idle"));
            return PlayState.CONTINUE;
        }));

        // Attack controller
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("patientia_normal_attack_right", RawAnimation.begin().then("patientia_normal_attack_right", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("patientia_normal_attack_left", RawAnimation.begin().then("patientia_normal_attack_left", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("patientia_defensive", RawAnimation.begin().then("patientia_defensive", Animation.LoopType.HOLD_ON_LAST_FRAME))
                .triggerableAnim("stop", RawAnimation.begin()));

        // Hurt controller
        controllers.add(new AnimationController<>(this, "hurt_controller", 2, state -> PlayState.STOP)
                .triggerableAnim("patientia_hurt", RawAnimation.begin().then("patientia_hurt", Animation.LoopType.PLAY_ONCE)));

        // Death controller
        controllers.add(new AnimationController<>(this, "death_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("patientia_defeat", RawAnimation.begin().then("patientia_defeat", Animation.LoopType.HOLD_ON_LAST_FRAME)));

        // Weapons controller
        controllers.add(new AnimationController<>(this, "weapons_controller", 0, state -> {
            if (isDefeated) return PlayState.STOP;
            int attack = getAttackType();
            if (attack == ATTACK_MAGIC_PROJECTILE || attack == ATTACK_MAGIC_CIRCLES || attack == ATTACK_GIANT_SLICE || attack == ATTACK_HEAVENLY_EXEC || attack == ATTACK_DEFENSIVE) {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("patientia_swords_special_attack"));
            } else {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("patientia_swords_idle"));
            }
            return PlayState.CONTINUE;
        })
                .triggerableAnim("stop", RawAnimation.begin()));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}
