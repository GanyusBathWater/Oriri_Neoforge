package net.ganyusbathwater.oririmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.IcicleEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class IcicleModel extends EntityModel<IcicleEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "icicle"), "main");

    private final ModelPart root;

    public IcicleModel(ModelPart root) {
        this.root = root;
    }

    /**
     * Creates a pointed icicle shape: a tapered pillar with a wider base and
     * narrower tip.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rootPart = mesh.getRoot();

        // Main body: wider at the top, tapers down
        // Base section (top of icicle — wider)
        rootPart.addOrReplaceChild("base", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, 8, -3, 6, 6, 6, CubeDeformation.NONE),
                PartPose.ZERO);

        // Middle section
        rootPart.addOrReplaceChild("middle", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-2, 2, -2, 4, 6, 4, CubeDeformation.NONE),
                PartPose.ZERO);

        // Tip section (bottom — pointed)
        rootPart.addOrReplaceChild("tip", CubeListBuilder.create()
                .texOffs(16, 12).addBox(-1, -2, -1, 2, 4, 2, CubeDeformation.NONE),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(IcicleEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        // No animation needed — icicle just falls straight
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
            int packedColor) {
        root.render(poseStack, buffer, packedLight, packedOverlay, packedColor);
    }
}
