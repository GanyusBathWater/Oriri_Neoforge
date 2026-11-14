package net.ganyusbathwater.oririmod.particle.custom;

import net.ganyusbathwater.oririmod.client.render.world.AbyssalDarknessManager;
import net.ganyusbathwater.oririmod.util.ShaderDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AbyssalShroudParticle extends TextureSheetParticle {
    private static final int SHADER_NEUTRAL_LIGHT = LightTexture.pack(4, 12);

    private final SpriteSet sprites;
    private final float rotSpeed;
    private final float maxAlpha;
    private final float baseQuadSize;
    private final boolean shaderActive;
    private float lightDissolve = 0.0F;
    private float targetLightDissolve = 0.0F;

    public AbyssalShroudParticle(ClientLevel level, double x, double y, double z,
                                double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = spriteSet;
        this.shaderActive = ShaderDetector.isShaderActive();

        // Massive volumetric cloud size (3.5 - 6.0 blocks wide)
        this.baseQuadSize = 3.5F + this.random.nextFloat() * 2.5F;
        this.quadSize = this.baseQuadSize;
        this.lifetime = 120 + this.random.nextInt(60); // 6 to 9 seconds

        // Lift base tint slightly under shaders so 8-bit/half-float G-buffers do not discard near-zero ink clouds
        if (this.shaderActive) {
            this.rCol = 0.018F;
            this.gCol = 0.025F;
            this.bCol = 0.036F;
        } else {
            this.rCol = 0.006F;
            this.gCol = 0.011F;
            this.bCol = 0.018F;
        }
        this.maxAlpha = (this.shaderActive ? 0.90F : 0.88F) + this.random.nextFloat() * 0.08F;
        this.alpha = 0.0F; // Fades in smoothly

        // Gentle oceanic current drift
        this.xd = xd + (this.random.nextDouble() - 0.5) * 0.015;
        this.yd = yd + (this.random.nextDouble() - 0.5) * 0.008;
        this.zd = zd + (this.random.nextDouble() - 0.5) * 0.015;

        this.gravity = 0.0F;
        this.friction = 0.985F;

        // Subtle slow atmospheric roll / churn
        this.roll = this.random.nextFloat() * (float) (2 * Math.PI);
        this.oRoll = this.roll;
        this.rotSpeed = (this.random.nextFloat() - 0.5F) * 0.008F;

        this.targetLightDissolve = sampleLightDissolve();
        this.lightDissolve = this.targetLightDissolve;

        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);

        this.oRoll = this.roll;
        this.roll += this.rotSpeed;

        if ((this.age & 3) == 0) {
            this.targetLightDissolve = sampleLightDissolve();
        }
        this.lightDissolve = Mth.lerp(0.25F, this.lightDissolve, this.targetLightDissolve);

        // Smooth alpha envelope: fade in over first 20 ticks, fade out over last 30 ticks
        int fadeInTicks = 20;
        int fadeOutTicks = 30;

        float baseAlpha;
        if (this.age < fadeInTicks) {
            baseAlpha = ((float) this.age / (float) fadeInTicks) * this.maxAlpha;
        } else if (this.age > this.lifetime - fadeOutTicks) {
            int remaining = this.lifetime - this.age;
            baseAlpha = ((float) remaining / (float) fadeOutTicks) * this.maxAlpha;
        } else {
            baseAlpha = this.maxAlpha;
        }

        // Light sources modestly thin nearby darkness clouds without clearing the abyss
        this.alpha = baseAlpha * (1.0F - 0.42F * this.lightDissolve);
        this.quadSize = this.baseQuadSize * (1.0F - 0.16F * this.lightDissolve);
    }

    private float sampleLightDissolve() {
        int blockLight = this.level.getBrightness(LightLayer.BLOCK, BlockPos.containing(this.x, this.y, this.z));
        float blockFactor = Mth.clamp(blockLight / 12.0F, 0.0F, 1.0F);

        float heldFactor = 0.0F;
        float heldStrength = AbyssalDarknessManager.getHeldLightStrength();
        if (heldStrength > 0.05F) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                double maxRadius = 4.0 + 7.0 * heldStrength;
                double distSq = mc.player.position().distanceToSqr(this.x, this.y, this.z);
                if (distSq < maxRadius * maxRadius) {
                    float dist = (float) Math.sqrt(distSq);
                    heldFactor = heldStrength * 0.65F * (1.0F - dist / (float) maxRadius);
                }
            }
        }
        return Mth.clamp(Math.max(blockFactor, heldFactor), 0.0F, 1.0F);
    }

    @Override
    public int getLightColor(float partialTick) {
        // Under shaders, zero lightmap collapses translucent particles to invisible; pack neutral sky light instead
        return this.shaderActive ? SHADER_NEUTRAL_LIGHT : 0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType particleType, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new AbyssalShroudParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}

