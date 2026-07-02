package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.RexAraneaEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class RexAraneaRenderer extends GeoEntityRenderer<RexAraneaEntity> {

    private static final ResourceLocation HAIR_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/rex_aranea_hair.png");
    private static final ResourceLocation CLOTHES_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/rex_aranea_clothes.png");
    private static final ResourceLocation EYE_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/rex_aranea_eye.png");

    public RexAraneaRenderer(EntityRendererProvider.Context context) {
        super(context, new RexAraneaModel());
        
        // Hair layer
        this.addRenderLayer(new RexAraneaTintLayer(this, HAIR_TEXTURE, RexAraneaEntity.DATA_HAIR_COLOR));
        // Clothes layer
        this.addRenderLayer(new RexAraneaTintLayer(this, CLOTHES_TEXTURE, RexAraneaEntity.DATA_CLOTHES_COLOR));
        // Glow layer
        this.addRenderLayer(new RexAraneaGlowLayer(this, EYE_TEXTURE));
        // Item layer
        this.addRenderLayer(new RexAraneaItemLayer(this));
    }

    @Override
    public void preRender(PoseStack poseStack, RexAraneaEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        if (!isReRender) {
            GeoBone head = this.getGeoModel().getAnimationProcessor().getBone("head");
            if (head != null) {
                head.setTrackingMatrices(true);
            }
            
            boolean isCeiling = animatable.getEntityData().get(RexAraneaEntity.DATA_IS_CEILING_CLINGING);
            boolean isWall = animatable.getEntityData().get(RexAraneaEntity.DATA_IS_WALL_CLIMBING);
            
            if (isCeiling) {
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                poseStack.translate(0, -animatable.getBbHeight(), 0);
            }
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }



    @Override
    protected float getDeathMaxRotation(RexAraneaEntity animatable) {
        return 0.0F; // Prevent vanilla sideways death rotation
    }

    private static class RexAraneaTintLayer extends GeoRenderLayer<RexAraneaEntity> {
        private final ResourceLocation texture;
        private final net.minecraft.network.syncher.EntityDataAccessor<Integer> dataAccessor;

        public RexAraneaTintLayer(GeoEntityRenderer<RexAraneaEntity> renderer, ResourceLocation texture, net.minecraft.network.syncher.EntityDataAccessor<Integer> dataAccessor) {
            super(renderer);
            this.texture = texture;
            this.dataAccessor = dataAccessor;
        }

        @Override
        public void render(PoseStack poseStack, RexAraneaEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            int color = animatable.getEntityData().get(this.dataAccessor);
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            RenderType tintedRender = RenderType.entityCutoutNoCull(texture);
            VertexConsumer tintedBuffer = bufferSource.getBuffer(tintedRender);

            poseStack.pushPose();
            // Tiny scale to prevent Z-fighting with base model!
            float s = texture.getPath().contains("hair") ? 1.002f : 1.001f;
            poseStack.scale(s, s, s);
            
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, tintedRender, tintedBuffer, partialTick, packedLight, packedOverlay, (int)(r * 255) << 16 | (int)(g * 255) << 8 | (int)(b * 255) | 0xFF000000);
            
            poseStack.popPose();
        }
    }

    private static class RexAraneaGlowLayer extends GeoRenderLayer<RexAraneaEntity> {
        private final ResourceLocation texture;

        public RexAraneaGlowLayer(GeoEntityRenderer<RexAraneaEntity> renderer, ResourceLocation texture) {
            super(renderer);
            this.texture = texture;
        }

        @Override
        public void render(PoseStack poseStack, RexAraneaEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            RenderType glowRender = RenderType.eyes(texture);
            VertexConsumer glowBuffer = bufferSource.getBuffer(glowRender);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glowRender, glowBuffer, partialTick, 0xF00000, packedOverlay, 0xFFFFFFFF);
        }
    }

    private static class RexAraneaItemLayer extends GeoRenderLayer<RexAraneaEntity> {
        public RexAraneaItemLayer(GeoEntityRenderer<RexAraneaEntity> renderer) {
            super(renderer);
        }

        @Override
        public void renderForBone(PoseStack poseStack, RexAraneaEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            if (animatable.getEntityData().get(RexAraneaEntity.DATA_ATTACK_STATE) == RexAraneaEntity.ATTACK_EAT) {
                int eatTimer = animatable.clientEatTimer;
                
                if (bone.getName().equals("lower_left_arm") && eatTimer >= 10 && eatTimer <= 50) {
                    poseStack.pushPose();
                    
                    software.bernie.geckolib.util.RenderUtil.translateAndRotateMatrixForBone(poseStack, bone);
                    
                    // locator offset
                    poseStack.translate(1.0 / 16.0, -5.0 / 16.0, 0);
                    // Translate outwards to avoid head clipping
                    poseStack.translate(0, 0, 0.4);
                    
                    // Scale down the item by half
                    poseStack.scale(0.5F, 0.5F, 0.5F);
                    
                    // Rotate so the item lays flat in the hand, and orient correctly
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
                    
                    ItemStack beef = new ItemStack(net.minecraft.world.item.Items.BEEF);
                    Minecraft.getInstance().getItemRenderer().renderStatic(beef, net.minecraft.world.item.ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource, animatable.level(), 0);
                    poseStack.popPose();
                }
                
                // Particles moved to RexAraneaEntity.tick() to prevent framerate-dependent spawning
            }
            super.renderForBone(poseStack, animatable, bone, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
        }
    }
}
