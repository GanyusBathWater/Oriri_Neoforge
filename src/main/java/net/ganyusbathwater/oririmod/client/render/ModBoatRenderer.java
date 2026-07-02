package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.ModBoatEntity;
import net.ganyusbathwater.oririmod.entity.custom.ModChestBoatEntity;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;

import java.util.Map;
import java.util.HashMap;

public class ModBoatRenderer extends EntityRenderer<Boat> {
    private final Map<ModBoatEntity.ModBoatType, ResourceLocation> boatResources = new HashMap<>();
    private final BoatModel boatModel;

    public ModBoatRenderer(EntityRendererProvider.Context pContext, boolean pChestBoat) {
        super(pContext);
        this.shadowRadius = 0.8F;
        this.boatModel = pChestBoat ? new ChestBoatModel(pContext.bakeLayer(ModelLayers.createChestBoatModelName(Boat.Type.OAK)))
                                    : new BoatModel(pContext.bakeLayer(ModelLayers.createBoatModelName(Boat.Type.OAK)));
        
        for (ModBoatEntity.ModBoatType type : ModBoatEntity.ModBoatType.values()) {
            String suffix = pChestBoat ? "_chest_boat" : "_boat";
            boatResources.put(type, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/" + type.getName() + suffix + ".png"));
        }
    }

    @Override
    public void render(Boat pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        pMatrixStack.pushPose();
        pMatrixStack.translate(0.0F, 0.375F, 0.0F);
        pMatrixStack.mulPose(Axis.YP.rotationDegrees(180.0F - pEntityYaw));
        float f = (float)pEntity.getHurtTime() - pPartialTicks;
        float f1 = pEntity.getDamage() - pPartialTicks;
        if (f1 < 0.0F) {
            f1 = 0.0F;
        }

        if (f > 0.0F) {
            pMatrixStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(f) * f * f1 / 10.0F * (float)pEntity.getHurtDir()));
        }

        float f2 = pEntity.getBubbleAngle(pPartialTicks);
        if (!Mth.equal(f2, 0.0F)) {
            pMatrixStack.mulPose(Axis.ZP.rotationDegrees(pEntity.getBubbleAngle(pPartialTicks)));
        }

        pMatrixStack.scale(-1.0F, -1.0F, 1.0F);
        pMatrixStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        this.boatModel.setupAnim(pEntity, pPartialTicks, 0.0F, -0.1F, 0.0F, 0.0F);
        
        ResourceLocation texture = getTextureLocation(pEntity);
        if (texture != null) {
            this.boatModel.renderToBuffer(pMatrixStack, pBuffer.getBuffer(RenderType.entityCutoutNoCull(texture)), pPackedLight, OverlayTexture.NO_OVERLAY);
        }

        pMatrixStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(Boat pEntity) {
        if (pEntity instanceof ModBoatEntity modBoat) {
            return boatResources.get(modBoat.getModVariant());
        } else if (pEntity instanceof ModChestBoatEntity modChestBoat) {
            return boatResources.get(modChestBoat.getModVariant());
        }
        return null;
    }
}
