package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.ganyusbathwater.oririmod.network.packet.HomewardConfirmPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A small confirmation dialog shown when the player finishes charging
 * the Homeward item inside a dungeon.
 *
 * <pre>
 *  ┌─────────────────────────────────┐
 *  │       Leave the Dungeon?        │
 *  │                                 │
 *  │  You are about to leave this    │
 *  │  dungeon. Your progress will    │
 *  │  be saved.                      │
 *  │                                 │
 *  │  [ Leave ]        [ Cancel ]    │
 *  └─────────────────────────────────┘
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public class HomewardConfirmScreen extends Screen {

    private static final int PANEL_WIDTH  = 230;
    private static final int PANEL_HEIGHT = 120;

    /** Colors */
    private static final int COLOR_BACKGROUND = 0xCC0D0D18;  // dark navy, 80 % opaque
    private static final int COLOR_BORDER      = 0xFF3A3A6A;  // dim purple
    private static final int COLOR_TITLE       = 0xFFE0D9FF;  // soft lavender
    private static final int COLOR_BODY        = 0xFFBBB8CC;  // muted grey-purple

    public HomewardConfirmScreen() {
        super(Component.translatable("screen.oririmod.homeward.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }

    @Override
    protected void init() {
        super.init();

        int cx = width  / 2;
        int cy = height / 2;

        int panelLeft = cx - PANEL_WIDTH / 2;
        int panelTop  = cy - PANEL_HEIGHT / 2;

        int buttonY = panelTop + PANEL_HEIGHT - 30;

        // "Leave" button – green-ish
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("screen.oririmod.homeward.leave"),
                btn -> confirm(true)
            )
            .pos(panelLeft + 16, buttonY)
            .size(90, 20)
            .build()
        );

        // "Cancel" button – red-ish
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("screen.oririmod.homeward.cancel"),
                btn -> confirm(false)
            )
            .pos(panelLeft + PANEL_WIDTH - 106, buttonY)
            .size(90, 20)
            .build()
        );
    }

    private void confirm(boolean leave) {
        PacketDistributor.sendToServer(new HomewardConfirmPayload(leave));
        onClose();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // Dim world behind the screen
        this.renderBackground(gfx, mouseX, mouseY, partialTick);

        int cx = width  / 2;
        int cy = height / 2;
        int panelLeft = cx - PANEL_WIDTH / 2;
        int panelTop  = cy - PANEL_HEIGHT / 2;

        // Panel background
        gfx.fill(panelLeft, panelTop,
                panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT,
                COLOR_BACKGROUND);

        // Border (1-px inset)
        gfx.hLine(panelLeft,     panelLeft + PANEL_WIDTH - 1,  panelTop,                 COLOR_BORDER);
        gfx.hLine(panelLeft,     panelLeft + PANEL_WIDTH - 1,  panelTop + PANEL_HEIGHT - 1, COLOR_BORDER);
        gfx.vLine(panelLeft,     panelTop,  panelTop + PANEL_HEIGHT - 1, COLOR_BORDER);
        gfx.vLine(panelLeft + PANEL_WIDTH - 1, panelTop, panelTop + PANEL_HEIGHT - 1, COLOR_BORDER);

        // Title
        gfx.drawCenteredString(font,
                Component.translatable("screen.oririmod.homeward.title").withStyle(net.minecraft.ChatFormatting.BOLD),
                cx, panelTop + 10, COLOR_TITLE);

        // Body lines
        gfx.drawCenteredString(font,
                Component.translatable("screen.oririmod.homeward.line1"),
                cx, panelTop + 30, COLOR_BODY);
        gfx.drawCenteredString(font,
                Component.translatable("screen.oririmod.homeward.line2"),
                cx, panelTop + 44, COLOR_BODY);

        // Render buttons on top
        super.render(gfx, mouseX, mouseY, partialTick);
    }
}
