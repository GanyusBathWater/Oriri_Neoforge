package net.ganyusbathwater.oririmod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.entity.model.LamiaEarsModel;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticRenderer;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.LamiaEarsAnimatable;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.item.custom.cosmetic.LamiaScaleItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import top.theillusivec4.curios.api.CuriosApi;

public class LamiaEarsRenderer extends AbstractPlayerCosmeticRenderer<LamiaEarsAnimatable> {

    public static final ResourceLocation SCALES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/lamia_essence_ears_scales.png");

    public LamiaEarsRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LamiaEarsModel(), LamiaEarsAnimatable.INSTANCE, true);
        this.addRenderLayer(new LamiaEarsScalesLayer(this));
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

    private static class LamiaEarsScalesLayer extends GeoRenderLayer<LamiaEarsAnimatable> {
        private final LamiaEarsRenderer earsRenderer;

        public LamiaEarsScalesLayer(LamiaEarsRenderer renderer) {
            super(renderer);
            this.earsRenderer = renderer;
        }

        @Override
        public void render(PoseStack poseStack, LamiaEarsAnimatable animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource,
                           VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            int color = this.earsRenderer.getScaleColor() | 0xFF000000;

            RenderType tintedRender = RenderType.entityCutoutNoCull(SCALES_TEXTURE);
            VertexConsumer tintedBuffer = bufferSource.getBuffer(tintedRender);

            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    tintedRender, tintedBuffer, partialTick, packedLight, packedOverlay, color);
        }
    }
}
