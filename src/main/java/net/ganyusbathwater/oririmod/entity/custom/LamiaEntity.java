package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.combat.IElementalEntity;
import net.ganyusbathwater.oririmod.world.GodsTrialData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
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
import net.minecraft.world.phys.AABB;
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
    public static final EntityDataAccessor<Boolean> DATA_IS_HUNGRY = SynchedEntityData.defineId(
            LamiaEntity.class, EntityDataSerializers.BOOLEAN);

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

    // Multi-part tail hitboxes (8 segments covering the full ~6.9m tail)
    public final LamiaPart tailBase;
    public final LamiaPart tail2;
    public final LamiaPart tailMid;
    public final LamiaPart tail4;
    public final LamiaPart tail5;
    public final LamiaPart tail6;
    public final LamiaPart tail7;
    public final LamiaPart tailEnd;
    private final LamiaPart[] subEntities;

    // Breadcrumb ring buffer for multipart position tracking
    private static final int BUFFER_SIZE = 128;
    private final double[] pastX = new double[BUFFER_SIZE];
    private final double[] pastY = new double[BUFFER_SIZE];
    private final double[] pastZ = new double[BUFFER_SIZE];
    private int bufferIndex = 0;

    // Combat timers & hunger state
    private int attackTicks = 0;
    private int leapCooldown = 0;
    private int hungerCooldown = 6000; // ~5 minutes initial
    private LivingEntity chokedVictim = null;
    private int chokeholdTicks = 0;
    private float leapLockedYaw = 0.0F;
    private boolean leapImpacted = false;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public LamiaEntity(EntityType<? extends LamiaEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 15;

        this.tailBase = new LamiaPart(this, "tail_1", 0.75F, 0.60F);
        this.tail2 = new LamiaPart(this, "tail_2", 0.70F, 0.55F);
        this.tailMid = new LamiaPart(this, "tail_3", 0.65F, 0.50F);
        this.tail4 = new LamiaPart(this, "tail_4", 0.60F, 0.45F);
        this.tail5 = new LamiaPart(this, "tail_5", 0.55F, 0.40F);
        this.tail6 = new LamiaPart(this, "tail_6", 0.50F, 0.35F);
        this.tail7 = new LamiaPart(this, "tail_7", 0.45F, 0.30F);
        this.tailEnd = new LamiaPart(this, "tail_8", 0.40F, 0.25F);
        this.subEntities = new LamiaPart[]{
                this.tailBase, this.tail2, this.tailMid, this.tail4,
                this.tail5, this.tail6, this.tail7, this.tailEnd
        };

        for (int i = 0; i < this.subEntities.length; i++) {
            this.subEntities[i].setId(this.getId() + i + 1);
        }

        for (int i = 0; i < BUFFER_SIZE; i++) {
            this.pastX[i] = this.getX();
            this.pastY[i] = this.getY();
            this.pastZ[i] = this.getZ();
        }
        updateTailParts();
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
        builder.define(DATA_IS_HUNGRY, false);
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
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;

        // In lush caves, allow spawning even with light from glow berries
        Holder<Biome> biome = level.getBiome(pos);
        if (biome.is(ResourceLocation.withDefaultNamespace("lush_caves"))) {
            return checkMobSpawnRules(type, level, spawnType, pos, random);
        }

        // In surface swamps and moist forests: allow in light level <= 7 (canopies, dusk, night)
        return level.getRawBrightness(pos, 0) <= 7 && checkMobSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                                                   MobSpawnType spawnType, net.minecraft.world.entity.SpawnGroupData spawnGroupData) {
        LamiaColorTheme theme = NATURE_THEMES[this.random.nextInt(NATURE_THEMES.length)];
        this.setScalesColor(theme.scalesColor());
        this.setHairColor(theme.hairColor());
        this.hungerCooldown = 4000 + this.random.nextInt(4000); // 3.5 to 7 minutes before hungry
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LamiaCombatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Neutral mob: only defends if attacked
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

        // When hungry: hunts any living entity (with 5% chance on other Lamias)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                this::isValidPrey) {
            @Override
            public boolean canUse() {
                return LamiaEntity.this.isHungry() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return LamiaEntity.this.isHungry() && super.canContinueToUse();
            }
        });
    }

    public boolean isHungry() {
        return this.hungerCooldown <= 0;
    }

    public boolean isValidPrey(LivingEntity target) {
        if (target == null || !target.isAlive() || target == this) return false;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        // Cannibalism check: only a very small percentage (5%) to lock onto another Lamia
        if (target instanceof LamiaEntity) {
            return this.random.nextFloat() < 0.05F;
        }
        return true;
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

    @Override
    public void setId(int id) {
        super.setId(id);
        if (this.subEntities != null) {
            for (int i = 0; i < this.subEntities.length; i++) {
                this.subEntities[i].setId(id + i + 1);
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB box = this.getBoundingBox();
        if (this.subEntities != null) {
            for (LamiaPart part : this.subEntities) {
                box = box.minmax(part.getBoundingBox());
            }
        }
        return box;
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

        // Handle Active Attack States & Hunger Countdown
        if (!this.level().isClientSide) {
            if (this.hungerCooldown > -24000) {
                this.hungerCooldown--;
            }
            boolean hungry = isHungry();
            if (this.entityData.get(DATA_IS_HUNGRY) != hungry) {
                this.entityData.set(DATA_IS_HUNGRY, hungry);
            }

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

                // Check impact mid-air or upon landing
                if (!this.leapImpacted && this.attackTicks >= 4) {
                    LivingEntity target = this.getTarget();
                    if (target != null && target.isAlive() && this.distanceToSqr(target) <= 8.0D) {
                        target.hurt(this.damageSources().mobAttack(this), 12.0F);
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0)); // 5s Poison
                        this.leapImpacted = true;
                    }
                }

                // Complete leap when landing or timeout
                if ((this.attackTicks > 5 && this.onGround()) || this.attackTicks >= 18) {
                    setAttackState(ATTACK_NONE);
                    this.attackTicks = 0;
                }
            }
        }
    }

    private static final int[] TAIL_TICKS = {5, 11, 18, 26, 35, 45, 56, 68};
    private static final double[] TAIL_DISTS = {0.88D, 1.63D, 2.56D, 3.50D, 4.44D, 5.19D, 5.94D, 6.88D};
    private static final int[] SIM_PART_INDICES = {4, 8, 13, 18, 23, 27, 31, 36};

    private void updateTailParts() {
        if (this.subEntities == null) return;
        for (int i = 0; i < this.subEntities.length; i++) {
            setPartPosition(this.subEntities[i], TAIL_TICKS[i], TAIL_DISTS[i]);
        }
    }

    /**
     * Client-side synchronization: aligns sub-hitboxes to the exact physics-simulated tail mesh coordinates.
     */
    public void syncPartsFromSimulation(double[] px, double[] py, double[] pz) {
        if (this.subEntities == null || px == null || py == null || pz == null) return;
        for (int i = 0; i < this.subEntities.length && i < SIM_PART_INDICES.length; i++) {
            int pt = SIM_PART_INDICES[i];
            if (pt < px.length && pt < py.length && pt < pz.length) {
                double x = px[pt];
                double y = py[pt];
                double z = pz[pt];
                LamiaPart part = this.subEntities[i];
                part.setPos(x, y, z);
                part.xo = x;
                part.yo = y;
                part.zo = z;
                part.xOld = x;
                part.yOld = y;
                part.zOld = z;
            }
        }
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

        double vx = this.chokedVictim.getX();
        double vy = this.chokedVictim.getY();
        double vz = this.chokedVictim.getZ();

        // Lock victim in place
        this.chokedVictim.setDeltaMovement(Vec3.ZERO);
        this.chokedVictim.fallDistance = 0.0F;
        this.chokedVictim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 5, false, false));
        this.chokedVictim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1, false, false));

        // Lock Lamia closely in front of the victim (0.35m chest-to-chest)
        Vec3 diff = this.chokedVictim.position().subtract(this.position());
        float targetYaw = (float) (Mth.atan2(diff.z, diff.x) * (180.0D / Math.PI)) - 90.0F;
        if (Float.isNaN(targetYaw)) targetYaw = this.yBodyRot;

        float rad = targetYaw * Mth.DEG_TO_RAD;
        double lamiaX = vx + Mth.sin(rad) * 0.35D;
        double lamiaZ = vz - Mth.cos(rad) * 0.35D;

        this.setPos(lamiaX, vy, lamiaZ);
        this.setYRot(targetYaw);
        this.yBodyRot = targetYaw;
        this.yHeadRot = targetYaw;
        this.setDeltaMovement(Vec3.ZERO);
        this.getNavigation().stop();

        // Place tail sub-hitboxes in an upward spiral surrounding the victim
        if (this.subEntities != null) {
            for (int k = 0; k < this.subEntities.length; k++) {
                double h = vy + 0.15D + k * (1.30D / 7.0D); // Ascends from 0.15m up to 1.45m
                double angle = (targetYaw * Mth.DEG_TO_RAD) + (k * 2.35D); // Spiral angle around victim
                double r = 0.42D;
                double px = vx + Math.cos(angle) * r;
                double pz = vz + Math.sin(angle) * r;
                LamiaPart part = this.subEntities[k];
                part.setPos(px, h, pz);
                part.xo = px;
                part.yo = h;
                part.zo = pz;
            }
        }

        // Crushing damage every 15 ticks (0.75s)
        if (this.chokeholdTicks % 15 == 0) {
            this.chokedVictim.hurt(this.damageSources().mobAttack(this), 2.5F);
            if (!this.chokedVictim.isAlive() || this.chokedVictim.getHealth() <= 0.0F) {
                this.heal(8.0F);
                this.hungerCooldown = 8000 + this.random.nextInt(4000);
                this.setTarget(null);
                endChokehold();
                return;
            }
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
        this.leapCooldown = 80; // 4.0s cooldown
        this.leapImpacted = false;
        this.getNavigation().stop();

        Vec3 diff = target.position().subtract(this.position());
        double dist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        // Lock yaw directly facing the target
        this.leapLockedYaw = (float) (Mth.atan2(diff.z, diff.x) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(this.leapLockedYaw);
        this.yBodyRot = this.leapLockedYaw;
        this.yHeadRot = this.leapLockedYaw;

        // Calculate speed to land ~0.8m in front of target
        double targetDist = Math.max(1.0D, dist - 0.8D);
        double horizSpeed = Mth.clamp(targetDist / 6.5D, 0.45D, 1.05D);

        Vec3 horizontal = new Vec3(diff.x, 0, diff.z).normalize().scale(horizSpeed);
        this.setDeltaMovement(horizontal.x, 0.44D, horizontal.z);
        this.hasImpulse = true;
    }

    public void triggerPunchAttack(LivingEntity target) {
        setAttackState(ATTACK_PUNCH);
        this.attackTicks = 0;
        this.doHurtTarget(target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living && (!living.isAlive() || living.getHealth() <= 0.0F)) {
            // Prey killed and consumed
            this.heal(6.0F);
            this.hungerCooldown = 6000 + this.random.nextInt(4000);
            this.setTarget(null);
        }
        return hurt;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("LeapCooldown", this.leapCooldown);
        compound.putInt("HungerCooldown", this.hungerCooldown);
        compound.putInt("ScalesColor", this.getScalesColor());
        compound.putInt("HairColor", this.getHairColor());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.leapCooldown = compound.getInt("LeapCooldown");
        if (compound.contains("HungerCooldown")) {
            this.hungerCooldown = compound.getInt("HungerCooldown");
        }
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

            if (this.lamia.getAttackState() == ATTACK_CHOKEHOLD || this.lamia.getAttackState() == ATTACK_LEAP) {
                return; // Stationary during chokehold, or airborne during leap
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
