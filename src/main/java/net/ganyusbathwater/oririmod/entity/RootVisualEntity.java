package net.ganyusbathwater.oririmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class RootVisualEntity extends Entity {
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(RootVisualEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFESPAN_TICKS = SynchedEntityData.defineId(RootVisualEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE_TICKS = SynchedEntityData.defineId(RootVisualEntity.class,
            EntityDataSerializers.INT);

    public RootVisualEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setTargetId(int id) {
        this.entityData.set(TARGET_ID, id);
    }

    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    public void setLifespan(int ticks) {
        this.entityData.set(LIFESPAN_TICKS, ticks);
    }

    public int getLifespan() {
        return this.entityData.get(LIFESPAN_TICKS);
    }

    public int getAgeTicks() {
        return this.entityData.get(AGE_TICKS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET_ID, -1);
        builder.define(LIFESPAN_TICKS, 60);
        builder.define(AGE_TICKS, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("TargetId")) {
            this.setTargetId(tag.getInt("TargetId"));
        }
        if (tag.contains("LifespanTicks")) {
            this.setLifespan(tag.getInt("LifespanTicks"));
        }
        if (tag.contains("AgeTicks")) {
            this.entityData.set(AGE_TICKS, tag.getInt("AgeTicks"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("TargetId", this.getTargetId());
        tag.putInt("LifespanTicks", this.getLifespan());
        tag.putInt("AgeTicks", this.getAgeTicks());
    }

    @Override
    public void tick() {
        super.tick();

        int currentAge = this.getAgeTicks();
        this.entityData.set(AGE_TICKS, currentAge + 1);

        if (!this.level().isClientSide) {
            if (currentAge >= this.getLifespan()) {
                this.discard();
                return;
            }

            int targetId = this.getTargetId();
            if (targetId >= 0 && this.level() instanceof ServerLevel serverLevel) {
                Entity target = serverLevel.getEntity(targetId);
                if (target != null && target.isAlive()) {
                    this.moveTo(target.getX(), target.getY(), target.getZ());
                } else {
                    // Target died or disappeared, stop tracking
                    this.setTargetId(-1);
                }
            }
        }
    }
}
