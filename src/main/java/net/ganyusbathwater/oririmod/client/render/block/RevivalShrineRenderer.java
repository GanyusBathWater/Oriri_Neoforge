package net.ganyusbathwater.oririmod.client.render.block;

import net.ganyusbathwater.oririmod.block.entity.RevivalShrineBlockEntity;
import net.ganyusbathwater.oririmod.client.render.model.RevivalShrineModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class RevivalShrineRenderer extends GeoBlockRenderer<RevivalShrineBlockEntity> {
    public RevivalShrineRenderer(BlockEntityRendererProvider.Context context) {
        super(new RevivalShrineModel());
    }
}
