package net.ganyusbathwater.oririmod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * LaserBeamEntity — a fully configurable, server-authoritative laser beam.
 *
 * <p>All beam parameters are synced to clients via SynchedEntityData so the
 * client renderer can read them without any extra network packets. The entity
 * handles its own damage, lifespan, and sounds entirely on the server side.
 *
 * <p>Creative use cases:
 * <ul>
 *   <li>Simple targeted beam: spawn and forget.</li>
 *   <li>Orbiting/rotating beam: keep a reference and call {@link #updateBeamPositions} each tick.</li>
 *   <li>Ground upward blast: start = ground pos, end = ground + (0, height, 0).</li>
 *   <li>Fan attack: spawn multiple entities with angles offset from the same origin.</li>
 * </ul>
 */
public class LaserBeamEntity extends Entity {

    // ──────────────────────────────────────────────────────────────────────────
    // Synced fields (start position)
    // ──────────────────────────────────────────────────────────────────────────
    private static final EntityDataAccessor<Float> START_X =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> START_Y =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> START_Z =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);

    // ──────────────────────────────────────────────────────────────────────────
    // Synced fields (end position)
    // ──────────────────────────────────────────────────────────────────────────
    private static final EntityDataAccessor<Float> END_X =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Y =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Z =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);

    // ──────────────────────────────────────────────────────────────────────────
    // Synced fields (appearance & behavior)
    // ──────────────────────────────────────────────────────────────────────────
    private static final EntityDataAccessor<Float>   BEAM_WIDTH           =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> BEAM_COLOR           =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION_TICKS       =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE_TICKS            =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float>   DAMAGE               =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DAMAGE_INTERVAL      =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID             =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGE_TICKS         =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CORE_HIDDEN          =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> USE_WAVE_CIRCLE      =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> USE_WATER_CIRCLE     =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.BOOLEAN);
            
    // Indicator Sync
    private static final EntityDataAccessor<Byte> INDICATOR_TYPE = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> ORBIT_CX = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORBIT_CY = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORBIT_CZ = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORBIT_P1 = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORBIT_P2 = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORBIT_P3 = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);

    // ──────────────────────────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────────────────────────

    public LaserBeamEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // SynchedEntityData
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(START_X, 0.0f);
        builder.define(START_Y, 0.0f);
        builder.define(START_Z, 0.0f);
        builder.define(END_X, 0.0f);
        builder.define(END_Y, 0.0f);
        builder.define(END_Z, 0.0f);
        builder.define(CORE_HIDDEN, false);
        builder.define(USE_WAVE_CIRCLE, false);
        builder.define(USE_WATER_CIRCLE, false);
        builder.define(BEAM_WIDTH,      0.4f);
        builder.define(BEAM_COLOR,      0xFF_00AAFF);  // electric blue default
        builder.define(DURATION_TICKS,  60);
        builder.define(AGE_TICKS,       0);
        builder.define(DAMAGE,          5.0f);
        builder.define(DAMAGE_INTERVAL, 10);
        builder.define(OWNER_ID,        -1);
        builder.define(CHARGE_TICKS,    40);
        builder.define(INDICATOR_TYPE,  (byte) 0);
        builder.define(ORBIT_CX, 0f);
        builder.define(ORBIT_CY, 0f);
        builder.define(ORBIT_CZ, 0f);
        builder.define(ORBIT_P1, 0f);
        builder.define(ORBIT_P2, 0f);
        builder.define(ORBIT_P3, 0f);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public setters (called by LaserBeamUtil before spawning)
    // ──────────────────────────────────────────────────────────────────────────

    /** Sets both start and end positions of the beam in world-space. */
    public void updateBeamPositions(Vec3 start, Vec3 end) {
        this.entityData.set(START_X, (float) start.x);
        this.entityData.set(START_Y, (float) start.y);
        this.entityData.set(START_Z, (float) start.z);
        this.entityData.set(END_X,   (float) end.x);
        this.entityData.set(END_Y,   (float) end.y);
        this.entityData.set(END_Z,   (float) end.z);
        // Keep the entity's logical position at the midpoint so tracking range is correct
        Vec3 mid = start.lerp(end, 0.5);
        this.moveTo(mid.x, mid.y, mid.z);
    }

    public void setBeamWidth(float width)           { this.entityData.set(BEAM_WIDTH,      width);    }
    public void setBeamColor(int argbColor)         { this.entityData.set(BEAM_COLOR,      argbColor);}
    public void setDurationTicks(int ticks)         { this.entityData.set(DURATION_TICKS,  ticks);    }
    public void setDamage(float dmg)                { this.entityData.set(DAMAGE,          dmg);      }
    public void setDamageIntervalTicks(int ticks)   { this.entityData.set(DAMAGE_INTERVAL, ticks);    }
    public void setOwnerId(int id)                  { this.entityData.set(OWNER_ID,        id);       }
    public void setChargeTicks(int ticks)           { this.entityData.set(CHARGE_TICKS,    ticks);    }
    public void setCoreHidden(boolean hidden)       { this.entityData.set(CORE_HIDDEN,     hidden);   }
    public void setUseWaveCircle(boolean useWave)   { this.entityData.set(USE_WAVE_CIRCLE, useWave);  }
    public void setUseWaterCircle(boolean use)      { this.entityData.set(USE_WATER_CIRCLE, use);     }

    // ──────────────────────────────────────────────────────────────────────────
    // Orbital mechanics (Server-only)
    // ──────────────────────────────────────────────────────────────────────────
    private boolean isCylinderOrbit = false;
    private boolean isClockOrbit = false;
    private Vec3 orbitCenter = Vec3.ZERO;
    private float orbitRadius = 0f;
    private float orbitHeight = 0f;
    private float orbitSpeed = 0f;
    private float currentAngle = 0f;

    private boolean isSphereOrbit = false;
    private float staticPitch   = 0f;

    private boolean hasCapturedStaticShape = false;
    private Vec3 nativeStaticStart = null;
    private Vec3 nativeStaticEnd = null;

    public void setCylinderOrbit(Vec3 center, float radius, float height, float speedRad, float startAngleRad) {
        this.orbitCenter = center;
        this.orbitRadius = radius;
        this.orbitHeight = height;
        this.orbitSpeed = speedRad;
        this.currentAngle = startAngleRad;
        this.isCylinderOrbit = true;
        this.isClockOrbit = false;
        this.isSphereOrbit = false;
        
        this.entityData.set(INDICATOR_TYPE, (byte) 1);
        this.entityData.set(ORBIT_CX, (float) center.x);
        this.entityData.set(ORBIT_CY, (float) center.y);
        this.entityData.set(ORBIT_CZ, (float) center.z);
        this.entityData.set(ORBIT_P1, radius);
        this.entityData.set(ORBIT_P2, speedRad);
    }

    public void setClockOrbit(Vec3 center, float radius, float speedRad, float startAngleRad) {
        this.orbitCenter = center;
        this.orbitRadius = radius;
        this.orbitSpeed = speedRad;
        this.currentAngle = startAngleRad;
        this.isClockOrbit = true;
        this.isCylinderOrbit = false;
        this.isSphereOrbit = false;
        
        this.entityData.set(INDICATOR_TYPE, (byte) 2);
        this.entityData.set(ORBIT_CX, (float) center.x);
        this.entityData.set(ORBIT_CY, (float) center.y);
        this.entityData.set(ORBIT_CZ, (float) center.z);
        this.entityData.set(ORBIT_P1, radius);
        this.entityData.set(ORBIT_P2, speedRad);
        this.entityData.set(ORBIT_P3, speedRad * this.getDurationTicks()); // sweep angle
    }

    public void setSphereOrbit(Vec3 center, float radius, float staticPitchRad, float spinSpeedRad, float startYawRad) {
        this.orbitCenter = center;
        this.orbitRadius = radius;
        this.staticPitch = staticPitchRad;
        this.orbitSpeed = spinSpeedRad;
        this.currentAngle = startYawRad;
        this.isSphereOrbit = true;
        this.isCylinderOrbit = false;
        this.isClockOrbit = false;
        
        this.entityData.set(INDICATOR_TYPE, (byte) 3); // Impact point indicator
    }

    public void setIndicatorType(byte type, float p1, float p2, float p3) {
        this.entityData.set(INDICATOR_TYPE, type);
        this.entityData.set(ORBIT_P1, p1);
        this.entityData.set(ORBIT_P2, p2);
        this.entityData.set(ORBIT_P3, p3);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public getters (used by renderer and util)
    // ──────────────────────────────────────────────────────────────────────────

    public Vec3 getBeamStart() {
        return new Vec3(
                this.entityData.get(START_X),
                this.entityData.get(START_Y),
                this.entityData.get(START_Z));
    }

    public Vec3 getBeamEnd() {
        return new Vec3(
                this.entityData.get(END_X),
                this.entityData.get(END_Y),
                this.entityData.get(END_Z));
    }

    public float  getBeamWidth()          { return this.entityData.get(BEAM_WIDTH);      }
    public int    getBeamColor()          { return this.entityData.get(BEAM_COLOR);       }
    public int    getDurationTicks()      { return this.entityData.get(DURATION_TICKS);   }
    public int    getAgeTicks()           { return this.entityData.get(AGE_TICKS);        }
    public float  getDamage()             { return this.entityData.get(DAMAGE);           }
    public int    getDamageIntervalTicks(){ return this.entityData.get(DAMAGE_INTERVAL);  }
    public int    getOwnerId()            { return this.entityData.get(OWNER_ID);         }
    public int    getChargeTicks()        { return this.entityData.get(CHARGE_TICKS);     }
    public boolean isCoreHidden()         { return this.entityData.get(CORE_HIDDEN);      }
    public boolean usesWaveCircle()       { return this.entityData.get(USE_WAVE_CIRCLE);  }
    public boolean usesWaterCircle()      { return this.entityData.get(USE_WATER_CIRCLE); }

    // ──────────────────────────────────────────────────────────────────────────
    // Tick logic
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            int charge = this.entityData.get(CHARGE_TICKS);
            if (this.tickCount == 1 && charge > 0 && !this.isSilent()) {
                // Play shrieker wind up sound locally
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.SCULK_SHRIEKER_SHRIEK,
                        net.minecraft.sounds.SoundSource.HOSTILE, 2.0f, 1.2f, false);
            }

            int spawnTick = charge > 0 ? charge + 1 : 1;
            if (this.tickCount == spawnTick && !this.isSilent()) {
                playClientSound();
            }
            
            byte type = this.entityData.get(INDICATOR_TYPE);
            java.util.UUID id = this.getUUID();
            int color = this.getBeamColor();
            float width = this.getBeamWidth();
            
            // Render AoE Indicators while charging
            if (charge > 0 && this.getAgeTicks() < charge) {
                int dur = charge;
                
                if (type == 0) { // LINE
                    Vec3 start = this.getBeamStart();
                    Vec3 end = this.getBeamEnd();
                    if (start.distanceToSqr(end) > 0.1) {
                        if (Math.abs(start.x - end.x) < 0.1 && Math.abs(start.z - end.z) < 0.1) {
                            BlockPos groundPos = BlockPos.containing(start);
                            while (groundPos.getY() > this.level().getMinBuildHeight() && this.level().getBlockState(groundPos).isAir()) {
                                groundPos = groundPos.below();
                            }
                            net.ganyusbathwater.oririmod.client.render.AoEIndicatorClientState.addCircleIndicator(id, groundPos, width * 1.5f, dur, color);
                        } else {
                            net.ganyusbathwater.oririmod.client.render.AoEIndicatorClientState.addLineIndicator(id, start, end, width, dur, color);
                        }
                    }
                } else if (type == 3) { // IMPACT POINT
                    Vec3 end = this.getBeamEnd();
                    // Project down to ground to make sure it renders nicely on the floor
                    BlockPos groundPos = BlockPos.containing(end.x, end.y, end.z);
                    while (groundPos.getY() > this.level().getMinBuildHeight() && this.level().getBlockState(groundPos).isAir()) {
                        groundPos = groundPos.below();
                    }
                    net.ganyusbathwater.oririmod.client.render.AoEIndicatorClientState.addCircleIndicator(id, groundPos, width * 2f, dur, color);
                } else if (type == 4) { // EXPANDING CIRCLE
                    float maxRadius = 4.0f;
                    float radius = maxRadius * ((float) this.getAgeTicks() / charge);
                    BlockPos center = BlockPos.containing(this.getBeamStart());
                    net.ganyusbathwater.oririmod.client.render.AoEIndicatorClientState.addCircleIndicator(id, center, radius, charge, color);
                } else if (type == 5) { // EXPANDING CONE
                    float maxRadius = 4.0f;
                    float radius = maxRadius * ((float) this.getAgeTicks() / charge);
                    BlockPos center = BlockPos.containing(this.getBeamStart());
                    float facingYaw = this.entityData.get(ORBIT_P1);
                    float startAngle = -facingYaw - (float) Math.PI / 2f - (float) Math.toRadians(45); // Adjust for MC rotation
                    float sweepAngle = (float) Math.toRadians(90);
                    net.ganyusbathwater.oririmod.client.render.AoEIndicatorClientState.addArcIndicator(id, center, radius, startAngle, sweepAngle, charge, color);
                } else if (type == 6) { // EXPANDING PLAIN
                    float maxLength = 4.0f;
                    float length = maxLength * ((float) this.getAgeTicks() / charge);
                    Vec3 start = this.getBeamStart();
                    float facingYaw = this.entityData.get(ORBIT_P1);
                    float dx = (float) Math.sin(facingYaw);
                    float dz = (float) Math.cos(facingYaw);
                    Vec3 end = start.add(dx * length, 0, dz * length);
                    float w = 1.5f; // half width is 0.75 for left, 0.75 for right
                    if (start.distanceToSqr(end) > 0.01) {
                        net.ganyusbathwater.oririmod.client.render.AoEIndicatorClientState.addLineIndicator(id, start, end, w, charge, color);
                    }
                }
            }
            
            // For moving beams (Type 1 & 2), show a dynamic 2-second-ahead indicator throughout its lifetime!
            if (type == 1 || type == 2) {
                int ticksLeft = charge + this.getDurationTicks() - this.getAgeTicks();
                if (ticksLeft > 0) {
                    int lookaheadTicks = 40; // 2 seconds ahead
                    int activeTicksLeft = Math.max(0, this.getDurationTicks() - Math.max(0, this.getAgeTicks() - charge));
                    int actualLookahead = Math.min(lookaheadTicks, activeTicksLeft);
                    
                    if (actualLookahead > 0) {
                        BlockPos center = BlockPos.containing(this.entityData.get(ORBIT_CX), this.entityData.get(ORBIT_CY), this.entityData.get(ORBIT_CZ));
                        float radius = this.entityData.get(ORBIT_P1);
                        float speedRad = this.entityData.get(ORBIT_P2);
                        float sweepAngle = speedRad * actualLookahead;
                        
                        Vec3 startVec = this.getBeamStart();
                        float currentAngle = (float) Math.atan2(startVec.z - center.getZ(), startVec.x - center.getX());
                        
                        if (type == 1) { // CYLINDER
                            net.ganyusbathwater.oririmod.client.render.AoEIndicatorClientState.addDonutArcIndicator(
                                id, center, radius - width, radius + width, currentAngle, sweepAngle, 5, color);
                        } else { // CIRCLE
                            net.ganyusbathwater.oririmod.client.render.AoEIndicatorClientState.addDonutArcIndicator(
                                id, center, 0f, radius + width, currentAngle, sweepAngle, 5, color);
                        }
                    }
                }
            }
            
            return; // rendering is handled entirely by LaserBeamRenderer
        }

        int age = this.entityData.get(AGE_TICKS);
        this.entityData.set(AGE_TICKS, age + 1);

        int charge = this.entityData.get(CHARGE_TICKS);

        boolean isCharging = age < charge;

        if (this.isCylinderOrbit) {
            if (!isCharging) this.currentAngle += this.orbitSpeed;
            double offsetX = Math.cos(this.currentAngle) * this.orbitRadius;
            double offsetZ = Math.sin(this.currentAngle) * this.orbitRadius;
            Vec3 newStart = new Vec3(this.orbitCenter.x + offsetX, this.orbitCenter.y, this.orbitCenter.z + offsetZ);
            Vec3 newEnd = new Vec3(newStart.x, newStart.y + this.orbitHeight, newStart.z);
            this.updateBeamPositions(newStart, clipToBlock(newStart, newEnd));
        } else if (this.isClockOrbit) {
            if (!isCharging) this.currentAngle += this.orbitSpeed;
            double offsetX = Math.cos(this.currentAngle) * this.orbitRadius;
            double offsetZ = Math.sin(this.currentAngle) * this.orbitRadius;
            Vec3 newEnd = new Vec3(this.orbitCenter.x + offsetX, this.orbitCenter.y, this.orbitCenter.z + offsetZ);
            
            Vec3 dir = newEnd.subtract(this.orbitCenter).normalize();
            Vec3 newStart = this.orbitCenter.add(dir.scale(1.5));
            this.updateBeamPositions(newStart, clipToBlock(newStart, newEnd));
        } else if (this.isSphereOrbit) {
            if (!isCharging) this.currentAngle += this.orbitSpeed;
            
            // Map out the 3D rotation based on the fixed vertical tilt (pitch) 
            // and the continuously varying horizontal spinning tilt (yaw).
            double horizontalRadius = Math.cos(this.staticPitch) * this.orbitRadius;
            double offsetY = Math.sin(this.staticPitch) * this.orbitRadius;
            
            double offsetX = Math.cos(this.currentAngle) * horizontalRadius;
            double offsetZ = Math.sin(this.currentAngle) * horizontalRadius;

            Vec3 newEnd = new Vec3(this.orbitCenter.x + offsetX, this.orbitCenter.y + offsetY, this.orbitCenter.z + offsetZ);
            
            Vec3 dir = newEnd.subtract(this.orbitCenter).normalize();
            Vec3 newStart = this.orbitCenter.add(dir.scale(1.5));
            this.updateBeamPositions(newStart, clipToBlock(newStart, newEnd));
        } else {
            if (!this.hasCapturedStaticShape) {
                this.nativeStaticStart = getBeamStart();
                this.nativeStaticEnd = getBeamEnd();
                this.hasCapturedStaticShape = true;
            }
            if (this.nativeStaticStart != null && this.nativeStaticEnd != null) {
                this.updateBeamPositions(this.nativeStaticStart, clipToBlock(this.nativeStaticStart, this.nativeStaticEnd));
            }
        }

        // Discard when lifetime is up
        int duration = this.entityData.get(DURATION_TICKS);
        if (age >= charge + duration) {
            this.discard();
            return;
        }

        // Damage entities along the beam at the configured interval ONLY after charge
        int interval = this.entityData.get(DAMAGE_INTERVAL);
        if (age >= charge && interval > 0 && (age - charge) % interval == 0) {
            applyBeamDamage();
        }
    }

    private Vec3 clipToBlock(Vec3 start, Vec3 end) {
        if (this.level() == null) return end;
        net.minecraft.world.phys.BlockHitResult hit = this.level().clip(
                new net.minecraft.world.level.ClipContext(
                        start, 
                        end, 
                        net.minecraft.world.level.ClipContext.Block.COLLIDER, 
                        net.minecraft.world.level.ClipContext.Fluid.NONE, 
                        this));
                        
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return hit.getLocation();
        }
        return end;
    }

    /**
     * Deals damage to all LivingEntities whose bounding box geometrically intersects 
     * the physical line segment traced by the laser.
     */
    private void applyBeamDamage() {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        Vec3 start = getBeamStart();
        Vec3 end   = getBeamEnd();
        float dmg   = this.entityData.get(DAMAGE);
        int ownerId = this.entityData.get(OWNER_ID);
        float r     = Math.max(0.5f, this.entityData.get(BEAM_WIDTH));

        // Build enclosing AABB of the beam segment
        double minX = Math.min(start.x, end.x) - r;
        double minY = Math.min(start.y, end.y) - r;
        double minZ = Math.min(start.z, end.z) - r;
        double maxX = Math.max(start.x, end.x) + r;
        double maxY = Math.max(start.y, end.y) + r;
        double maxZ = Math.max(start.z, end.z) + r;

        AABB searchBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        List<LivingEntity> entities = serverLevel.getEntitiesOfClass(
                LivingEntity.class, searchBox,
                e -> e.getId() != ownerId && e.getBoundingBox().inflate(r).clip(start, end).isPresent());

        for (LivingEntity entity : entities) {
            entity.hurt(serverLevel.damageSources().magic(), dmg);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // NBT serialization
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(START_X,        tag.getFloat("StartX"));
        this.entityData.set(START_Y,        tag.getFloat("StartY"));
        this.entityData.set(START_Z,        tag.getFloat("StartZ"));
        this.entityData.set(END_X,          tag.getFloat("EndX"));
        this.entityData.set(END_Y,          tag.getFloat("EndY"));
        this.entityData.set(END_Z,          tag.getFloat("EndZ"));
        this.entityData.set(BEAM_WIDTH,     tag.getFloat("BeamWidth"));
        this.entityData.set(BEAM_COLOR,     tag.getInt("BeamColor"));
        this.entityData.set(DURATION_TICKS, tag.getInt("DurationTicks"));
        this.entityData.set(AGE_TICKS,      tag.getInt("AgeTicks"));
        this.entityData.set(DAMAGE,         tag.getFloat("Damage"));
        this.entityData.set(DAMAGE_INTERVAL,tag.getInt("DamageInterval"));
        this.entityData.set(OWNER_ID,       tag.getInt("OwnerId"));
        this.entityData.set(CHARGE_TICKS,   tag.getInt("ChargeTicks"));
        
        if (tag.contains("NativeStaticStartX")) {
            this.nativeStaticStart = new Vec3(tag.getDouble("NativeStaticStartX"), tag.getDouble("NativeStaticStartY"), tag.getDouble("NativeStaticStartZ"));
            this.nativeStaticEnd = new Vec3(tag.getDouble("NativeStaticEndX"), tag.getDouble("NativeStaticEndY"), tag.getDouble("NativeStaticEndZ"));
            this.hasCapturedStaticShape = tag.getBoolean("HasCapturedStaticShape");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("StartX",        this.entityData.get(START_X));
        tag.putFloat("StartY",        this.entityData.get(START_Y));
        tag.putFloat("StartZ",        this.entityData.get(START_Z));
        tag.putFloat("EndX",          this.entityData.get(END_X));
        tag.putFloat("EndY",          this.entityData.get(END_Y));
        tag.putFloat("EndZ",          this.entityData.get(END_Z));
        tag.putFloat("BeamWidth",     this.entityData.get(BEAM_WIDTH));
        tag.putInt("BeamColor",       this.entityData.get(BEAM_COLOR));
        tag.putInt("DurationTicks",   this.entityData.get(DURATION_TICKS));
        tag.putInt("AgeTicks",        this.entityData.get(AGE_TICKS));
        tag.putFloat("Damage",        this.entityData.get(DAMAGE));
        tag.putInt("DamageInterval",  this.entityData.get(DAMAGE_INTERVAL));
        tag.putInt("OwnerId",         this.entityData.get(OWNER_ID));
        tag.putInt("ChargeTicks",     this.entityData.get(CHARGE_TICKS));
        
        if (this.nativeStaticStart != null && this.nativeStaticEnd != null) {
            tag.putDouble("NativeStaticStartX", this.nativeStaticStart.x);
            tag.putDouble("NativeStaticStartY", this.nativeStaticStart.y);
            tag.putDouble("NativeStaticStartZ", this.nativeStaticStart.z);
            tag.putDouble("NativeStaticEndX", this.nativeStaticEnd.x);
            tag.putDouble("NativeStaticEndY", this.nativeStaticEnd.y);
            tag.putDouble("NativeStaticEndZ", this.nativeStaticEnd.z);
            tag.putBoolean("HasCapturedStaticShape", this.hasCapturedStaticShape);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Physics overrides
    // ──────────────────────────────────────────────────────────────────────────

    @Override public boolean isPushedByFluid()  { return false; }
    @Override public boolean isPushable()       { return false; }
    @Override public boolean isNoGravity()      { return true;  }

    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private void playClientSound() {
        net.ganyusbathwater.oririmod.client.sound.LaserBeamSoundInstance.play(this);
    }
}
