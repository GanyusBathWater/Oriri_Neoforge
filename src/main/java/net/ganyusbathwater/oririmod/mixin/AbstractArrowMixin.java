package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.util.ISniperArrow;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin extends Projectile implements ISniperArrow {

    @Unique
    private static final EntityDataAccessor<Float> SNIPER_SPEED = SynchedEntityData.defineId(AbstractArrow.class,
            EntityDataSerializers.FLOAT);

    protected AbstractArrowMixin(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void defineSniperData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(SNIPER_SPEED, 0.0F);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void addSniperSaveData(CompoundTag tag, CallbackInfo ci) {
        tag.putFloat("SniperSpeed", this.entityData.get(SNIPER_SPEED));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readSniperSaveData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("SniperSpeed")) {
            this.entityData.set(SNIPER_SPEED, tag.getFloat("SniperSpeed"));
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void sniperTick(CallbackInfo ci) {
        float speed = this.entityData.get(SNIPER_SPEED);
        if (speed > 0) {
            this.setNoGravity(true);
            Vec3 current = this.getDeltaMovement();
            this.setDeltaMovement(current.normalize().scale(speed));
        }
    }

    @Override
    public void oririmod$setSniperSpeed(float speed) {
        this.entityData.set(SNIPER_SPEED, speed);
    }

    @Override
    public float oririmod$getSniperSpeed() {
        return this.entityData.get(SNIPER_SPEED);
    }
}
