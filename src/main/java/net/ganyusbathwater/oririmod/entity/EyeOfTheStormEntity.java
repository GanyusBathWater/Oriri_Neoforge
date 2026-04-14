package net.ganyusbathwater.oririmod.entity;

import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.ganyusbathwater.oririmod.util.LaserBeamUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class EyeOfTheStormEntity extends Entity {
    private LaserBeamEntity visualCircle;

    public EyeOfTheStormEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public void tick() {
        super.tick();

        // Target duration tracking explicitly server-side
        if (tickCount > 400 && !this.level().isClientSide) {
            this.discard();
            return;
        }

        if (this.level().isClientSide) {
            // Spawn an ultra-dense rotating physical wall of snow exactly at the Safe Region Boundary!
            for (int i = 0; i < 30; i++) {
                double t = this.random.nextDouble() * Math.PI * 2.0;
                double r = 8.0 + this.random.nextDouble() * 0.5; // strictly adhering to the 8-block wall
                double px = this.getX() + r * Math.cos(t);
                double pz = this.getZ() + r * Math.sin(t);
                double py = this.getY() + this.random.nextDouble() * 12.0; // 0 to 12 blocks high wall
                
                double vx = -0.5 * Math.sin(t); // tangental spin velocity sweeping
                double vz = 0.5 * Math.cos(t);
                
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, px, py, pz, vx, -0.1, vz);
                if (i % 2 == 0) {
                    this.level().addParticle(net.minecraft.core.particles.ParticleTypes.WHITE_ASH, px, py, pz, vx, 0.05, vz); // Ash makes dense fog
                }
            }
        }

        // Spawn magic circle on ground beneath us strictly on Tick 1 (Server only)
        if (tickCount == 1 && !this.level().isClientSide) {
            // Anchor 8-block radius safe zone
            // radius = width * 3.8
            // 8.0 = width * 3.8 -> width = 8.0 / 3.8 = 2.105f
            LaserBeamUtil.LaserBeamConfig config = new LaserBeamUtil.LaserBeamConfig(
                    this.position(), this.position(), 2.105f, 0xFF_FFFFFF, 400, 0f, 0, -1, 0, true
            );
            this.visualCircle = LaserBeamUtil.unleash((net.minecraft.server.level.ServerLevel) this.level(), config);
            if (this.visualCircle != null) {
                this.visualCircle.setSilent(true);
                this.visualCircle.setCoreHidden(true);
                this.visualCircle.setUseWaterCircle(true); 
            }
        }

        double maxRadius = 64.0;
        double safeRadius = 8.0;

        AABB stormBox = this.getBoundingBox().inflate(maxRadius);
        // Expand the target list to hit ALL LivingEntities (Players and Mobs!)
        List<net.minecraft.world.entity.LivingEntity> exposedEntities = this.level().getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class, stormBox, 
                e -> !(e instanceof Player p && (p.isSpectator() || p.isCreative())) && e.isAlive()
        );

        for (net.minecraft.world.entity.LivingEntity e : exposedEntities) {
            double dx = e.getX() - this.getX();
            double dz = e.getZ() - this.getZ();
            double distSq = dx * dx + dz * dz;
            
            if (distSq > (safeRadius * safeRadius) && distSq <= (maxRadius * maxRadius)) {
                // Flag them firmly inside the blizzard immediately!
                // Since this runs on client-side too, the local client's Player natively gets their HUD variable assigned!
                e.getPersistentData().putLong("LastBlizzardTick", e.level().getGameTime());

                if (!this.level().isClientSide) {
                    // The user requested 1 stack per second! Meaning +1 amplifier every 20 ticks.
                    int blizzTicks = e.getPersistentData().getInt("ColdAuraExposureTicks");
                    blizzTicks++;
                    
                    if (blizzTicks >= 20) {
                        blizzTicks = 0;
                        MobEffectInstance existing = e.getEffect(ModEffects.COLD_AURA_EFFECT);
                        int nextAmp = existing != null ? existing.getAmplifier() + 1 : 0;
                        
                        e.removeEffect(ModEffects.COLD_AURA_EFFECT); // Pull down to reapply natively
                        e.addEffect(new MobEffectInstance(ModEffects.COLD_AURA_EFFECT, 200, nextAmp, false, false, true));
                    }
                    
                    e.getPersistentData().putInt("ColdAuraExposureTicks", blizzTicks);
                }
            } else if (distSq <= (safeRadius * safeRadius)) {
                // Inside Safe Zone! Flag them so the Client HUD knows they are looking AT the storm safely
                e.getPersistentData().putLong("LastBlizzardSafeZoneTick", e.level().getGameTime());

                if (!this.level().isClientSide) {
                    // In safe zone, reset stacking buildup timer organically.
                    e.getPersistentData().putInt("ColdAuraExposureTicks", 0);
                }
            }
        }
    }
    
    @Override
    public void remove(RemovalReason reason) {
        if (this.visualCircle != null && !this.visualCircle.isRemoved()) {
            this.visualCircle.discard();
        }
        super.remove(reason);
    }
}
