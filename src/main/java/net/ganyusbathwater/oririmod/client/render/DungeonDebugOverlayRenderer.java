package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.network.packet.SyncDungeonDebugZonesPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class DungeonDebugOverlayRenderer {

    private static boolean enabled = false;
    private static List<SyncDungeonDebugZonesPayload.DebugZoneEntry> activeZones = new ArrayList<>();

    private DungeonDebugOverlayRenderer() {}

    public static void updateZones(SyncDungeonDebugZonesPayload payload) {
        enabled = payload.enabled();
        activeZones = new ArrayList<>(payload.zones());
    }

    public static void clear() {
        enabled = false;
        activeZones.clear();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (!enabled || activeZones.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (!mc.level.dimension().location().getPath().startsWith("dungeon_")) {
            clear();
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        VertexConsumer lineBuffer = buffers.getBuffer(RenderType.lines());
        for (SyncDungeonDebugZonesPayload.DebugZoneEntry zone : activeZones) {
            int argb = zone.argbColor();
            float a = ((argb >> 24) & 0xFF) / 255.0f;
            float r = ((argb >> 16) & 0xFF) / 255.0f;
            float g = ((argb >> 8) & 0xFF) / 255.0f;
            float b = (argb & 0xFF) / 255.0f;

            LevelRenderer.renderLineBox(
                    poseStack, lineBuffer,
                    zone.minX(), zone.minY(), zone.minZ(),
                    zone.maxX(), zone.maxY(), zone.maxZ(),
                    r, g, b, a
            );
        }
        buffers.endBatch(RenderType.lines());

        // Render floating labels for nearby zones (within 64 blocks)
        Font font = mc.font;
        for (SyncDungeonDebugZonesPayload.DebugZoneEntry zone : activeZones) {
            if (zone.label() == null || zone.label().isBlank()) continue;
            double cx = (zone.minX() + zone.maxX()) * 0.5;
            double cy = zone.maxY() + 0.35;
            double cz = (zone.minZ() + zone.maxZ()) * 0.5;

            if (camPos.distanceToSqr(cx, cy, cz) > 64.0 * 64.0) continue;

            poseStack.pushPose();
            poseStack.translate(cx, cy, cz);
            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(0.025f, -0.025f, 0.025f);

            Matrix4f matrix4f = poseStack.last().pose();
            float xOffset = -font.width(zone.label()) / 2.0f;
            font.drawInBatch(
                    zone.label(), xOffset, 0,
                    zone.argbColor(), false,
                    matrix4f, buffers,
                    Font.DisplayMode.SEE_THROUGH,
                    0x66000000, LightTexture.FULL_BRIGHT
            );
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}

