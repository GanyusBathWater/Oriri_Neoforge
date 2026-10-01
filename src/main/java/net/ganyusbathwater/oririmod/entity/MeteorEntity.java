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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class MeteorEntity extends Projectile {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(MeteorEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> METEOR_SCALE = SynchedEntityData.defineId(MeteorEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> METEOR_GRAVITY = SynchedEntityData.defineId(MeteorEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EXPLOSION_POWER = SynchedEntityData.defineId(MeteorEntity.class,
            EntityDataSerializers.FLOAT);

    private BlockPos impactPos = BlockPos.ZERO;
    private float explosionPower = 4.0f;
    private int fireRadius = 3;
    private int maxLife = 20 * 10; // 10s Failsafe
    private boolean destroysBlocks = true;

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public void configure(BlockPos impactPos, float explosionPower, int fireRadius) {
        this.impactPos = impactPos.immutable();
        this.explosionPower = explosionPower;
        this.entityData.set(EXPLOSION_POWER, explosionPower);
        this.fireRadius = Math.max(0, fireRadius);
    }

    public void setOwnerId(int id) {
        this.entityData.set(OWNER_ID, id);
    }

    public int getOwnerId() {
        return this.entityData.get(OWNER_ID);
    }

    public void setMeteorScale(float scale) {
        this.entityData.set(METEOR_SCALE, scale);
    }

    public float getMeteorScale() {
        return this.entityData.get(METEOR_SCALE);
    }

    public void setDestroysBlocks(boolean destroysBlocks) {
        this.destroysBlocks = destroysBlocks;
    }

    public boolean destroysBlocks() {
        return this.destroysBlocks;
    }

    public void setMeteorGravity(float gravity) {
        this.entityData.set(METEOR_GRAVITY, gravity);
    }

    public float getMeteorGravity() {
        return this.entityData.get(METEOR_GRAVITY);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // 1.21+: Synched‑Daten über den Builder registrieren
        builder.define(OWNER_ID, 0);
        builder.define(METEOR_SCALE, 5.5f);
        builder.define(METEOR_GRAVITY, 0.12f);
        builder.define(EXPLOSION_POWER, 4.0f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.impactPos = BlockPos.of(tag.getLong("ImpactPos"));
        this.explosionPower = tag.getFloat("Power");
        this.fireRadius = tag.getInt("FireRadius");
        this.maxLife = tag.getInt("MaxLife");
        if (tag.contains("OwnerId")) {
            this.setOwnerId(tag.getInt("OwnerId"));
        }
        if (tag.contains("MeteorScale")) {
            this.setMeteorScale(tag.getFloat("MeteorScale"));
        }
        if (tag.contains("DestroysBlocks")) {
            this.destroysBlocks = tag.getBoolean("DestroysBlocks");
        }
        if (tag.contains("MeteorGravity")) {
            this.setMeteorGravity(tag.getFloat("MeteorGravity"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("ImpactPos", this.impactPos.asLong());
        tag.putFloat("Power", this.explosionPower);
        tag.putInt("FireRadius", this.fireRadius);
        tag.putInt("MaxLife", this.maxLife);
        tag.putInt("OwnerId", this.getOwnerId());
        tag.putFloat("MeteorScale", this.getMeteorScale());
        tag.putBoolean("DestroysBlocks", this.destroysBlocks);
        tag.putFloat("MeteorGravity", this.getMeteorGravity());
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 vel = getDeltaMovement();
        vel = new Vec3(vel.x * 0.99, vel.y - this.getMeteorGravity(), vel.z * 0.99);
        setDeltaMovement(vel);

        if (!level().isClientSide) {
            if (this.tickCount > maxLife || !level().hasChunkAt(blockPosition())) {
                discard();
                return;
            }
        }

        // Pre-emptively smash through soft blocks in our full movement path for this tick.
        // This MUST run on both client and server, otherwise the client will predict a collision,
        // stop the meteor visually (losing velocity), and cause stuttering.
        if (!level().dimension().location().getPath().startsWith("dungeon_")) {
            net.minecraft.world.phys.AABB box = this.getBoundingBox().expandTowards(vel);
            for (BlockPos pos : BlockPos.betweenClosed(
                    net.minecraft.util.Mth.floor(box.minX), net.minecraft.util.Mth.floor(box.minY), net.minecraft.util.Mth.floor(box.minZ),
                    net.minecraft.util.Mth.floor(box.maxX), net.minecraft.util.Mth.floor(box.maxY), net.minecraft.util.Mth.floor(box.maxZ))) {
                net.minecraft.world.level.block.state.BlockState state = level().getBlockState(pos);
                if (!state.isAir() && state.getBlock().getExplosionResistance() < 3.0f) {
                    // Client only spawns particles, server actually breaks it and drops items.
                    // But setting it to air on the client prevents the move() collision engine from stopping the meteor.
                    level().destroyBlock(pos, !level().isClientSide, this);
                }
            }
        }

        move(MoverType.SELF, vel);

        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 6, 0.25, 0.25, 0.25, 0.02);
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 4, 0.35, 0.35, 0.35, 0.01);
            if (tickCount % 10 == 0) {
                server.playSound(
                        null,
                        getX(), getY(), getZ(),
                        SoundEvents.FIREWORK_ROCKET_BLAST,
                        SoundSource.AMBIENT,
                        0.6f,
                        0.6f + server.random.nextFloat() * 0.2f);
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
        net.minecraft.world.level.block.state.BlockState state = level().getBlockState(below);
        if (state.isAir()) return false;
        
        // If resistance is low (like dirt, leaves, wood), we consider it NOT solid enough to stop the meteor
        if (state.getBlock().getExplosionResistance() < 3.0f) {
            return false;
        }
        return true;
    }

    private void doImpact() {
        ServerLevel server = (ServerLevel) level();

        server.playSound(
                null,
                getX(), getY(), getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS,
                4.0f,
                0.9f + server.random.nextFloat() * 0.2f);
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        
        // Broadcast a custom entity event to all tracking clients.
        // This tells the clients to spawn the massive shockwave locally, bypassing server particle distance limits.
        level().broadcastEntityEvent(this, (byte) 100);

        if (this.destroysBlocks && !level().dimension().location().getPath().startsWith("dungeon_")) {
            net.ganyusbathwater.oririmod.entity.custom.CraterWorkerEntity crater = new net.ganyusbathwater.oririmod.entity.custom.CraterWorkerEntity(net.ganyusbathwater.oririmod.entity.ModEntities.CRATER_WORKER.get(), level());
            crater.setPos(getX(), getY(), getZ());
            int durationTicks = Math.max(10, Math.min(40, (int) explosionPower));
            crater.setCraterData((int) explosionPower, durationTicks); 
            level().addFreshEntity(crater);
        }
        
        // We ALWAYS trigger a vanilla blockless explosion to apply entity damage and knockback.
        // We scale down the power by 0.3x so it's not instantly lethal at huge radii.
        float damagePower = Math.max(4.0f, explosionPower * 0.3f);
        server.explode(this, getX(), getY(), getZ(), damagePower, ExplosionInteraction.NONE);
        igniteAround(server, this.blockPosition(), fireRadius);

        discard();
    }

    private void igniteAround(ServerLevel server, BlockPos center, int radius) {
        if (radius <= 0)
            return;
        int r = Math.max(0, radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r)
                    continue;
                BlockPos p = center.offset(dx, 0, dz);
                BlockPos ground = findGround(server, p, 6);
                if (ground == null)
                    continue;
                BlockPos firePos = ground.above();
                if (server.isEmptyBlock(firePos)) {
                    server.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                }
            }
        }
    }

    private BlockPos findGround(ServerLevel server, BlockPos start, int maxDrop) {
        BlockPos p = start;
        for (int i = 0; i < maxDrop; i++) {
            if (!server.isEmptyBlock(p))
                return p;
            p = p.below();
        }
        return null;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 100) {
            // Client-side execution of the shockwave
            double surfaceY = Math.max(getY(), impactPos != null ? impactPos.getY() : getY());
            
            float power = this.entityData.get(EXPLOSION_POWER);
            int particleCount = (int) (10 * power); // Scale count by power
            float speedScale = power / 15.0f; // Baseline 15 radius = 1x speed
            
            for (int i = 0; i < particleCount; i++) {
                double angle = this.random.nextDouble() * 2 * Math.PI;
                // Scale spread velocity with explosion power
                double speed = (1.0 + this.random.nextDouble() * 4.0) * speedScale; 
                double py = surfaceY + this.random.nextDouble() * (3.0 * speedScale);
                
                // Massive, lingering dark smoke core
                this.level().addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, getX(), py, getZ(), Math.cos(angle)*speed*0.8, 0.1, Math.sin(angle)*speed*0.8);
                // Faster, medium grey smoke
                this.level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), py, getZ(), Math.cos(angle)*speed, 0.2, Math.sin(angle)*speed);
                // Extremely fast, sharp white dust shooting out in front
                this.level().addParticle(ParticleTypes.POOF, getX(), py, getZ(), Math.cos(angle)*speed*1.4, 0.3, Math.sin(angle)*speed*1.4);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
}