package net.ganyusbathwater.oririmod.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Shared programmatic rendering primitives and custom widgets for the
 * "Ancient Gilded Sanctuary" Dungeon & Dialogue GUI suite.
 */
@OnlyIn(Dist.CLIENT)
final class DungeonGuiStyle {

    private DungeonGuiStyle() {}

    // ── Core Obsidian & Antique Gold Palette ──
    static final int BG_TOP        = 0xF212111E;
    static final int BG_BOT        = 0xF50A0912;
    static final int OUTER_SHADOW  = 0xFF050408;
    static final int WELL_BG       = 0xE608070F;
    static final int CARD_BG       = 0xDC151324;
    static final int CARD_HOVER    = 0xEB211D36;

    static final int GOLD_BRIGHT   = 0xFFFFE596;
    static final int GOLD          = 0xFFD9A94E;
    static final int GOLD_DIM      = 0xFF8A6832;
    static final int BRONZE        = 0xFF4F3B20;
    static final int DIVIDER       = 0xFF3A2C1E;

    static final int TEXT_TITLE    = 0xFFFFE596;
    static final int TEXT_PRIMARY  = 0xFFF2EADF;
    static final int TEXT_BODY     = 0xFFCBBEA8;
    static final int TEXT_MUTED    = 0xFF7E756B;

    // ── Semantic Accents ──
    static final int EMERALD_LIGHT = 0xFF6EF09A;
    static final int EMERALD       = 0xFF39B86B;
    static final int EMERALD_DARK  = 0xFF143524;

    static final int CRIMSON_LIGHT = 0xFFFF7A7A;
    static final int CRIMSON       = 0xFFC94444;
    static final int CRIMSON_DARK  = 0xFF3A1216;

    static final int AMBER_LIGHT   = 0xFFFFD166;
    static final int AMBER         = 0xFFE09F3E;
    static final int AMBER_DARK    = 0xFF3A2810;

    static final int CYAN_LIGHT    = 0xFF7BE8F5;
    static final int CYAN          = 0xFF3BA8C4;
    static final int CYAN_DARK     = 0xFF102A36;

    static final int AMETHYST      = 0xFF7B5EA7;
    static final int AMETHYST_DIM  = 0xFF3D2D59;

    // ── Panel & Chrome Drawing ──

    /**
     * Draws a notched Ancient Gilded Sanctuary panel with vertical obsidian gradient,
     * double beveled antique-gold/accent border, and gilded corner filigree.
     */
    static void drawSanctuaryPanel(GuiGraphics gfx, int x, int y, int w, int h) {
        drawSanctuaryPanel(gfx, x, y, w, h, GOLD, BRONZE);
    }

    static void drawSanctuaryPanel(GuiGraphics gfx, int x, int y, int w, int h, int accentColor, int outerBorder) {
        // Outer drop shadow (notched corners)
        gfx.fill(x + 2, y + h, x + w - 1, y + h + 2, 0x66000000);
        gfx.fill(x + w, y + 2, x + w + 2, y + h - 1, 0x66000000);

        // Outer dark silhouette (1px chamfered corners)
        gfx.fill(x + 1, y, x + w - 1, y + h, OUTER_SHADOW);
        gfx.fill(x, y + 1, x + w, y + h - 1, OUTER_SHADOW);

        // Main gradient interior
        gfx.fillGradient(x + 2, y + 2, x + w - 2, y + h - 2, BG_TOP, BG_BOT);

        // Outer rim
        gfx.hLine(x + 2, x + w - 3, y + 1, outerBorder);
        gfx.hLine(x + 2, x + w - 3, y + h - 2, outerBorder);
        gfx.vLine(x + 1, y + 2, y + h - 3, outerBorder);
        gfx.vLine(x + w - 2, y + 2, y + h - 3, outerBorder);

        // Subtle inner highlight rim
        int innerRim = withAlpha(accentColor, 0x38);
        gfx.hLine(x + 3, x + w - 4, y + 2, withAlpha(accentColor, 0x55));
        gfx.hLine(x + 3, x + w - 4, y + h - 3, innerRim);
        gfx.vLine(x + 2, y + 3, y + h - 4, innerRim);
        gfx.vLine(x + w - 3, y + 3, y + h - 4, innerRim);

        // Ornate corner filigree brackets
        drawCornerFiligree(gfx, x + 1, y + 1, w - 2, h - 2, 9, accentColor);
    }

    /**
     * Draws L-shaped gilded corner brackets with tiny inner accent studs.
     */
    static void drawCornerFiligree(GuiGraphics gfx, int x, int y, int w, int h, int len, int color) {
        int r = x + w - 1;
        int b = y + h - 1;

        // Top-left
        gfx.hLine(x + 1, x + len, y, color);
        gfx.vLine(x, y + 1, y + len, color);
        gfx.fill(x + 2, y + 2, x + 4, y + 4, withAlpha(color, 0xCC));

        // Top-right
        gfx.hLine(r - len, r - 1, y, color);
        gfx.vLine(r, y + 1, y + len, color);
        gfx.fill(r - 3, y + 2, r - 1, y + 4, withAlpha(color, 0xCC));

        // Bottom-left
        gfx.hLine(x + 1, x + len, b, color);
        gfx.vLine(x, b - len, b - 1, color);
        gfx.fill(x + 2, b - 3, x + 4, b - 1, withAlpha(color, 0xCC));

        // Bottom-right
        gfx.hLine(r - len, r - 1, b, color);
        gfx.vLine(r, b - len, b - 1, color);
        gfx.fill(r - 3, b - 3, r - 1, b - 1, withAlpha(color, 0xCC));
    }

    /**
     * Draws a recessed dark container well with subtle inset shadow and bottom catch-light.
     */
    static void drawRecessedWell(GuiGraphics gfx, int x, int y, int w, int h, int borderColor) {
        gfx.fill(x, y, x + w, y + h, WELL_BG);
        drawBorder(gfx, x, y, w, h, borderColor);
        // Inner top shadow
        gfx.hLine(x + 1, x + w - 2, y + 1, 0x66000000);
        gfx.vLine(x + 1, y + 1, y + h - 2, 0x44000000);
    }

    /**
     * Draws a gilded horizontal divider tapering softly at the edges with a diamond center clasp.
     */
    static void drawGildedDivider(GuiGraphics gfx, int x, int y, int w, int accentColor) {
        int cx = x + w / 2;
        int dim = withAlpha(accentColor, 0x45);
        int mid = withAlpha(accentColor, 0x99);
        int fade = Math.max(8, w / 6);

        gfx.hLine(x, x + fade, y, dim);
        gfx.hLine(x + fade, cx - 5, y, mid);
        gfx.hLine(cx + 5, x + w - fade, y, mid);
        gfx.hLine(x + w - fade, x + w - 1, y, dim);

        drawDiamond(gfx, cx, y, 2, accentColor);
    }

    static void drawDiamond(GuiGraphics gfx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int span = r - Math.abs(dy);
            gfx.hLine(cx - span, cx + span, cy + dy, color);
        }
    }

    /**
     * Draws a vertical gold scrollbar inside a track.
     */
    static void drawScrollbar(GuiGraphics gfx, int x, int y, int h, float scrollFraction, float visibleFraction) {
        gfx.fill(x, y, x + 4, y + h, 0x9906060C);
        drawBorder(gfx, x, y, 4, h, 0xFF2B221A);

        int thumbH = Math.max(14, Math.round(h * Mth.clamp(visibleFraction, 0.05F, 1.0F)));
        int travel = Math.max(0, h - thumbH);
        int thumbY = y + Math.round(travel * Mth.clamp(scrollFraction, 0.0F, 1.0F));

        gfx.fill(x + 1, thumbY, x + 3, thumbY + thumbH, GOLD);
        gfx.fill(x + 1, thumbY, x + 3, thumbY + 1, GOLD_BRIGHT);
    }

    /**
     * Draws a compact rounded-look status pill badge.
     */
    static void drawPill(GuiGraphics gfx, Font font, String text, int x, int y, int bg, int border, int textColor) {
        int tw = font.width(text);
        int pw = tw + 10;
        int ph = 13;
        gfx.fill(x + 1, y, x + pw - 1, y + ph, bg);
        gfx.fill(x, y + 1, x + pw, y + ph - 1, bg);
        gfx.hLine(x + 1, x + pw - 2, y, border);
        gfx.hLine(x + 1, x + pw - 2, y + ph - 1, border);
        gfx.vLine(x, y + 1, y + ph - 2, border);
        gfx.vLine(x + pw - 1, y + 1, y + ph - 2, border);
        gfx.drawString(font, text, x + 5, y + 3, textColor, false);
    }

    static void drawBorder(GuiGraphics gfx, int x, int y, int w, int h, int color) {
        gfx.hLine(x, x + w - 1, y, color);
        gfx.hLine(x, x + w - 1, y + h - 1, color);
        gfx.vLine(x, y, y + h - 1, color);
        gfx.vLine(x + w - 1, y, y + h - 1, color);
    }

    static String truncate(Font font, String text, int maxWidth) {
        if (text == null || maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisW = font.width(ellipsis);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (font.width(sb.toString() + c) + ellipsisW > maxWidth) break;
            sb.append(c);
        }
        return sb + ellipsis;
    }

    static int withAlpha(int argb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (argb & 0x00FFFFFF);
    }

    static void fillHorizontalGradient(GuiGraphics gfx, int x1, int y1, int x2, int y2, int cLeft, int cRight) {
        int width = Math.max(1, x2 - x1);
        int a1 = (cLeft >>> 24) & 0xFF, r1 = (cLeft >>> 16) & 0xFF, g1 = (cLeft >>> 8) & 0xFF, b1 = cLeft & 0xFF;
        int a2 = (cRight >>> 24) & 0xFF, r2 = (cRight >>> 16) & 0xFF, g2 = (cRight >>> 8) & 0xFF, b2 = cRight & 0xFF;
        for (int i = 0; i < width; i++) {
            float t = (float) i / width;
            int a = (int) Mth.lerp(t, a1, a2);
            int r = (int) Mth.lerp(t, r1, r2);
            int g = (int) Mth.lerp(t, g1, g2);
            int b = (int) Mth.lerp(t, b1, b2);
            int col = (a << 24) | (r << 16) | (g << 8) | b;
            gfx.fill(x1 + i, y1, x1 + i + 1, y2, col);
        }
    }

    // ── Custom Sanctuary Button Widget ──

    enum ButtonTone {
        NEUTRAL,
        PRIMARY,
        EMERALD,
        CRIMSON,
        CYAN
    }

    static class SanctuaryButton extends AbstractButton {
        private final Runnable onPress;
        private ButtonTone tone;
        private float fillProgress = -1.0F;
        private int fillOverlayColor = 0;

        SanctuaryButton(int x, int y, int w, int h, Component message, ButtonTone tone, Runnable onPress) {
            super(x, y, w, h, message);
            this.tone = tone;
            this.onPress = onPress;
        }

        void setTone(ButtonTone tone) {
            this.tone = tone;
        }

        /**
         * Optional live progress fill drawn inside the button (e.g. cooldown recharge bar).
         * Pass negative value to disable.
         */
        void setFillProgress(float progress, int fillColor) {
            this.fillProgress = progress;
            this.fillOverlayColor = fillColor;
        }

        @Override
        public void onPress() {
            if (this.active && this.onPress != null) {
                this.onPress.run();
            }
        }

        @Override
        protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            boolean hovered = this.active && this.isHoveredOrFocused();

            int bgTop;
            int bgBot;
            int border;
            int innerHighlight;
            int textColor;

            if (!this.active) {
                bgTop = 0xCC12111A;
                bgBot = 0xCC0C0B12;
                border = 0xFF332D2B;
                innerHighlight = 0x22FFFFFF;
                textColor = TEXT_MUTED;
            } else {
                switch (tone) {
                    case PRIMARY -> {
                        bgTop = hovered ? 0xEE4A3519 : 0xDD322413;
                        bgBot = hovered ? 0xEE2B1E0D : 0xDD1C140B;
                        border = hovered ? GOLD_BRIGHT : GOLD;
                        innerHighlight = hovered ? 0x55FFE596 : 0x33D9A94E;
                        textColor = hovered ? 0xFFFFF7D6 : GOLD_BRIGHT;
                    }
                    case EMERALD -> {
                        bgTop = hovered ? 0xEE1E5937 : 0xDD143D26;
                        bgBot = hovered ? 0xEE103620 : 0xDD0C2417;
                        border = hovered ? EMERALD_LIGHT : EMERALD;
                        innerHighlight = hovered ? 0x556EF09A : 0x3339B86B;
                        textColor = hovered ? 0xFFE8FFF0 : 0xFFB8F5CC;
                    }
                    case CRIMSON -> {
                        bgTop = hovered ? 0xEE5E1E24 : 0xDD40141A;
                        bgBot = hovered ? 0xEE381014 : 0xDD240B0F;
                        border = hovered ? CRIMSON_LIGHT : CRIMSON;
                        innerHighlight = hovered ? 0x55FF7A7A : 0x33C94444;
                        textColor = hovered ? 0xFFFFEAEA : 0xFFF5B8B8;
                    }
                    case CYAN -> {
                        bgTop = hovered ? 0xEE1A4E61 : 0xDD113442;
                        bgBot = hovered ? 0xEE0E2F3D : 0xDD0A1E29;
                        border = hovered ? CYAN_LIGHT : CYAN;
                        innerHighlight = hovered ? 0x557BE8F5 : 0x333BA8C4;
                        textColor = hovered ? 0xFFE8FDFF : 0xFFB8EEF5;
                    }
                    default -> {
                        bgTop = hovered ? 0xEE26223A : 0xDD181626;
                        bgBot = hovered ? 0xEE161424 : 0xDD100F1B;
                        border = hovered ? GOLD : GOLD_DIM;
                        innerHighlight = hovered ? 0x44D9A94E : 0x22FFFFFF;
                        textColor = hovered ? TEXT_TITLE : TEXT_PRIMARY;
                    }
                }
            }

            // Soft outer outline
            gfx.fill(x, y, x + w, y + h, OUTER_SHADOW);
            // Gradient fill
            gfx.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, bgTop, bgBot);

            // Optional progress fill (e.g. Respawn cooldown recharge)
            if (fillProgress >= 0.0F) {
                int fillW = Math.round((w - 2) * Mth.clamp(fillProgress, 0.0F, 1.0F));
                if (fillW > 0) {
                    gfx.fill(x + 1, y + 1, x + 1 + fillW, y + h - 1, fillOverlayColor);
                    gfx.vLine(x + fillW, y + 1, y + h - 2, withAlpha(EMERALD_LIGHT, 0xCC));
                }
            }

            // Border & top catch-light
            drawBorder(gfx, x, y, w, h, border);
            gfx.hLine(x + 1, x + w - 2, y + 1, innerHighlight);

            // Tiny corner accent pixels on hover
            if (hovered) {
                gfx.fill(x + 2, y + 2, x + 3, y + 3, border);
                gfx.fill(x + w - 3, y + 2, x + w - 2, y + 3, border);
                gfx.fill(x + 2, y + h - 3, x + 3, y + h - 2, border);
                gfx.fill(x + w - 3, y + h - 3, x + w - 2, y + h - 2, border);
            }

            Font font = Minecraft.getInstance().font;
            gfx.drawCenteredString(font, getMessage(), x + w / 2, y + (h - 8) / 2, textColor);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
