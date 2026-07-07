package net.ganyusbathwater.oririmod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.custom.ForcefieldEmitterBlock;
import net.ganyusbathwater.oririmod.client.model.item.ForcefieldEmitterItemModel;
import net.ganyusbathwater.oririmod.item.custom.ForcefieldEmitterBlockItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class ForcefieldEmitterItemRenderer extends GeoItemRenderer<ForcefieldEmitterBlockItem> {
    public ForcefieldEmitterItemRenderer() {
        super(new ForcefieldEmitterItemModel());
        
        addRenderLayer(new GeoRenderLayer<ForcefieldEmitterBlockItem>(this) {
            private final ResourceLocation CRYSTAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/block/forcefield_emitter_crystal.png");

            @Override
            public void render(PoseStack poseStack, ForcefieldEmitterBlockItem animatable, software.bernie.geckolib.cache.object.BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (animatable.getBlock() instanceof ForcefieldEmitterBlock emitter) {
                    RenderType customRenderType = RenderType.entityTranslucentEmissive(CRYSTAL_TEXTURE);
                    VertexConsumer customBuffer = bufferSource.getBuffer(customRenderType);
                    getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, customRenderType, customBuffer, partialTick, packedLight, packedOverlay, emitter.getVariant().getColor());
                }
            }
        });
    }
}
