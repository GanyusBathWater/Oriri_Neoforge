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

    private BlockPos impactPos = BlockPos.ZERO;
    private float explosionPower = 4.0f;
    private int fireRadius = 3;
    private int maxLife = 20 * 10; // 10s Failsafe

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public void configure(BlockPos impactPos, float explosionPower, int fireRadius) {
        this.impactPos = impactPos.immutable();
        this.explosionPower = explosionPower;
        this.fireRadius = Math.max(0, fireRadius);
    }

    public void setOwnerId(int id) {
        this.entityData.set(OWNER_ID, id);
    }

    public int getOwnerId() {
        return this.entityData.get(OWNER_ID);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // 1.21+: Synched‑Daten über den Builder registrieren
        builder.define(OWNER_ID, 0);
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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("ImpactPos", this.impactPos.asLong());
        tag.putFloat("Power", this.explosionPower);
        tag.putInt("FireRadius", this.fireRadius);
        tag.putInt("MaxLife", this.maxLife);
        tag.putInt("OwnerId", this.getOwnerId());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (this.tickCount > maxLife || !level().hasChunkAt(blockPosition())) {
                discard();
                return;
            }
        }

        Vec3 vel = getDeltaMovement();
        vel = new Vec3(vel.x * 0.99, vel.y - 0.12, vel.z * 0.99);
        setDeltaMovement(vel);
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
        return !level().getBlockState(below).isAir();
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
        server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY(), getZ(), 30, 1.8, 0.3, 1.8, 0.02);

        server.explode(null, getX(), getY(), getZ(), explosionPower, ExplosionInteraction.TNT);
        igniteAround(server, this.blockPosition(), fireRadius);

        discard();
    }

    private void igniteAround(ServerLevel server, BlockPos center, int radius) {
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
}