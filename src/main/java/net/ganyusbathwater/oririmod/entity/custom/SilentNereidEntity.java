package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * SilentNereidEntity — an aggressive abyssal mermaid variant endemic to the
 * Silent Depths.
 *
 * Features:
 * - 4 paired color themes with randomized per-entity shade jitter:
 * 0: Biolumineszentes Blau (#00E5FF) & Tiefsee-Indigo (#1A237E)
 * 1: Glasten Violett (#BA55D3) & Abgrund-Schwarz (#18181B)
 * 2: Phosphor-Grün (#39FF14) & Kaltes Steingrau (#5A6B7C)
 * 3: Tiefsee-Rot (#DC143C) & Dunkelgrau (#2B2D31)
 * - Mixed color: Average of hair and scale color, applied to both the skin and
 * scale2 layer.
 * - Lure stalk and glowing accents via fullbright emissive render layer.
 * - Universal Aggression: Attacks every other living mob except themselves.
 * Attacks players when within 8 blocks (64.0 dist sqr).
 * - On-Kill Regeneration: Regenerates 50% max HP (20.0 HP) whenever killing any
 * entity.
 * - Biome-bound Territory: Stays in the Silent Depths; swims back down when
 * idle outside the biome.
 * - Full 3D aquatic and terrestrial melee attack goals.
 * - Enhanced land debuffs: Slowness II, Mining Fatigue II, and dehydration
 * damage after prolonged exposure (> 15s).
 * - 1-in-130 spawn check in the Silent Depths.
 */
public class SilentNereidEntity extends MermaidEntity {

    public enum Variant {
        BIOLUMINESCENT_BLUE_INDIGO(0, "bioluminescent_blue_indigo", 0x00E5FF, 0x1A237E),
        GLISTERING_VIOLET_ABYSSAL_BLACK(1, "glistering_violet_abyssal_black", 0xBA55D3, 0x18181B),
        PHOSPHOR_GREEN_COLD_STONE(2, "phosphor_green_cold_stone", 0x39FF14, 0x5A6B7C),
        DEEP_SEA_RED_DARK_GREY(3, "deep_sea_red_dark_grey", 0xDC143C, 0x2B2D31);

        private final int id;
        private final String name;
        private final int hairColor;
        private final int scaleColor;

        Variant(int id, String name, int hairColor, int scaleColor) {
            this.id = id;
            this.name = name;
            this.hairColor = hairColor;
            this.scaleColor = scaleColor;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getHairColor() {
            return hairColor;
        }

        public int getScaleColor() {
            return scaleColor;
        }

        public static Variant byId(int id) {
            Variant[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    public static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(
            SilentNereidEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Integer> DATA_MIXED_COLOR = SynchedEntityData.defineId(
            SilentNereidEntity.class, EntityDataSerializers.INT);

    private int landExposureTicks = 0;

    @Nullable
    private BlockPos lastKnownAbyssPos = null;

    public SilentNereidEntity(EntityType<? extends SilentNereidEntity> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D) // +5 more health (was 35.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ATTACK_DAMAGE, 4.5D) // +1 more attack damage (was 3.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D);
    }

    public static boolean checkSilentNereidSpawnRules(EntityType<? extends Monster> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random) {
        if (spawnType == MobSpawnType.SPAWNER || spawnType == MobSpawnType.SPAWN_EGG) {
            return true;
        }
        if (!net.ganyusbathwater.oririmod.entity.ai.FleeSilentDepthsGoal.isAbyssalBiome(level.getBiome(pos))) {
            return false;
        }
        if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            return false;
        }
        // Global spawn rate throttling near the Silent Abyss is handled uniformly in AbyssalSpawnEvents;
        // keep a generous local cap here as a safety floor for overlapping patrol pockets.
        return level.getEntitiesOfClass(
                SilentNereidEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(56.0)).size() < 4;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        Variant[] values = Variant.values();
        Variant initial = values[this.random.nextInt(values.length)];
        int hair = jitterColor(initial.getHairColor(), this.random);
        int scale = jitterColor(initial.getScaleColor(), this.random);
        int mixed = calculateMixedColor(hair, scale);

        builder.define(DATA_VARIANT, initial.getId());
        builder.define(DATA_MIXED_COLOR, mixed);
    }

    public static int jitterColor(int baseRgb, RandomSource random) {
        int r = (baseRgb >> 16) & 0xFF;
        int g = (baseRgb >> 8) & 0xFF;
        int b = baseRgb & 0xFF;

        float factor = 0.80F + random.nextFloat() * 0.40F; // 80% - 120% brightness
        int jr = Math.clamp((int) (r * factor) + (random.nextInt(31) - 15), 0, 255);
        int jg = Math.clamp((int) (g * factor) + (random.nextInt(31) - 15), 0, 255);
        int jb = Math.clamp((int) (b * factor) + (random.nextInt(31) - 15), 0, 255);

        return (jr << 16) | (jg << 8) | jb;
    }

    public static int calculateMixedColor(int hair, int scale) {
        int r = (((hair >> 16) & 0xFF) + ((scale >> 16) & 0xFF)) / 2;
        int g = (((hair >> 8) & 0xFF) + ((scale >> 8) & 0xFF)) / 2;
        int b = ((hair & 0xFF) + (scale & 0xFF)) / 2;
        return (r << 16) | (g << 8) | b;
    }

    public void randomizeColors() {
        Variant[] values = Variant.values();
        Variant variant = values[this.random.nextInt(values.length)];
        int hair = jitterColor(variant.getHairColor(), this.random);
        int scale = jitterColor(variant.getScaleColor(), this.random);
        int mixed = calculateMixedColor(hair, scale);

        this.entityData.set(DATA_VARIANT, variant.getId());
        this.entityData.set(DATA_HAIR_COLOR, hair);
        this.entityData.set(DATA_FIN_COLOR, scale);
        this.entityData.set(DATA_MIXED_COLOR, mixed);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        randomizeColors();
        return data;
    }

    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(Variant variant) {
        int hair = variant.getHairColor();
        int scale = variant.getScaleColor();
        int mixed = calculateMixedColor(hair, scale);

        this.entityData.set(DATA_VARIANT, variant.getId());
        this.entityData.set(DATA_HAIR_COLOR, hair);
        this.entityData.set(DATA_FIN_COLOR, scale);
        this.entityData.set(DATA_MIXED_COLOR, mixed);
    }

    public int getMixedColor() {
        return this.entityData.get(DATA_MIXED_COLOR);
    }

    @Override
    public int getHairColor() {
        return this.entityData.get(DATA_HAIR_COLOR);
    }

    @Override
    public int getFinColor() {
        return this.entityData.get(DATA_FIN_COLOR);
    }

    @Nullable
    public BlockPos getLastKnownAbyssPos() {
        return this.lastKnownAbyssPos;
    }

    public void setLastKnownAbyssPos(@Nullable BlockPos pos) {
        this.lastKnownAbyssPos = pos;
    }

    public static final ResourceKey<Biome> SILENT_DEPTHS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_silent_depths"));
    public static final ResourceKey<Biome> SUNKEN_TRENCHES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_sunken_trenches"));

    public boolean isInsideSilentDepths() {
        Holder<Biome> biome = this.level().getBiome(this.blockPosition());
        return biome.is(SILENT_DEPTHS_KEY) || biome.is(SUNKEN_TRENCHES_KEY);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.entityData.get(DATA_VARIANT));
        tag.putInt("HairColor", this.entityData.get(DATA_HAIR_COLOR));
        tag.putInt("ScaleColor", this.entityData.get(DATA_FIN_COLOR));
        tag.putInt("MixedColor", this.entityData.get(DATA_MIXED_COLOR));
        tag.putInt("LandExposureTicks", this.landExposureTicks);
        if (this.lastKnownAbyssPos != null) {
            tag.putLong("LastKnownAbyssPos", this.lastKnownAbyssPos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) {
            this.entityData.set(DATA_VARIANT, tag.getInt("Variant"));
        }
        if (tag.contains("HairColor")) {
            this.entityData.set(DATA_HAIR_COLOR, tag.getInt("HairColor"));
        }
        if (tag.contains("ScaleColor")) {
            this.entityData.set(DATA_FIN_COLOR, tag.getInt("ScaleColor"));
        }
        if (tag.contains("MixedColor")) {
            this.entityData.set(DATA_MIXED_COLOR, tag.getInt("MixedColor"));
        } else {
            this.entityData.set(DATA_MIXED_COLOR, calculateMixedColor(getHairColor(), getFinColor()));
        }
        if (tag.contains("LandExposureTicks")) {
            this.landExposureTicks = tag.getInt("LandExposureTicks");
        }
        if (tag.contains("LastKnownAbyssPos")) {
            this.lastKnownAbyssPos = BlockPos.of(tag.getLong("LastKnownAbyssPos"));
        }
    }

    @Override
    protected void registerGoals() {
        // Melee attack goal: handles both full 3D swimming combat and land combat
        this.goalSelector.addGoal(2, new SilentNereidAttackGoal(this));

        // When not hunting and outside the abyss, swim back down to the Silent Depths
        this.goalSelector.addGoal(3, new SilentNereidReturnToDepthsGoal(this));

        this.goalSelector.addGoal(4, new MermaidWanderGoal(this));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.9D) {
            @Override
            public boolean canUse() {
                return !SilentNereidEntity.this.isInWater() && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !SilentNereidEntity.this.isInWater() && super.canUse();
            }
        });

        // Retaliate when hurt, ignoring friendly fire from other Silent Nereids
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, SilentNereidEntity.class));

        // Attack all other living mobs! For players: triggers within 8 blocks (64.0D
        // sqr)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                this::isValidAttackTarget));
    }

    public boolean isValidAttackTarget(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        // Never attack other Silent Nereids
        if (target instanceof SilentNereidEntity) {
            return false;
        }
        // For players: only attack if in survival/adventure AND within 8 blocks (or
        // already current target)
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
            return this.distanceToSqr(player) <= 64.0D || this.getTarget() == player;
        }
        // Attack EVERY other living mob!
        return true;
    }

    @Override
    public void awardKillScore(Entity killed, int scoreValue, DamageSource damageSource) {
        super.awardKillScore(killed, scoreValue, damageSource);
        // Regenerate 50% of maximum health on any kill
        this.heal(this.getMaxHealth() * 0.5F);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // Keep track of current abyss position when inside native territory
            if (this.tickCount % 20 == 0 && isInsideSilentDepths()) {
                this.lastKnownAbyssPos = this.blockPosition();
            }

            // Enhanced land debuffs: deep-sea abyssal creatures struggle intensely out of
            // water
            if (!this.isInWaterState()) {
                this.landExposureTicks++;

                if (this.tickCount % 20 == 0) {
                    // Slowness II and Mining Fatigue II (exhausted on land without nullifying
                    // damage)
                    this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, false));
                    this.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 1, false, false, false));

                    // After 15 seconds (300 ticks) out of water, suffer dehydration damage
                    if (this.landExposureTicks > 300) {
                        this.hurt(this.damageSources().dryOut(), 1.0F);
                    }
                }
            } else {
                this.landExposureTicks = 0;
            }
        }
    }

    // ── Dedicated Goal: Return to the Silent Depths Territory ─────────────────
    public static class SilentNereidReturnToDepthsGoal extends Goal {
        private final SilentNereidEntity nereid;
        private BlockPos returnPos;

        public SilentNereidReturnToDepthsGoal(SilentNereidEntity nereid) {
            this.nereid = nereid;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // Only active when not engaged in combat
            LivingEntity target = this.nereid.getTarget();
            if (target != null && target.isAlive()) {
                return false;
            }
            // If already inside native biome, no need to return
            if (this.nereid.isInsideSilentDepths()) {
                return false;
            }

            // Locate return coordinate
            this.returnPos = determineReturnPosition();
            return this.returnPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.nereid.getTarget();
            if (target != null && target.isAlive()) {
                return false;
            }
            if (this.nereid.isInsideSilentDepths()) {
                return false;
            }
            return this.returnPos != null && this.nereid.distanceToSqr(Vec3.atCenterOf(this.returnPos)) > 9.0D;
        }

        @Override
        public void start() {
            if (this.returnPos != null) {
                if (this.nereid.isInWater()) {
                    this.nereid.getMoveControl().setWantedPosition(
                            this.returnPos.getX() + 0.5D,
                            this.returnPos.getY() + 0.5D,
                            this.returnPos.getZ() + 0.5D,
                            1.3D);
                } else {
                    this.nereid.getNavigation().moveTo(
                            this.returnPos.getX() + 0.5D,
                            this.returnPos.getY() + 0.5D,
                            this.returnPos.getZ() + 0.5D,
                            1.2D);
                }
            }
        }

        @Override
        public void tick() {
            if (this.returnPos == null)
                return;

            this.nereid.getLookControl().setLookAt(
                    this.returnPos.getX() + 0.5D,
                    this.returnPos.getY() + 0.5D,
                    this.returnPos.getZ() + 0.5D,
                    30.0F, 30.0F);

            if (this.nereid.isInWater()) {
                this.nereid.getMoveControl().setWantedPosition(
                        this.returnPos.getX() + 0.5D,
                        this.returnPos.getY() + 0.5D,
                        this.returnPos.getZ() + 0.5D,
                        1.35D);
            } else {
                if (this.nereid.getNavigation().isDone()) {
                    this.nereid.getNavigation().moveTo(
                            this.returnPos.getX() + 0.5D,
                            this.returnPos.getY() + 0.5D,
                            this.returnPos.getZ() + 0.5D,
                            1.2D);
                }
            }
        }

        @Nullable
        private BlockPos determineReturnPosition() {
            // 1. Prefer known home position if recorded
            BlockPos home = this.nereid.getLastKnownAbyssPos();
            if (home != null) {
                return home;
            }

            // 2. Scan nearby for the_silent_depths or the_sunken_trenches
            Level level = this.nereid.level();
            BlockPos current = this.nereid.blockPosition();

            // Check downward column first (Silent Depths and Sunken Trenches are beneath photic oceans)
            for (int dy = -5; dy >= -40; dy -= 5) {
                BlockPos check = current.offset(0, dy, 0);
                Holder<Biome> b = level.getBiome(check);
                if ((b.is(SILENT_DEPTHS_KEY) || b.is(SUNKEN_TRENCHES_KEY)) && level.getFluidState(check).is(FluidTags.WATER)) {
                    return check;
                }
            }

            // Search surrounding area in water
            for (int dx = -32; dx <= 32; dx += 16) {
                for (int dz = -32; dz <= 32; dz += 16) {
                    for (int dy = -20; dy <= 0; dy += 10) {
                        BlockPos check = current.offset(dx, dy, dz);
                        Holder<Biome> b = level.getBiome(check);
                        if ((b.is(SILENT_DEPTHS_KEY) || b.is(SUNKEN_TRENCHES_KEY)) && level.getFluidState(check).is(FluidTags.WATER)) {
                            return check;
                        }
                    }
                }
            }

            // If in ocean water, swim down towards abyss depth (Y = 20)
            if (this.nereid.isInWater() && current.getY() > 25) {
                return new BlockPos(current.getX(), 20, current.getZ());
            }

            return null;
        }
    }

    // ── Dedicated Melee Combat Goal (3D Swimming & Land Attack) ───────────────
    public static class SilentNereidAttackGoal extends Goal {
        private final SilentNereidEntity nereid;
        private int attackCooldown = 0;

        public SilentNereidAttackGoal(SilentNereidEntity nereid) {
            this.nereid = nereid;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.nereid.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.nereid.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            this.attackCooldown = 0;
            super.start();
        }

        @Override
        public void tick() {
            LivingEntity target = this.nereid.getTarget();
            if (target == null) return;

            this.nereid.getLookControl().setLookAt(target, 30.0F, 30.0F);

            double distSqr = this.nereid.distanceToSqr(target);

            // ── Movement: Direct 3D swimming in water vs Ground navigation on land ──
            if (this.nereid.isInWater()) {
                this.nereid.getMoveControl().setWantedPosition(
                        target.getX(),
                        target.getY() + target.getBbHeight() * 0.5D,
                        target.getZ(),
                        1.4D
                );
            } else {
                this.nereid.getNavigation().moveTo(target, 1.25D);
            }

            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }

            // Bounding box reach calculation
            double reach = (this.nereid.getBbWidth() * 2.0F) + target.getBbWidth() + 0.8D;
            double reachSqr = reach * reach;

            if (distSqr <= reachSqr && this.nereid.getSensing().hasLineOfSight(target) && this.attackCooldown <= 0) {
                this.attackCooldown = 20; // 1 second cooldown
                this.nereid.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                this.nereid.doHurtTarget(target);
            }
        }
    }
}
