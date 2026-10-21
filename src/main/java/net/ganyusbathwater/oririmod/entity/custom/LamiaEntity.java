package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.combat.IElementalEntity;
import net.ganyusbathwater.oririmod.world.GodsTrialData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class LamiaEntity extends Monster implements GeoEntity, IElementalEntity {

    public static final int ATTACK_NONE = 0;
    public static final int ATTACK_PUNCH = 1;
    public static final int ATTACK_LEAP = 2;
    public static final int ATTACK_CHOKEHOLD = 3;

    public static final EntityDataAccessor<Integer> DATA_ATTACK_STATE = SynchedEntityData.defineId(
            LamiaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_SCALES_COLOR = SynchedEntityData.defineId(
            LamiaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_HAIR_COLOR = SynchedEntityData.defineId(
            LamiaEntity.class, EntityDataSerializers.INT);

    public record LamiaColorTheme(int scalesColor, int hairColor) {}

    public static final LamiaColorTheme[] NATURE_THEMES = {
            new LamiaColorTheme(0x2D7A47, 0x2E1D13), // Emerald green scales, dark chestnut hair
            new LamiaColorTheme(0x1F5C3B, 0xD8B467), // Deep jungle green scales, honey blonde hair
            new LamiaColorTheme(0x234F32, 0x53463B), // Pine green scales, ash brown hair
            new LamiaColorTheme(0x556B2F, 0x3D2817), // Dark olive green scales, walnut hair
            new LamiaColorTheme(0x6B8E23, 0x733019), // Olive drab scales, auburn hair
            new LamiaColorTheme(0x7C8A4E, 0x8C7C68), // Willow sage scales, light taupe hair
            new LamiaColorTheme(0xBA6B24, 0x2E1D13), // Warm amber scales, dark chestnut hair
            new LamiaColorTheme(0xC28422, 0x733019), // Autumn ochre scales, auburn hair
            new LamiaColorTheme(0x9E472A, 0x9C4C23), // Terracotta copper scales, cinnamon hair
            new LamiaColorTheme(0x5C4033, 0xD8B467), // Deep loam earth scales, golden wheat hair
            new LamiaColorTheme(0x4A2F1B, 0x1A281E), // Bark brown scales, dark forest pine hair
            new LamiaColorTheme(0x3B2818, 0x733019)  // Dark chestnut scales, auburn hair
    };

    // Multi-part tail hitboxes
    public final LamiaPart tailBase;
    public final LamiaPart tailMid;
    public final LamiaPart tailEnd;
    private final LamiaPart[] subEntities;

    // Breadcrumb ring buffer for multipart position tracking
    private static final int BUFFER_SIZE = 64;
    private final double[] pastX = new double[BUFFER_SIZE];
    private final double[] pastY = new double[BUFFER_SIZE];
    private final double[] pastZ = new double[BUFFER_SIZE];
    private int bufferIndex = 0;

    // Combat timers
    private int attackTicks = 0;
    private int leapCooldown = 0;
    private LivingEntity chokedVictim = null;
    private int chokeholdTicks = 0;
    private float leapLockedYaw = 0.0F;
    private boolean leapImpacted = false;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public LamiaEntity(EntityType<? extends LamiaEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 15;

        this.tailBase = new LamiaPart(this, "tail_base", 0.75F, 0.6F);
        this.tailMid = new LamiaPart(this, "tail_mid", 0.65F, 0.5F);
        this.tailEnd = new LamiaPart(this, "tail_end", 0.55F, 0.4F);
        this.subEntities = new LamiaPart[]{this.tailBase, this.tailMid, this.tailEnd};

        for (int i = 0; i < BUFFER_SIZE; i++) {
            this.pastX[i] = this.getX();
            this.pastY[i] = this.getY();
            this.pastZ[i] = this.getZ();
        }
    }

    @Override
    public void push(Entity entity) {
        if (this.getAttackState() == ATTACK_CHOKEHOLD && entity == this.chokedVictim) {
            return;
        }
        super.push(entity);
    }

    @Override
    protected void doPush(Entity entity) {
        if (this.getAttackState() == ATTACK_CHOKEHOLD && entity == this.chokedVictim) {
            return;
        }
        super.doPush(entity);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // Lamia has a serpent body with no bipedal feet; moves completely silently
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_STATE, ATTACK_NONE);
        builder.define(DATA_SCALES_COLOR, NATURE_THEMES[0].scalesColor());
        builder.define(DATA_HAIR_COLOR, NATURE_THEMES[0].hairColor());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D) // 25 hearts
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D);
    }

    public static boolean checkLamiaSpawnRules(EntityType<LamiaEntity> type, ServerLevelAccessor level,
                                               MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                                                   MobSpawnType spawnType, net.minecraft.world.entity.SpawnGroupData spawnGroupData) {
        LamiaColorTheme theme = NATURE_THEMES[this.random.nextInt(NATURE_THEMES.length)];
        this.setScalesColor(theme.scalesColor());
        this.setHairColor(theme.hairColor());
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LamiaCombatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public Element getElement() {
        return Element.EARTH;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return this.subEntities;
    }

    public int getAttackState() {
        return this.entityData.get(DATA_ATTACK_STATE);
    }

    public void setAttackState(int state) {
        this.entityData.set(DATA_ATTACK_STATE, state);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.leapCooldown > 0) {
            this.leapCooldown--;
        }

        // Update past positions ring buffer
        this.pastX[this.bufferIndex] = this.getX();
        this.pastY[this.bufferIndex] = this.getY();
        this.pastZ[this.bufferIndex] = this.getZ();
        this.bufferIndex = (this.bufferIndex + 1) % BUFFER_SIZE;

        // Position sub-hitboxes along the trailing path
        updateTailParts();

        // Handle Active Attack States
        if (!this.level().isClientSide) {
            int state = getAttackState();

            if (state == ATTACK_CHOKEHOLD) {
                handleChokeholdTick();
            } else if (state == ATTACK_PUNCH) {
                this.attackTicks++;
                if (this.attackTicks >= 12) { // 0.6s animation
                    setAttackState(ATTACK_NONE);
                    this.attackTicks = 0;
                }
            } else if (state == ATTACK_LEAP) {
                this.attackTicks++;
                // Lock rotation during leap so mob doesn't whip 180 degrees
                this.setYRot(this.leapLockedYaw);
                this.yBodyRot = this.leapLockedYaw;
                this.yHeadRot = this.leapLockedYaw;
                this.getNavigation().stop();

                // Check impact mid-air or upon landing
                if (!this.leapImpacted && this.attackTicks >= 4) {
                    LivingEntity target = this.getTarget();
                    if (target != null && target.isAlive() && this.distanceToSqr(target) <= 6.25D) {
                        target.hurt(this.damageSources().mobAttack(this), 12.0F);
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0)); // 5s Poison
                        this.leapImpacted = true;
                    }
                }

                if (this.attackTicks >= 20) { // 1.0s animation
                    setAttackState(ATTACK_NONE);
                    this.attackTicks = 0;
                }
            }
        }
    }

    private void updateTailParts() {
        // Place tailBase ~8 ticks back, tailMid ~16 ticks back, tailEnd ~24 ticks back
        setPartPosition(this.tailBase, 8, 1.0D);
        setPartPosition(this.tailMid, 18, 2.2D);
        setPartPosition(this.tailEnd, 30, 3.5D);
    }

    private void setPartPosition(LamiaPart part, int ticksBack, double defaultDist) {
        int idx = (this.bufferIndex - ticksBack + BUFFER_SIZE) % BUFFER_SIZE;
        double targetX = this.pastX[idx];
        double targetY = this.pastY[idx];
        double targetZ = this.pastZ[idx];

        // Fallback when standing still: orient directly behind body yaw
        double distSq = this.distanceToSqr(targetX, targetY, targetZ);
        if (distSq < 0.2D) {
            float rad = (float) Math.toRadians(this.yBodyRot);
            targetX = this.getX() + Math.sin(rad) * defaultDist;
            targetY = this.getY();
            targetZ = this.getZ() - Math.cos(rad) * defaultDist;
        }

        part.setPos(targetX, targetY, targetZ);
        part.xo = targetX;
        part.yo = targetY;
        part.zo = targetZ;
        part.xOld = targetX;
        part.yOld = targetY;
        part.zOld = targetZ;
    }

    private void handleChokeholdTick() {
        if (this.chokedVictim == null || !this.chokedVictim.isAlive() || this.distanceToSqr(this.chokedVictim) > 16.0D) {
            endChokehold();
            return;
        }

        this.chokeholdTicks++;

        // Lamia stays completely stationary
        this.setDeltaMovement(Vec3.ZERO);
        this.getNavigation().stop();

        // Lock Lamia's yaw facing the victim
        Vec3 diff = this.chokedVictim.position().subtract(this.position());
        float targetYaw = (float) (Mth.atan2(diff.z, diff.x) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(targetYaw);
        this.yBodyRot = targetYaw;
        this.yHeadRot = targetYaw;

        // Position victim directly in front of the Lamia
        double frontX = this.getX() - Mth.sin(this.yBodyRot * Mth.DEG_TO_RAD) * 0.85D;
        double frontZ = this.getZ() + Mth.cos(this.yBodyRot * Mth.DEG_TO_RAD) * 0.85D;
        this.chokedVictim.setPos(frontX, this.getY(), frontZ);
        this.chokedVictim.setDeltaMovement(Vec3.ZERO);
        this.chokedVictim.fallDistance = 0.0F;

        // Apply pinning slowness
        this.chokedVictim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 5, false, false));

        // Crushing damage every 15 ticks (0.75s)
        if (this.chokeholdTicks % 15 == 0) {
            this.chokedVictim.hurt(this.damageSources().mobAttack(this), 2.5F);
        }

        if (this.chokeholdTicks >= 100) { // 5.0 seconds
            endChokehold();
        }
    }

    public void startChokehold(LivingEntity target) {
        this.chokedVictim = target;
        this.chokeholdTicks = 0;
        setAttackState(ATTACK_CHOKEHOLD);
        this.getNavigation().stop();
    }

    public void endChokehold() {
        this.chokedVictim = null;
        this.chokeholdTicks = 0;
        setAttackState(ATTACK_NONE);
    }

    public float getChokeholdChance() {
        if (this.level() instanceof ServerLevel sl && GodsTrialData.get(sl).isActive()) {
            return 0.10F; // 10% in God's Trial
        }
        if (this.level().getDifficulty() == Difficulty.HARD) {
            return 0.075F; // 7.5% on Hard
        }
        return 0.05F; // 5% on Normal / Easy
    }

    public void triggerLeapAttack(LivingEntity target) {
        setAttackState(ATTACK_LEAP);
        this.attackTicks = 0;
        this.leapCooldown = 90; // 4.5s cooldown
        this.leapImpacted = false;
        this.getNavigation().stop();

        Vec3 diff = target.position().subtract(this.position());
        double dist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        // Lock yaw directly facing the target
        this.leapLockedYaw = (float) (Mth.atan2(diff.z, diff.x) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(this.leapLockedYaw);
        this.yBodyRot = this.leapLockedYaw;
        this.yHeadRot = this.leapLockedYaw;

        // Scale horizontal impulse so she lands ~1.0 block in front of target, avoiding overshoot
        double targetDist = Math.max(0.5D, dist - 1.0D);
        double horizSpeed = Mth.clamp(targetDist * 0.16D, 0.35D, 0.75D);

        Vec3 horizontal = new Vec3(diff.x, 0, diff.z).normalize().scale(horizSpeed);
        this.setDeltaMovement(horizontal.x, 0.38D, horizontal.z);
    }

    public void triggerPunchAttack(LivingEntity target) {
        setAttackState(ATTACK_PUNCH);
        this.attackTicks = 0;
        this.doHurtTarget(target);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("LeapCooldown", this.leapCooldown);
        compound.putInt("ScalesColor", this.getScalesColor());
        compound.putInt("HairColor", this.getHairColor());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.leapCooldown = compound.getInt("LeapCooldown");
        if (compound.contains("ScalesColor")) {
            this.setScalesColor(compound.getInt("ScalesColor"));
        }
        if (compound.contains("HairColor")) {
            this.setHairColor(compound.getInt("HairColor"));
        }
    }

    public int getScalesColor() {
        return this.entityData.get(DATA_SCALES_COLOR);
    }

    public void setScalesColor(int color) {
        this.entityData.set(DATA_SCALES_COLOR, color);
    }

    public int getHairColor() {
        return this.entityData.get(DATA_HAIR_COLOR);
    }

    public void setHairColor(int color) {
        this.entityData.set(DATA_HAIR_COLOR, color);
    }

    // ── GeckoLib Animations ────────────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle_torso", 5, event ->
                event.setAndContinue(RawAnimation.begin().thenLoop("lamia_idle_torso"))));

        controllers.add(new AnimationController<>(this, "idle_ears", 5, event ->
                event.setAndContinue(RawAnimation.begin().thenLoop("lamia_idle_ears"))));

        controllers.add(new AnimationController<>(this, "movement", 4, event -> {
            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("lamia_walking"));
            }
            return PlayState.STOP;
        }));

        controllers.add(new AnimationController<>(this, "attack", 2, event -> {
            int state = getAttackState();
            if (state == ATTACK_PUNCH) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("lamia_attack_punch"));
            } else if (state == ATTACK_LEAP) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("lamia_attack_leap"));
            }
            event.getController().forceAnimationReset();
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }

    // ── Combat AI Goal ─────────────────────────────────────────────────────
    static class LamiaCombatGoal extends Goal {
        private final LamiaEntity lamia;
        private int attackDelay = 0;

        public LamiaCombatGoal(LamiaEntity lamia) {
            this.lamia = lamia;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.lamia.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = this.lamia.getTarget();
            if (target == null) return;

            this.lamia.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSq = this.lamia.distanceToSqr(target);

            if (this.lamia.getAttackState() == ATTACK_CHOKEHOLD) {
                return; // Stationary during chokehold
            }

            if (this.attackDelay > 0) {
                this.attackDelay--;
            }

            // Leap Attack at medium range (between 12.0 and 49.0 distSq = 3.5 to 7.0 blocks)
            if (distSq >= 12.25D && distSq <= 49.0D && this.lamia.leapCooldown <= 0 && this.lamia.hasLineOfSight(target)) {
                this.lamia.triggerLeapAttack(target);
                this.attackDelay = 25;
                return;
            }

            // Close Melee Attack
            if (distSq <= 6.25D && this.attackDelay <= 0) { // <= 2.5 blocks
                float roll = this.lamia.getRandom().nextFloat();
                if (roll < this.lamia.getChokeholdChance()) {
                    this.lamia.startChokehold(target);
                } else {
                    this.lamia.triggerPunchAttack(target);
                }
                this.attackDelay = 20;
            } else {
                this.lamia.getNavigation().moveTo(target, 1.0D);
            }
        }
    }
}
