package net.ganyusbathwater.oririmod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.CuriosApi;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> {

    private boolean oririmod$shouldHideLegs(LivingEntity livingEntity, EquipmentSlot slot) {
        if (slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
            if (livingEntity instanceof Player player) {
                boolean hasLamia = CuriosApi.getCuriosInventory(player)
                        .map(inv -> inv.findFirstCurio(ModItems.LAMIA_SCALE.get()).isPresent())
                        .orElse(false);
                if (hasLamia) {
                    return true;
                }

                boolean hasMermaid = CuriosApi.getCuriosInventory(player)
                        .map(inv -> inv.findFirstCurio(ModItems.MERMAID_SCALE.get()).isPresent())
                        .orElse(false);
                if (hasMermaid) {
                    return player.isInWater() ||
                            player.isInFluidType((fluidType, height) -> player.canSwimInFluidType(fluidType)) ||
                            player.isVisuallySwimming();
                }
            }
        }
        return false;
    }

    // NeoForge 1.21.1 12-parameter overload
    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void oririmod$hideLegArmorNeoForge(PoseStack poseStack, MultiBufferSource bufferSource, T livingEntity, EquipmentSlot slot, int packedLight, A model, float f, float f1, float f2, float f3, float f4, float f5, CallbackInfo ci) {
        if (oririmod$shouldHideLegs(livingEntity, slot)) {
            ci.cancel();
        }
    }

    // Vanilla 6-parameter overload
    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V", at = @At("HEAD"), cancellable = true)
    private void oririmod$hideLegArmorVanilla(PoseStack poseStack, MultiBufferSource bufferSource, T livingEntity, EquipmentSlot slot, int packedLight, A model, CallbackInfo ci) {
        if (oririmod$shouldHideLegs(livingEntity, slot)) {
            ci.cancel();
        }
    }
}
