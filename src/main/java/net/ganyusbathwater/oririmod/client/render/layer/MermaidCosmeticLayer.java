package net.ganyusbathwater.oririmod.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ganyusbathwater.oririmod.event.CosmeticPlayerRenderEventHandler;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

public class MermaidCosmeticLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public MermaidCosmeticLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pRenderer) {
        super(pRenderer);
    }

    private boolean hasCurioEquipped(Player player, Item item) {
        return CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findFirstCurio(item).isPresent())
                .orElse(false);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        if (player.isSpectator()) return;
        
        if (!hasCurioEquipped(player, ModItems.MERMAID_SCALE.get())) {
            return;
        }

        boolean inWater = player.isInWater() || player.isInFluidType((fluidType, height) -> player.canSwimInFluidType(fluidType)) || player.isVisuallySwimming();
        
        if (CosmeticPlayerRenderEventHandler.MERMAID_COSMETIC_RENDERER != null) {
            // Both TAIL MODEL and LAND MODEL must be rendered at the root!
            // If we translate to the body, the arm fins (which are root-level bones) will inherit the body's rotation and detach from the vanilla arms.
            poseStack.pushPose();
            
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -1.5F, 0.0F);
            
            float entityYaw = inWater ? net.minecraft.util.Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) : 180.0F;
            CosmeticPlayerRenderEventHandler.MERMAID_COSMETIC_RENDERER.render(player, entityYaw, partialTick, poseStack, buffer, packedLight);
            
            poseStack.popPose();
        }
        
        if (CosmeticPlayerRenderEventHandler.MERMAID_HEADFINS_RENDERER != null) {
            // HEADFINS MODEL (renders on both Land and Water)
            poseStack.pushPose();
            
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -1.5F, 0.0F);
            
            CosmeticPlayerRenderEventHandler.MERMAID_HEADFINS_RENDERER.render(player, 180.0F, partialTick, poseStack, buffer, packedLight);
            
            poseStack.popPose();
        }
    }
}
