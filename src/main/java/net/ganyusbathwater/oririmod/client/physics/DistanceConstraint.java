package net.ganyusbathwater.oririmod.client.physics;

import org.joml.Vector3f;

/**
 * Enforces a strict rest distance between two Verlet particles.
 */
public class DistanceConstraint {
    public final VerletParticle a;
    public final VerletParticle b;
    public float restDistance;
    public float stiffness = 1.0F;

    private final Vector3f delta = new Vector3f();

    public DistanceConstraint(VerletParticle a, VerletParticle b, float restDistance) {
        this.a = a;
        this.b = b;
        this.restDistance = restDistance;
    }

    public void solve() {
        delta.set(b.position).sub(a.position);
        float currentDist = delta.length();
        if (currentDist < 1.0E-6F) return;

        float diff = (currentDist - restDistance) / currentDist * stiffness;

        if (!a.pinned && !b.pinned) {
            delta.mul(0.5F * diff);
            a.position.add(delta);
            b.position.sub(delta);
        } else if (a.pinned && !b.pinned) {
            delta.mul(diff);
            b.position.sub(delta);
        } else if (!a.pinned && b.pinned) {
            delta.mul(diff);
            a.position.add(delta);
        }
    }
}
