package net.ganyusbathwater.oririmod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.entity.model.SilentNereidHeadfinsModel;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticRenderer;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidHeadfinsAnimatable;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.item.component.ModDataComponents;
import net.ganyusbathwater.oririmod.item.component.SilentNereidColors;
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

public class SilentNereidHeadfinsRenderer extends AbstractPlayerCosmeticRenderer<MermaidHeadfinsAnimatable> {

    public static final ResourceLocation SCALE2_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/silent_nereids_scale2.png");
    public static final ResourceLocation EMISSIVE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/silent_nereids_emissive.png");

    public SilentNereidHeadfinsRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SilentNereidHeadfinsModel(), MermaidHeadfinsAnimatable.INSTANCE, true);
        this.addRenderLayer(new SilentNereidHeadfinsScale2Layer(this));
        this.addRenderLayer(new SilentNereidHeadfinsEmissiveLayer(this));
    }

    public SilentNereidColors getSilentNereidColors() {
        if (this.currentEntity instanceof Player player) {
            return CuriosApi.getCuriosInventory(player).map(inv ->
                    inv.findFirstCurio(ModItems.SILENT_NEREID_SCALE.get()).map(slotResult -> {
                        ItemStack stack = slotResult.stack();
                        return stack.getOrDefault(ModDataComponents.SILENT_NEREID_COLORS.get(), SilentNereidColors.DEFAULT);
                    }).orElse(SilentNereidColors.DEFAULT)
            ).orElse(SilentNereidColors.DEFAULT);
        }
        return SilentNereidColors.DEFAULT;
    }

    @Override
    public Color getRenderColor(MermaidHeadfinsAnimatable animatable, float partialTick, int packedLight) {
        return Color.ofOpaque(getSilentNereidColors().primary());
    }

    private static class SilentNereidHeadfinsScale2Layer extends GeoRenderLayer<MermaidHeadfinsAnimatable> {
        private final SilentNereidHeadfinsRenderer cosmeticRenderer;

        public SilentNereidHeadfinsScale2Layer(SilentNereidHeadfinsRenderer renderer) {
            super(renderer);
            this.cosmeticRenderer = renderer;
        }

        @Override
        public void render(PoseStack poseStack, MermaidHeadfinsAnimatable animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource,
                           VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            int secondary = cosmeticRenderer.getSilentNereidColors().secondary();
            int color = secondary | 0xFF000000;

            RenderType tintedRender = RenderType.entityCutoutNoCull(SCALE2_TEXTURE);
            VertexConsumer tintedBuffer = bufferSource.getBuffer(tintedRender);

            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    tintedRender, tintedBuffer, partialTick, packedLight, packedOverlay, color);
        }
    }

    private static class SilentNereidHeadfinsEmissiveLayer extends GeoRenderLayer<MermaidHeadfinsAnimatable> {
        private final SilentNereidHeadfinsRenderer cosmeticRenderer;

        public SilentNereidHeadfinsEmissiveLayer(SilentNereidHeadfinsRenderer renderer) {
            super(renderer);
            this.cosmeticRenderer = renderer;
        }

        @Override
        public void render(PoseStack poseStack, MermaidHeadfinsAnimatable animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource,
                           VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            int secondary = cosmeticRenderer.getSilentNereidColors().secondary();
            int color = secondary | 0xFF000000;

            RenderType emissiveRenderType = RenderType.entityTranslucentEmissive(EMISSIVE_TEXTURE, false);
            VertexConsumer emissiveBuffer = bufferSource.getBuffer(emissiveRenderType);

            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    emissiveRenderType, emissiveBuffer, partialTick, 15728880, packedOverlay, color);
        }
    }
}
