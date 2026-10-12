package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ganyusbathwater.oririmod.entity.TornadoEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class TornadoRenderer extends GeoEntityRenderer<TornadoEntity> {

    public TornadoRenderer(EntityRendererProvider.Context context) {
        super(context, new TornadoModel());
        this.shadowRadius = 0.5F;
    }

    @Override
    public RenderType getRenderType(TornadoEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        float f = (float) animatable.tickCount + partialTick;
        // Scrolls horizontally in the shader pipeline (the real authentic Wind Charge technique in 1.21)
        return RenderType.breezeWind(texture, (f * 0.03F) % 1.0F, 0.0F);
    }

    @Override
    public void render(TornadoEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Wind charge is rendered with full brightness for crisp elemental luminescence
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    protected int getBlockLightLevel(TornadoEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public Color getRenderColor(TornadoEntity animatable, float partialTick, int packedLight) {
        return Color.ofOpaque(animatable.getClientBiomeColor());
    }
}
