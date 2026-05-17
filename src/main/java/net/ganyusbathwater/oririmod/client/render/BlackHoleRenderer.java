package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ganyusbathwater.oririmod.client.model.BlackHoleGeckoModel;
import net.ganyusbathwater.oririmod.entity.custom.BlackHoleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class BlackHoleRenderer extends GeoEntityRenderer<BlackHoleEntity> {

    public BlackHoleRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BlackHoleGeckoModel());
    }

    @Override
    public RenderType getRenderType(BlackHoleEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void preRender(PoseStack poseStack, BlackHoleEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        float currentRadius = animatable.getCurrentRadius();
        float maxRadius = animatable.getMaxRadius();
        
        // Visual size doesn't increase that much.
        // When currentRadius grows from 0 to 5, visualScale goes from 0 to 1.5 roughly.
        // It should still shrink to 0 at the end.
        float visualScale = (currentRadius / maxRadius) * 1.5f;

        poseStack.scale(visualScale, visualScale, visualScale);

        // Optional: Tint the entire model black
        int pureBlack = 0xFF000000;

        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, pureBlack);
    }

    @Override
    protected int getBlockLightLevel(BlackHoleEntity entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }
}
