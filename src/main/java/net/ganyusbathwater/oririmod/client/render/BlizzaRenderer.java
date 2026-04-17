package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.BlizzaEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BlizzaRenderer extends GeoEntityRenderer<BlizzaEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/blizza.png");

    // Ice-blue ARGB for particle tinting (rendered as nearest vanilla particle)
    private static final int PARTICLE_COLOR = 0xFF4FC3F7;

    public BlizzaRenderer(EntityRendererProvider.Context context) {
        super(context, new net.ganyusbathwater.oririmod.client.model.BlizzaModel());
    }

    @Override
    public ResourceLocation getTextureLocation(BlizzaEntity entity) {
        return TEXTURE;
    }

    // ── Issue #4: spawn particles on hands during magic animations ─────────
    @Override
    public void render(BlizzaEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        int attackType = entity.getAttackType();
        // Spawn hand particles for magic_1, magic_2, magic_3 (and while storm is active)
        boolean magicActive = (attackType == BlizzaEntity.ATTACK_ICICLE
                || attackType == BlizzaEntity.ATTACK_STORM
                || attackType == BlizzaEntity.ATTACK_ILLAGER
                || entity.isCasting());

        if (magicActive) {
            spawnHandParticles(entity);
        }
    }

    private void spawnHandParticles(BlizzaEntity entity) {
        double ex  = entity.getX();
        double ey  = entity.getY() + 1.4; // approx hand height
        double ez  = entity.getZ();
        double yaw = Math.toRadians(entity.getYRot());
        double side = 0.5;

        // Right hand (entity-relative)
        spawnIceParticle(ex + Math.cos(yaw) * side, ey, ez + Math.sin(yaw) * side, entity);
        // Left hand (entity-relative)
        spawnIceParticle(ex - Math.cos(yaw) * side, ey, ez - Math.sin(yaw) * side, entity);
    }

    private void spawnIceParticle(double x, double y, double z, BlizzaEntity entity) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        RandomSource rng = entity.getRandom();
        double vx = (rng.nextDouble() - 0.5) * 0.1;
        double vy = rng.nextDouble() * 0.1;
        double vz = (rng.nextDouble() - 0.5) * 0.1;

        level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, vx, vy, vz);
        if (rng.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.DRIPPING_WATER, x, y + 0.1, z, vx, vy * 0.5, vz);
        }
    }
}
