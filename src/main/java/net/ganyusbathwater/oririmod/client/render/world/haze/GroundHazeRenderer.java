package net.ganyusbathwater.oririmod.client.render.world.haze;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.config.OririClientConfig;
import net.ganyusbathwater.oririmod.util.ShaderDetector;
import net.ganyusbathwater.oririmod.world.fog.ClientFogAnchorCache;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Renders unified GPU heightfield volumetric ground fog for both natural biomes
 * (Scarlet Swamp, Elderwoods, Elder Beach, Vile Plains, Elder Valley) and manually placed
 * {@link net.ganyusbathwater.oririmod.block.custom.FogSourceBlock} / chunk-attached structure emitters.
 * <p>
 * Architecture:
 * <ul>
 *   <li>Runs immediately after {@code LevelRenderer.renderLevel} returns via {@code GameRendererMixin}
 *       (strictly AFTER Iris's {@code finalizeLevelRendering()}, so Iris never overrides our shader
 *       and {@code F1} GUI toggle behaves identically across all shaderpacks).</li>
 *   <li>Packs both surface biome haze and deduplicated local dungeon fog (289x289 blocks, ~144-block
 *       radius) into two tiny GPU textures ({@code RGBA16F} geometry/density + {@code RGBA8} lit color)
 *       only when geometry, patches, or lighting actually change.</li>
 *   <li>Executes ONE single 4-vertex fullscreen volumetric raymarch pass against a format-matched
 *       3D scene depth copy — working identically whether standing inside the fog, crouching, looking
 *       horizontally through a doorway from outside, or viewing a 140-block crater dungeon from afar,
 *       with zero circles, zero horizon beams, zero wall sandwich stripes, and 1x GPU overdraw.</li>
 * </ul>
 */
@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class GroundHazeRenderer {

    public static final ResourceLocation GROUND_HAZE_SHADER_LOC =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "ground_haze");

    private static final float INVALID_Y_CUTOFF = GroundHazeGrid.INVALID_Y_CUTOFF;
    private static final int MAP_SIZE = GroundHazeGrid.COMP_SIZE; // 289 (144-block radius)
    private static final int MAP_CELLS = MAP_SIZE * MAP_SIZE;
    private static final float LOCAL_FAR_FADE_START = 122.0F;
    private static final float LOCAL_FAR_FADE_END = 142.0F;

    private static final GroundHazeGrid GRID = new GroundHazeGrid();

    private static volatile ShaderInstance groundHazeShader = null;

    // Captured from AFTER_LEVEL each frame and consumed right after LevelRenderer.renderLevel returns
    private static final Matrix4f LAST_MODEL_VIEW = new Matrix4f();
    private static final Matrix4f LAST_PROJECTION = new Matrix4f();
    private static final Matrix4f INV_PROJECTION = new Matrix4f();
    private static final Matrix4f INV_VIEW = new Matrix4f();
    private static final Matrix4f IDENTITY_MAT = new Matrix4f();
    private static boolean hasValidMatrices = false;

    // Unified 289x289 GPU heightfield textures (Sampler2 = RGBA16F geo/density, Sampler3 = RGBA8 sRGB color)
    private static int geoTexId = 0;
    private static int colTexId = 0;
    private static FloatBuffer geoUploadBuf = null;
    private static ByteBuffer colUploadBuf = null;
    private static boolean heightfieldHasData = false;
    private static int mapOriginX = 0;
    private static int mapOriginZ = 0;
    private static float mapBaseY = 0.0F;
    private static float mapMinFogY = 0.0F;
    private static float mapMaxFogTopY = 0.0F;
    private static int uploadedSurfaceVersion = -1;
    private static int uploadedLocalVersion = -1;
    private static int uploadedNightBucket = Integer.MIN_VALUE;
    private static boolean uploadedShaderActive = false;

    // Reusable CPU staging arrays for zero-allocation heightfield merging & 1-texel neighbor dilation
    private static float[] tmpFloorY;
    private static float[] tmpFogH;
    private static float[] tmpDen;
    private static float[] tmpMask;
    private static float[] tmpR;
    private static float[] tmpG;
    private static float[] tmpB;
    private static boolean[] tmpValid;

    private static volatile boolean pendingGpuReset = false;

    // Format-matched OpenGL depth blit target (safe sampling without FBO feedback loop)
    private static int depthCopyFbo = 0;
    private static int depthCopyTex = 0;
    private static int depthCopyW = 0;
    private static int depthCopyH = 0;
    private static int depthCopySrcId = 0;
    private static boolean depthCopyValid = false;

    private GroundHazeRenderer() {
    }

    public static void onShaderRegistered(ShaderInstance shader) {
        groundHazeShader = shader;
        OririMod.LOGGER.info("[OririMod] ground_haze volumetric heightfield shader registered successfully.");
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        GRID.reset();
        ClientFogAnchorCache.clear();
        hasValidMatrices = false;
        pendingGpuReset = true;
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
        if (!OririClientConfig.isGroundHazeEnabled()) {
            return;
        }

        RenderLevelStageEvent.Stage stage = event.getStage();

        // Creative Fog Source marker wireframes stay in the normal translucent world stage
        if (stage == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            Item fogItem = ModBlocks.FOG_SOURCE.asItem();
            if (player != null && (player.getMainHandItem().is(fogItem) || player.getOffhandItem().is(fogItem))) {
                List<GroundHazeGrid.LocalFogPatch> patches = GRID.getActiveLocalPatches();
                if (!patches.isEmpty()) {
                    PoseStack poseStack = event.getPoseStack();
                    Vec3 camPos = event.getCamera().getPosition();
                    MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
                    poseStack.pushPose();
                    poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
                    renderFogSourceMarkers(poseStack, bufferSource, patches);
                    poseStack.popPose();
                }
            }
            return;
        }

        if (stage == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            LAST_MODEL_VIEW.set(event.getModelViewMatrix());
            LAST_PROJECTION.set(event.getProjectionMatrix());
            hasValidMatrices = true;
        }
    }

    /**
     * Called by {@link net.ganyusbathwater.oririmod.mixin.GameRendererMixin} immediately after
     * {@code LevelRenderer.renderLevel(...)} returns — strictly AFTER Iris's {@code finalizeLevelRendering()}
     * and BEFORE GUI depth clear.
     */
    public static void renderPostWorld(DeltaTracker deltaTracker) {
        if (pendingGpuReset) {
            pendingGpuReset = false;
            releaseGpuResources();
        }

        if (!hasValidMatrices || !OririClientConfig.isGroundHazeEnabled()) {
            return;
        }
        hasValidMatrices = false;

        ShaderInstance shader = groundHazeShader;
        if (shader == null) {
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

        boolean hasSurface = GRID.hasActiveSurfaceHaze();
        GroundHazeGrid.LocalCompositeGrid localComp = GRID.getLocalCompositeGrid();
        boolean hasLocal = localComp.hasData;
        if (!hasSurface && !hasLocal) {
            heightfieldHasData = false;
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        float nightFactor = GRID.getNightFactor();
        int nightBucket = Math.round(nightFactor * 40.0F);
        boolean shaderActive = ShaderDetector.isShaderActive();
        int sVer = GRID.getSurfaceMeshVersion();
        int lVer = GRID.getLocalMeshVersion();

        // Rebuild & upload the 289x289 GPU heightfield textures ONLY when terrain/patches/lighting change
        if (geoTexId == 0 || colTexId == 0
                || sVer != uploadedSurfaceVersion
                || lVer != uploadedLocalVersion
                || nightBucket != uploadedNightBucket
                || shaderActive != uploadedShaderActive) {
            rebuildAndUploadHeightfield(hasSurface, localComp, camPos, nightFactor, shaderActive,
                    OririClientConfig.getGroundHazeRadius());
            uploadedSurfaceVersion = sVer;
            uploadedLocalVersion = lVer;
            uploadedNightBucket = nightBucket;
            uploadedShaderActive = shaderActive;
        }

        if (!heightfieldHasData) {
            return;
        }

        // Blit 3D scene depth buffer into our format-matched read texture (0 feedback loops)
        RenderTarget mainTarget = mc.getMainRenderTarget();
        if (!copyMainDepthBuffer(mainTarget)) {
            return;
        }

        // Re-bind mainRenderTarget for color output after depth blit
        mainTarget.bindWrite(false);

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        float timeSeconds = (level.getGameTime() + partialTick) * 0.05F;

        ResourceLocation noiseTex = GroundHazeTexture.getTexture();
        GroundHazeTexture.ensureRepeatWrap();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Mth.clamp(configDensity, 0.0F, 1.5F));
        RenderSystem.setShaderTexture(0, noiseTex);
        RenderSystem.setShaderTexture(1, depthCopyTex);
        RenderSystem.setShaderTexture(2, geoTexId);
        RenderSystem.setShaderTexture(3, colTexId);

        Uniform uInvProjMat = shader.getUniform("InvProjMat");
        Uniform uInvViewMat = shader.getUniform("InvViewMat");
        Uniform uDepthBufSize = shader.getUniform("DepthBufferSize");
        Uniform uCamWorldPos = shader.getUniform("CamWorldPos");
        Uniform uMapRelOrigin = shader.getUniform("MapRelOrigin");
        Uniform uMapSize = shader.getUniform("MapSize");
        Uniform uFogYBounds = shader.getUniform("FogYBounds");
        Uniform uFogTime = shader.getUniform("FogTime");

        if (uInvProjMat != null) {
            INV_PROJECTION.set(LAST_PROJECTION).invert();
            uInvProjMat.set(INV_PROJECTION);
        }
        if (uInvViewMat != null) {
            INV_VIEW.set(LAST_MODEL_VIEW).invert();
            uInvViewMat.set(INV_VIEW);
        }
        if (uDepthBufSize != null) {
            uDepthBufSize.set((float) mainTarget.width, (float) mainTarget.height);
        }
        if (uCamWorldPos != null) {
            float wxWrap = (float) (camPos.x - Math.floor(camPos.x / 256.0) * 256.0);
            float wzWrap = (float) (camPos.z - Math.floor(camPos.z / 256.0) * 256.0);
            uCamWorldPos.set(wxWrap, (float) camPos.y, wzWrap);
        }
        if (uMapRelOrigin != null) {
            uMapRelOrigin.set(
                    (float) (mapOriginX - camPos.x),
                    (float) (mapBaseY - camPos.y),
                    (float) (mapOriginZ - camPos.z));
        }
        if (uMapSize != null) {
            uMapSize.set((float) MAP_SIZE);
        }
        if (uFogYBounds != null) {
            // Slight 0.45-block bottom padding matches the shader's slab/stair tolerance
            uFogYBounds.set(
                    (float) (mapMinFogY - 0.45 - camPos.y),
                    (float) (mapMaxFogTopY + 0.15 - camPos.y));
        }
        if (uFogTime != null) {
            uFogTime.set(timeSeconds);
        }

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(IDENTITY_MAT, VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(() -> shader);

        try {
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bb = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            bb.addVertex(-1.0F, -1.0F, 0.0F).setUv(0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F);
            bb.addVertex( 1.0F, -1.0F, 0.0F).setUv(1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F);
            bb.addVertex( 1.0F,  1.0F, 0.0F).setUv(1.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F);
            bb.addVertex(-1.0F,  1.0F, 0.0F).setUv(0.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F);
            MeshData quad = bb.build();
            if (quad != null) {
                BufferUploader.drawWithShader(quad);
            }
        } finally {
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.setShaderTexture(1, 0);
            RenderSystem.setShaderTexture(2, 0);
            RenderSystem.setShaderTexture(3, 0);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Unified 289x289 GPU Heightfield Texture Builder & Uploader
    // ─────────────────────────────────────────────────────────────────────────────

    private static void ensureStagingArrays() {
        if (tmpValid != null) return;
        tmpFloorY = new float[MAP_CELLS];
        tmpFogH = new float[MAP_CELLS];
        tmpDen = new float[MAP_CELLS];
        tmpMask = new float[MAP_CELLS];
        tmpR = new float[MAP_CELLS];
        tmpG = new float[MAP_CELLS];
        tmpB = new float[MAP_CELLS];
        tmpValid = new boolean[MAP_CELLS];
        geoUploadBuf = BufferUtils.createFloatBuffer(MAP_CELLS * 4);
        colUploadBuf = BufferUtils.createByteBuffer(MAP_CELLS * 4);
    }

    private static void ensureHeightfieldTextures() {
        if (geoTexId == 0) {
            geoTexId = GL11.glGenTextures();
            int prev = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, geoTexId);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, MAP_SIZE, MAP_SIZE, 0,
                    GL11.GL_RGBA, GL11.GL_FLOAT, (ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prev);
        }
        if (colTexId == 0) {
            colTexId = GL11.glGenTextures();
            int prev = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, colTexId);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, MAP_SIZE, MAP_SIZE, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prev);
        }
    }

    private static void rebuildAndUploadHeightfield(
            boolean hasSurface,
            GroundHazeGrid.LocalCompositeGrid localComp,
            Vec3 camPos,
            float nightFactor,
            boolean shaderActive,
            float maxSurfaceRadius) {
        ensureStagingArrays();
        ensureHeightfieldTextures();

        int oX = localComp.hasData ? localComp.originX : (Mth.floor(camPos.x) - GroundHazeGrid.COMP_RADIUS);
        int oZ = localComp.hasData ? localComp.originZ : (Mth.floor(camPos.z) - GroundHazeGrid.COMP_RADIUS);
        mapOriginX = oX;
        mapOriginZ = oZ;

        Arrays.fill(tmpValid, false);
        Arrays.fill(tmpMask, 0.0F);
        Arrays.fill(tmpDen, 0.0F);

        int activeMinX = MAP_SIZE, activeMaxX = -1;
        int activeMinZ = MAP_SIZE, activeMaxZ = -1;
        float minY = Float.MAX_VALUE;
        float maxTopY = -Float.MAX_VALUE;

        // 1. Merge natural biome surface haze (upsampled cleanly from 2-block sliding grid into 1-block map)
        if (hasSurface) {
            int sSize = GRID.getGridSize();
            if (sSize >= 2) {
                int sOriginX = GRID.getOriginWorldX();
                int sOriginZ = GRID.getOriginWorldZ();
                float sCenterLocal = (sSize - 1) * GroundHazeGrid.CELL_STEP * 0.5F;
                float fadeInner = maxSurfaceRadius * 0.56F;
                float fadeSpan = Math.max(8.0F, maxSurfaceRadius - fadeInner);

                float[] sY = GRID.smoothY;
                float[] eF = GRID.openEdgeFade;
                float[] sA = GRID.smoothAlpha;
                float[] sR = GRID.smoothR;
                float[] sG = GRID.smoothG;
                float[] sB = GRID.smoothB;
                float[] sL2 = GRID.smoothLayer2;
                int[] lights = GRID.packedLight;

                for (int gz = 0; gz < sSize; gz++) {
                    int wzBase = sOriginZ + gz * GroundHazeGrid.CELL_STEP;
                    float dz = (gz * GroundHazeGrid.CELL_STEP) - sCenterLocal;
                    int sRow = gz * sSize;
                    for (int gx = 0; gx < sSize; gx++) {
                        int sIdx = sRow + gx;
                        float sy = sY[sIdx];
                        float sa = sA[sIdx];
                        float ef = eF[sIdx];
                        if (sy <= INVALID_Y_CUTOFF || sa <= 0.004F || ef <= 0.004F) continue;

                        float dx = (gx * GroundHazeGrid.CELL_STEP) - sCenterLocal;
                        float dist = Mth.sqrt(dx * dx + dz * dz);
                        if (dist >= maxSurfaceRadius) continue;

                        float outerT = 1.0F - Mth.clamp((dist - fadeInner) / fadeSpan, 0.0F, 1.0F);
                        float outerFade = outerT * outerT * (3.0F - 2.0F * outerT);
                        float mask = ef * outerFade;
                        if (mask <= 0.004F) continue;

                        float fh = Mth.clamp(sL2[sIdx] + 1.15F, 2.15F, 2.45F);
                        int light = lights[sIdx];
                        float lr = applyLightR(sR[sIdx], light, nightFactor, shaderActive);
                        float lg = applyLightG(sG[sIdx], light, nightFactor, shaderActive);
                        float lb = applyLightB(sB[sIdx], light, nightFactor, shaderActive);

                        int wxBase = sOriginX + gx * GroundHazeGrid.CELL_STEP;
                        for (int subZ = 0; subZ < GroundHazeGrid.CELL_STEP; subZ++) {
                            int mz = (wzBase + subZ) - oZ;
                            if (mz < 0 || mz >= MAP_SIZE) continue;
                            int mRow = mz * MAP_SIZE;
                            for (int subX = 0; subX < GroundHazeGrid.CELL_STEP; subX++) {
                                int mx = (wxBase + subX) - oX;
                                if (mx < 0 || mx >= MAP_SIZE) continue;
                                int mIdx = mRow + mx;
                                tmpValid[mIdx] = true;
                                tmpFloorY[mIdx] = sy;
                                tmpFogH[mIdx] = fh;
                                tmpDen[mIdx] = sa;
                                tmpMask[mIdx] = mask;
                                tmpR[mIdx] = lr;
                                tmpG[mIdx] = lg;
                                tmpB[mIdx] = lb;
                                if (mx < activeMinX) activeMinX = mx;
                                if (mx > activeMaxX) activeMaxX = mx;
                                if (mz < activeMinZ) activeMinZ = mz;
                                if (mz > activeMaxZ) activeMaxZ = mz;
                                if (sy < minY) minY = sy;
                                if (sy + fh > maxTopY) maxTopY = sy + fh;
                            }
                        }
                    }
                }
            }
        }

        // 2. Merge deduplicated local dungeon/structure fog (1-block precision across full 289x289 map)
        if (localComp.hasData) {
            float center = GroundHazeGrid.COMP_RADIUS + 0.5F;
            float farSpan = LOCAL_FAR_FADE_END - LOCAL_FAR_FADE_START;

            float[] lY = localComp.smoothY;
            float[] lH = localComp.fogHeight;
            float[] lDen = localComp.maskAlpha;
            float[] lEdge = localComp.edgeFade;
            float[] lR = localComp.r;
            float[] lG = localComp.g;
            float[] lB = localComp.b;
            int[] lLight = localComp.packedLight;

            for (int cz = localComp.dirtyMinZ; cz <= localComp.dirtyMaxZ; cz++) {
                float dz = (cz + 0.5F) - center;
                int row = cz * MAP_SIZE;
                for (int cx = localComp.dirtyMinX; cx <= localComp.dirtyMaxX; cx++) {
                    int idx = row + cx;
                    float den = lDen[idx];
                    float edge = lEdge[idx];
                    float sy = lY[idx];
                    if (den <= 0.004F || edge <= 0.004F || sy <= INVALID_Y_CUTOFF) continue;

                    float dx = (cx + 0.5F) - center;
                    float dist = Mth.sqrt(dx * dx + dz * dz);
                    if (dist >= LOCAL_FAR_FADE_END) continue;

                    float farFade = 1.0F;
                    if (dist > LOCAL_FAR_FADE_START) {
                        float ft = 1.0F - Mth.clamp((dist - LOCAL_FAR_FADE_START) / farSpan, 0.0F, 1.0F);
                        farFade = ft * ft * (3.0F - 2.0F * ft);
                    }
                    float mask = edge * farFade;
                    if (mask <= 0.004F) continue;

                    int light = lLight[idx];
                    float lr = applyLightR(lR[idx], light, nightFactor, shaderActive);
                    float lg = applyLightG(lG[idx], light, nightFactor, shaderActive);
                    float lb = applyLightB(lB[idx], light, nightFactor, shaderActive);
                    float fh = lH[idx];

                    if (!tmpValid[idx] || den * mask >= tmpDen[idx] * tmpMask[idx]) {
                        tmpValid[idx] = true;
                        tmpFloorY[idx] = sy;
                        tmpFogH[idx] = fh;
                        tmpDen[idx] = den;
                        tmpMask[idx] = mask;
                        tmpR[idx] = lr;
                        tmpG[idx] = lg;
                        tmpB[idx] = lb;
                    }
                    if (cx < activeMinX) activeMinX = cx;
                    if (cx > activeMaxX) activeMaxX = cx;
                    if (cz < activeMinZ) activeMinZ = cz;
                    if (cz > activeMaxZ) activeMaxZ = cz;
                    if (sy < minY) minY = sy;
                    if (sy + fh > maxTopY) maxTopY = sy + fh;
                }
            }
        }

        if (activeMaxX < activeMinX || minY > maxTopY) {
            heightfieldHasData = false;
            return;
        }

        // 3. 1-texel perimeter & wall dilation: copies valid floorY, fogHeight, density, and RGB into
        //    adjacent empty/wall texels while keeping mask = 0.0 -> guarantees 100% glitch-free bilinear
        //    interpolation right up to curved walls & edges with ZERO leak through 1-block thin walls!
        int dilMinX = Math.max(0, activeMinX - 1);
        int dilMaxX = Math.min(MAP_SIZE - 1, activeMaxX + 1);
        int dilMinZ = Math.max(0, activeMinZ - 1);
        int dilMaxZ = Math.min(MAP_SIZE - 1, activeMaxZ + 1);

        for (int z = dilMinZ; z <= dilMaxZ; z++) {
            int row = z * MAP_SIZE;
            for (int x = dilMinX; x <= dilMaxX; x++) {
                int idx = row + x;
                if (tmpValid[idx]) continue;

                float sumY = 0, sumH = 0, sumD = 0, sumR = 0, sumG = 0, sumB = 0;
                int cnt = 0;
                for (int dz = -1; dz <= 1; dz++) {
                    int nz = z + dz;
                    if (nz < 0 || nz >= MAP_SIZE) continue;
                    int nRow = nz * MAP_SIZE;
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dz == 0) continue;
                        int nx = x + dx;
                        if (nx < 0 || nx >= MAP_SIZE) continue;
                        int nIdx = nRow + nx;
                        if (!tmpValid[nIdx]) continue;
                        sumY += tmpFloorY[nIdx];
                        sumH += tmpFogH[nIdx];
                        sumD += tmpDen[nIdx];
                        sumR += tmpR[nIdx];
                        sumG += tmpG[nIdx];
                        sumB += tmpB[nIdx];
                        cnt++;
                    }
                }
                if (cnt > 0) {
                    float inv = 1.0F / cnt;
                    tmpFloorY[idx] = sumY * inv;
                    tmpFogH[idx] = sumH * inv;
                    tmpDen[idx] = sumD * inv;
                    tmpR[idx] = sumR * inv;
                    tmpG[idx] = sumG * inv;
                    tmpB[idx] = sumB * inv;
                    // mask stays 0.0F so open edges & 1-block walls feather cleanly without bleeding!
                }
            }
        }

        // Center GL_RGBA16F height storage around active elevation for maximum half-float precision
        mapBaseY = (minY + maxTopY) * 0.5F;
        mapMinFogY = minY;
        mapMaxFogTopY = maxTopY;

        geoUploadBuf.clear();
        colUploadBuf.clear();
        for (int i = 0; i < MAP_CELLS; i++) {
            float m = tmpMask[i];
            if (tmpValid[i] || tmpDen[i] > 0.0F) {
                geoUploadBuf.put(tmpFloorY[i] - mapBaseY);
                geoUploadBuf.put(tmpFogH[i]);
                geoUploadBuf.put(tmpDen[i]);
                geoUploadBuf.put(m);

                colUploadBuf.put((byte) Mth.clamp(Math.round(tmpR[i] * 255.0F), 0, 255));
                colUploadBuf.put((byte) Mth.clamp(Math.round(tmpG[i] * 255.0F), 0, 255));
                colUploadBuf.put((byte) Mth.clamp(Math.round(tmpB[i] * 255.0F), 0, 255));
                colUploadBuf.put((byte) 255);
            } else {
                geoUploadBuf.put(0.0F);
                geoUploadBuf.put(2.0F);
                geoUploadBuf.put(0.0F);
                geoUploadBuf.put(0.0F);

                colUploadBuf.put((byte) 220);
                colUploadBuf.put((byte) 230);
                colUploadBuf.put((byte) 242);
                colUploadBuf.put((byte) 255);
            }
        }
        geoUploadBuf.flip();
        colUploadBuf.flip();

        // Reset unpack state cleanly (Minecraft font/atlas uploads can leave non-zero row length!)
        int prevRowLen = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        int prevSkipPix = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        int prevSkipRow = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        int prevAlign = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, geoTexId);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, MAP_SIZE, MAP_SIZE,
                GL11.GL_RGBA, GL11.GL_FLOAT, geoUploadBuf);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colTexId);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, MAP_SIZE, MAP_SIZE,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, colUploadBuf);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, prevRowLen);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, prevSkipPix);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, prevSkipRow);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, prevAlign);

        heightfieldHasData = true;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // sRGB-Calibrated Atmospheric Lighting (luminous mist under both shaders & vanilla)
    // ─────────────────────────────────────────────────────────────────────────────

    private static float computeSrgbBrightness(int packedLight, float nightFactor, boolean shaderActive) {
        float blockF = LightTexture.block(packedLight) / 15.0F;
        float skyNightScale = shaderActive ? 0.72F : 0.60F;
        float skyContrib = (LightTexture.sky(packedLight) / 15.0F) * Mth.lerp(nightFactor, 1.0F, skyNightScale);
        float blockContrib = blockF * 0.96F;
        float combined = Math.max(skyContrib, blockContrib);
        return 0.34F + 0.62F * Mth.sqrt(Mth.clamp(combined, 0.0F, 1.0F));
    }

    private static float applyLightR(float r, int packedLight, float nightFactor, boolean shaderActive) {
        float brightness = computeSrgbBrightness(packedLight, nightFactor, shaderActive);
        float blockF = LightTexture.block(packedLight) / 15.0F;
        float skyF = (LightTexture.sky(packedLight) / 15.0F) * Mth.lerp(nightFactor, 1.0F, shaderActive ? 0.72F : 0.60F);
        float warm = Math.max(0.0F, blockF - skyF) * 0.12F;
        return Mth.clamp(r * brightness * (1.0F + warm), 0.0F, 1.0F);
    }

    private static float applyLightG(float g, int packedLight, float nightFactor, boolean shaderActive) {
        float brightness = computeSrgbBrightness(packedLight, nightFactor, shaderActive);
        float blockF = LightTexture.block(packedLight) / 15.0F;
        float skyF = (LightTexture.sky(packedLight) / 15.0F) * Mth.lerp(nightFactor, 1.0F, shaderActive ? 0.72F : 0.60F);
        float warm = Math.max(0.0F, blockF - skyF) * 0.05F;
        return Mth.clamp(g * brightness * (1.0F + warm), 0.0F, 1.0F);
    }

    private static float applyLightB(float b, int packedLight, float nightFactor, boolean shaderActive) {
        float brightness = computeSrgbBrightness(packedLight, nightFactor, shaderActive);
        float blockF = LightTexture.block(packedLight) / 15.0F;
        float skyF = (LightTexture.sky(packedLight) / 15.0F) * Mth.lerp(nightFactor, 1.0F, shaderActive ? 0.72F : 0.60F);
        float cool = Math.max(0.0F, skyF - blockF) * nightFactor * 0.08F;
        float warmDrop = Math.max(0.0F, blockF - skyF) * 0.04F;
        return Mth.clamp(b * brightness * (1.0F + cool - warmDrop), 0.0F, 1.0F);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // OpenGL Spec-Compliant Format-Matched Depth Buffer Copy (<0.03ms GPU blit)
    // ─────────────────────────────────────────────────────────────────────────────

    private static boolean copyMainDepthBuffer(RenderTarget mainTarget) {
        int srcDepthTex = mainTarget.getDepthTextureId();
        int w = mainTarget.width;
        int h = mainTarget.height;
        if (srcDepthTex <= 0 || w <= 0 || h <= 0) {
            return false;
        }

        boolean needsAlloc = (depthCopyFbo == 0 || depthCopyTex == 0
                || w != depthCopyW || h != depthCopyH || srcDepthTex != depthCopySrcId);

        if (needsAlloc) {
            releaseDepthCopy();

            int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, srcDepthTex);
            int internalFormat = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
            if (internalFormat == 0) {
                internalFormat = GL14.GL_DEPTH_COMPONENT24;
            }

            int pixelFormat;
            int pixelType;
            int attachment;
            if (internalFormat == GL30.GL_DEPTH24_STENCIL8) {
                pixelFormat = GL30.GL_DEPTH_STENCIL;
                pixelType = GL30.GL_UNSIGNED_INT_24_8;
                attachment = GL30.GL_DEPTH_STENCIL_ATTACHMENT;
            } else if (internalFormat == GL30.GL_DEPTH32F_STENCIL8) {
                pixelFormat = GL30.GL_DEPTH_STENCIL;
                pixelType = GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV;
                attachment = GL30.GL_DEPTH_STENCIL_ATTACHMENT;
            } else {
                pixelFormat = GL11.GL_DEPTH_COMPONENT;
                pixelType = (internalFormat == GL30.GL_DEPTH_COMPONENT32F) ? GL11.GL_FLOAT : GL11.GL_UNSIGNED_INT;
                attachment = GL30.GL_DEPTH_ATTACHMENT;
            }

            depthCopyTex = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthCopyTex);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, w, h, 0, pixelFormat, pixelType, (ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);

            int prevFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            depthCopyFbo = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, depthCopyFbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachment, GL11.GL_TEXTURE_2D, depthCopyTex, 0);
            GL11.glDrawBuffer(GL11.GL_NONE);
            GL11.glReadBuffer(GL11.GL_NONE);
            int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);

            depthCopyW = w;
            depthCopyH = h;
            depthCopySrcId = srcDepthTex;
            depthCopyValid = (status == GL30.GL_FRAMEBUFFER_COMPLETE);
            if (!depthCopyValid) {
                OririMod.LOGGER.warn("[OririMod] Ground haze depth copy FBO incomplete (status={}, fmt={}).",
                        status, internalFormat);
                releaseDepthCopy();
                return false;
            }
        }

        if (!depthCopyValid) {
            return false;
        }

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainTarget.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, depthCopyFbo);
        GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mainTarget.frameBufferId);
        return true;
    }

    private static void releaseDepthCopy() {
        if (depthCopyFbo != 0) {
            GL30.glDeleteFramebuffers(depthCopyFbo);
            depthCopyFbo = 0;
        }
        if (depthCopyTex != 0) {
            GL11.glDeleteTextures(depthCopyTex);
            depthCopyTex = 0;
        }
        depthCopyW = 0;
        depthCopyH = 0;
        depthCopySrcId = 0;
        depthCopyValid = false;
    }

    private static void releaseGpuResources() {
        if (geoTexId != 0) {
            GL11.glDeleteTextures(geoTexId);
            geoTexId = 0;
        }
        if (colTexId != 0) {
            GL11.glDeleteTextures(colTexId);
            colTexId = 0;
        }
        heightfieldHasData = false;
        uploadedSurfaceVersion = -1;
        uploadedLocalVersion = -1;
        releaseDepthCopy();
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
