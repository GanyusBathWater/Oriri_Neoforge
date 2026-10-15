package net.ganyusbathwater.oririmod.client.physics;

import net.minecraft.util.Mth;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.List;

/**
 * Maps a simulated 3D VerletChain onto a hierarchy of GeckoLib GeoBones.
 * Converts world-space particle orientations into local bone Euler angles.
 */
public class GeckoLibChainAdapter {

    /**
     * Applies the orientations of consecutive particles in the VerletChain to a list of GeoBones.
     *
     * @param chain       The simulated VerletChain
     * @param bones       The list of GeoBones corresponding to segments 1..N
     * @param baseYaw     The base world yaw of the entity/root anchor
     * @param restPitch   The rest pitch of the bones in Blockbench (typically -90 deg for horizontal tail)
     */
    public static void applyChainToBones(VerletChain chain, List<GeoBone> bones, float baseYaw, float restPitch) {
        if (chain.particles.size() < bones.size() + 1) return;

        float prevSegmentYaw = baseYaw;
        float prevSegmentPitch = 0.0F;

        for (int i = 0; i < bones.size(); i++) {
            GeoBone bone = bones.get(i);
            if (bone == null) continue;

            VerletParticle pPrev = chain.particles.get(i);
            VerletParticle pNext = chain.particles.get(i + 1);

            float dx = pNext.renderPos.x - pPrev.renderPos.x;
            float dy = pNext.renderPos.y - pPrev.renderPos.y;
            float dz = pNext.renderPos.z - pPrev.renderPos.z;

            float horizontalDist = (float) Math.sqrt(dx * dx + dz * dz);
            if (horizontalDist < 1.0E-5F && Math.abs(dy) < 1.0E-5F) {
                bone.setRotX(bone.getInitialSnapshot().getRotX());
                bone.setRotY(bone.getInitialSnapshot().getRotY());
                bone.setRotZ(bone.getInitialSnapshot().getRotZ());
                continue;
            }

            // World angles of this segment
            float worldYaw = (float) Math.toDegrees(Mth.atan2(dx, dz));
            float worldPitch = (float) Math.toDegrees(Mth.atan2(dy, horizontalDist));

            // Relative rotation to predecessor
            float deltaYaw = Mth.wrapDegrees(worldYaw - prevSegmentYaw);
            float deltaPitch = Mth.wrapDegrees(worldPitch - prevSegmentPitch);

            // In GeckoLib's coordinate frame:
            // rotZ turns left/right on horizontal plane
            // rotX pitches up/down
            bone.setRotZ(bone.getInitialSnapshot().getRotZ() + (float) Math.toRadians(Mth.clamp(deltaYaw, -25.0F, 25.0F)));
            bone.setRotX(bone.getInitialSnapshot().getRotX() + (float) Math.toRadians(Mth.clamp(deltaPitch, -25.0F, 25.0F)));
            bone.setRotY(bone.getInitialSnapshot().getRotY());

            prevSegmentYaw = worldYaw;
            prevSegmentPitch = worldPitch;
        }
    }
}
