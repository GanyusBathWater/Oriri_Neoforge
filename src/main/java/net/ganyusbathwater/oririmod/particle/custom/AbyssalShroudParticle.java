package net.ganyusbathwater.oririmod.particle.custom;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AbyssalShroudParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float rotSpeed;
    private final float maxAlpha;

    public AbyssalShroudParticle(ClientLevel level, double x, double y, double z,
                                double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = spriteSet;

        // Massive volumetric cloud size (3.5 - 6.0 blocks wide)
        this.quadSize = 3.5F + this.random.nextFloat() * 2.5F;
        this.lifetime = 120 + this.random.nextInt(60); // 6 to 9 seconds

        // Pitch black abyssal tint with faint midnight blue undertone
        this.rCol = 0.003F;
        this.gCol = 0.007F;
        this.bCol = 0.012F;
        this.maxAlpha = 0.88F + this.random.nextFloat() * 0.08F; // 0.88 - 0.96 opacity
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

        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);

        this.oRoll = this.roll;
        this.roll += this.rotSpeed;

        // Smooth alpha envelope: fade in over first 20 ticks, fade out over last 30 ticks
        int fadeInTicks = 20;
        int fadeOutTicks = 30;

        if (this.age < fadeInTicks) {
            this.alpha = ((float) this.age / (float) fadeInTicks) * this.maxAlpha;
        } else if (this.age > this.lifetime - fadeOutTicks) {
            int remaining = this.lifetime - this.age;
            this.alpha = ((float) remaining / (float) fadeOutTicks) * this.maxAlpha;
        } else {
            this.alpha = this.maxAlpha;
        }
    }

    @Override
    public int getLightColor(float partialTick) {
        // Enforce zero light so daylight does not illuminate the abyssal clouds
        return 0;
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

