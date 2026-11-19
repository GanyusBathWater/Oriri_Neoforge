package net.ganyusbathwater.oririmod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.entity.model.SilentNereidLandModel;
import net.ganyusbathwater.oririmod.client.render.entity.model.SilentNereidTailModel;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticRenderer;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidCosmeticAnimatable;
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
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;
import top.theillusivec4.curios.api.CuriosApi;

public class SilentNereidCosmeticRenderer extends AbstractPlayerCosmeticRenderer<MermaidCosmeticAnimatable> {

    public static final ResourceLocation SCALE2_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/silent_nereids_scale2.png");

    private final GeoModel<MermaidCosmeticAnimatable> tailModel;
    private final GeoModel<MermaidCosmeticAnimatable> landModel;

    public SilentNereidCosmeticRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SilentNereidLandModel(), MermaidCosmeticAnimatable.INSTANCE, true);
        this.landModel = this.getGeoModel();
        this.tailModel = new SilentNereidTailModel();
        this.addRenderLayer(new SilentNereidSecondaryLayer(this));
    }

    public GeoModel<MermaidCosmeticAnimatable> getGeoModel(Player player) {
        boolean inWater = player.isInWater()
                || player.isInFluidType((fluidType, height) -> player.canSwimInFluidType(fluidType))
                || player.isVisuallySwimming();
        return inWater ? this.tailModel : this.landModel;
    }

    @Override
    public GeoModel<MermaidCosmeticAnimatable> getGeoModel() {
        if (this.currentEntity instanceof Player player) {
            return getGeoModel(player);
        }
        return super.getGeoModel();
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
    public Color getRenderColor(MermaidCosmeticAnimatable animatable, float partialTick, int packedLight) {
        return Color.ofOpaque(getSilentNereidColors().primary());
    }

    private static class SilentNereidSecondaryLayer extends GeoRenderLayer<MermaidCosmeticAnimatable> {
        private final SilentNereidCosmeticRenderer cosmeticRenderer;

        public SilentNereidSecondaryLayer(SilentNereidCosmeticRenderer renderer) {
            super(renderer);
            this.cosmeticRenderer = renderer;
        }

        @Override
        public void render(PoseStack poseStack, MermaidCosmeticAnimatable animatable, BakedGeoModel bakedModel,
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
}
