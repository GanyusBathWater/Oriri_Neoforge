package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.LamiaEntity;
import net.ganyusbathwater.oririmod.client.physics.TailPhysicsController;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class LamiaRenderer extends GeoEntityRenderer<LamiaEntity> {

    public static final ResourceLocation HAIR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/lamia_hair.png");
    public static final ResourceLocation SCALES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/lamia_scales.png");

    public LamiaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LamiaModel());
        this.shadowRadius = 0.8F;
        this.addRenderLayer(new LamiaHairLayer(this));
        this.addRenderLayer(new LamiaScalesLayer(this));
    }

    @Override
    public boolean shouldRender(LamiaEntity animatable, Frustum frustum, double camX, double camY, double camZ) {
        if (!animatable.shouldRender(camX, camY, camZ)) {
            return false;
        }
        if (animatable.noCulling) {
            return true;
        }

        AABB cullingBox = animatable.getBoundingBoxForCulling();
        TailPhysicsController.TailSimulation sim = TailPhysicsController.getOrCreateSimulation(animatable);
        if (sim != null && sim.initialized) {
            double minX = cullingBox.minX;
            double minY = cullingBox.minY;
            double minZ = cullingBox.minZ;
            double maxX = cullingBox.maxX;
            double maxY = cullingBox.maxY;
            double maxZ = cullingBox.maxZ;

            for (int i = 0; i < TailPhysicsController.NUM_POINTS; i += 2) {
                double px = sim.px[i];
                double py = sim.py[i];
                double pz = sim.pz[i];
                if (!Double.isNaN(px) && !Double.isInfinite(px)) {
                    if (px < minX) minX = px;
                    if (px > maxX) maxX = px;
                }
                if (!Double.isNaN(py) && !Double.isInfinite(py)) {
                    if (py < minY) minY = py;
                    if (py > maxY) maxY = py;
                }
                if (!Double.isNaN(pz) && !Double.isInfinite(pz)) {
                    if (pz < minZ) minZ = pz;
                    if (pz > maxZ) maxZ = pz;
                }
            }
            int lastPt = TailPhysicsController.NUM_POINTS - 1;
            double tipX = sim.px[lastPt];
            double tipY = sim.py[lastPt];
            double tipZ = sim.pz[lastPt];
            if (!Double.isNaN(tipX) && !Double.isInfinite(tipX)) {
                if (tipX < minX) minX = tipX;
                if (tipX > maxX) maxX = tipX;
            }
            if (!Double.isNaN(tipY) && !Double.isInfinite(tipY)) {
                if (tipY < minY) minY = tipY;
                if (tipY > maxY) maxY = tipY;
            }
            if (!Double.isNaN(tipZ) && !Double.isInfinite(tipZ)) {
                if (tipZ < minZ) minZ = tipZ;
                if (tipZ > maxZ) maxZ = tipZ;
            }

            cullingBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }

        return frustum.isVisible(cullingBox.inflate(0.5D));
    }

    private static class LamiaHairLayer extends GeoRenderLayer<LamiaEntity> {
        public LamiaHairLayer(GeoEntityRenderer<LamiaEntity> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(PoseStack poseStack, LamiaEntity animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource,
                           VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            int color = animatable.getHairColor() | 0xFF000000;
            RenderType tintedRender = RenderType.entityCutoutNoCull(HAIR_TEXTURE);
            VertexConsumer tintedBuffer = bufferSource.getBuffer(tintedRender);

            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    tintedRender, tintedBuffer, partialTick, packedLight, packedOverlay, color);
        }
    }

    private static class LamiaScalesLayer extends GeoRenderLayer<LamiaEntity> {
        public LamiaScalesLayer(GeoEntityRenderer<LamiaEntity> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(PoseStack poseStack, LamiaEntity animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource,
                           VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            int color = animatable.getScalesColor() | 0xFF000000;
            RenderType tintedRender = RenderType.entityCutoutNoCull(SCALES_TEXTURE);
            VertexConsumer tintedBuffer = bufferSource.getBuffer(tintedRender);

            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    tintedRender, tintedBuffer, partialTick, packedLight, packedOverlay, color);
        }
    }
}
