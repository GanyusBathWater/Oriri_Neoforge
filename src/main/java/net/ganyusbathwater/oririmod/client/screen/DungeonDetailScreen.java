package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.packet.DungeonActionPayload;
import net.ganyusbathwater.oririmod.network.packet.OpenDungeonSelectionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Screen 2: Dungeon Detail — shows preview image and lore text for a specific dungeon.
 *
 * Layout (350 × 280 panel):
 * ┌──────────────────────────────────────────────┐
 * │  ⚔ [Dungeon Display Name] ⚔                  │  ← Title Zone (30px)
 * │──────────────────────────────────────────────│
 * │  ┌────────────────────────────────────────┐  │  ← Image Zone (128px tall, centered)
 * │  │       [Dungeon Preview Image]          │  │
 * │  └────────────────────────────────────────┘  │
 * │                                              │
 * │  [Lore text, word-wrapped, max 5 lines]      │  ← Lore Zone (bounded, clipped)
 * │                                              │
 * │──────────────────────────────────────────────│
 * │  [← Back]                      [Enter ▶]    │  ← Footer Zone (30px)
 * └──────────────────────────────────────────────┘
 */
@OnlyIn(Dist.CLIENT)
public class DungeonDetailScreen extends Screen {

    private static final int PANEL_W = 350;
    private static final int PANEL_H = 280;

    // Image dimensions (rendered size)
    private static final int IMG_W = 200;
    private static final int IMG_H = 100;

    // Colors
    private static final int C_BG      = 0xDD0A0A14;
    private static final int C_BORDER  = 0xFF6A4A2A;
    private static final int C_TITLE   = 0xFFFFD700;
    private static final int C_LORE    = 0xFFCCBB99;
    private static final int C_DIVIDER = 0xFF4A3A2A;
    private static final int C_MUTED   = 0xFF777766;
    private static final int C_IMG_BG  = 0xFF1A1A2A;

    private final OpenDungeonSelectionPayload selectionData;
    private final int dungeonIndex;

    private final String dungeonId;
    private final String displayName;
    private final String loreText;
    private final String previewTexturePath;
    
    private double scrollOffset = 0;

    public DungeonDetailScreen(OpenDungeonSelectionPayload selectionData, int dungeonIndex) {
        super(Component.literal(selectionData.displayNames().get(dungeonIndex)));
        this.selectionData = selectionData;
        this.dungeonIndex = dungeonIndex;
        this.dungeonId = selectionData.dungeonIds().get(dungeonIndex);
        this.displayName = selectionData.displayNames().get(dungeonIndex);
        this.loreText = selectionData.loreTexts().get(dungeonIndex);
        this.previewTexturePath = selectionData.previewTextures().get(dungeonIndex);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();

        int left = (width - PANEL_W) / 2;
        int top  = (height - PANEL_H) / 2;
        int bottomY = top + PANEL_H - 30;

        // ← Back button
        addRenderableWidget(Button.builder(
                Component.translatable("screen.oririmod.dungeon.detail_back").withStyle(ChatFormatting.GRAY),
                btn -> minecraft.setScreen(new DungeonSelectionScreen(selectionData)))
                .pos(left + 10, bottomY)
                .size(70, 20)
                .build());

        // Enter ▶ button
        addRenderableWidget(Button.builder(
                Component.translatable("screen.oririmod.dungeon.detail_enter").withStyle(ChatFormatting.GREEN),
                btn -> {
                    PacketDistributor.sendToServer(new DungeonActionPayload("SELECT", UUID.randomUUID(), dungeonId));
                    onClose();
                })
                .pos(left + PANEL_W - 80, bottomY)
                .size(70, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx, mouseX, mouseY, partial);

        int left = (width - PANEL_W) / 2;
        int top  = (height - PANEL_H) / 2;

        // Background panel
        gfx.fill(left, top, left + PANEL_W, top + PANEL_H, C_BG);
        drawBorder(gfx, left, top, PANEL_W, PANEL_H, C_BORDER);

        // Render widgets (buttons) on top of background
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(gfx, mouseX, mouseY, partial);
        }

        // ── Title Zone ──
        gfx.drawCenteredString(font,
                Component.literal("⚔ " + displayName + " ⚔").withStyle(ChatFormatting.BOLD),
                left + PANEL_W / 2, top + 10, C_TITLE);
        gfx.hLine(left + 8, left + PANEL_W - 8, top + 28, C_DIVIDER);

        // ── Image Zone ──
        int imgX = left + (PANEL_W - IMG_W) / 2;
        int imgY = top + 35;

        // Dark background for image area
        gfx.fill(imgX, imgY, imgX + IMG_W, imgY + IMG_H, C_IMG_BG);
        drawBorder(gfx, imgX, imgY, IMG_W, IMG_H, C_DIVIDER);

        if (!previewTexturePath.isBlank()) {
            try {
                ResourceLocation texLoc = ResourceLocation.tryParse(previewTexturePath);
                if (texLoc != null) {
                    gfx.blit(texLoc, imgX, imgY, 0, 0, IMG_W, IMG_H, IMG_W, IMG_H);
                }
            } catch (Exception ignored) {
                // Texture missing or invalid — fall through to placeholder
                drawNoPreview(gfx, imgX, imgY);
            }
        } else {
            drawNoPreview(gfx, imgX, imgY);
        }

        // ── Lore Zone ──
        int loreY = imgY + IMG_H + 10;
        int loreMaxWidth = PANEL_W - 30;
        int maxLoreLines = 8;
        int visibleHeight = maxLoreLines * 11;

        if (!loreText.isBlank()) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(loreText), loreMaxWidth);
            int totalHeight = lines.size() * 11;
            int maxScroll = Math.max(0, totalHeight - visibleHeight);
            this.scrollOffset = net.minecraft.util.Mth.clamp(this.scrollOffset, 0, maxScroll);
            
            // Enable Scissoring
            gfx.enableScissor(left + 10, loreY, left + PANEL_W - 10, loreY + visibleHeight);
            
            for (int i = 0; i < lines.size(); i++) {
                int yPos = loreY + (i * 11) - (int) this.scrollOffset;
                // Only draw if within visible vertical bounds to save performance
                if (yPos > loreY - 11 && yPos < loreY + visibleHeight) {
                    gfx.drawString(font, lines.get(i), left + 15, yPos, C_LORE, true);
                }
            }
            
            gfx.disableScissor();
            
            // Draw a subtle scroll indicator if scrolling is possible
            if (maxScroll > 0) {
                int scrollbarHeight = Math.max(10, (int)((visibleHeight / (float)totalHeight) * visibleHeight));
                int scrollbarY = loreY + (int)((this.scrollOffset / maxScroll) * (visibleHeight - scrollbarHeight));
                gfx.fill(left + PANEL_W - 15, loreY, left + PANEL_W - 13, loreY + visibleHeight, C_IMG_BG);
                gfx.fill(left + PANEL_W - 15, scrollbarY, left + PANEL_W - 13, scrollbarY + scrollbarHeight, C_BORDER);
            }
        }

        // ── Footer divider ──
        gfx.hLine(left + 8, left + PANEL_W - 8, top + PANEL_H - 38, C_DIVIDER);
    }

    private void drawNoPreview(GuiGraphics gfx, int imgX, int imgY) {
        String noPreview = Component.translatable("screen.oririmod.dungeon.no_preview").getString();
        gfx.drawCenteredString(font, noPreview, imgX + IMG_W / 2, imgY + IMG_H / 2 - 4, C_MUTED);
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!loreText.isBlank()) {
            int loreMaxWidth = PANEL_W - 30;
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(loreText), loreMaxWidth);
            int totalHeight = lines.size() * 11;
            int visibleHeight = 8 * 11;
            int maxScroll = Math.max(0, totalHeight - visibleHeight);
            
            if (maxScroll > 0) {
                this.scrollOffset -= scrollY * 11;
                this.scrollOffset = net.minecraft.util.Mth.clamp(this.scrollOffset, 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void drawBorder(GuiGraphics gfx, int x, int y, int w, int h, int color) {
        gfx.hLine(x, x + w - 1, y,         color);
        gfx.hLine(x, x + w - 1, y + h - 1, color);
        gfx.vLine(x,         y, y + h - 1, color);
        gfx.vLine(x + w - 1, y, y + h - 1, color);
    }
}
