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

public class LizardTailCosmeticLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public LizardTailCosmeticLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pRenderer) {
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

        if (!hasCurioEquipped(player, ModItems.LIZARD_SCALE.get())) {
            return;
        }

        if (CosmeticPlayerRenderEventHandler.LIZARD_TAIL_COSMETIC_RENDERER != null) {
            poseStack.pushPose();

            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -1.5F, 0.0F);

            CosmeticPlayerRenderEventHandler.LIZARD_TAIL_COSMETIC_RENDERER.render(player, 180.0F, partialTick, poseStack, buffer, packedLight);

            poseStack.popPose();
        }
    }
}
