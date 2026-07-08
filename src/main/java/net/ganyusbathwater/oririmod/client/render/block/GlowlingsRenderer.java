package net.ganyusbathwater.oririmod.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ganyusbathwater.oririmod.block.entity.GlowlingsBlockEntity;
import net.ganyusbathwater.oririmod.client.render.model.GlowlingsModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import org.joml.Quaternionf;
import org.joml.AxisAngle4f;
import java.util.Random;

public class GlowlingsRenderer extends GeoBlockRenderer<GlowlingsBlockEntity> {
    public GlowlingsRenderer(BlockEntityRendererProvider.Context context) {
        super(new GlowlingsModel());
    }

    @Override
    public void render(GlowlingsBlockEntity animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        long seed = animatable.getBlockPos().asLong();
        Random random = new Random(seed);
        
        int numMushrooms = 2 + random.nextInt(3); // 2 to 4 mushrooms
        
        for (int i = 0; i < numMushrooms; i++) {
            poseStack.pushPose();
            
            // Move to center
            poseStack.translate(0.5, 0, 0.5);
            
            // Random translation relative to center
            float tx = (random.nextFloat() - 0.5f) * 0.75f;
            float tz = (random.nextFloat() - 0.5f) * 0.75f;
            poseStack.translate(tx, 0, tz);
            
            // Random rotation
            float rotY = random.nextFloat() * 360f;
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotY));
            
            // Random scale (increased size slightly)
            float scale = 1.2f + random.nextFloat() * 0.5f;
            poseStack.scale(scale, scale, scale);
            
            // Translate back so GeoBlockRenderer's internal translation is negated
            poseStack.translate(-0.5, 0, -0.5);
            
            super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }
}
