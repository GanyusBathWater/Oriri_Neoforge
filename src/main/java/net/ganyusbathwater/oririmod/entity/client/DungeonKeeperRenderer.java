package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.dungeon.entity.DungeonKeeperEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DungeonKeeperRenderer extends MobRenderer<DungeonKeeperEntity, VillagerModel<DungeonKeeperEntity>> {
    private static final ResourceLocation WANDERING_TRADER_TEXTURE = ResourceLocation.parse("textures/entity/wandering_trader.png");

    public DungeonKeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.WANDERING_TRADER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(DungeonKeeperEntity entity) {
        return WANDERING_TRADER_TEXTURE;
    }
}
