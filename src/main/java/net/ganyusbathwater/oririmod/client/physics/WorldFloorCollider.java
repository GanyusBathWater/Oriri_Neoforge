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

        // Check current block and 1 block below
        double highestFloor = Double.NEGATIVE_INFINITY;

        for (int dy = 0; dy >= -1; dy--) {
            BlockPos check = pos.above(dy);
            BlockState state = level.getBlockState(check);
            VoxelShape shape = state.getCollisionShape(level, check);

            if (!shape.isEmpty()) {
                double surfaceY = check.getY() + shape.max(Direction.Axis.Y);
                if (surfaceY > highestFloor && surfaceY <= py + 0.5D) {
                    highestFloor = surfaceY;
                }
            }
        }

        if (highestFloor != Double.NEGATIVE_INFINITY) {
            float minAllowedY = (float) highestFloor + particle.radius;
            if (particle.position.y < minAllowedY) {
                float pen = minAllowedY - particle.position.y;
                particle.position.y = minAllowedY;
                particle.prevPosition.y += pen; // Absorb vertical kinetic energy so it rests flat without bouncing

                // Ground friction on horizontal velocity
                float vx = (particle.position.x - particle.prevPosition.x) * friction;
                float vz = (particle.position.z - particle.prevPosition.z) * friction;
                particle.prevPosition.x = particle.position.x - vx;
                particle.prevPosition.z = particle.position.z - vz;
            }
        }
    }
}
