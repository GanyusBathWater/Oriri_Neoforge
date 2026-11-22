package net.ganyusbathwater.oririmod.client.render.world.haze;

import com.mojang.blaze3d.platform.NativeImage;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Procedurally generates a 512x512 (2x2 atlas of four 256x256) soft volumetric mist-puff sprites.
 * Each sprite uses pure white RGB with a smooth radial-Gaussian x multi-octave FBM cloud alpha
 * that falls strictly to 0 well before the tile edge, guaranteeing zero quad edges, zero atlas
 * bleeding, and full compatibility across Vanilla, Sodium, and all Iris shaderpacks.
 */
public final class GroundHazeTexture {

    private static final int TILE_SIZE = 256;
    private static final int ATLAS_TILES = 2;
    private static final int ATLAS_SIZE = TILE_SIZE * ATLAS_TILES;
    private static final ResourceLocation TEXTURE_ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "dynamic/ground_haze_puffs");

    private static boolean initialized = false;
    private static DynamicTexture dynamicTexture;

    private GroundHazeTexture() {
    }

    public static ResourceLocation getTexture() {
        if (!initialized) {
            initialized = true;
            NativeImage image = buildMistPuffAtlas();
            dynamicTexture = new DynamicTexture(image);
            // Bilinear smoothing ON, mipmaps OFF so atlas tile edges never bleed into neighbors
            dynamicTexture.setFilter(true, false);
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, dynamicTexture);
        }
        return TEXTURE_ID;
    }

    private static NativeImage buildMistPuffAtlas() {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, ATLAS_SIZE, ATLAS_SIZE, false);
        for (int ty = 0; ty < ATLAS_TILES; ty++) {
            for (int tx = 0; tx < ATLAS_TILES; tx++) {
                int tileIndex = ty * ATLAS_TILES + tx;
                fillMistPuffTile(img, tx * TILE_SIZE, ty * TILE_SIZE, 137 + tileIndex * 313);
            }
        }
        return img;
    }

    private static void fillMistPuffTile(NativeImage img, int baseX, int baseY, int seed) {
        for (int ly = 0; ly < TILE_SIZE; ly++) {
            double ny = (ly + 0.5) / (TILE_SIZE * 0.5) - 1.0; // -1 .. +1
            double v = (double) ly / TILE_SIZE;
            for (int lx = 0; lx < TILE_SIZE; lx++) {
                double nx = (lx + 0.5) / (TILE_SIZE * 0.5) - 1.0; // -1 .. +1
                double u = (double) lx / TILE_SIZE;

                double r = Math.sqrt(nx * nx + ny * ny);
                if (r >= 0.97) {
                    // Strictly transparent outer margin prevents any square edges or tile bleed
                    img.setPixelRGBA(baseX + lx, baseY + ly, 0x00FFFFFF);
                    continue;
                }

                // Organic multi-octave FBM noise that BOTH warps the outer silhouette (no circular dots!)
                // and modulates interior density softly so overlapping puffs fuse into continuous mist
                double n1 = smoothNoise(u * 3.2 + 1.3, v * 3.2 + 2.7, seed);
                double n2 = smoothNoise(u * 6.4 + 4.1, v * 6.4 + 0.9, seed + 53);
                double n3 = smoothNoise(u * 12.0 + 2.2, v * 12.0 + 6.4, seed + 127);
                double fbm = n1 * 0.56 + n2 * 0.30 + n3 * 0.14;

                double warp = smoothNoise(u * 4.0 + (n1 - 0.5) * 0.9, v * 4.0 + (n2 - 0.5) * 0.9, seed + 211);
                double wisp = fbm * 0.62 + warp * 0.38;

                // Perturb radial boundary organically (damped to 0 near r = 0.96 so edges stay strictly 0)
                double warpWeight = Mth.clamp((0.96 - r) / 0.25, 0.0, 1.0);
                double rWarped = r + (wisp - 0.5) * 0.30 * warpWeight;

                // Broad, flat-topped cloud plateau (no bright circular bullseye center!)
                // Stays broadly even across r = 0..0.38, then feathers smoothly to 0 by r = 0.95
                double plateauT = Mth.clamp((0.95 - rWarped) / 0.62, 0.0, 1.0);
                double plateau = plateauT * plateauT * (3.0 - 2.0 * plateauT);

                // Clamp strictly by hard outer safety ring so no pixel ever touches the tile border
                double safetyT = Mth.clamp((0.97 - r) / 0.14, 0.0, 1.0);
                double cloud = 0.68 + 0.32 * wisp;

                double alphaF = Mth.clamp(plateau * safetyT * cloud, 0.0, 1.0);
                int alpha = Mth.clamp((int) Math.round(alphaF * 255.0), 0, 255);

                // NativeImage ABGR layout: alpha in top byte, pure white RGB so vertex tint & lightmap drive color
                int abgr = (alpha << 24) | 0x00FFFFFF;
                img.setPixelRGBA(baseX + lx, baseY + ly, abgr);
            }
        }
    }

    private static double smoothNoise(double x, double y, int seed) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double tx = x - x0;
        double ty = y - y0;
        double sx = tx * tx * (3.0 - 2.0 * tx);
        double sy = ty * ty * (3.0 - 2.0 * ty);

        double c00 = hash01(x0, y0, seed);
        double c10 = hash01(x0 + 1, y0, seed);
        double c01 = hash01(x0, y0 + 1, seed);
        double c11 = hash01(x0 + 1, y0 + 1, seed);

        double ix0 = c00 + (c10 - c00) * sx;
        double ix1 = c01 + (c11 - c01) * sx;
        return ix0 + (ix1 - ix0) * sy;
    }

    private static double hash01(int x, int y, int seed) {
        long h = x * 374761393L + y * 668265263L + seed * 1442695041L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= (h >>> 16);
        return (double) (h & 0xFFFFL) / 65535.0;
    }
}
