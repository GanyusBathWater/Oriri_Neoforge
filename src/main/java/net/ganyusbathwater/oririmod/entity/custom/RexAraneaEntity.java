package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.combat.IElementalEntity;
import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.Animation;
import net.ganyusbathwater.oririmod.entity.custom.projectile.RexAraneaWebEntity;

public class RexAraneaEntity extends Monster implements GeoEntity, IElementalEntity {

    // States for rendering/logic
    public static final EntityDataAccessor<Integer> DATA_HAIR_COLOR = SynchedEntityData.defineId(RexAraneaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_CLOTHES_COLOR = SynchedEntityData.defineId(RexAraneaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DATA_IS_WALL_CLIMBING = SynchedEntityData.defineId(RexAraneaEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_IS_CEILING_CLINGING = SynchedEntityData.defineId(RexAraneaEntity.class, EntityDataSerializers.BOOLEAN);
    
    // Attack types: 0 = none, 1 = melee, 2 = ranged, 3 = eat (kill)
    public static final EntityDataAccessor<Integer> DATA_ATTACK_STATE = SynchedEntityData.defineId(RexAraneaEntity.class, EntityDataSerializers.INT);

    public static final int ATTACK_NONE = 0;
    public static final int ATTACK_MELEE = 1;
    public static final int ATTACK_RANGED = 2;
    public static final int ATTACK_EAT = 3;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("rex_aranea_walk");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("rex_aranea_idle");
    private static final RawAnimation CLIMB_ANIM = RawAnimation.begin().thenLoop("rex_aranea_climbing");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlayAndHold("rex_aranea_death");

    private boolean fallingFromCeiling = false;
    
    // Custom logic state
    public int eatTimer = 0;
    public int spiderSpawnTimer = 0;
    public int webAttackCooldown = 0;
    public int meleeAttackCooldown = 0;
    public int attackAnimTimer = 0;
    public int webShootDelay = 0;
    public boolean isEating = false;
    public int clientEatTimer = 0;
    public int wallClimbTimer = 0;

    public RexAraneaEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D) // 25 hearts
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D) // 4 hearts
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HAIR_COLOR, 0x333333); 
        builder.define(DATA_CLOTHES_COLOR, 0x222222); 
        builder.define(DATA_IS_WALL_CLIMBING, false);
        builder.define(DATA_IS_CEILING_CLINGING, false);
        builder.define(DATA_ATTACK_STATE, ATTACK_NONE);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // Client side logic: Dynamically fix animation playback speeds
            try {
                software.bernie.geckolib.animation.AnimatableManager<RexAraneaEntity> manager = this.animCache.getManagerForId(this.getId());
                if (manager != null) {
                    software.bernie.geckolib.animation.AnimationController<RexAraneaEntity> controller = manager.getAnimationControllers().get("body_controller");
                    if (controller != null && controller.getCurrentAnimation() != null) {
                        String animName = controller.getCurrentAnimation().animation().name();
                        // Slow down attack/kill animations if they exported too fast from Blockbench
                        if (animName.contains("kill") || animName.contains("attack") || animName.contains("hurt") || animName.contains("feeling")) {
                            controller.setAnimationSpeed(0.5);
                        } else {
                            controller.setAnimationSpeed(1.0); // Keep walk/idle/climb normal
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore initialization race conditions
            }
            
            if (this.entityData.get(DATA_ATTACK_STATE) == ATTACK_EAT) {
                this.clientEatTimer++;
                this.getNavigation().stop();
                this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
                this.setYRot(this.yBodyRot);
                this.setYHeadRot(this.yBodyRot);
            } else {
                this.clientEatTimer = 0;
            }
        } else {
            // Physical climbing is now based on horizontal collision directly in onClimbable.
            // Visual climbing (DATA_IS_WALL_CLIMBING) only triggers if off-ground and moving vertically, avoiding rotation when just pushed.
            if (this.horizontalCollision && !this.onGround() && Math.abs(this.getDeltaMovement().y) > 0.05) {
                this.wallClimbTimer++;
                if (this.wallClimbTimer > 3) {
                    this.entityData.set(DATA_IS_WALL_CLIMBING, true);
                }
            } else {
                this.wallClimbTimer = 0;
                this.entityData.set(DATA_IS_WALL_CLIMBING, false);
            }
            
            handleCustomPhysics();
            
            if (this.entityData.get(DATA_ATTACK_STATE) == ATTACK_NONE && !this.entityData.get(DATA_IS_WALL_CLIMBING) && !this.entityData.get(DATA_IS_CEILING_CLINGING) && this.getDeltaMovement().lengthSqr() < 0.01) {
                if (this.random.nextInt(60) == 0) { // Increased chance from 1/200 to 1/60 (approx every 3 seconds)
                    if (this.random.nextBoolean()) {
                        triggerAnim("leg_controller", "feel_left");
                    } else {
                        triggerAnim("leg_controller", "feel_right");
                    }
                }
            }
            
            // Timers
            if (webAttackCooldown > 0) webAttackCooldown--;
            if (meleeAttackCooldown > 0) meleeAttackCooldown--;
            if (attackAnimTimer > 0) {
                attackAnimTimer--;
                if (attackAnimTimer == 0 && !isEating) {
                    this.entityData.set(DATA_ATTACK_STATE, ATTACK_NONE);
                }
            }
            if (webShootDelay > 0) {
                webShootDelay--;
                if (webShootDelay == 0 && this.getTarget() != null) {
                    double dX = this.getTarget().getX() - this.getX();
                    double dY = this.getTarget().getY(0.5D) - this.getEyeY();
                    double dZ = this.getTarget().getZ() - this.getZ();
                    
                    RexAraneaWebEntity web = new RexAraneaWebEntity(this.level(), this, dX, dY, dZ);
                    web.setPos(this.getX(), this.getEyeY(), this.getZ());
                    this.level().addFreshEntity(web);
                }
            }

            if (this.entityData.get(DATA_ATTACK_STATE) != ATTACK_NONE) {
                this.getNavigation().stop();
                this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            }
            
            if (this.level().isClientSide && this.entityData.get(DATA_ATTACK_STATE) == ATTACK_EAT) {
                if (this.clientEatTimer >= 20 && this.clientEatTimer <= 40 && this.clientEatTimer % 5 == 0) {
                    double yaw = Math.toRadians(this.yBodyRot);
                    double forwardX = -Math.sin(yaw);
                    double forwardZ = Math.cos(yaw);
                    
                    double mouthX = this.getX() + forwardX * 0.8;
                    double mouthY = this.getEyeY() - 0.2;
                    double mouthZ = this.getZ() + forwardZ * 0.8;
                    
                    for (int i = 0; i < 3; i++) {
                        this.level().addParticle(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEEF)), 
                            mouthX,
                            mouthY,
                            mouthZ,
                            (this.random.nextFloat() - 0.5) * 0.15,
                            this.random.nextFloat() * 0.15 + 0.05,
                            (this.random.nextFloat() - 0.5) * 0.15);
                    }
                }
            }

            if (isEating) {
                eatTimer++;
                this.entityData.set(DATA_ATTACK_STATE, ATTACK_EAT); // Ensure state is set
                if (eatTimer >= 40) { // 2 seconds eating time approx
                    isEating = false;
                    eatTimer = 0;
                    this.heal(30.0F); // 15 hearts
                    this.spiderSpawnTimer = 200; // 10 seconds
                    this.entityData.set(DATA_ATTACK_STATE, ATTACK_NONE);
                }
            } else if (spiderSpawnTimer > 0) {
                spiderSpawnTimer--;
                if (spiderSpawnTimer == 0) {
                    spawnSpiders();
                }
            }
        }
    }
    
    private void spawnSpiders() {
        int count = 1 + this.random.nextInt(3);
        double dX = Math.sin(Math.toRadians(this.getYRot())) * 1.5;
        double dZ = -Math.cos(Math.toRadians(this.getYRot())) * 1.5;
        
        for (int i = 0; i < count; i++) {
            Monster spider;
            int type = this.random.nextInt(3);
            if (type == 0) spider = new net.minecraft.world.entity.monster.CaveSpider(EntityType.CAVE_SPIDER, this.level());
            else if (type == 1) spider = new net.minecraft.world.entity.monster.Spider(EntityType.SPIDER, this.level());
            else spider = new SplinterSpiderEntity(net.ganyusbathwater.oririmod.entity.ModEntities.SPLINTER_SPIDER.get(), this.level());
            
            spider.setPos(this.getX() + dX, this.getY() + 1.0D, this.getZ() + dZ);
            this.level().addFreshEntity(spider);
        }
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new net.minecraft.world.entity.ai.navigation.WallClimberNavigation(this, level);
    }

    @Override
    public boolean onClimbable() {
        return this.horizontalCollision;
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
        if (!state.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(state, motionMultiplier);
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.entityData.get(DATA_ATTACK_STATE) != ATTACK_NONE) {
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RexAraneaCeilingMoveGoal(this));
        this.goalSelector.addGoal(2, new RexAraneaSafetyGoal(this));
        this.goalSelector.addGoal(3, new RexAraneaPredatorGoal(this));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Animal.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Skeleton.class, true));
    }

    private void handleCustomPhysics() {
        // Wall climbing checks handled by Vanilla through onClimbable, 
        // we just sync it to client for rendering.
        // Ceiling cling checks
        boolean isCollidingAbove = this.verticalCollision && this.getDeltaMovement().y > 0;
        // Or if there's a block directly above the AABB
        boolean blockAbove = !this.level().noCollision(this.getBoundingBox().move(0, 0.1, 0));

        boolean wasClinging = this.entityData.get(DATA_IS_CEILING_CLINGING);
        boolean shouldCling = false;

        if (blockAbove || wasClinging) {
            // Check clearance if we weren't just climbing a wall
            double distanceToFloor = getDistanceToFloor();
            if (this.horizontalCollision || distanceToFloor >= 4.0 || wasClinging) {
                shouldCling = blockAbove; 
            }
        }

        // Target drop logic
        if (shouldCling && this.getTarget() != null) {
            double hDistSqr = this.distanceToSqr(this.getTarget().getX(), this.getY(), this.getTarget().getZ());
            if (hDistSqr < 4.0 && this.getY() > this.getTarget().getY()) { // Target directly below
                shouldCling = false;
                this.fallingFromCeiling = true; // Negate fall damage
            }
        }

        this.entityData.set(DATA_IS_CEILING_CLINGING, shouldCling);

        if (shouldCling) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, 0.08D, motion.z); // Constant upward velocity to stick
            this.setNoGravity(true);
        } else {
            this.setNoGravity(false);
        }

        // Reset fall distance if dropping safely or clinging
        if (shouldCling || fallingFromCeiling) {
            this.fallDistance = 0.0F;
        }

        if (this.onGround()) {
            this.fallingFromCeiling = false;
        }
    }

    private double getDistanceToFloor() {
        BlockPos current = this.blockPosition();
        int y = current.getY();
        while (y > this.level().getMinBuildHeight()) {
            BlockPos p = new BlockPos(current.getX(), y, current.getZ());
            if (!this.level().getBlockState(p).getCollisionShape(this.level(), p).isEmpty()) {
                break;
            }
            y--;
        }
        return this.getY() - y;
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        // Wait 30 ticks (1.5 seconds) for death animation before despawning
        if (this.deathTime >= 30 && !this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // Play hurt animation, handled by controllers checking hurtTime on client?
        // Or triggerAnim
        if (!this.level().isClientSide) {
            triggerAnim("body_controller", "hurt");
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof net.minecraft.world.entity.LivingEntity livingTarget) {
            if (this.random.nextFloat() < 0.25f) {
                livingTarget.addEffect(new MobEffectInstance(ModEffects.STUNNED_EFFECT, 100, 0));
            }
        }
        return hurt;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body_controller", 3, state -> {
            if (this.isDeadOrDying()) {
                if (state.getController().getCurrentAnimation() != null && !state.getController().getCurrentAnimation().animation().name().equals("rex_aranea_death")) {
                    state.getController().forceAnimationReset();
                }
                return state.setAndContinue(DEATH_ANIM);
            }
            
            if (this.entityData.get(DATA_ATTACK_STATE) != ATTACK_NONE) {
                // Let the triggered attack/kill animation play without Walk/Idle overriding it!
                return software.bernie.geckolib.animation.PlayState.CONTINUE;
            }
            
            boolean isClimbing = this.entityData.get(DATA_IS_WALL_CLIMBING) || this.entityData.get(DATA_IS_CEILING_CLINGING);
            if (isClimbing) {
                return state.setAndContinue(CLIMB_ANIM);
            }
            
            if (state.isMoving()) {
                return state.setAndContinue(WALK_ANIM);
            }

            return state.setAndContinue(IDLE_ANIM);
        })
        .triggerableAnim("normal_attack", RawAnimation.begin().then("rex_aranea_normal_attack", Animation.LoopType.PLAY_ONCE))
        .triggerableAnim("special_attack", RawAnimation.begin().then("rex_aranea_special_attack", Animation.LoopType.PLAY_ONCE))
        .triggerableAnim("kill", RawAnimation.begin().then("rex_aranea_kill", Animation.LoopType.PLAY_ONCE))
        .triggerableAnim("hurt", RawAnimation.begin().then("rex_aranea_hurt", Animation.LoopType.PLAY_ONCE)));

        controllers.add(new AnimationController<>(this, "leg_controller", 3, state -> {
            return software.bernie.geckolib.animation.PlayState.CONTINUE;
        })
        .triggerableAnim("feel_left", RawAnimation.begin().then("rex_aranea_idle_feeling_leg_left", Animation.LoopType.PLAY_ONCE))
        .triggerableAnim("feel_right", RawAnimation.begin().then("rex_aranea_idle_feeling_leg_right", Animation.LoopType.PLAY_ONCE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    @Override
    public Element getElement() {
        return Element.DARKNESS;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HairColor", this.entityData.get(DATA_HAIR_COLOR));
        tag.putInt("ClothesColor", this.entityData.get(DATA_CLOTHES_COLOR));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HairColor")) {
            this.entityData.set(DATA_HAIR_COLOR, tag.getInt("HairColor"));
        }
        if (tag.contains("ClothesColor")) {
            this.entityData.set(DATA_CLOTHES_COLOR, tag.getInt("ClothesColor"));
        }
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType spawnType, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.SpawnGroupData spawnGroupData) {
        // Random dark colors
        int[] darkColors = {0x1a1a1a, 0x2b2b2b, 0x3d3d3d, 0x141414, 0x000000, 0x4a0000, 0x000033};
        this.entityData.set(DATA_HAIR_COLOR, darkColors[this.random.nextInt(darkColors.length)]);
        this.entityData.set(DATA_CLOTHES_COLOR, darkColors[this.random.nextInt(darkColors.length)]);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    // --- INNER CLASSES (AI GOALS) ---

    class RexAraneaSafetyGoal extends Goal {
        private final RexAraneaEntity mob;
        private BlockPos targetWeb;
        private int spinCooldown;

        public RexAraneaSafetyGoal(RexAraneaEntity mob) {
            this.mob = mob;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.mob.getTarget() == null && !this.mob.isEating;
        }

        @Override
        public void start() {
            this.targetWeb = findNearbyWeb();
            this.spinCooldown = 60;
        }

        @Override
        public void tick() {
            if (this.targetWeb != null) {
                if (this.mob.distanceToSqr(Vec3.atCenterOf(this.targetWeb)) > 16.0) {
                    this.mob.getNavigation().moveTo(this.targetWeb.getX(), this.targetWeb.getY(), this.targetWeb.getZ(), 1.0D);
                }
            } else {
                this.spinCooldown--;
                if (this.spinCooldown <= 0) {
                    spinWebs();
                    this.spinCooldown = 200;
                    this.targetWeb = findNearbyWeb();
                }
            }
        }

        private BlockPos findNearbyWeb() {
            BlockPos current = this.mob.blockPosition();
            for (int x = -16; x <= 16; x++) {
                for (int y = -8; y <= 8; y++) {
                    for (int z = -16; z <= 16; z++) {
                        BlockPos p = current.offset(x, y, z);
                        BlockState state = this.mob.level().getBlockState(p);
                        if (state.is(Blocks.COBWEB) || state.is(BlockTags.WOOL)) {
                            return p;
                        }
                    }
                }
            }
            return null;
        }

        private void spinWebs() {
            BlockPos pos = this.mob.blockPosition();
            int count = 0;
            for (int i = 0; i < 10 && count < 5; i++) {
                BlockPos target = pos.offset(this.mob.random.nextInt(5) - 2, this.mob.random.nextInt(3) - 1, this.mob.random.nextInt(5) - 2);
                if (this.mob.level().isEmptyBlock(target)) {
                    this.mob.level().setBlockAndUpdate(target, Blocks.COBWEB.defaultBlockState());
                    count++;
                }
            }
        }
    }

    class RexAraneaCeilingMoveGoal extends Goal {
        private final RexAraneaEntity mob;

        public RexAraneaCeilingMoveGoal(RexAraneaEntity mob) {
            this.mob = mob;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.mob.entityData.get(DATA_IS_CEILING_CLINGING) && this.mob.getTarget() != null;
        }

        @Override
        public void tick() {
            net.minecraft.world.entity.LivingEntity target = this.mob.getTarget();
            if (target != null) {
                double dX = target.getX() - this.mob.getX();
                double dZ = target.getZ() - this.mob.getZ();
                double dist = Math.sqrt(dX * dX + dZ * dZ);
                
                if (dist > 1.0) {
                    double speed = this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
                    this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(dX / dist * speed * 0.2, 0, dZ / dist * speed * 0.2));
                    
                    float yaw = (float) (Math.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0f;
                    this.mob.setYRot(yaw);
                    this.mob.yBodyRot = yaw;
                }
            }
        }
    }

    class RexAraneaPredatorGoal extends Goal {
        private final RexAraneaEntity mob;

        public RexAraneaPredatorGoal(RexAraneaEntity mob) {
            this.mob = mob;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.mob.getTarget() != null && !this.mob.isEating;
        }

        @Override
        public void tick() {
            net.minecraft.world.entity.LivingEntity target = this.mob.getTarget();
            if (target == null) return;

            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSqr = this.mob.distanceToSqr(target);
            
            // Edible check
            boolean isEdible = target instanceof Player || target instanceof Animal;

            if (distSqr < 9.0) {
                // Melee
                if (this.mob.entityData.get(DATA_ATTACK_STATE) != ATTACK_NONE) {
                    this.mob.getNavigation().stop();
                    this.mob.setDeltaMovement(0, this.mob.getDeltaMovement().y, 0);
                } else if (!this.mob.entityData.get(DATA_IS_CEILING_CLINGING)) {
                    this.mob.getNavigation().moveTo(target, 1.2D);
                }
                
                if (this.mob.meleeAttackCooldown <= 0) {
                    this.mob.entityData.set(DATA_ATTACK_STATE, ATTACK_MELEE);
                    this.mob.attackAnimTimer = 15; // 0.75s
                    this.mob.triggerAnim("body_controller", "normal_attack");
                    this.mob.doHurtTarget(target);
                    this.mob.meleeAttackCooldown = 15;
                    
                    // Kill check
                    if (target.isDeadOrDying() && isEdible) {
                        this.mob.isEating = true;
                        this.mob.attackAnimTimer = 60; // 3s
                        this.mob.entityData.set(DATA_ATTACK_STATE, ATTACK_EAT);
                        this.mob.triggerAnim("body_controller", "kill");
                        this.mob.setTarget(null);
                    }
                }
            } else {
                // Ranged
                if (this.mob.entityData.get(DATA_ATTACK_STATE) != ATTACK_NONE) {
                    this.mob.getNavigation().stop();
                    this.mob.setDeltaMovement(0, this.mob.getDeltaMovement().y, 0);
                } else if (!this.mob.entityData.get(DATA_IS_CEILING_CLINGING)) {
                    this.mob.getNavigation().moveTo(target, 1.0D);
                }
                
                if (this.mob.webAttackCooldown <= 0 && this.mob.hasLineOfSight(target)) {
                    this.mob.entityData.set(DATA_ATTACK_STATE, ATTACK_RANGED);
                    this.mob.attackAnimTimer = 30;
                    this.mob.triggerAnim("body_controller", "special_attack");
                    
                    this.mob.webShootDelay = 15;
                    this.mob.webAttackCooldown = 100; // 5s
                }
            }
        }
    }
}
