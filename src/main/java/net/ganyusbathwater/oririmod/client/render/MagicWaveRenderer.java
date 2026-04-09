package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.entity.MagicWaveEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * MagicWaveRenderer — renders a dynamic Swept-V overlapping dual-layer magical fin.
 *
 * It uses the provided "half wave" texture mirroring it outwardly to form an arrowhead. 
 * Renders multiple instances of this arrowhead geometry that organically slide 
 * backward and forward across each other to simulate violent rushing energy momentum.
 */
public class MagicWaveRenderer extends EntityRenderer<MagicWaveEntity> {

    private static final RenderType WAVE_LAYER =
            RenderType.entityTranslucent(
                    ResourceLocation.parse("oririmod:textures/effect/magic_circles/wave.png"));

    // Base dimensions for the V-shape geometries
    private static final float HALF_HEIGHT = 0.5f;   // Total 1.0 block tall
    private static final float HALF_WIDTH  = 1.2f;   // Wide wingspan

    public MagicWaveRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(MagicWaveEntity entity) {
        return ResourceLocation.parse("oririmod:textures/effect/magic_circles/wave.png");
    }

    @Override
    public boolean shouldRender(MagicWaveEntity entity,
                                net.minecraft.client.renderer.culling.Frustum frustum,
                                double cx, double cy, double cz) {
        return true; // always render — small entity bounding box can be misleading
    }

    @Override
    public void render(MagicWaveEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        int age      = entity.getAgeTicks();
        int lifetime = MagicWaveEntity.MAX_LIFETIME;
        int fade     = MagicWaveEntity.FADE_TICKS;

        // ── Alpha fade in/out ───────────────────────────────────────────────
        float totalAge = age + partialTicks;
        float alpha = 1.0f;
        if (totalAge < 10f) {
            alpha = totalAge / 10f;
        } else if (totalAge > lifetime - fade) {
            alpha = Math.max(0f, (lifetime - totalAge) / fade);
        }
        if (alpha < 0.01f) return;

        // ── Decode color ────────────────────────────────────────────────────
        int argb  = entity.getWaveColor();
        float cR  = ((argb >> 16) & 0xFF) / 255f;
        float cG  = ((argb >>  8) & 0xFF) / 255f;
        float cB  = ( argb        & 0xFF) / 255f;

        // ── Pulse / Oscillation (Sliding the two waves dynamically) ─────────
        // Slower, subtle sine wave to simulate fluid rushing energy 
        float oscillation = (float) Math.sin(totalAge * 0.4f);

        // ── Vectors ─────────────────────────────────────────────────────────
        float fX = entity.getDirX();
        float fZ = entity.getDirZ();

        // Right-perpendicular vector
        float rX = -fZ;
        float rZ =  fX;

        poseStack.pushPose();

        PoseStack.Pose pose = poseStack.last();
        Matrix4f posM       = pose.pose();
        VertexConsumer vc   = buffer.getBuffer(WAVE_LAYER);

        // Fast, tiny vertical shake math
        float coreY  = (float) Math.sin(totalAge * 1.5f) * 0.05f;
        float ghostY = (float) Math.sin(totalAge * 1.2f) * 0.04f; // slightly out of sync

        // ── First V-Shape (Ghost/Phantom trailing layer) ────────────────────
        // Slides backwards when the core slides forward. Faded heavily, slightly wider.
        // Reduced amplitude heavily to stop "shaking", now a smooth glide horizontally.
        float ghostShift = -oscillation * 0.05f - 0.1f; 
        drawVShapeLayer(vc, posM, pose, 
                fX, fZ, rX, rZ, 
                HALF_WIDTH * 1.3f, HALF_HEIGHT, HALF_WIDTH * 1.3f, // sweepBack matches width for pure 45deg
                ghostShift, ghostY, 
                cR, cG, cB, alpha * 0.40f, packedLight);

        // ── Second V-Shape (Core surging layer) ─────────────────────────────
        // Reduced amplitude heavily to stop "shaking" horizontally.
        float coreShift = oscillation * 0.05f;
        drawVShapeLayer(vc, posM, pose, 
                fX, fZ, rX, rZ, 
                HALF_WIDTH, HALF_HEIGHT, HALF_WIDTH, 
                coreShift, coreY,
                cR, cG, cB, alpha * 0.95f, packedLight);

        poseStack.popPose();
    }

    /**
     * Builds a 3D geometry V-shape / arrowhead sweeping outward and heavily sloped backwards.
     * U/V coordinates are locked assuming a "half wave" asset where U=0 is the central core peak,
     * and U=1 is the faded sweeping trail.
     */
    private static void drawVShapeLayer(VertexConsumer vc, Matrix4f posM, PoseStack.Pose pose,
                                        float fX, float fZ, float rX, float rZ,
                                        float hw, float hh, float sweepBack,
                                        float shiftForward, float shiftY,
                                        float r, float g, float b, float a, int light) {

        // The absolute tip/nose of the V-shape sliding along the forward vector
        float noseX = fX * shiftForward;
        float noseZ = fZ * shiftForward;
        
        // The dragging trailing outer edges positioned sharply backward along the vector
        float rightTrailX = noseX + rX * hw - fX * sweepBack;
        float rightTrailZ = noseZ + rZ * hw - fZ * sweepBack;

        float leftTrailX = noseX - rX * hw - fX * sweepBack;
        float leftTrailZ = noseZ - rZ * hw - fZ * sweepBack;

        float bY = shiftY;
        float tY = (hh * 2f) + shiftY; 

        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

        // ── RIGHT WING (Nose -> Right Edge) ──
        // Front face (U=1 at Nose, U=0 at Trail edge to flip orientation)
        vc.addVertex(posM, noseX,       bY, noseZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(posM, noseX,       tY, noseZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(posM, rightTrailX, tY, rightTrailZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(posM, rightTrailX, bY, rightTrailZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        // Back face (so it can be seen if looked at horizontally from the interior)
        vc.addVertex(posM, rightTrailX, bY, rightTrailZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(posM, rightTrailX, tY, rightTrailZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(posM, noseX,       tY, noseZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(posM, noseX,       bY, noseZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);

        // ── LEFT WING (Left Edge -> Nose) ──
        // Front face
        vc.addVertex(posM, leftTrailX,  bY, leftTrailZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(posM, leftTrailX,  tY, leftTrailZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(posM, noseX,       tY, noseZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(posM, noseX,       bY, noseZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        // Back face
        vc.addVertex(posM, noseX,       bY, noseZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(posM, noseX,       tY, noseZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(posM, leftTrailX,  tY, leftTrailZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(posM, leftTrailX,  bY, leftTrailZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
    }
}
