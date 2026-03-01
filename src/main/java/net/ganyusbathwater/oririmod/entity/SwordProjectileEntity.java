package net.ganyusbathwater.oririmod.entity;

import net.ganyusbathwater.oririmod.network.packet.SpawnAoEIndicatorPacket;
import net.ganyusbathwater.oririmod.network.packet.SpawnAoEIndicatorPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class SwordProjectileEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Boolean> HAS_LOCKED = SynchedEntityData
            .defineId(SwordProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> RANDOM_ROLL = SynchedEntityData
            .defineId(SwordProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_DEAD = SynchedEntityData
            .defineId(SwordProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID targetId;
    private int deadTimer = 0;

    public SwordProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public SwordProjectileEntity(Level level, LivingEntity owner) {
        super(ModEntities.SWORD_PROJECTILE.get(), owner, level);
        this.setItem(new ItemStack(Items.NETHERITE_SWORD));
        this.setNoGravity(true);
        if (!level.isClientSide) {
            this.entityData.set(RANDOM_ROLL, level.random.nextFloat() * 360.0F);
        }
    }

    public SwordProjectileEntity(Level level, double x, double y, double z) {
        super(ModEntities.SWORD_PROJECTILE.get(), x, y, z, level);
        this.setItem(new ItemStack(Items.NETHERITE_SWORD));
        this.setNoGravity(true);
        if (!level.isClientSide) {
            this.entityData.set(RANDOM_ROLL, level.random.nextFloat() * 360.0F);
        }
    }

    @Override
    protected Item getDefaultItem() {
        return Items.NETHERITE_SWORD;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_LOCKED, false);
        builder.define(RANDOM_ROLL, 0.0F);
        builder.define(IS_DEAD, false);
    }

    public float getRandomRoll() {
        return this.entityData.get(RANDOM_ROLL);
    }

    public boolean isDeadState() {
        return this.entityData.get(IS_DEAD);
    }

    public void initializeRotationDown() {
        // Will rely on the renderer reading tick data instead of forcing protected
        // fields
    }

    public void setTargetId(UUID id) {
        this.targetId = id;
    }

    @Override
    protected void updateRotation() {
        if (this.entityData.get(IS_DEAD) || !this.entityData.get(HAS_LOCKED)) {
            return;
        }
        super.updateRotation();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.entityData.get(IS_DEAD)) {
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
            if (!this.level().isClientSide) {
                this.deadTimer++;
                if (this.deadTimer > 60) { // Stay in ground for 3 seconds
                    this.discard();
                }
            }
            return; // Completely freeze rotation and movement logic
        }

        if (!this.entityData.get(HAS_LOCKED)) {
            // Hovering phase: JUST POINT DOWN. No spinning, no tweaking.
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            // Flying phase: Calculate rotation naturally from deltaMovement exactly like an
            // arrow
            Vec3 shootVec = this.getDeltaMovement();
            if (shootVec.lengthSqr() > 0.001D) {
                double horizontalDistance = Math.sqrt(shootVec.x * shootVec.x + shootVec.z * shootVec.z);
                this.setYRot((float) (Math.atan2(shootVec.x, shootVec.z) * (double) (180F / (float) Math.PI)));
                this.setXRot(
                        (float) (Math.atan2(shootVec.y, horizontalDistance) * (double) (180F / (float) Math.PI)));
            }

            if (this.level().isClientSide) {
                // Client-side flight particles
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT, this.getX(),
                        this.getY(), this.getZ(), 0, 0, 0);
            }
        }

        // Server only logic
        if (!this.level().isClientSide && !this.entityData.get(HAS_LOCKED)) {
            if (this.tickCount >= 40) {
                // Lock onto target
                LivingEntity target = null;
                if (this.targetId != null && this.level() instanceof ServerLevel serverLevel) {
                    Entity e = serverLevel.getEntity(this.targetId);
                    if (e instanceof LivingEntity le) {
                        target = le;
                    }
                }

                if (target == null) {
                    target = this.level().getNearestPlayer(this, 64.0D);
                }

                if (target != null) {
                    BlockPos groundPos = findGroundBelow(target.blockPosition());

                    if (groundPos != null) {
                        PacketDistributor.sendToAllPlayers(new SpawnAoEIndicatorPayload(
                                new SpawnAoEIndicatorPacket(groundPos, 1.5f, 20, 0xFFFF0000)));

                        Vec3 targetVec = new Vec3(groundPos.getX() + 0.5, groundPos.getY() + 1.0,
                                groundPos.getZ() + 0.5);
                        Vec3 shootVec = targetVec.subtract(this.position()).normalize().scale(1.5D); // Speed
                        this.setDeltaMovement(shootVec);
                        this.entityData.set(HAS_LOCKED, true);
                    } else {
                        this.discard();
                    }
                } else {
                    this.discard();
                }
            }
        }

        if (!this.level().isClientSide && this.tickCount > 200) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.entityData.get(IS_DEAD)) {
            if (!this.level().isClientSide) {
                result.getEntity().hurt(this.damageSources().magic(), 10.0F); // 10 magic damage
                this.discard(); // Despawn instantly upon hitting an entity
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.entityData.get(IS_DEAD)) {
            this.level().playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_LAND,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
            this.entityData.set(IS_DEAD, true);
        }
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

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HasLocked", this.entityData.get(HAS_LOCKED));
        tag.putBoolean("IsDead", this.entityData.get(IS_DEAD));
        tag.putFloat("RandomRoll", this.entityData.get(RANDOM_ROLL));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(HAS_LOCKED, tag.getBoolean("HasLocked"));
        this.entityData.set(IS_DEAD, tag.getBoolean("IsDead"));
        this.entityData.set(RANDOM_ROLL, tag.getFloat("RandomRoll"));
    }
}
