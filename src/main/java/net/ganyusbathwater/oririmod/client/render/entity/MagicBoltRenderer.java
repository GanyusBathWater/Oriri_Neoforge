package net.ganyusbathwater.oririmod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.entity.MagicBoltEntity;
import net.ganyusbathwater.oririmod.util.MagicBoltAbility;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MagicBoltRenderer extends EntityRenderer<MagicBoltEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("oririmod", "textures/entity/magic_projectile.png");
    private final ItemRenderer itemRenderer;
    private final net.ganyusbathwater.oririmod.client.model.MagicProjectileModel<MagicBoltEntity> projectileModel;

    public MagicBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.projectileModel = new net.ganyusbathwater.oririmod.client.model.MagicProjectileModel<>(context.bakeLayer(net.ganyusbathwater.oririmod.client.model.MagicProjectileModel.LAYER_LOCATION));
    }

    @Override
    public void render(MagicBoltEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        MagicBoltAbility ability = entity.getAbility();
        
        switch (ability) {
            case SONIC -> {
                // Invisible
                return;
            }
            case BLAZE, EXPLOSIVE -> {
                renderItem(entity, new ItemStack(Items.FIRE_CHARGE), poseStack, buffer, packedLight);
                return;
            }
            case ENDER -> {
                renderItem(entity, new ItemStack(Items.ENDER_PEARL), poseStack, buffer, packedLight);
                return;
            }
            case NORMAL -> {
                float f = (float)entity.tickCount + partialTicks;
                VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.breezeWind(TEXTURE, (f * 0.03F) % 1.0F, 0.0F));
                
                int color = java.awt.Color.WHITE.getRGB(); 
                
                poseStack.pushPose();
                // Match the visual height offset that MagicProjectileModel typically uses
                poseStack.translate(0.0D, 0.15D, 0.0D); 
                this.projectileModel.setupAnim(entity, 0.0F, 0.0F, f, 0.0F, 0.0F);
                this.projectileModel.renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, color);
                poseStack.popPose();
                return;
            }
            default -> {
            }
        }
    }

    private void renderItem(MagicBoltEntity entity, ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(0.8F, 0.8F, 0.8F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
    }



    public void vertex(PoseStack.Pose pose, VertexConsumer consumer, int x, int y, int z, float u, float v, int nx, int ny, int nz, int packedLight) {
        consumer.addVertex(pose.pose(), (float)x, (float)y, (float)z)
                .setColor(50, 150, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, (float)nx, (float)ny, (float)nz);
    }

    @Override
    public ResourceLocation getTextureLocation(MagicBoltEntity entity) {
        return TEXTURE;
    }
}
