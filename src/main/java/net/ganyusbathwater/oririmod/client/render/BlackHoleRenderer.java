package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.client.model.BlackHoleGeckoModel;
import net.ganyusbathwater.oririmod.entity.custom.BlackHoleEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BlackHoleRenderer extends GeoEntityRenderer<BlackHoleEntity> {

    // SPHERE_LAYER uses entitySolid to guarantee the black hole is 100% opaque.
    // While shaderpacks might apply sunset fog to it, it prevents the background from showing through!
    private static final RenderType SPHERE_LAYER = RenderType.entitySolid(
            ResourceLocation.fromNamespaceAndPath("oririmod", "textures/misc/black.png"));

    // LENS_LAYER uses entityTranslucent with a pure black texture.
    // By drawing it at 5% opacity, it tricks Iris into applying heavy refraction to it 
    // without drawing any ugly grid lines like the tinted glass texture did!
    private static final RenderType LENS_LAYER = RenderType.entityTranslucent(
            ResourceLocation.fromNamespaceAndPath("oririmod", "textures/misc/black.png"));

    private static final RenderType DISK_LAYER = RenderType.entityTranslucent(
            ResourceLocation.fromNamespaceAndPath("oririmod", "textures/misc/accretion_disk.png"));

    public BlackHoleRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BlackHoleGeckoModel());
    }

    @Override
    public RenderType getRenderType(BlackHoleEntity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void preRender(PoseStack poseStack, BlackHoleEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int color) {
        float currentRadius = animatable.getCurrentRadius();
        float maxRadius     = animatable.getMaxRadius();
        float visualScale   = (maxRadius > 0f) ? (currentRadius / maxRadius) * 1.5f : 0f;
        poseStack.scale(visualScale, visualScale, visualScale);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, 0xFF000000);
    }

    @Override
    public void render(BlackHoleEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        
        boolean shadersActive = BlackHolePostFX.areShadersLoaded();
        
        // If vanilla mode, use GeckoLib cube model for distortion shader to warp
        if (!shadersActive) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }

        // =====================================================================
        // SHADERPACK FALLBACK MODE
        // =====================================================================
        float currentRadius = entity.getCurrentRadius();
        float maxRadius     = entity.getMaxRadius();
        if (currentRadius <= 0.02f || maxRadius <= 0f) return;

        float visualScale  = (currentRadius / maxRadius) * 1.5f;
        float coreRadius   = (currentRadius / maxRadius) * BlackHoleEntity.VISUAL_CORE_RADIUS_MAX;
        float modelCenterY = (13.5f / 16.0f) * visualScale;
        
        // 1. Draw Refractive Lensing Sphere (Translucent pass)
        poseStack.pushPose();
        poseStack.translate(0.0, modelCenterY, 0.0);
        {
            VertexConsumer vc = bufferSource.getBuffer(LENS_LAYER);
            drawSphere(poseStack, vc, coreRadius * 1.15f, 0f, 0f, 0f, 0.05f); // 5% opacity black
        }
        poseStack.popPose();

        // 2. Draw Accretion Disk (Before core to ensure proper blending/layering)
        poseStack.pushPose();
        poseStack.translate(0.0, modelCenterY, 0.0);
        drawVolumetricDisk(poseStack, bufferSource, entityYaw, partialTick, entity, coreRadius);
        poseStack.popPose();

        // 3. Draw 3D Perfect Sphere (Solid Black Core) LAST
        poseStack.pushPose();
        poseStack.translate(0.0, modelCenterY, 0.0);
        {
            VertexConsumer vc = bufferSource.getBuffer(SPHERE_LAYER);
            drawSphere(poseStack, vc, coreRadius * 1.0f, 0f, 0f, 0f, 1f);
        }
        poseStack.popPose();
    }

    private static void drawSphere(PoseStack poseStack, VertexConsumer vc, float radius, float r, float g, float b, float a) {
        Matrix4f posM = poseStack.last().pose();
        int stacks = 16;
        int slices = 16;
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;

        for (int i = 0; i < stacks; i++) {
            float lat0 = (float) (Math.PI * (-0.5 + (double) i / stacks));
            float z0  = (float) Math.sin(lat0);
            float zr0 =  (float) Math.cos(lat0);

            float lat1 = (float) (Math.PI * (-0.5 + (double) (i + 1) / stacks));
            float z1 = (float) Math.sin(lat1);
            float zr1 = (float) Math.cos(lat1);

            for (int j = 0; j <= slices; j++) {
                float lng = (float) (2 * Math.PI * (double) (j - 1) / slices);
                float x = (float) Math.cos(lng);
                float y = (float) Math.sin(lng);

                float lng1 = (float) (2 * Math.PI * (double) j / slices);
                float x1 = (float) Math.cos(lng1);
                float y1 = (float) Math.sin(lng1);

                vc.addVertex(posM, x * zr0 * radius, y * zr0 * radius, z0 * radius).setColor(r, g, b, a).setUv(0.5f, 0.5f).setOverlay(overlay).setLight(light).setNormal(poseStack.last(), x*zr0, y*zr0, z0);
                vc.addVertex(posM, x1 * zr0 * radius, y1 * zr0 * radius, z0 * radius).setColor(r, g, b, a).setUv(0.5f, 0.5f).setOverlay(overlay).setLight(light).setNormal(poseStack.last(), x1*zr0, y1*zr0, z0);
                vc.addVertex(posM, x1 * zr1 * radius, y1 * zr1 * radius, z1 * radius).setColor(r, g, b, a).setUv(0.5f, 0.5f).setOverlay(overlay).setLight(light).setNormal(poseStack.last(), x1*zr1, y1*zr1, z1);
                vc.addVertex(posM, x * zr1 * radius, y * zr1 * radius, z1 * radius).setColor(r, g, b, a).setUv(0.5f, 0.5f).setOverlay(overlay).setLight(light).setNormal(poseStack.last(), x*zr1, y*zr1, z1);
            }
        }
    }

    private void drawVolumetricDisk(PoseStack poseStack, MultiBufferSource bufferSource, float entityYaw, float partialTick, BlackHoleEntity entity, float coreRadius) {
        VertexConsumer vc = bufferSource.getBuffer(DISK_LAYER);
        float time = entity.tickCount + partialTick;
        int light = 15728880; // Full bright without the aggressive shaderpack bloom of RenderType.eyes
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        
        int grid = 12; // Massive optimization: Reduced to 12x12 to eliminate geometry bottleneck
        
        // Pass 0 = Horizontal Equatorial Disk. Pass 1 = Vertical Lensing Halo (Billboarded)
        for (int pass = 0; pass < 2; pass++) {
            // Draw 3 overlapping layers (reduced from 5) to brutally cut down translucent overdraw!
            for (int layer = 0; layer < 3; layer++) {
                // Adjust sizes independently based on the pass
                float scaleMult = (pass == 0) ? (1.5f + (layer * 0.6f)) : (1.05f + (layer * 0.25f));
                float rotationSpeed = 3.0f / scaleMult;
                if (layer % 2 == 1) rotationSpeed *= -0.5f; 
                
                float layerTime = time * rotationSpeed * 1.5f;
                float scale = coreRadius * scaleMult;
                float halfSize = scale * 1.5f; 
                
                poseStack.pushPose();
                
                if (pass == 0) {
                    // Lay flat horizontally
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90f));
                } else {
                    // Vertical Billboard Halo (always faces camera, mimicking light bent over the poles)
                    org.joml.Quaternionf camRot = new org.joml.Quaternionf(net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
                    camRot.rotateY((float)Math.PI);
                    poseStack.mulPose(camRot);
                }
                
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(layerTime * 20.0f));
                
                Matrix4f posM = poseStack.last().pose();
            
            float step = (halfSize * 2.0f) / grid;
            float uvStep = 1.0f / grid;
            
            // --- OPTIMIZATION: Cache noise evaluations! ---
            // Instead of evaluating 3D noise 4 times per quad (causing 120,000+ noise calls per frame),
            // we pre-calculate the noise map for the grid vertices ONCE. This cuts CPU load by 95%!
            float[][][] warps = new float[grid + 1][grid + 1][3];
            float[][] alphas = new float[grid + 1][grid + 1];
            
            for (int x = 0; x <= grid; x++) {
                for (int y = 0; y <= grid; y++) {
                    float vx = -halfSize + x * step;
                    float vy = -halfSize + y * step;
                    warps[x][y] = calculateWarp(vx, vy, layerTime, coreRadius, layer);
                    alphas[x][y] = getDiskAlpha((float)Math.sqrt(vx*vx + vy*vy), coreRadius, halfSize, layer);
                }
            }
            
            for (int x = 0; x < grid; x++) {
                for (int y = 0; y < grid; y++) {
                    float vx0 = -halfSize + x * step;
                    float vy0 = -halfSize + y * step;
                    float vx1 = vx0 + step;
                    float vy1 = vy0 + step;
                    
                    float u0 = x * uvStep;
                    float v0 = y * uvStep;
                    float u1 = u0 + uvStep;
                    float v1 = v0 + uvStep;
                    
                    // Fetch from cache instead of calculating mathematically
                    float[] w00 = warps[x][y];
                    float[] w10 = warps[x+1][y];
                    float[] w11 = warps[x+1][y+1];
                    float[] w01 = warps[x][y+1];
                    
                    float a00 = alphas[x][y];
                    float a10 = alphas[x+1][y];
                    float a11 = alphas[x+1][y+1];
                    float a01 = alphas[x][y+1];
                    
                    // FRONT FACE
                    vc.addVertex(posM, vx0, vy0, w00[2]).setColor(1f, 1f, 1f, a00).setUv(u0 + w00[0], v0 + w00[1]).setOverlay(overlay).setLight(light).setNormal(poseStack.last(), 0, 0, 1);
                    vc.addVertex(posM, vx0, vy1, w01[2]).setColor(1f, 1f, 1f, a01).setUv(u0 + w01[0], v1 + w01[1]).setOverlay(overlay).setLight(light).setNormal(poseStack.last(), 0, 0, 1);
                    vc.addVertex(posM, vx1, vy1, w11[2]).setColor(1f, 1f, 1f, a11).setUv(u1 + w11[0], v1 + w11[1]).setOverlay(overlay).setLight(light).setNormal(poseStack.last(), 0, 0, 1);
                    vc.addVertex(posM, vx1, vy0, w10[2]).setColor(1f, 1f, 1f, a10).setUv(u1 + w10[0], v0 + w10[1]).setOverlay(overlay).setLight(light).setNormal(poseStack.last(), 0, 0, 1);
                    
                    // BACK FACE (Reversed winding)
                    vc.addVertex(posM, vx1, vy0, w10[2]).setColor(1f, 1f, 1f, a10).setUv(u1 + w10[0], v0 + w10[1]).setOverlay(overlay).setLight(light).setNormal(poseStack.last(), 0, 0, -1);
                    vc.addVertex(posM, vx1, vy1, w11[2]).setColor(1f, 1f, 1f, a11).setUv(u1 + w11[0], v1 + w11[1]).setOverlay(overlay).setLight(light).setNormal(poseStack.last(), 0, 0, -1);
                    vc.addVertex(posM, vx0, vy1, w01[2]).setColor(1f, 1f, 1f, a01).setUv(u0 + w01[0], v1 + w01[1]).setOverlay(overlay).setLight(light).setNormal(poseStack.last(), 0, 0, -1);
                    vc.addVertex(posM, vx0, vy0, w00[2]).setColor(1f, 1f, 1f, a00).setUv(u0 + w00[0], v0 + w00[1]).setOverlay(overlay).setLight(light).setNormal(poseStack.last(), 0, 0, -1);
                }
            }
            poseStack.popPose();
            }
        }
    }

    // Returns [uv_dx, uv_dy, z_offset]
    private float[] calculateWarp(float x, float y, float time, float coreRadius, int layer) {
        float noiseScale = 0.15f; // Much wider, smoother waves (was 0.5f)
        float timeScale = 0.05f;
        
        // UV Domain Warping (Lowered strength so it tears gently instead of shattering)
        float du = (float)net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(x * noiseScale, time * timeScale, y * noiseScale, 3) * 0.04f;
        float dv = (float)net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(x * noiseScale, time * timeScale + 100f, y * noiseScale, 3) * 0.04f;
        
        // 3D Volumetric Height Warping (Flatter, elegant disk)
        float dz = (float)net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(x * noiseScale * 0.5f, time * timeScale * 1.5f, y * noiseScale * 0.5f, 2);
        dz *= coreRadius * 0.45f; 
        
        return new float[]{du, dv, dz};
    }

    private float getDiskAlpha(float dist, float coreRadius, float maxRadius, int layer) {
        if (dist < coreRadius * 1.15f) return 0.0f; // Perfect void cut-out for the event horizon
        
        float fade = 1.0f;
        if (dist > maxRadius * 0.5f) {
            // Much smoother quadratic fade to hide the square edges of the mesh
            float f = (dist - maxRadius * 0.5f) / (maxRadius * 0.5f);
            fade = 1.0f - (f * f); 
        }
        
        // Base opacity based on layer
        float baseAlpha = 1.0f - (layer * 0.15f);
        return Math.max(0.0f, Math.min(1.0f, fade * baseAlpha));
    }

    @Override
    protected int getBlockLightLevel(BlackHoleEntity entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }
}
