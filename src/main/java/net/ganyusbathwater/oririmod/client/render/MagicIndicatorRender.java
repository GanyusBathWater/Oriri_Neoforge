package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.util.MagicIndicatorClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class MagicIndicatorRender {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long gameTime = mc.level.getGameTime();
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(mc.isPaused());
        PoseStack pose = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        MultiBufferSource buffers = mc.renderBuffers().bufferSource();

        Iterator<Map.Entry<Integer, MagicIndicatorClientState.Indicator>> it =
                MagicIndicatorClientState.INSTANCE.getActive().entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<Integer, MagicIndicatorClientState.Indicator> e = it.next();
            Entity ent = mc.level.getEntity(e.getKey());
            if (!(ent instanceof LivingEntity living) || !living.isAlive()) {
                it.remove();
                continue;
            }

            MagicIndicatorClientState.Indicator ind = e.getValue();

            float progress = ind.progress(gameTime, pt);
            if (ind.durationTicks() > 0 && progress >= 1f) {
                it.remove();
                continue;
            }

            float baseAlpha = 1f;
            if (ind.durationTicks() > 0) {
                float t = progress;
                baseAlpha = (float) (Math.sin(Math.min(1f, t) * Math.PI));
            }

            Vec3 eye = living.getEyePosition(pt);
            Vec3 forward = living.getViewVector(pt).normalize();

            Vec3 worldUp = Math.abs(forward.dot(new Vec3(0, 1, 0))) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 right = forward.cross(worldUp).normalize();
            Vec3 up = right.cross(forward).normalize();

            pose.pushPose();
            pose.translate(-camPos.x, -camPos.y, -camPos.z);

            List<MagicIndicatorClientState.Indicator.Layer> layers =
                    (ind.layers() != null && !ind.layers().isEmpty())
                            ? ind.layers()
                            : List.of(new MagicIndicatorClientState.Indicator.Layer(
                            ind.texture(), ind.radius(), ind.spinDegPerTick(), ind.argbColor(), 0f
                    ));

            for (MagicIndicatorClientState.Indicator.Layer layer : layers) {
                ResourceLocation tex = layer.texture() != null
                        ? layer.texture()
                        : (ind.texture() != null ? ind.texture() : MagicIndicatorClientState.DEFAULT_TEX);

                // Fail‑safe: Layer überspringen, wenn Textur fehlt
                if (!textureExists(tex)) continue;

                float spinPerTick = layer.spinDegPerTick() != 0f ? layer.spinDegPerTick() : ind.spinDegPerTick();
                float baseRadius = layer.radius() > 0f ? layer.radius() : ind.radius();
                int lc = layer.argbColor() != 0 ? layer.argbColor() : ind.argbColor();

                float dist = Math.max(0.0f, ind.distanceForward() + Math.max(0f, layer.extraDistanceForward()));
                Vec3 center = eye.add(forward.scale(dist));

                float angleRad = (float) Math.toRadians(spinPerTick * (gameTime + pt - ind.startGameTime()));
                double sin = Math.sin(angleRad);
                double cos = Math.cos(angleRad);
                Vec3 rightR = new Vec3(right.x * cos + up.x * -sin, right.y * cos + up.y * -sin, right.z * cos + up.z * -sin);
                Vec3 upR = new Vec3(right.x * sin + up.x * cos, right.y * sin + up.y * cos, right.z * sin + up.z * cos);

                float rads = Math.max(0.01f, baseRadius);

                float alpha = baseAlpha * (((lc >>> 24) & 0xFF) / 255f);
                if (alpha <= 0f) continue;
                int a = (int)(Math.max(0f, Math.min(1f, alpha)) * 255f) & 0xFF;
                int r = (lc >>> 16) & 0xFF;
                int g = (lc >>> 8) & 0xFF;
                int b = lc & 0xFF;

                Vec3 c0 = center.add(rightR.scale(-rads)).add(upR.scale(-rads));
                Vec3 c1 = center.add(rightR.scale(-rads)).add(upR.scale( rads));
                Vec3 c2 = center.add(rightR.scale( rads)).add(upR.scale( rads));
                Vec3 c3 = center.add(rightR.scale( rads)).add(upR.scale(-rads));

                drawQuad(pose, buffers, tex, r, g, b, a, c0, c1, c2, c3);
            }

            pose.popPose();
        }
    }

    private static boolean textureExists(ResourceLocation tex) {
        if (tex == null) return false;
        return Minecraft.getInstance().getResourceManager().getResource(tex).isPresent();
    }

    private static void drawQuad(PoseStack pose, MultiBufferSource buffers, ResourceLocation tex,
                                 int r, int g, int b, int a,
                                 Vec3 c0, Vec3 c1, Vec3 c2, Vec3 c3) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(tex));
        PoseStack.Pose last = pose.last();
        var poseMat = last.pose();

        Vec3 u = c2.subtract(c0);
        Vec3 v = c1.subtract(c0);
        Vec3 n = u.cross(v).normalize();
        float nx = (float) n.x;
        float ny = (float) n.y;
        float nz = (float) n.z;

        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;

        vc.addVertex(poseMat, (float) c0.x, (float) c0.y, (float) c0.z)
                .setColor(r, g, b, a).setUv(0f, 0f).setOverlay(overlay).setLight(light)
                .setNormal(last, nx, ny, nz);
        vc.addVertex(poseMat, (float) c1.x, (float) c1.y, (float) c1.z)
                .setColor(r, g, b, a).setUv(0f, 1f).setOverlay(overlay).setLight(light)
                .setNormal(last, nx, ny, nz);
        vc.addVertex(poseMat, (float) c2.x, (float) c2.y, (float) c2.z)
                .setColor(r, g, b, a).setUv(1f, 1f).setOverlay(overlay).setLight(light)
                .setNormal(last, nx, ny, nz);
        vc.addVertex(poseMat, (float) c3.x, (float) c3.y, (float) c3.z)
                .setColor(r, g, b, a).setUv(1f, 0f).setOverlay(overlay).setLight(light)
                .setNormal(last, nx, ny, nz);
    }
}