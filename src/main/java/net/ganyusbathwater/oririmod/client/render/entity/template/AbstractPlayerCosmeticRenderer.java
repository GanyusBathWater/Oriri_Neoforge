package net.ganyusbathwater.oririmod.client.render.entity.template;

import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

public abstract class AbstractPlayerCosmeticRenderer<T extends AbstractPlayerCosmeticAnimatable> extends GeoReplacedEntityRenderer<AbstractClientPlayer, T> {

    public AbstractPlayerCosmeticRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model, T animatable) {
        super(renderManager, model, animatable);
        
        addRenderLayer(new software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer<>(this) {
            @Override
            public void renderForBone(com.mojang.blaze3d.vertex.PoseStack poseStack, T animatable, software.bernie.geckolib.cache.object.GeoBone bone, net.minecraft.client.renderer.RenderType renderType, net.minecraft.client.renderer.MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (((AbstractPlayerCosmeticRenderer<?>) this.getRenderer()).getCurrentEntity() instanceof net.minecraft.world.entity.player.Player player) {
                    boolean isRight = bone.getName().equals("right_item") || bone.getName().equals("rightItem") || bone.getName().equals("bipedRightItem");
                    boolean isLeft = bone.getName().equals("left_item") || bone.getName().equals("leftItem") || bone.getName().equals("bipedLeftItem");
                    
                    if (!isRight && !isLeft) return;
                    
                    net.minecraft.world.item.ItemStack stack = isRight ? 
                        (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? player.getMainHandItem() : player.getOffhandItem()) :
                        (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? player.getMainHandItem() : player.getOffhandItem());
                        
                    if (stack.isEmpty()) return;
                    
                    poseStack.pushPose();
                    
                    software.bernie.geckolib.util.RenderUtil.translateToPivotPoint(poseStack, bone);
                    poseStack.scale(-1.0F, -1.0F, 1.0F);
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
                    poseStack.translate((isLeft ? -1 : 1) / 16.0F, 0.125F, 0.0F);
                    
                    net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(
                        player,
                        stack,
                        isLeft ? net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND : net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        isLeft,
                        poseStack,
                        bufferSource,
                        player.level(),
                        packedLight,
                        packedOverlay,
                        player.getId()
                    );
                    
                    poseStack.popPose();
                }
            }
        });
    }

    @Override
    protected void applyRotations(T animatable, com.mojang.blaze3d.vertex.PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        // Do NOT call super.applyRotations()!
        // GeoReplacedEntityRenderer internally fetches the entity's yBodyRot and applies it.
        // Since we are rendering inside a Vanilla RenderLayer, the PoseStack ALREADY has:
        // - yBodyRot
        // - Swimming pitch
        // - Fall flying pitch
        // - Death rotation
        // If we apply them again here, the cosmetic model will spin twice as fast as the player!

        if (this.currentEntity instanceof net.minecraft.world.entity.player.Player player) {
            float swimAmount = player.getSwimAmount(partialTick);
            
            if (swimAmount > 0.0F && !player.isFallFlying()) {
                // We ONLY need the local space translation to slide the Mermaid tail down the spine!
                // The actual pitch rotation is already handled by the Vanilla PoseStack!
                poseStack.translate(0.0F, -1.0F * swimAmount, 0.0F);
            }
        }
    }
}
