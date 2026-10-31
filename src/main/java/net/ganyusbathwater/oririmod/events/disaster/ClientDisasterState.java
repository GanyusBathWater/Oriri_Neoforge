package net.ganyusbathwater.oririmod.events.disaster;

import org.joml.Vector3f;

/**
 * Tracks active natural disaster state on the client side for rendering sky/fog colors,
 * atmospheric screen effects, and audio ambience.
 */
public final class ClientDisasterState {

    private static NaturalDisasterType activeType = NaturalDisasterType.NONE;
    private static NaturalDisasterType lastActiveType = NaturalDisasterType.NONE;
    private static int ticksRemaining = 0;
    private static int totalDuration = 0;
    private static float transitionProgress = 0.0f;

    private ClientDisasterState() {}

    public static void update(NaturalDisasterType type, int remaining, int duration) {
        if (type != null && type != NaturalDisasterType.NONE) {
            lastActiveType = type;
        }
        activeType = type != null ? type : NaturalDisasterType.NONE;
        ticksRemaining = remaining;
        totalDuration = duration;
    }

    public static NaturalDisasterType getActiveType() {
        return activeType;
    }

    public static NaturalDisasterType getLastActiveType() {
        return lastActiveType;
    }

    public static boolean isDisasterActive() {
        return activeType != null && activeType != NaturalDisasterType.NONE && ticksRemaining > 0;
    }

    public static int getTicksRemaining() {
        return ticksRemaining;
    }

    public static int getTotalDuration() {
        return totalDuration;
    }

    public static float getTransitionProgress() {
        return transitionProgress;
    }

    /**
     * Ticked once per client tick in OririClient.onClientTick (20 times per second).
     */
    public static void tick() {
        if (isDisasterActive()) {
            if (ticksRemaining > 0) {
                ticksRemaining--;
            }
            if (transitionProgress < 1.0f) {
                transitionProgress = Math.min(1.0f, transitionProgress + 0.04f); // ~0.5s fade in
            }
        } else {
            if (transitionProgress > 0.0f) {
                transitionProgress = Math.max(0.0f, transitionProgress - 0.04f); // ~0.5s fade out
            }
        }
    }

    public static Vector3f getSkyColor() {
        NaturalDisasterType typeToUse = isDisasterActive() ? activeType : lastActiveType;
        if (typeToUse == NaturalDisasterType.NONE || transitionProgress <= 0.0f) {
            return null;
        }
        return switch (typeToUse) {
            case METEOR_SHOWER -> new Vector3f(0.85F, 0.22F, 0.06F); // Apocalyptic burning ember orange
            case TORNADO -> new Vector3f(0.38F, 0.32F, 0.25F); // Dusty tempest brown-gray
            case LIGHTNING_STORM -> new Vector3f(0.06F, 0.06F, 0.10F); // Pitch-black electrical thunderhead
            case SNOWSTORM -> new Vector3f(0.82F, 0.86F, 0.94F); // Freezing white-blue
            case EARTHQUAKE -> new Vector3f(0.42F, 0.38F, 0.34F); // Seismic dust haze
            default -> null;
        };
    }
}
