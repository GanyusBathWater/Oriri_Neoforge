package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class AetherChargeEntity extends AbstractHurtingProjectile implements ItemSupplier {
    
    public AetherChargeEntity(EntityType<? extends AbstractHurtingProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public AetherChargeEntity(Level level, LivingEntity shooter, double xPower, double yPower, double zPower) {
        super(ModEntities.AETHER_CHARGE_ENTITY.get(), shooter, new net.minecraft.world.phys.Vec3(xPower, yPower, zPower), level);
    }

    public AetherChargeEntity(Level level, double x, double y, double z, double xPower, double yPower, double zPower) {
        super(ModEntities.AETHER_CHARGE_ENTITY.get(), x, y, z, new net.minecraft.world.phys.Vec3(xPower, yPower, zPower), level);
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SOUL_FIRE_FLAME; // Using soul fire flame for blue fire visual
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            net.minecraft.world.entity.Entity target = result.getEntity();
            net.minecraft.world.entity.Entity owner = this.getOwner();

            // Direct damage before explosion
            target.hurt(this.damageSources().mobProjectile(this, owner instanceof net.minecraft.world.entity.LivingEntity ? (net.minecraft.world.entity.LivingEntity) owner : null), 6.0F);

            this.explode();
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            this.explode();
            this.discard();
        }
    }

    private void explode() {
        // Explodes like a Ghast fireball but slightly stronger
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), 2.0F, false, Level.ExplosionInteraction.BLOCK);

        // Spawn Aether Fire in a small radius around the impact
        float fireRadius = 1.5f;
        int maxRadius = (int) Math.ceil(fireRadius);
        BlockPos center = this.blockPosition();

        for (int x = -maxRadius; x <= maxRadius; ++x) {
            for (int y = -maxRadius; y <= maxRadius; ++y) {
                for (int z = -maxRadius; z <= maxRadius; ++z) {
                    if (x * x + y * y + z * z <= fireRadius * fireRadius) {
                        BlockPos targetPos = center.offset(x, y, z);
                        if (this.level().isEmptyBlock(targetPos) && !this.level().isEmptyBlock(targetPos.below())) {
                            if (this.random.nextFloat() < 0.6F) {
                                this.level().setBlockAndUpdate(targetPos, ModBlocks.AETHER_FIRE_BLOCK.get().defaultBlockState());
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.AETHER_CHARGE.get());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected float getInertia() {
        return 1.0F; // Maintains speed
    }
}
