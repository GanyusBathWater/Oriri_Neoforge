package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class ModRenderTypes extends RenderType {

    private static final RenderType ADDITIVE_LIGHTNING_FLARE = RenderType.create(
            "oriri_additive_flare",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            2048,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setWriteMaskState(COLOR_WRITE)
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    );

    public ModRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType getAdditiveBeam(ResourceLocation texture) {
        return RenderType.create(
                "energy_swirl",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        );
    }

    /**
     * Depth-writing procedural vortex render type.
     * Uses COLOR_DEPTH_WRITE and sortOnUpload=true so the tornado writes to depthtex0,
     * ensuring water, translucent terrain, and shader clouds never render in front of it.
     */
    public static RenderType getTornadoVortex(ResourceLocation texture) {
        return RenderType.create(
                "tornado_vortex",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                2048,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(true)
        );
    }

    /**
     * Shader-compatible untextured additive RenderType (backed by vanilla's lightning shader).
     * Iris/Oculus shader packs automatically recognize {@code RENDERTYPE_LIGHTNING_SHADER}
     * as high-intensity emissive geometry and apply HDR bloom to it.
     */
    public static RenderType getAdditiveFlare() {
        return ADDITIVE_LIGHTNING_FLARE;
    }
}
