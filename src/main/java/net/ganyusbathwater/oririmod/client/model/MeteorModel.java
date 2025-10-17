package net.ganyusbathwater.oririmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.MeteorEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class MeteorModel extends EntityModel<MeteorEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "meteor"), "main");

    private final ModelPart root;

    public MeteorModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        CubeDeformation d = new CubeDeformation(0.3f);
        root.addOrReplaceChild("core", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4, -4, -4, 8, 8, 8, d)
                        .texOffs(0, 16).addBox(-3, -6, -3, 6, 6, 6, d)
                        .texOffs(24, 16).addBox(-5, -2, -2, 4, 4, 4, d),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MeteorEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // keine Animation; Rotation im Renderer
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int packedColor) {
        root.render(poseStack, buffer, packedLight, packedOverlay, packedColor);
    }
}
