package net.ganyusbathwater.oririmod.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Thorn Projectile — fired by the Venomous Plant turret.
 *
 * Behaviour:
 *  - Travels in a straight line (inertia = 1.0, no gravity contribution from power).
 *  - On entity hit: deals 5 damage.  25% chance to apply Poison I (5 s / 100 ticks).
 *  - On block hit: discards immediately.
 *  - Uses GeckoLib for the 3-D model + texture.
 *    The animation file is a placeholder (empty loop) so GeckoLib never logs errors.
 */
public class ThornProjectileEntity extends AbstractHurtingProjectile implements GeoEntity {

    // ─── Combat constants ────────────────────────────────────────────────────
    private static final float  DAMAGE        = 5.0f;
    private static final float  POISON_CHANCE = 0.25f;
    /** Poison I for 5 seconds (100 ticks). */
    private static final int    POISON_TICKS  = 100;

    // ─── GeckoLib ────────────────────────────────────────────────────────────
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // ─── Constructors ────────────────────────────────────────────────────────

    /** Required by EntityType.Builder (deserialization). */
    public ThornProjectileEntity(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    /**
     * Spawns a thorn from {@code owner} travelling in {@code direction}.
     * The caller is responsible for setting the initial position via {@link #setPos}.
     */
    public ThornProjectileEntity(Level level, LivingEntity owner, Vec3 direction) {
        super(ModEntities.THORN_PROJECTILE.get(), owner, direction, level);
    }

    // ─── Projectile behaviour ────────────────────────────────────────────────

    /** Thorn does not set targets on fire. */
    @Override
    protected boolean shouldBurn() {
        return false;
    }

    /** No deceleration — flies at constant speed. */
    @Override
    protected float getInertia() {
        return 1.0F;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            Entity  target = result.getEntity();
            Entity  owner  = this.getOwner();

            boolean hit = target.hurt(this.damageSources().thrown(this, owner), DAMAGE);
            if (hit && target instanceof LivingEntity living) {
                // 25 % chance to apply Poison I for 5 seconds
                if (this.random.nextFloat() < POISON_CHANCE) {
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 0));
                }
            }
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

    /** Cannot be picked up by players. */
    @Override
    public boolean isPickable() {
        return false;
    }

    // ─── GeckoLib ────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Idle placeholder loop — keeps the model visible and GeckoLib silent.
        controllers.add(new AnimationController<>(this, "idle_controller", 0, state -> {
            state.getController().setAnimation(
                    RawAnimation.begin().thenLoop("thorn_projectile_idle"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}
