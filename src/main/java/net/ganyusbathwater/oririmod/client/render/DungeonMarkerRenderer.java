package net.ganyusbathwater.oririmod.client.render;

import net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class DungeonMarkerRenderer extends EntityRenderer<DungeonMarkerEntity> {

    public DungeonMarkerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(DungeonMarkerEntity entity) {
        return null;
    }

    @Override
    protected boolean shouldShowName(DungeonMarkerEntity entity) {
        Player player = Minecraft.getInstance().player;
        if (player != null && (player.getMainHandItem().is(ModItems.DUNGEON_MARKER_SPAWN_EGG.get()) || player.getOffhandItem().is(ModItems.DUNGEON_MARKER_SPAWN_EGG.get()))) {
            return true;
        }
        return false;
    }
}
