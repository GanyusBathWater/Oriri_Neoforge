package net.ganyusbathwater.oririmod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class JadeShieldRenderer extends BlockEntityWithoutLevelRenderer {
    private ShieldModel shieldModel;
    public static final Material JADE_SHIELD_MATERIAL = new Material(
            ResourceLocation.withDefaultNamespace("textures/atlas/shield_patterns.png"),
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "entity/jade_shield_base_nopattern"));

    public JadeShieldRenderer() {
        super(net.minecraft.client.Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                net.minecraft.client.Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (this.shieldModel == null) {
            this.shieldModel = new ShieldModel(
                    net.minecraft.client.Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
        }

        poseStack.pushPose();
        poseStack.scale(1.0F, -1.0F, -1.0F);

        VertexConsumer vertexconsumer = JADE_SHIELD_MATERIAL.sprite().wrap(ItemRenderer.getFoilBufferDirect(buffer,
                this.shieldModel.renderType(JADE_SHIELD_MATERIAL.atlasLocation()), true, stack.hasFoil()));
        this.shieldModel.handle().render(poseStack, vertexconsumer, packedLight, packedOverlay, 0xFFFFFFFF);
        this.shieldModel.plate().render(poseStack, vertexconsumer, packedLight, packedOverlay, 0xFFFFFFFF);

        poseStack.popPose();
    }
}
