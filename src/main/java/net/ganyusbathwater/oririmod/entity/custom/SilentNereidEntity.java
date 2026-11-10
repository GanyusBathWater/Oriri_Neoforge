package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.combat.IElementalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * SilentNereidEntity — an abyssal mermaid variant endemic to the Silent Depths.
 *
 * Features:
 * - 4 predefined paired color variants (Hair + Scales):
 *     0: Biolumineszentes Blau (#00E5FF) & Tiefsee-Indigo (#1A237E)
 *     1: Glasten Violett (#BA55D3) & Abgrund-Schwarz (#18181B)
 *     2: Phosphor-Grün (#39FF14) & Kaltes Steingrau (#5A6B7C)
 *     3: Tiefsee-Rot (#DC143C) & Dunkelgrau (#2B2D31)
 * - Lure stalk and glowing accents via emissive render layer.
 * - Territorial aggression: Neutral unless a player steps within 8 blocks (<= 64.0 dist sqr).
 * - Severe land penalties: Slowness II, Weakness I, Mining Fatigue I, and dehydration damage after prolonged exposure (> 15s).
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

    private int landExposureTicks = 0;

    public SilentNereidEntity(EntityType<? extends SilentNereidEntity> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 35.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ATTACK_DAMAGE, 3.5D)
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
        // Spawn chance 1 to 130
        if (random.nextInt(130) != 0) {
            return false;
        }
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        Variant randomVariant = Variant.byId(this.random.nextInt(Variant.values().length));
        setVariant(randomVariant);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.getId());
        this.entityData.set(DATA_HAIR_COLOR, variant.getHairColor());
        this.entityData.set(DATA_FIN_COLOR, variant.getScaleColor());
    }

    @Override
    public int getHairColor() {
        return getVariant().getHairColor();
    }

    @Override
    public int getFinColor() {
        return getVariant().getScaleColor();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.entityData.get(DATA_VARIANT));
        tag.putInt("LandExposureTicks", this.landExposureTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) {
            setVariant(Variant.byId(tag.getInt("Variant")));
        }
        if (tag.contains("LandExposureTicks")) {
            this.landExposureTicks = tag.getInt("LandExposureTicks");
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, true) {
            private int customAttackTime = 0;

            @Override
            public void tick() {
                super.tick();
                if (this.customAttackTime > 0) {
                    this.customAttackTime--;
                }

                LivingEntity target = this.mob.getTarget();
                if (target != null && this.customAttackTime <= 0) {
                    double dist = this.mob.distanceToSqr(target);
                    if (dist < 6.25D && this.mob.getSensing().hasLineOfSight(target)) {
                        this.customAttackTime = 20;
                        this.mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        this.mob.doHurtTarget(target);
                    }
                }
            }
        });

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

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

        // Hunt fish when hungry (same as mermaid)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractFish.class, false) {
            @Override
            public boolean canUse() {
                return SilentNereidEntity.this.getHealth() > 0 && super.canUse();
            }
        });

        // Territorial Aggression: Neutral unless a player steps within 8 blocks (64.0D sqr)
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                living -> living instanceof Player player
                        && !player.isCreative()
                        && !player.isSpectator()
                        && player.distanceToSqr(SilentNereidEntity.this) <= 64.0D) {

            {
                this.setUnseenMemoryTicks(100); // 5 seconds persistence before dropping target
            }

            @Override
            public boolean canContinueToUse() {
                LivingEntity current = this.mob.getTarget();
                if (current instanceof Player player) {
                    if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                        return false;
                    }
                    // Disengage if player flees past 14 blocks (196.0D)
                    return this.mob.distanceToSqr(player) <= 196.0D && super.canContinueToUse();
                }
                return super.canContinueToUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // Enhanced land debuffs: deep-sea abyssal creatures struggle intensely out of water
            if (!this.isInWaterState()) {
                this.landExposureTicks++;

                if (this.tickCount % 20 == 0) {
                    // Slowness II, Weakness I, Mining Fatigue I
                    this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, false));
                    this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, false));
                    this.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, false, false, false));

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
}
