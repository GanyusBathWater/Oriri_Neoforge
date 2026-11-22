package net.ganyusbathwater.oririmod.client.render.world.haze;

/**
 * Visual configuration for an atmospheric floor-hugging ground haze layer.
 *
 * @param r              Red tint [0..1]
 * @param g              Green tint [0..1]
 * @param b              Blue tint [0..1]
 * @param maxAlpha       Peak opacity under optimal night/post-rain conditions [0..1]
 * @param dryDayRatio    Fraction of {@code maxAlpha} visible during dry daytime [0..1]
 * @param nightWeight    Strength reached during night [0..1]
 * @param postRainWeight Strength reached during and right after rain [0..1]
 * @param layer1Height   Height offset of the lower haze sheet above smoothed ground (blocks)
 * @param layer2Height   Height offset of the upper haze sheet above smoothed ground (blocks)
 * @param noiseScale     World-space UV frequency (blocks per tile = 1 / noiseScale)
 * @param driftSpeed     Slow UV drift speed (tiles per second)
 */
public record GroundHazeProfile(
        float r,
        float g,
        float b,
        float maxAlpha,
        float dryDayRatio,
        float nightWeight,
        float postRainWeight,
        float layer1Height,
        float layer2Height,
        float noiseScale,
        float driftSpeed) {

    public static final GroundHazeProfile NONE = new GroundHazeProfile(
            0.85F, 0.90F, 0.95F,
            0.0F, 0.0F, 0.0F, 0.0F,
            0.30F, 0.95F, 0.032F, 0.012F);
}
