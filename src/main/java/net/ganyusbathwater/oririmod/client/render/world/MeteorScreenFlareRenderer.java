package net.ganyusbathwater.oririmod.client.render.world;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.MeteorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the blinding fierce orange-gold 8-point celestial star-flare and diagonal entry streak
 * (Screenshot 1) in post-shader screen space ({@link RenderGuiEvent.Pre}) with 3D line-of-sight
 * raycast occlusion.
 *
 * <p>Because this executes after the 3D world and all Iris/Oculus shader passes (volumetric clouds,
 * water, tonemapping) have finished, shader clouds and water can NEVER draw over the light, while
 * solid terrain naturally occludes it via the line-of-sight visibility check.
 */
@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class MeteorScreenFlareRenderer {

    private record ProjectedMeteorFlare(
            float headX, float headY,
            float tailX, float tailY,
            float apparentRadius,
            float age,
            float visibility
    ) {
    }

    private static final List<ProjectedMeteorFlare> PROJECTED_FLARES = new ArrayList<>();
    private static final Matrix4f CAPTURED_VIEW_MAT = new Matrix4f();
    private static final Matrix4f CAPTURED_PROJ_MAT = new Matrix4f();
    private static Vec3 capturedCamPos = Vec3.ZERO;
    private static boolean matricesValid = false;

    private MeteorScreenFlareRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelAfterEntities(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            matricesValid = false;
            return;
        }

        capturedCamPos = event.getCamera().getPosition();
        Quaternionf camRot = event.getCamera().rotation().conjugate(new Quaternionf());
        CAPTURED_VIEW_MAT.identity().rotation(camRot);
        CAPTURED_PROJ_MAT.set(event.getProjectionMatrix());
        matricesValid = true;
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        if (!matricesValid) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PROJECTED_FLARES.clear();

        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(mc.isPaused());
        int guiW = event.getGuiGraphics().guiWidth();
        int guiH = event.getGuiGraphics().guiHeight();

        for (Entity ent : mc.level.entitiesForRendering()) {
            if (!(ent instanceof MeteorEntity meteor) || !meteor.isAlive() || !meteor.hasVisualOverhaul()) {
                continue;
            }

            double lx = Mth.lerp(pt, meteor.xo, meteor.getX());
            double ly = Mth.lerp(pt, meteor.yo, meteor.getY());
            double lz = Mth.lerp(pt, meteor.zo, meteor.getZ());
            Vec3 headWorld = new Vec3(lx, ly, lz);

            float scale = meteor.getMeteorScale();
            float age = meteor.tickCount + pt;

            // 1. Check 3D line-of-sight occlusion against solid blocks (sampled across 3 points for smooth fade)
            float visibility = computeVisibility(mc, capturedCamPos, headWorld, scale * 0.4f);
            if (visibility <= 0.01f) {
                continue;
            }

            // 2. Compute 3D tail world position along the diagonal approach trajectory
            Vec3 tailDir = null;
            BlockPos impactPos = meteor.getImpactPos();
            if (!impactPos.equals(BlockPos.ZERO)) {
                Vec3 fromImpact = new Vec3(
                        lx - (impactPos.getX() + 0.5),
                        ly - (impactPos.getY() + 0.5),
                        lz - (impactPos.getZ() + 0.5)
                );
                if (fromImpact.lengthSqr() > 1e-3) {
                    tailDir = fromImpact.normalize();
                }
            }
            if (tailDir == null) {
                Vec3 vel = meteor.getDeltaMovement();
                tailDir = vel.lengthSqr() > 1e-5 ? vel.normalize().reverse() : new Vec3(0.0, 1.0, 0.0);
            }

            float maxTailLen = Math.max(40.0f, scale * 10.0f);
            float tailLen = Math.min(maxTailLen, 6.0f + age * 4.8f);
            Vec3 tailWorld = headWorld.add(tailDir.scale(tailLen));

            // 3. Transform head and tail into camera view space
            Vector4f headView = toViewSpace(headWorld);
            // If meteor head is behind the camera near-plane, skip
            if (headView.z() >= -0.15f) {
                continue;
            }

            Vector4f tailView = toViewSpace(tailWorld);
            // If tail extends behind camera, clip the 3D segment to the camera near plane (z = -0.15)
            if (tailView.z() > -0.15f) {
                float dz = tailView.z() - headView.z();
                if (Math.abs(dz) > 1e-4f) {
                    float t = (-0.15f - headView.z()) / dz;
                    t = Mth.clamp(t, 0.0f, 1.0f);
                    tailView.set(
                            Mth.lerp(t, headView.x(), tailView.x()),
                            Mth.lerp(t, headView.y(), tailView.y()),
                            -0.15f,
                            1.0f
                    );
                }
            }

            // 4. Project view-space positions to 2D GUI coordinates
            Vector4f headClip = new Vector4f(headView);
            CAPTURED_PROJ_MAT.transform(headClip);
            if (headClip.w() <= 0.0001f) continue;
            float headNdcX = headClip.x() / headClip.w();
            float headNdcY = headClip.y() / headClip.w();

            // Skip if far off-screen
            if (Math.abs(headNdcX) > 1.6f || Math.abs(headNdcY) > 1.6f) {
                continue;
            }

            Vector4f tailClip = new Vector4f(tailView);
            CAPTURED_PROJ_MAT.transform(tailClip);
            float tailNdcX = headNdcX;
            float tailNdcY = headNdcY - 0.25f;
            if (tailClip.w() > 0.0001f) {
                tailNdcX = tailClip.x() / tailClip.w();
                tailNdcY = tailClip.y() / tailClip.w();
            }

            float headGuiX = (headNdcX + 1.0f) * 0.5f * guiW;
            float headGuiY = (1.0f - headNdcY) * 0.5f * guiH;
            float tailGuiX = (tailNdcX + 1.0f) * 0.5f * guiW;
            float tailGuiY = (1.0f - tailNdcY) * 0.5f * guiH;

            // Perspective-correct apparent radius on screen so it completely covers the 3D meteor model
            float distDepth = Math.max(3.5f, -headView.z());
            float focalY = Math.abs(CAPTURED_PROJ_MAT.m11());
            float apparentRadius = (scale * 0.85f * focalY * (guiH * 0.5f)) / distDepth;
            apparentRadius = Mth.clamp(apparentRadius, 14.0f, Math.max(guiW, guiH) * 0.42f);

            PROJECTED_FLARES.add(new ProjectedMeteorFlare(
                    headGuiX, headGuiY,
                    tailGuiX, tailGuiY,
                    apparentRadius,
                    age,
                    visibility
            ));
        }

        if (PROJECTED_FLARES.isEmpty()) {
            return;
        }

        renderProjectedFlares(event.getGuiGraphics());
    }

    private static Vector4f toViewSpace(Vec3 worldPos) {
        Vector4f v = new Vector4f(
                (float) (worldPos.x - capturedCamPos.x),
                (float) (worldPos.y - capturedCamPos.y),
                (float) (worldPos.z - capturedCamPos.z),
                1.0f
        );
        CAPTURED_VIEW_MAT.transform(v);
        return v;
    }

    /**
     * Multi-sample raycast from the camera to the meteor center and 4 offsets around it.
     * Ignores soft canopy blocks (leaves/glass with explosion resistance < 3.0f) that the meteor
     * smashes through, but cleanly occludes behind solid terrain/walls.
     */
    private static float computeVisibility(Minecraft mc, Vec3 camPos, Vec3 target, float sampleOffset) {
        Vec3[] offsets = new Vec3[]{
                Vec3.ZERO,
                new Vec3(sampleOffset, 0, 0),
                new Vec3(-sampleOffset, 0, 0),
                new Vec3(0, sampleOffset, 0),
                new Vec3(0, 0, sampleOffset)
        };

        int visibleSamples = 0;
        for (Vec3 off : offsets) {
            Vec3 end = target.add(off);
            BlockHitResult hit = mc.level.clip(new ClipContext(
                    camPos, end,
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE,
                    mc.player
            ));
            if (hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(end) < 2.25) {
                visibleSamples++;
            } else {
                // Allow light to shine through soft blocks (leaves) that the meteor destroys
                var state = mc.level.getBlockState(hit.getBlockPos());
                if (!state.isSolidRender(mc.level, hit.getBlockPos()) || !state.canOcclude()) {
                    visibleSamples++;
                }
            }
        }
        return (float) visibleSamples / (float) offsets.length;
    }

    private static void renderProjectedFlares(GuiGraphics guiGraphics) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        // Pure additive blending: layers accumulate into blinding white-hot light
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE
        );
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f mat = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder vc = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (ProjectedMeteorFlare f : PROJECTED_FLARES) {
            drawSingleMeteorFlare(vc, mat, f);
        }

        BufferUploader.drawWithShader(vc.buildOrThrow());

        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void drawSingleMeteorFlare(BufferBuilder vc, Matrix4f mat, ProjectedMeteorFlare f) {
        float hx = f.headX();
        float hy = f.headY();
        float tx = f.tailX();
        float ty = f.tailY();
        float age = f.age();
        float vis = f.visibility();

        // High-altitude spawn ignition burst ("Sky Tear" during first 14 ticks)
        float spawnBurst = 0.0f;
        if (age < 14.0f) {
            float t = 1.0f - (age / 14.0f);
            spawnBurst = t * t * 2.4f;
        }

        float pulse = 1.0f + 0.12f * Mth.sin(age * 0.95f) + 0.07f * Mth.cos(age * 2.1f);
        float r = f.apparentRadius() * (1.0f + spawnBurst) * pulse;

        // ── 1. Angled Atmospheric Plasma Entry Streak (Screenshot 1 Upper Streak) ──
        float dx = tx - hx;
        float dy = ty - hy;
        float trailLen = Mth.sqrt(dx * dx + dy * dy);
        if (trailLen > 1.0f) {
            float nx = -dy / trailLen;
            float ny = dx / trailLen;

            // Outer fierce orange-red glow wake
            draw2DTaperedStreak(vc, mat, hx, hy, tx, ty, nx, ny,
                    r * 0.85f, r * 0.05f,
                    1.0f, 0.38f, 0.02f, 0.72f * vis,
                    1.0f, 0.20f, 0.0f, 0.0f);

            // Mid golden-amber plasma stream
            draw2DTaperedStreak(vc, mat, hx, hy, tx, ty, nx, ny,
                    r * 0.42f, r * 0.02f,
                    1.0f, 0.75f, 0.14f, 0.92f * vis,
                    1.0f, 0.48f, 0.04f, 0.0f);

            // Inner razor-sharp pure white-gold entry spine (Screenshot 1 laser streak)
            draw2DTaperedStreak(vc, mat, hx, hy, tx, ty, nx, ny,
                    r * 0.16f, 0.0f,
                    1.0f, 1.0f, 0.96f, 1.0f * vis,
                    1.0f, 0.85f, 0.35f, 0.0f);
        }

        // ── 2. Blinding Concentric Core Disks (Completely Obscures the Meteor Model) ──
        // Wide fierce orange-gold atmospheric bloom
        draw2DRadialDisk(vc, mat, hx, hy, r * 2.8f,
                1.0f, 0.45f, 0.02f, 0.65f * vis,
                1.0f, 0.20f, 0.0f, 0.0f);

        // Bright golden-amber mid corona
        draw2DRadialDisk(vc, mat, hx, hy, r * 1.55f,
                1.0f, 0.80f, 0.18f, 0.95f * vis,
                1.0f, 0.48f, 0.04f, 0.0f);

        // Overexposed pure-white inner core (double-layered so the rock model inside is almost invisible)
        draw2DRadialDisk(vc, mat, hx, hy, r * 0.92f,
                1.0f, 0.99f, 0.90f, 1.0f * vis,
                1.0f, 0.85f, 0.25f, 0.0f);
        draw2DRadialDisk(vc, mat, hx, hy, r * 0.65f,
                1.0f, 1.0f, 1.0f, 1.0f * vis,
                1.0f, 0.98f, 0.82f, 0.0f);

        // ── 3. Primary Horizontal (0°) & Vertical (90°) Cross Needles (Screenshot 1) ──
        float mainLen = r * 5.6f;
        float mainW = r * 0.30f;

        // Outer fierce orange-gold cross glow
        draw2DStarSpike(vc, mat, hx, hy, 0.0f, mainLen, mainW * 2.0f,
                1.0f, 0.58f, 0.06f, 0.88f * vis, 1.0f, 0.32f, 0.0f);
        draw2DStarSpike(vc, mat, hx, hy, (float) (Math.PI * 0.5), mainLen, mainW * 2.0f,
                1.0f, 0.58f, 0.06f, 0.88f * vis, 1.0f, 0.32f, 0.0f);

        // Mid golden-white cross
        draw2DStarSpike(vc, mat, hx, hy, 0.0f, mainLen * 0.95f, mainW * 0.92f,
                1.0f, 0.90f, 0.50f, 1.0f * vis, 1.0f, 0.70f, 0.15f);
        draw2DStarSpike(vc, mat, hx, hy, (float) (Math.PI * 0.5), mainLen * 0.95f, mainW * 0.92f,
                1.0f, 0.90f, 0.50f, 1.0f * vis, 1.0f, 0.70f, 0.15f);

        // Razor-sharp pure white inner needle cross
        draw2DStarSpike(vc, mat, hx, hy, 0.0f, mainLen * 0.88f, mainW * 0.38f,
                1.0f, 1.0f, 1.0f, 1.0f * vis, 1.0f, 0.94f, 0.70f);
        draw2DStarSpike(vc, mat, hx, hy, (float) (Math.PI * 0.5), mainLen * 0.88f, mainW * 0.38f,
                1.0f, 1.0f, 1.0f, 1.0f * vis, 1.0f, 0.94f, 0.70f);

        // ── 4. Secondary Diagonal (45° & 135°) Cross Needles (Screenshot 1) ──
        float diagLen = r * 2.75f;
        float diagW = r * 0.19f;
        float a45 = (float) (Math.PI * 0.25);
        float a135 = (float) (Math.PI * 0.75);

        // Outer fierce orange-gold diagonal glow
        draw2DStarSpike(vc, mat, hx, hy, a45, diagLen, diagW * 1.85f,
                1.0f, 0.56f, 0.05f, 0.82f * vis, 1.0f, 0.30f, 0.0f);
        draw2DStarSpike(vc, mat, hx, hy, a135, diagLen, diagW * 1.85f,
                1.0f, 0.56f, 0.05f, 0.82f * vis, 1.0f, 0.30f, 0.0f);

        // Inner pure white-gold diagonal needles
        draw2DStarSpike(vc, mat, hx, hy, a45, diagLen * 0.88f, diagW * 0.50f,
                1.0f, 1.0f, 0.95f, 1.0f * vis, 1.0f, 0.82f, 0.30f);
        draw2DStarSpike(vc, mat, hx, hy, a135, diagLen * 0.88f, diagW * 0.50f,
                1.0f, 1.0f, 0.95f, 1.0f * vis, 1.0f, 0.82f, 0.30f);
    }

    private static void draw2DTaperedStreak(
            BufferBuilder vc, Matrix4f mat,
            float hx, float hy, float tx, float ty,
            float nx, float ny,
            float startHalfW, float endHalfW,
            float sr, float sg, float sb, float sa,
            float er, float eg, float eb, float ea) {

        // Split into 2 longitudinal quads along the spine so the center is full alpha
        // and the lateral outer edges feather smoothly to 0 alpha (no boxy edges!)
        // Left half (spine -> +normal)
        vc.addVertex(mat, hx, hy, 0.0f).setColor(sr, sg, sb, sa);
        vc.addVertex(mat, hx + nx * startHalfW, hy + ny * startHalfW, 0.0f).setColor(sr, sg, sb, 0.0f);
        vc.addVertex(mat, tx + nx * endHalfW, ty + ny * endHalfW, 0.0f).setColor(er, eg, eb, 0.0f);
        vc.addVertex(mat, tx, ty, 0.0f).setColor(er, eg, eb, ea);

        // Right half (-normal -> spine)
        vc.addVertex(mat, hx - nx * startHalfW, hy - ny * startHalfW, 0.0f).setColor(sr, sg, sb, 0.0f);
        vc.addVertex(mat, hx, hy, 0.0f).setColor(sr, sg, sb, sa);
        vc.addVertex(mat, tx, ty, 0.0f).setColor(er, eg, eb, ea);
        vc.addVertex(mat, tx - nx * endHalfW, ty - ny * endHalfW, 0.0f).setColor(er, eg, eb, 0.0f);
    }

    private static void draw2DStarSpike(
            BufferBuilder vc, Matrix4f mat,
            float cx, float cy, float angleRad,
            float length, float halfWidth,
            float cR, float cG, float cB, float cA,
            float tipR, float tipG, float tipB) {

        float dirX = Mth.cos(angleRad);
        float dirY = Mth.sin(angleRad);
        float perpX = -dirY;
        float perpY = dirX;

        float tip1X = cx + dirX * length;
        float tip1Y = cy + dirY * length;
        float tip2X = cx - dirX * length;
        float tip2Y = cy - dirY * length;

        float side1X = cx + perpX * halfWidth;
        float side1Y = cy + perpY * halfWidth;
        float side2X = cx - perpX * halfWidth;
        float side2Y = cy - perpY * halfWidth;

        // Positive arm
        vc.addVertex(mat, cx, cy, 0.0f).setColor(cR, cG, cB, cA);
        vc.addVertex(mat, side1X, side1Y, 0.0f).setColor(tipR, tipG, tipB, 0.0f);
        vc.addVertex(mat, tip1X, tip1Y, 0.0f).setColor(tipR, tipG, tipB, 0.0f);
        vc.addVertex(mat, cx, cy, 0.0f).setColor(cR, cG, cB, cA);

        vc.addVertex(mat, cx, cy, 0.0f).setColor(cR, cG, cB, cA);
        vc.addVertex(mat, tip1X, tip1Y, 0.0f).setColor(tipR, tipG, tipB, 0.0f);
        vc.addVertex(mat, side2X, side2Y, 0.0f).setColor(tipR, tipG, tipB, 0.0f);
        vc.addVertex(mat, cx, cy, 0.0f).setColor(cR, cG, cB, cA);

        // Negative arm
        vc.addVertex(mat, cx, cy, 0.0f).setColor(cR, cG, cB, cA);
        vc.addVertex(mat, side2X, side2Y, 0.0f).setColor(tipR, tipG, tipB, 0.0f);
        vc.addVertex(mat, tip2X, tip2Y, 0.0f).setColor(tipR, tipG, tipB, 0.0f);
        vc.addVertex(mat, cx, cy, 0.0f).setColor(cR, cG, cB, cA);

        vc.addVertex(mat, cx, cy, 0.0f).setColor(cR, cG, cB, cA);
        vc.addVertex(mat, tip2X, tip2Y, 0.0f).setColor(tipR, tipG, tipB, 0.0f);
        vc.addVertex(mat, side1X, side1Y, 0.0f).setColor(tipR, tipG, tipB, 0.0f);
        vc.addVertex(mat, cx, cy, 0.0f).setColor(cR, cG, cB, cA);
    }

    private static void draw2DRadialDisk(
            BufferBuilder vc, Matrix4f mat,
            float cx, float cy, float radius,
            float centerR, float centerG, float centerB, float centerA,
            float edgeR, float edgeG, float edgeB, float edgeA) {

        int segments = 24;
        for (int i = 0; i < segments; i++) {
            float a1 = (float) (2.0 * Math.PI * i / segments);
            float a2 = (float) (2.0 * Math.PI * (i + 1) / segments);

            float x1 = cx + Mth.cos(a1) * radius;
            float y1 = cy + Mth.sin(a1) * radius;
            float x2 = cx + Mth.cos(a2) * radius;
            float y2 = cy + Mth.sin(a2) * radius;

            vc.addVertex(mat, cx, cy, 0.0f).setColor(centerR, centerG, centerB, centerA);
            vc.addVertex(mat, x1, y1, 0.0f).setColor(edgeR, edgeG, edgeB, edgeA);
            vc.addVertex(mat, x2, y2, 0.0f).setColor(edgeR, edgeG, edgeB, edgeA);
            vc.addVertex(mat, cx, cy, 0.0f).setColor(centerR, centerG, centerB, centerA);
        }
    }
}
