package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Handles cinematic presentation during NPC conversations:
 * - Hides standard Minecraft HUD layers while a conversation is open.
 * - Applies a smooth, subtle camera FOV zoom-in while conversing, easing back
 *   out cleanly once the conversation ends.
 */
@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public class ConversationHudHandler {

    private static final float ZOOM_SPEED = 4.2F;       // reaches full focus in ~0.35s
    private static final double TARGET_FOV_MULT = 0.86; // gentle cinematic focus

    private static float zoomProgress = 0.0F;
    private static long lastFrameNanos = 0L;

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!ConversationScreen.isConversationActive()) return;

        var layerName = event.getName();

        if (layerName.equals(VanillaGuiLayers.HOTBAR)
                || layerName.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || layerName.equals(VanillaGuiLayers.ARMOR_LEVEL)
                || layerName.equals(VanillaGuiLayers.FOOD_LEVEL)
                || layerName.equals(VanillaGuiLayers.AIR_LEVEL)
                || layerName.equals(VanillaGuiLayers.EXPERIENCE_BAR)
                || layerName.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)
                || layerName.equals(VanillaGuiLayers.JUMP_METER)
                || layerName.equals(VanillaGuiLayers.SELECTED_ITEM_NAME)
                || layerName.equals(VanillaGuiLayers.EFFECTS)
                || layerName.equals(VanillaGuiLayers.BOSS_OVERLAY)
                || layerName.equals(VanillaGuiLayers.CROSSHAIR)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0.016F : Math.min(0.1F, (now - lastFrameNanos) / 1_000_000_000.0F);
        lastFrameNanos = now;

        float target = ConversationScreen.isConversationActive() ? 1.0F : 0.0F;
        if (zoomProgress != target) {
            if (zoomProgress < target) {
                zoomProgress = Math.min(target, zoomProgress + dt * ZOOM_SPEED);
            } else {
                zoomProgress = Math.max(target, zoomProgress - dt * ZOOM_SPEED);
            }
        }

        if (zoomProgress > 0.001F) {
            // Smoothstep easing for silky camera focus
            float eased = zoomProgress * zoomProgress * (3.0F - 2.0F * zoomProgress);
            double mult = Mth.lerp(eased, 1.0, TARGET_FOV_MULT);
            event.setFOV(event.getFOV() * mult);
        }
    }
}
