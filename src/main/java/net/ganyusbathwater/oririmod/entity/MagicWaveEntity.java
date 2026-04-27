package net.ganyusbathwater.oririmod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * MagicWaveEntity — a slab-height ground-skimming magical shockwave projectile.
 *
 * <h2>Behaviour</h2>
 * <ul>
 *   <li>Travels at {@value SPEED} blocks/tick (≈ normal walking speed)</li>
 *   <li>Despawns after {@value MAX_LIFETIME} ticks (20 s)</li>
 *   <li>Despawns when its forward path is blocked by a solid wall it cannot step over</li>
 *   <li>Damages and despawns when it intersects a {@link LivingEntity}</li>
 * </ul>
 */
public class MagicWaveEntity extends Entity {

    // ── Constants ─────────────────────────────────────────────────────────────
    public static final float  SPEED        = 0.15f;   // blocks/tick — normal walk speed
    public static final int    MAX_LIFETIME = 400;     // 20 s
    public static final int    FADE_TICKS   = 20;      // fade-out duration

    // ── Synced data ────────────────────────────────────────────────────────────
    private static final EntityDataAccessor<Integer> WAVE_COLOR =
            SynchedEntityData.defineId(MagicWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DIR_X =
            SynchedEntityData.defineId(MagicWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z =
            SynchedEntityData.defineId(MagicWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(MagicWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(MagicWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGE_TICKS =
            SynchedEntityData.defineId(MagicWaveEntity.class, EntityDataSerializers.INT);

    // ── Local server-only state ───────────────────────────────────────────────
    private int ageTicks = 0;

    // ─────────────────────────────────────────────────────────────────────────
    public MagicWaveEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WAVE_COLOR, 0xFF_AA00FF);
        builder.define(DIR_X,     0f);
        builder.define(DIR_Z,     1f);
        builder.define(DAMAGE,    4f);
        builder.define(OWNER_ID, -1);
        builder.define(CHARGE_TICKS, 0);
    }

    // ── Public setters ─────────────────────────────────────────────────────────
    public void setWaveColor(int argb)     { this.entityData.set(WAVE_COLOR, argb); }
    public void setDirection(float dx, float dz) {
        // Normalize
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len > 1e-5f) { dx /= len; dz /= len; }
        this.entityData.set(DIR_X, dx);
        this.entityData.set(DIR_Z, dz);
    }
    public void setWaveDamage(float dmg)   { this.entityData.set(DAMAGE, dmg); }
    public void setOwnerId(int id)         { this.entityData.set(OWNER_ID, id); }
    public void setChargeTicks(int ticks)  { this.entityData.set(CHARGE_TICKS, ticks); }

    // ── Public getters (used by renderer) ─────────────────────────────────────
    public int   getWaveColor()  { return this.entityData.get(WAVE_COLOR); }
    public float getDirX()       { return this.entityData.get(DIR_X); }
    public float getDirZ()       { return this.entityData.get(DIR_Z); }
    public int   getAgeTicks()   { return ageTicks; }

    // ─────────────────────────────────────────────────────────────────────────
    // Tick
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            ageTicks++;
            return; // All logic is server-authoritative
        }

        ageTicks++;
        int charge = this.entityData.get(CHARGE_TICKS);
        
        if (ageTicks < charge) {
            return; // Wait for charge
        }

        // ── Lifetime check ─────────────────────────────────────────────────
        if (ageTicks >= MAX_LIFETIME + charge) {
            this.discard();
            return;
        }

        float dx = this.entityData.get(DIR_X);
        float dz = this.entityData.get(DIR_Z);

        // ── Move ───────────────────────────────────────────────────────────
        // Use NeoForge's built-in movement which handles 1-block step-ups
        Vec3 oldPos = this.position();
        this.setDeltaMovement(dx * SPEED, -0.08, dz * SPEED); // slight gravity pull to stay grounded
        this.move(MoverType.SELF, this.getDeltaMovement());

        // ── Forward wall check ─────────────────────────────────────────────
        // If the entity hit a wall and could not step over it, its horizontal movement
        // will be severely restricted by the collision engine.
        Vec3 newPos = this.position();
        double horizontalDistSq = (newPos.x - oldPos.x) * (newPos.x - oldPos.x) 
                                + (newPos.z - oldPos.z) * (newPos.z - oldPos.z);
        
        // If it moved less than 10% of its intended speed, it's blocked.
        if (horizontalDistSq < (SPEED * SPEED * 0.1)) {
            this.discard();
            return;
        }

        // ── Damage mobs in the wave's AABB ─────────────────────────────────
        applyWaveDamage();
    }

    private void applyWaveDamage() {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        int ownerId = this.entityData.get(OWNER_ID);
        float dmg   = this.entityData.get(DAMAGE);

        // Fetch direction vectors
        float fX = this.entityData.get(DIR_X);
        float fZ = this.entityData.get(DIR_Z);
        float rX = -fZ;
        float rZ = fX;

        // Visual constants mapped exactly to MagicWaveRenderer
        float hw = 1.2f;        
        float sweepBack = 1.2f; 

        // Broad-phase search expanding natively up to the full wingspan limit
        AABB broadSearchBox = this.getBoundingBox().inflate(1.5, 0.4, 1.5);

        List<LivingEntity> hits = serverLevel.getEntitiesOfClass(
                LivingEntity.class, broadSearchBox, e -> {
                    if (e.getId() == ownerId) return false;

                    // Narrow-phase Trigonometric V-Shape Filter — align exactly with the textures!
                    double vX = e.getX() - this.getX();
                    double vZ = e.getZ() - this.getZ();

                    // Project relative coordinates down the forward and right-perpendicular axes
                    double depth = vX * fX + vZ * fZ;
                    double lateral = Math.abs(vX * rX + vZ * rZ);

                    // Reject if it is significantly wider than the wingtips
                    if (lateral > hw + 0.6) return false; 

                    // Compute the exact mathematical depth the V-shape wing should physically be located at
                    // Since it trails backwards, depth drops natively relative to lateral displacement
                    double expectedDepth = -(lateral / hw) * sweepBack;

                    // Accept only if the mob is tightly sandwiched flush against the angled wing texture 
                    return Math.abs(depth - expectedDepth) < 0.6;
                });

        if (!hits.isEmpty()) {
            DamageSource src = serverLevel.damageSources().magic();
            for (LivingEntity target : hits) {
                target.hurt(src, dmg);
            }
            // Wave is consumed on first mob contact
            this.discard();
        }
    }

    // ── NBT ───────────────────────────────────────────────────────────────────
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("WaveColor",  this.entityData.get(WAVE_COLOR));
        tag.putFloat("DirX",     this.entityData.get(DIR_X));
        tag.putFloat("DirZ",     this.entityData.get(DIR_Z));
        tag.putFloat("Damage",   this.entityData.get(DAMAGE));
        tag.putInt("OwnerId",    this.entityData.get(OWNER_ID));
        tag.putInt("ChargeTicks",this.entityData.get(CHARGE_TICKS));
        tag.putInt("AgeTicks",   ageTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(WAVE_COLOR, tag.getInt("WaveColor"));
        this.entityData.set(DIR_X,      tag.getFloat("DirX"));
        this.entityData.set(DIR_Z,      tag.getFloat("DirZ"));
        this.entityData.set(DAMAGE,     tag.getFloat("Damage"));
        this.entityData.set(OWNER_ID,   tag.getInt("OwnerId"));
        this.entityData.set(CHARGE_TICKS, tag.getInt("ChargeTicks"));
        ageTicks = tag.getInt("AgeTicks");
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return distSq < 64 * 64;
    }
}
