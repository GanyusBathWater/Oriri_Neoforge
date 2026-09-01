package net.ganyusbathwater.oririmod.client.render.entity;

import net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidCosmeticAnimatable;
import net.ganyusbathwater.oririmod.client.render.entity.model.MermaidTailModel;
import net.ganyusbathwater.oririmod.client.render.entity.model.MermaidLandModel;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.model.GeoModel;
import top.theillusivec4.curios.api.CuriosApi;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

public class MermaidCosmeticRenderer extends AbstractPlayerCosmeticRenderer<MermaidCosmeticAnimatable> {

    private final GeoModel<MermaidCosmeticAnimatable> tailModel;
    private final GeoModel<MermaidCosmeticAnimatable> landModel;

    public MermaidCosmeticRenderer(EntityRendererProvider.Context renderManager) {
        // Pass the land model as default, we will override getGeoModel()
        super(renderManager, new MermaidLandModel(), MermaidCosmeticAnimatable.INSTANCE);
        this.landModel = this.getGeoModel();
        this.tailModel = new MermaidTailModel();
    }

    @Override
    public GeoModel<MermaidCosmeticAnimatable> getGeoModel() {
        if (this.currentEntity instanceof Player player) {
            boolean inWater = player.isInWater() || player.isInFluidType((fluidType, height) -> player.canSwimInFluidType(fluidType)) || player.isVisuallySwimming();
            return inWater ? this.tailModel : this.landModel;
        }
        return super.getGeoModel();
    }

    @Override
    public software.bernie.geckolib.util.Color getRenderColor(MermaidCosmeticAnimatable animatable, float partialTick, int packedLight) {
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
    protected void applyRotations(MermaidCosmeticAnimatable animatable, com.mojang.blaze3d.vertex.PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        // No-op because MermaidCosmeticLayer handles the PoseStack completely!
    }
}
