package net.ganyusbathwater.oririmod.client.render.entity;

import net.ganyusbathwater.oririmod.client.render.entity.model.LamiaTailModel;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticRenderer;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.LamiaTailAnimatable;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.item.custom.cosmetic.LamiaScaleItem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.util.Color;
import top.theillusivec4.curios.api.CuriosApi;

public class LamiaTailRenderer extends AbstractPlayerCosmeticRenderer<LamiaTailAnimatable> {

    public LamiaTailRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LamiaTailModel(), LamiaTailAnimatable.INSTANCE, true);
    }

    public int getScaleColor() {
        if (this.currentEntity instanceof Player player) {
            return CuriosApi.getCuriosInventory(player).map(inv ->
                    inv.findFirstCurio(ModItems.LAMIA_SCALE.get()).map(slotResult ->
                            LamiaScaleItem.getColor(slotResult.stack())
                    ).orElseGet(() -> {
                        if (player.getMainHandItem().is(ModItems.LAMIA_SCALE.get())) {
                            return LamiaScaleItem.getColor(player.getMainHandItem());
                        } else if (player.getOffhandItem().is(ModItems.LAMIA_SCALE.get())) {
                            return LamiaScaleItem.getColor(player.getOffhandItem());
                        }
                        return LamiaScaleItem.DEFAULT_COLOR;
                    })
            ).orElse(LamiaScaleItem.DEFAULT_COLOR);
        }
        return LamiaScaleItem.DEFAULT_COLOR;
    }

    @Override
    public Color getRenderColor(LamiaTailAnimatable animatable, float partialTick, int packedLight) {
        return Color.ofOpaque(getScaleColor());
    }
}
