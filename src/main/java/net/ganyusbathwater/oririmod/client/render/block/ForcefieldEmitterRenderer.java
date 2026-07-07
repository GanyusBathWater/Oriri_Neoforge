package net.ganyusbathwater.oririmod.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.entity.ForcefieldEmitterBlockEntity;
import net.ganyusbathwater.oririmod.client.model.block.ForcefieldEmitterModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class ForcefieldEmitterRenderer extends GeoBlockRenderer<ForcefieldEmitterBlockEntity> {
    public ForcefieldEmitterRenderer() {
        super(new ForcefieldEmitterModel());
        
        // Add layer for the colored crystal
        addRenderLayer(new GeoRenderLayer<ForcefieldEmitterBlockEntity>(this) {
            private final ResourceLocation CRYSTAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/block/forcefield_emitter_crystal.png");

            @Override
            public void render(PoseStack poseStack, ForcefieldEmitterBlockEntity animatable, software.bernie.geckolib.cache.object.BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (!animatable.isActive() && animatable.getVariant() != net.ganyusbathwater.oririmod.block.custom.ForcefieldVariant.REPELLENT) {
                    return; // Don't render glowing crystal if inactive (unless Repellent)
                }
                
                RenderType customRenderType = RenderType.entityTranslucentEmissive(CRYSTAL_TEXTURE);
                VertexConsumer customBuffer = bufferSource.getBuffer(customRenderType);
                
                getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, customRenderType, customBuffer, partialTick, packedLight, packedOverlay, animatable.getVariant().getColor());
            }
        });
    }

    @Override
    public void actuallyRender(PoseStack poseStack, ForcefieldEmitterBlockEntity animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
