package net.ganyusbathwater.oririmod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.entity.model.LizardTailCosmeticModel;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticRenderer;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.LizardTailCosmeticAnimatable;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.item.component.LizardTailColors;
import net.ganyusbathwater.oririmod.item.component.ModDataComponents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;
import top.theillusivec4.curios.api.CuriosApi;

public class LizardTailCosmeticRenderer extends AbstractPlayerCosmeticRenderer<LizardTailCosmeticAnimatable> {

    public static final ResourceLocation SECONDARY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/lizard_tail_secondary.png");

    public LizardTailCosmeticRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LizardTailCosmeticModel(), LizardTailCosmeticAnimatable.INSTANCE, true);
        this.addRenderLayer(new LizardTailSecondaryLayer(this));
    }

    public LizardTailColors getLizardTailColors() {
        if (this.currentEntity instanceof Player player) {
            return CuriosApi.getCuriosInventory(player).map(inv ->
                    inv.findFirstCurio(ModItems.LIZARD_SCALE.get()).map(slotResult -> {
                        ItemStack stack = slotResult.stack();
                        return stack.getOrDefault(ModDataComponents.LIZARD_TAIL_COLORS.get(), LizardTailColors.DEFAULT);
                    }).orElse(LizardTailColors.DEFAULT)
            ).orElse(LizardTailColors.DEFAULT);
        }
        return LizardTailColors.DEFAULT;
    }

    @Override
    public Color getRenderColor(LizardTailCosmeticAnimatable animatable, float partialTick, int packedLight) {
        return Color.ofOpaque(getLizardTailColors().primary());
    }

    private static class LizardTailSecondaryLayer extends GeoRenderLayer<LizardTailCosmeticAnimatable> {
        private final LizardTailCosmeticRenderer cosmeticRenderer;

        public LizardTailSecondaryLayer(LizardTailCosmeticRenderer renderer) {
            super(renderer);
            this.cosmeticRenderer = renderer;
        }

        @Override
        public void render(PoseStack poseStack, LizardTailCosmeticAnimatable animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource,
                           VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            int secondary = cosmeticRenderer.getLizardTailColors().secondary();
            int color = secondary | 0xFF000000;

            RenderType tintedRender = RenderType.entityCutoutNoCull(SECONDARY_TEXTURE);
            VertexConsumer tintedBuffer = bufferSource.getBuffer(tintedRender);

            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    tintedRender, tintedBuffer, partialTick, packedLight, packedOverlay, color);
        }
    }
}
