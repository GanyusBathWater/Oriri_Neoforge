package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.particle.ModParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class SporeBlossomEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> DATA_SPAWN_TICKS = SynchedEntityData.defineId(SporeBlossomEntity.class, EntityDataSerializers.INT);
    
    private int spawnTicks = 0;
    private int aliveTicks = 0;

    public SporeBlossomEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D) // 10 hearts
                .add(Attributes.ARMOR, 5.0D) // 5 Defense
                .add(Attributes.MOVEMENT_SPEED, 0.0D) // Stationary
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPAWN_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (this.spawnTicks < 20) {
                this.spawnTicks++;
                this.entityData.set(DATA_SPAWN_TICKS, this.spawnTicks);
            } else {
                this.aliveTicks++;
                
                // Calculate current radius: Starts at 1, +1 every 100 ticks (5 seconds), max 5
                int radius = 1 + (this.aliveTicks / 100);
                if (radius > 5) radius = 5;

                // Apply Poison to nearby entities
                if (this.tickCount % 10 == 0) { // Check every half second to save performance
                    AABB aabb = this.getBoundingBox().inflate(radius, radius, radius);
                    List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, aabb, e -> e.isAlive() && e != this && !(e instanceof DeviartrasEntity) && !(e instanceof SporeBlossomEntity));
                    
                    for (LivingEntity entity : entities) {
                        double distSq = this.distanceToSqr(entity);
                        if (distSq <= radius * radius) {
                            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true)); // 5 seconds of Poison I
                        }
                    }
                }
                
                // Spawn particles randomly in the radius to indicate toxic area
                if (this.level() instanceof ServerLevel serverLevel) {
                    if (this.tickCount % 2 == 0) { // Throttle to every 2 ticks to reduce lag
                        for (int i = 0; i < 1; i++) {
                            double ox = (this.random.nextDouble() - 0.5) * 2.0 * radius;
                            double oz = (this.random.nextDouble() - 0.5) * 2.0 * radius;
                            
                            // Check if it's roughly inside the circular radius
                            if (ox * ox + oz * oz <= radius * radius) {
                                double py = this.getY() + this.random.nextDouble() * 2.0;
                                serverLevel.sendParticles(
                                        ModParticles.DEVIARTRAS_SPORE_PARTICLE.get(),
                                        this.getX() + ox, py, this.getZ() + oz,
                                        1,
                                        0.0, 0.0, 0.0,
                                        0.0
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Immovable Logic ────────────────────────────────────────────────────────
    
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        // Do nothing
    }

    @Override
    public void push(double x, double y, double z) {
        // Do nothing
    }

    @Override
    public void knockback(double strength, double x, double z) {
        // Do nothing
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Invincible during the 1-second spawn animation
        if (this.spawnTicks < 20) {
            return false;
        }
        
        // ── Apply weaknesses ──────────────────────────────────────────────────
        boolean isFire   = source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)
                        || source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE)
                        || source.is(net.minecraft.world.damagesource.DamageTypes.LAVA)
                        || source.is(net.minecraft.world.damagesource.DamageTypes.FIREBALL);
        boolean isWither = source.is(net.minecraft.world.damagesource.DamageTypes.WITHER)
                        || source.is(net.minecraft.world.damagesource.DamageTypes.WITHER_SKULL);

        if (isFire || isWither) {
            amount *= 1.5f;
        }
        
        return super.hurt(source, amount);
    }

    @Override
    public boolean addEffect(MobEffectInstance effectInstance, @javax.annotation.Nullable net.minecraft.world.entity.Entity entity) {
        var type = effectInstance.getEffect();
        if (type == MobEffects.POISON
         || type == MobEffects.WITHER
         || type == MobEffects.MOVEMENT_SLOWDOWN
         || type == MobEffects.BLINDNESS
         || type == MobEffects.WEAKNESS
         || type == MobEffects.LEVITATION) {
            return false; // silently reject — immunity
        }
        return super.addEffect(effectInstance, entity);
    }

    // ── GeckoLib ───────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<SporeBlossomEntity> state) {
        int ticks = this.entityData.get(DATA_SPAWN_TICKS);
        if (ticks < 20) {
            state.getController().setAnimation(RawAnimation.begin().thenPlay("spore_blossom_spawn"));
        } else {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("spore_blossom_idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ── NBT Persistence ────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SpawnTicks", this.spawnTicks);
        tag.putInt("AliveTicks", this.aliveTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SpawnTicks")) {
            this.spawnTicks = tag.getInt("SpawnTicks");
            this.entityData.set(DATA_SPAWN_TICKS, this.spawnTicks);
        }
        if (tag.contains("AliveTicks")) {
            this.aliveTicks = tag.getInt("AliveTicks");
        }
    }
}
