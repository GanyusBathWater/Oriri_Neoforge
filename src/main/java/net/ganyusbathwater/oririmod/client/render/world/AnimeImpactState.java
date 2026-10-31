package net.ganyusbathwater.oririmod.client.render.world;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

public class AnimeImpactState {

    public static final ResourceLocation ANIME_IMPACT_SHADER_LOC =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "anime_impact");

    private static long impactStartTime = 0;
    private static long impactEndTime = 0;

    @Nullable
    private static ShaderInstance animeImpactShader = null;

    private static int screenCopyTexId = -1;
    private static int cachedTexWidth = -1;
    private static int cachedTexHeight = -1;

    /**
     * Called from {@code OririClient.onRegisterShaders} when resources load or reload.
     */
    public static void onShadersRegistered(ShaderInstance shader) {
        animeImpactShader = shader;
        OririMod.LOGGER.info("[OririMod] anime_impact shader registered successfully.");
    }

    /**
     * Triggers the high-contrast pure black-and-white anime impact flash.
     *
     * @param durationMs The duration in milliseconds.
     */
    public static void triggerImpact(long durationMs) {
        long now = System.currentTimeMillis();
        impactStartTime = now;
        impactEndTime = now + Math.max(50L, durationMs);
    }

    /**
     * Checks if the impact flash is currently active.
     */
    public static boolean isActive() {
        return System.currentTimeMillis() < impactEndTime;
    }

    /**
     * Returns normalized progress [0.0 .. 1.0] through the current impact flash.
     */
    public static float getProgress() {
        long now = System.currentTimeMillis();
        long total = Math.max(1L, impactEndTime - impactStartTime);
        return Math.max(0.0f, Math.min(1.0f, (float) (now - impactStartTime) / (float) total));
    }

    private static void ensureScreenCopyTexture(int fbWidth, int fbHeight) {
        if (screenCopyTexId == -1) {
            screenCopyTexId = TextureUtil.generateTextureId();
        }
        if (cachedTexWidth != fbWidth || cachedTexHeight != fbHeight) {
            cachedTexWidth = fbWidth;
            cachedTexHeight = fbHeight;
            GlStateManager._bindTexture(screenCopyTexId);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    fbWidth,
                    fbHeight,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    (java.nio.ByteBuffer) null
            );
        }
    }

    /**
     * Renders the pure black-and-white high-contrast anime impact overlay during {@code RenderGuiEvent.Post}.
     * Because this executes in the GUI stage after world rendering completes, it is 100% compatible with Iris/Oculus shaders.
     */
    public static void renderOverlay(GuiGraphics guiGraphics) {
        if (!isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        int fbW = mc.getMainRenderTarget().width;
        int fbH = mc.getMainRenderTarget().height;

        if (fbW <= 0 || fbH <= 0) return;

        float progress = getProgress();
        long elapsedMs = System.currentTimeMillis() - impactStartTime;

        // Multi-stage anime impact cadence:
        //  - First 45ms: blinding positive B&W flash (ImpactPhase = 0.0)
        //  - Main body: stark inverted pure B&W impact frame (ImpactPhase = 1.0)
        //  - Final 12%: brief positive flicker & smooth ramp out
        float phase = (elapsedMs < 45L || (progress > 0.82f && progress < 0.90f)) ? 0.0f : 1.0f;
        float intensity = 1.0f;
        if (progress > 0.88f) {
            intensity = Math.max(0.0f, (1.0f - progress) / 0.12f);
        }

        ShaderInstance shader = animeImpactShader;
        if (shader != null) {
            ensureScreenCopyTexture(fbW, fbH);

            // Copy current rendered screen (including any active Iris shader output) into our texture
            GlStateManager._bindTexture(screenCopyTexId);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, fbW, fbH);

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            shader.setSampler("Sampler0", screenCopyTexId);
            RenderSystem.setShaderTexture(0, screenCopyTexId);

            var uScreenSize = shader.safeGetUniform("ScreenSize");
            if (uScreenSize != null) {
                uScreenSize.set((float) fbW, (float) fbH);
            }
            var uPhase = shader.safeGetUniform("ImpactPhase");
            if (uPhase != null) {
                uPhase.set(phase);
            }
            var uIntensity = shader.safeGetUniform("ImpactIntensity");
            if (uIntensity != null) {
                uIntensity.set(intensity);
            }
            var uTime = shader.safeGetUniform("ImpactTime");
            if (uTime != null) {
                float wallSec = (float) (System.currentTimeMillis() % 1_000_000L) / 1000.0f;
                uTime.set(wallSec);
            }

            RenderSystem.setShader(() -> shader);

            Matrix4f matrix = guiGraphics.pose().last().pose();
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            // OpenGL framebuffer texture has V=0 at bottom and V=1 at top
            builder.addVertex(matrix, 0, guiH, 0).setUv(0.0f, 0.0f).setColor(255, 255, 255, 255);
            builder.addVertex(matrix, guiW, guiH, 0).setUv(1.0f, 0.0f).setColor(255, 255, 255, 255);
            builder.addVertex(matrix, guiW, 0, 0).setUv(1.0f, 1.0f).setColor(255, 255, 255, 255);
            builder.addVertex(matrix, 0, 0, 0).setUv(0.0f, 1.0f).setColor(255, 255, 255, 255);

            BufferUploader.drawWithShader(builder.buildOrThrow());

            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            return;
        }

        // Fallback if shader has not finished compiling yet
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        GL14.glBlendEquation(GL14.GL_FUNC_SUBTRACT);
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE
        );

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR
        );

        Matrix4f matrix = guiGraphics.pose().last().pose();
        bufferbuilder.addVertex(matrix, 0, guiH, 0).setColor(255, 255, 255, 255);
        bufferbuilder.addVertex(matrix, guiW, guiH, 0).setColor(255, 255, 255, 255);
        bufferbuilder.addVertex(matrix, guiW, 0, 0).setColor(255, 255, 255, 255);
        bufferbuilder.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 255);

        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());

        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
    }
}
