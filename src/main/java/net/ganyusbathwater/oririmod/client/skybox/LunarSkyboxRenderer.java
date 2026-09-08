package net.ganyusbathwater.oririmod.client.skybox;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * Phase 3a: Renders a pure black void sky and a dense animated star field.
 *
 * Architecture:
 *  - Black sky dome: static VertexBuffer (6-quad cube), rendered first, opaque.
 *    Used as the Iris-proof void fill — covers whatever Iris rendered before AFTER_SKY fires.
 *
 *  - Star field: CPU-side data arrays (generated once). Rebuilt into Tesselator quads
 *    EVERY FRAME with animated per-star alpha so stars can twinkle independently.
 *    Cost: 8,000 stars × 4 verts × ~16 bytes ≈ 512KB/frame — acceptable at 60fps.
 *
 * Star twinkling:
 *  - Each star has a random phaseOffset [0, 2π] and twinkleSpeed [0.3, 2.0] generated at init.
 *  - Animated alpha = baseAlpha * (TWINKLE_MIN + (1 - TWINKLE_MIN) * 0.5 * (1 + sin(t * speed + phase)))
 *  - Stars never go fully dark (TWINKLE_MIN = 0.30) — they pulse, not flash.
 *  - Brighter stars twinkle more visibly; dim stars have subtler variation.
 *  - Time source: System.nanoTime() for frame-rate-independent smooth animation.
 */
@OnlyIn(Dist.CLIENT)
public final class LunarSkyboxRenderer {

    // -------------------------------------------------------------------------
    // Earth constants
    // -------------------------------------------------------------------------

    /** Equirectangular Earth surface texture. Loaded automatically by MC's resource manager. */
    private static final ResourceLocation EARTH_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("oririmod", "textures/skybox/earth.png");

    /** Nighttime city lights texture. Additive blend over the dark side. */
    private static final ResourceLocation EARTH_NIGHT_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("oririmod", "textures/skybox/earth_night.png");

    /**
     * Earth sky position.
     * In MC coordinates: Y = up, -Z = north, +X = east.
     * Elevation 5° (just above horizon), azimuth 25° east of north.
     * With a half-size of 75, the Earth will dip below the horizon line.
     * Pre-normalised direction vector.
     */
    private static final float EARTH_DX = (float)(Math.sin(Math.toRadians(25)) * Math.cos(Math.toRadians(5)));
    private static final float EARTH_DY = (float)(Math.sin(Math.toRadians(5)));
    private static final float EARTH_DZ = (float)(-Math.cos(Math.toRadians(25)) * Math.cos(Math.toRadians(5)));

    /**
     * Angular half-size of the Earth quad in world units.
     * 75 units at sky radius 100 ≈ 36.9° half-angle ≈ 74° total angular diameter.
     * Dramatically large — fills a significant portion of the visible sky.
     */
    private static final float EARTH_HALF_SIZE = 75.0f;

    /**
     * Earth axial tilt in radians (23.4°), applied to the billboard up-vector.
     * Makes the equatorial band appear correctly angled rather than horizontal.
     */
    private static final float EARTH_AXIAL_TILT = (float) Math.toRadians(23.4);

    /**
     * UV scroll speed for Earth rotation: 1 full rotation in 480 real seconds (8 minutes).
     */
    private static final float EARTH_ROTATION_SPEED = 1.0f / 480.0f;

    // -------------------------------------------------------------------------
    // Star generation constants
    // -------------------------------------------------------------------------
    private static final long  STAR_SEED   = 0xDEADBEEFCAFEL;
    private static final int   STAR_COUNT  = 8000;
    private static final float SKY_RADIUS  = 100.0f;

    // Size classes (world-space half-extent of billboard quads)
    private static final float SIZE_TINY   = 0.07f;
    private static final float SIZE_SMALL  = 0.16f;
    private static final float SIZE_MEDIUM = 0.32f;

    // RGB + max-alpha per tier (alpha gets modulated by twinkling)
    // Tier 1: dim warm-white
    private static final float[] TIER_DIM    = { 0.75f, 0.78f, 0.85f, 0.72f };
    // Tier 2: neutral white
    private static final float[] TIER_NORMAL = { 0.90f, 0.92f, 1.00f, 0.92f };
    // Tier 3: bright blue-white
    private static final float[] TIER_BRIGHT = { 0.95f, 0.97f, 1.00f, 1.00f };

    // Twinkling envelope: stars oscillate between TWINKLE_MIN and 1.0 of their base alpha.
    // 0.55 means a star at peak brightness dims to 55% at its darkest — noticeably bright at all times.
    private static final float TWINKLE_MIN        = 0.55f;
    // Speed range in radians/second. Slow = 0.3 rad/s (~20s full cycle). Fast = 2.0 rad/s (~3s cycle).
    private static final float TWINKLE_SPEED_MIN  = 0.3f;
    private static final float TWINKLE_SPEED_MAX  = 2.0f;

    // -------------------------------------------------------------------------
    // CPU-side star data (generated once, reused every frame)
    // -------------------------------------------------------------------------
    private static float[] sCx, sCy, sCz;            // Billboard center positions
    private static float[] sHx, sHy, sHz;            // Horizontal billboard half-vector (right * size)
    private static float[] sVx, sVy, sVz;            // Vertical billboard half-vector (up * size)
    private static float[] sR,  sG,  sB;             // Base RGB color
    private static float[] sBaseAlpha;               // Base (max) alpha for this star
    private static float[] sPhase;                   // Twinkle phase offset [0, 2π]
    private static float[] sSpeed;                   // Twinkle speed (rad/s)
    private static boolean starDataReady = false;

    // Wall-clock origin for smooth time-independent animation
    private static final long TIME_ORIGIN = System.nanoTime();

    // -------------------------------------------------------------------------
    // GPU resources
    // -------------------------------------------------------------------------
    private static VertexBuffer domeBuffer    = null;
    private static boolean      domeBufferBuilt = false;

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Main entry point. Called once per frame while the lunar skybox is active.
     *
     * @param poseStack        Pose stack from renderSky / RenderLevelStageEvent
     * @param projectionMatrix Projection matrix
     * @param partialTick      Partial render tick (unused for animation, kept for future use)
     */
    public static void render(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick) {
        // 1. Clear color hint (non-Iris path)
        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // 2. Ensure one-time initialization
        ensureDomeBuilt();
        ensureStarDataBuilt();

        // 3. Opaque black dome — covers Iris's sky composite output
        renderDome(poseStack, projectionMatrix);

        // 4. Stars — render before Earth so Earth correctly occludes them
        float timeSeconds = (System.nanoTime() - TIME_ORIGIN) / 1_000_000_000.0f;
        renderStars(poseStack, projectionMatrix, timeSeconds);

        // 5. Earth quad on top of stars
        renderEarth(poseStack, projectionMatrix, timeSeconds);
    }

    /**
     * Releases all GPU resources and forces a rebuild on next render.
     * Call this on resource reload or when the renderer is no longer needed.
     */
    public static void invalidate() {
        domeBufferBuilt = false;
        starDataReady   = false;
        if (domeBuffer != null) {
            domeBuffer.close();
            domeBuffer = null;
        }
        // CPU arrays are just garbage-collected; null them to allow GC
        sCx = sCy = sCz = null;
        sHx = sHy = sHz = null;
        sVx = sVy = sVz = null;
        sR  = sG  = sB  = null;
        sBaseAlpha = sPhase = sSpeed = null;
    }

    // =========================================================================
    // Earth rendering (Phase 3c - Day/Night Cycle)
    // =========================================================================

    /**
     * Renders the Earth as a rotating 3D sphere with day/night phases.
     * 
     * To maintain Iris compatibility, we do not use custom shaders. Instead, we
     * generate the sphere mesh dynamically on the CPU each frame.
     * 1. We define a fixed Sun direction in local space.
     * 2. We rotate the texture coordinates (U) over time to spin the globe, 
     *    keeping the mesh and the shadow stationary.
     * 3. Pass 1 (Day): Vertex colors are darkened on the night side.
     * 4. Pass 2 (Night): Additive blend city lights, visible only on the night side.
     */
    private static void renderEarth(PoseStack poseStack, Matrix4f projectionMatrix, float timeSeconds) {
        poseStack.pushPose();

        // 1. Position and orient the globe in the sky
        poseStack.translate(EARTH_DX * SKY_RADIUS, EARTH_DY * SKY_RADIUS, EARTH_DZ * SKY_RADIUS);
        
        float yaw = (float) Math.atan2(EARTH_DX, EARTH_DZ);
        float pitch = (float) Math.asin(EARTH_DY);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotation(yaw));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotation(-pitch));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(-EARTH_AXIAL_TILT));
        
        // Note: We DO NOT rotate the PoseStack over time. The mesh stays static
        // so the sun shadow stays static. We animate the texture UVs instead.

        // --- Render state setup ---
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        
        // We must enable culling to prevent the back of the sphere showing through the front
        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        float uOffset = (timeSeconds * EARTH_ROTATION_SPEED) % 1.0f;

        // --- PASS 1: Daytime Surface ---
        RenderSystem.defaultBlendFunc(); // Normal alpha blend
        Minecraft.getInstance().getTextureManager().bindForSetup(EARTH_TEXTURE);
        RenderSystem.setShaderTexture(0, EARTH_TEXTURE);
        drawSpherePass(poseStack.last().pose(), projectionMatrix, uOffset, false);

        // --- PASS 2: Nighttime City Lights ---
        // Additive blend: GL_ONE, GL_ONE
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE, 
                               com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        Minecraft.getInstance().getTextureManager().bindForSetup(EARTH_NIGHT_TEXTURE);
        RenderSystem.setShaderTexture(0, EARTH_NIGHT_TEXTURE);
        drawSpherePass(poseStack.last().pose(), projectionMatrix, uOffset, true);

        // --- Restore render state ---
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();

        poseStack.popPose();
    }

    /**
     * Dynamically generates and draws a 32x16 sphere mesh with per-vertex lighting.
     */
    private static void drawSpherePass(Matrix4f pose, Matrix4f projection, float uOffset, boolean isNightPass) {
        final int segments = 32;
        final int rings = 16;
        
        // Sun direction (local space, coming exactly from the right)
        // This puts the day/night terminator perfectly in the middle.
        float sunX = 1.0f;
        float sunY = 0.0f;
        float sunZ = 0.0f;

        BufferBuilder bb = Tesselator.getInstance().begin(
            VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR
        );

        for (int r = 0; r < rings; r++) {
            float v0 = (float) r / rings;
            float v1 = (float) (r + 1) / rings;
            float phi0 = (float) Math.PI * v0;
            float phi1 = (float) Math.PI * v1;

            float y0 = (float) Math.cos(phi0);
            float y1 = (float) Math.cos(phi1);
            float r0 = (float) Math.sin(phi0);
            float r1 = (float) Math.sin(phi1);

            for (int s = 0; s < segments; s++) {
                // Base U without offset (for rendering)
                float u0_base = (float) s / segments;
                float u1_base = (float) (s + 1) / segments;
                
                // Texture U with offset (for sampling the equirectangular map)
                float u0 = u0_base + uOffset;
                float u1 = u1_base + uOffset;

                // theta is based on the un-offset segment to keep the mesh static
                float theta0 = (float) (2.0 * Math.PI * u0_base);
                float theta1 = (float) (2.0 * Math.PI * u1_base);

                float x00 = (float) Math.sin(theta0) * r0;
                float z00 = (float) Math.cos(theta0) * r0;
                float x10 = (float) Math.sin(theta1) * r0;
                float z10 = (float) Math.cos(theta1) * r0;

                float x01 = (float) Math.sin(theta0) * r1;
                float z01 = (float) Math.cos(theta0) * r1;
                float x11 = (float) Math.sin(theta1) * r1;
                float z11 = (float) Math.cos(theta1) * r1;

                // Scale
                float S = EARTH_HALF_SIZE;
                
                // Calculate colors for the 4 vertices based on their normal (which is just x,y,z)
                float c01 = getVertexColor(x01, y1, z01, sunX, sunY, sunZ, isNightPass);
                float c11 = getVertexColor(x11, y1, z11, sunX, sunY, sunZ, isNightPass);
                float c10 = getVertexColor(x10, y0, z10, sunX, sunY, sunZ, isNightPass);
                float c00 = getVertexColor(x00, y0, z00, sunX, sunY, sunZ, isNightPass);

                // Quad: BL, BR, TR, TL
                bb.addVertex(x01*S, y1*S, z01*S).setUv(u0, v1).setColor(c01, c01, c01, 1f);
                bb.addVertex(x11*S, y1*S, z11*S).setUv(u1, v1).setColor(c11, c11, c11, 1f);
                bb.addVertex(x10*S, y0*S, z10*S).setUv(u1, v0).setColor(c10, c10, c10, 1f);
                bb.addVertex(x00*S, y0*S, z00*S).setUv(u0, v0).setColor(c00, c00, c00, 1f);
            }
        }

        MeshData mesh = bb.buildOrThrow();
        VertexBuffer tempBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        tempBuffer.bind();
        tempBuffer.upload(mesh);
        tempBuffer.drawWithShader(pose, projection, RenderSystem.getShader());
        VertexBuffer.unbind();
        tempBuffer.close();
    }

    private static float getVertexColor(float nx, float ny, float nz, float sx, float sy, float sz, boolean isNightPass) {
        float dot = nx * sx + ny * sy + nz * sz;
        if (isNightPass) {
            // Night lights appear when facing AWAY from sun (dot < 0)
            // Fade in between dot = 0.1 (twilight) and -0.2 (deep night)
            float night = net.minecraft.util.Mth.clamp((0.1f - dot) / 0.3f, 0f, 1f);
            return night;
        } else {
            // Day texture is fully lit facing sun, very dim on dark side
            float day = net.minecraft.util.Mth.clamp((dot + 0.2f) / 0.3f, 0f, 1f);
            return Math.max(0.02f, day); // Give dark side a tiny bit of ambient visibility
        }
    }

    // =========================================================================
    // Initialization
    // =========================================================================

    /**
     * Builds the static opaque black sky dome (6-face cube at radius 95).
     * Runs once; no rebuild needed unless invalidate() is called.
     */
    private static void ensureDomeBuilt() {
        if (domeBufferBuilt && domeBuffer != null) return;
        if (domeBuffer != null) domeBuffer.close();

        final float R = 95.0f;
        BufferBuilder bb = Tesselator.getInstance().begin(
            VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR
        );

        // +X
        bb.addVertex( R,-R, R).setColor(0f,0f,0f,1f); bb.addVertex( R, R, R).setColor(0f,0f,0f,1f);
        bb.addVertex( R, R,-R).setColor(0f,0f,0f,1f); bb.addVertex( R,-R,-R).setColor(0f,0f,0f,1f);
        // -X
        bb.addVertex(-R,-R,-R).setColor(0f,0f,0f,1f); bb.addVertex(-R, R,-R).setColor(0f,0f,0f,1f);
        bb.addVertex(-R, R, R).setColor(0f,0f,0f,1f); bb.addVertex(-R,-R, R).setColor(0f,0f,0f,1f);
        // +Y
        bb.addVertex(-R, R,-R).setColor(0f,0f,0f,1f); bb.addVertex( R, R,-R).setColor(0f,0f,0f,1f);
        bb.addVertex( R, R, R).setColor(0f,0f,0f,1f); bb.addVertex(-R, R, R).setColor(0f,0f,0f,1f);
        // -Y
        bb.addVertex(-R,-R, R).setColor(0f,0f,0f,1f); bb.addVertex( R,-R, R).setColor(0f,0f,0f,1f);
        bb.addVertex( R,-R,-R).setColor(0f,0f,0f,1f); bb.addVertex(-R,-R,-R).setColor(0f,0f,0f,1f);
        // +Z
        bb.addVertex(-R,-R, R).setColor(0f,0f,0f,1f); bb.addVertex(-R, R, R).setColor(0f,0f,0f,1f);
        bb.addVertex( R, R, R).setColor(0f,0f,0f,1f); bb.addVertex( R,-R, R).setColor(0f,0f,0f,1f);
        // -Z
        bb.addVertex( R,-R,-R).setColor(0f,0f,0f,1f); bb.addVertex( R, R,-R).setColor(0f,0f,0f,1f);
        bb.addVertex(-R, R,-R).setColor(0f,0f,0f,1f); bb.addVertex(-R,-R,-R).setColor(0f,0f,0f,1f);

        domeBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        domeBuffer.bind();
        domeBuffer.upload(bb.buildOrThrow());
        VertexBuffer.unbind();
        domeBufferBuilt = true;
    }

    /**
     * Generates all star data into CPU-side parallel arrays.
     *
     * Runs exactly once. After this, the render loop reads these arrays
     * every frame to build animated quads via Tesselator.
     *
     * Each star stores:
     *   - World-space billboard center (cx, cy, cz)
     *   - Pre-computed right and up half-vectors (hx/hy/hz, vx/vy/vz)
     *   - Base RGB color and maximum alpha
     *   - Twinkle phase offset and speed
     */
    private static void ensureStarDataBuilt() {
        if (starDataReady) return;

        sCx = new float[STAR_COUNT]; sCy = new float[STAR_COUNT]; sCz = new float[STAR_COUNT];
        sHx = new float[STAR_COUNT]; sHy = new float[STAR_COUNT]; sHz = new float[STAR_COUNT];
        sVx = new float[STAR_COUNT]; sVy = new float[STAR_COUNT]; sVz = new float[STAR_COUNT];
        sR  = new float[STAR_COUNT]; sG  = new float[STAR_COUNT]; sB  = new float[STAR_COUNT];
        sBaseAlpha = new float[STAR_COUNT];
        sPhase     = new float[STAR_COUNT];
        sSpeed     = new float[STAR_COUNT];

        Random rng = new Random(STAR_SEED);
        final float TWO_PI = (float)(Math.PI * 2.0);

        for (int i = 0; i < STAR_COUNT; i++) {
            // --- Uniform sphere sampling (Marsaglia rejection method) ---
            float u, v, w;
            do {
                u = rng.nextFloat() * 2f - 1f;
                v = rng.nextFloat() * 2f - 1f;
                w = u * u + v * v;
            } while (w >= 1f);

            float s = 2f * (float) Math.sqrt(1.0 - w);
            float cx = u * s * SKY_RADIUS;
            float cy = v * s * SKY_RADIUS;
            float cz = (2f * w - 1f) * SKY_RADIUS;
            sCx[i] = cx; sCy[i] = cy; sCz[i] = cz;

            // --- Size class: 60% tiny, 30% small, 10% medium ---
            float size;
            int sr = rng.nextInt(100);
            if      (sr < 60) size = SIZE_TINY   + rng.nextFloat() * 0.02f;
            else if (sr < 90) size = SIZE_SMALL  + rng.nextFloat() * 0.04f;
            else              size = SIZE_MEDIUM + rng.nextFloat() * 0.06f;

            // --- Billboard vectors ---
            float nx = cx / SKY_RADIUS;
            float ny = cy / SKY_RADIUS;
            float nz = cz / SKY_RADIUS;

            // Arbitrary "up" reference not parallel to the star normal
            float refX = (Math.abs(ny) < 0.9f) ? 0f : 1f;
            float refY = (Math.abs(ny) < 0.9f) ? 1f : 0f;
            float refZ = 0f;

            // right = normalize(cross(normal, ref))
            float rx = ny * refZ - nz * refY;
            float ry = nz * refX - nx * refZ;
            float rz = nx * refY - ny * refX;
            float rLen = (float) Math.sqrt(rx*rx + ry*ry + rz*rz);
            rx /= rLen; ry /= rLen; rz /= rLen;

            // up = cross(right, normal)
            float ux = ry * nz - rz * ny;
            float uy = rz * nx - rx * nz;
            float uz = rx * ny - ry * nx;

            sHx[i] = rx * size; sHy[i] = ry * size; sHz[i] = rz * size;
            sVx[i] = ux * size; sVy[i] = uy * size; sVz[i] = uz * size;

            // --- Color tier: 55% dim, 35% normal, 10% bright ---
            float[] tier;
            int br = rng.nextInt(100);
            if      (br < 55) tier = TIER_DIM;
            else if (br < 90) tier = TIER_NORMAL;
            else              tier = TIER_BRIGHT;

            sR[i] = tier[0];
            sG[i] = tier[1];
            sB[i] = tier[2];
            sBaseAlpha[i] = tier[3] * (0.85f + rng.nextFloat() * 0.15f);

            // --- Twinkle parameters ---
            sPhase[i] = rng.nextFloat() * TWO_PI;
            sSpeed[i] = TWINKLE_SPEED_MIN + rng.nextFloat() * (TWINKLE_SPEED_MAX - TWINKLE_SPEED_MIN);
        }

        starDataReady = true;
    }

    // =========================================================================
    // Per-frame rendering
    // =========================================================================

    /**
     * Renders the opaque black dome.
     *
     * Fully opaque, no blending — overwrites whatever Iris rendered before this call.
     * Establishes a true void black background for the star layer.
     */
    private static void renderDome(PoseStack poseStack, Matrix4f projectionMatrix) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        domeBuffer.bind();
        domeBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
        VertexBuffer.unbind();
        // State will be overwritten by renderStars — no restore needed here.
    }

    /**
     * Renders 8,000 twinkling star quads via Tesselator (immediate mode).
     *
     * Called every frame. Reads CPU-side star arrays, computes per-star animated
     * alpha using a sine wave keyed to real elapsed time, then emits 4 vertices
     * per star directly to the GPU.
     *
     * Twinkle formula:
     *   alpha = baseAlpha * lerp(TWINKLE_MIN, 1.0, 0.5 * (1 + sin(t * speed + phase)))
     *
     * This means:
     *   - At sin() = -1 → alpha = baseAlpha * TWINKLE_MIN  (dimmest)
     *   - At sin() =  0 → alpha = baseAlpha * midpoint
     *   - At sin() = +1 → alpha = baseAlpha * 1.0          (brightest)
     *
     * @param timeSeconds Elapsed real-world seconds since TIME_ORIGIN
     */
    private static void renderStars(PoseStack poseStack, Matrix4f projectionMatrix, float timeSeconds) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc(); // SRC_ALPHA, ONE_MINUS_SRC_ALPHA
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        // Grab the shader and apply matrices manually — required when using Tesselator
        // with drawWithShader after building a fresh MeshData.
        BufferBuilder builder = Tesselator.getInstance().begin(
            VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR
        );

        final float twinkleRange = 1.0f - TWINKLE_MIN;

        for (int i = 0; i < STAR_COUNT; i++) {
            // --- Animated alpha ---
            // sin() oscillates [-1, 1]; map to [0, 1] via (1 + sin) * 0.5
            float sine     = (float) Math.sin(timeSeconds * sSpeed[i] + sPhase[i]);
            float envelope = TWINKLE_MIN + twinkleRange * (0.5f + 0.5f * sine);
            float alpha    = sBaseAlpha[i] * envelope;

            float r = sR[i], g = sG[i], b = sB[i];
            float cx = sCx[i], cy = sCy[i], cz = sCz[i];
            float hx = sHx[i], hy = sHy[i], hz = sHz[i];
            float vx = sVx[i], vy = sVy[i], vz = sVz[i];

            // 4 corners of the billboard quad
            builder.addVertex(cx - hx - vx, cy - hy - vy, cz - hz - vz).setColor(r, g, b, alpha);
            builder.addVertex(cx + hx - vx, cy + hy - vy, cz + hz - vz).setColor(r, g, b, alpha);
            builder.addVertex(cx + hx + vx, cy + hy + vy, cz + hz + vz).setColor(r, g, b, alpha);
            builder.addVertex(cx - hx + vx, cy - hy + vy, cz - hz + vz).setColor(r, g, b, alpha);
        }

        MeshData mesh = builder.buildOrThrow();

        // Upload and draw in one shot via a temporary VertexBuffer.
        // Tesselator itself can't draw with the shader matrix API in 1.21.1 —
        // we need a VertexBuffer to use drawWithShader().
        VertexBuffer tempBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        tempBuffer.bind();
        tempBuffer.upload(mesh);
        tempBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
        VertexBuffer.unbind();
        tempBuffer.close();

        // Restore render state
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
