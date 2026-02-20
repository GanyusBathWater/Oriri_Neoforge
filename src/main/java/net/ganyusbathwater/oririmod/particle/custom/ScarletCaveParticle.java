package net.ganyusbathwater.oririmod.particle.custom;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScarletCaveParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    public ScarletCaveParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
            SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = spriteSet;
        this.friction = 0.9F;
        this.xd = xd * 0.1;
        this.yd = yd * 0.1;
        this.zd = zd * 0.1;
        this.quadSize *= 0.85F;
        this.lifetime = 80 + this.random.nextInt(30);
        this.setSpriteFromAge(spriteSet);
        this.rCol = 1f;
        this.gCol = 1f;
        this.bCol = 1f;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);

        // Scarlet particles rise slightly
        // this.yd += 0.002;
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
            return new ScarletCaveParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}
