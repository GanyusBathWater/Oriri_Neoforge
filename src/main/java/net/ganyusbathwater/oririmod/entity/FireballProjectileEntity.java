package net.ganyusbathwater.oririmod.entity;

import net.ganyusbathwater.oririmod.util.MagicBoltAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class FireballProjectileEntity extends AbstractHurtingProjectile implements ItemSupplier {
    private static final EntityDataAccessor<Float> DATA_SCALE = SynchedEntityData
            .defineId(FireballProjectileEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> DATA_EXPLOSION_RADIUS = SynchedEntityData
            .defineId(FireballProjectileEntity.class, EntityDataSerializers.FLOAT);

    public FireballProjectileEntity(EntityType<? extends AbstractHurtingProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public FireballProjectileEntity(Level level, LivingEntity shooter, double xPower, double yPower, double zPower) {
        super(ModEntities.FIREBALL_PROJECTILE.get(), shooter, new net.minecraft.world.phys.Vec3(xPower, yPower, zPower),
                level);
    }

    public FireballProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.FIREBALL_PROJECTILE.get(), level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SCALE, 1.0F);
        builder.define(DATA_EXPLOSION_RADIUS, 2.0F);
    }

    public void setScale(float scale) {
        this.entityData.set(DATA_SCALE, scale);
    }

    public float getScale() {
        return this.entityData.get(DATA_SCALE);
    }

    public void setExplosionRadius(float radius) {
        this.entityData.set(DATA_EXPLOSION_RADIUS, radius);
    }

    public float getExplosionRadius() {
        return this.entityData.get(DATA_EXPLOSION_RADIUS);
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.FLAME;
    }

    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            this.explode();
            this.discard();
        }
    }

    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            this.explode();
            this.discard();
        }
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.FIRE_CHARGE);
    }

    private void explode() {
        float explosionRadius = this.getExplosionRadius();

        // Crate explosion that damages entities but doesn't break blocks
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), explosionRadius, false,
                Level.ExplosionInteraction.NONE);

        // Spawn fire in 1/4 radius
        float fireRadius = explosionRadius * 0.25f;
        int maxRadius = (int) Math.ceil(fireRadius);
        BlockPos center = this.blockPosition();

        for (int x = -maxRadius; x <= maxRadius; ++x) {
            for (int y = -maxRadius; y <= maxRadius; ++y) {
                for (int z = -maxRadius; z <= maxRadius; ++z) {
                    if (x * x + y * y + z * z <= fireRadius * fireRadius) {
                        BlockPos targetPos = center.offset(x, y, z);
                        if (this.level().isEmptyBlock(targetPos) && !this.level().isEmptyBlock(targetPos.below())) {
                            if (this.random.nextFloat() < 0.6F) { // Add a bit of randomness so it's not a perfect
                                                                  // blanket of fire
                                this.level().setBlockAndUpdate(targetPos,
                                        BaseFireBlock.getState(this.level(), targetPos));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected float getInertia() {
        return 1.0F;
    }

    public void configureForGrade(MagicBoltAbility ability) {
        switch (ability) {
            case AMATEUR_FIREBALL -> {
                this.setScale(0.5F);
                this.setExplosionRadius(2.0F);
            }
            case APPRENTICE_FIREBALL -> {
                this.setScale(0.75F);
                this.setExplosionRadius(5.0F);
            }
            case JOURNEYMAN_FIREBALL -> {
                this.setScale(1.0F);
                this.setExplosionRadius(8.0F);
            }
            case WISE_FIREBALL -> {
                this.setScale(1.25F);
                this.setExplosionRadius(11.0F);
            }
        }
    }

    public void launchStraight(LivingEntity shooter, float speed) {
        net.minecraft.world.phys.Vec3 view = shooter.getViewVector(1.0f);
        this.shoot(view.x, view.y, view.z, speed, 0.0f);
    }
}
