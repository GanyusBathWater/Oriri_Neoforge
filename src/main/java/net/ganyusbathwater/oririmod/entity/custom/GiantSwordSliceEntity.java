package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.damage.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GiantSwordSliceEntity extends Entity implements GeoEntity {

    public int initialDelay = 0;
    public int chargeDelay = 40; // 2 seconds
    public float sliceSpeed = 2.5f; // Blocks per tick (Increased from 2.0)
    public float damage = 20.0f; // 10 hearts
    public UUID ownerId;
    
    private int lifeTicks = 0;
    private final Set<Integer> hitEntities = new HashSet<>();
    private boolean clientIndicatorAdded = false;
    
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE = SynchedEntityData.defineId(GiantSwordSliceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SLICING = SynchedEntityData.defineId(GiantSwordSliceEntity.class, EntityDataSerializers.BOOLEAN);

    public GiantSwordSliceEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true; // Bypasses standard block collision
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ACTIVE, false);
        builder.define(DATA_SLICING, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        initialDelay = tag.getInt("InitialDelay");
        chargeDelay = tag.getInt("ChargeDelay");
        sliceSpeed = tag.getFloat("SliceSpeed");
        damage = tag.getFloat("Damage");
        lifeTicks = tag.getInt("LifeTicks");
        if (tag.hasUUID("OwnerId")) {
            ownerId = tag.getUUID("OwnerId");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("InitialDelay", initialDelay);
        tag.putInt("ChargeDelay", chargeDelay);
        tag.putFloat("SliceSpeed", sliceSpeed);
        tag.putFloat("Damage", damage);
        tag.putInt("LifeTicks", lifeTicks);
        if (ownerId != null) {
            tag.putUUID("OwnerId", ownerId);
        }
    }

    public boolean isActive() {
        return this.entityData.get(DATA_ACTIVE);
    }
    
    public boolean isSlicing() {
        return this.entityData.get(DATA_SLICING);
    }

    @Override
    public void tick() {
        super.tick();
        
        lifeTicks++;
        
        if (this.level().isClientSide) {
            if (isActive() && !isSlicing() && !clientIndicatorAdded) {
                clientIndicatorAdded = true;
                net.minecraft.world.phys.Vec3 start = this.position();
                net.minecraft.world.phys.Vec3 forward = net.minecraft.world.phys.Vec3.directionFromRotation(0, this.getYRot());
                // The sword travels at sliceSpeed (2.0) for ~60 ticks, so 120 blocks length
                net.minecraft.world.phys.Vec3 end = start.add(forward.scale(120.0));
                
                // Add a red line indicator representing the path of the slice
                // Width reduced to 0.5 to match the newly tightened 1-block hitbox
                net.ganyusbathwater.oririmod.client.render.AoEIndicatorClientState.addLineIndicator(
                        this.getUUID(), start, end, 0.5f, chargeDelay, 0x88FF0000
                );
            }
        }
        
        if (!this.level().isClientSide) {
            if (lifeTicks >= initialDelay && !isActive()) {
                this.entityData.set(DATA_ACTIVE, true);
                this.playSound(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 2.0f, 0.5f);
            }
            
            if (lifeTicks >= initialDelay + chargeDelay && !isSlicing()) {
                this.entityData.set(DATA_SLICING, true);
                this.playSound(net.minecraft.sounds.SoundEvents.WITHER_SHOOT, 1.5f, 1.5f);
            }
            
            if (isSlicing()) {
                Vec3 movement = this.getDeltaMovement();
                
                // Sweep collision
                AABB currentBox = this.getBoundingBox();
                // Removed the massive inflate(1.0) to keep the hitbox strictly to the sword's 1-block width.
                // We leave a tiny 0.1 inflation just to prevent clipping bugs at high speeds.
                AABB sweepBox = currentBox.expandTowards(movement).inflate(0.1);
                
                List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, sweepBox, e -> e.isAlive() && (ownerId == null || !e.getUUID().equals(ownerId)));
                for (LivingEntity t : targets) {
                    if (!hitEntities.contains(t.getId())) {
                        t.hurt(ModDamageTypes.getTrueDamage(this.level(), getOwner()), damage);
                        hitEntities.add(t.getId());
                    }
                }
                
                this.move(net.minecraft.world.entity.MoverType.SELF, movement);
                
                if (this.level() instanceof ServerLevel sl) {
                    // Reduced from 5 particles with large spread down to 1 particle with tiny spread
                    // Vanilla End Rod particles have hardcoded lifespans, so reducing density is the only way 
                    // to prevent them from blinding the player without creating a custom particle.
                    sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
                    
                    // Rip up the ground as it moves by scanning downwards to find the actual floor
                    net.minecraft.world.level.block.state.BlockState groundState = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                    double groundY = this.getY();
                    for (int yOffset = 0; yOffset <= 3; yOffset++) {
                        net.minecraft.core.BlockPos checkPos = net.minecraft.core.BlockPos.containing(this.getX(), this.getY() - yOffset - 0.1, this.getZ());
                        net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(checkPos);
                        if (!state.isAir() && state.getFluidState().isEmpty()) {
                            groundState = state;
                            groundY = checkPos.getY() + 1.0;
                            break;
                        }
                    }
                    
                    if (!groundState.isAir()) {
                        // Increased from 5 to 15 particles, slightly higher speed to ensure they fly up visibly
                        sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, groundState), 
                            this.getX(), groundY, this.getZ(), 15, 0.3, 0.2, 0.3, 0.2);
                    }
                }
                
                // Discard after moving for ~60 ticks (way past arena)
                if (lifeTicks > initialDelay + chargeDelay + 60) {
                    this.discard();
                }
            }
        }
    }

    private Entity getOwner() {
        if (ownerId != null && this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(ownerId);
        }
        return this;
    }

    public void setDirection(Vec3 direction) {
        Vec3 normalized = direction.normalize();
        this.setDeltaMovement(normalized.scale(sliceSpeed));
        
        // Calculate rotation based on direction
        double d0 = normalized.x;
        double d1 = normalized.z;
        this.setYRot((float)(Math.atan2(d1, d0) * (180F / Math.PI)) - 90.0F);
        this.yRotO = this.getYRot();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "sword_slice_controller", 0, state -> {
            if (isActive()) {
                // Play the spawning animation and hold its final frame
                return state.setAndContinue(RawAnimation.begin().thenPlayAndHold("giant_blade_spawning"));
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    @Override
    public boolean isPickable() {
        return false;
    }
}
