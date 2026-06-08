package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
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
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public class CircuitryRenderSystem {

    private static final float TRACE_WIDTH = 0.012f;
    private static final RenderType LINE_LAYER = RenderType.entityTranslucent(
            ResourceLocation.withDefaultNamespace("textures/misc/white.png"));

    public static final List<CircuitryInstance> ACTIVE_EFFECTS = new ArrayList<>();

    public static class CircuitryInstance {
        public static final int DRAW_TICKS   = 6;
        public static final int LINGER_TICKS = 20;
        public static final int TOTAL_LIFE   = DRAW_TICKS + LINGER_TICKS + 15;

        public final Vec3[] nodes;
        public final float r, g, b;
        public int age = 0;

        public CircuitryInstance(Vec3[] nodes, float r, float g, float b) {
            this.nodes = nodes;
            this.r = r; this.g = g; this.b = b;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) return;
        Iterator<CircuitryInstance> it = ACTIVE_EFFECTS.iterator();
        while (it.hasNext()) {
            CircuitryInstance inst = it.next();
            inst.age++;
            if (inst.age >= CircuitryInstance.TOTAL_LIFE) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (ACTIVE_EFFECTS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(mc.isPaused());
        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource buffers = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        // Shift drawing coordinates so world space (0,0,0) becomes correct relative to camera
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f posM       = pose.pose();
        VertexConsumer vc   = buffers.getBuffer(LINE_LAYER);

        for (CircuitryInstance inst : ACTIVE_EFFECTS) {
            Vec3[] nodes = inst.nodes;
            if (nodes.length < 2) continue;

            float agef = inst.age + pt;
            float alpha = computeAlpha(agef);
            if (alpha <= 0.001f) continue;

            int numSegments = nodes.length - 1;
            float drawProgress = computeDrawProgress(agef, numSegments);

            for (int i = 0; i < numSegments; i++) {
                float segProgress = net.minecraft.util.Mth.clamp(drawProgress - i, 0f, 1f);
                if (segProgress <= 0f) break;

                Vec3 nodeA = nodes[i];
                Vec3 nodeB = nodes[i + 1];

                float ax = (float) nodeA.x;
                float ay = (float) nodeA.y;
                float az = (float) nodeA.z;

                float bx = ax + (float)((nodeB.x - ax) * segProgress);
                float by = ay + (float)((nodeB.y - ay) * segProgress);
                float bz = az + (float)((nodeB.z - az) * segProgress);

                // dirVec = normalised (B - A)
                float dX = bx - ax, dY = by - ay, dZ = bz - az;
                float dLen = (float) Math.sqrt(dX*dX + dY*dY + dZ*dZ);
                if (dLen < 1e-5f) continue;
                dX /= dLen; dY /= dLen; dZ /= dLen;

                // camVec = normalised (cam - midpoint)
                float midX = (float)((nodeA.x + nodeB.x) * 0.5 - cam.x);
                float midY = (float)((nodeA.y + nodeB.y) * 0.5 - cam.y);
                float midZ = (float)((nodeA.z + nodeB.z) * 0.5 - cam.z);
                float camLen = (float) Math.sqrt(midX*midX + midY*midY + midZ*midZ);
                if (camLen > 1e-5f) { midX /= camLen; midY /= camLen; midZ /= camLen; }
                else { midX = 0; midY = 1; midZ = 0; }

                // right = dir × camVec
                float rX = dY * midZ - dZ * midY;
                float rY = dZ * midX - dX * midZ;
                float rZ = dX * midY - dY * midX;
                float rLen = (float) Math.sqrt(rX*rX + rY*rY + rZ*rZ);
                if (rLen < 1e-5f) { rX = 1; rY = 0; rZ = 0; }
                else { rX /= rLen; rY /= rLen; rZ /= rLen; }

                // Draw layers
                drawQuad(vc, posM, pose, ax, ay, az, bx, by, bz, rX, rY, rZ, TRACE_WIDTH * 3.5f, inst.r, inst.g, inst.b, 0.10f * alpha);
                drawQuad(vc, posM, pose, ax, ay, az, bx, by, bz, rX, rY, rZ, TRACE_WIDTH * 2.0f, inst.r, inst.g, inst.b, 0.25f * alpha);
                drawQuad(vc, posM, pose, ax, ay, az, bx, by, bz, rX, rY, rZ, TRACE_WIDTH, inst.r, inst.g, inst.b, alpha);
                drawQuad(vc, posM, pose, ax, ay, az, bx, by, bz, rX, rY, rZ, TRACE_WIDTH * 0.2f, 1f, 1f, 1f, alpha);
            }
        }

        poseStack.popPose();
    }

    private static float computeDrawProgress(float agef, int numSegments) {
        if (agef >= CircuitryInstance.DRAW_TICKS) return numSegments;
        return (agef / CircuitryInstance.DRAW_TICKS) * numSegments;
    }

    private static float computeAlpha(float agef) {
        int lingerEnd = CircuitryInstance.DRAW_TICKS + CircuitryInstance.LINGER_TICKS;
        int fadeStart = lingerEnd;
        int totalLife = CircuitryInstance.TOTAL_LIFE;
        if (agef < fadeStart) return 1.0f;
        float fadeLen = totalLife - fadeStart;
        if (fadeLen <= 0) return 0f;
        return Math.max(0f, 1f - (agef - fadeStart) / fadeLen);
    }

    private static void drawQuad(
            VertexConsumer vc, Matrix4f posM, PoseStack.Pose pose,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float rX, float rY, float rZ,
            float halfW,
            float r, float g, float b, float a) {

        int light   = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;

        vc.addVertex(posM, ax - rX*halfW, ay - rY*halfW, az - rZ*halfW)
          .setColor(r, g, b, a).setUv(0f, 0f).setOverlay(overlay).setLight(light)
          .setNormal(pose, rX, rY, rZ);
        vc.addVertex(posM, ax + rX*halfW, ay + rY*halfW, az + rZ*halfW)
          .setColor(r, g, b, a).setUv(1f, 0f).setOverlay(overlay).setLight(light)
          .setNormal(pose, rX, rY, rZ);
        vc.addVertex(posM, bx + rX*halfW, by + rY*halfW, bz + rZ*halfW)
          .setColor(r, g, b, a).setUv(1f, 1f).setOverlay(overlay).setLight(light)
          .setNormal(pose, rX, rY, rZ);
        vc.addVertex(posM, bx - rX*halfW, by - rY*halfW, bz - rZ*halfW)
          .setColor(r, g, b, a).setUv(0f, 1f).setOverlay(overlay).setLight(light)
          .setNormal(pose, rX, rY, rZ);
    }
}
