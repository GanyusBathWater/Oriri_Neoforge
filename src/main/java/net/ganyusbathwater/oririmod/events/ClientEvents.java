package net.ganyusbathwater.oririmod.events;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.config.ManaConfig;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    private static final ResourceLocation TEX_0 = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/gui/mana_0.png");
    private static final ResourceLocation TEX_25 = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/gui/mana_25.png");
    private static final ResourceLocation TEX_50 = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/gui/mana_50.png");
    private static final ResourceLocation TEX_75 = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/gui/mana_75.png");
    private static final ResourceLocation TEX_100 = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/gui/mana_100.png");

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        GuiGraphics gui = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Integer manaOpt = ModManaUtil.getManaIfPresent(player);
        Integer maxOpt = ModManaUtil.getMaxManaIfPresent(player);

        String manaText;
        int percentForTexture;
        if (manaOpt == null || maxOpt == null) {
            manaText = "syncing...";
            percentForTexture = 100;
        } else {
            int mana = manaOpt;
            int max = Math.max(1, maxOpt);
            manaText = mana + " / " + max;
            percentForTexture = (int) Math.round((mana * 100.0) / max);
        }
        ResourceLocation tex = selectTextureByPercent(percentForTexture);

        int x = 10;
        int y = mc.getWindow().getGuiScaledHeight() - 38;
        int size = 32;

        RenderSystem.enableBlend();
        gui.blit(tex, x, y, 0, 0, size, size, size, size);
        RenderSystem.disableBlend();

        int textX = x + size + 6;
        int textY = y + (size / 2) - 4;
        gui.drawString(mc.font, Component.literal(manaText), textX, textY, 0xFFFFFF, true);
    }

    private static ResourceLocation selectTextureByPercent(int percent) {
        if (percent <= 0) return TEX_0;
        if (percent <= 25) return TEX_25;
        if (percent <= 50) return TEX_50;
        if (percent <= 75) return TEX_75;
        return TEX_100;
    }
}