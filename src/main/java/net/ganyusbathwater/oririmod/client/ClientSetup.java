package net.ganyusbathwater.oririmod.client;

import net.ganyusbathwater.oririmod.client.screen.ExtraInventoryScreen;
import net.ganyusbathwater.oririmod.menu.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "oririmod", value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent e) {
        e.register(ModMenus.EXTRA_INVENTORY.value(), ExtraInventoryScreen::new);
    }
}