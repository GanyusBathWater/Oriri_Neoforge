package net.ganyusbathwater.oririmod.events;

import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.ganyusbathwater.oririmod.network.packet.OpenExtraInventoryPacket;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = "oririmod", value = Dist.CLIENT)
public class InventoryButtonClientEvents {
    private static final Map<CreativeModeInventoryScreen, ImageButton> CREATIVE_BUTTONS = new WeakHashMap<>();

    @SubscribeEvent
    public static void onInitPost(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof InventoryScreen || e.getScreen() instanceof CreativeModeInventoryScreen)) return;

        AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) e.getScreen();
        int left = s.getGuiLeft();
        int top = s.getGuiTop();
        int x = left + s.getXSize() - 24;
        int y = top + 6;

        WidgetSprites sprites = new WidgetSprites(
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/ender_pearl.png"),
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/barrier.png"),
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/experience_bottle.png")
        );

        ImageButton btn = new ImageButton(
                x, y, 20, 20, sprites,
                b -> NetworkHandler.sendOpenExtraInventory(),
                Component.translatable("screen.oririmod.extra_inventory")
        );

        // Anfangssichtbarkeit nur im Creative-Survival-Reiter
        if (s instanceof CreativeModeInventoryScreen cs) {
            btn.visible = cs.isInventoryOpen();
            CREATIVE_BUTTONS.put(cs, btn);
        }

        e.addListener(btn);
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post e) {
        if (!(e.getScreen() instanceof CreativeModeInventoryScreen cs)) return;
        ImageButton btn = CREATIVE_BUTTONS.get(cs);
        if (btn != null) {
            btn.visible = cs.isInventoryOpen();
        }
    }
}