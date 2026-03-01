package net.ganyusbathwater.oririmod.entity;

import net.ganyusbathwater.oririmod.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class DoomClockEntity extends Entity {

    private static final EntityDataAccessor<Integer> TICK_COUNT_SYNC = SynchedEntityData.defineId(DoomClockEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID_SYNC = SynchedEntityData.defineId(DoomClockEntity.class,
            EntityDataSerializers.INT);
    public static final int MAX_TICKS = 240; // 12 seconds

    public DoomClockEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TICK_COUNT_SYNC, 0);
        builder.define(OWNER_ID_SYNC, -1);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            int ticks = this.getEntityData().get(TICK_COUNT_SYNC);

            if ((ticks + 5) % 20 == 0 && ticks < MAX_TICKS) {
                // Play ticking sound every second, but 5 ticks earlier
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.DOOM_CLOCK_TICK.get(), SoundSource.HOSTILE, 1.5f, 1.0f);
            }

            if (ticks == MAX_TICKS - 5) {
                // Play gong 5 ticks earlier
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.DOOM_CLOCK_GONG.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
            }

            if (ticks >= MAX_TICKS) {
                // Insta-death logic
                double radius = 10.0;
                AABB area = new AABB(this.getX() - radius, this.getY() - radius, this.getZ() - radius,
                        this.getX() + radius, this.getY() + radius, this.getZ() + radius);

                int ownerId = this.getEntityData().get(OWNER_ID_SYNC);
                this.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area).forEach(living -> {
                    if (living.getId() != ownerId) {
                        living.hurt(this.level().damageSources().generic(), Float.MAX_VALUE);
                        // Optional: trigger some death particles on the entity
                    }
                });

                this.discard();
                return;
            }

            this.getEntityData().set(TICK_COUNT_SYNC, ticks + 1);
        } else {
            // Client side logic, maybe spawn a few static particles
            if (this.random.nextFloat() < 0.1f) {
                this.level().addParticle(ParticleTypes.ENCHANT,
                        this.getX() + (this.random.nextDouble() - 0.5) * 4.0,
                        this.getY() + (this.random.nextDouble() - 0.5) * 4.0,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 4.0,
                        0, 0, 0);
            }
        }
    }

    public int getClientTickCount() {
        return this.getEntityData().get(TICK_COUNT_SYNC);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.getEntityData().set(TICK_COUNT_SYNC, compound.getInt("DoomTicks"));
        this.getEntityData().set(OWNER_ID_SYNC, compound.getInt("OwnerId"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("DoomTicks", this.getEntityData().get(TICK_COUNT_SYNC));
        compound.putInt("OwnerId", this.getEntityData().get(OWNER_ID_SYNC));
    }

    public void setOwnerId(int id) {
        this.getEntityData().set(OWNER_ID_SYNC, id);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }
}
