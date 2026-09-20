package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.packet.DungeonActionPayload;
import net.ganyusbathwater.oririmod.network.packet.OpenDungeonSelectionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Screen 1: Dungeon Selection List.
 *
 * Layout (350 × 260 panel):
 * ┌──────────────────────────────────────────────┐
 * │  ⚔ Select a Dungeon ⚔                        │  ← Title Zone (fixed 30px)
 * │──────────────────────────────────────────────│
 * │  [Dungeon Name]                        [→]   │  ← Scrollable list entries
 * │  [Short desc, truncated to 1 line]           │     Text maxWidth = PANEL_W - 90
 * │──────────────────────────────────────────────│     Arrow button in dedicated column
 * │  [Dungeon Name 2]                      [→]   │
 * │  ...                                         │
 * │──────────────────────────────────────────────│
 * │                                     [Close]  │  ← Footer Zone (fixed 30px)
 * └──────────────────────────────────────────────┘
 */
@OnlyIn(Dist.CLIENT)
public class DungeonSelectionScreen extends Screen {

    private static final int PANEL_W = 350;
    private static final int PANEL_H = 260;
    private static final int ITEM_HEIGHT = 50;

    // Colors
    private static final int C_BG      = 0xDD0A0A14;
    private static final int C_BORDER  = 0xFF6A4A2A;
    private static final int C_TITLE   = 0xFFFFD700;
    private static final int C_DESC    = 0xFFCCBB99;
    private static final int C_DIVIDER = 0xFF4A3A2A;
    private static final int C_NAME    = 0xFFEEDDAA;

    private final OpenDungeonSelectionPayload data;

    public DungeonSelectionScreen(OpenDungeonSelectionPayload data) {
        super(Component.translatable("screen.oririmod.dungeon.select_title"));
        this.data = data;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();

        int left = (width - PANEL_W) / 2;
        int top  = (height - PANEL_H) / 2;
        int listTop = top + 38;

        // Dungeon entry buttons — arrow button in the dedicated right column
        for (int i = 0; i < data.dungeonIds().size(); i++) {
            final int index = i;
            int itemY = listTop + (i * ITEM_HEIGHT) + 12;

            Button btn = Button.builder(Component.literal("→").withStyle(ChatFormatting.GOLD), b -> {
                // Open the detail screen for this dungeon
                minecraft.setScreen(new DungeonDetailScreen(data, index));
            })
            .pos(left + PANEL_W - 45, itemY)
            .size(30, 20)
            .build();

            addRenderableWidget(btn);
        }

        // Close button at the bottom
        addRenderableWidget(Button.builder(
                Component.translatable("screen.oririmod.dungeon.close").withStyle(ChatFormatting.GRAY),
                btn -> onClose())
                .pos(left + PANEL_W - 70, top + PANEL_H - 30)
                .size(60, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx, mouseX, mouseY, partial);

        int left = (width - PANEL_W) / 2;
        int top  = (height - PANEL_H) / 2;
        int listTop = top + 38;

        // Background panel
        gfx.fill(left, top, left + PANEL_W, top + PANEL_H, C_BG);
        drawBorder(gfx, left, top, PANEL_W, PANEL_H, C_BORDER);

        // Render widgets (buttons) on top of background
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(gfx, mouseX, mouseY, partial);
        }

        // Title
        gfx.drawCenteredString(font,
                Component.translatable("screen.oririmod.dungeon.select_title").withStyle(ChatFormatting.BOLD),
                left + PANEL_W / 2, top + 12, C_TITLE);
        gfx.hLine(left + 8, left + PANEL_W - 8, top + 30, C_DIVIDER);

        // List items — text is bounded to left column (PANEL_W - 90px to leave room for button)
        int textMaxWidth = PANEL_W - 90;
        for (int i = 0; i < data.dungeonIds().size(); i++) {
            int itemY = listTop + (i * ITEM_HEIGHT);

            String name = data.displayNames().get(i);
            String desc = data.descriptions().get(i);

            // Truncate description to fit
            String truncatedDesc = truncateToWidth(desc, textMaxWidth);

            gfx.drawString(font, name, left + 15, itemY + 8, C_NAME, true);
            gfx.drawString(font, truncatedDesc, left + 15, itemY + 22, C_DESC, true);

            // Divider below each entry
            gfx.hLine(left + 10, left + PANEL_W - 10, itemY + ITEM_HEIGHT - 3, C_DIVIDER);
        }
    }

    private String truncateToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisW = font.width(ellipsis);
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (font.width(sb.toString() + c) + ellipsisW > maxWidth) break;
            sb.append(c);
        }
        return sb + ellipsis;
    }

    private void drawBorder(GuiGraphics gfx, int x, int y, int w, int h, int color) {
        gfx.hLine(x, x + w - 1, y,         color);
        gfx.hLine(x, x + w - 1, y + h - 1, color);
        gfx.vLine(x,         y, y + h - 1, color);
        gfx.vLine(x + w - 1, y, y + h - 1, color);
    }
}
