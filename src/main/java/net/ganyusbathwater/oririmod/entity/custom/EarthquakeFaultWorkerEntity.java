package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.events.disaster.DisasterSafetyUtil;
import net.ganyusbathwater.oririmod.events.disaster.EarthquakeNoiseUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Worker entity that carves an organic, non-repeating earthquake fissure across the landscape.
 *
 * <p>Uses 2D/3D fractional Brownian motion to dynamically warp the crack trajectory and
 * overhang the subterranean walls so they never look like artificial vertical trenches.
 *
 * <p>Carving is strictly tick-sliced (1-2 blocks of crack advance per tick) to prevent
 * server lag, respect safety areas, and progressively stream the opening fissure to players.
 */
public class EarthquakeFaultWorkerEntity extends Entity {

    private static final EntityDataAccessor<Float> SHAKE_RADIUS = SynchedEntityData.defineId(EarthquakeFaultWorkerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_CARVING = SynchedEntityData.defineId(EarthquakeFaultWorkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockPos> TIP_POS = SynchedEntityData.defineId(EarthquakeFaultWorkerEntity.class, EntityDataSerializers.BLOCK_POS);

    private double currentX;
    private double currentZ;
    private double currentAzimuth;
    private float currentArclength = 0f;
    private float targetLength = 60f;
    private float maxWidth = 3.6f;
    private int maxDepth = 16;
    private boolean destroyBlocks = true;
    private boolean ignoreSafety = false;
    private long safetyThreshold = 6000L;
    private long noiseSeed = 0L;
    private boolean isBranch = false;
    private boolean hasSpawnedBranch = false;

    private EarthquakeNoiseUtil noise;

    public EarthquakeFaultWorkerEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SHAKE_RADIUS, 48.0f);
        builder.define(IS_CARVING, true);
        builder.define(TIP_POS, BlockPos.ZERO);
    }

    public void configure(double startX, double startZ, double initialAzimuth, float targetLength,
                          int maxDepth, float maxWidth, boolean destroyBlocks, boolean ignoreSafety,
                          long safetyThreshold, long seed, boolean isBranch) {
        this.currentX = startX;
        this.currentZ = startZ;
        this.currentAzimuth = initialAzimuth;
        this.targetLength = targetLength;
        this.maxDepth = maxDepth;
        this.maxWidth = maxWidth;
        this.destroyBlocks = destroyBlocks;
        this.ignoreSafety = ignoreSafety;
        this.safetyThreshold = safetyThreshold;
        this.noiseSeed = seed;
        this.isBranch = isBranch;
        this.noise = new EarthquakeNoiseUtil(seed);

        this.setPos(startX, level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) startX, (int) startZ), startZ);
        this.entityData.set(TIP_POS, blockPosition());
    }

    @Override
    public void tick() {
        super.tick();

        if (this.noise == null) {
            this.noise = new EarthquakeNoiseUtil(this.noiseSeed != 0 ? this.noiseSeed : this.getId() * 31L);
        }

        if (level().isClientSide) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level();

        // If finished carving, linger for 2.5s (50 ticks) so camera rumble and smoke fade out smoothly
        if (currentArclength >= targetLength) {
            this.entityData.set(IS_CARVING, false);
            if (this.tickCount > (int) (targetLength / 1.5f) + 50) {
                this.discard();
            }
            return;
        }

        // Advance arclength by 2.4 blocks per tick
        float step = 2.4f;
        advanceFissure(serverLevel, step);
    }

    private void advanceFissure(ServerLevel level, float step) {
        // Warp heading angle using multi-octave noise
        double noiseAngle = noise.fbm2D(currentX * 0.04, currentZ * 0.04) * 0.65
                + noise.noise2D(currentX * 0.12 + 42.1, currentZ * 0.12 - 19.3) * 0.35;
        double heading = currentAzimuth + noiseAngle;

        double nextX = currentX + Math.cos(heading) * step;
        double nextZ = currentZ + Math.sin(heading) * step;

        currentX = nextX;
        currentZ = nextZ;
        currentArclength += step;

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(currentX), (int) Math.floor(currentZ));
        BlockPos tipPos = new BlockPos((int) Math.floor(currentX), surfaceY, (int) Math.floor(currentZ));
        this.entityData.set(TIP_POS, tipPos);
        this.setPos(currentX, surfaceY, currentZ);

        // Safety & boundary check: if approaching inhabited land or unloaded chunks, stop immediately
        if (!level.isPositionEntityTicking(tipPos)) {
            currentArclength = targetLength;
            return;
        }
        if (!ignoreSafety && !DisasterSafetyUtil.isImpactSiteFresh(level, tipPos, safetyThreshold)) {
            currentArclength = targetLength; // Gracefully terminate at sanctuary border
            return;
        }

        // Water check: only abort if entering deep ocean or deep lake
        if (level.getFluidState(tipPos).is(net.minecraft.tags.FluidTags.WATER)
                && level.getFluidState(tipPos.below(3)).is(net.minecraft.tags.FluidTags.WATER)) {
            currentArclength = targetLength;
            return;
        }

        // Spawn a natural secondary branch crack at ~45% of spine length (only once, main spine only)
        if (!isBranch && !hasSpawnedBranch && currentArclength >= targetLength * 0.45f) {
            hasSpawnedBranch = true;
            spawnBranchCrack(level, heading);
        }

        // Tapered envelope for width and depth
        float progress = Math.min(1.0f, currentArclength / targetLength);
        float envelope = (float) Math.sin(progress * Math.PI); // 0.0 -> 1.0 -> 0.0
        float widthNoise = 0.75f + (float) noise.noise2D(currentX * 0.18, currentZ * 0.18) * 0.45f;
        float actualWidth = Math.max(1.0f, maxWidth * envelope * widthNoise);

        float depthEnvelope = (float) Math.pow(Math.sin(progress * Math.PI), 0.55);
        int actualDepth = Math.max(10, (int) (maxDepth * depthEnvelope * (0.9f + (float) noise.noise2D(currentX * 0.1, currentZ * 0.1) * 0.3f)));

        if (destroyBlocks) {
            carveFissureSegment(level, currentX, currentZ, surfaceY, actualWidth, actualDepth);
        }

        // Ground effects: sound, camera rumble cues, particles, and entity stagger
        playRumbleEffects(level, tipPos, actualWidth, actualDepth);
    }

    private void carveFissureSegment(ServerLevel level, double cx, double cz, int surfaceY, float width, int depth) {
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        int reach = (int) Math.ceil(width / 2.0f) + 2;

        int bottomY = Math.max(level.getMinBuildHeight() + 5, surfaceY - depth);
        int topY = Math.min(level.getMaxBuildHeight() - 1, surfaceY + 2); // Clear vegetation/snow above surface

        for (int y = topY; y >= bottomY; y--) {
            float depthFactor = (surfaceY - y) / (float) Math.max(1, depth);
            depthFactor = Math.clamp(depthFactor, 0.0f, 1.0f);

            // Subterranean wall tapering: wider trench at top, narrow crack at bottom
            float widthAtY = width * (1.0f - 0.7f * (float) Math.pow(depthFactor, 1.25));

            // 3D Organic Wall Skew: shifts horizontal center as we descend so walls jag and slope
            double skewX = noise.fbm3D(cx * 0.12, y * 0.16, cz * 0.12) * 2.2;
            double skewZ = noise.fbm3D((cx + 27.3) * 0.12, y * 0.16, (cz - 18.7) * 0.12) * 2.2;

            double sliceCenterX = cx + skewX;
            double sliceCenterZ = cz + skewZ;

            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    double sampleX = sliceCenterX + dx;
                    double sampleZ = sliceCenterZ + dz;
                    double distSq = (sampleX - sliceCenterX) * (sampleX - sliceCenterX) + (sampleZ - sliceCenterZ) * (sampleZ - sliceCenterZ);

                    // Organic jitter on the wall perimeter
                    double wallJitter = noise.noise3D(sampleX * 0.4, y * 0.35, sampleZ * 0.4) * 0.85;
                    double maxDist = (widthAtY * 0.5) + wallJitter;

                    if (distSq > maxDist * maxDist) {
                        continue;
                    }

                    mpos.set((int) Math.floor(sampleX), y, (int) Math.floor(sampleZ));
                    BlockState state = level.getBlockState(mpos);

                    // Preservation rules: never break unbreakable blocks, tile entities or fluids
                    if (state.isAir() || state.getDestroySpeed(level, mpos) < 0 || state.hasBlockEntity()) {
                        continue;
                    }

                    // Bottom layer: occasionally place magma, deepslate or cobblestone rubble
                    if (y == bottomY && level.random.nextFloat() < 0.25f) {
                        float roll = level.random.nextFloat();
                        BlockState rubble = roll < 0.35f ? Blocks.MAGMA_BLOCK.defaultBlockState()
                                : (roll < 0.7f ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                        level.setBlock(mpos, rubble, Block.UPDATE_CLIENTS);
                    } else {
                        level.setBlock(mpos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            }
        }
    }

    private void spawnBranchCrack(ServerLevel level, double currentHeading) {
        EarthquakeFaultWorkerEntity branch = ModEntities.EARTHQUAKE_WORKER.get().create(level);
        if (branch == null) {
            return;
        }
        // Branch off at 35 to 55 degrees, shorter length and shallower depth
        double branchAngle = currentHeading + (level.random.nextBoolean() ? 1 : -1) * (0.6 + level.random.nextDouble() * 0.35);
        float branchLength = targetLength * (0.35f + level.random.nextFloat() * 0.2f);
        int branchDepth = Math.max(5, (int) (maxDepth * 0.7f));
        float branchWidth = Math.max(1.8f, maxWidth * 0.75f);

        branch.configure(currentX, currentZ, branchAngle, branchLength, branchDepth, branchWidth,
                destroyBlocks, ignoreSafety, safetyThreshold, noiseSeed ^ 0x9E3779B9L, true);
        level.addFreshEntity(branch);
    }

    private void playRumbleEffects(ServerLevel level, BlockPos tipPos, float width, int depth) {
        // Sounds: deep subterranean rock fracturing, crushing and seismic thunder
        if (this.tickCount % 4 == 0) {
            float rockPitch = 0.6f + level.random.nextFloat() * 0.2f;
            level.playSound(null, tipPos.getX() + 0.5, tipPos.getY() + 0.5, tipPos.getZ() + 0.5,
                    SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 3.0f, rockPitch);
            level.playSound(null, tipPos.getX() + 0.5, tipPos.getY() + 0.5, tipPos.getZ() + 0.5,
                    SoundEvents.TUFF_BREAK, SoundSource.BLOCKS, 2.5f, rockPitch * 0.9f);

            if (this.tickCount % 12 == 0) {
                level.playSound(null, tipPos.getX() + 0.5, tipPos.getY() + 0.5, tipPos.getZ() + 0.5,
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2.0f, 0.45f);
                level.playSound(null, tipPos.getX() + 0.5, tipPos.getY() + 0.5, tipPos.getZ() + 0.5,
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.4f, 0.35f);
            }
        }

        // Particles: rising debris and dust puffs
        int particleCount = Math.max(4, (int) (width * 4));
        for (int i = 0; i < particleCount; i++) {
            double px = tipPos.getX() + (level.random.nextDouble() - 0.5) * width;
            double pz = tipPos.getZ() + (level.random.nextDouble() - 0.5) * width;
            double py = tipPos.getY() + level.random.nextDouble() * 1.5;

            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 1, 0.05, 0.15, 0.05, 0.02);
            level.sendParticles(ParticleTypes.POOF, px, py, pz, 1, 0.1, 0.1, 0.1, 0.05);

            BlockState surfaceBlock = level.getBlockState(tipPos.below());
            if (!surfaceBlock.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, surfaceBlock),
                        px, py, pz, 2, 0.2, 0.3, 0.2, 0.1);
            }
        }

        // Entity physics: stagger entities right over the active fissure
        AABB impactZone = new AABB(tipPos).inflate(width + 1.0, 3.0, width + 1.0);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, impactZone, LivingEntity::isAlive);
        for (LivingEntity entity : nearby) {
            Vec3 motion = entity.getDeltaMovement();
            double pullX = (tipPos.getX() + 0.5 - entity.getX()) * 0.12;
            double pullZ = (tipPos.getZ() + 0.5 - entity.getZ()) * 0.12;
            entity.setDeltaMovement(motion.x * 0.6 + pullX, -0.25, motion.z * 0.6 + pullZ);
            entity.hurtMarked = true;
        }
    }

    public boolean isCarving() {
        return this.entityData.get(IS_CARVING);
    }

    public BlockPos getTipPos() {
        return this.entityData.get(TIP_POS);
    }

    public float getShakeRadius() {
        return this.entityData.get(SHAKE_RADIUS);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.currentX = tag.getDouble("CurrentX");
        this.currentZ = tag.getDouble("CurrentZ");
        this.currentAzimuth = tag.getDouble("Azimuth");
        this.currentArclength = tag.getFloat("Arclength");
        this.targetLength = tag.getFloat("TargetLength");
        this.maxWidth = tag.getFloat("MaxWidth");
        this.maxDepth = tag.getInt("MaxDepth");
        this.destroyBlocks = tag.getBoolean("DestroyBlocks");
        this.ignoreSafety = tag.getBoolean("IgnoreSafety");
        this.safetyThreshold = tag.getLong("SafetyThreshold");
        this.noiseSeed = tag.getLong("NoiseSeed");
        this.isBranch = tag.getBoolean("IsBranch");
        this.hasSpawnedBranch = tag.getBoolean("HasSpawnedBranch");
        this.entityData.set(IS_CARVING, tag.getBoolean("IsCarving"));
        if (tag.contains("TipX")) {
            this.entityData.set(TIP_POS, new BlockPos(tag.getInt("TipX"), tag.getInt("TipY"), tag.getInt("TipZ")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("CurrentX", this.currentX);
        tag.putDouble("CurrentZ", this.currentZ);
        tag.putDouble("Azimuth", this.currentAzimuth);
        tag.putFloat("Arclength", this.currentArclength);
        tag.putFloat("TargetLength", this.targetLength);
        tag.putFloat("MaxWidth", this.maxWidth);
        tag.putInt("MaxDepth", this.maxDepth);
        tag.putBoolean("DestroyBlocks", this.destroyBlocks);
        tag.putBoolean("IgnoreSafety", this.ignoreSafety);
        tag.putLong("SafetyThreshold", this.safetyThreshold);
        tag.putLong("NoiseSeed", this.noiseSeed);
        tag.putBoolean("IsBranch", this.isBranch);
        tag.putBoolean("HasSpawnedBranch", this.hasSpawnedBranch);
        tag.putBoolean("IsCarving", this.isCarving());
        BlockPos tip = getTipPos();
        tag.putInt("TipX", tip.getX());
        tag.putInt("TipY", tip.getY());
        tag.putInt("TipZ", tip.getZ());
    }
}

