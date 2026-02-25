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

            BlockPos center = ind.center;
            int rad = (int) Math.ceil(ind.radius);
            int radSq = rad * rad;

            // Scan blocks in radius
            for (int x = -rad; x <= rad; x++) {
                for (int z = -rad; z <= rad; z++) {
                    if (x * x + z * z > radSq)
                        continue;

                    BlockPos colPos = center.offset(x, 0, z);

                    // Find highest solid block surface within +/- 4 blocks Y
                    BlockPos surfacePos = null;
                    VoxelShape surfaceShape = null;

                    for (int y = 4; y >= -4; y--) {
                        BlockPos checkPos = colPos.offset(0, y, 0);
                        BlockState state = level.getBlockState(checkPos);
                        VoxelShape shape = state.getShape(level, checkPos);
                        if (!shape.isEmpty()) {
                            surfacePos = checkPos;
                            surfaceShape = shape;
                            break;
                        }
                    }

                    if (surfacePos != null && surfaceShape != null) {
                        // Render a colored quad exactly on top of the shape
                        AABB aabb = surfaceShape.bounds().move(surfacePos);

                        // We will just draw a flat quad slightly above the AABB max Y
                        double quadY = aabb.maxY + 0.02; // slight offset to prevent Z-fighting

                        PoseStack.Pose last = poseStack.last();
                        var mat = last.pose();

                        drawQuad(mat, buffers, r, g, b, a,
                                aabb.minX, quadY, aabb.minZ,
                                aabb.maxX, aabb.maxZ);

                        // Also draw a box perimeter outline for better visibility
                        LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()),
                                aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ,
                                r, g, b, Math.min(1.0f, a * 2.0f));
                    }
                }
            }
        }

        poseStack.popPose();
    }

    private static void drawQuad(org.joml.Matrix4f mat, MultiBufferSource buffers, float r, float g, float b, float a,
            double minX, double y, double minZ, double maxX, double maxZ) {
        VertexConsumer vc = buffers.getBuffer(RenderType.debugFilledBox());
        vc.addVertex(mat, (float) minX, (float) y, (float) minZ).setColor(r, g, b, a);
        vc.addVertex(mat, (float) minX, (float) y, (float) maxZ).setColor(r, g, b, a);
        vc.addVertex(mat, (float) maxX, (float) y, (float) maxZ).setColor(r, g, b, a);
        vc.addVertex(mat, (float) maxX, (float) y, (float) minZ).setColor(r, g, b, a);
    }
}
