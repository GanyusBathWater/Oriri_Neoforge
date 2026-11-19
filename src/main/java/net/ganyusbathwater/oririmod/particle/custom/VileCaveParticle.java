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
public class VileCaveParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    public VileCaveParticle(ClientLevel level, double x, double y, double z,
            double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = spriteSet;

        this.quadSize *= 0.48F;
        this.lifetime = 70 + this.random.nextInt(60);

        // Soft eerie violet-purple spore/mist mote
        this.rCol = 0.68f + this.random.nextFloat() * 0.14f;
        this.gCol = 0.24f + this.random.nextFloat() * 0.10f;
        this.bCol = 0.92f + this.random.nextFloat() * 0.08f;
        this.alpha = 0.80f;

        this.xd = (this.random.nextDouble() - 0.5) * 0.012;
        this.yd = 0.008 + this.random.nextDouble() * 0.010;
        this.zd = (this.random.nextDouble() - 0.5) * 0.012;

        this.gravity = 0.0F;
        this.friction = 0.985F;

        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);

        this.yd += 0.0002;

        float lifeRatio = (float) this.age / (float) this.lifetime;
        if (lifeRatio > 0.7f) {
            this.alpha = 0.80f * (1.0f - ((lifeRatio - 0.7f) / 0.3f));
        }
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
                double x, double y, double z, double dx, double dy, double dz) {
            return new VileCaveParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}
