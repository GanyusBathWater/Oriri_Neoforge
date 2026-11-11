package net.ganyusbathwater.oririmod.entity.ai;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.SilentNereidEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * AI goal injected into non-native mobs inside The Silent Depths and Sunken
 * Trenches.
 * Causes them to be frightened of the abyssal trench and actively swim/flee
 * upwards
 * and away into the safety of the photic ocean (the_silent_oceans).
 */
public class FleeSilentDepthsGoal extends Goal {
    public static final ResourceKey<Biome> SILENT_DEPTHS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_silent_depths"));
    public static final ResourceKey<Biome> SUNKEN_TRENCHES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_sunken_trenches"));

    private final PathfinderMob mob;
    private Vec3 fleeTarget;
    private int recalculateCooldown = 0;

    public FleeSilentDepthsGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public static boolean isAbyssalBiome(Holder<Biome> biome) {
        return biome.is(SILENT_DEPTHS_KEY) || biome.is(SUNKEN_TRENCHES_KEY);
    }

    public static boolean isInSilentDepths(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide())
            return false;
        return isAbyssalBiome(entity.level().getBiome(entity.blockPosition()));
    }

    @Override
    public boolean canUse() {
        if (this.mob.isDeadOrDying() || this.mob instanceof SilentNereidEntity) {
            return false;
        }
        if (!isInSilentDepths(this.mob)) {
            return false;
        }

        this.fleeTarget = findEscapeTarget();
        return this.fleeTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.mob.isDeadOrDying())
            return false;
        if (!isInSilentDepths(this.mob))
            return false;
        return this.fleeTarget != null && !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.recalculateCooldown = 0;
        if (this.fleeTarget != null) {
            this.mob.getNavigation().moveTo(this.fleeTarget.x, this.fleeTarget.y, this.fleeTarget.z, 1.45D);
        }
    }

    @Override
    public void tick() {
        if (this.fleeTarget == null)
            return;

        if (--this.recalculateCooldown <= 0) {
            this.recalculateCooldown = 20;
            this.mob.getNavigation().moveTo(this.fleeTarget.x, this.fleeTarget.y, this.fleeTarget.z, 1.45D);
        }

        // Apply upward and outward propulsion when swimming in water
        if (this.mob.isInWater()) {
            Vec3 delta = this.mob.getDeltaMovement();
            double targetUpward = Math.min(delta.y + 0.05D, 0.22D);
            this.mob.setDeltaMovement(delta.x * 1.05D, targetUpward, delta.z * 1.05D);
            this.mob.hasImpulse = true;
        }
    }

    private Vec3 findEscapeTarget() {
        BlockPos current = this.mob.blockPosition();
        // Escape towards the photic zone: Y >= 48 in the water column
        int targetY = Math.max(current.getY() + 20, 50);
        // Slightly randomize horizontal escape trajectory
        double targetX = this.mob.getX() + (this.mob.getRandom().nextDouble() - 0.5D) * 24.0D;
        double targetZ = this.mob.getZ() + (this.mob.getRandom().nextDouble() - 0.5D) * 24.0D;

        BlockPos targetPos = new BlockPos((int) targetX, targetY, (int) targetZ);
        if (this.mob.level().getFluidState(targetPos).is(FluidTags.WATER) || this.mob.level().isEmptyBlock(targetPos)) {
            return new Vec3(targetX, targetY, targetZ);
        }

        // Fallback: directly above current pos
        return new Vec3(this.mob.getX(), targetY, this.mob.getZ());
    }
}
