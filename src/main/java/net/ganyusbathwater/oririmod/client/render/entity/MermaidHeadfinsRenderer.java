package net.ganyusbathwater.oririmod.client.render.entity;

import net.ganyusbathwater.oririmod.client.render.entity.model.MermaidHeadfinsModel;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidHeadfinsAnimatable;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import top.theillusivec4.curios.api.CuriosApi;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

public class MermaidHeadfinsRenderer extends AbstractPlayerCosmeticRenderer<MermaidHeadfinsAnimatable> {

    public MermaidHeadfinsRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MermaidHeadfinsModel(), MermaidHeadfinsAnimatable.INSTANCE);
    }

    @Override
    public software.bernie.geckolib.util.Color getRenderColor(MermaidHeadfinsAnimatable animatable, float partialTick, int packedLight) {
        if (this.currentEntity instanceof Player player) {
            // Find the item in Curios to get the dye color
            int color = CuriosApi.getCuriosInventory(player).map(inv -> {
                return inv.findFirstCurio(ModItems.MERMAID_SCALE.get()).map(slotResult -> {
                    ItemStack stack = slotResult.stack();
                    net.minecraft.world.item.DyeColor baseColor = stack.get(DataComponents.BASE_COLOR);
                    if (baseColor != null) {
                        return baseColor.getTextureDiffuseColor();
                    }
                    return 0xFFFFFF;
                }).orElse(0xFFFFFF);
            }).orElse(0xFFFFFF);
            
            if (color != 0xFFFFFF) {
                return software.bernie.geckolib.util.Color.ofOpaque(color);
            }
        }
        return super.getRenderColor(animatable, partialTick, packedLight);
    }

    @Override
    protected void applyRotations(MermaidHeadfinsAnimatable animatable, com.mojang.blaze3d.vertex.PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        // No-op because the RenderLayer ALREADY applies the head's yaw and pitch via PoseStack!
    }
}
