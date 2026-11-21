package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.packet.DungeonActionPayload;
import net.ganyusbathwater.oririmod.network.packet.OpenDungeonSelectionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;

/**
 * Screen 2: Dungeon Dossier — framed preview banner, recessed scrollable lore
 * chamber, and gilded Back / Enter actions.
 */
@OnlyIn(Dist.CLIENT)
public class DungeonDetailScreen extends Screen {

    private static final int LINE_HEIGHT = 11;

    private final OpenDungeonSelectionPayload selectionData;
    private final String dungeonId;
    private final String displayName;
    private final String loreText;
    private final String previewTexturePath;

    private double scrollOffset = 0.0;

    public DungeonDetailScreen(OpenDungeonSelectionPayload selectionData, int dungeonIndex) {
        super(Component.literal(selectionData.displayNames().get(dungeonIndex)));
        this.selectionData = selectionData;
        this.dungeonId = selectionData.dungeonIds().get(dungeonIndex);
        this.displayName = selectionData.displayNames().get(dungeonIndex);
        this.loreText = selectionData.loreTexts().get(dungeonIndex);
        this.previewTexturePath = selectionData.previewTextures().get(dungeonIndex);
    }

    private int getPanelW() { return Math.min(372, this.width - 20); }
    private int getPanelH() { return Math.min(296, this.height - 20); }

    private int getPanelLeft() { return (this.width - getPanelW()) / 2; }
    private int getPanelTop()  { return (this.height - getPanelH()) / 2; }

    private int getImgW() { return Math.min(216, getPanelW() - 36); }
    private int getImgH() { return Math.min(104, Math.max(70, (getPanelH() - 160))); }

    private int getLoreTop()    { return getPanelTop() + 38 + getImgH() + 12; }
    private int getLoreBottom() { return getPanelTop() + getPanelH() - 42; }
    private int getLoreHeight() { return Math.max(32, getLoreBottom() - getLoreTop()); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();

        int left = getPanelLeft();
        int top  = getPanelTop();
        int pw   = getPanelW();
        int ph   = getPanelH();
        int bottomY = top + ph - 30;

        // ← Back button
        addRenderableWidget(new SanctuaryButton(
                left + 12,
                bottomY,
                76,
                20,
                Component.translatable("screen.oririmod.dungeon.detail_back"),
                ButtonTone.NEUTRAL,
                () -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new DungeonSelectionScreen(selectionData));
                    }
                }
        ));

        // Enter ▶ button
        addRenderableWidget(new SanctuaryButton(
                left + pw - 96,
                bottomY,
                84,
                20,
                Component.translatable("screen.oririmod.dungeon.detail_enter"),
                ButtonTone.EMERALD,
                () -> {
                    PacketDistributor.sendToServer(new DungeonActionPayload("SELECT", UUID.randomUUID(), dungeonId));
                    onClose();
                }
        ));
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(gfx);

        int left = getPanelLeft();
        int top  = getPanelTop();
        int pw   = getPanelW();
        int ph   = getPanelH();

        drawSanctuaryPanel(gfx, left, top, pw, ph);

        // Title Zone
        gfx.drawCenteredString(font,
                Component.literal("⚔ " + displayName + " ⚔").withStyle(ChatFormatting.BOLD),
                left + pw / 2, top + 12, TEXT_TITLE);
        drawGildedDivider(gfx, left + 12, top + 29, pw - 24, GOLD);

        // Gallery Image Frame
        int imgW = getImgW();
        int imgH = getImgH();
        int imgX = left + (pw - imgW) / 2;
        int imgY = top + 38;

        // Outer matted gallery surround
        gfx.fill(imgX - 3, imgY - 3, imgX + imgW + 3, imgY + imgH + 3, OUTER_SHADOW);
        gfx.fill(imgX, imgY, imgX + imgW, imgY + imgH, 0xFF0E0D1A);

        boolean drewTexture = false;
        if (previewTexturePath != null && !previewTexturePath.isBlank()) {
            ResourceLocation texLoc = ResourceLocation.tryParse(previewTexturePath);
            if (texLoc != null && net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(texLoc).isPresent()) {
                gfx.blit(texLoc, imgX, imgY, 0, 0, imgW, imgH, imgW, imgH);
                drewTexture = true;
            }
        }
        if (!drewTexture) {
            drawNoPreview(gfx, imgX, imgY, imgW, imgH);
        } else {
            // Subtle bottom/top vignette inside gallery frame for cohesion
            gfx.fillGradient(imgX, imgY, imgX + imgW, imgY + 12, 0x55000000, 0x00000000);
            gfx.fillGradient(imgX, imgY + imgH - 14, imgX + imgW, imgY + imgH, 0x00000000, 0x66000000);
        }

        drawBorder(gfx, imgX - 1, imgY - 1, imgW + 2, imgH + 2, GOLD_DIM);
        drawCornerFiligree(gfx, imgX - 3, imgY - 3, imgW + 6, imgH + 6, 7, GOLD);

        // Recessed Lore Chamber
        int loreX = left + 14;
        int loreY = getLoreTop();
        int loreW = pw - 28;
        int loreH = getLoreHeight();
        drawRecessedWell(gfx, loreX, loreY, loreW, loreH, BRONZE);

        // Footer Divider
        drawGildedDivider(gfx, left + 12, top + ph - 36, pw - 24, GOLD_DIM);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);

        int left = getPanelLeft();
        int pw   = getPanelW();
        int loreX = left + 14;
        int loreY = getLoreTop();
        int loreW = pw - 28;
        int loreH = getLoreHeight();

        if (loreText != null && !loreText.isBlank()) {
            int padX = 10;
            int padY = 7;
            int visibleH = Math.max(12, loreH - padY * 2);
            int maxTextW = loreW - padX * 2 - 10;

            List<FormattedCharSequence> lines = font.split(Component.literal(loreText), maxTextW);
            int totalH = lines.size() * LINE_HEIGHT;
            int maxScroll = Math.max(0, totalH - visibleH);
            this.scrollOffset = Mth.clamp(this.scrollOffset, 0.0, maxScroll);

            gfx.enableScissor(loreX + 2, loreY + 2, loreX + loreW - 2, loreY + loreH - 2);
            for (int i = 0; i < lines.size(); i++) {
                int yPos = loreY + padY + i * LINE_HEIGHT - (int) this.scrollOffset;
                if (yPos > loreY - LINE_HEIGHT && yPos < loreY + loreH) {
                    gfx.drawString(font, lines.get(i), loreX + padX, yPos, TEXT_BODY, false);
                }
            }
            gfx.disableScissor();

            if (maxScroll > 0) {
                float scrollFraction = (float) (this.scrollOffset / maxScroll);
                float visibleFraction = (float) visibleH / (float) totalH;
                drawScrollbar(gfx, loreX + loreW - 8, loreY + 4, loreH - 8, scrollFraction, visibleFraction);
            }
        }
    }

    private void drawNoPreview(GuiGraphics gfx, int imgX, int imgY, int imgW, int imgH) {
        int cx = imgX + imgW / 2;
        int cy = imgY + imgH / 2;
        drawDiamond(gfx, cx, cy - 10, 10, withAlpha(GOLD_DIM, 0x44));
        drawDiamond(gfx, cx, cy - 10, 5, withAlpha(GOLD, 0x88));
        String noPreview = Component.translatable("screen.oririmod.dungeon.no_preview").getString();
        gfx.drawCenteredString(font, noPreview, cx, cy + 8, TEXT_MUTED);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (loreText != null && !loreText.isBlank()) {
            int loreW = getPanelW() - 28;
            int visibleH = Math.max(12, getLoreHeight() - 14);
            List<FormattedCharSequence> lines = font.split(Component.literal(loreText), loreW - 30);
            int totalH = lines.size() * LINE_HEIGHT;
            int maxScroll = Math.max(0, totalH - visibleH);
            if (maxScroll > 0) {
                this.scrollOffset = Mth.clamp(this.scrollOffset - scrollY * 16.0, 0.0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
