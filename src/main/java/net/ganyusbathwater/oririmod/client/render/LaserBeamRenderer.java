package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.entity.LaserBeamEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/**
 * LaserBeamRenderer — renders a 3-layer billboard laser beam.
 *
 * <h2>Visual Layers (back-to-front in same draw call)</h2>
 * <ol>
 *   <li><b>Outer glow</b>  — 3× beam-width, ~12% alpha, soft bloom silhouette.</li>
 *   <li><b>Mid glow</b>    — 2× beam-width, ~30% alpha, fills in the halo.</li>
 *   <li><b>Core beam</b>   — 1× beam-width, full alpha, beam color.</li>
 *   <li><b>Hot core</b>    — 0.25× beam-width, full white, the energy center.
 *                            Flickers with a sine wave to feel alive.</li>
 * </ol>
 *
 * <h2>Performance</h2>
 * All layers share a single {@link RenderType} buffer (translucent-no-texture).
 * No textures are sampled, no heap objects are created per frame.
 * The frustum culling in {@link #shouldRender} is inherited from {@link EntityRenderer}
 * and uses the entity's bounding box, so off-screen beams are skipped cheaply.
 */
public class LaserBeamRenderer extends EntityRenderer<LaserBeamEntity> {

    /**
     * We use the translucent render type with the "lines" texture slot left as a
     * solid white 1×1 built-in texture so no PNG file is required.
     * RenderType.debugFilledBox() is opaque.  We use entityTranslucentCull() with
     * a 1×1 white texture (same ResourceLocation trick as EndRodRenderer).
     */
    private static final RenderType BEAM_LAYER = RenderType.entityTranslucent(
            ResourceLocation.withDefaultNamespace("textures/misc/white.png"));

    private static final RenderType CIRCLE_LAYER = RenderType.entityTranslucent(
            ResourceLocation.parse("oririmod:textures/effect/magic_circles/beam_circle.png"));

    private static final RenderType WAVE_CIRCLE_LAYER = RenderType.entityTranslucent(
            ResourceLocation.parse("oririmod:textures/effect/magic_circles/wave_circle.png"));

    public LaserBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(LaserBeamEntity entity) {
        // Required override; the actual texture is set per-layer below.
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Main render entry-point
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public boolean shouldRender(LaserBeamEntity entity, net.minecraft.client.renderer.culling.Frustum camera, double camX, double camY, double camZ) {
        // The beam visually escapes its mathematical bounding box, so we override 
        // frustum culling entirely to prevent it from vanishing when the camera 
        // isn't looking directly at the logical midpoint.
        return true;
    }

    @Override
    public void render(LaserBeamEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        float totalAge = entity.getAgeTicks() + partialTicks;
        float duration = entity.getDurationTicks();

        // ── Hot-core flicker (sine) ──────────────────────────────────────────
        float flicker = 1.0f + 0.08f * (float) Math.sin(totalAge * Math.PI * 0.35);

        // ── Decode ARGB color ────────────────────────────────────────────────
        int argb = entity.getBeamColor();
        float coreR = ((argb >> 16) & 0xFF) / 255.0f;
        float coreG = ((argb >>  8) & 0xFF) / 255.0f;
        float coreB = ( argb        & 0xFF) / 255.0f;

        // ── World-space beam offsets (LERPED FOR SMOOTHNESS!) ────────────────
        Vec3 start = entity.getBeamStart();
        Vec3 end   = entity.getBeamEnd();
        
        double lerpX = net.minecraft.util.Mth.lerp((double)partialTicks, entity.xo, entity.getX());
        double lerpY = net.minecraft.util.Mth.lerp((double)partialTicks, entity.yo, entity.getY());
        double lerpZ = net.minecraft.util.Mth.lerp((double)partialTicks, entity.zo, entity.getZ());
        
        float sx = (float)(start.x - lerpX);
        float sy = (float)(start.y - lerpY);
        float sz = (float)(start.z - lerpZ);
        float ex = (float)(end.x   - lerpX);
        float ey = (float)(end.y   - lerpY);
        float ez = (float)(end.z   - lerpZ);

        // ── Compute direction for circle alignment ───────────────────────────
        float dx = ex - sx;
        float dy = ey - sy;
        float dz = ez - sz;
        float beamLen = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (beamLen > 1e-4f) {
            dx /= beamLen; dy /= beamLen; dz /= beamLen;
        } else {
            dy = 1f; dx = 0f; dz = 0f;
        }
        float xzLen = (float) Math.sqrt(dx*dx + dz*dz);
        float circleYaw = (float) Math.atan2(dx, dz);
        float circlePitch = (float) Math.atan2(-dy, xzLen);

        // ── Render Magic Circle if it has a charge OR is a standalone visual anchor ────
        int charge = entity.getChargeTicks();
        if (charge > 0 || entity.usesWaveCircle()) {
            float circleAge = totalAge;
            float circleAlpha = 1.0f;
            float circleScale = 1.0f;
            float rotAngle = 0.0f;

            if (circleAge < 20.0f) {
                circleAlpha = circleAge / 20.0f;
                circleScale = 0.5f + 0.5f * (circleAge / 20.0f);
            } 
            
            // Branch: Support Zero-Charge Instant Spawns (like our Wave Anchor!)
            if (charge == 0) {
                // Steadiest sweeping continuous spin for instant-cast magic circles
                rotAngle = circleAge * 15.0f * ((float)Math.PI / 180f);
                
                if (circleAge > duration - 20.0f) {
                    float remaining = duration - circleAge;
                    circleAlpha = Math.max(0.0f, Math.min(1.0f, remaining / 20.0f));
                }
            } else {
                if (circleAge < charge) {
                    float spinTicks = circleAge - 20.0f;
                    rotAngle = (spinTicks * spinTicks) * ((float)Math.PI / 180f); 
                } else {
                    float spinTicks = charge - 20.0f;
                    float accumulated = (spinTicks * spinTicks) * ((float)Math.PI / 180f);
                    rotAngle = accumulated + (circleAge - charge) * 20.0f * ((float)Math.PI / 180f);
                    
                    if (circleAge > charge + duration - 20.0f) {
                        float remaining = (charge + duration) - circleAge;
                        circleAlpha = Math.max(0.0f, Math.min(1.0f, remaining / 20.0f));
                    }
                }
            }

            if (circleAlpha > 0.01f) {
                RenderType activeCircleLayer = entity.usesWaveCircle() ? WAVE_CIRCLE_LAYER : CIRCLE_LAYER;
                VertexConsumer cvc = buffer.getBuffer(activeCircleLayer);
                poseStack.pushPose();
                poseStack.translate(sx, sy, sz);
                
                // Align to face precisely down the laser beam!
                poseStack.mulPose(com.mojang.math.Axis.YP.rotation(circleYaw));
                poseStack.mulPose(com.mojang.math.Axis.XP.rotation(circlePitch));
                // Rotate Z for the magical spin effect
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(rotAngle));
                
                Matrix4f cPosM = poseStack.last().pose();
                float hs = (entity.getBeamWidth() * 3.8f) * circleScale;
                int overlay = OverlayTexture.NO_OVERLAY;
                
                // Brighter circle blending logic for extra glowing contrast!
                float cR = Math.min(1.0f, coreR + 0.35f);
                float cG = Math.min(1.0f, coreG + 0.35f);
                float cB = Math.min(1.0f, coreB + 0.35f);
                
                cvc.addVertex(cPosM, -hs, -hs, 0).setColor(cR, cG, cB, circleAlpha).setUv(0, 0).setOverlay(overlay).setLight(packedLight).setNormal(poseStack.last(), 0, 0, 1);
                cvc.addVertex(cPosM, -hs,  hs, 0).setColor(cR, cG, cB, circleAlpha).setUv(0, 1).setOverlay(overlay).setLight(packedLight).setNormal(poseStack.last(), 0, 0, 1);
                cvc.addVertex(cPosM,  hs,  hs, 0).setColor(cR, cG, cB, circleAlpha).setUv(1, 1).setOverlay(overlay).setLight(packedLight).setNormal(poseStack.last(), 0, 0, 1);
                cvc.addVertex(cPosM,  hs, -hs, 0).setColor(cR, cG, cB, circleAlpha).setUv(1, 0).setOverlay(overlay).setLight(packedLight).setNormal(poseStack.last(), 0, 0, 1);
                
                poseStack.popPose();
            }
        }

        // If it's still charging, do not render the actual laser beam
        float localAge = totalAge - charge;
        if (localAge < 0.0f) return;

        // ── Compute scale multiplier based on local age / duration (1 second fade) ──
        float fadeTicks = 20.0f; // Exactly 1 second
        float alphaScale = 1.0f;
        if (localAge < fadeTicks) {
            alphaScale = localAge / fadeTicks;
        } else if (localAge > duration - fadeTicks) {
            alphaScale = (duration - localAge) / fadeTicks;
        }
        alphaScale = Math.max(0.0f, Math.min(1.0f, alphaScale));
        if (alphaScale < 0.01f) return;

        float halfW = (entity.getBeamWidth() * 0.5f) * alphaScale;


        // ── Billboard right vector ───────────────────────────────────────────
        // Compute beam direction, then cross with camera view to get the billboard axis.
        float dirX = ex - sx;
        float dirY = ey - sy;
        float dirZ = ez - sz;
        float dirLen = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (dirLen < 1e-4f) return; // degenerate beam

        dirX /= dirLen;
        dirY /= dirLen;
        dirZ /= dirLen;

        // Camera look vector (from geometric center of beam to camera)
        float midX = (start.x == 0 && end.x == 0) ? sx : (sx + ex) * 0.5f;
        float midY = (start.y == 0 && end.y == 0) ? sy : (sy + ey) * 0.5f;
        float midZ = (start.z == 0 && end.z == 0) ? sz : (sz + ez) * 0.5f;

        Vec3 camPos = this.entityRenderDispatcher.camera.getPosition();
        float camDX = (float)(camPos.x - (lerpX + midX));
        float camDY = (float)(camPos.y - (lerpY + midY));
        float camDZ = (float)(camPos.z - (lerpZ + midZ));
        
        float camLen = (float) Math.sqrt(camDX*camDX + camDY*camDY + camDZ*camDZ);
        if (camLen > 1e-4f) {
            camDX /= camLen;
            camDY /= camLen;
            camDZ /= camLen;
        } else {
            camDX = 0; camDY = 1; camDZ = 0;
        }

        // right = dir × cam  (billboard perpendicular to both beam and camera)
        float rX = dirY * camDZ - dirZ * camDY;
        float rY = dirZ * camDX - dirX * camDZ;
        float rZ = dirX * camDY - dirY * camDX;
        float rLen = (float) Math.sqrt(rX*rX + rY*rY + rZ*rZ);
        
        if (rLen < 1e-4f) {
            // Beam is pointing nearly perfectly parallel directly AT or AWAY FROM the camera axis!
            // We must dynamically compute an alternative perpendicular right vector.
            // By crossing `dir` with a standard cardinal static axis, we derive a perfect plane.
            float fallbackUpX = (Math.abs(dirY) > 0.8f) ? 1.0f : 0.0f; // If beam is heavily vertical, use X-axis
            float fallbackUpY = (Math.abs(dirY) > 0.8f) ? 0.0f : 1.0f; // Otherwise use standard Y-axis
            float fallbackUpZ = 0.0f;
            
            rX = dirY * fallbackUpZ - dirZ * fallbackUpY;
            rY = dirZ * fallbackUpX - dirX * fallbackUpZ;
            rZ = dirX * fallbackUpY - dirY * fallbackUpX;
            rLen = (float) Math.sqrt(rX*rX + rY*rY + rZ*rZ);
            
            // Absolute worst-case safety net
            if (rLen < 1e-4f) {
                rX = 1; rY = 0; rZ = 0; rLen = 1;
            }
        }
        rX /= rLen;  rY /= rLen;  rZ /= rLen;

        // ── Draw all layers via single buffer ────────────────────────────────
        if (!entity.isCoreHidden()) {
            poseStack.pushPose();

            PoseStack.Pose   pose   = poseStack.last();
            Matrix4f         posM   = pose.pose();
            VertexConsumer   vc     = buffer.getBuffer(BEAM_LAYER);

            // Layer 1 — Outer glow (3× width, 12% alpha)
            drawQuadLayer(vc, posM, pose,
                    sx, sy, sz, ex, ey, ez,
                    rX, rY, rZ, halfW * 3.0f,
                    coreR, coreG, coreB, 0.12f * alphaScale);

            // Layer 2 — Mid glow (2× width, 30% alpha)
            drawQuadLayer(vc, posM, pose,
                    sx, sy, sz, ex, ey, ez,
                    rX, rY, rZ, halfW * 2.0f,
                    coreR, coreG, coreB, 0.30f * alphaScale);

            // Layer 3 — Core beam (1× width, full alpha)
            drawQuadLayer(vc, posM, pose,
                    sx, sy, sz, ex, ey, ez,
                    rX, rY, rZ, halfW,
                    coreR, coreG, coreB, 1.0f * alphaScale);

            // Layer 4 — Hot core (white, flickers, very thin)
            drawQuadLayer(vc, posM, pose,
                    sx, sy, sz, ex, ey, ez,
                    rX, rY, rZ, halfW * 0.25f * flicker,
                    1.0f, 1.0f, 1.0f, 1.0f * alphaScale);

            poseStack.popPose();
        }

        // Don't call super.render() — no shadow, no name tag needed for a beam
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Quad helper
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Draws a single billboard quad for one beam layer.
     *
     * <p>The quad faces the camera by construction: the four vertices are offset
     * from start and end by ±{@code halfWidth} along the precomputed billboard
     * right vector {@code (rX, rY, rZ)}.
     *
     * <p>UV coordinates (0,0)→(1,1) map onto the solid-white texture so the raw
     * color/alpha values come through without any texture modulation.
     */
    private static void drawQuadLayer(
            VertexConsumer vc, Matrix4f posM, PoseStack.Pose pose,
            float sx, float sy, float sz,
            float ex, float ey, float ez,
            float rX, float rY, float rZ,
            float halfWidth,
            float r, float g, float b, float a) {

        // v0 — start, −right
        vc.addVertex(posM, sx - rX * halfWidth, sy - rY * halfWidth, sz - rZ * halfWidth)
          .setColor(r, g, b, a)
          .setUv(0.0f, 0.0f)
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(LightTexture.FULL_BRIGHT)
          .setNormal(pose, rX, rY, rZ);

        // v1 — start, +right
        vc.addVertex(posM, sx + rX * halfWidth, sy + rY * halfWidth, sz + rZ * halfWidth)
          .setColor(r, g, b, a)
          .setUv(1.0f, 0.0f)
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(LightTexture.FULL_BRIGHT)
          .setNormal(pose, rX, rY, rZ);

        // v2 — end, +right
        vc.addVertex(posM, ex + rX * halfWidth, ey + rY * halfWidth, ez + rZ * halfWidth)
          .setColor(r, g, b, a)
          .setUv(1.0f, 1.0f)
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(LightTexture.FULL_BRIGHT)
          .setNormal(pose, rX, rY, rZ);

        // v3 — end, −right
        vc.addVertex(posM, ex - rX * halfWidth, ey - rY * halfWidth, ez - rZ * halfWidth)
          .setColor(r, g, b, a)
          .setUv(0.0f, 1.0f)
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(LightTexture.FULL_BRIGHT)
          .setNormal(pose, rX, rY, rZ);
    }
}
