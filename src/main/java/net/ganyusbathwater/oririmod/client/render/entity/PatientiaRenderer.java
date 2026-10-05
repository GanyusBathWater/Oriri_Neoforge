package net.ganyusbathwater.oririmod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ganyusbathwater.oririmod.client.render.entity.model.PatientiaModel;
import net.ganyusbathwater.oririmod.entity.custom.PatientiaEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PatientiaRenderer extends GeoEntityRenderer<PatientiaEntity> {
    public PatientiaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PatientiaModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public void preRender(PoseStack poseStack, PatientiaEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
        
        GeoBone head = this.getGeoModel().getAnimationProcessor().getBone("head");
        if (head != null) {
            head.setTrackingMatrices(true);
        }
    }
}
