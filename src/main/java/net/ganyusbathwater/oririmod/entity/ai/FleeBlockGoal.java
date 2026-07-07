package net.ganyusbathwater.oririmod.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;

import java.util.EnumSet;

public class FleeBlockGoal extends Goal {
    protected final PathfinderMob mob;
    protected final BlockPos targetPos;
    protected final float distance;
    protected final double walkSpeedModifier;
    protected final double sprintSpeedModifier;
    protected PathNavigation navigation;
    protected net.minecraft.world.level.pathfinder.Path path;

    public FleeBlockGoal(PathfinderMob mob, BlockPos targetPos, float distance, double walkSpeedModifier, double sprintSpeedModifier) {
        this.mob = mob;
        this.targetPos = targetPos;
        this.distance = distance;
        this.walkSpeedModifier = walkSpeedModifier;
        this.sprintSpeedModifier = sprintSpeedModifier;
        this.navigation = mob.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }
    
    public BlockPos getTargetPos() {
        return targetPos;
    }

    @Override
    public boolean canUse() {
        if (this.mob.distanceToSqr(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ()) > this.distance * this.distance) {
            return false;
        }
        
        Vec3 targetVec = Vec3.atCenterOf(this.targetPos);
        Vec3 fleePos = DefaultRandomPos.getPosAway(this.mob, 16, 7, targetVec);
        if (fleePos == null) {
            return false;
        } else if (targetVec.distanceToSqr(fleePos) < targetVec.distanceToSqr(this.mob.position())) {
            return false;
        } else {
            this.path = this.navigation.createPath(fleePos.x, fleePos.y, fleePos.z, 0);
            return this.path != null;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.navigation.isDone() && this.mob.distanceToSqr(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ()) <= this.distance * this.distance;
    }

    @Override
    public void start() {
        this.navigation.moveTo(this.path, this.mob.distanceToSqr(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ()) < 49.0 ? this.sprintSpeedModifier : this.walkSpeedModifier);
    }
    
    @Override
    public void stop() {
        this.path = null;
    }
}
