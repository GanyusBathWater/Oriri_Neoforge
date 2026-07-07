package net.ganyusbathwater.oririmod.particle.custom;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class ElderSporeBlossomSporeParticle extends TextureSheetParticle {
    
    protected ElderSporeBlossomSporeParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet, double xd, double yd, double zd) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        
        this.friction = 0.96F;
        this.gravity = 0.02F;
        this.xd = this.random.nextFloat() * 0.02F - 0.01F;
        this.yd = this.random.nextFloat() * -0.02F - 0.01F;
        this.zd = this.random.nextFloat() * 0.02F - 0.01F;
        
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        
        // Small size matching vanilla Spore Blossom
        this.quadSize = this.random.nextFloat() * 0.1F + 0.05F;
        this.lifetime = (int)(40.0D / (Math.random() * 0.8D + 0.2D)) + 40;
        
        this.hasPhysics = true;
        this.pickSprite(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        this.xd *= 0.95D;
        this.zd *= 0.95D;
        
        // Emulate vanilla Spore Blossom Air Particle sway
        this.xd += (this.random.nextFloat() - 0.5F) * 0.005D;
        this.zd += (this.random.nextFloat() - 0.5F) * 0.005D;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240 | (240 << 16); // full brightness/glow
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType particleType, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new ElderSporeBlossomSporeParticle(level, x, y, z, this.sprites, dx, dy, dz);
        }
    }
}
