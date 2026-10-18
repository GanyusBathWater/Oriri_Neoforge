package net.ganyusbathwater.oririmod.client.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Collides particles against Minecraft world terrain so segments stay on top of the ground.
 */
public class WorldFloorCollider implements ICollider {
    private Level level;
    public float friction = 0.85F;

    public void setLevel(Level level) {
        this.level = level;
    }

    @Override
    public void resolve(VerletParticle particle) {
        if (particle.pinned || level == null) return;

        double px = particle.position.x;
        double py = particle.position.y;
        double pz = particle.position.z;

        BlockPos pos = BlockPos.containing(px, py, pz);

        // Check 1 block above, current block, and down to 3 blocks below for terrain/slabs/stairs
        double highestFloor = Double.NEGATIVE_INFINITY;
        double maxSearchY = Math.max(py, particle.prevPosition.y) + 0.35D;

        for (int dy = 1; dy >= -3; dy--) {
            BlockPos check = pos.above(dy);
            if (!level.hasChunkAt(check)) continue;
            BlockState state = level.getBlockState(check);
            VoxelShape shape = state.getCollisionShape(level, check);

            if (!shape.isEmpty()) {
                double surfaceY = check.getY() + shape.max(Direction.Axis.Y);
                if (surfaceY > highestFloor && surfaceY <= maxSearchY) {
                    highestFloor = surfaceY;
                }
            }
        }

        if (highestFloor != Double.NEGATIVE_INFINITY) {
            float minAllowedY = (float) highestFloor + particle.radius;
            if (particle.position.y < minAllowedY) {
                particle.position.y = minAllowedY;
                particle.prevPosition.y = particle.position.y; // Zero vertical velocity to rest stably on floor

                // Ground friction on horizontal velocity
                float vx = (particle.position.x - particle.prevPosition.x) * friction;
                float vz = (particle.position.z - particle.prevPosition.z) * friction;
                particle.prevPosition.x = particle.position.x - vx;
                particle.prevPosition.z = particle.position.z - vz;
            }
        }
    }
}
