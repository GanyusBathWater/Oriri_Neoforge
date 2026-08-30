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
        if (!hasCurioEquipped(player, ModItems.MERMAID_SCALE.get())) {
            return;
        }

        if (CosmeticPlayerRenderEventHandler.MERMAID_COSMETIC_RENDERER != null) {
            poseStack.pushPose();

            boolean inWater = player.isInWater()
                    || player.isInFluidType((fluidType, height) -> player.canSwimInFluidType(fluidType))
                    || player.isVisuallySwimming();

            if (!inWater) {
                poseStack.popPose();
                return;
            }

            // TAIL MODEL: Physically attach the origin to the Vanilla body bone
            this.getParentModel().body.translateAndRotate(poseStack);

            // Un-invert Vanilla's Y-axis so GeckoLib renders right-side up
            poseStack.scale(1.0F, -1.0F, 1.0F);

            // Apply a static downward translation to align the waists
            poseStack.translate(0.0F, -1.5F, 0.0F);

            // 4. Render the GeckoLib model using the inherited PoseStack
            float entityYaw = net.minecraft.util.Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
            CosmeticPlayerRenderEventHandler.MERMAID_COSMETIC_RENDERER.render(player, entityYaw, partialTick, poseStack,
                    buffer, packedLight);

            poseStack.popPose();
        }
    }
}
