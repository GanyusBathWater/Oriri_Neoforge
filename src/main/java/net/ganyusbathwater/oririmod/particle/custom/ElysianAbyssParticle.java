package net.ganyusbathwater.oririmod.particle.custom;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ElysianAbyssParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    public ElysianAbyssParticle(ClientLevel level, double x, double y, double z,
            double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = spriteSet;

        // Very small, sparse
        this.quadSize *= 0.45F;
        this.lifetime = 60 + this.random.nextInt(61); // 60-120 ticks

        // Blue color
        this.rCol = 0.15f;
        this.gCol = 0.45f;
        this.bCol = 1.0f;
        this.alpha = 0.85f;

        // Initial gentle upward drift; no horizontal velocity
        this.xd = (this.random.nextDouble() - 0.5) * 0.01;
        this.yd = 0.015 + this.random.nextDouble() * 0.01; // ascend upward
        this.zd = (this.random.nextDouble() - 0.5) * 0.01;

        this.gravity = 0.0F; // ignore gravity
        this.friction = 0.98F;

        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);

        // Accelerate upward slowly, like fire embers
        this.yd += 0.0004;

        // Fade out near end of lifetime
        float lifeRatio = (float) this.age / (float) this.lifetime;
        if (lifeRatio > 0.7f) {
            this.alpha = 0.85f * (1.0f - ((lifeRatio - 0.7f) / 0.3f));
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
            return new ElysianAbyssParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}
