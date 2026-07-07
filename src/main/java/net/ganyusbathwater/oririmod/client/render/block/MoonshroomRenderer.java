package net.ganyusbathwater.oririmod.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ganyusbathwater.oririmod.block.custom.MoonshroomBlock;
import net.ganyusbathwater.oririmod.block.entity.MoonshroomBlockEntity;
import net.ganyusbathwater.oririmod.client.render.model.MoonshroomModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import org.joml.Quaternionf;
import org.joml.AxisAngle4f;

public class MoonshroomRenderer extends GeoBlockRenderer<MoonshroomBlockEntity> {
    public MoonshroomRenderer(BlockEntityRendererProvider.Context context) {
        super(new MoonshroomModel());
    }

    @Override
    public void render(MoonshroomBlockEntity animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        Direction facing = animatable.getBlockState().getValue(MoonshroomBlock.FACING);

        // Translate to center for rotation
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotate to match the wall it's attached to
        float rotation = -facing.toYRot() + 180f;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation));

        // Translate back so the base is at the surface of the block
        poseStack.translate(-0.5, -0.5, -0.5);

        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
