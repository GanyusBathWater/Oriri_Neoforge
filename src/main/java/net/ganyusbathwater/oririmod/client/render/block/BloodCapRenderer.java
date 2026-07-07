package net.ganyusbathwater.oririmod.client.render.block;

import net.ganyusbathwater.oririmod.block.entity.BloodCapBlockEntity;
import net.ganyusbathwater.oririmod.client.render.model.BloodCapModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class BloodCapRenderer extends GeoBlockRenderer<BloodCapBlockEntity> {
    public BloodCapRenderer(BlockEntityRendererProvider.Context context) {
        super(new BloodCapModel());
    }
}
