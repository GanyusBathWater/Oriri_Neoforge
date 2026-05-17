package net.ganyusbathwater.oririmod.entity.custom.arrow;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.EntityHitResult;
import net.ganyusbathwater.oririmod.damage.ModDamageTypes;

public class DragonIronArrowEntity extends AbstractArrow implements net.minecraft.world.entity.projectile.ItemSupplier {
    public DragonIronArrowEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    public DragonIronArrowEntity(Level level, double x, double y, double z, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.DRAGON_IRON_ARROW.get(), x, y, z, level, pickupItem, weapon);
    }

    public DragonIronArrowEntity(Level level, LivingEntity shooter, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.DRAGON_IRON_ARROW.get(), shooter, level, pickupItem, weapon);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.inGround) {
            this.level().addParticle(net.ganyusbathwater.oririmod.particle.ModParticles.SCARLET_CAVE_PARTICLE.get(), this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.DRAGON_IRON_ARROW.get());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.DRAGON_IRON_ARROW.get());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide() && result.getEntity() instanceof LivingEntity living) {
            living.hurt(ModDamageTypes.getTrueDamage(this.level(), this.getOwner()), (float)this.getBaseDamage() + 6.0F);
        }
        super.onHitEntity(result);
    }
}
