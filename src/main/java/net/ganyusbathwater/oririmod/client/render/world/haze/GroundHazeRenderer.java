package net.ganyusbathwater.oririmod.client.render.world.haze;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.client.render.ModRenderTypes;
import net.ganyusbathwater.oririmod.config.OririClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Renders Iris/Sodium-compatible volumetric ground fog for both natural biomes
 * (Scarlet Swamp, Elderwoods, Elder Beach, Vile Plains, Elder Valley) and manually placed
 * {@link net.ganyusbathwater.oririmod.block.custom.FogSourceBlock} emitters.
 * <p>
 * Uses overlapping, soft-edged radial-Gaussian FBM mist clouds anchored to {@link GroundHazeGrid}'s
 * heightfield and wall-bounded BFS patches. Because mist is rendered as soft overlapping volumetric
 * puffs rather than stitched horizontal planes:
 * <ul>
 *   <li>There are ZERO horizontal "sandwich" stripes on vertical walls or steps.</li>
 *   <li>There are ZERO polygon mesh seams, T-junction tears, or sharp corners anywhere.</li>
 *   <li>Mist naturally thickens with depth through corridors and valleys while hugging walls flush
 *       (and sizing down near 1-block-thick walls so it never leaks into adjacent rooms).</li>
 *   <li>Works natively with Vanilla, Sodium, and all Iris shaderpacks via
 *       {@code RENDERTYPE_ENTITY_TRANSLUCENT_SHADER}.</li>
 * </ul>
 */
@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class GroundHazeRenderer {

    private static final float INVALID_Y_CUTOFF = -90000.0F;
    private static final float SURFACE_NEAR_FADE_START = 0.45F;
    private static final float SURFACE_NEAR_FADE_END = 1.55F;
    private static final float LOCAL_NEAR_FADE_START = 0.30F;
    private static final float LOCAL_NEAR_FADE_END = 1.10F;

    private static final GroundHazeGrid GRID = new GroundHazeGrid();

    private GroundHazeRenderer() {
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        GRID.reset();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!OririClientConfig.isGroundHazeEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            return;
        }
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        GRID.tick(level, camPos.x, camPos.y, camPos.z, OririClientConfig.getGroundHazeRadius());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (!OririClientConfig.isGroundHazeEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        float configDensity = OririClientConfig.getGroundHazeDensity();
        if (configDensity <= 0.005F) {
            return;
        }

        boolean drawSurface = GRID.hasActiveSurfaceHaze();
        List<GroundHazeGrid.LocalFogPatch> localPatches = GRID.getActiveLocalPatches();
        boolean drawLocal = !localPatches.isEmpty();

        Player player = mc.player;
        Item fogItem = ModBlocks.FOG_SOURCE.asItem();
        boolean holdingFogTool = player != null
                && (player.getMainHandItem().is(fogItem) || player.getOffhandItem().is(fogItem));

        if (!drawSurface && !drawLocal) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float timeSeconds = (level.getGameTime() + partialTick) * 0.05F;

        // Screen-aligned billboard basis from camera rotation; expand vertical puff aspect smoothly
        // when looking straight down from above so top-down views look lush and circular
        Quaternionf camRot = camera.rotation();
        Vector3f camRight = new Vector3f(1.0F, 0.0F, 0.0F).rotate(camRot);
        Vector3f camUp = new Vector3f(0.0F, 1.0F, 0.0F).rotate(camRot);
        float pitchFactor = Mth.clamp(Math.abs(Mth.sin(camera.getXRot() * Mth.DEG_TO_RAD)), 0.0F, 1.0F);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        RenderType hazeType = ModRenderTypes.getGroundHaze(GroundHazeTexture.getTexture());

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        VertexConsumer vc = bufferSource.getBuffer(hazeType);
        PoseStack.Pose pose = poseStack.last();

        if (drawSurface) {
            float maxRadius = OririClientConfig.getGroundHazeRadius();
            renderSurfacePuffs(vc, pose, camPos, camRight, camUp, pitchFactor, timeSeconds, configDensity, maxRadius);
        }

        if (drawLocal) {
            for (int i = 0; i < localPatches.size(); i++) {
                renderLocalPuffs(vc, pose, localPatches.get(i), camPos, camRight, camUp, pitchFactor, timeSeconds, configDensity);
            }
        }

        bufferSource.endBatch(hazeType);

        if (holdingFogTool && drawLocal) {
            renderFogSourceMarkers(poseStack, bufferSource, localPatches);
        }

        poseStack.popPose();
    }

    private static void renderSurfacePuffs(
            VertexConsumer vc,
            PoseStack.Pose pose,
            Vec3 camPos,
            Vector3f camRight,
            Vector3f camUp,
            float pitchFactor,
            float timeSec,
            float densityMult,
            float maxRadius) {
        int size = GRID.getGridSize();
        if (size < 2) return;

        Matrix4f mat = pose.pose();
        int originX = GRID.getOriginWorldX();
        int originZ = GRID.getOriginWorldZ();
        float fadeInner = maxRadius * 0.56F;
        float fadeSpan = Math.max(8.0F, maxRadius - fadeInner);

        float[] sY = GRID.smoothY;
        float[] eFade = GRID.openEdgeFade;
        boolean[] nearWall = GRID.nearSurfaceWall;
        float[] sA = GRID.smoothAlpha;
        float[] sR = GRID.smoothR;
        float[] sG = GRID.smoothG;
        float[] sB = GRID.smoothB;
        int[] lights = GRID.packedLight;

        for (int gz = 0; gz < size; gz++) {
            int wz = originZ + gz * GroundHazeGrid.CELL_STEP;
            int row = gz * size;
            int cz = Math.floorDiv(wz, GroundHazeGrid.CELL_STEP);

            for (int gx = 0; gx < size; gx++) {
                int wx = originX + gx * GroundHazeGrid.CELL_STEP;
                int cx = Math.floorDiv(wx, GroundHazeGrid.CELL_STEP);

                int idx = row + gx;
                float groundY = sY[idx];
                if (groundY < INVALID_Y_CUTOFF) {
                    continue;
                }

                float biomeAlpha = sA[idx];
                float edgeFade = eFade[idx];
                if (biomeAlpha <= 0.004F || edgeFade <= 0.004F) {
                    continue;
                }

                // Deterministic world-locked jitter & organic phase so puffs stay anchored to terrain
                int h = hashInt(wx, wz);
                float jx = ((h & 255) / 255.0F - 0.5F) * 1.10F;
                float jz = (((h >>> 8) & 255) / 255.0F - 0.5F) * 1.10F;
                float phase = (((h >>> 16) & 255) / 255.0F) * Mth.TWO_PI;
                int tile = (h >>> 24) & 3;
                int flipBits = (h >>> 22) & 3;

                // Gentle drifting motion (bounded so puffs never wander off cliffs or through walls)
                float driftAmp = nearWall[idx] ? 0.22F : 0.60F;
                float px = wx + 0.5F + jx + Mth.sin(timeSec * 0.26F + phase) * driftAmp;
                float pz = wz + 0.5F + jz + Mth.cos(timeSec * 0.21F + phase * 1.3F) * driftAmp;

                float dx = (float) (px - camPos.x);
                float dz = (float) (pz - camPos.z);
                float horizDist = Mth.sqrt(dx * dx + dz * dz);
                if (horizDist > maxRadius) {
                    continue;
                }

                float outerFade = 1.0F - Mth.clamp((horizDist - fadeInner) / fadeSpan, 0.0F, 1.0F);
                outerFade = outerFade * outerFade * (3.0F - 2.0F * outerFade);
                if (outerFade <= 0.004F) {
                    continue;
                }

                float py1 = groundY + 0.72F + Mth.sin(timeSec * 0.35F + phase) * 0.08F;
                float dy = (float) (py1 - camPos.y);
                float vertFade = 1.0F - Mth.clamp((Math.abs(dy) - 30.0F) / 38.0F, 0.0F, 1.0F);
                if (vertFade <= 0.004F) {
                    continue;
                }

                float dist3D = Mth.sqrt(dx * dx + dy * dy + dz * dz);
                float nearFade = Mth.clamp((dist3D - SURFACE_NEAR_FADE_START) / (SURFACE_NEAR_FADE_END - SURFACE_NEAR_FADE_START), 0.0F, 1.0F);
                nearFade = nearFade * nearFade * (3.0F - 2.0F * nearFade);
                if (nearFade <= 0.004F) {
                    continue;
                }

                // Slow drifting mist-bank density modulation
                float bankMod = 0.78F + 0.22F * Mth.sin(px * 0.08F + timeSec * 0.14F) * Mth.cos(pz * 0.08F - timeSec * 0.11F);
                float baseAlpha = biomeAlpha * densityMult * outerFade * vertFade * nearFade * bankMod;

                float sizeScale = 0.90F + (((h >>> 26) & 63) / 63.0F) * 0.26F;
                float maxHalfW = nearWall[idx] ? 2.55F : 4.10F;
                float halfW = maxHalfW * sizeScale * (0.78F + 0.22F * edgeFade);
                float baseHalfH = (nearWall[idx] ? 1.18F : 1.54F) * sizeScale;
                float halfH = Mth.lerp(pitchFactor, baseHalfH, halfW * 0.84F);

                float r = sR[idx];
                float g = sG[idx];
                float b = sB[idx];
                int light = lights[idx];

                // Dense overlapping ground-hugging cloud on EVERY surface cell (no sparse dot gaps!)
                float a1 = Mth.clamp(baseAlpha * edgeFade * 0.24F, 0.0F, 0.42F);
                if (a1 > 0.004F) {
                    emitMistPuff(vc, mat, pose, px, py1, pz, camRight, camUp, halfW, halfH, tile, flipBits, r, g, b, a1, light);
                }

                // Softer elevated billowing wisp on checkerboard cells (keeps GPU cost low & mist rounded)
                if (((cx + cz) & 1) == 0) {
                    float edgeUpper = edgeFade * edgeFade;
                    float a2 = Mth.clamp(baseAlpha * edgeUpper * 0.17F, 0.0F, 0.30F);
                    if (a2 > 0.004F) {
                        float py2 = groundY + 1.32F + Mth.cos(timeSec * 0.30F + phase * 0.8F) * 0.12F;
                        float px2 = px + Mth.cos(timeSec * 0.18F + phase) * 0.45F;
                        float pz2 = pz - Mth.sin(timeSec * 0.22F + phase) * 0.45F;
                        emitMistPuff(vc, mat, pose, px2, py2, pz2, camRight, camUp,
                                halfW * 0.88F, halfH * 0.84F, (tile + 1) & 3, flipBits ^ 3, r, g, b, a2, light);
                    }
                }
            }
        }
    }

    private static void renderLocalPuffs(
            VertexConsumer vc,
            PoseStack.Pose pose,
            GroundHazeGrid.LocalFogPatch patch,
            Vec3 camPos,
            Vector3f camRight,
            Vector3f camUp,
            float pitchFactor,
            float timeSec,
            float densityMult) {
        int size = patch.gridSize;
        if (size < 2) return;

        Matrix4f mat = pose.pose();
        float[] sY = patch.smoothY;
        float[] mask = patch.alphaMask;
        float[] eFade = patch.openEdgeFade;
        byte[] wDist = patch.wallDist;
        int[] lights = patch.packedLight;
        float r = patch.color.r;
        float g = patch.color.g;
        float b = patch.color.b;

        for (int gz = 0; gz < size; gz++) {
            int wz = patch.originZ + gz * GroundHazeGrid.LOCAL_CELL_STEP;
            int row = gz * size;

            for (int gx = 0; gx < size; gx++) {
                int idx = row + gx;
                float groundY = sY[idx];
                if (groundY < INVALID_Y_CUTOFF) {
                    continue;
                }

                float maskAlpha = mask[idx];
                float edgeFade = eFade[idx];
                if (maskAlpha <= 0.004F || edgeFade <= 0.004F) {
                    continue;
                }

                int wx = patch.originX + gx * GroundHazeGrid.LOCAL_CELL_STEP;
                int h = hashInt(wx, wz);
                float phase = (((h >>> 16) & 255) / 255.0F) * Mth.TWO_PI;
                int tile = (h >>> 24) & 3;
                int flipBits = (h >>> 22) & 3;
                int wallDistance = wDist[idx];

                // Compact radius beside 1-block walls (<= 1.38 < 1.50 wall thickness -> flush hug, ZERO bleed through!),
                // broad overlapping clouds in room interiors so individual puffs fuse completely seamlessly
                float baseHalfW;
                float jitterAmp;
                float driftAmp;
                if (wallDistance <= 1) {
                    baseHalfW = 1.36F;
                    jitterAmp = 0.16F;
                    driftAmp = 0.08F;
                } else if (wallDistance == 2) {
                    baseHalfW = 1.88F;
                    jitterAmp = 0.28F;
                    driftAmp = 0.18F;
                } else {
                    baseHalfW = 2.25F;
                    jitterAmp = 0.36F;
                    driftAmp = 0.26F;
                }

                float jx = ((h & 255) / 255.0F - 0.5F) * jitterAmp;
                float jz = (((h >>> 8) & 255) / 255.0F - 0.5F) * jitterAmp;
                float px = wx + 0.5F + jx + Mth.sin(timeSec * 0.42F + phase) * driftAmp;
                float pz = wz + 0.5F + jz + Mth.cos(timeSec * 0.36F + phase * 1.25F) * driftAmp;
                float py1 = groundY + 0.50F + Mth.sin(timeSec * 0.48F + phase) * 0.06F;

                float dx = (float) (px - camPos.x);
                float dy = (float) (py1 - camPos.y);
                float dz = (float) (pz - camPos.z);
                float dist3D = Mth.sqrt(dx * dx + dy * dy + dz * dz);
                float nearFade = Mth.clamp((dist3D - LOCAL_NEAR_FADE_START) / (LOCAL_NEAR_FADE_END - LOCAL_NEAR_FADE_START), 0.0F, 1.0F);
                nearFade = nearFade * nearFade * (3.0F - 2.0F * nearFade);
                if (nearFade <= 0.004F) {
                    continue;
                }

                float sizeScale = 0.92F + (((h >>> 26) & 63) / 63.0F) * 0.18F;
                float halfW = baseHalfW * sizeScale * (0.80F + 0.20F * edgeFade);
                float baseHalfH = (wallDistance <= 1 ? 0.72F : 0.88F) * sizeScale;
                float halfH = Mth.lerp(pitchFactor, baseHalfH, halfW * 0.86F);

                float baseAlpha = maskAlpha * densityMult * nearFade;
                int light = lights[idx];

                // Every visited floor cell gets a main cloud (eliminates dotted rows along walls & thin spots!)
                float a1 = Mth.clamp(baseAlpha * edgeFade * 0.22F, 0.0F, 0.40F);
                if (a1 > 0.004F) {
                    emitMistPuff(vc, mat, pose, px, py1, pz, camRight, camUp, halfW, halfH, tile, flipBits, r, g, b, a1, light);
                }

                // Upper drifting veil on checkerboard cells AND along walls/low-density areas for full smooth blend
                if (((wx + wz) & 1) == 0 || wallDistance <= 1 || maskAlpha < 0.50F) {
                    float edgeUpper = edgeFade * edgeFade;
                    float a2 = Mth.clamp(baseAlpha * edgeUpper * 0.15F, 0.0F, 0.28F);
                    if (a2 > 0.004F) {
                        float py2 = groundY + 0.94F + Mth.cos(timeSec * 0.38F + phase) * 0.08F;
                        float px2 = px + Mth.cos(timeSec * 0.31F + phase) * (driftAmp * 0.8F);
                        float pz2 = pz - Mth.sin(timeSec * 0.29F + phase) * (driftAmp * 0.8F);
                        emitMistPuff(vc, mat, pose, px2, py2, pz2, camRight, camUp,
                                halfW * 0.88F, halfH * 0.84F, (tile + 2) & 3, flipBits ^ 2, r, g, b, a2, light);
                    }
                }
            }
        }
    }

    private static void emitMistPuff(
            VertexConsumer vc,
            Matrix4f mat,
            PoseStack.Pose pose,
            float cx,
            float cy,
            float cz,
            Vector3f right,
            Vector3f up,
            float halfW,
            float halfH,
            int tileIndex,
            int flipBits,
            float r,
            float g,
            float b,
            float a,
            int light) {
        float rx = right.x * halfW;
        float ry = right.y * halfW;
        float rz = right.z * halfW;

        float ux = up.x * halfH;
        float uy = up.y * halfH;
        float uz = up.z * halfH;

        float uMin = (tileIndex & 1) * 0.5F;
        float vMin = ((tileIndex >>> 1) & 1) * 0.5F;
        float uMax = uMin + 0.5F;
        float vMax = vMin + 0.5F;

        boolean flipU = (flipBits & 1) != 0;
        boolean flipV = (flipBits & 2) != 0;
        float u0 = flipU ? uMax : uMin;
        float u1 = flipU ? uMin : uMax;
        float v0 = flipV ? vMax : vMin;
        float v1 = flipV ? vMin : vMax;

        // Upward normal gives soft, consistent ambient & sky lighting across Vanilla and Iris shaderpacks
        vc.addVertex(mat, cx - rx - ux, cy - ry - uy, cz - rz - uz)
                .setColor(r, g, b, a)
                .setUv(u0, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        vc.addVertex(mat, cx - rx + ux, cy - ry + uy, cz - rz + uz)
                .setColor(r, g, b, a)
                .setUv(u0, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        vc.addVertex(mat, cx + rx + ux, cy + ry + uy, cz + rz + uz)
                .setColor(r, g, b, a)
                .setUv(u1, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        vc.addVertex(mat, cx + rx - ux, cy + ry - uy, cz + rz - uz)
                .setColor(r, g, b, a)
                .setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static int hashInt(int x, int z) {
        int h = x * 374761393 + z * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private static void renderFogSourceMarkers(
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            List<GroundHazeGrid.LocalFogPatch> patches) {
        VertexConsumer lineVc = bufferSource.getBuffer(RenderType.lines());
        for (int i = 0; i < patches.size(); i++) {
            GroundHazeGrid.LocalFogPatch p = patches.get(i);
            BlockPos pos = p.sourcePos;
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineVc,
                    pos.getX() + 0.15, pos.getY() + 0.15, pos.getZ() + 0.15,
                    pos.getX() + 0.85, pos.getY() + 0.85, pos.getZ() + 0.85,
                    p.color.r, p.color.g, p.color.b, 0.90F);
        }
        bufferSource.endBatch(RenderType.lines());
    }
}
