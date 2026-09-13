package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.entity.custom.NoxusCultistEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

public class NoxusCultistRenderer extends GeoEntityRenderer<NoxusCultistEntity> {
    private static final ResourceLocation EMISSIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/noxus_cultist_emissive.png");
    
    private static final Map<Element, Color> ELEMENT_COLORS = new EnumMap<>(Element.class);
    static {
        ELEMENT_COLORS.put(Element.FIRE, new Color(255, 60, 60));
        ELEMENT_COLORS.put(Element.NATURE, new Color(60, 255, 60));
        ELEMENT_COLORS.put(Element.EARTH, new Color(180, 120, 60));
        ELEMENT_COLORS.put(Element.WATER, new Color(60, 120, 255));
        ELEMENT_COLORS.put(Element.LIGHT, new Color(255, 255, 180));
        ELEMENT_COLORS.put(Element.DARKNESS, new Color(160, 60, 255));
        ELEMENT_COLORS.put(Element.TRUE_DAMAGE, new Color(255, 60, 220));
        ELEMENT_COLORS.put(Element.PHYSICAL, Color.WHITE);
    }
    
    private static final Map<Element, ResourceLocation> MAGIC_CIRCLES = new EnumMap<>(Element.class);
    static {
        MAGIC_CIRCLES.put(Element.FIRE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/effect/magic_circles/fire_ground.png"));
        MAGIC_CIRCLES.put(Element.NATURE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/effect/magic_circles/nature_ground.png"));
        MAGIC_CIRCLES.put(Element.EARTH, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/effect/magic_circles/earth_ground.png"));
        MAGIC_CIRCLES.put(Element.WATER, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/effect/magic_circles/water_ground.png"));
        MAGIC_CIRCLES.put(Element.LIGHT, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/effect/magic_circles/light_ground.png"));
        MAGIC_CIRCLES.put(Element.DARKNESS, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/effect/magic_circles/darkness_ground.png"));
    }

    public NoxusCultistRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NoxusCultistModel());
        
        this.addRenderLayer(new CultistEmissiveLayer(this));
        this.addRenderLayer(new CultistMagicCircleLayer(this));
    }

    private static class CultistEmissiveLayer extends GeoRenderLayer<NoxusCultistEntity> {
        public CultistEmissiveLayer(GeoEntityRenderer<NoxusCultistEntity> entityRendererIn) {
            super(entityRendererIn);
        }

        @Override
        public void render(PoseStack poseStack, NoxusCultistEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            Color color = ELEMENT_COLORS.getOrDefault(animatable.getElement(), Color.WHITE);
            int packedColor = color.getRGB(); // ARGB

            RenderType emissiveRenderType = RenderType.entityTranslucentEmissive(EMISSIVE_TEXTURE);
            VertexConsumer emissiveBuffer = bufferSource.getBuffer(emissiveRenderType);

            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, emissiveRenderType,
                    emissiveBuffer, partialTick, 15728880, packedOverlay, packedColor);
        }
    }

    private static class CultistMagicCircleLayer extends GeoRenderLayer<NoxusCultistEntity> {
        public CultistMagicCircleLayer(GeoEntityRenderer<NoxusCultistEntity> entityRendererIn) {
            super(entityRendererIn);
        }

        @Override
        public void render(PoseStack poseStack, NoxusCultistEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            if (animatable.isAttacking() && animatable.attackTick >= 5) {
                GeoBone magicCircleBone = bakedModel.getBone("magic_circle_ancor").orElse(null);
                if (magicCircleBone != null) {
                    ResourceLocation circleTexture = MAGIC_CIRCLES.get(animatable.getElement());
                    if (circleTexture != null) {
                        poseStack.pushPose();
                        
                        // Scale up over time
                        float progress = Math.min((animatable.attackTick + partialTick - 5) / 15.0f, 1.0f);
                        float scale = progress * 2.0f; // max scale 2.0
                        
                        // Navigate directly to the bone's world transform
                        software.bernie.geckolib.util.RenderUtil.translateAndRotateMatrixForBone(poseStack, magicCircleBone);
                        
                        // Move the circle slightly forward relative to the bone if it's still rendering behind.
                        // Minecraft's Z is inverted in Geckolib models. -Z is usually forward.
                        // We also flip it around so it faces the right way.
                        poseStack.translate(0, 0, -0.1f);
                        
                        // Spin effect
                        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((animatable.tickCount + partialTick) * 10));
                        poseStack.scale(scale, scale, scale);

                        RenderType circleRenderType = RenderType.entityTranslucentEmissive(circleTexture);
                        VertexConsumer vc = bufferSource.getBuffer(circleRenderType);
                        
                        Color color = ELEMENT_COLORS.getOrDefault(animatable.getElement(), Color.WHITE);
                        
                        org.joml.Matrix4f poseMat = poseStack.last().pose();
                        
                        // Draw quad
                        vc.addVertex(poseMat, -0.5F, -0.5F, 0.0F).setColor(color.getRed(), color.getGreen(), color.getBlue(), 255).setUv(0, 1).setOverlay(packedOverlay).setLight(15728880).setNormal(0, 0, -1);
                        vc.addVertex(poseMat, 0.5F, -0.5F, 0.0F).setColor(color.getRed(), color.getGreen(), color.getBlue(), 255).setUv(1, 1).setOverlay(packedOverlay).setLight(15728880).setNormal(0, 0, -1);
                        vc.addVertex(poseMat, 0.5F, 0.5F, 0.0F).setColor(color.getRed(), color.getGreen(), color.getBlue(), 255).setUv(1, 0).setOverlay(packedOverlay).setLight(15728880).setNormal(0, 0, -1);
                        vc.addVertex(poseMat, -0.5F, 0.5F, 0.0F).setColor(color.getRed(), color.getGreen(), color.getBlue(), 255).setUv(0, 0).setOverlay(packedOverlay).setLight(15728880).setNormal(0, 0, -1);
                        
                        poseStack.popPose();
                    }
                }
            }
        }
    }
}
