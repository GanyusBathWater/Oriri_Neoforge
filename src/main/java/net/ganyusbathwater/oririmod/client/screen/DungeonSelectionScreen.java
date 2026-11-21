package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.packet.OpenDungeonSelectionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;

/**
 * Screen 1: Dungeon Selection Ledger — scrollable list of clickable dungeon cards
 * with miniature preview thumbnails, gilded hover highlights, and custom scrollbar.
 */
@OnlyIn(Dist.CLIENT)
public class DungeonSelectionScreen extends Screen {

    private static final int CARD_HEIGHT = 54;
    private static final int CARD_GAP = 6;
    private static final int THUMB_W = 72;
    private static final int THUMB_H = 38;

    private final OpenDungeonSelectionPayload data;
    private double scrollOffset = 0.0;

    public DungeonSelectionScreen(OpenDungeonSelectionPayload data) {
        super(Component.translatable("screen.oririmod.dungeon.select_title"));
        this.data = data;
    }

    private int getPanelW() { return Math.min(372, this.width - 20); }
    private int getPanelH() { return Math.min(272, this.height - 20); }

    private int getPanelLeft() { return (this.width - getPanelW()) / 2; }
    private int getPanelTop()  { return (this.height - getPanelH()) / 2; }

    private int getListLeft()   { return getPanelLeft() + 12; }
    private int getListTop()    { return getPanelTop() + 38; }
    private int getListWidth()  { return getPanelW() - 24; }
    private int getListHeight() { return getPanelH() - 78; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();

        int left = getPanelLeft();
        int top  = getPanelTop();
        int pw   = getPanelW();
        int ph   = getPanelH();

        addRenderableWidget(new SanctuaryButton(
                left + pw - 78,
                top + ph - 30,
                66,
                20,
                Component.translatable("screen.oririmod.dungeon.close"),
                ButtonTone.NEUTRAL,
                this::onClose
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

        // Header title
        gfx.drawCenteredString(font,
                Component.translatable("screen.oririmod.dungeon.select_title").withStyle(ChatFormatting.BOLD),
                left + pw / 2, top + 12, TEXT_TITLE);

        // Dungeon count badge on top-right
        int count = data.dungeonIds().size();
        String badge = count + (count == 1 ? " Realm" : " Realms");
        int badgeW = font.width(badge) + 10;
        drawPill(gfx, font, badge, left + pw - badgeW - 14, top + 10, 0xCC1A1526, BRONZE, GOLD);

        // Header divider
        drawGildedDivider(gfx, left + 12, top + 29, pw - 24, GOLD);

        // Recessed list chamber
        int lx = getListLeft();
        int ly = getListTop();
        int lw = getListWidth();
        int lh = getListHeight();
        drawRecessedWell(gfx, lx, ly, lw, lh, BRONZE);

        // Footer divider & hint
        drawGildedDivider(gfx, left + 12, top + ph - 36, pw - 24, GOLD_DIM);
        gfx.drawString(font,
                Component.translatable("screen.oririmod.dungeon.select_hint"),
                left + 14, top + ph - 24, TEXT_MUTED, false);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);

        int lx = getListLeft();
        int ly = getListTop();
        int lw = getListWidth();
        int lh = getListHeight();

        int count = data.dungeonIds().size();
        if (count == 0) {
            gfx.drawCenteredString(font,
                    Component.translatable("screen.oririmod.dungeon.none_available"),
                    lx + lw / 2, ly + lh / 2 - 4, TEXT_MUTED);
            return;
        }

        int contentH = count * CARD_HEIGHT + Math.max(0, count - 1) * CARD_GAP + 12;
        int maxScroll = Math.max(0, contentH - lh);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0.0, maxScroll);

        boolean hasScroll = maxScroll > 0;
        int cardW = lw - (hasScroll ? 18 : 12);
        int cardX = lx + 6;
        boolean inList = mouseX >= lx + 1 && mouseX <= lx + lw - 1 && mouseY >= ly + 1 && mouseY <= ly + lh - 1;

        gfx.enableScissor(lx + 1, ly + 1, lx + lw - 1, ly + lh - 1);

        for (int i = 0; i < count; i++) {
            int cardY = ly + 6 + i * (CARD_HEIGHT + CARD_GAP) - (int) this.scrollOffset;
            if (cardY + CARD_HEIGHT < ly || cardY > ly + lh) continue;

            boolean hovered = inList && mouseX >= cardX && mouseX <= cardX + cardW
                    && mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT;

            renderDungeonCard(gfx, i, cardX, cardY, cardW, CARD_HEIGHT, hovered);
        }

        gfx.disableScissor();

        if (hasScroll) {
            float scrollFraction = (float) (this.scrollOffset / maxScroll);
            float visibleFraction = (float) lh / (float) contentH;
            drawScrollbar(gfx, lx + lw - 8, ly + 4, lh - 8, scrollFraction, visibleFraction);
        }
    }

    private void renderDungeonCard(GuiGraphics gfx, int index, int x, int y, int w, int h, boolean hovered) {
        int bgTop = hovered ? 0xEE231E3A : 0xDD151324;
        int bgBot = hovered ? 0xEE171429 : 0xDD0F0E1B;
        int border = hovered ? GOLD : BRONZE;

        gfx.fillGradient(x, y, x + w, y + h, bgTop, bgBot);
        drawBorder(gfx, x, y, w, h, border);

        // Left vertical accent stripe
        gfx.fill(x + 1, y + 1, x + (hovered ? 4 : 3), y + h - 1, hovered ? GOLD_BRIGHT : GOLD_DIM);
        if (hovered) {
            gfx.hLine(x + 1, x + w - 2, y + 1, withAlpha(GOLD_BRIGHT, 0x55));
        }

        // Miniature preview thumbnail
        int thumbX = x + 9;
        int thumbY = y + (h - THUMB_H) / 2;
        gfx.fill(thumbX, thumbY, thumbX + THUMB_W, thumbY + THUMB_H, 0xFF0A0914);

        boolean drewPreview = false;
        String previewPath = index < data.previewTextures().size() ? data.previewTextures().get(index) : "";
        if (previewPath != null && !previewPath.isBlank()) {
            ResourceLocation texLoc = ResourceLocation.tryParse(previewPath);
            if (texLoc != null && Minecraft.getInstance().getResourceManager().getResource(texLoc).isPresent()) {
                gfx.blit(texLoc, thumbX, thumbY, 0, 0, THUMB_W, THUMB_H, THUMB_W, THUMB_H);
                drewPreview = true;
            }
        }
        if (!drewPreview) {
            // Subtle gilded diamond crest placeholder
            int cx = thumbX + THUMB_W / 2;
            int cy = thumbY + THUMB_H / 2;
            drawDiamond(gfx, cx, cy, 8, withAlpha(GOLD_DIM, 0x55));
            drawDiamond(gfx, cx, cy, 4, withAlpha(GOLD, 0x99));
        }
        drawBorder(gfx, thumbX, thumbY, THUMB_W, THUMB_H, hovered ? GOLD : GOLD_DIM);

        // Text column
        int textX = thumbX + THUMB_W + 10;
        int arrowColW = 30;
        int textMaxW = Math.max(40, w - (textX - x) - arrowColW);

        String name = data.displayNames().get(index);
        String desc = data.descriptions().get(index);

        String truncatedName = truncate(font, name, textMaxW);
        gfx.drawString(font, truncatedName, textX, y + 8, hovered ? GOLD_BRIGHT : TEXT_TITLE, true);

        // Up to 2 wrapped lines of description
        if (desc != null && !desc.isBlank()) {
            List<FormattedCharSequence> lines = font.split(Component.literal(desc), textMaxW);
            int maxLines = Math.min(2, lines.size());
            for (int l = 0; l < maxLines; l++) {
                gfx.drawString(font, lines.get(l), textX, y + 22 + l * 10, hovered ? TEXT_PRIMARY : TEXT_BODY, false);
            }
        }

        // Right arrow cue
        int arrowX = x + w - 20 + (hovered ? 2 : 0);
        int arrowY = y + (h - 8) / 2;
        gfx.drawString(font, "→", arrowX, arrowY, hovered ? GOLD_BRIGHT : GOLD_DIM, hovered);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) return false;

        int lx = getListLeft();
        int ly = getListTop();
        int lw = getListWidth();
        int lh = getListHeight();

        if (mouseX < lx + 1 || mouseX > lx + lw - 1 || mouseY < ly + 1 || mouseY > ly + lh - 1) {
            return false;
        }

        int count = data.dungeonIds().size();
        int contentH = count * CARD_HEIGHT + Math.max(0, count - 1) * CARD_GAP + 12;
        boolean hasScroll = contentH > lh;
        int cardW = lw - (hasScroll ? 18 : 12);
        int cardX = lx + 6;

        for (int i = 0; i < count; i++) {
            int cardY = ly + 6 + i * (CARD_HEIGHT + CARD_GAP) - (int) this.scrollOffset;
            if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new DungeonDetailScreen(data, i));
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int count = data.dungeonIds().size();
        int contentH = count * CARD_HEIGHT + Math.max(0, count - 1) * CARD_GAP + 12;
        int maxScroll = Math.max(0, contentH - getListHeight());
        if (maxScroll > 0) {
            this.scrollOffset = Mth.clamp(this.scrollOffset - scrollY * 22.0, 0.0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
