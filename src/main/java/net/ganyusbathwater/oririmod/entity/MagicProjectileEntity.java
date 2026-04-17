package net.ganyusbathwater.oririmod.entity;

import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.combat.IElementalEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class MagicProjectileEntity extends AbstractHurtingProjectile implements IElementalEntity {
    private static final EntityDataAccessor<Integer> DATA_ELEMENT = SynchedEntityData
            .defineId(MagicProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE = SynchedEntityData
            .defineId(MagicProjectileEntity.class, EntityDataSerializers.FLOAT);

    public MagicProjectileEntity(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    public MagicProjectileEntity(Level level, LivingEntity shooter, double xPower, double yPower, double zPower) {
        super(ModEntities.MAGIC_PROJECTILE.get(), shooter, new net.minecraft.world.phys.Vec3(xPower, yPower, zPower), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ELEMENT, Element.FIRE.ordinal());
        builder.define(DATA_DAMAGE, 4.0F);
    }

    public void setElement(Element element) {
        this.entityData.set(DATA_ELEMENT, element.ordinal());
    }

    @Override
    public Element getElement() {
        return Element.values()[this.entityData.get(DATA_ELEMENT)];
    }

    public void setDamage(float damage) {
        this.entityData.set(DATA_DAMAGE, damage);
    }

    public float getDamage() {
        return this.entityData.get(DATA_DAMAGE);
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            net.minecraft.world.entity.Entity target = result.getEntity();
            net.minecraft.world.entity.Entity owner = this.getOwner();
            
            // Damage is handled by ElementalDamageHandler based on the element of this direct entity
            target.hurt(this.damageSources().thrown(this, owner), this.getDamage());
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            this.discard();
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
}
