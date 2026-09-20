package net.ganyusbathwater.oririmod.particle.custom;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class TeleporterVortexParticle extends TextureSheetParticle {
    private final double targetX;
    private final double targetY;
    private final double targetZ;

    protected TeleporterVortexParticle(ClientLevel level, double x, double y, double z, double targetX, double targetY, double targetZ, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0);
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        
        this.xd = (Math.random() - 0.5) * 0.1;
        this.yd = (Math.random() - 0.5) * 0.1;
        this.zd = (Math.random() - 0.5) * 0.1;
        
        this.quadSize *= 0.75F + this.random.nextFloat() * 0.5F;
        this.lifetime = 40 + this.random.nextInt(20);
        this.hasPhysics = false;
        
        // Random glowing colors (e.g. purple/pink for the rift)
        float r = 0.6f + this.random.nextFloat() * 0.4f;
        float g = 0.0f + this.random.nextFloat() * 0.2f;
        float b = 0.8f + this.random.nextFloat() * 0.2f;
        this.setColor(r, g, b);
        
        this.pickSprite(spriteSet);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        
        // Accelerate towards the center
        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;
        
        double distSq = dx * dx + dy * dy + dz * dz;
        
        if (distSq < 0.1) {
            // Reached the center, disappear
            this.remove();
            return;
        }
        
        // Normalize and apply acceleration
        double dist = Math.sqrt(distSq);
        double force = 0.05 / dist; // stronger the closer it gets
        
        this.xd += (dx / dist) * force;
        this.yd += (dy / dist) * force;
        this.zd += (dz / dist) * force;
        
        // Add some spin/swirl
        this.xd += (dz / dist) * 0.02;
        this.zd -= (dx / dist) * 0.02;

        this.move(this.xd, this.yd, this.zd);
        
        // Fade out slightly over time, or shrink
        this.alpha = 1.0F - (float) this.age / (float) this.lifetime;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    
    @Override
    public int getLightColor(float partialTick) {
        return 15728880; // Full brightness
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double targetX, double targetY, double targetZ) {
            return new TeleporterVortexParticle(level, x, y, z, targetX, targetY, targetZ, this.spriteSet);
        }
    }
}
