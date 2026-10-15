package net.ganyusbathwater.oririmod.client.physics;

/**
 * Interface for spatial colliders that resolve penetrations on Verlet particles.
 */
public interface ICollider {
    void resolve(VerletParticle particle);
}
