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
 * Renders unified GPU heightfield volumetric fog for:
 * <ul>
 *   <li>Natural surface biome ground haze (Scarlet Swamp, Elderwoods, Elder Beach, Vile Plains, Elder Valley)</li>
 *   <li>Ground-hugging {@link net.ganyusbathwater.oririmod.block.custom.FogSourceBlock} / chunk-attached structure emitters</li>
 *   <li>3D floating &amp; room-filling {@link net.ganyusbathwater.oririmod.block.custom.VolumeFogSourceBlock} emitters</li>
 * </ul>
 * Both layers evaluate in ONE single 4-vertex fullscreen volumetric raymarch pass post-Iris
 * ({@code GameRendererMixin}), guaranteeing zero circles, zero horizon beams, zero wall sandwich stripes,
 * full ~144-block visibility, and identical visuals across all shaderpacks and {@code F1} GUI toggles.
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

    // Layer A (Ground-Hugging) 289x289 GPU textures (Sampler2 = RGBA16F geo/density, Sampler3 = RGBA8 sRGB color)
    private static int geoTexId = 0;
    private static int colTexId = 0;
    private static boolean groundMapHasData = false;
    private static int mapOriginX = 0;
    private static int mapOriginZ = 0;
    private static float mapBaseY = 0.0F;
    private static float mapMinFogY = 0.0F;
    private static float mapMaxFogTopY = 0.0F;
    private static int uploadedSurfaceVersion = -1;
    private static int uploadedLocalVersion = -1;
    private static int uploadedGroundNightBucket = Integer.MIN_VALUE;
    private static boolean uploadedGroundShaderActive = false;

    // Layer B (3D Floating / Room-Filling Volume) 289x289 GPU textures (Sampler4 = RGBA16F, Sampler5 = RGBA8)
    private static int volGeoTexId = 0;
    private static int volColTexId = 0;
    private static boolean volumeMapHasData = false;
    private static int volOriginX = 0;
    private static int volOriginZ = 0;
    private static float volBaseY = 0.0F;
    private static float volMinY = 0.0F;
    private static float volMaxTopY = 0.0F;
    private static int uploadedVolumeVersion = -1;
    private static int uploadedVolNightBucket = Integer.MIN_VALUE;
    private static boolean uploadedVolShaderActive = false;

    // Shared reusable native upload buffers & staging arrays (zero per-frame GC!)
    private static FloatBuffer geoUploadBuf = null;
    private static ByteBuffer colUploadBuf = null;
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

        // Creative Fog Source & Volume Fog Source marker wireframes stay in the translucent world stage
        if (stage == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            Item fogItem = ModBlocks.FOG_SOURCE.asItem();
            Item volItem = ModBlocks.VOLUME_FOG_SOURCE.asItem();
            boolean holdingFog = player != null
                    && (player.getMainHandItem().is(fogItem) || player.getOffhandItem().is(fogItem)
                    || player.getMainHandItem().is(volItem) || player.getOffhandItem().is(volItem));
            if (holdingFog) {
                List<GroundHazeGrid.LocalFogPatch> groundPatches = GRID.getActiveLocalPatches();
                List<GroundHazeGrid.VolumeFogPatch> volPatches = GRID.getActiveVolumePatches();
                if (!groundPatches.isEmpty() || !volPatches.isEmpty()) {
                    PoseStack poseStack = event.getPoseStack();
                    Vec3 camPos = event.getCamera().getPosition();
                    MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
                    poseStack.pushPose();
                    poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
                    renderFogSourceMarkers(poseStack, bufferSource, groundPatches, volPatches);
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
        GroundHazeGrid.VolumeCompositeGrid volComp = GRID.getVolumeCompositeGrid();
        boolean hasGroundSource = hasSurface || localComp.hasData;
        boolean hasVolumeSource = volComp.hasData;

        if (!hasGroundSource && !hasVolumeSource) {
            groundMapHasData = false;
            volumeMapHasData = false;
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        float nightFactor = GRID.getNightFactor();
        int nightBucket = Math.round(nightFactor * 40.0F);
        boolean shaderActive = ShaderDetector.isShaderActive();

        // Update Layer A (Ground-Hugging) GPU textures only when terrain/patches/lighting change
        if (hasGroundSource) {
            int sVer = GRID.getSurfaceMeshVersion();
            int lVer = GRID.getLocalMeshVersion();
            if (geoTexId == 0 || colTexId == 0
                    || sVer != uploadedSurfaceVersion
                    || lVer != uploadedLocalVersion
                    || nightBucket != uploadedGroundNightBucket
                    || shaderActive != uploadedGroundShaderActive) {
                rebuildAndUploadGroundHeightfield(hasSurface, localComp, camPos, nightFactor, shaderActive,
                        OririClientConfig.getGroundHazeRadius());
                uploadedSurfaceVersion = sVer;
                uploadedLocalVersion = lVer;
                uploadedGroundNightBucket = nightBucket;
                uploadedGroundShaderActive = shaderActive;
            }
        } else {
            groundMapHasData = false;
        }

        // Update Layer B (3D Floating / Room-Filling Volume) GPU textures only when volume patches/lighting change
        if (hasVolumeSource) {
            int vVer = GRID.getVolumeMeshVersion();
            if (volGeoTexId == 0 || volColTexId == 0
                    || vVer != uploadedVolumeVersion
                    || nightBucket != uploadedVolNightBucket
                    || shaderActive != uploadedVolShaderActive) {
                rebuildAndUploadVolumeField(volComp, nightFactor, shaderActive);
                uploadedVolumeVersion = vVer;
                uploadedVolNightBucket = nightBucket;
                uploadedVolShaderActive = shaderActive;
            }
        } else {
            volumeMapHasData = false;
        }

        if (!groundMapHasData && !volumeMapHasData) {
            return;
        }

        // Blit 3D scene depth buffer into our format-matched read texture (0 feedback loops)
        RenderTarget mainTarget = mc.getMainRenderTarget();
        if (!copyMainDepthBuffer(mainTarget)) {
            return;
        }

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
        RenderSystem.setShaderTexture(2, groundMapHasData ? geoTexId : 0);
        RenderSystem.setShaderTexture(3, groundMapHasData ? colTexId : 0);
        RenderSystem.setShaderTexture(4, volumeMapHasData ? volGeoTexId : 0);
        RenderSystem.setShaderTexture(5, volumeMapHasData ? volColTexId : 0);

        Uniform uInvProjMat = shader.getUniform("InvProjMat");
        Uniform uInvViewMat = shader.getUniform("InvViewMat");
        Uniform uDepthBufSize = shader.getUniform("DepthBufferSize");
        Uniform uCamWorldPos = shader.getUniform("CamWorldPos");
        Uniform uMapRelOrigin = shader.getUniform("MapRelOrigin");
        Uniform uVolRelOrigin = shader.getUniform("VolRelOrigin");
        Uniform uMapSize = shader.getUniform("MapSize");
        Uniform uFogYBounds = shader.getUniform("FogYBounds");
        Uniform uVolYBounds = shader.getUniform("VolYBounds");
        Uniform uHasGroundFog = shader.getUniform("HasGroundFog");
        Uniform uHasVolumeFog = shader.getUniform("HasVolumeFog");
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
        if (uVolRelOrigin != null) {
            uVolRelOrigin.set(
                    (float) (volOriginX - camPos.x),
                    (float) (volBaseY - camPos.y),
                    (float) (volOriginZ - camPos.z));
        }
        if (uMapSize != null) {
            uMapSize.set((float) MAP_SIZE);
        }
        if (uFogYBounds != null) {
            uFogYBounds.set(
                    (float) (mapMinFogY - 0.45 - camPos.y),
                    (float) (mapMaxFogTopY + 0.15 - camPos.y));
        }
        if (uVolYBounds != null) {
            uVolYBounds.set(
                    (float) (volMinY - 0.15 - camPos.y),
                    (float) (volMaxTopY + 0.15 - camPos.y));
        }
        if (uHasGroundFog != null) {
            uHasGroundFog.set(groundMapHasData ? 1.0F : 0.0F);
        }
        if (uHasVolumeFog != null) {
            uHasVolumeFog.set(volumeMapHasData ? 1.0F : 0.0F);
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
            RenderSystem.setShaderTexture(4, 0);
            RenderSystem.setShaderTexture(5, 0);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Unified 289x289 GPU Texture Builders & Uploaders (Ground + 3D Volume)
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

    private static int createGeoTexture() {
        int id = GL11.glGenTextures();
        int prev = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, MAP_SIZE, MAP_SIZE, 0,
                GL11.GL_RGBA, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prev);
        return id;
    }

    private static int createColTexture() {
        int id = GL11.glGenTextures();
        int prev = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, MAP_SIZE, MAP_SIZE, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prev);
        return id;
    }

    private static void rebuildAndUploadGroundHeightfield(
            boolean hasSurface,
            GroundHazeGrid.LocalCompositeGrid localComp,
            Vec3 camPos,
            float nightFactor,
            boolean shaderActive,
            float maxSurfaceRadius) {
        ensureStagingArrays();
        if (geoTexId == 0) geoTexId = createGeoTexture();
        if (colTexId == 0) colTexId = createColTexture();

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

        // 1. Natural biome surface haze
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

        // 2. Deduplicated local ground FogSourceBlock / FogAnchor fog
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
            groundMapHasData = false;
            return;
        }

        dilatePerimeterTexels(activeMinX, activeMaxX, activeMinZ, activeMaxZ);
        mapBaseY = (minY + maxTopY) * 0.5F;
        mapMinFogY = minY;
        mapMaxFogTopY = maxTopY;
        uploadStagingBuffersToTextures(geoTexId, colTexId, mapBaseY);
        groundMapHasData = true;
    }

    private static void rebuildAndUploadVolumeField(
            GroundHazeGrid.VolumeCompositeGrid volComp,
            float nightFactor,
            boolean shaderActive) {
        ensureStagingArrays();
        if (volGeoTexId == 0) volGeoTexId = createGeoTexture();
        if (volColTexId == 0) volColTexId = createColTexture();

        volOriginX = volComp.originX;
        volOriginZ = volComp.originZ;

        Arrays.fill(tmpValid, false);
        Arrays.fill(tmpMask, 0.0F);
        Arrays.fill(tmpDen, 0.0F);

        int activeMinX = MAP_SIZE, activeMaxX = -1;
        int activeMinZ = MAP_SIZE, activeMaxZ = -1;
        float minY = Float.MAX_VALUE;
        float maxTopY = -Float.MAX_VALUE;

        float center = GroundHazeGrid.COMP_RADIUS + 0.5F;
        float farSpan = LOCAL_FAR_FADE_END - LOCAL_FAR_FADE_START;

        float[] bArr = volComp.bottomY;
        float[] tArr = volComp.topY;
        float[] dArr = volComp.density;
        float[] mArr = volComp.mask;
        float[] rArr = volComp.r;
        float[] gArr = volComp.g;
        float[] bColArr = volComp.b;
        int[] lArr = volComp.packedLight;

        for (int cz = volComp.dirtyMinZ; cz <= volComp.dirtyMaxZ; cz++) {
            float dz = (cz + 0.5F) - center;
            int row = cz * MAP_SIZE;
            for (int cx = volComp.dirtyMinX; cx <= volComp.dirtyMaxX; cx++) {
                int idx = row + cx;
                float m = mArr[idx];
                if (m <= 0.004F) continue;
                float by = bArr[idx];
                float ty = tArr[idx];
                float vh = ty - by;
                if (vh <= 0.1F) continue;

                float dx = (cx + 0.5F) - center;
                float dist = Mth.sqrt(dx * dx + dz * dz);
                if (dist >= LOCAL_FAR_FADE_END) continue;

                float farFade = 1.0F;
                if (dist > LOCAL_FAR_FADE_START) {
                    float ft = 1.0F - Mth.clamp((dist - LOCAL_FAR_FADE_START) / farSpan, 0.0F, 1.0F);
                    farFade = ft * ft * (3.0F - 2.0F * ft);
                }
                float mask = m * farFade;
                if (mask <= 0.004F) continue;

                int light = lArr[idx];
                tmpValid[idx] = true;
                tmpFloorY[idx] = by;
                tmpFogH[idx] = vh;
                tmpDen[idx] = dArr[idx];
                tmpMask[idx] = mask;
                tmpR[idx] = applyLightR(rArr[idx], light, nightFactor, shaderActive);
                tmpG[idx] = applyLightG(gArr[idx], light, nightFactor, shaderActive);
                tmpB[idx] = applyLightB(bColArr[idx], light, nightFactor, shaderActive);

                if (cx < activeMinX) activeMinX = cx;
                if (cx > activeMaxX) activeMaxX = cx;
                if (cz < activeMinZ) activeMinZ = cz;
                if (cz > activeMaxZ) activeMaxZ = cz;
                if (by < minY) minY = by;
                if (ty > maxTopY) maxTopY = ty;
            }
        }

        if (activeMaxX < activeMinX || minY > maxTopY) {
            volumeMapHasData = false;
            return;
        }

        dilatePerimeterTexels(activeMinX, activeMaxX, activeMinZ, activeMaxZ);
        volBaseY = (minY + maxTopY) * 0.5F;
        volMinY = minY;
        volMaxTopY = maxTopY;
        uploadStagingBuffersToTextures(volGeoTexId, volColTexId, volBaseY);
        volumeMapHasData = true;
    }

    /**
     * 1-texel perimeter &amp; wall dilation: copies valid height/density/RGB into adjacent empty/wall texels
     * while keeping {@code mask = 0.0} so bilinear interpolation stays 100% glitch-free right up to curved
     * walls with ZERO bleed through 1-block thin walls.
     */
    private static void dilatePerimeterTexels(int activeMinX, int activeMaxX, int activeMinZ, int activeMaxZ) {
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
                }
            }
        }
    }

    private static void uploadStagingBuffersToTextures(int targetGeoTex, int targetColTex, float baseY) {
        geoUploadBuf.clear();
        colUploadBuf.clear();
        for (int i = 0; i < MAP_CELLS; i++) {
            float m = tmpMask[i];
            if (tmpValid[i] || tmpDen[i] > 0.0F) {
                geoUploadBuf.put(tmpFloorY[i] - baseY);
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

        int prevRowLen = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        int prevSkipPix = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        int prevSkipRow = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        int prevAlign = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, targetGeoTex);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, MAP_SIZE, MAP_SIZE,
                GL11.GL_RGBA, GL11.GL_FLOAT, geoUploadBuf);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, targetColTex);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, MAP_SIZE, MAP_SIZE,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, colUploadBuf);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, prevRowLen);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, prevSkipPix);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, prevSkipRow);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, prevAlign);
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
        if (volGeoTexId != 0) {
            GL11.glDeleteTextures(volGeoTexId);
            volGeoTexId = 0;
        }
        if (volColTexId != 0) {
            GL11.glDeleteTextures(volColTexId);
            volColTexId = 0;
        }
        groundMapHasData = false;
        volumeMapHasData = false;
        uploadedSurfaceVersion = -1;
        uploadedLocalVersion = -1;
        uploadedVolumeVersion = -1;
        releaseDepthCopy();
    }

    private static void renderFogSourceMarkers(
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            List<GroundHazeGrid.LocalFogPatch> groundPatches,
            List<GroundHazeGrid.VolumeFogPatch> volPatches) {
        VertexConsumer lineVc = bufferSource.getBuffer(RenderType.lines());
        for (int i = 0; i < groundPatches.size(); i++) {
            GroundHazeGrid.LocalFogPatch p = groundPatches.get(i);
            BlockPos pos = p.sourcePos;
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineVc,
                    pos.getX() + 0.15, pos.getY() + 0.15, pos.getZ() + 0.15,
                    pos.getX() + 0.85, pos.getY() + 0.85, pos.getZ() + 0.85,
                    p.color.r, p.color.g, p.color.b, 0.90F);
        }
        // Double-box wireframe distinguishes 3D Volume Fog Sources from ground Fog Sources at a glance
        for (int i = 0; i < volPatches.size(); i++) {
            GroundHazeGrid.VolumeFogPatch p = volPatches.get(i);
            BlockPos pos = p.sourcePos;
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineVc,
                    pos.getX() + 0.08, pos.getY() + 0.08, pos.getZ() + 0.08,
                    pos.getX() + 0.92, pos.getY() + 0.92, pos.getZ() + 0.92,
                    p.color.r, p.color.g, p.color.b, 0.92F);
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineVc,
                    pos.getX() + 0.28, pos.getY() + 0.28, pos.getZ() + 0.28,
                    pos.getX() + 0.72, pos.getY() + 0.72, pos.getZ() + 0.72,
                    1.0F, 1.0F, 1.0F, 0.75F);
        }
        bufferSource.endBatch(RenderType.lines());
    }
}
