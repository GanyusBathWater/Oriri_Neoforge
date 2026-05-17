package net.ganyusbathwater.oririmod.entity.custom.arrow;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class EventHorizonArrowEntity extends AbstractArrow implements net.minecraft.world.entity.projectile.ItemSupplier {
    public EventHorizonArrowEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    public EventHorizonArrowEntity(Level level, double x, double y, double z, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.EVENT_HORIZON_ARROW.get(), x, y, z, level, pickupItem, weapon);
    }

    public EventHorizonArrowEntity(Level level, LivingEntity shooter, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.EVENT_HORIZON_ARROW.get(), shooter, level, pickupItem, weapon);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.inGround) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.EVENT_HORIZON_ARROW.get());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.EVENT_HORIZON_ARROW.get());
    }

    @Override
    protected void onHit(net.minecraft.world.phys.HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            net.ganyusbathwater.oririmod.entity.custom.BlackHoleEntity blackHole = new net.ganyusbathwater.oririmod.entity.custom.BlackHoleEntity(this.level(), this.getX(), this.getY(), this.getZ(), 100, 5.0f, true);
            this.level().addFreshEntity(blackHole);
            this.discard();
        }
    }
}
