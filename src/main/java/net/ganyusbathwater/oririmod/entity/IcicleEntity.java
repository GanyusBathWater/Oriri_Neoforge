package net.ganyusbathwater.oririmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class IcicleEntity extends Projectile {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(IcicleEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLOATING_TICKS = SynchedEntityData.defineId(IcicleEntity.class,
            EntityDataSerializers.INT);

    private BlockPos impactPos = BlockPos.ZERO;
    private int maxLife = 20 * 10; // 10s failsafe
    private static final float DAMAGE = 20.0f; // 10 hearts
    private static final float IMPACT_RADIUS = 1.5f;
    private static final int FREEZE_TICKS = 300; // enough to trigger full freeze effect

    public IcicleEntity(EntityType<? extends IcicleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public void configure(BlockPos impactPos) {
        this.impactPos = impactPos.immutable();
    }

    public void setOwnerId(int id) {
        this.entityData.set(OWNER_ID, id);
    }

    public int getOwnerId() {
        return this.entityData.get(OWNER_ID);
    }

    public void setFloatingTicks(int ticks) {
        this.entityData.set(FLOATING_TICKS, ticks);
    }

    public int getFloatingTicks() {
        return this.entityData.get(FLOATING_TICKS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_ID, 0);
        builder.define(FLOATING_TICKS, 30);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.impactPos = BlockPos.of(tag.getLong("ImpactPos"));
        this.maxLife = tag.getInt("MaxLife");
        if (tag.contains("OwnerId")) {
            this.setOwnerId(tag.getInt("OwnerId"));
        }
        if (tag.contains("FloatingTicks")) {
            this.setFloatingTicks(tag.getInt("FloatingTicks"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("ImpactPos", this.impactPos.asLong());
        tag.putInt("MaxLife", this.maxLife);
        tag.putInt("OwnerId", this.getOwnerId());
        tag.putInt("FloatingTicks", this.getFloatingTicks());
    }

    @Override
    public void tick() {
        super.tick();
        int floatTicks = this.getFloatingTicks();

        if (!level().isClientSide) {
            // Extend max life by the floating duration so it doesn't despawn early
            if (this.tickCount > maxLife + floatTicks || !level().hasChunkAt(blockPosition())) {
                discard();
                return;
            }
        }

        Vec3 vel = getDeltaMovement();

        if (this.tickCount < floatTicks) {
            // Floating phase: no gravity
            vel = Vec3.ZERO;
            setDeltaMovement(vel);
        } else if (this.tickCount == floatTicks) {
            // Start falling with an initial velocity
            vel = new Vec3(0, -0.2, 0);
            setDeltaMovement(vel);
        } else {
            // Accelerate downward (gravity)
            vel = new Vec3(vel.x * 0.99, vel.y - 0.03, vel.z * 0.99);
            setDeltaMovement(vel);
        }

        move(MoverType.SELF, vel);

        // Spawn ice particles (Client-Side)
        if (level().isClientSide) {
            // Snowflakes all the time
            for (int i = 0; i < 2; i++) {
                level().addParticle(ParticleTypes.SNOWFLAKE,
                        this.getX() + (level().random.nextDouble() - 0.5) * 0.2,
                        this.getY() + level().random.nextDouble() * 0.4,
                        this.getZ() + (level().random.nextDouble() - 0.5) * 0.2,
                        (level().random.nextDouble() - 0.5) * 0.02,
                        -0.01,
                        (level().random.nextDouble() - 0.5) * 0.02);
            }
            // Snowballs only while falling
            if (this.tickCount % 5 == 0 && this.tickCount >= floatTicks) {
                level().addParticle(ParticleTypes.ITEM_SNOWBALL,
                        this.getX() + (level().random.nextDouble() - 0.5) * 0.1,
                        this.getY() + level().random.nextDouble() * 0.2,
                        this.getZ() + (level().random.nextDouble() - 0.5) * 0.1,
                        0.0, 0.0, 0.0);
            }
        }

        boolean hitY = this.getY() <= impactPos.getY() + 1.0;
        boolean collide = this.verticalCollision || this.horizontalCollision || isBlockSolidBelow();
        if (!level().isClientSide && (hitY || collide)) {
            doImpact();
        }
    }

    private boolean isBlockSolidBelow() {
        BlockPos below = this.blockPosition().below();
        return !level().getBlockState(below).isAir();
    }

    private void doImpact() {
        ServerLevel server = (ServerLevel) level();

        // Sound
        server.playSound(
                null,
                getX(), getY(), getZ(),
                SoundEvents.GLASS_BREAK,
                SoundSource.BLOCKS,
                1.5f,
                0.8f + server.random.nextFloat() * 0.4f);

        // Particles
        server.sendParticles(ParticleTypes.ITEM_SNOWBALL, getX(), getY(), getZ(), 20, 0.8, 0.3, 0.8, 0.05);
        server.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 15, 1.0, 0.5, 1.0, 0.03);

        // Damage and freeze entities in radius
        AABB area = new AABB(
                getX() - IMPACT_RADIUS, getY() - 1.0, getZ() - IMPACT_RADIUS,
                getX() + IMPACT_RADIUS, getY() + 2.0, getZ() + IMPACT_RADIUS);
        List<Entity> entities = level().getEntities(this, area);
        for (Entity e : entities) {
            if (e instanceof LivingEntity living) {
                // Deal 10 hearts of damage
                living.hurt(level().damageSources().freeze(), DAMAGE);
                // Apply freezing
                living.setTicksFrozen(Math.max(living.getTicksFrozen(), FREEZE_TICKS));
            }
        }

        discard();
    }
}
