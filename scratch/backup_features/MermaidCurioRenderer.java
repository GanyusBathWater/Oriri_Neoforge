package net.ganyusbathwater.oririmod.client.render.entity;

import top.theillusivec4.curios.api.client.ICurioRenderer;
import top.theillusivec4.curios.api.SlotContext;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.PlayerModel;
import software.bernie.geckolib.cache.object.GeoBone;
import net.ganyusbathwater.oririmod.event.CosmeticPlayerRenderEventHandler;

public class MermaidCurioRenderer implements ICurioRenderer {
    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack matrixStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource renderTypeBuffer, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (slotContext.entity() instanceof AbstractClientPlayer player && CosmeticPlayerRenderEventHandler.MERMAID_COSMETIC_RENDERER != null) {
            
            // Sync bones
            if (renderLayerParent.getModel() instanceof PlayerModel<?> playerModel) {
                GeoBone leftLeg = CosmeticPlayerRenderEventHandler.MERMAID_COSMETIC_RENDERER.getGeoModel().getAnimationProcessor().getBone("left_leg");
                if (leftLeg != null) {
                    leftLeg.setRotX(playerModel.leftLeg.xRot);
                    leftLeg.setRotY(playerModel.leftLeg.yRot);
                    leftLeg.setRotZ(playerModel.leftLeg.zRot);
                }
                
                GeoBone rightLeg = CosmeticPlayerRenderEventHandler.MERMAID_COSMETIC_RENDERER.getGeoModel().getAnimationProcessor().getBone("right_leg");
                if (rightLeg != null) {
                    rightLeg.setRotX(playerModel.rightLeg.xRot);
                    rightLeg.setRotY(playerModel.rightLeg.yRot);
                    rightLeg.setRotZ(playerModel.rightLeg.zRot);
                }
                
                GeoBone body = CosmeticPlayerRenderEventHandler.MERMAID_COSMETIC_RENDERER.getGeoModel().getAnimationProcessor().getBone("body");
                if (body != null) {
                    body.setRotX(playerModel.body.xRot);
                    body.setRotY(playerModel.body.yRot);
                    body.setRotZ(playerModel.body.zRot);
                }
            }
            
            // The matrixStack provided by Curios is already transformed with swimming, sneaking, scaling etc!
            // However, Vanilla applies scale(-1, -1, 1) and translate(0, -1.501, 0) to its model.
            // GeckoLib expects an unscaled coordinate system where feet are at Y=0.
            // So we undo Vanilla's scale and translate, render the GeckoLib model, and let it pass through the matrix.
            matrixStack.pushPose();
            matrixStack.translate(0.0D, 1.501D, 0.0D);
            matrixStack.scale(-1.0F, -1.0F, 1.0F);
            
            CosmeticPlayerRenderEventHandler.MERMAID_COSMETIC_RENDERER.render(player, player.yBodyRot, partialTicks, matrixStack, renderTypeBuffer, light);
            
            matrixStack.popPose();
        }
    }
}
