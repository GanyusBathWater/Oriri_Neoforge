package net.ganyusbathwater.oririmod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.entity.model.KitsuneCosmeticModel;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticRenderer;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.KitsuneCosmeticAnimatable;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.item.component.KitsuneColors;
import net.ganyusbathwater.oririmod.item.component.ModDataComponents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import top.theillusivec4.curios.api.CuriosApi;

public class KitsuneCosmeticRenderer extends AbstractPlayerCosmeticRenderer<KitsuneCosmeticAnimatable> {

    public static final ResourceLocation SECONDARY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/kitsune_cosmetic_secondary.png");

    public KitsuneCosmeticRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new KitsuneCosmeticModel(), KitsuneCosmeticAnimatable.INSTANCE, true);
        this.addRenderLayer(new KitsuneSecondaryLayer(this));
    }

    public KitsuneColors getKitsuneColors() {
        if (this.currentEntity instanceof Player player) {
            return CuriosApi.getCuriosInventory(player).map(inv ->
                    inv.findFirstCurio(ModItems.KITSUNE_BELL.get()).map(slotResult -> {
                        ItemStack stack = slotResult.stack();
                        return stack.getOrDefault(ModDataComponents.KITSUNE_COLORS.get(), KitsuneColors.DEFAULT);
                    }).orElse(KitsuneColors.DEFAULT)
            ).orElse(KitsuneColors.DEFAULT);
        }
        return KitsuneColors.DEFAULT;
    }

    @Override
    public software.bernie.geckolib.util.Color getRenderColor(KitsuneCosmeticAnimatable animatable, float partialTick, int packedLight) {
        int primary = getKitsuneColors().primary();
        return software.bernie.geckolib.util.Color.ofOpaque(primary);
    }

    private static class KitsuneSecondaryLayer extends GeoRenderLayer<KitsuneCosmeticAnimatable> {
        private final KitsuneCosmeticRenderer cosmeticRenderer;

        public KitsuneSecondaryLayer(KitsuneCosmeticRenderer renderer) {
            super(renderer);
            this.cosmeticRenderer = renderer;
        }

        @Override
        public void render(PoseStack poseStack, KitsuneCosmeticAnimatable animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource,
                           VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            int secondary = cosmeticRenderer.getKitsuneColors().secondary();
            int color = secondary | 0xFF000000;

            RenderType tintedRender = RenderType.entityCutoutNoCull(SECONDARY_TEXTURE);
            VertexConsumer tintedBuffer = bufferSource.getBuffer(tintedRender);

            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    tintedRender, tintedBuffer, partialTick, packedLight, packedOverlay, color);
        }
    }
}
