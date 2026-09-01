package net.ganyusbathwater.oririmod.client.render.item;

import net.ganyusbathwater.oririmod.client.render.model.ElementalChoirItemModel;
import net.ganyusbathwater.oririmod.item.custom.ElementalChoirItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ElementalChoirItemRenderer extends GeoItemRenderer<ElementalChoirItem> {
    public static net.minecraft.world.entity.player.Player currentRenderEntity;
    
    public ElementalChoirItemRenderer() {
        super(new ElementalChoirItemModel());
    }

    @Override
    public void renderByItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext transformType, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // Hide vanilla hand rendering because we render the swords around the body in events
        if (transformType == net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || 
            transformType == net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND ||
            transformType == net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ||
            transformType == net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return;
        }
        super.renderByItem(stack, transformType, poseStack, buffer, packedLight, packedOverlay);
    }
}
