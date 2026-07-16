package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.combat.IElementalEntity;
import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * MermaidEntity — a hostile aquatic monster that switches between two body forms
 * (water: tail fin model / land: legs model) and has a chance to charm nearby players.
 *
 * Features:
 * - Dual GeckoLib model switching via DATA_IN_WATER synced boolean
 * - Per-entity randomized hair/fin color (DATA_HAIR_COLOR packed RGB, persisted in NBT)
 * - Charmed effect: 2.5% chance every 20 ticks when targeting a Player (both land and water)
 * - Slowness I applied on land (re-applied every 20t while not in water)
 * - WATER element (IElementalEntity)
 * - Arms controller stops cleanly when attack animation is playing
 * - Land anchor bones (left_arm_fin, right_arm_fin, left_leg_fin, right_leg_fin)
 *   used by the renderer to spawn dripping water particles
 */
public class MermaidEntity extends Monster implements GeoEntity, IElementalEntity {

    // ── Attack type constants ──────────────────────────────────────────────
    public static final int ATTACK_NONE  = 0;
    public static final int ATTACK_MELEE = 1;

    // ── Saturated oceanic color palette for randomized hair/fins ─────────
    private static final int[] OCEANIC_PALETTE = {
        0x00CED1, // dark turquoise
        0x20B2AA, // light sea green
        0x48D1CC, // medium turquoise
        0x40E0D0, // turquoise
        0x00FA9A, // medium spring green
        0xFF6B6B, // coral red
        0xFF7F50, // coral
        0xF4A460, // sandy coral
        0x7B68EE, // medium slate blue
        0x9370DB, // medium purple
        0x8A2BE2, // blue violet
    };

    // ── Synced data ────────────────────────────────────────────────────────
    /** True while the mermaid is in water — drives renderer model switch. */
    public static final EntityDataAccessor<Boolean> DATA_IN_WATER =
            SynchedEntityData.defineId(MermaidEntity.class, EntityDataSerializers.BOOLEAN);

    /** Packed RGB color applied to hair template texture by the renderer. */
    public static final EntityDataAccessor<Integer> DATA_HAIR_COLOR =
            SynchedEntityData.defineId(MermaidEntity.class, EntityDataSerializers.INT);

    /** Packed RGB color applied to fin template texture by the renderer. */
    public static final EntityDataAccessor<Integer> DATA_FIN_COLOR =
            SynchedEntityData.defineId(MermaidEntity.class, EntityDataSerializers.INT);

    /** Current attack type (0 = none, 1 = melee) synced to client for animation controller. */
    public static final EntityDataAccessor<Integer> DATA_ATTACK_TYPE =
            SynchedEntityData.defineId(MermaidEntity.class, EntityDataSerializers.INT);

    // ── Hunger / Eating Mechanics ─────────────────────────────────────────
    private int fishEatenCount = 0;
    private int hungerCooldown = 0;

    // ── Animation & State Tracking ────────────────────────────────────────
    private int outOfWaterTicks = 0;

    // ── GeckoLib ──────────────────────────────────────────────────────────
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // ── Constructor ───────────────────────────────────────────────────────
    public MermaidEntity(EntityType<? extends MermaidEntity> type, Level level) {
        super(type, level);
        this.xpReward = 12;
        // Use smooth swimming with 90-degree max turn Y to allow full vertical diving without horizontal circling
        this.moveControl = new MermaidMoveControl(this);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    // ── Attributes ────────────────────────────────────────────────────────
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,       30.0D)
                .add(Attributes.ARMOR,             4.0D)
                .add(Attributes.MOVEMENT_SPEED,    0.20D)
                .add(Attributes.ATTACK_DAMAGE,     2.5D)
                .add(Attributes.FOLLOW_RANGE,     32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
    }

    public static boolean checkMermaidSpawnRules(net.minecraft.world.entity.EntityType<MermaidEntity> type, net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnType, net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        return level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER);
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnReason) {
        return true;
    }

    @Override
    public boolean checkSpawnObstruction(net.minecraft.world.level.LevelReader level) {
        // Water animals must override this so they don't abort spawning when their bounding box contains liquid!
        return level.isUnobstructed(this);
    }

    // ── Synced data init ──────────────────────────────────────────────────
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IN_WATER, false);
        builder.define(DATA_HAIR_COLOR, 0x00CED1); // default: dark turquoise
        builder.define(DATA_FIN_COLOR, 0x00CED1);
        builder.define(DATA_ATTACK_TYPE, ATTACK_NONE);
    }

    // ── Spawn initialization ──────────────────────────────────────────────
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                   MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        // Randomize hair and fin colors independently from the oceanic palette
        int hairColorIndex = this.random.nextInt(OCEANIC_PALETTE.length);
        int finColorIndex = this.random.nextInt(OCEANIC_PALETTE.length);
        this.entityData.set(DATA_HAIR_COLOR, OCEANIC_PALETTE[hairColorIndex]);
        this.entityData.set(DATA_FIN_COLOR, OCEANIC_PALETTE[finColorIndex]);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    // ── AI goals ──────────────────────────────────────────────────────────
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        // Use custom 3D wander goal for water, and standard stroll for land
        this.goalSelector.addGoal(4, new MermaidWanderGoal(this));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                return !MermaidEntity.this.isInWater() && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !MermaidEntity.this.isInWater() && super.canUse();
            }
        });

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true,
                p -> p instanceof Player player && !player.isCreative() && !player.isSpectator()));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractFish.class, false) {
            @Override
            public boolean canUse() {
                return MermaidEntity.this.hungerCooldown <= 0 && super.canUse();
            }
            @Override
            public boolean canContinueToUse() {
                return MermaidEntity.this.hungerCooldown <= 0 && super.canContinueToUse();
            }
        });
    }

    // ── Smooth Aquatic Movement ──────────────────────────────────────────
    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            // Apply speed multiplier based on whether she is hunting (fast) or wandering (cruising)
            float speedMult = this.getTarget() != null ? 1.5F : 0.8F;
            this.moveRelative(this.getSpeed() * speedMult, travelVector);
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, net.minecraft.world.DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        
        // Drowned trident spawn logic: ~6.25% overall chance
        if (random.nextFloat() > 0.9F) {
            int chance = random.nextInt(16);
            if (chance < 10) {
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TRIDENT));
            }
        }
    }

    // ── Combat & State ──────────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // ── Sync in-water state with debounce to prevent model flickering
            if (this.isInWater()) {
                this.outOfWaterTicks = 0;
                if (!this.entityData.get(DATA_IN_WATER)) {
                    this.entityData.set(DATA_IN_WATER, true);
                }
            } else {
                this.outOfWaterTicks++;
                if (this.outOfWaterTicks > 10 && this.entityData.get(DATA_IN_WATER)) {
                    this.entityData.set(DATA_IN_WATER, false);
                }
            }
            
            boolean inWaterState = this.entityData.get(DATA_IN_WATER);

            if (!inWaterState && this.tickCount % 20 == 0) {
                // Land penalty: apply Slowness I while not in water
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, false));
            }

            // ── Hunger Cooldown ──────────────────────────────────────────
            if (this.hungerCooldown > 0) {
                this.hungerCooldown--;
                if (this.hungerCooldown == 0 && this.getTarget() instanceof AbstractFish) {
                    this.setTarget(null);
                }
            }

            // ── Prevent drowning ─────────────────────────────────────────
            this.setAirSupply(this.getMaxAirSupply());

            // ── Charmed effect: 2.5% chance every 20 ticks when targeting ─
            if (this.tickCount % 20 == 0 && this.getTarget() instanceof Player target
                    && !target.isCreative() && !target.isSpectator()) {
                // 2.5% = 1 in 40
                if (this.random.nextInt(40) == 0) {
                    target.addEffect(new MobEffectInstance(ModEffects.CHARMED_EFFECT, 100, 0, false, true));
                    // Store this mermaid's UUID as caster so OririClient can prevent the player from hurting her
                    target.getPersistentData().putUUID("CharmCaster", this.getUUID());
                }
            }
        }
    }

    // ── Melee hit override — trigger attack animation ─────────────────────
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    /**
     * Called by MeleeAttackGoal when the mob successfully performs a melee swing.
     * We override performAttack (via doHurtTarget) to also set the attack type synced
     * data so GeckoLib can trigger the attack animation on the client.
     */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (!this.level().isClientSide) {
            setAttackType(ATTACK_MELEE);
            triggerAnim("attack_controller", "mermaid_attack");
        }

        // ── Fish eating logic: discard fish to prevent items from dropping ──
        if (target instanceof AbstractFish fish) {
            float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (damage >= fish.getHealth()) {
                fish.discard();
                this.heal(2.0f); // Heal slightly when eating a fish
                
                // Hunger mechanics
                this.fishEatenCount++;
                if (this.fishEatenCount >= 2 + this.random.nextInt(3)) { // 2 to 4 fish
                    this.hungerCooldown = 6000; // 5 minutes (20 ticks * 60 seconds * 5)
                    this.fishEatenCount = 0;
                    this.setTarget(null);
                }
                return true;
            }
        }

        return super.doHurtTarget(target);
    }

    // ── IElementalEntity ─────────────────────────────────────────────────
    @Override
    public Element getElement() {
        return Element.WATER;
    }

    // ── Getters / setters ─────────────────────────────────────────────────
    public int getAttackType()  { return this.entityData.get(DATA_ATTACK_TYPE); }
    public void setAttackType(int type) { this.entityData.set(DATA_ATTACK_TYPE, type); }

    public boolean isInWaterState() { return this.entityData.get(DATA_IN_WATER); }
    public int getHairColor()       { return this.entityData.get(DATA_HAIR_COLOR); }
    public int getFinColor()        { return this.entityData.get(DATA_FIN_COLOR); }

    // ── NBT persistence ───────────────────────────────────────────────────
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HairColor", this.entityData.get(DATA_HAIR_COLOR));
        tag.putInt("FinColor", this.entityData.get(DATA_FIN_COLOR));
        tag.putInt("HungerCooldown", this.hungerCooldown);
        tag.putInt("FishEatenCount", this.fishEatenCount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HairColor")) {
            this.entityData.set(DATA_HAIR_COLOR, tag.getInt("HairColor"));
        }
        if (tag.contains("FinColor")) {
            this.entityData.set(DATA_FIN_COLOR, tag.getInt("FinColor"));
        }
        if (tag.contains("HungerCooldown")) {
            this.hungerCooldown = tag.getInt("HungerCooldown");
        }
        if (tag.contains("FishEatenCount")) {
            this.fishEatenCount = tag.getInt("FishEatenCount");
        }
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        // ─── Body controller — idle/swim (water) or legs (land) ─────────
        // Plays movement anims based on environment; independent of arms
        controllers.add(new AnimationController<>(this, "body_controller", 5, state -> {
            boolean inWater = isInWaterState();
            boolean isMoving = state.isMoving();

            if (inWater) {
                if (isMoving) {
                    state.getController().setAnimation(RawAnimation.begin().thenLoop("mermaid_swimming"));
                } else {
                    state.getController().setAnimation(RawAnimation.begin().thenLoop("mermaid_idle_fins"));
                }
                return PlayState.CONTINUE;
            } else {
                // On land — only play leg anim if moving
                if (isMoving) {
                    state.getController().setAnimation(RawAnimation.begin().thenLoop("mermaid_land_moving_legs"));
                    return PlayState.CONTINUE;
                }
                return PlayState.STOP;
            }
        }));

        // ─── Arms controller — idle arms (water) or land arms ────────────
        // STOPS cleanly when an attack is active so attack_controller has full control
        controllers.add(new AnimationController<>(this, "arms_controller", 5, state -> {
            // Cancel arm loop while attacking — lets attack_controller own the arms
            if (getAttackType() != ATTACK_NONE) return PlayState.STOP;

            boolean inWater = isInWaterState();
            boolean isMoving = state.isMoving();

            if (inWater) {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("mermaid_idle_arms"));
                return PlayState.CONTINUE;
            } else {
                if (isMoving) {
                    state.getController().setAnimation(RawAnimation.begin().thenLoop("mermaid_land_moving_arms"));
                    return PlayState.CONTINUE;
                }
                return PlayState.STOP;
            }
        }));

        // ─── Attack controller — triggerable, shared anim name for both models
        // After PLAY_ONCE completes GeckoLib stops the controller naturally;
        // we reset DATA_ATTACK_TYPE to NONE at the end of the animation via
        // the STOP state check in arms_controller (client driven).
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> {
            // Once the triggered animation finishes, reset attack type on client
            if (!state.getController().isPlayingTriggeredAnimation()) {
                if (getAttackType() != ATTACK_NONE) {
                    this.entityData.set(DATA_ATTACK_TYPE, ATTACK_NONE);
                }
            }
            return PlayState.STOP;
        })
                .triggerableAnim("mermaid_attack",
                        RawAnimation.begin().then("mermaid_attack", Animation.LoopType.PLAY_ONCE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    // ── Custom Move Control to Prevent Overshoot Circling ────────────────
    class MermaidMoveControl extends net.minecraft.world.entity.ai.control.MoveControl {
        public MermaidMoveControl(MermaidEntity mermaid) {
            super(mermaid);
        }

        @Override
        public void tick() {
            if (this.mob.isInWater()) {
                if (this.operation == net.minecraft.world.entity.ai.control.MoveControl.Operation.MOVE_TO) {
                    double d0 = this.wantedX - this.mob.getX();
                    double d1 = this.wantedY - this.mob.getY();
                    double d2 = this.wantedZ - this.mob.getZ();
                    
                    // If wandering (no target) and within 1.5 blocks of the node, consider it reached
                    if (this.mob.getTarget() == null && (d0 * d0 + d1 * d1 + d2 * d2 < 2.25D)) {
                        this.operation = net.minecraft.world.entity.ai.control.MoveControl.Operation.WAIT;
                        this.mob.setSpeed(0.0F);
                        return;
                    }
                    
                    // Steer yaw
                    float targetYaw = (float)(net.minecraft.util.Mth.atan2(d2, d0) * (180F / (float)Math.PI)) - 90.0F;
                    this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetYaw, 90.0F));
                    this.mob.yBodyRot = this.mob.getYRot();
                    this.mob.yHeadRot = this.mob.getYRot();
                    
                    // Steer pitch
                    double horizontalDist = Math.sqrt(d0 * d0 + d2 * d2);
                    float targetPitch = (float)(-(net.minecraft.util.Mth.atan2(d1, horizontalDist) * (180F / (float)Math.PI)));
                    this.mob.setXRot(this.rotlerp(this.mob.getXRot(), targetPitch, 90.0F));
                    
                    // Calculate 3D thrust components! Without this, she only swims horizontally and spins!
                    float speed = (float)(this.speedModifier * this.mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
                    this.mob.setSpeed(speed);
                    float pitchCos = net.minecraft.util.Mth.cos(this.mob.getXRot() * ((float)Math.PI / 180F));
                    float pitchSin = net.minecraft.util.Mth.sin(this.mob.getXRot() * ((float)Math.PI / 180F));
                    this.mob.setZza(pitchCos * speed);
                    this.mob.setYya(-pitchSin * speed);
                } else {
                    this.mob.setSpeed(0.0F);
                    this.mob.setXxa(0.0F);
                    this.mob.setYya(0.0F);
                    this.mob.setZza(0.0F);
                }
            } else {
                // We are on land! Rely on standard MoveControl which flawlessly handles jumping up 1 block steps!
                super.tick();
            }
        }
    }

    // ── Custom Wander Goal for Free 3D Swimming ──────────────────────────
    class MermaidWanderGoal extends Goal {
        private final MermaidEntity mermaid;

        public MermaidWanderGoal(MermaidEntity mermaid) {
            this.mermaid = mermaid;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.mermaid.isInWater()) return false;
            if (this.mermaid.getTarget() != null) return false;
            return this.mermaid.getRandom().nextInt(40) == 0;
        }

        @Override
        public void start() {
            net.minecraft.util.RandomSource random = this.mermaid.getRandom();
            double x = this.mermaid.getX() + (random.nextDouble() * 16.0D - 8.0D);
            double y = this.mermaid.getY() + (random.nextDouble() * 10.0D - 5.0D);
            double z = this.mermaid.getZ() + (random.nextDouble() * 16.0D - 8.0D);
            
            // Try to keep the target within water
            if (this.mermaid.level().getFluidState(net.minecraft.core.BlockPos.containing(x, y, z)).isSource()) {
                this.mermaid.getMoveControl().setWantedPosition(x, y, z, 1.0D);
            } else {
                // Fallback to horizontal movement if y is out of water
                this.mermaid.getMoveControl().setWantedPosition(x, this.mermaid.getY(), z, 1.0D);
            }
        }
        
        @Override
        public boolean canContinueToUse() {
            return this.mermaid.getMoveControl().hasWanted() 
                && this.mermaid.getTarget() == null 
                && this.mermaid.isInWater();
        }
    }
}
