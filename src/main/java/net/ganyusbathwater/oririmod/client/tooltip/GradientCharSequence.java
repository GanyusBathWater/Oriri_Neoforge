package net.ganyusbathwater.oririmod.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

/**
 * Draws a {@link FormattedCharSequence} with a per-character flowing colour
 * gradient that sweeps horizontally across the full tooltip width over time.
 *
 * ── Design ───────────────────────────────────────────────────────────────────
 *
 *  Calamity-style text gradients cannot be done via the font atlas fragment
 *  shader without replacing it globally (which breaks everything else).
 *  Instead, we intercept the FormattedCharSink callback, override the Style
 *  colour for each character, and re-emit it into the same font batch.
 *
 *  The gradient position is computed as:
 *    t = (charIndex / totalChars) + (GameTime * speed)
 *
 *  where 'charIndex' is the ordinal position of the character in the sequence.
 *  This produces a left-to-right colour sweep that is screen-space-correct: a
 *  wider tooltip (more characters per line) has the same sweep duration because
 *  t is normalised by totalChars, not raw pixel width.
 *
 *  The colour palette is a cosine palette (IQ-style) that mirrors the GLSL
 *  palette() function in cosmic_tooltip_bg.fsh so text and background share
 *  the same colour language.
 *
 * ── Batch compatibility ──────────────────────────────────────────────────────
 *
 *  Each call to font.drawInBatch() below uses the SAME bufferSource, so all
 *  glyph quads land in the same font atlas batch. Sodium / ImmediatelyFast see
 *  a single contiguous vertex range and can merge it with the rest of the frame.
 */
public final class GradientCharSequence {

    private GradientCharSequence() {}

    private static final float TWO_PI = 6.28318f; // kept for potential future use

    /**
     * Draws {@code seq} with a flowing cosine gradient colour applied per character.
     *
     * @param font         Client font renderer
     * @param seq          The pre-wrapped line to draw
     * @param x            Left pixel of the line (screen-space)
     * @param y            Top pixel of the line (screen-space)
     * @param totalChars   Total character count across ALL lines — used to
     *                     normalise the gradient phase so the sweep speed is
     *                     constant regardless of line length or tooltip width.
     * @param charOffset   Index of the first character of this line within the
     *                     full multi-line block (accumulated across previous lines).
     * @param gameTime     Current game time in ticks (fractional)
     * @param styleIndex   Palette index matching {@code CosmicTooltipData.style}
     * @param shadow       Whether to render text shadow
     * @param matrix       Current pose matrix from the tooltip renderer
     * @param bufferSource Font batch buffer — must be the same source across all
     *                     lines to preserve batch contiguity
     * @return             The number of characters consumed (for charOffset tracking)
     */
    public static int draw(
            Font font,
            FormattedCharSequence seq,
            float x,
            float y,
            int totalChars,
            int charOffset,
            float timeSeconds,       // wall-clock seconds (NOT game ticks)
            int styleIndex,
            boolean shadow,
            Matrix4f matrix,
            MultiBufferSource bufferSource
    ) {
        float[] cursorX   = { x };
        int[]   charCount = { 0 };

        seq.accept((index, style, codePoint) -> {
            int globalIdx = charOffset + index;

            float normalised = totalChars > 1
                    ? (globalIdx / (float)(totalChars - 1)) * 1.5f
                    : 0.0f;

            // H = fract(normalised - timeSeconds * 0.3): sweeps full 0→1 hue range.
            // fract() wraps H so blue (0.67) and purple (0.75) are always reached.
            // Subtracting time makes the sweep travel left→right on screen.
            float h = fract(normalised - timeSeconds * 0.3f);
            int argb = hsvToArgb(h, 1.0f, 1.0f);

            // Override the character's style colour.  We preserve ALL other
            // style properties (bold, italic, underline, font) from the original.
            Style coloured = style.withColor(TextColor.fromRgb(argb & 0x00FFFFFF));

            // Wrap this single codepoint as a minimal FormattedCharSequence.
            // Using String.valueOf(char) is safe for BMP characters (all MC uses).
            FormattedCharSequence single = FormattedCharSequence.forward(
                    new String(Character.toChars(codePoint)), coloured);

            font.drawInBatch(
                    single,
                    cursorX[0],
                    y,
                    0xFFFFFFFF,   // color param is ignored when style has a TextColor
                    shadow,
                    matrix,
                    bufferSource,
                    Font.DisplayMode.NORMAL,
                    0,            // transparent background
                    15728880      // full-bright packed lightmap
            );

            cursorX[0] += font.width(single);
            charCount[0]++;
            return true;
        });

        return charCount[0];
    }

    // ── Full-spectrum HSV → RGB ───────────────────────────────────────────────
    // H sweeps [0, 1) = 0°→360°. At S=1, V=1 every hue is fully saturated.
    // fract() guarantees the hue wraps correctly — blue (H≈0.67) and
    // purple (H≈0.75) are always present in the sweep.
    private static int hsvToArgb(float h, float s, float v) {
        float hh = fract(h) * 6.0f;          // sector 0-5
        int   sector = (int) hh;
        float f  = hh - sector;              // fractional part within sector
        float p  = v * (1.0f - s);
        float q  = v * (1.0f - s * f);
        float t  = v * (1.0f - s * (1.0f - f));

        float r, g, b;
        switch (sector % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default-> { r = v; g = p; b = q; }
        }
        return (0xFF << 24) | (chan(r) << 16) | (chan(g) << 8) | chan(b);
    }

    /** fract() for float — equivalent to GLSL fract(). */
    private static float fract(float x) { return x - (float) Math.floor(x); }

    /** Clamp float [0,1] → int [0,255]. */
    private static int chan(float v) {
        return (int)(Math.max(0f, Math.min(1f, v)) * 255f);
    }
}
