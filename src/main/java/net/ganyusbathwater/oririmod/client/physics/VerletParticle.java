package net.ganyusbathwater.oririmod.client.physics;

import org.joml.Vector3f;

/**
 * A physical particle in 3D world space simulated using Verlet integration.
 */
public class VerletParticle {
    public final Vector3f position = new Vector3f();
    public final Vector3f prevPosition = new Vector3f();
    public final Vector3f renderPos = new Vector3f();
    public final Vector3f prevRenderPos = new Vector3f();

    public boolean pinned = false;
    public float radius = 0.15F;
    public float mass = 1.0F;

    public VerletParticle(float x, float y, float z) {
        set(x, y, z);
    }

    public void set(float x, float y, float z) {
        this.position.set(x, y, z);
        this.prevPosition.set(x, y, z);
        this.renderPos.set(x, y, z);
        this.prevRenderPos.set(x, y, z);
    }

    public void updateRenderPos(float partialTick) {
        this.renderPos.set(
                prevRenderPos.x + (position.x - prevRenderPos.x) * partialTick,
                prevRenderPos.y + (position.y - prevRenderPos.y) * partialTick,
                prevRenderPos.z + (position.z - prevRenderPos.z) * partialTick
        );
    }

    public void commitTick() {
        this.prevRenderPos.set(this.position);
    }
}
