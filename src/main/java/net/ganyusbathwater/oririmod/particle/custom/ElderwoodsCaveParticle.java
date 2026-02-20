package net.ganyusbathwater.oririmod.particle.custom;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ElderwoodsCaveParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    public ElderwoodsCaveParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
            SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = spriteSet;
        this.friction = 0.96F; // Higher friction = slower movement
        this.xd = xd * 0.05; // Very little initial velocity
        this.yd = yd * 0.05;
        this.zd = zd * 0.05;
        this.quadSize *= 0.75F;
        this.lifetime = 80 + this.random.nextInt(40); // 3-5 seconds
        this.setSpriteFromAge(spriteSet);
        this.rCol = 1f;
        this.gCol = 1f;
        this.bCol = 1f;
        this.hasPhysics = true;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);

        // Gentle drift
        this.xd += (Math.random() - 0.5) * 0.002;
        this.yd += (Math.random() - 0.5) * 0.002;
        this.zd += (Math.random() - 0.5) * 0.002;
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

        public Particle createParticle(SimpleParticleType particleType, ClientLevel level, double x, double y, double z,
                double dx, double dy, double dz) {
            return new ElderwoodsCaveParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}
