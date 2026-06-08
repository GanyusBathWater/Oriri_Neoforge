package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.entity.custom.LoadedBlazeEntity;
import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class LoadedBlazeRenderer extends MobRenderer<LoadedBlazeEntity, BlazeModel<LoadedBlazeEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("oririmod", "textures/entity/loaded_blaze.png");

    public LoadedBlazeRenderer(EntityRendererProvider.Context context) {
        super(context, new BlazeModel<>(context.bakeLayer(ModelLayers.BLAZE)), 0.5F);
    }

    @Override
    protected int getBlockLightLevel(LoadedBlazeEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(LoadedBlazeEntity entity) {
        return TEXTURE;
    }
}
