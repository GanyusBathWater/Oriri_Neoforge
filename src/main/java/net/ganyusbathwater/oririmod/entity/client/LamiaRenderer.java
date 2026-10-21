package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.LamiaEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class LamiaRenderer extends GeoEntityRenderer<LamiaEntity> {

    public static final ResourceLocation HAIR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/lamia_hair.png");
    public static final ResourceLocation SCALES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/lamia_scales.png");

    public LamiaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LamiaModel());
        this.shadowRadius = 0.8F;
        this.addRenderLayer(new LamiaHairLayer(this));
        this.addRenderLayer(new LamiaScalesLayer(this));
    }

    private static class LamiaHairLayer extends GeoRenderLayer<LamiaEntity> {
        public LamiaHairLayer(GeoEntityRenderer<LamiaEntity> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(PoseStack poseStack, LamiaEntity animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource,
                           VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            int color = animatable.getHairColor() | 0xFF000000;
            RenderType tintedRender = RenderType.entityCutoutNoCull(HAIR_TEXTURE);
            VertexConsumer tintedBuffer = bufferSource.getBuffer(tintedRender);

            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    tintedRender, tintedBuffer, partialTick, packedLight, packedOverlay, color);
        }
    }

    private static class LamiaScalesLayer extends GeoRenderLayer<LamiaEntity> {
        public LamiaScalesLayer(GeoEntityRenderer<LamiaEntity> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(PoseStack poseStack, LamiaEntity animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource,
                           VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            int color = animatable.getScalesColor() | 0xFF000000;
            RenderType tintedRender = RenderType.entityCutoutNoCull(SCALES_TEXTURE);
            VertexConsumer tintedBuffer = bufferSource.getBuffer(tintedRender);

            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    tintedRender, tintedBuffer, partialTick, packedLight, packedOverlay, color);
        }
    }
}
