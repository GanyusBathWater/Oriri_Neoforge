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

public class TeleporterDebrisLayer extends GeoRenderLayer<TeleporterBlockEntity> {
    private static final ResourceLocation DEBRIS_TEXTURE = ResourceLocation.fromNamespaceAndPath("oririmod", "textures/entity/teleporter_debrie.png");

    public TeleporterDebrisLayer(GeoRenderer<TeleporterBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void renderForBone(PoseStack poseStack, TeleporterBlockEntity animatable, software.bernie.geckolib.cache.object.GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
    }

    @Override
    public void render(PoseStack poseStack, TeleporterBlockEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        RenderType debrisRenderType = RenderType.entityCutoutNoCull(DEBRIS_TEXTURE);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(debrisRenderType);
        
        // Re-render the entire model using the debris texture
        // Debris is not colored by baseColor, it's just standard rendering
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, debrisRenderType, vertexConsumer, partialTick, packedLight, packedOverlay, 0xFFFFFFFF);
    }
}
