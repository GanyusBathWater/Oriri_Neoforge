package net.ganyusbathwater.oririmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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
        this.setPos(owner.getX(), owner.getEyeY() - 1.0D, owner.getZ());
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
        tag.putInt("Color", this.getColor());
    }

    public void setHomingEnabled(boolean homing) { this.entityData.set(HOMING_ENABLED, homing); }
    public boolean isHomingEnabled() { return this.entityData.get(HOMING_ENABLED); }
    public void setCanBreakBlocks(boolean canBreak) { this.entityData.set(CAN_BREAK_BLOCKS, canBreak); }
    public boolean canBreakBlocks() { return this.entityData.get(CAN_BREAK_BLOCKS); }
    public void setLifespan(int lifespan) { this.entityData.set(LIFESPAN, lifespan); }
    public int getLifespan() { return this.entityData.get(LIFESPAN); }
    public void setDamage(float damage) { this.entityData.set(DAMAGE, damage); }
    public float getDamage() { return this.entityData.get(DAMAGE); }
    public void setRollAngle(float angle) {
        this.entityData.set(ROLL_ANGLE, angle);
        this.setBoundingBox(this.makeBoundingBox());
    }

    public float getRollAngle() { return this.entityData.get(ROLL_ANGLE); }
    public int getColor() { return this.entityData.get(COLOR); }
    public void setColor(int color) { this.entityData.set(COLOR, color); }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ROLL_ANGLE.equals(key)) {
            this.setBoundingBox(this.makeBoundingBox());
        }
    }

    @Override
    protected AABB makeBoundingBox() {
        if (this.entityData == null) {
            return new AABB(-0.125D, 0.0D, -0.125D, 0.125D, 2.0D, 0.125D).move(this.position());
        }
        return calculateOrientedBox(this.position());
    }

    public AABB calculateOrientedBox(Vec3 pos) {
        float yaw = this.getYRot();
        float pitch = this.getXRot();
        float roll = this.getRollAngle();

        org.joml.Quaternionf rot = new org.joml.Quaternionf();
        rot.rotationY((float) Math.toRadians(yaw + 180.0F));
        rot.rotateX((float) Math.toRadians(pitch));
        rot.rotateZ((float) Math.toRadians(roll));

        // Half-extents of the blade in local space:
        // Height axis: length 2.0m -> half-extent 1.0m along Y
        // Width axis: thickness 0.25m -> half-extent 0.125m along X
        // Depth axis: crescent curve ~1.3m -> half-extent 0.65m along Z
        org.joml.Vector3f sVec = rot.transform(new org.joml.Vector3f(0.0F, 1.0F, 0.0F));
        org.joml.Vector3f wVec = rot.transform(new org.joml.Vector3f(0.125F, 0.0F, 0.0F));
        org.joml.Vector3f dVec = rot.transform(new org.joml.Vector3f(0.0F, 0.0F, 0.65F));

        double hx = Math.abs(sVec.x) + Math.abs(wVec.x) + Math.abs(dVec.x);
        double hy = Math.abs(sVec.y) + Math.abs(wVec.y) + Math.abs(dVec.y);
        double hz = Math.abs(sVec.z) + Math.abs(wVec.z) + Math.abs(dVec.z);

        Vec3 center = pos.add(0.0D, 1.0D, 0.0D);
        return new AABB(center.x - hx, center.y - hy, center.z - hz,
                        center.x + hx, center.y + hy, center.z + hz);
    }

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

        Vec3 startPos = this.position();
        Vec3 vel = this.getDeltaMovement();
        double speed = vel.length();

        // Continuous Collision Detection (CCD) for block destruction and entity hits
        if (speed > 0.001D) {
            // Check sub-steps every 0.2 blocks to ensure no high-speed tunneling through blocks
            int steps = Math.max(1, (int) Math.ceil(speed / 0.2D));
            Set<BlockPos> brokenBlocks = new HashSet<>();

            for (int i = 1; i <= steps; i++) {
                double t = (double) i / steps;
                Vec3 stepPos = startPos.add(vel.scale(t));
                AABB stepBox = calculateOrientedBox(stepPos).inflate(0.05D);

                // 1. Process Blocks (Server-side)
                if (!this.level().isClientSide) {
                    BlockPos minPos = BlockPos.containing(stepBox.minX, stepBox.minY, stepBox.minZ);
                    BlockPos maxPos = BlockPos.containing(stepBox.maxX, stepBox.maxY, stepBox.maxZ);

                    for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
                        if (brokenBlocks.contains(pos)) continue;

                        net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(pos);
                        if (state.isAir() || state.liquid()) continue;

                        AABB blockAABB = new AABB(pos);
                        if (!stepBox.intersects(blockAABB)) continue;

                        float destroySpeed = state.getDestroySpeed(this.level(), pos);
                        if (destroySpeed < 0.0F) {
                            // Unbreakable block (Bedrock, Barrier, End Portal Frame, etc.) -> collide & discard
                            this.onHitBlock(new BlockHitResult(stepPos, net.minecraft.core.Direction.getNearest(vel.x, vel.y, vel.z).getOpposite(), pos.immutable(), false));
                            this.discard();
                            return;
                        }

                        if (this.canBreakBlocks()) {
                            brokenBlocks.add(pos.immutable());
                            this.level().destroyBlock(pos, false);
                        } else if (state.isSolid()) {
                            // Cannot break blocks (e.g. inside a dungeon): impact and discard
                            this.onHitBlock(new BlockHitResult(stepPos, net.minecraft.core.Direction.getNearest(vel.x, vel.y, vel.z).getOpposite(), pos.immutable(), false));
                            this.discard();
                            return;
                        }
                    }
                }
            }

            // 2. Process Entities along the swept path
            AABB sweptBox = calculateOrientedBox(startPos).minmax(calculateOrientedBox(startPos.add(vel))).inflate(0.2D);
            List<LivingEntity> hitEntities = this.level().getEntitiesOfClass(LivingEntity.class, sweptBox, this::canHitEntity);
            for (LivingEntity target : hitEntities) {
                if (target == this.getOwner()) continue;
                if (!this.piercedEntities.contains(target.getUUID())) {
                    if (target.getBoundingBox().intersects(sweptBox)) {
                        this.piercedEntities.add(target.getUUID());
                        target.hurt(this.damageSources().thrown(this, this.getOwner()), this.getDamage());
                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() / 2.0D, target.getZ(), 1, 0, 0, 0, 0);
                        }
                    }
                }
            }
        }

        // Update Position and Rotation
        this.setPos(startPos.add(vel));
        this.updateRotation();
        this.setBoundingBox(this.makeBoundingBox());

        // Spawn particles on client side
        if (this.level().isClientSide && this.tickCount % 2 == 0) {
            spawnWindParticles();
        }

        // Ensure we don't fall forever if homing is off
        this.setNoGravity(true);
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity, inaccuracy);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    public void shootFromRotation(Entity shooter, float x, float y, float z, float velocity, float inaccuracy) {
        super.shootFromRotation(shooter, x, y, z, velocity, inaccuracy);
        Vec3 vel = this.getDeltaMovement();
        double d0 = vel.horizontalDistance();
        if (d0 > 0.0001D || Math.abs(vel.y) > 0.0001D) {
            this.setYRot((float)(Mth.atan2(vel.x, vel.z) * 180.0F / (float)Math.PI));
            this.setXRot((float)(Mth.atan2(vel.y, d0) * 180.0F / (float)Math.PI));
        }
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        super.lerpMotion(x, y, z);
        double d0 = Math.sqrt(x * x + z * z);
        this.setXRot((float)(Mth.atan2(y, d0) * 180.0F / (float)Math.PI));
        this.setYRot((float)(Mth.atan2(x, z) * 180.0F / (float)Math.PI));
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    protected void updateRotation() {
        Vec3 vec3 = this.getDeltaMovement();
        double d0 = vec3.horizontalDistance();
        if (d0 > 0.0001D || Math.abs(vec3.y) > 0.0001D) {
            this.setXRot((float)(Mth.atan2(vec3.y, d0) * 180.0F / (float)Math.PI));
            this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * 180.0F / (float)Math.PI));
        }
    }

    private void spawnWindParticles() {
        Vec3 reverseVel = this.getDeltaMovement().normalize().scale(-0.2);

        org.joml.Quaternionf rot = new org.joml.Quaternionf();
        rot.rotationY((float) Math.toRadians(this.getYRot() + 180.0F));
        rot.rotateX((float) Math.toRadians(this.getXRot()));
        rot.rotateZ((float) Math.toRadians(this.getRollAngle()));

        // Local offsets relative to (0, 1.0, 0) center:
        // Top locator at Y = 29/16, Z = 12/16 -> relative to center: Y = 13/16, Z = 12/16
        // Bottom locator at Y = 2/16, Z = 2/16 -> relative to center: Y = -14/16, Z = 2/16
        org.joml.Vector3f topRel = rot.transform(new org.joml.Vector3f(0.0F, (29.0F - 16.0F) / 16.0F, 12.0F / 16.0F));
        org.joml.Vector3f botRel = rot.transform(new org.joml.Vector3f(0.0F, (2.0F - 16.0F) / 16.0F, 2.0F / 16.0F));

        double cx = this.getX();
        double cy = this.getY() + 1.0D;
        double cz = this.getZ();

        this.level().addParticle(ParticleTypes.CLOUD, 
            cx + topRel.x, cy + topRel.y, cz + topRel.z,
            reverseVel.x, reverseVel.y, reverseVel.z);

        this.level().addParticle(ParticleTypes.CLOUD, 
            cx + botRel.x, cy + botRel.y, cz + botRel.z,
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
        controllers.add(new AnimationController<>(this, "traveling", 0, state -> {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("air_slice_traveling"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }
}
