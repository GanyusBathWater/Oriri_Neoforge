package net.ganyusbathwater.oririmod.client.physics;

import org.joml.Vector3f;

/**
 * A spherical collider that repels particles from penetrating a solid volume.
 * Ideal for chest, torso, and shoulder colliders to prevent hair or appendages from clipping.
 */
public class SphereCollider implements ICollider {
    public final Vector3f center = new Vector3f();
    public float radius;

    private final Vector3f diff = new Vector3f();

    public SphereCollider(float x, float y, float z, float radius) {
        this.center.set(x, y, z);
        this.radius = radius;
    }

    public void updateCenter(float x, float y, float z) {
        this.center.set(x, y, z);
    }

    @Override
    public void resolve(VerletParticle particle) {
        if (particle.pinned) return;

        diff.set(particle.position).sub(center);
        float distSq = diff.lengthSquared();
        float totalRadius = radius + particle.radius;

        if (distSq < totalRadius * totalRadius && distSq > 1.0E-6F) {
            float dist = (float) Math.sqrt(distSq);
            diff.div(dist); // Normalize
            Vector3f targetPos = new Vector3f(center).add(new Vector3f(diff).mul(totalRadius));
            
            float penX = targetPos.x - particle.position.x;
            float penY = targetPos.y - particle.position.y;
            float penZ = targetPos.z - particle.position.z;

            particle.position.set(targetPos);
            particle.prevPosition.add(penX, penY, penZ); // Slide smoothly along collider surface
        }
    }
}
