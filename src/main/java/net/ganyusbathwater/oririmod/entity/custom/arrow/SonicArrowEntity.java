package net.ganyusbathwater.oririmod.entity.custom.arrow;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class SonicArrowEntity extends AbstractArrow implements net.minecraft.world.entity.projectile.ItemSupplier {
    public SonicArrowEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    public SonicArrowEntity(Level level, double x, double y, double z, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.SONIC_ARROW.get(), x, y, z, level, pickupItem, weapon);
    }

    public SonicArrowEntity(Level level, LivingEntity shooter, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.SONIC_ARROW.get(), shooter, level, pickupItem, weapon);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.inGround) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SCULK_CHARGE_POP, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
        if (!this.level().isClientSide() && this.tickCount == 1) {
            // Speed up projectile immediately
            this.setDeltaMovement(this.getDeltaMovement().scale(5.0D));
            // Reduce base damage to exactly compensate for the 5x speed multiplier
            this.setBaseDamage(this.getBaseDamage() * 0.2D);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.SONIC_ARROW.get());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.SONIC_ARROW.get());
    }
}
