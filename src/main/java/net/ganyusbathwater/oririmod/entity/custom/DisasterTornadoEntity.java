package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.config.OririConfig;
import net.ganyusbathwater.oririmod.events.disaster.DisasterSafetyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Natural Disaster Tornado entity.
 *
 * <p>Wanders across terrain sticking to the ground surface, ripping up soft vegetation and
 * foliage (up to a hardness cap), leaving muddy eroded trails, and spiraling up vulnerable
 * mobs and players lacking Knockback Resistance.
 *
 * <p>Rendered with a procedural, shader-compatible twisting vortex pipeline with orbiting
 * 3D terrain debris (no rigid 3D model).
 */
public class DisasterTornadoEntity extends Entity {

    private static final EntityDataAccessor<Float> SUCTION_RADIUS = SynchedEntityData.defineId(DisasterTornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FUNNEL_HEIGHT = SynchedEntityData.defineId(DisasterTornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BASE_RADIUS = SynchedEntityData.defineId(DisasterTornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TOP_RADIUS = SynchedEntityData.defineId(DisasterTornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DEBRIS_BLOCK_1 = SynchedEntityData.defineId(DisasterTornadoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DEBRIS_BLOCK_2 = SynchedEntityData.defineId(DisasterTornadoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DEBRIS_BLOCK_3 = SynchedEntityData.defineId(DisasterTornadoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DEBRIS_BLOCK_4 = SynchedEntityData.defineId(DisasterTornadoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DISSIPATING = SynchedEntityData.defineId(DisasterTornadoEntity.class, EntityDataSerializers.BOOLEAN);

    private int lifespan = 2400; // 2 minutes default
    private double headingAngle = 0.0;
    private double moveSpeed = 0.18;
    private boolean destroyBlocks = true;
    private boolean ignoreSafety = false;
    private long safetyThreshold = 6000L;
    private float maxHardness = 1.5f;
    private int dissipateTicks = 0;
    private float initialFunnelHeight = -1.0f;
    private float initialBaseRadius = -1.0f;
    private float initialTopRadius = -1.0f;
    private float initialSuctionRadius = -1.0f;

    private int debrisIndex = 0;

    public DisasterTornadoEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SUCTION_RADIUS, 36.0f);
        builder.define(FUNNEL_HEIGHT, 115.0f);
        builder.define(BASE_RADIUS, 8.5f);
        builder.define(TOP_RADIUS, 45.0f);
        builder.define(DEBRIS_BLOCK_1, Block.getId(Blocks.OAK_LEAVES.defaultBlockState()));
        builder.define(DEBRIS_BLOCK_2, Block.getId(Blocks.DIRT.defaultBlockState()));
        builder.define(DEBRIS_BLOCK_3, Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
        builder.define(DEBRIS_BLOCK_4, Block.getId(Blocks.OAK_PLANKS.defaultBlockState()));
        builder.define(DISSIPATING, false);
    }

    public void configure(int lifespan, float suctionRadius, float funnelHeight, float baseRadius,
                          float topRadius, boolean destroyBlocks, boolean ignoreSafety,
                          long safetyThreshold, float maxHardness) {
        this.lifespan = lifespan;
        this.destroyBlocks = destroyBlocks;
        this.ignoreSafety = ignoreSafety;
        this.safetyThreshold = safetyThreshold;
        this.maxHardness = maxHardness;
        this.headingAngle = random.nextDouble() * Math.PI * 2.0;

        this.entityData.set(SUCTION_RADIUS, suctionRadius);
        this.entityData.set(FUNNEL_HEIGHT, funnelHeight);
        this.entityData.set(BASE_RADIUS, baseRadius);
        this.entityData.set(TOP_RADIUS, topRadius);
    }

    @Override
    public void tick() {
        super.tick();

        if (isDissipating()) {
            if (initialFunnelHeight < 0) {
                initialFunnelHeight = getFunnelHeight();
                initialBaseRadius = getBaseRadius();
                initialTopRadius = getTopRadius();
                initialSuctionRadius = getSuctionRadius();
            }
            dissipateTicks++;
            float factor = Math.max(0.0f, 1.0f - (dissipateTicks / 25.0f));
            if (!level().isClientSide) {
                this.entityData.set(FUNNEL_HEIGHT, initialFunnelHeight * factor);
                this.entityData.set(BASE_RADIUS, initialBaseRadius * factor);
                this.entityData.set(TOP_RADIUS, initialTopRadius * factor);
                this.entityData.set(SUCTION_RADIUS, initialSuctionRadius * factor);
                if (dissipateTicks >= 25) {
                    this.discard();
                    return;
                }
            }
            if (level().isClientSide && factor > 0.05f) {
                spawnClientFootParticles();
            }
            return;
        }

        if (level().isClientSide) {
            spawnClientFootParticles();
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level();

        if (this.tickCount >= this.lifespan) {
            startDissipating();
            return;
        }

        // Automatic disaster sync check: if tornado event was stopped, dissipate immediately
        if (this.tickCount % 10 == 0) {
            net.ganyusbathwater.oririmod.events.disaster.NaturalDisasterManager mgr =
                    net.ganyusbathwater.oririmod.events.disaster.NaturalDisasterManager.get(serverLevel.getServer().overworld());
            if (mgr.getActiveType() != net.ganyusbathwater.oririmod.events.disaster.NaturalDisasterType.TORNADO) {
                startDissipating();
                return;
            }
        }

        // 1. Wandering & Ground Surface Tracking
        wanderAcrossTerrain(serverLevel);

        // 2. Selective Hardness-Capped Block Destruction
        if (destroyBlocks) {
            performSelectiveBlockStripping(serverLevel);
        }

        // 3. Suction Physics & Knockback Resistance Checking
        applySuctionPhysics(serverLevel);

        // 4. Roaring Wind Sounds & Ambient Spark Lightning
        if (this.tickCount % 10 == 0) {
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ELYTRA_FLYING, SoundSource.WEATHER, 4.0f, 0.5f + random.nextFloat() * 0.2f);
        }
        if (this.tickCount % 45 == 0) {
            serverLevel.playSound(null, getX(), getY() + 20, getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.8f, 0.4f);
            for (int s = 0; s < 12; s++) {
                double sy = getY() + random.nextDouble() * getFunnelHeight();
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        getX() + (random.nextDouble() - 0.5) * 8.0,
                        sy,
                        getZ() + (random.nextDouble() - 0.5) * 8.0,
                        2, 0.1, 0.1, 0.1, 0.1);
            }
        }
    }

    private void wanderAcrossTerrain(ServerLevel level) {
        // Gently adjust drift direction over time
        if (tickCount % 25 == 0) {
            headingAngle += (random.nextDouble() - 0.5) * 0.45;
        }

        double nextX = getX() + Math.cos(headingAngle) * moveSpeed;
        double nextZ = getZ() + Math.sin(headingAngle) * moveSpeed;

        BlockPos targetPos = new BlockPos((int) Math.floor(nextX), (int) getY(), (int) Math.floor(nextZ));
        if (!level.isPositionEntityTicking(targetPos)) {
            headingAngle += Math.PI; // Deflect back toward active chunks
            return;
        }

        // Sanctuary boundary check: if approaching player bases, turn around!
        if (!ignoreSafety && !DisasterSafetyUtil.isImpactSiteFresh(level, targetPos, safetyThreshold)) {
            headingAngle += Math.PI * 0.75 + random.nextDouble() * 0.5;
            return;
        }

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(nextX), (int) Math.floor(nextZ));
        double targetY = surfaceY;

        // Smooth vertical climbing
        double curY = getY();
        double lerpY = Mth.lerp(0.2, curY, targetY);

        this.setPos(nextX, lerpY, nextZ);
    }

    private void performSelectiveBlockStripping(ServerLevel level) {
        float suctionRad = getSuctionRadius();
        float stripRad = Math.min(18.0f, suctionRad * 0.5f);

        // Budgeted destruction: 4 to 8 blocks per tick
        int budget = 4 + random.nextInt(5);
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < budget; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = random.nextDouble() * stripRad;
            int bx = (int) Math.floor(getX() + Math.cos(angle) * dist);
            int bz = (int) Math.floor(getZ() + Math.sin(angle) * dist);

            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
            int motionY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);

            // Strip leaves and foliage from tree canopies, or strip surface ground
            int pickY = groundY - 1;
            if (motionY > groundY && random.nextFloat() < 0.65f) {
                pickY = groundY + random.nextInt(motionY - groundY + 1);
            }
            mpos.set(bx, pickY, bz);

            BlockState state = level.getBlockState(mpos);
            if (state.isAir() || state.hasBlockEntity()) {
                continue;
            }

            float destroySpeed = state.getDestroySpeed(level, mpos);
            // Never break bedrock/dungeons, and respect hardness cap (only soft blocks like leaves, wood, dirt, glass)
            if (destroySpeed < 0 || destroySpeed > maxHardness) {
                continue;
            }

            // Muddy trail scar: turn grass/podzol into dirt
            if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL)) {
                if (random.nextFloat() < 0.65f) {
                    BlockState mud = random.nextBoolean() ? Blocks.DIRT.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
                    level.setBlock(mpos, mud, Block.UPDATE_CLIENTS);
                    recordDebrisBlock(mud);
                }
                continue;
            }

            // Tear off foliage, crops, leaves, sand, wood
            recordDebrisBlock(state);
            level.destroyBlock(mpos, false, this);
        }
    }

    private void recordDebrisBlock(BlockState state) {
        int id = Block.getId(state);
        switch (debrisIndex % 4) {
            case 0 -> this.entityData.set(DEBRIS_BLOCK_1, id);
            case 1 -> this.entityData.set(DEBRIS_BLOCK_2, id);
            case 2 -> this.entityData.set(DEBRIS_BLOCK_3, id);
            case 3 -> this.entityData.set(DEBRIS_BLOCK_4, id);
        }
        debrisIndex++;
    }

    private void applySuctionPhysics(ServerLevel level) {
        float radius = getSuctionRadius();
        double kbThreshold = OririConfig.COMMON.naturalDisasters.tornadoKnockbackResistanceThreshold.get();

        AABB suctionBox = this.getBoundingBox().inflate(radius, getFunnelHeight() * 0.5, radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, suctionBox,
                e -> e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));

        for (LivingEntity entity : targets) {
            double dx = getX() - entity.getX();
            double dz = getZ() - entity.getZ();
            double distSq = dx * dx + dz * dz;

            if (distSq > radius * radius || distSq < 0.05) {
                continue;
            }
            double dist = Math.sqrt(distSq);

            // Knockback Resistance check
            double kbRes = entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            if (kbRes >= kbThreshold) {
                // Heavy / resistant entities resist lift; experience only mild horizontal wind resistance
                Vec3 drag = new Vec3(dx, 0, dz).normalize().scale(0.06);
                entity.push(drag.x, 0, drag.z);
                continue;
            }

            // Vulnerable entities are pulled into the vortex spiral
            double vulnerability = 1.0 - (kbRes / kbThreshold);
            double pullFactor = vulnerability * (1.0 - (dist / radius));

            // Tangential swirl vector (counter-clockwise)
            double tx = -dz / dist;
            double tz = dx / dist;

            Vec3 inward = new Vec3(dx / dist, 0, dz / dist).scale(0.65 * pullFactor);
            Vec3 swirl = new Vec3(tx, 0, tz).scale(0.80 * pullFactor);

            // Upward vortex lift inside the funnel
            double lift = 0.0;
            if (entity.getY() < getY() + 30.0) {
                lift = 0.15 + 0.14 * pullFactor;
            } else if (entity.getY() < getY() + getFunnelHeight() * 0.75) {
                lift = 0.08;
            } else {
                lift = -0.15; // Expelled outwards at the high funnel mouth
            }

            entity.resetFallDistance();

            if (entity instanceof ServerPlayer sp) {
                sp.push(inward.x + swirl.x, lift, inward.z + swirl.z);
                sp.hurtMarked = true;
                sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
            } else {
                Vec3 curMotion = entity.getDeltaMovement();
                entity.setDeltaMovement(
                        curMotion.x * 0.7 + inward.x + swirl.x,
                        lift,
                        curMotion.z * 0.7 + inward.z + swirl.z
                );
                entity.hurtMarked = true;
            }

            // Violent turbulence damage near the inner vortex core
            if (dist < 4.0 && tickCount % 20 == 0) {
                entity.hurt(entity.damageSources().flyIntoWall(), 3.5f);
            }
        }
    }

    private void spawnClientFootParticles() {
        float baseR = getBaseRadius();
        float suctionR = getSuctionRadius();

        // 1. Vast extended swirling dust & debris ring (visualizes ground being torn apart up to 45 blocks out)
        for (int i = 0; i < 35; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double r = 4.0 + random.nextDouble() * (suctionR * 1.15);
            double px = getX() + Math.cos(angle) * r;
            double pz = getZ() + Math.sin(angle) * r;
            double py = getY() + random.nextDouble() * 2.5;

            // Inward spiral velocity
            double distRatio = Math.max(0.1, r / suctionR);
            double speed = (1.2 - distRatio * 0.6);
            double vx = -Math.sin(angle) * speed + (getX() - px) * 0.03;
            double vz = Math.cos(angle) * speed + (getZ() - pz) * 0.03;
            double vy = 0.15 + (1.0 - distRatio) * 0.45;

            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, vx, vy, vz);
            level().addParticle(ParticleTypes.POOF, px, py, pz, vx * 1.1, vy * 0.8, vz * 1.1);

            // Flying block rubble torn from the ground
            if (i % 2 == 0) {
                BlockState debris = getDebrisState(i);
                level().addParticle(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, debris),
                        px, py, pz, vx * 1.3, vy * 1.2, vz * 1.3);
            }
        }

        // 2. High ascending funnel vortex particles
        for (int i = 0; i < 15; i++) {
            float hRatio = random.nextFloat();
            float py = (float) getY() + hRatio * (getFunnelHeight() * 0.7f);
            float radAtY = baseR + (getTopRadius() - baseR) * (float) Math.pow(hRatio, 1.55);
            double angle = random.nextDouble() * Math.PI * 2.0;
            double px = getX() + Math.cos(angle) * radAtY;
            double pz = getZ() + Math.sin(angle) * radAtY;

            double vx = -Math.sin(angle) * 1.4;
            double vz = Math.cos(angle) * 1.4;
            level().addParticle(ParticleTypes.LARGE_SMOKE, px, py, pz, vx, 0.3, vz);
            if (i % 4 == 0) {
                level().addParticle(ParticleTypes.CLOUD, px, py, pz, vx * 0.7, 0.2, vz * 0.7);
            }
        }
    }

    public float getSuctionRadius() { return this.entityData.get(SUCTION_RADIUS); }
    public float getFunnelHeight() { return this.entityData.get(FUNNEL_HEIGHT); }
    public float getBaseRadius() { return this.entityData.get(BASE_RADIUS); }
    public float getTopRadius() { return this.entityData.get(TOP_RADIUS); }

    public void startDissipating() {
        this.entityData.set(DISSIPATING, true);
    }

    public boolean isDissipating() {
        return this.entityData.get(DISSIPATING);
    }

    public BlockState getDebrisState(int slot) {
        int id = switch (slot % 4) {
            case 0 -> this.entityData.get(DEBRIS_BLOCK_1);
            case 1 -> this.entityData.get(DEBRIS_BLOCK_2);
            case 2 -> this.entityData.get(DEBRIS_BLOCK_3);
            default -> this.entityData.get(DEBRIS_BLOCK_4);
        };
        BlockState state = Block.stateById(id);
        return state.isAir() ? Blocks.OAK_LEAVES.defaultBlockState() : state;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifespan = tag.getInt("Lifespan");
        this.headingAngle = tag.getDouble("Heading");
        this.moveSpeed = tag.getDouble("Speed");
        this.destroyBlocks = tag.getBoolean("DestroyBlocks");
        this.ignoreSafety = tag.getBoolean("IgnoreSafety");
        this.safetyThreshold = tag.getLong("SafetyThreshold");
        this.maxHardness = tag.getFloat("MaxHardness");
        if (tag.contains("SuctionRadius")) this.entityData.set(SUCTION_RADIUS, tag.getFloat("SuctionRadius"));
        if (tag.contains("FunnelHeight")) this.entityData.set(FUNNEL_HEIGHT, tag.getFloat("FunnelHeight"));
        if (tag.contains("BaseRadius")) this.entityData.set(BASE_RADIUS, tag.getFloat("BaseRadius"));
        if (tag.contains("TopRadius")) this.entityData.set(TOP_RADIUS, tag.getFloat("TopRadius"));
        if (tag.contains("Dissipating")) this.entityData.set(DISSIPATING, tag.getBoolean("Dissipating"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifespan", this.lifespan);
        tag.putDouble("Heading", this.headingAngle);
        tag.putDouble("Speed", this.moveSpeed);
        tag.putBoolean("DestroyBlocks", this.destroyBlocks);
        tag.putBoolean("IgnoreSafety", this.ignoreSafety);
        tag.putLong("SafetyThreshold", this.safetyThreshold);
        tag.putFloat("MaxHardness", this.maxHardness);
        tag.putFloat("SuctionRadius", getSuctionRadius());
        tag.putFloat("FunnelHeight", getFunnelHeight());
        tag.putFloat("BaseRadius", getBaseRadius());
        tag.putFloat("TopRadius", getTopRadius());
        tag.putBoolean("Dissipating", isDissipating());
    }
}

