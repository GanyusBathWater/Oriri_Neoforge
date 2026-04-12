package net.ganyusbathwater.oririmod.entity.custom;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class FireZombieEntity extends Zombie {

    /** How many ticks remain before the post-death explosion fires. -1 = alive. */
    private int fuseTimer = -1;
    private DamageSource deathCause;

    public FireZombieEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        // Standard zombie goals but with Creative/Spectator check for players
        this.goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0D, false, 4, this::canBreakDoors));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, new Class[0])).setAlertOthers(new Class[]{ZombifiedPiglin.class}));
        
        // Explicitly ignore creative/spectator players
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                p -> p instanceof Player player && !player.isCreative() && !player.isSpectator()));
        
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isOnFire() {
        // We override this because the vanilla Entity.isOnFire() returns false if fireImmune() is true.
        // Using the public getter instead of the private field to avoid compilation errors.
        return this.isAlive() && (this.getRemainingFireTicks() > 0 || this.level().isClientSide);
    }

    @Override
    public boolean isSunSensitive() {
        return false;
    }

    @Override
    public void tick() {
        // ─── Visual Burning effect (Client & Server) ───────────────────────────────
        // We set it to a small value every tick so it remains visually on fire
        // without necessarily being "on fire" in a way that creates lots of particles/sounds every tick.
        if (this.isAlive()) {
            this.setRemainingFireTicks(20);
        }

        if (!this.level().isClientSide) {
            if (fuseTimer >= 0) {
                // Stand still while the fuse burns
                this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
                
                if (fuseTimer > 0) {
                    fuseTimer--;
                } else {
                    // Explode!
                    this.level().explode(
                            this,
                            this.getX(), this.getY(), this.getZ(),
                            3.0F,
                            false,
                            Level.ExplosionInteraction.MOB
                    );
                    
                    // Now actually die to trigger loot and sound
                    super.die(deathCause != null ? deathCause : this.damageSources().generic());
                    this.discard();
                    return;
                }
            }
        }
        
        super.tick();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Don't take damage while waiting to explode
        if (this.fuseTimer >= 0) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource cause) {
        // Catch the first death call to start the fuse
        if (this.fuseTimer < 0) {
            this.fuseTimer = 30; // 1.5 seconds fuse
            this.deathCause = cause;
            this.setNoAi(true);
            
            // We set health specifically to a tiny value instead of 0 
            // to keep the entity from entering the Pose.DYING (falling over) 
            // state until we are ready for it.
            this.setHealth(0.1f);
        } else {
            // This will be called when we call super.die() in tick()
            super.die(cause);
        }
    }
}
