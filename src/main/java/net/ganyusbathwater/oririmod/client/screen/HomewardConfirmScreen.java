package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.packet.HomewardConfirmPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;

/**
 * Sanctuary Return confirmation dialog shown when finishing the Homeward channel
 * inside a dungeon.
 */
@OnlyIn(Dist.CLIENT)
public class HomewardConfirmScreen extends Screen {

    private static final int PANEL_WIDTH  = 256;
    private static final int PANEL_HEIGHT = 148;

    public HomewardConfirmScreen() {
        super(Component.translatable("screen.oririmod.homeward.title"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();

        int left = (this.width - PANEL_WIDTH) / 2;
        int top  = (this.height - PANEL_HEIGHT) / 2;
        int buttonY = top + PANEL_HEIGHT - 32;

        // "Leave" button (arcane hearth cyan)
        addRenderableWidget(new SanctuaryButton(
                left + 16,
                buttonY,
                104,
                20,
                Component.translatable("screen.oririmod.homeward.leave"),
                ButtonTone.CYAN,
                () -> confirm(true)
        ));

        // "Cancel" button (obsidian/gold neutral)
        addRenderableWidget(new SanctuaryButton(
                left + PANEL_WIDTH - 120,
                buttonY,
                104,
                20,
                Component.translatable("screen.oririmod.homeward.cancel"),
                ButtonTone.NEUTRAL,
                () -> confirm(false)
        ));
    }

    private void confirm(boolean leave) {
        PacketDistributor.sendToServer(new HomewardConfirmPayload(leave));
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(gfx);

        int cx   = this.width / 2;
        int left = cx - PANEL_WIDTH / 2;
        int top  = (this.height - PANEL_HEIGHT) / 2;

        // Obsidian sanctuary panel accented with Homeward arcane cyan & gold
        drawSanctuaryPanel(gfx, left, top, PANEL_WIDTH, PANEL_HEIGHT, CYAN, 0xFF285566);

        // Animated Homeward rune seal emblem at top center
        drawHomewardSeal(gfx, cx, top + 20);

        // Title
        gfx.drawCenteredString(font,
                Component.translatable("screen.oririmod.homeward.title").withStyle(ChatFormatting.BOLD),
                cx, top + 38, CYAN_LIGHT);

        drawGildedDivider(gfx, left + 16, top + 52, PANEL_WIDTH - 32, CYAN);

        // Body lines
        gfx.drawCenteredString(font,
                Component.translatable("screen.oririmod.homeward.line1"),
                cx, top + 64, TEXT_PRIMARY);
        gfx.drawCenteredString(font,
                Component.translatable("screen.oririmod.homeward.line2"),
                cx, top + 78, TEXT_BODY);

        drawGildedDivider(gfx, left + 16, top + PANEL_HEIGHT - 40, PANEL_WIDTH - 32, GOLD_DIM);
    }

    private void drawHomewardSeal(GuiGraphics gfx, int cx, int cy) {
        float time = (System.currentTimeMillis() % 3000L) / 3000.0F;
        float pulse = 0.65F + 0.35F * Mth.sin(time * Mth.TWO_PI);

        // Soft outer halo diamond
        drawDiamond(gfx, cx, cy, 11, withAlpha(CYAN, (int) (0x28 * pulse)));
        // Mid cyan ring
        drawDiamond(gfx, cx, cy, 8, withAlpha(CYAN, (int) (0x80 * pulse)));
        drawDiamond(gfx, cx, cy, 6, 0xFF0C1622);
        // Inner hearth-gold core
        drawDiamond(gfx, cx, cy, 4, withAlpha(GOLD_BRIGHT, (int) (0xD0 + 0x2F * pulse)));
        drawDiamond(gfx, cx, cy, 2, 0xFFFFFFFF);

        // Subtle horizontal rays
        int rayAlpha = (int) (0x70 * pulse);
        gfx.hLine(cx - 20, cx - 12, cy, withAlpha(CYAN_LIGHT, rayAlpha));
        gfx.hLine(cx + 12, cx + 20, cy, withAlpha(CYAN_LIGHT, rayAlpha));
    }
}
