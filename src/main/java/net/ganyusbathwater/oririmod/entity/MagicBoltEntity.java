package net.ganyusbathwater.oririmod.entity;

import net.ganyusbathwater.oririmod.util.MagicBoltAbility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MagicBoltEntity extends ThrowableItemProjectile {
    private static final float BASE_DAMAGE = 6.0F;
    private static final EntityDataAccessor<Integer> ABILITY =
            SynchedEntityData.defineId(MagicBoltEntity.class, EntityDataSerializers.INT);

    public MagicBoltEntity(EntityType<? extends MagicBoltEntity> type, Level level) {
        super(type, level);
    }

    public MagicBoltEntity(Level level, LivingEntity owner) {
        super(ModEntities.MAGIC_BOLT.get(), owner, level);
        setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ABILITY, MagicBoltAbility.NORMAL.ordinal());
    }

    public void setAbility(MagicBoltAbility ability) {
        entityData.set(ABILITY, ability.ordinal());
    }

    public MagicBoltAbility getAbility() {
        return MagicBoltAbility.fromId(entityData.get(ABILITY));
    }

    public void updateGravityFlag() {
        setNoGravity(getAbility() != MagicBoltAbility.EXPLOSIVE);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AMETHYST_SHARD;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            switch (getAbility()) {
                case BLAZE -> spawnTrail(ParticleTypes.SMALL_FLAME, 2);
                case EXPLOSIVE -> spawnTrail(ParticleTypes.CRIT, 1);
                case ENDER -> spawnTrail(ParticleTypes.PORTAL, 3);
                case SONIC -> spawnTrail(ParticleTypes.SONIC_BOOM, 1);
                default -> {}
            }
        }
    }

    private void spawnTrail(net.minecraft.core.particles.ParticleOptions type, int count) {
        for (int i = 0; i < count; i++) {
            level().addParticle(type, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override
    protected double getDefaultGravity() {
        return getAbility() == MagicBoltAbility.EXPLOSIVE ? 0.05D : 0.0D;
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level().isClientSide) return;

        MagicBoltAbility ability = getAbility();
        Vec3 loc = hit.getLocation();

        switch (ability) {
            case EXPLOSIVE -> {
                level().playSound(null, loc.x, loc.y, loc.z,
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.0F);
                level().explode(this, loc.x, loc.y, loc.z, 3.0F, false, Level.ExplosionInteraction.MOB);
            }
            case ENDER -> {
                LivingEntity owner = getOwner() instanceof LivingEntity le ? le : null;
                if (owner != null && owner.level() == level()) {
                    owner.teleportTo(loc.x, loc.y, loc.z);
                    owner.resetFallDistance();
                    owner.hurt(damageSources().fall(), 5.0F);
                    level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                            SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
                }
            }
            case SONIC -> {
                level().playSound(null, loc.x, loc.y, loc.z,
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 1.0F);
                level().addParticle(ParticleTypes.SONIC_BOOM, loc.x, loc.y, loc.z, 0, 0, 0);
            }
            default -> {}
        }
        discard();
    }

    public void launchStraight(LivingEntity shooter, float speed) {
        Vec3 look = shooter.getLookAngle().normalize();
        if (getAbility() == MagicBoltAbility.EXPLOSIVE) speed *= 0.85F;
        setDeltaMovement(look.x * speed, look.y * speed, look.z * speed);
        setYRot(shooter.getYRot());
        setXRot(shooter.getXRot());
        xo = getX();
        yo = getY();
        zo = getZ();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide) return;

        LivingEntity owner = getOwner() instanceof LivingEntity le ? le : null;
        LivingEntity target = hit.getEntity() instanceof LivingEntity le ? le : null;
        DamageSources sources = damageSources();

        MagicBoltAbility ability = getAbility();
        float damage = switch (ability) {
            case SONIC -> 10.0F;
            case BLAZE -> BASE_DAMAGE - 1.0F;
            case NORMAL -> BASE_DAMAGE;
            case ENDER -> 0.0F;
            case EXPLOSIVE -> 0.0F;
        };

        if (damage > 0 && hit.getEntity().isAlive()) {
            DamageSource ds = switch (ability) {
                case SONIC -> sources.sonicBoom(this);
                default -> (owner != null ? sources.thrown(this, owner) : sources.generic());
            };
            hit.getEntity().hurt(ds, damage);
        }

        switch (ability) {
            case BLAZE -> {
                hit.getEntity().setRemainingFireTicks(40); // 2 Sekunden brennen
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.6F, 1.2F);
                level().addParticle(ParticleTypes.FLAME, hit.getLocation().x, hit.getLocation().y,
                        hit.getLocation().z, 0, 0, 0);
            }
            case SONIC -> {
                Vec3 push = getDeltaMovement().normalize().scale(1.2D);
                hit.getEntity().push(push.x, 0.2D, push.z);
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 1.0F);
                level().addParticle(ParticleTypes.SONIC_BOOM, hit.getLocation().x, hit.getLocation().y,
                        hit.getLocation().z, 0, 0, 0);
            }
            default -> {}
        }

        if (ability != MagicBoltAbility.EXPLOSIVE) {
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Ability", getAbility().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Ability")) {
            setAbility(MagicBoltAbility.fromId(tag.getInt("Ability")));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return super.getAddEntityPacket(serverEntity);
    }
}