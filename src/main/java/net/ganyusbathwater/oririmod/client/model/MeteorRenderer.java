package net.ganyusbathwater.oririmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.MeteorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class MeteorRenderer extends EntityRenderer<MeteorEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/entity/meteor.png");
    private static final int WHITE = 0xFFFFFFFF;

    private final MeteorModel model;

    public MeteorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new MeteorModel(ctx.bakeLayer(MeteorModel.LAYER_LOCATION));
        this.shadowRadius = 0.7f;
    }

    @Override
    public void render(MeteorEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float scale = entity.getMeteorScale();
        poseStack.scale(scale, scale, scale);

        float spin = (entity.tickCount + partialTicks) * 6.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.XP.rotationDegrees(spin * 0.6f));

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, WHITE);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity entity) {
        return TEXTURE;
    }
}
