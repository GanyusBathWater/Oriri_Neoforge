package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.entity.ai.FleeSilentDepthsGoal;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * SilentClamEntity — a passive, stationary abyssal clam native to the Silent Depths
 * and Sunken Trenches.
 *
 * Behavior & Animations:
 * - State 0 (CLOSED): Shell closed with a Pearl inside (plays silent_clam_pearl_regrown to ensure pearl scale = 1).
 * - State 1 (OPENING): Triggered when a player with Seaweed (Miracle Seaweed, Kelp, or Seagrass) in offhand
 *   approaches within 6 blocks or right-clicks. Plays silent_clam_opening (2.5s / 50 ticks) and holds open.
 *   Once fully open, right-clicking with an empty main hand or Pearl while holding Seaweed in offhand claims
 *   the Pearl (100% chance).
 * - State 2 (STOLEN): Plays silent_clam_pearl_stolen (0.375s; pearl vanishes and shell slams shut with recoil)
 *   and starts a 20-minute (24,000-tick) regrowth timer. Feeding Miracle Seaweed regrows the Pearl instantly;
 *   feeding normal Seaweed (Kelp/Seagrass) reduces the timer by 1 minute (1,200 ticks, so 20 total).
 */
public class SilentClamEntity extends WaterAnimal implements GeoEntity {

    public static final int STATE_CLOSED = 0;
    public static final int STATE_OPENING = 1;
    public static final int STATE_STOLEN = 2;

    /** 2.5 seconds (50 ticks), matching silent_clam_opening animation length. */
    public static final int OPENING_DURATION_TICKS = 50;

    /** Exactly 20 minutes = 20 * 60 * 20 = 24,000 ticks. */
    public static final int REGROW_DURATION_TICKS = 24000;

    /** 20 normal seaweeds regrow a pearl from 20 minutes -> 1,200 ticks (1 minute) per seaweed. */
    public static final int NORMAL_SEAWEED_REDUCTION_TICKS = REGROW_DURATION_TICKS / 20;

    private static final double LURE_OPEN_RANGE = 6.0D;
    private static final double LURE_KEEP_OPEN_RANGE = 8.0D;

    private static final EntityDataAccessor<Integer> DATA_CLAM_STATE = SynchedEntityData.defineId(
            SilentClamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HAS_PEARL = SynchedEntityData.defineId(
            SilentClamEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FULLY_OPEN = SynchedEntityData.defineId(
            SilentClamEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation ANIM_PEARL_REGROWN = RawAnimation.begin().thenPlayAndHold("silent_clam_pearl_regrown");
    private static final RawAnimation ANIM_OPENING = RawAnimation.begin().thenPlayAndHold("silent_clam_opening");
    private static final RawAnimation ANIM_PEARL_STOLEN = RawAnimation.begin().thenPlayAndHold("silent_clam_pearl_stolen");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int openingTicks = 0;
    private int regrowTimer = 0;

    public SilentClamEntity(EntityType<? extends WaterAnimal> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public static boolean checkSilentClamSpawnRules(
            EntityType<SilentClamEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random) {
        if (spawnType == MobSpawnType.SPAWNER || spawnType == MobSpawnType.SPAWN_EGG) {
            return true;
        }
        if (!FleeSilentDepthsGoal.isAbyssalBiome(level.getBiome(pos))) {
            return false;
        }
        if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            return false;
        }
        // Must have solid seafloor / underwater cave floor within 18 blocks below through open water
        BlockPos.MutableBlockPos floorCursor = new BlockPos.MutableBlockPos();
        boolean foundSeafloor = false;
        for (int dy = 1; dy <= 18; dy++) {
            floorCursor.set(pos.getX(), pos.getY() - dy, pos.getZ());
            if (level.getBlockState(floorCursor).blocksMotion()) {
                foundSeafloor = true;
                break;
            }
            if (!level.getFluidState(floorCursor).is(FluidTags.WATER)) {
                return false;
            }
        }
        if (!foundSeafloor) {
            return false;
        }
        return level.getEntitiesOfClass(
                SilentClamEntity.class,
                new AABB(pos).inflate(32.0D)).size() < 4;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            // Snap directly down onto the solid seafloor/cave floor underneath
            BlockPos.MutableBlockPos cursor = this.blockPosition().mutable();
            for (int dy = 1; dy <= 18; dy++) {
                cursor.set(this.getBlockX(), this.getBlockY() - dy, this.getBlockZ());
                if (level.getBlockState(cursor).blocksMotion()) {
                    this.setPos(cursor.getX() + 0.5D, cursor.getY() + 1.0D, cursor.getZ() + 0.5D);
                    break;
                }
            }
        }
        return data;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CLAM_STATE, STATE_CLOSED);
        builder.define(DATA_HAS_PEARL, true);
        builder.define(DATA_FULLY_OPEN, false);
    }

    @Override
    protected void registerGoals() {
        // Stationary clam — no movement or attack goals
    }

    public int getClamState() {
        return this.entityData.get(DATA_CLAM_STATE);
    }

    public void setClamState(int state) {
        this.entityData.set(DATA_CLAM_STATE, state);
    }

    public boolean hasPearl() {
        return this.entityData.get(DATA_HAS_PEARL);
    }

    public void setHasPearl(boolean hasPearl) {
        this.entityData.set(DATA_HAS_PEARL, hasPearl);
    }

    public boolean isFullyOpen() {
        return this.entityData.get(DATA_FULLY_OPEN);
    }

    public void setFullyOpen(boolean fullyOpen) {
        this.entityData.set(DATA_FULLY_OPEN, fullyOpen);
    }

    public static boolean isMiracleSeaweed(ItemStack stack) {
        return stack.is(ModItems.MIRACLE_SEAWEED.get());
    }

    public static boolean isNormalSeaweed(ItemStack stack) {
        return stack.is(Items.KELP) || stack.is(Items.SEAGRASS);
    }

    public static boolean isAnySeaweed(ItemStack stack) {
        return isMiracleSeaweed(stack) || isNormalSeaweed(stack);
    }

    private boolean hasNearbyPlayerWithOffhandSeaweed(double range) {
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(range))) {
            if (player.isAlive() && !player.isSpectator() && isAnySeaweed(player.getOffhandItem())) {
                return true;
            }
        }
        return false;
    }

    private void regrowPearlNow() {
        this.regrowTimer = 0;
        this.openingTicks = 0;
        this.setHasPearl(true);
        this.setFullyOpen(false);
        this.setClamState(STATE_CLOSED);
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.2F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.GLOW,
                    this.getX(), this.getY() + 0.35D, this.getZ(),
                    8, 0.2D, 0.15D, 0.2D, 0.02D);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            if (!this.hasPearl()) {
                if (this.getClamState() != STATE_STOLEN) {
                    this.setClamState(STATE_STOLEN);
                    this.setFullyOpen(false);
                }
                if (this.regrowTimer > 0) {
                    this.regrowTimer--;
                }
                if (this.regrowTimer <= 0) {
                    this.regrowPearlNow();
                }
            } else {
                int state = this.getClamState();
                if (state == STATE_CLOSED) {
                    if (this.tickCount % 5 == 0 && hasNearbyPlayerWithOffhandSeaweed(LURE_OPEN_RANGE)) {
                        this.openingTicks = 0;
                        this.setFullyOpen(false);
                        this.setClamState(STATE_OPENING);
                        this.playSound(SoundEvents.SHULKER_OPEN, 0.5F, 1.3F);
                    }
                } else if (state == STATE_OPENING) {
                    if (this.openingTicks < OPENING_DURATION_TICKS) {
                        this.openingTicks++;
                        if (this.openingTicks >= OPENING_DURATION_TICKS) {
                            this.setFullyOpen(true);
                        }
                    }
                    if (this.tickCount % 10 == 0 && !hasNearbyPlayerWithOffhandSeaweed(LURE_KEEP_OPEN_RANGE)) {
                        this.openingTicks = 0;
                        this.setFullyOpen(false);
                        this.setClamState(STATE_CLOSED);
                        this.playSound(SoundEvents.SHULKER_CLOSE, 0.4F, 1.2F);
                    }
                } else {
                    // Fallback consistency if hasPearl is true while in STATE_STOLEN
                    this.openingTicks = 0;
                    this.setFullyOpen(false);
                    this.setClamState(STATE_CLOSED);
                }
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack usedStack = player.getItemInHand(hand);

        // 1. If the clam currently has NO pearl, feeding Miracle Seaweed or normal Seaweed accelerates regrowth
        if (!this.hasPearl()) {
            if (isMiracleSeaweed(usedStack)) {
                if (!this.level().isClientSide) {
                    if (!player.getAbilities().instabuild) {
                        usedStack.shrink(1);
                    }
                    this.playSound(SoundEvents.GENERIC_EAT, 0.8F, 1.0F);
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                this.getX(), this.getY() + 0.35D, this.getZ(),
                                8, 0.25D, 0.2D, 0.25D, 0.05D);
                    }
                    this.regrowPearlNow();
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (isNormalSeaweed(usedStack)) {
                if (!this.level().isClientSide) {
                    if (!player.getAbilities().instabuild) {
                        usedStack.shrink(1);
                    }
                    this.regrowTimer = Math.max(0, this.regrowTimer - NORMAL_SEAWEED_REDUCTION_TICKS);
                    this.playSound(SoundEvents.GENERIC_EAT, 0.7F, 1.1F);
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                this.getX(), this.getY() + 0.35D, this.getZ(),
                                5, 0.25D, 0.15D, 0.25D, 0.02D);
                    }
                    if (this.regrowTimer <= 0) {
                        this.regrowPearlNow();
                    }
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            return InteractionResult.PASS;
        }

        // 2. Clam HAS a pearl: handle opening/claiming on MAIN_HAND only
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack offhand = player.getOffhandItem();
        ItemStack mainhand = player.getMainHandItem();
        boolean validMainHand = mainhand.isEmpty() || mainhand.is(ModItems.PEARL.get());

        if (isAnySeaweed(offhand) && validMainHand) {
            int state = this.getClamState();
            if (state == STATE_CLOSED) {
                if (!this.level().isClientSide) {
                    this.openingTicks = 0;
                    this.setFullyOpen(false);
                    this.setClamState(STATE_OPENING);
                    this.playSound(SoundEvents.SHULKER_OPEN, 0.5F, 1.3F);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (state == STATE_OPENING) {
                if (!this.isFullyOpen()) {
                    return InteractionResult.CONSUME;
                }

                // Fully open: claim the pearl (100% chance) and trigger silent_clam_pearl_stolen
                if (!this.level().isClientSide) {
                    ItemStack pearl = new ItemStack(ModItems.PEARL.get());
                    if (mainhand.isEmpty()) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, pearl);
                    } else if (mainhand.is(ModItems.PEARL.get()) && mainhand.getCount() < mainhand.getMaxStackSize()) {
                        mainhand.grow(1);
                    } else if (!player.getInventory().add(pearl)) {
                        player.drop(pearl, false);
                    }

                    this.setHasPearl(false);
                    this.setFullyOpen(false);
                    this.openingTicks = 0;
                    this.regrowTimer = REGROW_DURATION_TICKS;
                    this.setClamState(STATE_STOLEN);

                    this.playSound(SoundEvents.SHULKER_CLOSE, 0.9F, 0.85F);
                    this.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.2F);
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.BUBBLE,
                                this.getX(), this.getY() + 0.3D, this.getZ(),
                                10, 0.25D, 0.15D, 0.25D, 0.05D);
                    }
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        if (type == NeoForgeMod.WATER_TYPE.value()) {
            return false;
        }
        return super.canDrownInFluidType(type);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi()) {
            // Sink straight down onto the seafloor with zero horizontal drift and never float up
            double dy = Math.max(this.getDeltaMovement().y - 0.04D, -0.25D);
            this.setDeltaMovement(0.0D, dy, 0.0D);
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (this.onGround()) {
                this.setDeltaMovement(Vec3.ZERO);
            }
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // Stationary clam does not push or get pushed
    }

    @Override
    public void push(Entity entity) {
        // Stationary clam cannot be pushed
    }

    @Override
    public void push(double x, double y, double z) {
        // Stationary clam cannot be pushed
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ClamState", this.getClamState());
        tag.putBoolean("HasPearl", this.hasPearl());
        tag.putBoolean("FullyOpen", this.isFullyOpen());
        tag.putInt("OpeningTicks", this.openingTicks);
        tag.putInt("RegrowTimer", this.regrowTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ClamState")) {
            this.setClamState(tag.getInt("ClamState"));
        }
        if (tag.contains("HasPearl")) {
            this.setHasPearl(tag.getBoolean("HasPearl"));
        }
        if (tag.contains("FullyOpen")) {
            this.setFullyOpen(tag.getBoolean("FullyOpen"));
        }
        if (tag.contains("OpeningTicks")) {
            this.openingTicks = tag.getInt("OpeningTicks");
        }
        if (tag.contains("RegrowTimer")) {
            this.regrowTimer = tag.getInt("RegrowTimer");
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "clam_controller", 2, this::predicate));
    }

    private PlayState predicate(AnimationState<SilentClamEntity> state) {
        int clamState = this.getClamState();
        if (clamState == STATE_STOLEN) {
            return state.setAndContinue(ANIM_PEARL_STOLEN);
        } else if (clamState == STATE_OPENING) {
            return state.setAndContinue(ANIM_OPENING);
        }
        return state.setAndContinue(ANIM_PEARL_REGROWN);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
