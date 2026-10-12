package net.ganyusbathwater.oririmod.entity;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class TornadoEntity extends Projectile implements GeoEntity {

    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFESPAN = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> PULL_RADIUS = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PULL_STRENGTH = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DAMAGE_INTERVAL = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CAN_PULL_OWNER = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Client-side smooth color transition
    private int clientCurrentColor = 0xFFFFFF;

    public TornadoEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(true);
    }

    public TornadoEntity(Level level, LivingEntity owner) {
        super(ModEntities.TORNADO.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.2D, owner.getZ());
        this.noPhysics = false;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Default damage: 3.0F = 1.5 Hearts
        builder.define(DAMAGE, 3.0F);
        // Default lifespan: 120 ticks = 6 seconds
        builder.define(LIFESPAN, 120);
        // Default pull radius: 2.0 blocks
        builder.define(PULL_RADIUS, 2.0F);
        // Default pull strength
        builder.define(PULL_STRENGTH, 0.35F);
        // Damage applied every 10 ticks (0.5s) while inside
        builder.define(DAMAGE_INTERVAL, 10);
        // Whether the owner is pulled (default false, protects player / shooter)
        builder.define(CAN_PULL_OWNER, false);
        // 0 = dynamic biome coloring, > 0 = explicit fixed hex color
        builder.define(COLOR, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) this.setDamage(tag.getFloat("Damage"));
        if (tag.contains("Lifespan")) this.setLifespan(tag.getInt("Lifespan"));
        if (tag.contains("PullRadius")) this.setPullRadius(tag.getFloat("PullRadius"));
        if (tag.contains("PullStrength")) this.setPullStrength(tag.getFloat("PullStrength"));
        if (tag.contains("DamageInterval")) this.setDamageInterval(tag.getInt("DamageInterval"));
        if (tag.contains("CanPullOwner")) this.setCanPullOwner(tag.getBoolean("CanPullOwner"));
        if (tag.contains("Color")) this.setColor(tag.getInt("Color"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.getDamage());
        tag.putInt("Lifespan", this.getLifespan());
        tag.putFloat("PullRadius", this.getPullRadius());
        tag.putFloat("PullStrength", this.getPullStrength());
        tag.putInt("DamageInterval", this.getDamageInterval());
        tag.putBoolean("CanPullOwner", this.canPullOwner());
        tag.putInt("Color", this.getColor());
    }

    public float getDamage() { return this.entityData.get(DAMAGE); }
    public void setDamage(float damage) { this.entityData.set(DAMAGE, damage); }

    public int getLifespan() { return this.entityData.get(LIFESPAN); }
    public void setLifespan(int lifespan) { this.entityData.set(LIFESPAN, lifespan); }

    public float getPullRadius() { return this.entityData.get(PULL_RADIUS); }
    public void setPullRadius(float radius) { this.entityData.set(PULL_RADIUS, radius); }

    public float getPullStrength() { return this.entityData.get(PULL_STRENGTH); }
    public void setPullStrength(float strength) { this.entityData.set(PULL_STRENGTH, strength); }

    public int getDamageInterval() { return this.entityData.get(DAMAGE_INTERVAL); }
    public void setDamageInterval(int interval) { this.entityData.set(DAMAGE_INTERVAL, Math.max(1, interval)); }

    public boolean canPullOwner() { return this.entityData.get(CAN_PULL_OWNER); }
    public void setCanPullOwner(boolean canPull) { this.entityData.set(CAN_PULL_OWNER, canPull); }

    public int getColor() { return this.entityData.get(COLOR); }
    public void setColor(int color) { this.entityData.set(COLOR, color); }

    // Fluent API for clean instantiation from items, spells, and boss AI
    public TornadoEntity withDamage(float damage) { setDamage(damage); return this; }
    public TornadoEntity withLifespan(int lifespan) { setLifespan(lifespan); return this; }
    public TornadoEntity withPullRadius(float radius) { setPullRadius(radius); return this; }
    public TornadoEntity withPullStrength(float strength) { setPullStrength(strength); return this; }
    public TornadoEntity withDamageInterval(int interval) { setDamageInterval(interval); return this; }
    public TornadoEntity withCanPullOwner(boolean canPull) { setCanPullOwner(canPull); return this; }
    public TornadoEntity withColor(int color) { setColor(color); return this; }
    public TornadoEntity withGravity(boolean gravity) { setNoGravity(!gravity); return this; }

    public void shootFromOwner(LivingEntity shooter, float velocity, float inaccuracy) {
        this.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, velocity, inaccuracy);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.tickCount > this.getLifespan()) {
            this.discard();
            return;
        }

        Vec3 vel = this.getDeltaMovement();
        if (!this.isNoGravity()) {
            vel = vel.add(0.0D, -0.03D, 0.0D);
            this.setDeltaMovement(vel);
        }
        Vec3 startPos = this.position();

        // 1. Move and check collision
        if (vel.lengthSqr() > 0.0001D) {
            Vec3 nextPos = startPos.add(vel);
            BlockHitResult hitResult = this.level().clip(new ClipContext(
                    startPos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
            ));

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                Direction hitDir = hitResult.getDirection();
                if (hitDir == Direction.UP) {
                    // Glide along the floor instead of vanishing instantly
                    this.setDeltaMovement(vel.x, 0.0D, vel.z);
                    nextPos = hitResult.getLocation();
                } else {
                    // Impact solid wall or ceiling -> discard
                    this.onHitBlock(hitResult);
                    if (!this.level().isClientSide) {
                        this.discard();
                        return;
                    }
                }
            }
            this.setPos(nextPos);
        }

        this.updateRotation();

        // 2. Continuous Suction & Damage
        applyPullAndDamage();

        // 3. Client particle spawning at locator positions
        if (this.level().isClientSide) {
            spawnWindParticles();
        }
    }

    private void applyPullAndDamage() {
        double radius = this.getPullRadius();
        Vec3 center = this.position().add(0, this.getBbHeight() / 2.0D, 0);
        AABB pullBox = this.getBoundingBox().inflate(radius);
        List<Entity> targets = this.level().getEntities(this, pullBox);
        Entity owner = this.getOwner();

        for (Entity target : targets) {
            if (target == this) continue;
            if (target.isSpectator()) continue;
            if (target instanceof Player player && (player.isCreative() || player.isSpectator())) continue;

            // Protection check: Do not pull the owner if owner is player or canPullOwner is disabled
            if (target == owner && (!this.canPullOwner() || owner instanceof Player)) {
                continue;
            }

            Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0D, 0);
            double dist = center.distanceTo(targetCenter);
            double maxDist = radius + target.getBbWidth() / 2.0D;
            if (dist > maxDist) continue;

            // Knockback resistance dampens or fully negates pulling
            double kbResist = 0.0;
            if (target instanceof LivingEntity living) {
                kbResist = living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            }
            kbResist = Mth.clamp(kbResist, 0.0, 1.0);

            double factor = (1.0 - (dist / (maxDist + 0.2))) * (1.0 - kbResist);
            factor = Math.max(0.0, factor);

            Vec3 pullDir = center.subtract(targetCenter);
            if (pullDir.lengthSqr() > 0.0001) {
                pullDir = pullDir.normalize();
                double pullMag = this.getPullStrength() * factor;

                // Tangential vortex rotation + inward pull + slight upward lift
                Vec3 swirl = new Vec3(-pullDir.z, 0, pullDir.x).scale(pullMag * 0.4);
                Vec3 motion = pullDir.scale(pullMag).add(swirl).add(0, 0.03 * factor, 0);

                target.setDeltaMovement(target.getDeltaMovement().add(motion));
                target.hasImpulse = true;
                if (target instanceof Player) {
                    target.hurtMarked = true; // Forces client velocity packet sync
                }
            }

            // Continuous damage application on server side
            if (!this.level().isClientSide && target instanceof LivingEntity living && living.isAlive()) {
                if (this.tickCount % this.getDamageInterval() == 0) {
                    living.invulnerableTime = 0; // Reset i-frames so damage ticks reliably
                    living.hurt(this.damageSources().thrown(this, owner), this.getDamage());
                }
            }
        }
    }

    private void spawnWindParticles() {
        Level lvl = this.level();
        if (!lvl.isClientSide) return;

        double cx = this.getX();
        double cy = this.getY();
        double cz = this.getZ();

        // 4-second full loop rotation (90 deg/sec = 4.5 deg/tick)
        float angleWhirl1 = (float) Math.toRadians((this.tickCount * 4.5F) % 360.0F);
        float angleWhirl2 = (float) Math.toRadians((-this.tickCount * 4.5F) % 360.0F);
        float angleWhirl3 = (float) Math.toRadians((this.tickCount * 4.5F) % 360.0F);
        float angleWhirl4 = (float) Math.toRadians((-this.tickCount * 4.5F) % 360.0F);

        // 1. Ground locator: [0, 2/16, 0]
        double groundY = cy + 2.0 / 16.0;
        lvl.addParticle(ParticleTypes.CLOUD,
                cx + (this.random.nextDouble() - 0.5) * 0.3,
                groundY,
                cz + (this.random.nextDouble() - 0.5) * 0.3,
                (this.random.nextDouble() - 0.5) * 0.08,
                0.015,
                (this.random.nextDouble() - 0.5) * 0.08);

        // 2. Whirl 1 locator: pivot [0, 7, -9] -> Y = 7/16, radius = 9/16
        spawnWhirlParticle(lvl, cx, cy, cz, 7.0 / 16.0, 9.0 / 16.0, angleWhirl1 + (float) Math.PI, 1.0F);

        // 3. Whirl 2 locator: pivot [0, 10, 10] -> Y = 10/16, radius = 10/16
        spawnWhirlParticle(lvl, cx, cy, cz, 10.0 / 16.0, 10.0 / 16.0, angleWhirl2, -1.0F);

        // 4. Whirl 3 locator: pivot [12, 14, 0] -> Y = 14/16, radius = 12/16
        spawnWhirlParticle(lvl, cx, cy, cz, 14.0 / 16.0, 12.0 / 16.0, angleWhirl3 + (float) (Math.PI / 2.0), 1.0F);

        // 5. Whirl 4 locator: pivot [-1, 18, -14] -> Y = 18/16, radius ~14/16
        float initAngle4 = (float) Math.atan2(-1.0, -14.0);
        double rad4 = Math.sqrt(1.0 + 196.0) / 16.0;
        spawnWhirlParticle(lvl, cx, cy, cz, 18.0 / 16.0, rad4, angleWhirl4 + initAngle4, -1.0F);

        // Occasional wind gust cloud
        if (this.tickCount % 5 == 0) {
            lvl.addParticle(ParticleTypes.GUST,
                    cx + (this.random.nextDouble() - 0.5) * 0.6,
                    cy + 0.6 + (this.random.nextDouble() - 0.5) * 0.6,
                    cz + (this.random.nextDouble() - 0.5) * 0.6,
                    (this.random.nextDouble() - 0.5) * 0.12,
                    0.04,
                    (this.random.nextDouble() - 0.5) * 0.12);
        }
    }

    private void spawnWhirlParticle(Level lvl, double cx, double cy, double cz, double relY, double radius, float angle, float spinDir) {
        double px = cx + Math.sin(angle) * radius;
        double py = cy + relY;
        double pz = cz + Math.cos(angle) * radius;

        // Outward radial direction
        double outX = Math.sin(angle);
        double outZ = Math.cos(angle);

        // Tangential vortex direction
        double tanX = -Math.cos(angle) * spinDir;
        double tanZ = Math.sin(angle) * spinDir;

        // Slinging velocity: outward radial push + tangential inertia + slight vertical rise
        double vx = outX * 0.12 + tanX * 0.08;
        double vz = outZ * 0.12 + tanZ * 0.08;
        double vy = 0.02 + this.random.nextDouble() * 0.03;

        lvl.addParticle(ParticleTypes.CLOUD, px, py, pz, vx, vy, vz);
    }

    /**
     * Determines client display color based on biome or custom color, with smooth blending.
     */
    public int getClientBiomeColor() {
        if (this.getColor() != 0) {
            return this.getColor();
        }

        if (this.level().isClientSide && (this.tickCount % 4 == 0 || this.clientCurrentColor == 0xFFFFFF)) {
            Holder<Biome> biome = this.level().getBiome(this.blockPosition());
            int target = computeBiomeColor(biome);
            this.clientCurrentColor = blendColor(this.clientCurrentColor, target, 0.25F);
        }
        return this.clientCurrentColor;
    }

    public static int computeBiomeColor(Holder<Biome> biomeHolder) {
        if (biomeHolder == null) return 0xF0F5F8;

        if (biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_RIVER)) {
            return 0x5BB5FF; // Ocean Blue Tint
        }
        if (biomeHolder.is(BiomeTags.IS_BADLANDS) || biomeHolder.is(BiomeTags.IS_SAVANNA) || biomeHolder.is(BiomeTags.HAS_DESERT_PYRAMID)) {
            return 0xE5C37A; // Desert / Arid Golden Tint
        }
        if (biomeHolder.is(BiomeTags.IS_NETHER)) {
            return 0xFF5836; // Nether Ember Flame Tint
        }
        if (biomeHolder.is(BiomeTags.IS_END)) {
            return 0xC578FF; // End Void Purple Tint
        }
        if (biomeHolder.is(BiomeTags.IS_TAIGA) || biomeHolder.is(BiomeTags.IS_FOREST) || biomeHolder.is(BiomeTags.IS_JUNGLE)) {
            return 0x7EAD56; // Forest Nature Foliage Tint
        }
        if (biomeHolder.is(BiomeTags.HAS_IGLOO) || biomeHolder.is(BiomeTags.IS_HILL)) {
            return 0xDDF3FF; // Snowy / Frost White Tint
        }

        Biome biome = biomeHolder.value();
        if (biome.getBaseTemperature() >= 1.0F) {
            return 0xE2C47C;
        } else if (biome.getBaseTemperature() <= 0.15F) {
            return 0xD8F0FF;
        }

        return 0xF0F5F8; // Default Windy Silver-White
    }

    private static int blendColor(int current, int target, float ratio) {
        int r1 = (current >> 16) & 0xFF;
        int g1 = (current >> 8) & 0xFF;
        int b1 = current & 0xFF;

        int r2 = (target >> 16) & 0xFF;
        int g2 = (target >> 8) & 0xFF;
        int b2 = target & 0xFF;

        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (r << 16) | (g << 8) | b;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        super.lerpMotion(x, y, z);
        double d0 = Math.sqrt(x * x + z * z);
        this.setXRot((float)(Mth.atan2(y, d0) * 180.0F / (float)Math.PI));
        this.setYRot((float)(Mth.atan2(x, z) * 180.0F / (float)Math.PI));
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    @Override
    protected void updateRotation() {
        Vec3 vec3 = this.getDeltaMovement();
        double d0 = vec3.horizontalDistance();
        if (d0 > 0.0001D || Math.abs(vec3.y) > 0.0001D) {
            this.setXRot((float)(Mth.atan2(vec3.y, d0) * 180.0F / (float)Math.PI));
            this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * 180.0F / (float)Math.PI));
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Both animations play simultaneously at all times
        controllers.add(new AnimationController<>(this, "sway_controller", 0, state -> {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("tornado_sway"));
            return PlayState.CONTINUE;
        }));
        controllers.add(new AnimationController<>(this, "rotation_controller", 0, state -> {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("tornado_rotation"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
