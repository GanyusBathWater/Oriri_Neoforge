package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public class AoEIndicatorRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null)
            return;

        long gameTime = level.getGameTime();
        AoEIndicatorClientState.cleanExpired(gameTime);

        Iterable<AoEIndicatorClientState.Indicator> indicators = AoEIndicatorClientState.getActiveIndicators();
        if (!indicators.iterator().hasNext())
            return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        MultiBufferSource buffers = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        for (AoEIndicatorClientState.Indicator ind : indicators) {
            float progress = (float) (gameTime - ind.startTick) / ind.durationTicks;
            if (progress < 0)
                progress = 0;
            if (progress > 1)
                progress = 1;

            // Fade in and out
            float alphaMult = 1.0f;
            if (progress < 0.1f) {
                alphaMult = progress / 0.1f;
            } else if (progress > 0.9f) {
                alphaMult = 1.0f - ((progress - 0.9f) / 0.1f);
            }
            // Add a subtle pulsing effect
            float pulse = 0.8f + 0.2f * (float) Math.sin(progress * Math.PI * 10);
            alphaMult *= pulse;

            int argb = ind.argbColor;
            float r = ((argb >> 16) & 0xFF) / 255.0f;
            float g = ((argb >> 8) & 0xFF) / 255.0f;
            float b = (argb & 0xFF) / 255.0f;
            float baseAlpha = ((argb >> 24) & 0xFF) / 255.0f;
            float a = baseAlpha * alphaMult;

            if (a <= 0.05f)
                continue;

            // Pass 1: Draw all solid filled quads
            VertexConsumer filledBuffer = buffers.getBuffer(RenderType.debugFilledBox());
            PoseStack.Pose last = poseStack.last();
            var mat = last.pose();

            boolean firstQuad = true;
            float lastX = 0, lastY = 0, lastZ = 0;

            for (AABB aabb : ind.cachedAabbs) {
                float y = (float) (aabb.maxY + 0.02); // slight offset to prevent Z-fighting

                float swX = (float) aabb.minX;
                float swZ = (float) aabb.maxZ;
                float seX = (float) aabb.maxX;
                float seZ = (float) aabb.maxZ;
                float nwX = (float) aabb.minX;
                float nwZ = (float) aabb.minZ;
                float neX = (float) aabb.maxX;
                float neZ = (float) aabb.minZ;

                if (!firstQuad) {
                    // Insert degenerate triangles connecting the last quad to this one.
                    // Duplicate the last vertex of the previous quad, and the first vertex of this
                    // one.
                    filledBuffer.addVertex(mat, lastX, lastY, lastZ).setColor(r, g, b, a);
                    filledBuffer.addVertex(mat, swX, y, swZ).setColor(r, g, b, a);
                }

                // Quad drawn via TRIANGLE_STRIP (SW -> SE -> NW -> NE)
                filledBuffer.addVertex(mat, swX, y, swZ).setColor(r, g, b, a);
                filledBuffer.addVertex(mat, seX, y, seZ).setColor(r, g, b, a);
                filledBuffer.addVertex(mat, nwX, y, nwZ).setColor(r, g, b, a);
                filledBuffer.addVertex(mat, neX, y, neZ).setColor(r, g, b, a);

                lastX = neX;
                lastY = y;
                lastZ = neZ;
                firstQuad = false;
            }
        }

        poseStack.popPose();
    }

    private static void drawQuad(org.joml.Matrix4f mat, MultiBufferSource buffers, float r, float g, float b, float a,
            double minX, double y, double minZ, double maxX, double maxZ) {
        VertexConsumer vc = buffers.getBuffer(RenderType.debugFilledBox());
        vc.addVertex(mat, (float) minX, (float) y, (float) maxZ).setColor(r, g, b, a); // SW
        vc.addVertex(mat, (float) maxX, (float) y, (float) maxZ).setColor(r, g, b, a); // SE
        vc.addVertex(mat, (float) minX, (float) y, (float) minZ).setColor(r, g, b, a); // NW
        vc.addVertex(mat, (float) maxX, (float) y, (float) minZ).setColor(r, g, b, a); // NE
    }
}
