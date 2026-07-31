package net.ganyusbathwater.oririmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AirSliceEntity extends Projectile implements GeoEntity {

    private static final EntityDataAccessor<Boolean> HOMING_ENABLED = SynchedEntityData.defineId(AirSliceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CAN_BREAK_BLOCKS = SynchedEntityData.defineId(AirSliceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIFESPAN = SynchedEntityData.defineId(AirSliceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(AirSliceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROLL_ANGLE = SynchedEntityData.defineId(AirSliceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(AirSliceEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final Set<UUID> piercedEntities = new HashSet<>();

    public AirSliceEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public AirSliceEntity(Level level, LivingEntity owner) {
        super(ModEntities.AIR_SLICE.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HOMING_ENABLED, false);
        builder.define(CAN_BREAK_BLOCKS, false);
        builder.define(LIFESPAN, 100);
        builder.define(DAMAGE, 10.0f);
        builder.define(ROLL_ANGLE, 0.0f);
        builder.define(COLOR, 0xFFFFFF);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setHomingEnabled(tag.getBoolean("HomingEnabled"));
        this.setCanBreakBlocks(tag.getBoolean("CanBreakBlocks"));
        this.setLifespan(tag.getInt("Lifespan"));
        this.setDamage(tag.getFloat("Damage"));
        this.setRollAngle(tag.getFloat("RollAngle"));
        if (tag.contains("Color")) {
            this.setColor(tag.getInt("Color"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HomingEnabled", this.isHomingEnabled());
        tag.putBoolean("CanBreakBlocks", this.canBreakBlocks());
        tag.putInt("Lifespan", this.getLifespan());
        tag.putFloat("Damage", this.getDamage());
        tag.putFloat("RollAngle", this.getRollAngle());
    }

    public void setHomingEnabled(boolean homing) { this.entityData.set(HOMING_ENABLED, homing); }
    public boolean isHomingEnabled() { return this.entityData.get(HOMING_ENABLED); }
    public void setCanBreakBlocks(boolean canBreak) { this.entityData.set(CAN_BREAK_BLOCKS, canBreak); }
    public boolean canBreakBlocks() { return this.entityData.get(CAN_BREAK_BLOCKS); }
    public void setLifespan(int lifespan) { this.entityData.set(LIFESPAN, lifespan); }
    public int getLifespan() { return this.entityData.get(LIFESPAN); }
    public void setDamage(float damage) { this.entityData.set(DAMAGE, damage); }
    public float getDamage() { return this.entityData.get(DAMAGE); }
    public void setRollAngle(float angle) { this.entityData.set(ROLL_ANGLE, angle); }
    public float getRollAngle() { return this.entityData.get(ROLL_ANGLE); }
    public int getColor() { return this.entityData.get(COLOR); }
    public void setColor(int color) { this.entityData.set(COLOR, color); }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.tickCount > this.getLifespan()) {
            this.discard();
            return;
        }

        // Homing Logic
        if (this.isHomingEnabled() && !this.level().isClientSide) {
            LivingEntity target = getNearestTarget();
            if (target != null) {
                Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
                Vec3 currentDir = this.getDeltaMovement().normalize();
                Vec3 desiredDir = targetPos.subtract(this.position()).normalize();
                
                // Slowly interpolate towards target
                Vec3 newDir = currentDir.scale(0.85).add(desiredDir.scale(0.15)).normalize();
                double speed = this.getDeltaMovement().length();
                if (speed < 0.1) speed = 1.0; // Failsafe if spawned with 0 speed
                this.setDeltaMovement(newDir.scale(speed));
            }
        }

        // Update Position and Rotation
        Vec3 vel = this.getDeltaMovement();
        this.setPos(this.getX() + vel.x, this.getY() + vel.y, this.getZ() + vel.z);
        this.updateRotation();

        // Projectile Hit Logic
        net.minecraft.world.phys.HitResult hitResult = net.minecraft.world.entity.projectile.ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            this.onHit(hitResult);
        }

        // Spawn particles on client side
        if (this.level().isClientSide && this.tickCount % 2 == 0) {
            spawnWindParticles();
        }

        // Ensure we don't fall forever if homing is off
        this.setNoGravity(true);
    }

    private void spawnWindParticles() {
        Vec3 reverseVel = this.getDeltaMovement().normalize().scale(-0.2); // Move away from travel direction

        // Locators are defined at [0, 15, 2] and [0, 2, 2] in the geo.json
        // In block units, this is roughly Y=0.9375, Z=0.125 and Y=0.125, Z=0.125
        Vec3 topLocator = new Vec3(0, 15.0/16.0, 2.0/16.0);
        Vec3 bottomLocator = new Vec3(0, 2.0/16.0, 2.0/16.0);

        // Apply roll, pitch, yaw to locators to match the renderer
        // The renderer applies Yaw -> Pitch -> Roll (around Z).
        // In vector math (applied right to left), we apply Roll -> Pitch -> Yaw.
        float pitchRad = this.getXRot() * (float) (Math.PI / 180.0);
        float yawRad = -this.getYRot() * (float) (Math.PI / 180.0); // Vec3 yRot is negative of yaw
        float rollRad = this.getRollAngle() * (float) (Math.PI / 180.0);

        topLocator = topLocator.zRot(rollRad).xRot(pitchRad).yRot(yawRad);
        bottomLocator = bottomLocator.zRot(rollRad).xRot(pitchRad).yRot(yawRad);

        this.level().addParticle(ParticleTypes.CLOUD, 
            this.getX() + topLocator.x, this.getY() + topLocator.y, this.getZ() + topLocator.z,
            reverseVel.x, reverseVel.y, reverseVel.z);

        this.level().addParticle(ParticleTypes.CLOUD, 
            this.getX() + bottomLocator.x, this.getY() + bottomLocator.y, this.getZ() + bottomLocator.z,
            reverseVel.x, reverseVel.y, reverseVel.z);
    }

    private LivingEntity getNearestTarget() {
        AABB scanBox = this.getBoundingBox().inflate(30.0D);
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, scanBox, e -> e != this.getOwner() && e.isAlive());
        LivingEntity nearest = null;
        double minDistance = Double.MAX_VALUE;
        for (LivingEntity e : entities) {
            double dist = this.distanceToSqr(e);
            if (dist < minDistance) {
                minDistance = dist;
                nearest = e;
            }
        }
        return nearest;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (!this.piercedEntities.contains(entity.getUUID())) {
            this.piercedEntities.add(entity.getUUID());
            entity.hurt(this.damageSources().thrown(this, this.getOwner()), this.getDamage());
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide) {
            if (this.canBreakBlocks()) {
                BlockPos pos = result.getBlockPos();
                this.level().destroyBlock(pos, false);
                // We do NOT call super.onHitBlock(result) here, so the velocity isn't reset and it keeps flying!
            } else {
                super.onHitBlock(result);
                this.discard();
            }
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.air_slice.idle"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }
}
