package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.OririMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Hides the player's HUD during NPC conversations for a cinematic feel.
 * Cancels rendering of hotbar, health, food, experience, etc.
 */
@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public class ConversationHudHandler {

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!ConversationScreen.isConversationActive()) return;

        var layerName = event.getName();

        // Hide all standard HUD elements during conversation
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
}
