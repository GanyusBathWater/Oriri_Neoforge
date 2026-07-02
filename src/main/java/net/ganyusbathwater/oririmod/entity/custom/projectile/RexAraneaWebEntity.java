package net.ganyusbathwater.oririmod.entity.custom.projectile;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class RexAraneaWebEntity extends AbstractHurtingProjectile implements ItemSupplier {

    public RexAraneaWebEntity(EntityType<? extends AbstractHurtingProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public RexAraneaWebEntity(Level level, LivingEntity shooter, double xPower, double yPower, double zPower) {
        super(ModEntities.REX_ARANEA_WEB.get(), shooter, new net.minecraft.world.phys.Vec3(xPower, yPower, zPower), level);
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.WHITE_ASH; // Occasional white falling particles
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            net.minecraft.world.entity.Entity target = result.getEntity();
            net.minecraft.world.entity.Entity owner = this.getOwner();
            
            // Deal 2 hearts (4 damage)
            target.hurt(this.damageSources().thrown(this, owner), 4.0F);

            this.spawnCobweb(this.blockPosition());
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            this.spawnCobweb(result.getBlockPos().relative(result.getDirection()));
            this.discard();
        }
    }

    private void spawnCobweb(BlockPos pos) {
        if (this.level().isEmptyBlock(pos) || this.level().getBlockState(pos).canBeReplaced()) {
            this.level().setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState());
        }
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.COBWEB);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected float getInertia() {
        return 1.0F;
    }
}
