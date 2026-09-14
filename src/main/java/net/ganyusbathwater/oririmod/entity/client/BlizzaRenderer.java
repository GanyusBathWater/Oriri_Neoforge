package net.ganyusbathwater.oririmod.entity.client;

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
        super(context, new BlizzaModel());
    }

    @Override
    public ResourceLocation getTextureLocation(BlizzaEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BlizzaEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        int attackType = entity.getAttackType();
        boolean magicActive = (attackType == BlizzaEntity.ATTACK_ICICLE
                || attackType == BlizzaEntity.ATTACK_STORM
                || attackType == BlizzaEntity.ATTACK_ILLAGER
                || entity.isCasting());

        if (magicActive) {
            if (entity.lastParticleRenderTick != entity.tickCount) {
                entity.lastParticleRenderTick = entity.tickCount;
                spawnHandParticles(entity);
                spawnAuraParticles(entity);
            }
        }
    }

    private void spawnHandParticles(BlizzaEntity entity) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        
        net.minecraft.world.phys.Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        
        software.bernie.geckolib.cache.object.GeoBone rightHand = this.getGeoModel().getAnimationProcessor().getBone("right_arm_ancor");
        if (rightHand != null) {
            org.joml.Vector3d pos = rightHand.getWorldPosition();
            spawnSnowflakes(camPos.x + pos.x(), camPos.y + pos.y(), camPos.z + pos.z(), entity);
        }
        
        software.bernie.geckolib.cache.object.GeoBone leftHand = this.getGeoModel().getAnimationProcessor().getBone("left_arm_ancor");
        if (leftHand != null) {
            org.joml.Vector3d pos = leftHand.getWorldPosition();
            spawnSnowflakes(camPos.x + pos.x(), camPos.y + pos.y(), camPos.z + pos.z(), entity);
        }
    }

    private void spawnSnowflakes(double x, double y, double z, BlizzaEntity entity) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        RandomSource rng = entity.getRandom();
        // Spawn a set amount of snowflakes per tick (e.g. 3 per hand)
        for (int i = 0; i < 3; i++) {
            double vx = (rng.nextDouble() - 0.5) * 0.2;
            double vy = rng.nextDouble() * 0.1;
            double vz = (rng.nextDouble() - 0.5) * 0.2;
            level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, vx, vy, vz);
        }
    }
    
    private void spawnAuraParticles(BlizzaEntity entity) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        RandomSource rng = entity.getRandom();
        // Water/Aura particles floating up
        for (int i = 0; i < 2; i++) {
            double ox = (rng.nextDouble() - 0.5) * 2.0;
            double oy = rng.nextDouble() * 2.5;
            double oz = (rng.nextDouble() - 0.5) * 2.0;
            
            double vx = (rng.nextDouble() - 0.5) * 0.05;
            double vy = 0.05 + rng.nextDouble() * 0.05; // Fly higher
            double vz = (rng.nextDouble() - 0.5) * 0.05;

            // GLOW particles look magical, float nicely, and despawn smoothly
            level.addParticle(ParticleTypes.GLOW, entity.getX() + ox, entity.getY() + oy, entity.getZ() + oz, vx, vy, vz);
        }
    }
}
