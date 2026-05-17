package net.ganyusbathwater.oririmod.client.tooltip;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.item.component.CosmicTooltipData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Renders ONLY the item name row:
 * renderImage() — animated mist/nebula behind the name
 * renderText() — flowing gradient on the name characters
 *
 * Description lines are handled by vanilla TextComponents; they are never
 * touched by this component. This ensures:
 * - The nebula covers ONLY the name area
 * - Text is always legible (vanilla descriptions are plain white, unaffected)
 * - renderImage() always runs before renderText() per vanilla pipeline, so
 * the nebula is always visually behind the gradient name text
 */
public class CosmicTooltipComponent implements ClientTooltipComponent {

    private static final int LINE_HEIGHT = 9;
    private static final int LINE_MARGIN = 2;
    private static final int BORDER_LEFT = 4;
    private static final int BORDER_RIGHT = 4;
    private static final int BORDER_TOP = 2;
    private static final int BORDER_BOT = 2;

    private final CosmicTooltipData data;
    private final List<FormattedCharSequence> lines; // name lines only (usually 1)
    private final int totalChars;

    public CosmicTooltipComponent(CosmicTooltipData data, List<FormattedCharSequence> lines) {
        this.data = data;
        this.lines = lines;

        int count = 0;
        if (!lines.isEmpty()) {
            int[] n = { 0 };
            lines.get(0).accept((i, s, cp) -> {
                n[0]++;
                return true;
            });
            count = n[0];
        }
        this.totalChars = Math.max(count, 1);
    }

    // ─── Layout ──────────────────────────────────────────────────────────────
    // getHeight() matches the height of a vanilla first-line TextComponent (9px).
    // The tooltip renderer adds 2px gap after position-0 automatically.
    @Override
    public int getHeight() {
        int n = Math.max(lines.size(), 1);
        return BORDER_TOP + n * LINE_HEIGHT + (n - 1) * LINE_MARGIN + BORDER_BOT;
    }

    @Override
    public int getWidth(Font font) {
        int max = 0;
        for (FormattedCharSequence s : lines)
            max = Math.max(max, font.width(s));
        return max + BORDER_LEFT + BORDER_RIGHT;
    }


    // ─── Text ────────────────────────────────────────────────────────────────
    @Override
    public void renderText(Font font, int x, int y, Matrix4f matrix,
            MultiBufferSource.BufferSource bufferSource) {
        // Wall-clock seconds, wraps every 60 s — small values, no float precision loss.
        float timeSeconds = (float) (System.currentTimeMillis() % 60_000L) / 1000.0f;

        int drawX = x + BORDER_LEFT;
        int drawY = y + BORDER_TOP;

        for (int i = 0; i < lines.size(); i++) {
            int lineY = drawY + i * (LINE_HEIGHT + LINE_MARGIN);
            // Effectively-final capture required by lambdas below.
            FormattedCharSequence line = lines.get(i);

            if (data.animated()) {
                // ── 1. Black outline pass ──────────────────────────────────
                int outline = 0xFF000000;
                // 8-directional outline (cardinal + diagonal) gives a thicker
                // black border that remains legible over any mist colour/brightness.
                int[][] offsets = { {-1,0},{1,0},{0,-1},{0,1},{-1,-1},{1,-1},{-1,1},{1,1} };
                for (int[] off : offsets) {
                    // Strip any baked TextColor so drawInBatch uses 0xFF000000.
                    FormattedCharSequence stripped = sink -> line.accept(
                            (idx, style, cp) -> sink.accept(idx,
                                    style.withColor((net.minecraft.network.chat.TextColor) null), cp));
                    font.drawInBatch(stripped,
                            drawX + off[0], lineY + off[1],
                            outline, false,
                            matrix, bufferSource,
                            Font.DisplayMode.NORMAL, 0, 15728880);
                }

                // ── 2. Gradient pass ──────────────────────────────────────
                GradientCharSequence.draw(
                        font, line,
                        drawX, lineY,
                        totalChars, 0,
                        timeSeconds,
                        data.style(),
                        false,
                        matrix, bufferSource);
            } else {
                font.drawInBatch(line, drawX, lineY, 0xFFFFFFFF, true,
                        matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
            }
        }
    }
}
