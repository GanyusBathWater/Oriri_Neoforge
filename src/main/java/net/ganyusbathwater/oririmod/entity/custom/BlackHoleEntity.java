package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class BlackHoleEntity extends Entity implements GeoEntity {    // --- COMPRESSED LODESTONE VISUAL CONFIGURATION ---
    public static float VISUAL_CORE_RADIUS_MAX = 0.4f; // The max visual radius of the black hole model in blocks
    
    // System A: Halo
    public static float HALO_SCALE_MULTIPLIER = 1.8f; // Just slightly larger than the core
    public static java.awt.Color HALO_COLOR_1 = new java.awt.Color(0, 0, 0); // Void black
    public static java.awt.Color HALO_COLOR_2 = new java.awt.Color(10, 10, 20);   // Very dark blue/black

    // System B: Inner Accretion Disk (Emissive) — Blue/White-hot
    public static float INNER_DISK_RADIUS_MULTIPLIER = 1.1f;
    public static float INNER_DISK_ROTATION_SPEED = 1.5f; // Hyper-fast
    public static java.awt.Color INNER_DISK_COLOR_1 = new java.awt.Color(200, 230, 255);   // Blue-white
    public static java.awt.Color INNER_DISK_COLOR_2 = new java.awt.Color(255, 255, 255); // Blinding White

    // System C: Outer Accretion Disk — Bright Orange to Deep Red
    public static float OUTER_DISK_RADIUS_MULTIPLIER = 1.5f;
    public static float OUTER_DISK_ROTATION_SPEED = 0.3f;
    public static java.awt.Color OUTER_DISK_COLOR_1 = new java.awt.Color(255, 100, 0);   // Bright Orange
    public static java.awt.Color OUTER_DISK_COLOR_2 = new java.awt.Color(150, 20, 0);   // Deep Red

    // System D: Suction
    public static float SUCTION_VISUAL_RADIUS_MULTIPLIER = 3.5f;
    public static java.awt.Color SUCTION_VISUAL_COLOR_1 = new java.awt.Color(255, 150, 50);  // Bright Orange
    public static java.awt.Color SUCTION_VISUAL_COLOR_2 = new java.awt.Color(255, 255, 255);  // White

    // System E: Energy Arcs
    public static java.awt.Color ARC_COLOR_1 = new java.awt.Color(100, 200, 255);   // Cyan/Blue
    public static java.awt.Color ARC_COLOR_2 = new java.awt.Color(255, 255, 255); // Blinding White

    // System F: Ash Cloud
    public static java.awt.Color ASH_COLOR_1 = new java.awt.Color(20, 10, 0);  // Dark burnt orange
    public static java.awt.Color ASH_COLOR_2 = new java.awt.Color(0, 0, 0);   // Black

    // Nova Explosion
    public static java.awt.Color NOVA_COLOR_1 = java.awt.Color.WHITE;
    public static java.awt.Color NOVA_COLOR_2 = new java.awt.Color(200, 230, 255);
    // --------------------------------------
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<Integer> MAX_LIFETIME = SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MAX_RADIUS = SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DESTROYS_BLOCKS = SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.BOOLEAN);

    public BlackHoleEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public BlackHoleEntity(Level level, double x, double y, double z, int lifetime, float maxRadius, boolean destroysBlocks) {
        super(ModEntities.BLACK_HOLE.get(), level);
        this.setPos(x, y, z);
        this.setMaxLifetime(lifetime);
        this.setMaxRadius(maxRadius);
        this.setDestroysBlocks(destroysBlocks);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MAX_LIFETIME, 100);
        builder.define(MAX_RADIUS, 5.0F);
        builder.define(DESTROYS_BLOCKS, true);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("MaxLifetime")) {
            this.setMaxLifetime(compound.getInt("MaxLifetime"));
        }
        if (compound.contains("MaxRadius")) {
            this.setMaxRadius(compound.getFloat("MaxRadius"));
        }
        if (compound.contains("DestroysBlocks")) {
            this.setDestroysBlocks(compound.getBoolean("DestroysBlocks"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("MaxLifetime", this.getMaxLifetime());
        compound.putFloat("MaxRadius", this.getMaxRadius());
        compound.putBoolean("DestroysBlocks", this.getDestroysBlocks());
    }

    public int getMaxLifetime() {
        return this.entityData.get(MAX_LIFETIME);
    }

    public void setMaxLifetime(int lifetime) {
        this.entityData.set(MAX_LIFETIME, lifetime);
    }

    public float getMaxRadius() {
        return this.entityData.get(MAX_RADIUS);
    }

    public void setMaxRadius(float maxRadius) {
        this.entityData.set(MAX_RADIUS, maxRadius);
    }

    public boolean getDestroysBlocks() {
        return this.entityData.get(DESTROYS_BLOCKS);
    }

    public void setDestroysBlocks(boolean destroysBlocks) {
        this.entityData.set(DESTROYS_BLOCKS, destroysBlocks);
    }

    public float getCurrentRadius() {
        int age = this.tickCount;
        int maxLife = this.getMaxLifetime();
        float maxRad = this.getMaxRadius();
        
        float shrinkStart = maxLife * 0.95f; // Shrinks very late (last 5%)
        
        if (age < shrinkStart) {
            float progress = age / shrinkStart;
            // Easing out cubic: 1 - (1-t)^3 for a smooth curve
            return maxRad * (float) (1.0 - Math.pow(1.0 - progress, 3));
        } else {
            float shrinkProgress = (age - shrinkStart) / (maxLife - shrinkStart);
            return maxRad * (1.0f - Math.min(1.0f, shrinkProgress));
        }
    }

    @Override
    public void tick() {
        super.tick();

        int age = this.tickCount;
        int maxLife = this.getMaxLifetime();
        float currentRadius = this.getCurrentRadius();

        if (this.level().isClientSide) {
            // Visual Aesthetics: Spawn dust and energy particles that spiral into the center
            if (currentRadius > 0.5f) {
                net.minecraft.util.RandomSource random = this.level().random;
                
                // Divided by 12 again: Spawn incredibly rarely (e.g. 1 particle every few ticks)
                float expectedParticles = currentRadius * 0.04f; 
                int particleCount = (int) expectedParticles;
                if (random.nextFloat() < (expectedParticles - particleCount)) {
                    particleCount++;
                }
                
                float maxRad = this.getMaxRadius();
                float visualScale = (maxRad > 0f) ? (currentRadius / maxRad) * 1.5f : 0f;
                double centerY = this.getY() + ((13.5f / 16.0f) * visualScale);
                
                for (int i = 0; i < particleCount; i++) {
                    // Spawn particles widely around the black hole (up to ~10-15 blocks away)
                    double theta = random.nextDouble() * 2 * Math.PI;
                    double phi = Math.acos(2 * random.nextDouble() - 1);
                    double r = currentRadius + 1.0 + random.nextDouble() * 12.0;
                    
                    double px = this.getX() + r * Math.sin(phi) * Math.cos(theta);
                    double py = centerY + r * Math.sin(phi) * Math.sin(theta);
                    double pz = this.getZ() + r * Math.cos(phi);
                    
                    // Calculate velocity directly toward the center core
                    double vx = this.getX() - px;
                    double vy = centerY - py;
                    double vz = this.getZ() - pz;
                    
                    double len = Math.sqrt(vx*vx + vy*vy + vz*vz);
                    if (len > 0) {
                        // We double the speed so it's sucked in extremely fast, and calculate exactly
                        // how many ticks it will take to reach the center (15 ticks).
                        // Speed math: len = (speed * 0.05) * ((1 - 0.96^15) / 0.04) -> speed = len * 1.75
                        double speed = len * 1.75;
                        
                        vx = (vx / len) * speed;
                        vy = (vy / len) * speed;
                        vz = (vz / len) * speed;
                        
                        // We use the ParticleEngine directly so we can grab the particle instance and 
                        // FORCE it to die instantly upon reaching the center (15 ticks). 
                        // This prevents it from popping out the other side!
                        net.minecraft.client.particle.Particle p = net.minecraft.client.Minecraft.getInstance().particleEngine.createParticle(
                                net.ganyusbathwater.oririmod.particle.ModParticles.ELDERWOODS_CAVE_PARTICLE.get(), 
                                px, py, pz, 
                                vx, vy, vz);
                                
                        if (p != null) {
                            p.setLifetime(15); // Mathematically guaranteed to delete exactly at the center!
                        }
                    }
                }
            }
        } else {
            if (age >= maxLife) {
                this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.getMaxRadius() * 0.8f, Level.ExplosionInteraction.TNT);
                this.discard();
                return;
            }
            
            // Block Destruction
            if (this.getDestroysBlocks() && currentRadius > 0.5f && age % 3 == 0) {
                int r = Mth.ceil(currentRadius);
                BlockPos center = this.blockPosition();
                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            BlockPos pos = center.offset(x, y, z);
                            if (pos.distSqr(center) <= currentRadius * currentRadius) {
                                BlockState state = this.level().getBlockState(pos);
                                if (!state.isAir() && state.getDestroySpeed(this.level(), pos) >= 0) {
                                    // Spawn vanilla block break particles
                                    this.level().levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state));
                                    // Remove block without dropping items
                                    this.level().removeBlock(pos, false);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Entity Suction & Damage (Runs on both sides for smooth client prediction and server logic)
        double physicalSuctionRadius = currentRadius * 3.5; // Increased radius
        if (physicalSuctionRadius > 0) {
            List<Entity> entities = this.level().getEntities(this, this.getBoundingBox().inflate(physicalSuctionRadius));
            for (Entity entity : entities) {
                if (entity == this) continue;
                if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) continue;

                double distSq = this.distanceToSqr(entity);
                if (distSq < physicalSuctionRadius * physicalSuctionRadius) {
                    double dist = Math.sqrt(distSq);
                    
                    double pullStrength = 1.0 - (dist / physicalSuctionRadius);
                    pullStrength = Math.pow(pullStrength, 1.5); // Less dropoff, stronger pull further out
                    
                    double kbResist = 0;
                    if (entity instanceof LivingEntity living) {
                        kbResist = living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                    }
                    
                    // Massively increased pull strength (0.15 -> 0.45)
                    double actualPull = pullStrength * 0.45 * (1.0 - kbResist);
                    
                    if (actualPull > 0) {
                        double trueCenterY = this.getY() + (13.5 / 16.0) * ((currentRadius / this.getMaxRadius()) * 1.5f);
                        Vec3 dir = new Vec3(this.getX() - entity.getX(), trueCenterY - entity.getY(), this.getZ() - entity.getZ()).normalize();
                        entity.setDeltaMovement(entity.getDeltaMovement().add(dir.scale(actualPull)));
                        entity.hasImpulse = true;
                        if (entity instanceof Player) {
                            entity.hurtMarked = true; // Forces the server to send the motion update packet to the player
                        }
                    }
                    
                    if (dist < currentRadius && !this.level().isClientSide && age % 5 == 0) {
                        entity.hurt(this.damageSources().magic(), 2.0f);
                    }
                }
            }
        }
    }

}
