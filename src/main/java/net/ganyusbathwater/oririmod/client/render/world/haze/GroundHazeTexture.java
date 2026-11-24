package net.ganyusbathwater.oririmod.client.render.world.haze;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Procedurally generates a seamlessly tileable 256x256 3-channel periodic FBM cloud noise texture
 * for the volumetric {@code oririmod:ground_haze} fragment shader:
 * <ul>
 *   <li><b>R channel:</b> Low-frequency billowing fog banks (periods 3, 6, 12)</li>
 *   <li><b>G channel:</b> Mid-frequency swirling atmospheric currents (periods 5, 10, 20)</li>
 *   <li><b>B channel:</b> Fine organic vapor wisps (periods 7, 14, 28)</li>
 * </ul>
 * Because every octave wraps seamlessly across tile boundaries with {@code GL_REPEAT} and bilinear
 * filtering, sampling with domain warping produces completely continuous mist with zero seams,
 * zero circles, and zero grid artifacts at any altitude.
 */
public final class GroundHazeTexture {

    private static final int SIZE = 256;
    private static final ResourceLocation TEXTURE_ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "dynamic/ground_haze_noise");

    private static boolean initialized = false;
    private static DynamicTexture dynamicTexture;

    private GroundHazeTexture() {
    }

    public static ResourceLocation getTexture() {
        if (!initialized) {
            initialized = true;
            NativeImage image = buildTileableNoise();
            dynamicTexture = new DynamicTexture(image);
            dynamicTexture.setFilter(true, false);
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, dynamicTexture);
            GlStateManager._bindTexture(dynamicTexture.getId());
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            applyRepeatWrap();
        }
        return TEXTURE_ID;
    }

    public static void ensureRepeatWrap() {
        if (dynamicTexture != null) {
            applyRepeatWrap();
        }
    }

    private static void applyRepeatWrap() {
        GlStateManager._bindTexture(dynamicTexture.getId());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    private static NativeImage buildTileableNoise() {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, SIZE, SIZE, false);
        for (int y = 0; y < SIZE; y++) {
            double v = (double) y / SIZE;
            for (int x = 0; x < SIZE; x++) {
                double u = (double) x / SIZE;

                // R: billowing macro cloud banks
                double rVal = periodicFbm(u, v, 3, 137) * 0.56
                        + periodicFbm(u, v, 6, 311) * 0.29
                        + periodicFbm(u, v, 12, 577) * 0.15;

                // G: swirling mid-scale tendrils (independent seed & coprime base period)
                double gVal = periodicFbm(u, v, 5, 919) * 0.54
                        + periodicFbm(u, v, 10, 1237) * 0.30
                        + periodicFbm(u, v, 20, 1601) * 0.16;

                // B: fine organic vapor texture
                double bVal = periodicFbm(u, v, 7, 2083) * 0.52
                        + periodicFbm(u, v, 14, 2441) * 0.31
                        + periodicFbm(u, v, 28, 2909) * 0.17;

                // Gently stretch contrast around 0.5 for rich atmospheric cloud variation
                int rByte = toByte(0.5 + (rVal - 0.5) * 1.45);
                int gByte = toByte(0.5 + (gVal - 0.5) * 1.45);
                int bByte = toByte(0.5 + (bVal - 0.5) * 1.40);

                // NativeImage ABGR packed int: (A << 24) | (B << 16) | (G << 8) | R
                int abgr = (0xFF << 24) | (bByte << 16) | (gByte << 8) | rByte;
                img.setPixelRGBA(x, y, abgr);
            }
        }
        return img;
    }

    private static int toByte(double v) {
        return Mth.clamp((int) Math.round(v * 255.0), 0, 255);
    }

    private static double periodicFbm(double u, double v, int period, int seed) {
        double fx = u * period;
        double fy = v * period;
        int x0 = (int) Math.floor(fx);
        int y0 = (int) Math.floor(fy);
        double tx = fx - x0;
        double ty = fy - y0;
        // Quintic C2 smoothstep prevents grid derivative creases under bilinear zoom
        double sx = tx * tx * tx * (tx * (tx * 6.0 - 15.0) + 10.0);
        double sy = ty * ty * ty * (ty * (ty * 6.0 - 15.0) + 10.0);

        int ix0 = Math.floorMod(x0, period);
        int ix1 = Math.floorMod(x0 + 1, period);
        int iy0 = Math.floorMod(y0, period);
        int iy1 = Math.floorMod(y0 + 1, period);

        double c00 = hash01(ix0, iy0, seed);
        double c10 = hash01(ix1, iy0, seed);
        double c01 = hash01(ix0, iy1, seed);
        double c11 = hash01(ix1, iy1, seed);

        double a = c00 + (c10 - c00) * sx;
        double b = c01 + (c11 - c01) * sx;
        return a + (b - a) * sy;
    }

    private static double hash01(int x, int y, int seed) {
        long h = x * 374761393L + y * 668265263L + seed * 1442695041L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= (h >>> 16);
        return (double) (h & 0xFFFFL) / 65535.0;
    }
}
