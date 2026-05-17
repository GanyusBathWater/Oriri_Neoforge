package net.ganyusbathwater.oririmod.entity.custom.arrow;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.HitResult;

public class TntArrowEntity extends AbstractArrow implements net.minecraft.world.entity.projectile.ItemSupplier {
    public TntArrowEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    public TntArrowEntity(Level level, double x, double y, double z, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.TNT_ARROW.get(), x, y, z, level, pickupItem, weapon);
    }

    public TntArrowEntity(Level level, LivingEntity shooter, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.TNT_ARROW.get(), shooter, level, pickupItem, weapon);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.inGround) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.TNT_ARROW.get());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.TNT_ARROW.get());
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 2.5F, Level.ExplosionInteraction.TNT);
            this.discard();
        }
    }
}
