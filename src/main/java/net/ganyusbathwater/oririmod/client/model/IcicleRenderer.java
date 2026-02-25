package net.ganyusbathwater.oririmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.entity.IcicleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class IcicleRenderer extends EntityRenderer<IcicleEntity> {

    // Use vanilla blue_ice block texture
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/block/blue_ice.png");
    private static final int WHITE = 0xFFFFFFFF;
    private static final float SCALE = 1.5f;

    private final IcicleModel model;

    public IcicleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new IcicleModel(ctx.bakeLayer(IcicleModel.LAYER_LOCATION));
        this.shadowRadius = 0.3f;
    }

    @Override
    public void render(IcicleEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        poseStack.scale(SCALE, SCALE, SCALE);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, WHITE);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(IcicleEntity entity) {
        return TEXTURE;
    }
}
