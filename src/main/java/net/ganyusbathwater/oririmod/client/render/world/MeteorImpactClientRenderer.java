package net.ganyusbathwater.oririmod.client.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.ModRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Renders the 3D ground-impact after-effects for standard Meteors:
 * <ul>
 *   <li>A towering vertical energy pillar shooting skyward from the impact center</li>
 *   <li>Concentric expanding ground shockwave rings</li>
 *   <li>A jagged high-voltage perimeter fracture/lightning arc (matching Screenshot 2)</li>
 * </ul>
 * Uses shader-compatible additive RenderTypes so Iris/Oculus applies bloom, and during
 * the {@link AnimeImpactState} pure B&W inversion frame, the blazing white-gold core
 * inverts into a stark black silhouette column framed by a glowing white aura.
 */
@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class MeteorImpactClientRenderer {

    private static final ResourceLocation BEAM_NOISE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/effect/beam_noise.png");
    private static final RenderType PILLAR_LAYER = ModRenderTypes.getAdditiveBeam(BEAM_NOISE_TEXTURE);

    private record ActiveImpact(Vec3 center, float power, long startTimeMs, int durationMs) {
    }

    private static final List<ActiveImpact> ACTIVE_IMPACTS = new CopyOnWriteArrayList<>();

    private MeteorImpactClientRenderer() {
    }

    public static void spawnImpact(double x, double y, double z, float power) {
        spawnImpact(x, y, z, power, true);
    }

    /**
     * @param impactFlash whether to trigger the black &amp; white {@link AnimeImpactState} frame
     *                    (disabled for small / medium Meteor Shower meteors)
     */
    public static void spawnImpact(double x, double y, double z, float power, boolean impactFlash) {
        int durationMs = Math.max(900, Math.min(2200, (int) (power * 55.0f)));
        ACTIVE_IMPACTS.add(new ActiveImpact(new Vec3(x, y, z), Math.max(4.0f, power), System.currentTimeMillis(), durationMs));

        Minecraft mc = Minecraft.getInstance();
        if (impactFlash && mc.player != null && mc.player.distanceToSqr(x, y, z) <= 140.0 * 140.0) {
            // Scale the high-contrast B&W anime impact duration slightly with meteor power (280ms .. 450ms)
            long impactFrameMs = Math.max(280L, Math.min(450L, (long) (power * 12.0f)));
            AnimeImpactState.triggerImpact(impactFrameMs);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (ACTIVE_IMPACTS.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        Iterator<ActiveImpact> it = ACTIVE_IMPACTS.iterator();
        while (it.hasNext()) {
            ActiveImpact impact = it.next();
            long elapsed = now - impact.startTimeMs();
            if (elapsed >= impact.durationMs()) {
                ACTIVE_IMPACTS.remove(impact);
                continue;
            }

            float progress = Math.max(0.0f, Math.min(1.0f, (float) elapsed / (float) impact.durationMs()));
            renderSingleImpact(poseStack, buffers, camPos, impact, progress, elapsed);
        }

        poseStack.popPose();

        buffers.endBatch(PILLAR_LAYER);
        buffers.endBatch(ModRenderTypes.getAdditiveFlare());
    }

    private static void renderSingleImpact(PoseStack poseStack, MultiBufferSource buffers, Vec3 camPos,
                                           ActiveImpact impact, float progress, long elapsedMs) {
        Vec3 c = impact.center();
        float power = impact.power();

        // Fast punch-in during first 8% of lifetime, then exponential taper
        float punchIn = Math.min(1.0f, progress / 0.08f);
        float fadeOut = (float) Math.pow(1.0f - progress, 1.5);
        float widthScale = punchIn * fadeOut;

        if (widthScale <= 0.01f) return;

        float pillarRadius = Math.max(2.0f, power * 0.28f) * widthScale;
        float pillarHeight = Math.max(65.0f, power * 4.5f);

        // ── 1. Towering Vertical Impact Pillar ───────────────────────────────
        VertexConsumer beamVc = buffers.getBuffer(PILLAR_LAYER);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();

        // Compute camera billboard right vector for horizontal X/Z plane
        double dx = camPos.x - c.x;
        double dz = camPos.z - c.z;
        double horizLen = Math.sqrt(dx * dx + dz * dz);
        float rX = horizLen > 1e-4 ? (float) (-dz / horizLen) : 1.0f;
        float rZ = horizLen > 1e-4 ? (float) (dx / horizLen) : 0.0f;

        float vScroll = -(elapsedMs * 0.018f);
        float vLen = pillarHeight * 0.12f;

        float baseY = (float) c.y - power * 0.35f;
        float topY = (float) c.y + pillarHeight;

        // Outer fierce orange-gold corona sheath (2.2x width)
        drawVerticalBillboardQuad(beamVc, mat, pose,
                (float) c.x, baseY, (float) c.z, topY,
                rX, rZ, pillarRadius * 2.2f,
                1.0f, 0.42f, 0.04f, 0.55f * fadeOut,
                vScroll * 0.6f, vLen);

        // Mid golden-amber column (1.25x width)
        drawVerticalBillboardQuad(beamVc, mat, pose,
                (float) c.x, baseY, (float) c.z, topY,
                rX, rZ, pillarRadius * 1.25f,
                1.0f, 0.72f, 0.15f, 0.85f * fadeOut,
                vScroll, vLen);

        // Searing white-gold inner core (0.55x width) — inverts to pitch black during B&W impact frame!
        drawVerticalBillboardQuad(beamVc, mat, pose,
                (float) c.x, baseY, (float) c.z, topY,
                rX, rZ, pillarRadius * 0.55f,
                1.0f, 0.98f, 0.90f, 1.0f * fadeOut,
                vScroll * 1.4f, vLen);

        // ── 2. Expanding Ground Shockwave Rings & Jagged Fracture Lightning ──
        VertexConsumer flareVc = buffers.getBuffer(ModRenderTypes.getAdditiveFlare());

        // Primary fast-expanding shockwave ring
        float ringProgress = (float) (1.0 - Math.pow(1.0 - progress, 2.6));
        float maxRingRadius = Math.max(12.0f, power * 1.85f);
        float currRingRadius = maxRingRadius * ringProgress;
        float ringThickness = Math.max(0.8f, power * 0.09f) * fadeOut;

        drawGroundShockwaveRing(flareVc, mat, (float) c.x, (float) c.y + 0.25f, (float) c.z,
                currRingRadius, ringThickness,
                1.0f, 0.65f, 0.10f, 0.80f * fadeOut,
                1.0f, 0.96f, 0.85f, 0.95f * fadeOut);

        // Secondary inner aftershock ring
        if (progress > 0.06f) {
            float innerProgress = (float) (1.0 - Math.pow(1.0 - (progress - 0.06f) / 0.94f, 2.2));
            float innerRadius = maxRingRadius * 0.65f * innerProgress;
            drawGroundShockwaveRing(flareVc, mat, (float) c.x, (float) c.y + 0.35f, (float) c.z,
                    innerRadius, ringThickness * 0.7f,
                    1.0f, 0.45f, 0.05f, 0.65f * fadeOut,
                    1.0f, 0.88f, 0.55f, 0.85f * fadeOut);
        }

        // Jagged crackle lightning arc (Screenshot 2 foreground / rim effect)
        long jagSeed = (long) (c.x * 31 + c.z * 17) ^ (elapsedMs / 45L);
        drawJaggedLightningPerimeter(flareVc, mat, (float) c.x, (float) c.y + 0.6f, (float) c.z,
                currRingRadius * 0.92f, Math.max(0.35f, power * 0.045f) * fadeOut,
                fadeOut, jagSeed);
    }

    private static void drawVerticalBillboardQuad(
            VertexConsumer vc, Matrix4f mat, PoseStack.Pose pose,
            float cx, float bottomY, float cz, float topY,
            float rX, float rZ, float halfWidth,
            float r, float g, float b, float a,
            float vOffset, float vLength) {

        float v1 = vOffset;
        float v2 = vOffset + vLength;

        vc.addVertex(mat, cx - rX * halfWidth, bottomY, cz - rZ * halfWidth)
                .setColor(r, g, b, a)
                .setUv(0.0f, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, rX, 0.0f, rZ);

        vc.addVertex(mat, cx + rX * halfWidth, bottomY, cz + rZ * halfWidth)
                .setColor(r, g, b, a)
                .setUv(1.0f, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, rX, 0.0f, rZ);

        vc.addVertex(mat, cx + rX * halfWidth, topY, cz + rZ * halfWidth)
                .setColor(r, g, b, a)
                .setUv(1.0f, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, rX, 0.0f, rZ);

        vc.addVertex(mat, cx - rX * halfWidth, topY, cz - rZ * halfWidth)
                .setColor(r, g, b, a)
                .setUv(0.0f, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, rX, 0.0f, rZ);
    }

    private static void drawGroundShockwaveRing(
            VertexConsumer vc, Matrix4f mat,
            float cx, float cy, float cz,
            float radius, float halfThickness,
            float outerR, float outerG, float outerB, float outerA,
            float coreR, float coreG, float coreB, float coreA) {

        int segments = 40;
        float innerR = Math.max(0.05f, radius - halfThickness);
        float outerRad = radius + halfThickness;

        for (int i = 0; i < segments; i++) {
            float a1 = (float) (2.0 * Math.PI * i / segments);
            float a2 = (float) (2.0 * Math.PI * (i + 1) / segments);

            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);
            float cos2 = (float) Math.cos(a2);
            float sin2 = (float) Math.sin(a2);

            // Inner feather -> mid hot core
            vc.addVertex(mat, cx + cos1 * innerR, cy, cz + sin1 * innerR).setColor(outerR, outerG, outerB, 0.0f);
            vc.addVertex(mat, cx + cos2 * innerR, cy, cz + sin2 * innerR).setColor(outerR, outerG, outerB, 0.0f);
            vc.addVertex(mat, cx + cos2 * radius, cy + 0.1f, cz + sin2 * radius).setColor(coreR, coreG, coreB, coreA);
            vc.addVertex(mat, cx + cos1 * radius, cy + 0.1f, cz + sin1 * radius).setColor(coreR, coreG, coreB, coreA);

            // Mid hot core -> outer feather
            vc.addVertex(mat, cx + cos1 * radius, cy + 0.1f, cz + sin1 * radius).setColor(coreR, coreG, coreB, coreA);
            vc.addVertex(mat, cx + cos2 * radius, cy + 0.1f, cz + sin2 * radius).setColor(coreR, coreG, coreB, coreA);
            vc.addVertex(mat, cx + cos2 * outerRad, cy, cz + sin2 * outerRad).setColor(outerR, outerG, outerB, 0.0f);
            vc.addVertex(mat, cx + cos1 * outerRad, cy, cz + sin1 * outerRad).setColor(outerR, outerG, outerB, 0.0f);
        }
    }

    private static void drawJaggedLightningPerimeter(
            VertexConsumer vc, Matrix4f mat,
            float cx, float cy, float cz,
            float baseRadius, float ribbonHalfHeight,
            float alpha, long seed) {

        Random rand = new Random(seed);
        int segments = 36;
        float[] rOffsets = new float[segments + 1];
        float[] yOffsets = new float[segments + 1];

        for (int i = 0; i < segments; i++) {
            rOffsets[i] = baseRadius + (rand.nextFloat() - 0.5f) * (baseRadius * 0.22f);
            yOffsets[i] = cy + (rand.nextFloat() - 0.3f) * (baseRadius * 0.14f);
        }
        rOffsets[segments] = rOffsets[0];
        yOffsets[segments] = yOffsets[0];

        for (int i = 0; i < segments; i++) {
            float a1 = (float) (2.0 * Math.PI * i / segments);
            float a2 = (float) (2.0 * Math.PI * (i + 1) / segments);

            float x1 = cx + (float) Math.cos(a1) * rOffsets[i];
            float z1 = cz + (float) Math.sin(a1) * rOffsets[i];
            float y1 = yOffsets[i];

            float x2 = cx + (float) Math.cos(a2) * rOffsets[i + 1];
            float z2 = cz + (float) Math.sin(a2) * rOffsets[i + 1];
            float y2 = yOffsets[i + 1];

            // Outer fierce orange-gold halo ribbon
            float outerH = ribbonHalfHeight * 2.4f;
            vc.addVertex(mat, x1, y1 - outerH, z1).setColor(1.0f, 0.55f, 0.05f, 0.65f * alpha);
            vc.addVertex(mat, x2, y2 - outerH, z2).setColor(1.0f, 0.55f, 0.05f, 0.65f * alpha);
            vc.addVertex(mat, x2, y2 + outerH, z2).setColor(1.0f, 0.55f, 0.05f, 0.65f * alpha);
            vc.addVertex(mat, x1, y1 + outerH, z1).setColor(1.0f, 0.55f, 0.05f, 0.65f * alpha);

            // Inner blazing white-gold lightning core
            vc.addVertex(mat, x1, y1 - ribbonHalfHeight, z1).setColor(1.0f, 0.98f, 0.90f, 1.0f * alpha);
            vc.addVertex(mat, x2, y2 - ribbonHalfHeight, z2).setColor(1.0f, 0.98f, 0.90f, 1.0f * alpha);
            vc.addVertex(mat, x2, y2 + ribbonHalfHeight, z2).setColor(1.0f, 0.98f, 0.90f, 1.0f * alpha);
            vc.addVertex(mat, x1, y1 + ribbonHalfHeight, z1).setColor(1.0f, 0.98f, 0.90f, 1.0f * alpha);
        }
    }
}
