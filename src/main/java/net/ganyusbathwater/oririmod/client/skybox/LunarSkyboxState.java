package net.ganyusbathwater.oririmod.client.skybox;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Holds the client-side state for the Lunar Skybox system.
 * This is a pure client-side singleton — never access from server code.
 *
 * Toggle via LunarDebugItem for Overworld testing.
 * Final integration will key this to the custom dimension instead.
 */
@OnlyIn(Dist.CLIENT)
public final class LunarSkyboxState {

    private LunarSkyboxState() {}

    /** Whether the lunar skybox is currently active. */
    private static boolean enabled = false;

    /**
     * Toggles the skybox on/off and returns the new state.
     */
    public static boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }
}
