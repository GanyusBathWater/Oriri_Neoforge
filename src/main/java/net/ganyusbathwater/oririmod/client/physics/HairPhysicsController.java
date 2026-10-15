package net.ganyusbathwater.oririmod.client.physics;

import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Manages dynamic physics for hair strands with torso sphere collision
 * to permanently eliminate chest and body clipping.
 */
public class HairPhysicsController {

    public static class HairStrand {
        public final VerletChain chain = new VerletChain();
        public final List<String> boneNames;
        public final float segmentLength;
        public final SphereCollider torsoCollider;

        public HairStrand(List<String> boneNames, float segmentLength, SphereCollider torsoCollider) {
            this.boneNames = boneNames;
            this.segmentLength = segmentLength;
            this.torsoCollider = torsoCollider;

            // Setup particles (1 anchor + N bones)
            for (int i = 0; i <= boneNames.size(); i++) {
                VerletParticle p = new VerletParticle(0, 0, 0);
                if (i == 0) p.pinned = true;
                p.radius = 0.08F;
                chain.addParticle(p);
            }

            // Distance constraints
            for (int i = 0; i < boneNames.size(); i++) {
                chain.addDistanceConstraint(i, i + 1, segmentLength);
            }

            // Angular bend stiffness
            for (int i = 0; i < boneNames.size() - 1; i++) {
                chain.addAngleConstraint(i, i + 1, i + 2, 25.0F);
            }

            chain.addCollider(torsoCollider);
            chain.gravity.set(0.0F, -0.06F, 0.0F); // Gentle gravity
            chain.damping = 0.88F; // Smooth hair damping
        }

        public void init(double headX, double headY, double headZ) {
            for (int i = 0; i < chain.particles.size(); i++) {
                chain.particles.get(i).set((float) headX, (float) (headY - (i * segmentLength)), (float) headZ);
            }
            chain.setInitialized(true);
        }
    }

    private static final Map<Integer, List<HairStrand>> ENTITY_HAIR = new ConcurrentHashMap<>();

    /**
     * Registers hair strands for an entity with anti-clipping torso collider.
     */
    public static void registerHair(LivingEntity entity, List<String> boneNames, float segmentLength, float torsoRadius) {
        List<HairStrand> strands = ENTITY_HAIR.computeIfAbsent(entity.getId(), id -> new ArrayList<>());
        SphereCollider collider = new SphereCollider((float) entity.getX(), (float) entity.getY() + 1.1F, (float) entity.getZ(), torsoRadius);
        strands.add(new HairStrand(boneNames, segmentLength, collider));
    }

    /**
     * Updates and applies dynamic hair physics during render.
     */
    public static void applyHairPhysics(LivingEntity entity,
                                        Function<String, GeoBone> boneFinder,
                                        float headX, float headY, float headZ,
                                        float torsoX, float torsoY, float torsoZ,
                                        float partialTick) {
        List<HairStrand> strands = ENTITY_HAIR.get(entity.getId());
        if (strands == null || strands.isEmpty()) return;

        for (HairStrand strand : strands) {
            if (!strand.chain.isInitialized()) {
                strand.init(headX, headY, headZ);
            }

            // Update anchor to head position
            strand.chain.particles.get(0).position.set(headX, headY, headZ);

            // Update chest/torso collider position
            strand.torsoCollider.updateCenter(torsoX, torsoY, torsoZ);

            // Simulate
            strand.chain.simulate();
            strand.chain.interpolate(partialTick);

            // Apply to bones
            List<GeoBone> bones = new ArrayList<>();
            for (String name : strand.boneNames) {
                GeoBone b = boneFinder.apply(name);
                if (b != null) bones.add(b);
            }

            if (!bones.isEmpty()) {
                GeckoLibChainAdapter.applyChainToBones(strand.chain, bones, entity.yBodyRot, 0.0F);
            }
        }
    }
}
