package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.CraterWorkerEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class CraterWorkerRenderer extends EntityRenderer<CraterWorkerEntity> {
    private static final ResourceLocation TEXTURE_BASE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/explosion_mushroom.png");
    private static final ResourceLocation TEXTURE_GLOW = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/explosion_mushroom_glow.png");
    private static final ResourceLocation TEXTURE_SMOKE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/explosion_smoke.png");
    
    private static final RenderType RENDER_TYPE_BASE = RenderType.entityTranslucentCull(TEXTURE_BASE);
    private static final RenderType RENDER_TYPE_GLOW = RenderType.entityTranslucentCull(TEXTURE_GLOW);
    private static final RenderType RENDER_TYPE_SMOKE = RenderType.entityTranslucentCull(TEXTURE_SMOKE);

    public CraterWorkerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CraterWorkerEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int carveTicks = entity.getMaxTicks();
        int lifeTime = carveTicks + 100;
        float currentTick = entity.tickCount + partialTick;
        float progress = Math.min(1.0f, currentTick / lifeTime);
        
        if (progress <= 0 || progress >= 1.0f) {
            return;
        }

        float growProgress = Math.min(1.0f, currentTick / 20.0f);
        float scaleProgress = (float) (1.0 - Math.pow(1.0 - growProgress, 3)); 
        float targetScale = entity.getRadius() * 1.5f; 
        float scale = targetScale * scaleProgress;
        
        float alpha = 1.0f;
        float fadeStart = lifeTime - 40.0f;
        if (currentTick > fadeStart) {
            alpha = Math.max(0.0f, 1.0f - ((currentTick - fadeStart) / 40.0f));
        }
        int a = (int) (alpha * 255.0f);

        poseStack.pushPose();
        
        VertexConsumer consumerBase = buffer.getBuffer(RENDER_TYPE_BASE);
        VertexConsumer consumerGlow = buffer.getBuffer(RENDER_TYPE_GLOW);
        VertexConsumer consumerSmoke = buffer.getBuffer(RENDER_TYPE_SMOKE);

        // --- 1. RENDER MAIN MUSHROOM CLOUD ---
        poseStack.pushPose();
        // Shift the mushroom down so its base stem rests on the crater floor
        float craterFloorY = -entity.getRadius() * 0.6f;
        poseStack.translate(0.0, craterFloorY + (scale * 0.5f), 0.0);
        poseStack.scale(scale, scale, scale);

        // Render rigid symmetric planes for the main mushroom texture
        renderCrossPlanes(poseStack, consumerBase, packedLight, 255, 255, 255, a);

        // Render glow layer cleanly (static brightness) so it fades out smoothly without popping
        renderCrossPlanes(poseStack, consumerGlow, LightTexture.FULL_BRIGHT, 255, 255, 255, a);
        
        poseStack.popPose();

        // --- 2. RENDER GIANT GROUND SMOKE PUFFS ---
        poseStack.pushPose();
        // Translate base of all smoke to the crater floor
        poseStack.translate(0.0, craterFloorY, 0.0);

        java.util.Random random = new java.util.Random(entity.getId() * 31L);
        // Scale number of smoke clouds directly by the size of the crater
        int numPuffs = (int)(entity.getRadius() * 1.5f);
        
        for (int i = 0; i < numPuffs; i++) {
            poseStack.pushPose();
            
            float angle = random.nextFloat() * (float)Math.PI * 2.0f;
            // Distribute evenly across the crater floor
            float dist = (float)Math.sqrt(random.nextFloat()) * entity.getRadius();
            
            // Random height slightly varying from the floor
            float puffY = random.nextFloat() * (entity.getRadius() * 0.2f);
            poseStack.translate(dist * Math.cos(angle), puffY, dist * Math.sin(angle));
            
            // Giant scale relative to the radius (baseline 40)
            float radiusRatio = entity.getRadius() / 40.0f;
            float puffScale = (10.0f + random.nextFloat() * 10.0f) * radiusRatio * scaleProgress;
            poseStack.scale(puffScale, puffScale, puffScale);
            
            // Dark gray/black color to look like thick ash
            int shade = 10 + random.nextInt(25);
            
            float rotSpeed = (random.nextFloat() * 2.0f) - 1.0f; // Ranges from -1.0 to 1.0
            
            // 1. Draw a perfectly flat horizontal plane that only spins (no tumbling) to ensure ground cover
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(currentTick * rotSpeed * 2.5f));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0f));
            renderSinglePlane(poseStack.last().pose(), consumerSmoke, packedLight, shade, shade, shade, a);
            poseStack.popPose();
            
            // 2. Apply slow, dynamic tumbling over time to the volumetric part of the puff
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(currentTick * rotSpeed * 2.5f));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(currentTick * rotSpeed * 1.0f));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(currentTick * rotSpeed * 1.5f));
            
            // Render 3 chaotic planes per puff for volumetric thickness using the SMOKE texture
            renderChaoticVolume(poseStack, consumerSmoke, packedLight, shade, shade, shade, a, entity.getId() + i, 3);
            
            poseStack.popPose();
        }
        poseStack.popPose();

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderCrossPlanes(PoseStack poseStack, VertexConsumer consumer, int packedLight, int r, int g, int b, int alpha) {
        for(int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(i * 45.0f));
            renderSinglePlane(poseStack.last().pose(), consumer, packedLight, r, g, b, alpha);
            poseStack.popPose();
        }
    }

    private void renderChaoticVolume(PoseStack poseStack, VertexConsumer consumer, int packedLight, int r, int g, int b, int alpha, int seed, int numPlanes) {
        java.util.Random rand = new java.util.Random(seed);
        for(int i = 0; i < numPlanes; i++) {
            poseStack.pushPose();
            
            // Random chaotic tilt and rotation
            float rotX = (rand.nextFloat() - 0.5f) * 60.0f; 
            float rotY = rand.nextFloat() * 180.0f;
            float rotZ = (rand.nextFloat() - 0.5f) * 60.0f; 
            
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rotX));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotY));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotZ));
            
            renderSinglePlane(poseStack.last().pose(), consumer, packedLight, r, g, b, alpha);
            
            poseStack.popPose();
        }
    }

    private void renderSinglePlane(Matrix4f pose, VertexConsumer consumer, int packedLight, int r, int g, int b, int alpha) {
        float hw = 0.5f; 
        float hh = 0.5f; 
        
        // Front side
        consumer.addVertex(pose, -hw, -hh, 0).setColor(r, g, b, alpha).setUv(0.0f, 1.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(pose, hw, -hh, 0).setColor(r, g, b, alpha).setUv(1.0f, 1.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(pose, hw, hh, 0).setColor(r, g, b, alpha).setUv(1.0f, 0.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(pose, -hw, hh, 0).setColor(r, g, b, alpha).setUv(0.0f, 0.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
        
        // Back side
        consumer.addVertex(pose, -hw, -hh, 0).setColor(r, g, b, alpha).setUv(0.0f, 1.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(pose, -hw, hh, 0).setColor(r, g, b, alpha).setUv(0.0f, 0.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(pose, hw, hh, 0).setColor(r, g, b, alpha).setUv(1.0f, 0.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(pose, hw, -hh, 0).setColor(r, g, b, alpha).setUv(1.0f, 1.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(CraterWorkerEntity entity) {
        return TEXTURE_BASE;
    }
}
