package net.ganyusbathwater.oririmod.client.render.block;

import net.ganyusbathwater.oririmod.block.entity.TeleporterBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import net.minecraft.world.item.DyeColor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

public class TeleporterBlockEntityRenderer extends GeoBlockRenderer<TeleporterBlockEntity> {
    
    public TeleporterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new TeleporterModel());
        this.addRenderLayer(new TeleporterEmissiveLayer(this));
        this.addRenderLayer(new TeleporterDebrisLayer(this));
    }

    @Override
    public void render(TeleporterBlockEntity animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (animatable.getCurrentState() == TeleporterBlockEntity.TeleporterState.UNLINKED) {
            return;
        }

        net.minecraft.world.level.block.state.BlockState state = animatable.getBlockState();
        int rotation = 0;
        if (state.hasProperty(net.ganyusbathwater.oririmod.block.custom.TeleporterBlock.ROTATION)) {
            rotation = state.getValue(net.ganyusbathwater.oririmod.block.custom.TeleporterBlock.ROTATION);
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-rotation * 45.0f));
        poseStack.translate(-0.5, 0.0, -0.5);

        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        
        poseStack.popPose();
    }

    @Override
    public Color getRenderColor(TeleporterBlockEntity animatable, float partialTick, int packedLight) {
        if (animatable.getBaseColor() != -1) {
            return Color.ofOpaque(animatable.getBaseColor()); // ABGR packed color
        }
        return super.getRenderColor(animatable, partialTick, packedLight);
    }
}
