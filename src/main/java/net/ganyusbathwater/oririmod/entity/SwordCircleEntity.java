package net.ganyusbathwater.oririmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.UUID;

public class SwordCircleEntity extends Entity {
    
    private static final EntityDataAccessor<Optional<UUID>> TARGET_UUID = SynchedEntityData.defineId(SwordCircleEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public SwordCircleEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET_UUID, Optional.empty());
    }

    public void setTargetId(UUID id) {
        this.entityData.set(TARGET_UUID, Optional.ofNullable(id));
    }

    public Optional<UUID> getTargetId() {
        return this.entityData.get(TARGET_UUID);
    }

    @Override
    public void tick() {
        super.tick();

        Entity target = null;
        Optional<UUID> targetOpt = getTargetId();
        if (targetOpt.isPresent()) {
            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                target = serverLevel.getEntity(targetOpt.get());
            } else if (this.level().isClientSide) {
                target = this.level().getPlayerByUUID(targetOpt.get());
                if (target == null) {
                    for (Entity e : this.level().getEntities(null, this.getBoundingBox().inflate(128.0D))) {
                        if (e.getUUID().equals(targetOpt.get())) {
                            target = e;
                            break;
                        }
                    }
                }
            }
        } else {
            target = this.level().getNearestPlayer(this, 64.0D);
        }

        // Tracking: Teleport directly above target (9 blocks high)
        if (target != null) {
            this.setPos(target.getX(), target.getY() + 9.0, target.getZ());
        } else if (!this.level().isClientSide) {
            this.discard();
            return;
        }

        // Client dynamic indicator rendering
        if (this.level().isClientSide && this.tickCount < 50 && target != null) {
            BlockPos groundPos = findGroundBelow(target.blockPosition());
            if (groundPos != null) {
                net.ganyusbathwater.oririmod.client.render.AoEIndicatorClientState.addCircleIndicator(
                        this.getUUID(), groundPos, 5.0f, 60, 0xFFFF0000); 
            }
        }

        if (!this.level().isClientSide) {
            if (this.tickCount >= 50) { // 40 ticks orbit + 10 ticks tell phase
                if (target != null) {
                    // Strike Phase: Spawn 8 real SwordProjectileEntities
                    int numSwords = 8;
                    float radius = 4.0f; // Expanded radius during tell phase
                    
                    // We need to aim at the target's center (usually Y + target.getEyeHeight() / 2)
                    Vec3 aimTarget = new Vec3(target.getX(), target.getY() + (target.getBbHeight() / 2.0f), target.getZ());
                    
                    for (int i = 0; i < numSwords; i++) {
                        double angle = 2 * Math.PI * i / numSwords;
                        double spawnX = this.getX() + radius * Math.cos(angle);
                        double spawnZ = this.getZ() + radius * Math.sin(angle);
                        double spawnY = this.getY();

                        SwordProjectileEntity sword = new SwordProjectileEntity(this.level(), spawnX, spawnY, spawnZ);
                        
                        Vec3 spawnPos = new Vec3(spawnX, spawnY, spawnZ);
                        Vec3 shootVec = aimTarget.subtract(spawnPos).normalize().scale(1.5D); // Speed
                        
                        // Pre-calculate rotations so they don't visibly snap/whip on the client side
                        double horizontalDistance = Math.sqrt(shootVec.x * shootVec.x + shootVec.z * shootVec.z);
                        float targetYRot = (float) (Math.atan2(shootVec.x, shootVec.z) * (180F / Math.PI));
                        float targetXRot = (float) (Math.atan2(shootVec.y, horizontalDistance) * (180F / Math.PI));
                        sword.setYRot(targetYRot);
                        sword.setXRot(targetXRot);
                        sword.yRotO = targetYRot;
                        sword.xRotO = targetXRot;

                        sword.fireInstantly(shootVec, 15.0f);
                        if (targetOpt.isPresent()) {
                            sword.setTargetId(targetOpt.get());
                        }
                        
                        this.level().addFreshEntity(sword);
                    }
                }
                this.discard();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("TargetUUID")) {
            this.setTargetId(tag.getUUID("TargetUUID"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        getTargetId().ifPresent(uuid -> tag.putUUID("TargetUUID", uuid));
    }

    private BlockPos findGroundBelow(BlockPos pos) {
        BlockPos current = pos;
        while (current.getY() > this.level().getMinBuildHeight()) {
            if (!this.level().isEmptyBlock(current.below())) {
                return current.below();
            }
            current = current.below();
        }
        return null;
    }
}
