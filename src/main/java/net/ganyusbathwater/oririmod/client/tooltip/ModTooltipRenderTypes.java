package net.ganyusbathwater.oririmod.client.tooltip;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the custom {@link RenderType} used to draw the cosmic nebula background
 * and the {@link ShaderInstance} reference populated by {@link #onShadersRegistered}.
 *
 * ── GPU State ────────────────────────────────────────────────────────────────
 *
 *  The RenderType is configured with:
 *    • Vertex format  : POSITION_COLOR  — matches the .vsh attribute declarations
 *    • Transparency   : TRANSLUCENT     — standard alpha blending (srcA / 1-srcA)
 *    • Depth test     : NO_DEPTH_TEST   — GUI always renders on top; skip z-test
 *    • Write mask     : COLOR_WRITE     — do not update the depth buffer
 *    • needsSorting   : false           — our quad is always on top, no sorting needed
 *
 * ── Uniform access ───────────────────────────────────────────────────────────
 *
 *  Minecraft auto-binds GameTime and ScreenSize every frame before any draw
 *  call.  The three custom uniforms (TooltipOrigin, TooltipSize, StyleIndex)
 *  are set manually in CosmicTooltipComponent.renderImage() via
 *  {@link #setTooltipUniforms} immediately before the vertex flush.
 *
 *  The shader reference is exposed as a package-private field so
 *  CosmicTooltipComponent can access it without an extra indirection.
 */
public final class ModTooltipRenderTypes {

    private ModTooltipRenderTypes() {}

    // ── Public constants ─────────────────────────────────────────────────────

    public static final ResourceLocation COSMIC_BG_SHADER_LOC =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "cosmic_tooltip_bg");

    // ── Shader reference ─────────────────────────────────────────────────────
    // Populated by onShadersRegistered() during resource load.
    // null until the first successful resource pack load.
    @Nullable
    static ShaderInstance cosmicBgShader = null;

    // ── RenderType ───────────────────────────────────────────────────────────
    // Built lazily the first time cosmicBackground() is called after the shader
    // has been loaded. Rebuilt on resource reload by onShadersRegistered().
    @Nullable
    private static RenderType cosmicBgRenderType = null;

    /**
     * Returns the live cosmic background {@link RenderType}, or falls back to
     * {@code RenderType.gui()} if the shader has not been loaded yet.
     *
     * The fallback guarantees the tooltip renders correctly on the first frame
     * before the shader pipeline is ready (e.g., during world load transitions).
     */
    public static RenderType cosmicBackground() {
        if (cosmicBgRenderType != null) return cosmicBgRenderType;
        return RenderType.gui();   // safe fallback
    }

    /**
     * Called from {@code OririClient.onRegisterShaders()} after NeoForge has
     * compiled the shader program.  Stores the {@link ShaderInstance} and
     * constructs the {@link RenderType} that binds it.
     *
     * This method is called every time resources are reloaded (F3+T), so the
     * RenderType is always rebuilt against the freshly compiled shader.
     *
     * @param shader The compiled ShaderInstance for cosmic_tooltip_bg
     */
    public static void onShadersRegistered(ShaderInstance shader) {
        cosmicBgShader = shader;

        // Build the CompositeRenderType.
        // The lambda () -> cosmicBgShader captures the field reference, so if
        // the shader is reloaded and the field updated, the RenderType always
        // references the current shader without needing to be rebuilt itself.
        cosmicBgRenderType = RenderType.create(
                "oririmod:cosmic_tooltip_bg",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                256,       // initial buffer size (vertices); expands automatically
                false,     // affectsCrumbling: irrelevant for GUI
                false,     // sortOnUpload: our quads are axis-aligned, no sort needed
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> cosmicBgShader))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false)
        );

        OririMod.LOGGER.info("[OririMod] cosmic_tooltip_bg shader registered successfully.");
    }

    /**
     * Sets the three custom uniforms that Minecraft does NOT auto-bind.
     * Must be called BEFORE {@code graphics.flush()} so the values are in the
     * shader when the draw call is issued.
     *
     * @param tooltipX  Left edge of the tooltip in GUI-space pixels
     * @param tooltipY  Top edge of the tooltip in GUI-space pixels
     * @param width     Tooltip width in GUI-space pixels
     * @param height    Tooltip height in GUI-space pixels
     * @param style     Style index (float cast of CosmicTooltipData.style)
     */
    public static void setTooltipUniforms(float tooltipX, float tooltipY,
                                          float width, float height,
                                          float style) {
        if (cosmicBgShader == null) return;

        var uOrigin = cosmicBgShader.safeGetUniform("TooltipOrigin");
        if (uOrigin != null) uOrigin.set(tooltipX, tooltipY);

        var uSize = cosmicBgShader.safeGetUniform("TooltipSize");
        if (uSize != null) uSize.set(width, height);

        var uStyle = cosmicBgShader.safeGetUniform("StyleIndex");
        if (uStyle != null) uStyle.set(style);
    }

    public static void pushCosmicTimeUniform() {
        if (cosmicBgShader != null) {
            var uTime = cosmicBgShader.safeGetUniform("CosmicTime");
            if (uTime != null) {
                float wallSeconds = (float)(System.currentTimeMillis() % 10_000_000L) / 1000.0f;
                uTime.set(wallSeconds);
            }
        }
    }
}
