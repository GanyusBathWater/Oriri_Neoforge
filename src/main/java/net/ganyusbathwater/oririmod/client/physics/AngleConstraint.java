package net.ganyusbathwater.oririmod.client.physics;

import org.joml.Vector3f;

/**
 * Constrains the bend angle between three consecutive particles (a -> b -> c)
 * to prevent sharp kinks and corner clipping.
 */
public class AngleConstraint {
    public final VerletParticle a;
    public final VerletParticle b;
    public final VerletParticle c;
    public float maxAngleRadians;

    private final Vector3f v1 = new Vector3f();
    private final Vector3f v2 = new Vector3f();
    private final Vector3f correctedDir = new Vector3f();
    private final Vector3f targetPos = new Vector3f();

    public AngleConstraint(VerletParticle a, VerletParticle b, VerletParticle c, float maxAngleDegrees) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.maxAngleRadians = (float) Math.toRadians(maxAngleDegrees);
    }

    public void solve() {
        v1.set(b.position).sub(a.position);
        v2.set(c.position).sub(b.position);

        float l1 = v1.length();
        float l2 = v2.length();
        if (l1 < 1.0E-6F || l2 < 1.0E-6F) return;

        v1.div(l1);
        v2.div(l2);

        float dot = Math.max(-1.0F, Math.min(1.0F, v1.dot(v2)));
        float angle = (float) Math.acos(dot);

        if (angle > maxAngleRadians && !c.pinned) {
            float factor = (angle - maxAngleRadians) / angle;
            correctedDir.set(v2).lerp(v1, factor);
            float len = correctedDir.length();
            if (len > 1.0E-6F) {
                correctedDir.div(len);
                targetPos.set(b.position).add(correctedDir.mul(l2));

                float dx = targetPos.x - c.position.x;
                float dy = targetPos.y - c.position.y;
                float dz = targetPos.z - c.position.z;

                c.position.set(targetPos);
                c.prevPosition.add(dx, dy, dz); // Prevent kinetic energy explosion
            }
        }
    }
}
