package net.ganyusbathwater.oririmod.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.block.entity.TeleporterBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class TeleporterEmissiveLayer extends GeoRenderLayer<TeleporterBlockEntity> {
    private static final ResourceLocation EMISSIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath("oririmod", "textures/block/teleporter_emissive.png");

    public TeleporterEmissiveLayer(GeoRenderer<TeleporterBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void renderForBone(PoseStack poseStack, TeleporterBlockEntity animatable, software.bernie.geckolib.cache.object.GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        // We override render instead of renderForBone to apply to the whole model
    }

    @Override
    public void render(PoseStack poseStack, TeleporterBlockEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        // Use additive rendering for emissive glow
        RenderType emissiveRenderType = RenderType.eyes(EMISSIVE_TEXTURE);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(emissiveRenderType);
        
        int color = animatable.getBaseColor() != -1 ? animatable.getBaseColor() | 0xFF000000 : 0xFFFFFFFF;
        
        // Re-render the entire model using the emissive texture and additive blending
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, emissiveRenderType, vertexConsumer, partialTick, 15728880, packedOverlay, color);
    }
}
