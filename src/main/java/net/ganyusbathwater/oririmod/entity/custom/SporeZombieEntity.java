package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.damage.ModDamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
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

public class SporeZombieEntity extends Zombie {

    public SporeZombieEntity(EntityType<? extends Zombie> type, Level level) {
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

    // ─── On death: spawn a lingering poison cloud unless killed by fire ──────────

    @Override
    public void die(DamageSource cause) {
        super.die(cause);

        if (this.level().isClientSide) return;

        // Suppress the cloud if killed by vanilla fire damage OR the mod's
        // elemental-fire damage type (Element.FIRE weapons / enchantment).
        boolean killedByFire = cause.is(DamageTypeTags.IS_FIRE);
        boolean killedByElementFire = cause.is(ModDamageTypes.ELEMENT_FIRE); // ResourceKey<DamageType>

        if (killedByFire || killedByElementFire) return;

        // Spawn a lingering poison area-effect cloud
        AreaEffectCloud cloud = new AreaEffectCloud(this.level(),
                this.getX(), this.getY(), this.getZ());
        cloud.setRadius(3.0F);
        cloud.setRadiusOnUse(-0.5F);          // shrinks after each use
        cloud.setWaitTime(10);                 // 10 ticks before first application
        cloud.setDuration(200);                // 10 s total lifetime
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration()); // fade over lifetime
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 70, 0)); // Poison I ~3.5 s per hit
        cloud.setOwner(this);
        this.level().addFreshEntity(cloud);
    }
}
