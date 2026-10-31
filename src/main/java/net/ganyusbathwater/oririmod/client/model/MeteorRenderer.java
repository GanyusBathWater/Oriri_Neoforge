package net.ganyusbathwater.oririmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.MeteorEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class MeteorRenderer extends EntityRenderer<MeteorEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/entity/meteor.png");
    private static final ResourceLocation BEAM_NOISE_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/effect/beam_noise.png");

    private static final int WHITE = 0xFFFFFFFF;
    private static final int SEARING_GOLD_CORE = 0xFFFFF4D0;

    private final MeteorModel model;

    public MeteorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new MeteorModel(ctx.bakeLayer(MeteorModel.LAYER_LOCATION));
        this.shadowRadius = 0.7f;
    }

    @Override
    public boolean shouldRender(MeteorEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(MeteorEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float scale = entity.getMeteorScale();
        float age = entity.tickCount + partialTicks;

        if (!entity.hasVisualOverhaul()) {
            // Non-overhaul meteors (e.g. Meteor Shower barrage): standard solid model
            poseStack.pushPose();
            poseStack.scale(scale, scale, scale);
            float spin = age * 6.0f;
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            poseStack.mulPose(Axis.XP.rotationDegrees(spin * 0.6f));

            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
            model.renderToBuffer(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, WHITE);
            poseStack.popPose();
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        // ── Option 2: Depth-Writing 3D Glowing Core (Writes to depthtex0 so shader clouds never overwrite it) ──
        poseStack.pushPose();
        float innerCoreScale = scale * 0.85f;
        poseStack.scale(innerCoreScale, innerCoreScale, innerCoreScale);

        float spin = age * 7.5f;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.XP.rotationDegrees(spin * 0.6f));

        // 1. Solid full-bright rock core (writes real 3D depth)
        VertexConsumer solidVc = buffer.getBuffer(RenderType.entitySolid(getTextureLocation(entity)));
        model.renderToBuffer(poseStack, solidVc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, SEARING_GOLD_CORE);

        // 2. Opaque depth-writing beaconBeam shell (triggers shader emissive bloom while writing depth)
        poseStack.scale(1.06f, 1.06f, 1.06f);
        VertexConsumer beaconShellVc = buffer.getBuffer(RenderType.beaconBeam(BEAM_NOISE_TEXTURE, false));
        model.renderToBuffer(poseStack, beaconShellVc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, SEARING_GOLD_CORE);
        poseStack.popPose();

        // 3. Depth-writing 3D plasma core spine along the trajectory (writes depth in front of sky/clouds)
        renderDepthWritingCoreSpine(entity, age, partialTicks, scale, poseStack, buffer);

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * Renders a tight, depth-writing ({@code RenderType.beaconBeam(tex, false)}) 3D core spine along
     * the meteor's diagonal trajectory so the 3D world core has physical depth in shader G-buffers.
     */
    private void renderDepthWritingCoreSpine(MeteorEntity entity, float age, float partialTicks, float scale,
                                             PoseStack poseStack, MultiBufferSource buffer) {
        double lerpX = Mth.lerp(partialTicks, entity.xo, entity.getX());
        double lerpY = Mth.lerp(partialTicks, entity.yo, entity.getY());
        double lerpZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());

        Vec3 tailDir = null;
        BlockPos impactPos = entity.getImpactPos();
        if (!impactPos.equals(BlockPos.ZERO)) {
            Vec3 fromImpact = new Vec3(
                    lerpX - (impactPos.getX() + 0.5),
                    lerpY - (impactPos.getY() + 0.5),
                    lerpZ - (impactPos.getZ() + 0.5)
            );
            if (fromImpact.lengthSqr() > 1e-3) {
                tailDir = fromImpact.normalize();
            }
        }
        if (tailDir == null) {
            Vec3 vel = entity.getDeltaMovement();
            tailDir = vel.lengthSqr() > 1e-5 ? vel.normalize().reverse() : new Vec3(0.0, 1.0, 0.0);
        }

        float maxTailLen = Math.max(30.0f, scale * 7.5f);
        float tailLen = Math.min(maxTailLen, 4.0f + age * 4.0f);

        float ex = (float) (tailDir.x * tailLen);
        float ey = (float) (tailDir.y * tailLen);
        float ez = (float) (tailDir.z * tailLen);

        Vec3 camPos = this.entityRenderDispatcher.camera.getPosition();
        float camDX = (float) (camPos.x - lerpX);
        float camDY = (float) (camPos.y - lerpY);
        float camDZ = (float) (camPos.z - lerpZ);
        float camLen = Mth.sqrt(camDX * camDX + camDY * camDY + camDZ * camDZ);
        if (camLen > 1e-4f) {
            camDX /= camLen;
            camDY /= camLen;
            camDZ /= camLen;
        } else {
            camDX = 0.0f;
            camDY = 0.0f;
            camDZ = 1.0f;
        }

        float dirX = (float) tailDir.x;
        float dirY = (float) tailDir.y;
        float dirZ = (float) tailDir.z;

        float rX = dirY * camDZ - dirZ * camDY;
        float rY = dirZ * camDX - dirX * camDZ;
        float rZ = dirX * camDY - dirY * camDX;
        float rLen = Mth.sqrt(rX * rX + rY * rY + rZ * rZ);
        if (rLen < 1e-4f) {
            rX = 1.0f;
            rY = 0.0f;
            rZ = 0.0f;
        } else {
            rX /= rLen;
            rY /= rLen;
            rZ /= rLen;
        }

        float startHalfW = scale * 0.18f;
        float vScroll = -age * 0.25f;
        float vLen = tailLen * 0.2f;

        poseStack.pushPose();
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.beaconBeam(BEAM_NOISE_TEXTURE, false));

        vc.addVertex(mat, -rX * startHalfW, -rY * startHalfW, -rZ * startHalfW)
                .setColor(1.0f, 0.92f, 0.65f, 1.0f)
                .setUv(0.0f, vScroll)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, rX, rY, rZ);

        vc.addVertex(mat, rX * startHalfW, rY * startHalfW, rZ * startHalfW)
                .setColor(1.0f, 0.92f, 0.65f, 1.0f)
                .setUv(1.0f, vScroll)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, rX, rY, rZ);

        vc.addVertex(mat, ex, ey, ez)
                .setColor(1.0f, 0.60f, 0.10f, 1.0f)
                .setUv(1.0f, vScroll + vLen)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, rX, rY, rZ);

        vc.addVertex(mat, ex, ey, ez)
                .setColor(1.0f, 0.60f, 0.10f, 1.0f)
                .setUv(0.0f, vScroll + vLen)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, rX, rY, rZ);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity entity) {
        return TEXTURE;
    }
}
