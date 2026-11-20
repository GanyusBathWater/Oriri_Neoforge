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

/**
 * A 1.5x (+50%) scaled variant of the vanilla Warden sonic boom particle,
 * used by One Thousand Screams at Level 2 and Level 3.
 */
@OnlyIn(Dist.CLIENT)
public class LargeSonicBoomParticle extends TextureSheetParticle {
    private static final float BASE_SONIC_BOOM_SIZE = 1.5F;
    private static final float LEVEL_TWO_SIZE_MULTIPLIER = 1.5F;

    private final SpriteSet sprites;

    protected LargeSonicBoomParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = spriteSet;
        this.lifetime = 16;
        this.quadSize = BASE_SONIC_BOOM_SIZE * LEVEL_TWO_SIZE_MULTIPLIER;
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.hasPhysics = false;
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
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
            return new LargeSonicBoomParticle(level, x, y, z, this.sprites);
        }
    }
}
