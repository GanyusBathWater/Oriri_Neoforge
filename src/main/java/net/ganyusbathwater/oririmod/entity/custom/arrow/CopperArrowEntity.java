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
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

public class CopperArrowEntity extends AbstractArrow implements net.minecraft.world.entity.projectile.ItemSupplier {
    public CopperArrowEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    public CopperArrowEntity(Level level, double x, double y, double z, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.COPPER_ARROW.get(), x, y, z, level, pickupItem, weapon);
    }

    public CopperArrowEntity(Level level, LivingEntity shooter, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.COPPER_ARROW.get(), shooter, level, pickupItem, weapon);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.inGround) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.WAX_ON, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.COPPER_ARROW.get());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.COPPER_ARROW.get());
    }

    protected boolean canSummonLightning() {
        return this.level().isThundering();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            if (this.canSummonLightning()) {
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(this.level());
                if (lightning != null) {
                    lightning.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(result.getLocation())));
                    this.level().addFreshEntity(lightning);
                }
            }
            this.discard();
        }
    }
}
