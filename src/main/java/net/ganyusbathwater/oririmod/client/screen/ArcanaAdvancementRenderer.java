package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.item.custom.ArcanaItem;
import net.minecraft.Util;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.Random;

/**
 * Custom celestial renderer for the Major Arcana advancement tab:
 * <ul>
 *   <li>Pseudo-3D perspective starfield with depth-scaled stars, gentle forward Z-drift,
 *       twinkling hues, cosmic nebula clouds, and smooth drag parallax.</li>
 *   <li>Celestial zodiac wheel connecting the central starting slot to all 22 Major Arcana
 *       clockwise and culminating at the 12 o'clock Mastery slot.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class ArcanaAdvancementRenderer {

    public static final int VIEW_W = 234;
    public static final int VIEW_H = 113;

    // AdvancementWidget.draw() blits its 26x26 frame sprite at (scrollX + x + 3, scrollY + y),
    // so the exact visual center of every slot is (+16, +13) relative to (widget.getX(), widget.getY()).
    public static final int SLOT_CENTER_OFFSET_X = 16;
    public static final int SLOT_CENTER_OFFSET_Y = 13;

    private static final int STAR_COUNT = 210;
    private static final Star[] STARS = new Star[STAR_COUNT];
    private static final NebulaCloud[] NEBULAE = new NebulaCloud[6];

    private static final int[] STAR_PALETTE = {
            0xFFF4F8FF, // Astral white
            0xFFFFE6A3, // Warm starlight gold
            0xFF8BE9FD, // Pale celestial cyan
            0xFFD8B4FF, // Soft amethyst
            0xFFC7DCFF  // Ice blue
    };

    private record Star(float x, float y, float zOffset, float speed, float twinklePhase, float twinkleSpeed, int rgb) {}

    private record NebulaCloud(float x, float y, int rx, int ry, int color) {}

    static {
        Random rng = new Random(0xA4CA9A22L);
        for (int i = 0; i < STAR_COUNT; i++) {
            // Spread stars wider than viewport so parallax panning reveals rich star fields
            float sx = (rng.nextFloat() * 2.0F - 1.0F) * 210.0F;
            float sy = (rng.nextFloat() * 2.0F - 1.0F) * 145.0F;
            float zOff = rng.nextFloat();
            float speed = 0.045F + rng.nextFloat() * 0.075F;
            float tPhase = rng.nextFloat() * ((float) Math.PI * 2.0F);
            float tSpeed = 1.4F + rng.nextFloat() * 2.8F;
            int rgb = STAR_PALETTE[rng.nextInt(STAR_PALETTE.length)];
            STARS[i] = new Star(sx, sy, zOff, speed, tPhase, tSpeed, rgb);
        }

        NEBULAE[0] = new NebulaCloud(42.0F, 28.0F, 44, 30, 0x16522296);
        NEBULAE[1] = new NebulaCloud(186.0F, 82.0F, 48, 34, 0x151C4B82);
        NEBULAE[2] = new NebulaCloud(118.0F, 56.0F, 54, 38, 0x14441A70);
        NEBULAE[3] = new NebulaCloud(195.0F, 22.0F, 36, 26, 0x13175C6E);
        NEBULAE[4] = new NebulaCloud(34.0F, 92.0F, 40, 28, 0x1463237A);
        NEBULAE[5] = new NebulaCloud(114.0F, 18.0F, 34, 24, 0x1282561C);
    }

    private ArcanaAdvancementRenderer() {}

    /**
     * Renders the deep cosmic backdrop, parallax nebula clouds, and pseudo-3D drifting starfield.
     */
    public static void renderStarfieldBackdrop(GuiGraphics gui, double scrollX, double scrollY) {
        double timeSec = (Util.getMillis() % 3_600_000L) * 0.001D;

        // 1. Deep cosmic gradient sky
        gui.fillGradient(0, 0, VIEW_W, VIEW_H / 2, 0xFF04030D, 0xFF0B0721);
        gui.fillGradient(0, VIEW_H / 2, VIEW_W, VIEW_H, 0xFF0B0721, 0xFF050411);

        // 2. Soft layered nebula clouds with gentle drag parallax
        float nebPanX = (float) (scrollX * 0.14D);
        float nebPanY = (float) (scrollY * 0.14D);
        for (NebulaCloud c : NEBULAE) {
            int cx = Math.round(wrap(c.x + nebPanX, -c.rx, VIEW_W + c.rx));
            int cy = Math.round(wrap(c.y + nebPanY, -c.ry, VIEW_H + c.ry));
            drawSoftNebula(gui, cx, cy, c.rx, c.ry, c.color);
        }

        // 3. Pseudo-3D perspective starfield (depth Z in [0.16 .. 1.0], moving toward viewer)
        float halfW = VIEW_W * 0.5F;
        float halfH = VIEW_H * 0.5F;
        float panX = (float) ((scrollX + 48.0D) * 0.38D);
        float panY = (float) ((scrollY + 105.0D) * 0.38D);

        for (Star star : STARS) {
            // Smooth forward motion in Z with seamless wrap at Z=1.0 -> Z=0.16
            float zCycle = (float) ((star.zOffset - timeSec * star.speed) % 1.0D);
            if (zCycle < 0.0F) {
                zCycle += 1.0F;
            }
            float z = 0.16F + zCycle * 0.84F; // [0.16 .. 1.0]
            float invZ = 1.0F / z;

            float sx = halfW + (star.x + panX) * invZ;
            float sy = halfH + (star.y + panY) * invZ;

            if (sx < -4.0F || sx > VIEW_W + 4.0F || sy < -4.0F || sy > VIEW_H + 4.0F) {
                continue;
            }

            // Smooth depth fade-in from deep space (z near 1.0) and crisp brightness near camera
            float depthAlpha = Mth.clamp((1.0F - z) / 0.22F, 0.0F, 1.0F);
            float nearFactor = Mth.clamp((1.0F - z) / 0.84F, 0.0F, 1.0F);
            float twinkle = 0.76F + 0.24F * (float) Math.sin(timeSec * star.twinkleSpeed + star.twinklePhase);
            int alpha = Mth.clamp(Math.round((0.40F + 0.60F * nearFactor) * depthAlpha * twinkle * 255.0F), 0, 255);
            if (alpha < 14) {
                continue;
            }

            int ix = Math.round(sx);
            int iy = Math.round(sy);
            int coreColor = (alpha << 24) | (star.rgb & 0x00FFFFFF);

            if (z < 0.34F) {
                // Close foreground star: subtle radial motion trail + 2x2 bright core + 1px twinkle cross
                float prevInvZ = 1.0F / Math.min(1.0F, z + 0.045F);
                int tx = Math.round(halfW + (star.x + panX) * prevInvZ);
                int ty = Math.round(halfH + (star.y + panY) * prevInvZ);
                int trailColor = ((alpha / 3) << 24) | (star.rgb & 0x00FFFFFF);
                gui.fill(tx, ty, tx + 1, ty + 1, trailColor);

                gui.fill(ix, iy, ix + 2, iy + 2, coreColor);
                int glintColor = ((alpha / 2) << 24) | (star.rgb & 0x00FFFFFF);
                gui.fill(ix - 1, iy, ix, iy + 1, glintColor);
                gui.fill(ix + 2, iy + 1, ix + 3, iy + 2, glintColor);
                gui.fill(ix, iy - 1, ix + 1, iy, glintColor);
                gui.fill(ix + 1, iy + 2, ix + 2, iy + 3, glintColor);
            } else if (z < 0.62F) {
                // Mid-depth star: crisp 1x1 bright core with faint soft halo pixel
                gui.fill(ix, iy, ix + 1, iy + 1, coreColor);
                int haloColor = ((alpha / 4) << 24) | (star.rgb & 0x00FFFFFF);
                gui.fill(ix + 1, iy, ix + 2, iy + 1, haloColor);
            } else {
                // Distant background star: delicate 1x1 speck
                gui.fill(ix, iy, ix + 1, iy + 1, coreColor);
            }
        }
    }

    /**
     * Renders the celestial zodiac wheel, inner rotating arcane sigil, and constellation
     * connectors in scrolled canvas space behind the advancement slots.
     */
    public static void renderArcanaWheel(GuiGraphics gui, int scrollX, int scrollY,
                                         AdvancementWidget rootWidget,
                                         Map<AdvancementHolder, AdvancementWidget> widgets,
                                         Map<AdvancementHolder, Boolean> unlockedMap) {
        int centerX = scrollX + rootWidget.getX() + SLOT_CENTER_OFFSET_X;
        int centerY = scrollY + rootWidget.getY() + SLOT_CENTER_OFFSET_Y;
        double timeSec = (Util.getMillis() % 3_600_000L) * 0.001D;

        // 1. Subtle rotating inner arcane sigil rings around the central starting slot
        drawDashedCircle(gui, centerX, centerY, 31.0D, 36, timeSec * 0.14D, 0x55A77BFF);
        drawDashedCircle(gui, centerX, centerY, 42.0D, 48, -timeSec * 0.09D, 0x44D9A94E);

        // 8 slowly rotating starlight compass ticks inside the inner ring
        for (int i = 0; i < 8; i++) {
            double a = timeSec * 0.14D + i * (Math.PI * 0.25D);
            int x0 = centerX + (int) Math.round(Math.cos(a) * 26.0D);
            int y0 = centerY + (int) Math.round(Math.sin(a) * 26.0D);
            int x1 = centerX + (int) Math.round(Math.cos(a) * 44.0D);
            int y1 = centerY + (int) Math.round(Math.sin(a) * 44.0D);
            drawPixelLine(gui, x0, y0, x1, y1, 0x3AD9A94E);
        }

        // 2. Outer zodiac circle passing through all 23 ring slots (radius 132px)
        drawSolidRing(gui, centerX, centerY, 132.0D, 180, 0x4562438F);
        drawDashedCircle(gui, centerX, centerY, 148.0D, 96, timeSec * 0.04D, 0x33D9A94E);
        drawDashedCircle(gui, centerX, centerY, 116.0D, 84, -timeSec * 0.05D, 0x2E7B5EA7);

        // Build ordered array of ring widget centers (0..21 = Arcana 0..XXI, 22 = Mastery at 12 o'clock)
        ArcanaItem.ArcanaType[] types = ArcanaItem.ArcanaType.values();
        int totalRing = types.length + 1;
        int[] rx = new int[totalRing];
        int[] ry = new int[totalRing];
        boolean[] done = new boolean[totalRing];
        boolean[] present = new boolean[totalRing];

        for (Map.Entry<AdvancementHolder, AdvancementWidget> entry : widgets.entrySet()) {
            ResourceLocation id = entry.getKey().id();
            AdvancementWidget w = entry.getValue();
            int idx = -1;
            if (ArcanaItem.MASTERY_ADVANCEMENT_ID.equals(id)) {
                idx = types.length;
            } else {
                for (int i = 0; i < types.length; i++) {
                    if (types[i].getAdvancementId().equals(id)) {
                        idx = i;
                        break;
                    }
                }
            }
            if (idx >= 0) {
                rx[idx] = scrollX + w.getX() + SLOT_CENTER_OFFSET_X;
                ry[idx] = scrollY + w.getY() + SLOT_CENTER_OFFSET_Y;
                done[idx] = Boolean.TRUE.equals(unlockedMap.get(entry.getKey()));
                present[idx] = true;
            }
        }

        // 3. Constellation rays from center starting slot to each ring slot
        for (int i = 0; i < totalRing; i++) {
            if (!present[i]) continue;
            boolean isMastery = (i == types.length);
            if (isMastery) {
                // Special twin golden beam straight up to 12 o'clock Mastery node
                int beamCol = done[i] ? 0xCCFFE596 : 0x55B88E42;
                drawPixelLine(gui, centerX - 1, centerY - 14, rx[i] - 1, ry[i] + 14, beamCol);
                drawPixelLine(gui, centerX + 1, centerY - 14, rx[i] + 1, ry[i] + 14, beamCol);
            } else {
                int spokeCol = done[i] ? 0x667BE8F5 : 0x245C4285;
                drawPixelLine(gui, centerX, centerY, rx[i], ry[i], spokeCol);
            }
        }

        // 4. Clockwise perimeter links connecting consecutive ring slots (1 -> 2 -> ... -> 22 -> 12 o'clock)
        for (int i = 0; i < totalRing - 1; i++) {
            if (!present[i] || !present[i + 1]) continue;
            boolean lit = done[i] && done[i + 1];
            boolean partial = done[i] || done[i + 1];
            int linkCol = lit ? 0xBBFFE596 : (partial ? 0x668D6EC7 : 0x38433163);
            drawPixelLine(gui, rx[i], ry[i], rx[i + 1], ry[i + 1], linkCol);
        }
        // Subtle closing link from 12 o'clock (mastery) to The Fool (start of clockwise loop)
        if (present[totalRing - 1] && present[0]) {
            int closeCol = (done[totalRing - 1] && done[0]) ? 0x99FFE596 : 0x2B4D3B6E;
            drawPixelLine(gui, rx[totalRing - 1], ry[totalRing - 1], rx[0], ry[0], closeCol);
        }

        // 5. Soft glowing starlight halo behind unlocked slots and the 12 o'clock crown
        float pulse = 0.78F + 0.22F * (float) Math.sin(timeSec * 2.4D);
        for (int i = 0; i < totalRing; i++) {
            if (!present[i]) continue;
            boolean isMastery = (i == types.length);
            if (isMastery) {
                int haloA = Math.round((done[i] ? 0x70 : 0x36) * pulse);
                int col = (haloA << 24) | (done[i] ? 0xFFE596 : 0xB8863B);
                drawGlowSquare(gui, rx[i], ry[i], 17, col);
            } else if (done[i]) {
                int haloA = Math.round(0x4A * pulse);
                drawGlowSquare(gui, rx[i], ry[i], 16, (haloA << 24) | 0x5CE1F5);
            }
        }
    }

    /**
     * Renders a sleek obsidian-gold progress badge in the top-right corner of the viewport.
     */
    public static void renderProgressBadge(GuiGraphics gui, Font font, int unlocked, int total) {
        boolean complete = unlocked >= total;
        String label = unlocked + " / " + total;
        int textW = font.width(label);
        int bw = textW + 10;
        int bh = 13;
        int bx = VIEW_W - bw - 4;
        int by = 4;

        gui.fill(bx, by, bx + bw, by + bh, 0xC80A0816);
        int border = complete ? 0xFFE6BE5C : 0xFF6B4C9A;
        gui.hLine(bx, bx + bw - 1, by, border);
        gui.hLine(bx, bx + bw - 1, by + bh - 1, border);
        gui.vLine(bx, by, by + bh - 1, border);
        gui.vLine(bx + bw - 1, by, by + bh - 1, border);

        int textCol = complete ? 0xFFFFE596 : 0xFFD9C7F2;
        gui.drawString(font, label, bx + 5, by + 3, textCol, false);
    }

    private static void drawSoftNebula(GuiGraphics gui, int cx, int cy, int rx, int ry, int color) {
        int baseA = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        // Smooth elliptical scanline bands so nebula clouds never show sharp rectangular edges
        drawFilledEllipse(gui, cx, cy, rx, ry, ((baseA / 3) << 24) | rgb);
        drawFilledEllipse(gui, cx, cy, (rx * 2) / 3, (ry * 2) / 3, ((baseA / 2) << 24) | rgb);
        drawFilledEllipse(gui, cx, cy, rx / 3, ry / 3, (baseA << 24) | rgb);
    }

    private static void drawGlowSquare(GuiGraphics gui, int cx, int cy, int r, int color) {
        int a = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        drawFilledEllipse(gui, cx, cy, r + 2, r + 2, ((a / 3) << 24) | rgb);
        drawFilledEllipse(gui, cx, cy, r - 1, r - 1, ((a * 2 / 3) << 24) | rgb);
    }

    private static void drawFilledEllipse(GuiGraphics gui, int cx, int cy, int rx, int ry, int color) {
        if (rx <= 0 || ry <= 0 || ((color >>> 24) & 0xFF) == 0) {
            return;
        }
        int minY = Math.max(0, cy - ry);
        int maxY = Math.min(VIEW_H - 1, cy + ry);
        double invRySq = 1.0D / (ry * (double) ry);
        for (int y = minY; y <= maxY; y += 2) {
            double dy = y - cy;
            double term = 1.0D - (dy * dy) * invRySq;
            if (term <= 0.0D) continue;
            int halfW = (int) Math.round(rx * Math.sqrt(term));
            int x0 = Math.max(0, cx - halfW);
            int x1 = Math.min(VIEW_W, cx + halfW);
            if (x1 > x0) {
                gui.fill(x0, y, x1, Math.min(VIEW_H, y + 2), color);
            }
        }
    }

    private static void drawSolidRing(GuiGraphics gui, int cx, int cy, double radius, int steps, int color) {
        for (int i = 0; i < steps; i++) {
            double a = (Math.PI * 2.0D * i) / steps;
            int px = cx + (int) Math.round(Math.cos(a) * radius);
            int py = cy + (int) Math.round(Math.sin(a) * radius);
            if (px >= -2 && px <= VIEW_W + 2 && py >= -2 && py <= VIEW_H + 2) {
                gui.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    private static void drawDashedCircle(GuiGraphics gui, int cx, int cy, double radius, int dots, double phase, int color) {
        for (int i = 0; i < dots; i++) {
            double a = phase + (Math.PI * 2.0D * i) / dots;
            int px = cx + (int) Math.round(Math.cos(a) * radius);
            int py = cy + (int) Math.round(Math.sin(a) * radius);
            if (px >= -2 && px <= VIEW_W + 2 && py >= -2 && py <= VIEW_H + 2) {
                gui.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    private static void drawPixelLine(GuiGraphics gui, int x0, int y0, int x1, int y1, int color) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps <= 0) {
            return;
        }
        // Step by 2px for crisp constellation lines with low draw overhead
        int stride = 2;
        for (int i = 0; i <= steps; i += stride) {
            int px = x0 + (dx * i) / steps;
            int py = y0 + (dy * i) / steps;
            if (px >= -2 && px <= VIEW_W + 2 && py >= -2 && py <= VIEW_H + 2) {
                gui.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    private static float wrap(float val, float min, float max) {
        float span = max - min;
        float res = (val - min) % span;
        if (res < 0.0F) res += span;
        return min + res;
    }

    public static boolean isProgressDone(AdvancementProgress progress) {
        return progress != null && progress.isDone();
    }
}
