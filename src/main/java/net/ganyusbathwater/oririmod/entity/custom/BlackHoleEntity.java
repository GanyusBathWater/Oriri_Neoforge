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
    public static java.awt.Color HALO_COLOR_1 = new java.awt.Color(30, 0, 50); // Deep dark purple
    public static java.awt.Color HALO_COLOR_2 = new java.awt.Color(0, 0, 0);   // Void black

    // System B: Inner Accretion Disk (Emissive)
    public static float INNER_DISK_RADIUS_MULTIPLIER = 1.1f; // Partially inside the void boundary
    public static float INNER_DISK_ROTATION_SPEED = 0.8f;    // Extremely high speed
    public static java.awt.Color INNER_DISK_COLOR_1 = java.awt.Color.WHITE;
    public static java.awt.Color INNER_DISK_COLOR_2 = java.awt.Color.YELLOW;

    // System C: Outer Accretion Disk
    public static float OUTER_DISK_RADIUS_MULTIPLIER = 1.5f; // Extremely close to the inner ring
    public static float OUTER_DISK_ROTATION_SPEED = 0.1f;    // Slow orbit
    public static java.awt.Color OUTER_DISK_COLOR_1 = new java.awt.Color(150, 50, 0); // Dark orange
    public static java.awt.Color OUTER_DISK_COLOR_2 = new java.awt.Color(60, 0, 100); // Dark purple

    // System D: Suction
    public static float SUCTION_VISUAL_RADIUS_MULTIPLIER = 3.5f; // Short range beyond core surface
    public static java.awt.Color SUCTION_VISUAL_COLOR_1 = new java.awt.Color(40, 40, 40); // Ash
    public static java.awt.Color SUCTION_VISUAL_COLOR_2 = new java.awt.Color(80, 0, 120); // Enderman purple
    
    // Nova Explosion
    public static java.awt.Color NOVA_COLOR_1 = java.awt.Color.WHITE;
    public static java.awt.Color NOVA_COLOR_2 = new java.awt.Color(200, 50, 255);
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
            this.spawnLodestoneParticles(currentRadius, age, maxLife);
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
                                    this.level().destroyBlock(pos, false, this);
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

    private void spawnLodestoneParticles(float currentRadius, int age, int maxLife) {
        if (currentRadius <= 0.01f && age < maxLife - 2) return;

        double cx = this.getX();
        
        // The GeckoLib model's center is at Y=13.5 pixels (0.84375 blocks).
        // Since BlackHoleRenderer scales from Y=0 with a 1.5x multiplier, the visual center moves up as it grows.
        float visualScale = (currentRadius / this.getMaxRadius()) * 1.5f;
        double cy = this.getY() + (13.5 / 16.0) * visualScale;
        
        double cz = this.getZ();
        
        float visualCoreRadius = (currentRadius / this.getMaxRadius()) * VISUAL_CORE_RADIUS_MAX;

        // 3. The Nova Explosion
        if (age >= maxLife - 1) {
            for (int i = 0; i < 150; i++) {
                double vx = (this.random.nextDouble() - 0.5) * 3.0;
                double vy = (this.random.nextDouble() - 0.5) * 3.0;
                double vz = (this.random.nextDouble() - 0.5) * 3.0;
                team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder.create(team.lodestar.lodestone.registry.common.particle.LodestoneParticleTypes.WISP_PARTICLE.get())
                    .setForceSpawn(true)
                    .setScaleData(team.lodestar.lodestone.systems.particle.data.GenericParticleData.create(0.8f, 0).build())
                    .setTransparencyData(team.lodestar.lodestone.systems.particle.data.GenericParticleData.create(1.0f, 0).build())
                    .setColorData(team.lodestar.lodestone.systems.particle.data.color.ColorParticleData.create(NOVA_COLOR_1, NOVA_COLOR_2).build())
                    .setLifetime(20 + this.random.nextInt(20))
                    .setMotion(vx, vy, vz)
                    .spawn(this.level(), cx, cy, cz);
            }
            return;
        }

        float haloScale = visualCoreRadius * HALO_SCALE_MULTIPLIER;
        float innerRadius = visualCoreRadius * INNER_DISK_RADIUS_MULTIPLIER;
        float outerRadius = visualCoreRadius * OUTER_DISK_RADIUS_MULTIPLIER;
        float suctionRadius = visualCoreRadius * SUCTION_VISUAL_RADIUS_MULTIPLIER;
        
        // System A: The Compressed Halo (Soft boundary)
        if (age % 2 == 0) {
            team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder.create(team.lodestar.lodestone.registry.common.particle.LodestoneParticleTypes.WISP_PARTICLE.get())
                .setForceSpawn(true)
                .setScaleData(team.lodestar.lodestone.systems.particle.data.GenericParticleData.create(haloScale, haloScale * 1.1f).build())
                .setTransparencyData(team.lodestar.lodestone.systems.particle.data.GenericParticleData.create(0.7f, 0.0f).build())
                .setColorData(team.lodestar.lodestone.systems.particle.data.color.ColorParticleData.create(HALO_COLOR_1, HALO_COLOR_2).build())
                .setSpinData(team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData.create(age * 0.02f, (age + 1) * 0.02f).build())
                .setLifetime(6)
                .spawn(this.level(), cx, cy, cz);
        }

        // System B: Compressed Inner Accretion Disk (Emissive)
        // Spawning many particles per tick to create a continuous glowing trail
        for (int i = 0; i < 6; i++) {
            float angle = (age * INNER_DISK_ROTATION_SPEED) + (i * (float)Math.PI / 3.0f);
            double px = cx + Math.cos(angle) * innerRadius;
            double pz = cz + Math.sin(angle) * innerRadius;
            
            team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder.create(team.lodestar.lodestone.registry.common.particle.LodestoneParticleTypes.WISP_PARTICLE.get())
                .setForceSpawn(true)
                .setScaleData(team.lodestar.lodestone.systems.particle.data.GenericParticleData.create(visualCoreRadius * 0.5f, 0).build())
                .setTransparencyData(team.lodestar.lodestone.systems.particle.data.GenericParticleData.create(1.0f, 0).build())
                .setColorData(team.lodestar.lodestone.systems.particle.data.color.ColorParticleData.create(INNER_DISK_COLOR_1, INNER_DISK_COLOR_2).build())
                .setLifetime(5)
                .spawn(this.level(), px, cy, pz);
        }

        // System C: Compressed Outer Accretion Disk (Darker, wobbly)
        for (int i = 0; i < 4; i++) {
            float angle = -(age * OUTER_DISK_ROTATION_SPEED) + (i * (float)Math.PI / 2.0f);
            double px = cx + Math.cos(angle) * outerRadius;
            double pz = cz + Math.sin(angle) * outerRadius;
            double wobble = Math.sin(age * 0.15f + i) * (visualCoreRadius * 0.3f);
            
            team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder.create(team.lodestar.lodestone.registry.common.particle.LodestoneParticleTypes.SMOKE_PARTICLE.get())
                .setForceSpawn(true)
                .setScaleData(team.lodestar.lodestone.systems.particle.data.GenericParticleData.create(visualCoreRadius * 0.6f, 0).build())
                .setTransparencyData(team.lodestar.lodestone.systems.particle.data.GenericParticleData.create(0.8f, 0).build())
                .setColorData(team.lodestar.lodestone.systems.particle.data.color.ColorParticleData.create(OUTER_DISK_COLOR_1, OUTER_DISK_COLOR_2).build())
                .setLifetime(10)
                .spawn(this.level(), px, cy + wobble, pz);
        }

        // System D: Short-Range Visual Suction
        for (int i = 0; i < 3; i++) {
            double rx = (this.random.nextDouble() - 0.5) * 2.0 * suctionRadius;
            double ry = (this.random.nextDouble() - 0.5) * 2.0 * suctionRadius;
            double rz = (this.random.nextDouble() - 0.5) * 2.0 * suctionRadius;
            
            double dist = Math.sqrt(rx*rx + ry*ry + rz*rz);
            if (dist > suctionRadius || dist < visualCoreRadius) continue;

            double px = cx + rx;
            double py = cy + ry;
            double pz = cz + rz;

            // Velocity aimed exactly at center
            double speedMultiplier = 0.2;
            double vx = -rx * speedMultiplier;
            double vy = -ry * speedMultiplier;
            double vz = -rz * speedMultiplier;

            // Lifetime calculation based on distance and speed to disappear at void edge
            int calcLifetime = (int)Math.max(1, (dist - visualCoreRadius) / (dist * speedMultiplier));
            
            team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder.create(team.lodestar.lodestone.registry.common.particle.LodestoneParticleTypes.TWINKLE_PARTICLE.get())
                .setForceSpawn(true)
                .setScaleData(team.lodestar.lodestone.systems.particle.data.GenericParticleData.create(0.15f, 0).build())
                .setTransparencyData(team.lodestar.lodestone.systems.particle.data.GenericParticleData.create(1.0f, 0).build())
                .setColorData(team.lodestar.lodestone.systems.particle.data.color.ColorParticleData.create(SUCTION_VISUAL_COLOR_1, SUCTION_VISUAL_COLOR_2).build())
                .setLifetime(Math.min(calcLifetime, 15))
                .setMotion(vx, vy, vz)
                .spawn(this.level(), px, py, pz);
        }
    }
}
