package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.client.render.AoEIndicatorClientState;
import net.ganyusbathwater.oririmod.damage.ModDamageTypes;
import net.minecraft.core.BlockPos;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

public class GiantSwordEntity extends Entity implements GeoEntity {
    
    // Configurable state properties
    public int delayTicks = 60; // 3 seconds
    public float fallSpeed = -2.5f;
    public float impactDamage = 20.0f; // 10 hearts
    public float explosionDamage = 10.0f; // 5 hearts
    public float impactRadius = 7.0f;
    public float explosionRadius = 5.0f;
    public int embeddedTicks = 100; // 5 seconds
    public UUID ownerId;
    
    // Internal state
    private int lifeTicks = 0;
    private boolean impacted = false;
    private int ticksSinceImpact = 0;
    
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    
    // Sync state for animation
    private static final EntityDataAccessor<Boolean> DATA_IMPACTED = SynchedEntityData.defineId(GiantSwordEntity.class, EntityDataSerializers.BOOLEAN);

    public int clientImpactTicks = 0;
    private java.util.Map<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState> replacedBlocks = new java.util.HashMap<>();

    public GiantSwordEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_IMPACTED, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        delayTicks = compoundTag.getInt("DelayTicks");
        fallSpeed = compoundTag.getFloat("FallSpeed");
        impactDamage = compoundTag.getFloat("ImpactDamage");
        explosionDamage = compoundTag.getFloat("ExplosionDamage");
        impactRadius = compoundTag.getFloat("ImpactRadius");
        explosionRadius = compoundTag.getFloat("ExplosionRadius");
        embeddedTicks = compoundTag.getInt("EmbeddedTicks");
        if (compoundTag.hasUUID("OwnerId")) {
            ownerId = compoundTag.getUUID("OwnerId");
        }
        lifeTicks = compoundTag.getInt("LifeTicks");
        if (compoundTag.contains("TicksSinceImpact")) {
            ticksSinceImpact = compoundTag.getInt("TicksSinceImpact");
        }
        if (compoundTag.contains("ReplacedBlocks")) {
            net.minecraft.nbt.ListTag blocksTag = compoundTag.getList("ReplacedBlocks", 10);
            for (int i = 0; i < blocksTag.size(); i++) {
                net.minecraft.nbt.CompoundTag blockTag = blocksTag.getCompound(i);
                BlockPos pos = BlockPos.of(blockTag.getLong("Pos"));
                net.minecraft.world.level.block.state.BlockState state = net.minecraft.nbt.NbtUtils.readBlockState(this.level().holderLookup(net.minecraft.core.registries.Registries.BLOCK), blockTag.getCompound("State"));
                replacedBlocks.put(pos, state);
            }
        }
        impacted = compoundTag.getBoolean("Impacted");
        entityData.set(DATA_IMPACTED, impacted);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putInt("DelayTicks", delayTicks);
        compoundTag.putFloat("FallSpeed", fallSpeed);
        compoundTag.putFloat("ImpactDamage", impactDamage);
        compoundTag.putFloat("ExplosionDamage", explosionDamage);
        compoundTag.putFloat("ImpactRadius", impactRadius);
        compoundTag.putFloat("ExplosionRadius", explosionRadius);
        compoundTag.putInt("EmbeddedTicks", embeddedTicks);
        
        net.minecraft.nbt.ListTag blocksTag = new net.minecraft.nbt.ListTag();
        for (java.util.Map.Entry<BlockPos, net.minecraft.world.level.block.state.BlockState> entry : replacedBlocks.entrySet()) {
            net.minecraft.nbt.CompoundTag blockTag = new net.minecraft.nbt.CompoundTag();
            blockTag.putLong("Pos", entry.getKey().asLong());
            blockTag.put("State", net.minecraft.nbt.NbtUtils.writeBlockState(entry.getValue()));
            blocksTag.add(blockTag);
        }
        compoundTag.put("ReplacedBlocks", blocksTag);

        if (ownerId != null) {
            compoundTag.putUUID("OwnerId", ownerId);
        }
        compoundTag.putInt("LifeTicks", lifeTicks);
        compoundTag.putBoolean("Impacted", impacted);
        compoundTag.putInt("TicksSinceImpact", ticksSinceImpact);
    }

    public boolean isImpacted() {
        return this.entityData.get(DATA_IMPACTED);
    }

    @Override
    public void tick() {
        super.tick();
        
        if (this.level().isClientSide) {
            if (isImpacted()) {
                clientImpactTicks++;
            }
            if (!this.entityData.get(DATA_IMPACTED)) {
                if (this.getDeltaMovement().y < -0.1) {
                    // Falling phase: spawn CLOUD particles at anchors (scaled by 1.5x)
                    this.level().addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ() - 0.9375, 0, 0.1, 0);
                    this.level().addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ() + 0.9375, 0, 0.1, 0);
                    this.level().addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, this.getX(), this.getY() + 4.21875, this.getZ() - 1.5, 0, 0.1, 0);
                    this.level().addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, this.getX(), this.getY() + 4.21875, this.getZ() + 1.5, 0, 0.1, 0);
                } else {
                    // We show an indicator on the ground where it will fall
                    BlockPos groundPos = getGroundPos();
                    if (groundPos != null) {
                        AoEIndicatorClientState.addCircleIndicator(this.getUUID(), groundPos, impactRadius, delayTicks, 0x88FF0000);
                    }
                }
            } else {
                // If impacted, show explosion indicator for the remaining time
                int ticksLeft = embeddedTicks - ticksSinceImpact;
                if (ticksLeft > 0) {
                    BlockPos groundPos = this.blockPosition();
                    AoEIndicatorClientState.addCircleIndicator(this.getUUID(), groundPos, explosionRadius, embeddedTicks, 0x88FFAA00);
                }
            }
        }
        
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            Entity owner = getOwner();
            if (!impacted) {
                lifeTicks++;
                if (lifeTicks > delayTicks) {
                    // Falling
                    this.setDeltaMovement(0, fallSpeed, 0);
                    this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
                    
                    if (this.onGround() || this.verticalCollision || this.getY() <= this.level().getMinBuildHeight()) {
                        impacted = true;
                        this.entityData.set(DATA_IMPACTED, true);
                        this.setDeltaMovement(0, 0, 0);
                        
                        this.playSound(net.minecraft.sounds.SoundEvents.ANVIL_LAND, 2.0f, 0.8f);
                        
                        BlockPos groundPos = getGroundPos();
                        if (groundPos != null) {
                            net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(groundPos);
                            serverLevel.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state),
                                this.getX(), groundPos.getY() + 1.0, this.getZ(), 100, 2.0, 0.5, 2.0, 0.15);
                                
                            // Crater Blast
                            for (int i = 0; i < 36; i++) {
                                double angle = i * Math.PI * 2 / 36;
                                double dx = Math.cos(angle);
                                double dz = Math.sin(angle);
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, 
                                    this.getX(), groundPos.getY() + 1.0, this.getZ(), 0, dx, 0.2, dz, 0.5);
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, 
                                    this.getX(), groundPos.getY() + 1.0, this.getZ(), 0, dx, 0.2, dz, 0.5);
                            }
                            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, 
                                this.getX(), groundPos.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
                                
                            // Ground Rupture
                            for (int x = -1; x <= 1; x++) {
                                for (int z = -1; z <= 1; z++) {
                                    BlockPos p = groundPos.offset(x, 0, z);
                                    net.minecraft.world.level.block.state.BlockState pState = level().getBlockState(p);
                                    if (pState.getDestroySpeed(level(), p) >= 0 && !pState.isAir()) {
                                        replacedBlocks.put(p, pState);
                                        level().setBlock(p, net.minecraft.world.level.block.Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                                    }
                                }
                            }
                        }

                        // Deal Impact Damage
                        dealDamageAndKnockback(impactRadius, impactDamage, owner);
                    }
                }
            } else {
                ticksSinceImpact++;
                if (ticksSinceImpact >= embeddedTicks) {
                    // Explode
                    this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), 2.0f, 1.0f);
                    dealDamageAndKnockback(explosionRadius, explosionDamage, owner);
                    
                    // Visual explosion (particles only, no block destruction)
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, 
                            this.getX(), this.getY(), this.getZ(), 
                            5, 1, 1, 1, 0);
                    
                    this.discard();
                }
            }
        }
    }
    
    private void dealDamageAndKnockback(float radius, float damage, Entity owner) {
        AABB boundingBox = this.getBoundingBox().inflate(radius, radius, radius);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, boundingBox, e -> {
            return e != owner && e.distanceToSqr(this) <= radius * radius && e.isAlive();
        });
        
        for (LivingEntity target : targets) {
            target.hurt(ModDamageTypes.getTrueDamage(this.level(), owner != null ? owner : this), damage);
            
            // Knockback logic
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            target.knockback(1.0, -dx, -dz);
        }
    }
    
    private Entity getOwner() {
        if (ownerId != null && this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(ownerId);
        }
        return null;
    }
    
    public net.minecraft.core.BlockPos getGroundPos() {
        net.minecraft.core.BlockPos pos = this.blockPosition();
        while (pos.getY() > this.level().getMinBuildHeight() && !this.level().getBlockState(pos).isSolidRender(this.level(), pos)) {
            pos = pos.below();
        }
        return pos;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            for (java.util.Map.Entry<BlockPos, net.minecraft.world.level.block.state.BlockState> entry : replacedBlocks.entrySet()) {
                this.level().setBlock(entry.getKey(), entry.getValue(), 3);
            }
            replacedBlocks.clear();
        }
        super.remove(reason);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "sword_controller", 0, state -> {
            if (this.entityData.get(DATA_IMPACTED)) {
                return state.setAndContinue(RawAnimation.begin().thenPlayAndHold("giant_sword_hitting_ground"));
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
