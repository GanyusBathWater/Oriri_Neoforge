package net.ganyusbathwater.oririmod.client.physics;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages an entire chain of Verlet particles, constraints, and colliders.
 * Supports high-refresh frame interpolation (lerp on partialTick) to eliminate micro-stutter.
 */
public class VerletChain {
    public final List<VerletParticle> particles = new ArrayList<>();
    public final List<DistanceConstraint> distanceConstraints = new ArrayList<>();
    public final List<AngleConstraint> angleConstraints = new ArrayList<>();
    public final List<ICollider> colliders = new ArrayList<>();

    public final Vector3f gravity = new Vector3f(0.0F, -9.81F * 0.05F, 0.0F); // Scaled for tick simulation
    public float damping = 0.92F;
    public int solverIterations = 6;

    private boolean initialized = false;

    public void addParticle(VerletParticle particle) {
        this.particles.add(particle);
    }

    public void addDistanceConstraint(int indexA, int indexB, float restDistance) {
        this.distanceConstraints.add(new DistanceConstraint(particles.get(indexA), particles.get(indexB), restDistance));
    }

    public void addAngleConstraint(int indexA, int indexB, int indexC, float maxAngleDegrees) {
        this.angleConstraints.add(new AngleConstraint(particles.get(indexA), particles.get(indexB), particles.get(indexC), maxAngleDegrees));
    }

    public void addCollider(ICollider collider) {
        this.colliders.add(collider);
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * Executes one tick of physics simulation.
     */
    public void simulate() {
        // 1. Commit previous tick positions for render interpolation
        for (VerletParticle p : particles) {
            p.commitTick();
        }

        // 2. Verlet integration: x_new = x + (x - x_old) * damping + a * dt^2
        for (VerletParticle p : particles) {
            if (p.pinned) continue;

            float vx = (p.position.x - p.prevPosition.x) * damping;
            float vy = (p.position.y - p.prevPosition.y) * damping;
            float vz = (p.position.z - p.prevPosition.z) * damping;

            p.prevPosition.set(p.position);

            p.position.x += vx + gravity.x;
            p.position.y += vy + gravity.y;
            p.position.z += vz + gravity.z;
        }

        // 3. Gauss-Seidel constraint relaxation
        for (int iter = 0; iter < solverIterations; iter++) {
            for (DistanceConstraint dc : distanceConstraints) {
                dc.solve();
            }
            for (AngleConstraint ac : angleConstraints) {
                ac.solve();
            }
            for (ICollider collider : colliders) {
                for (VerletParticle p : particles) {
                    collider.resolve(p);
                }
            }
        }

        // 4. Safety validation: Recover from any NaN / Infinity to guarantee mesh never vanishes
        for (int i = 1; i < particles.size(); i++) {
            VerletParticle p = particles.get(i);
            if (Float.isNaN(p.position.x) || Float.isInfinite(p.position.x) ||
                Float.isNaN(p.position.y) || Float.isInfinite(p.position.y) ||
                Float.isNaN(p.position.z) || Float.isInfinite(p.position.z)) {
                VerletParticle prev = particles.get(i - 1);
                p.set(prev.position.x, prev.position.y, prev.position.z);
            }
        }
    }

    /**
     * Interpolates all particle positions for the current render frame based on partialTick.
     */
    public void interpolate(float partialTick) {
        for (VerletParticle p : particles) {
            p.updateRenderPos(partialTick);
        }
    }
}
