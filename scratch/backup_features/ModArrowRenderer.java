package net.ganyusbathwater.oririmod.client.render.arrow;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;

public class ModArrowRenderer<T extends AbstractArrow> extends ArrowRenderer<T> {
    private final ResourceLocation textureLocation;

    public ModArrowRenderer(EntityRendererProvider.Context context, ResourceLocation textureLocation) {
        super(context);
        this.textureLocation = textureLocation;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return textureLocation;
    }
}
