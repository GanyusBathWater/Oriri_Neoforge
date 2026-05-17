package net.ganyusbathwater.oririmod.client.render;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;

public class CustomArrowRenderer<T extends AbstractArrow> extends ArrowRenderer<T> {
    private final ResourceLocation textureLocation;

    public CustomArrowRenderer(EntityRendererProvider.Context context, ResourceLocation textureLocation) {
        super(context);
        this.textureLocation = textureLocation;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.textureLocation;
    }
}
