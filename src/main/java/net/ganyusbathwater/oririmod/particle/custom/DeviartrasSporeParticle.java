package net.ganyusbathwater.oririmod.particle.custom;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class DeviartrasSporeParticle extends TextureSheetParticle {
    
    protected DeviartrasSporeParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        
        this.friction = 0.96F;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.quadSize *= 1.5F + this.random.nextFloat() * 0.5F;
        this.lifetime = (int)(40.0D / (Math.random() * 0.8D + 0.2D)) + 20; // 2 to 4 seconds approx
        
        this.hasPhysics = false;
        
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        this.xd *= 0.9D;
        this.yd *= 0.9D;
        this.zd *= 0.9D;
        // Float around very gently
        this.yd += (this.random.nextFloat() - 0.5F) * 0.01F;
        this.xd += (this.random.nextFloat() - 0.5F) * 0.01F;
        this.zd += (this.random.nextFloat() - 0.5F) * 0.01F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType particleType, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new DeviartrasSporeParticle(level, x, y, z, this.sprites, dx, dy, dz);
        }
    }
}
