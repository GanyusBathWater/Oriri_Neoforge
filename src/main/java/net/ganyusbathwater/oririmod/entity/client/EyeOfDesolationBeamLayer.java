package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.entity.custom.EyeOfDesolationEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Render layer that draws a guardian-style laser beam from the Eye of Desolation
 * to its current target.
 *
 * Visual design:
 *  - Two perpendicular flat quads (forming a cross), textured with the vanilla
 *    guardian_beam.png and tinted bright red-orange.
 *  - Outer translucent glow quads at 2.5× the width.
 *  - UV V-coordinate scrolls each frame to create the "flowing laser" effect.
 *  - Both inner and outer layers are fully emissive (LightTexture.FULL_BRIGHT).
 *  - Opacity and width scale from 0 → 1 over the 60-tick charge-up.
 */
public class EyeOfDesolationBeamLayer extends GeoRenderLayer<EyeOfDesolationEntity> {

    /** Re-use the vanilla guardian beam texture — it tiles perfectly as a laser. */
    private static final ResourceLocation BEAM_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/guardian_beam.png");

    public EyeOfDesolationBeamLayer(GeoRenderer<EyeOfDesolationEntity> renderer) {
        super(renderer);
    }

    // ─── GeoRenderLayer entry point ──────────────────────────────────────────
    @Override
    public void render(PoseStack poseStack,
                       EyeOfDesolationEntity entity,
                       BakedGeoModel bakedModel,
                       RenderType renderType,
                       MultiBufferSource bufferSource,
                       VertexConsumer buffer,
                       float partialTick,
                       int packedLight,
                       int packedOverlay) {

        int targetId = entity.getTargetId();
        if (targetId < 0) return;

        Entity target = entity.level().getEntity(targetId);
        if (target == null || !target.isAlive()) return;

        float scale = entity.getLaserProgress(partialTick);
        if (scale <= 0.0f) return;

        // 1. Find the eye bone (configured as "head" in the model)
        software.bernie.geckolib.cache.object.GeoBone bone = bakedModel.getBone("eye_of_desolation").orElse(null);
        if (bone == null) return;

        // 2. Align the PoseStack with the physical bone animation
        poseStack.pushPose();
        software.bernie.geckolib.util.RenderUtil.prepMatrixForBone(poseStack, bone);
        // Pivot adjustment: the eye is at Y=24 pixels (1.5 blocks) relative to the root pivot
        poseStack.translate(0, 1.5, 0);

        // 3. Coordinate Math: Bring target into this Bone-Local space
        // Get target center in camera-relative world space
        Vec3 camPos = net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
        Vec3 worldTarget = target.getBoundingBox().getCenter().subtract(camPos);

        // Transform camera-relative world target into our currently rotated/translated Bone Space
        Matrix4f localMatrix = new Matrix4f(poseStack.last().pose()).invert();
        org.joml.Vector4f localVec = new org.joml.Vector4f((float)worldTarget.x, (float)worldTarget.y, (float)worldTarget.z, 1.0f);
        localMatrix.transform(localVec);
        
        // Final relative target relative to (0,0,0) of the eye core
        Vec3 localEnd = new Vec3(localVec.x(), localVec.y(), localVec.z());
        Vec3 localStart = Vec3.ZERO;

        drawBeam(poseStack, bufferSource, localStart, localEnd, scale, entity.tickCount, partialTick);
        
        poseStack.popPose();
    }

    private void drawBeam(PoseStack poseStack, MultiBufferSource bufferSource, 
                          Vec3 start, Vec3 end, float scale, int tickCount, float partialTick) {
        
        Vec3 dir = end.subtract(start);
        float dist = (float) dir.length();
        if (dist < 0.01f) return;
        dir = dir.normalize();

        // Basis Vectors for the beam planes
        Vec3 upRef = new Vec3(0, 1, 0);
        if (Math.abs(dir.y) > 0.95f) {
            upRef = new Vec3(1, 0, 0);
        }
        Vec3 right = dir.cross(upRef).normalize();
        Vec3 up = dir.cross(right).normalize();

        Matrix4f posM = poseStack.last().pose();
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(BEAM_TEXTURE));
        float uvOffset = -(tickCount + partialTick) * 0.04f;

        // 3 planes, 60 degrees apart for volumetric look
        for (int i = 0; i < 3; i++) {
            float angle = (float) Math.toRadians(i * 60.0f);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            Vec3 planeWidthVec = right.scale(cos).add(up.scale(sin));
            
            float coreHalf = 0.09f * scale;
            float glowHalf = 0.24f * scale;

            drawPlane(vc, posM, start, end, planeWidthVec, coreHalf, dist, uvOffset, 1.0f, 0.25f, 0.05f, 0.90f * scale);
            drawPlane(vc, posM, start, end, planeWidthVec, glowHalf, dist, uvOffset, 1.0f, 0.15f, 0.03f, 0.35f * scale);
        }
    }

    private void drawPlane(VertexConsumer vc, Matrix4f mat, Vec3 start, Vec3 end, 
                          Vec3 widthVec, float half, float dist, float uvOffset,
                          float r, float g, float b, float a) {
        
        int ri = (int) (r * 255), gi = (int) (g * 255), bi = (int) (b * 255), ai = (int) (a * 255);
        float v0 = uvOffset;
        float v1 = uvOffset + dist * 0.5f;

        Vec3 p0 = start.add(widthVec.scale(-half));
        Vec3 p1 = start.add(widthVec.scale(half));
        Vec3 p2 = end.add(widthVec.scale(half));
        Vec3 p3 = end.add(widthVec.scale(-half));

        Vec3 normal = widthVec.cross(end.subtract(start)).normalize();

        addVertex(vc, mat, p0, ri, gi, bi, ai, 0, v0, normal);
        addVertex(vc, mat, p1, ri, gi, bi, ai, 1, v0, normal);
        addVertex(vc, mat, p2, ri, gi, bi, ai, 1, v1, normal);
        addVertex(vc, mat, p3, ri, gi, bi, ai, 0, v1, normal);

        addVertex(vc, mat, p1, ri, gi, bi, ai, 1, v0, normal.reverse());
        addVertex(vc, mat, p0, ri, gi, bi, ai, 0, v0, normal.reverse());
        addVertex(vc, mat, p3, ri, gi, bi, ai, 0, v1, normal.reverse());
        addVertex(vc, mat, p2, ri, gi, bi, ai, 1, v1, normal.reverse());
    }

    private void addVertex(VertexConsumer vc, Matrix4f mat, Vec3 p,
                          int r, int g, int b, int a, float u, float v, Vec3 n) {
        vc.addVertex(mat, (float)p.x, (float)p.y, (float)p.z)
          .setColor(r, g, b, a)
          .setUv(u, v)
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(LightTexture.FULL_BRIGHT)
          .setNormal((float)n.x, (float)n.y, (float)n.z);
    }
}
