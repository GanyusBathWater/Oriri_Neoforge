package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.entity.MagicProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

public class MagicProjectileRenderer extends EntityRenderer<MagicProjectileEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/entity/magic_projectile.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);

    private static final Map<Element, Color> ELEMENT_COLORS = new EnumMap<>(Element.class);

    static {
        ELEMENT_COLORS.put(Element.FIRE, new Color(255, 60, 60));
        ELEMENT_COLORS.put(Element.NATURE, new Color(60, 255, 60));
        ELEMENT_COLORS.put(Element.EARTH, new Color(180, 120, 60));
        ELEMENT_COLORS.put(Element.WATER, new Color(60, 120, 255));
        ELEMENT_COLORS.put(Element.LIGHT, new Color(255, 255, 180));
        ELEMENT_COLORS.put(Element.DARKNESS, new Color(160, 60, 255));
        ELEMENT_COLORS.put(Element.TRUE_DAMAGE, new Color(255, 60, 220));
        ELEMENT_COLORS.put(Element.PHYSICAL, Color.WHITE);
    }

    private final net.ganyusbathwater.oririmod.client.model.MagicProjectileModel model;

    public MagicProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new net.ganyusbathwater.oririmod.client.model.MagicProjectileModel(context.bakeLayer(net.ganyusbathwater.oririmod.client.model.MagicProjectileModel.LAYER_LOCATION));
    }

    @Override
    public void render(MagicProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (entity.tickCount >= 2 || !(this.entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < 12.25D)) {
            float f = (float)entity.tickCount + partialTicks;
            
            Color color = ELEMENT_COLORS.getOrDefault(entity.getElement(), Color.WHITE);
            float r = color.getRed() / 255.0f;
            float g = color.getGreen() / 255.0f;
            float b = color.getBlue() / 255.0f;
            float a = color.getAlpha() / 255.0f;
            
            VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.breezeWind(TEXTURE, this.xOffset(f) % 1.0F, 0.0F));
            
            // To apply color to a model rendering, we can change the vertex consumer or renderType.
            // But breezeWind doesn't take color in its parameters. 
            // We can use a Translucent or Emissive render type with a tinted vertex consumer, 
            // or pass color into model.renderToBuffer.
            
            this.model.setupAnim(entity, 0.0F, 0.0F, f, 0.0F, 0.0F);
            this.model.renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, color.getRGB());
            
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        }
    }

    protected float xOffset(float tickCount) {
        return tickCount * 0.03F;
    }

    @Override
    public ResourceLocation getTextureLocation(MagicProjectileEntity entity) {
        return TEXTURE;
    }
}
