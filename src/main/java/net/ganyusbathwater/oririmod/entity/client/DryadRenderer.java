package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.DryadEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DryadRenderer extends GeoEntityRenderer<DryadEntity> {
    public DryadRenderer(EntityRendererProvider.Context context) {
        super(context, new DryadModel());
    }

    @Override
    public ResourceLocation getTextureLocation(DryadEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/dryad.png");
    }

    @Override
    public void render(DryadEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        // Adjust scale if necessary, typically Dryad is human-sized so 1.0f is fine.
        poseStack.scale(1.0f, 1.0f, 1.0f);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
