package org.jcs.egm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class RealityTrailParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;

    protected RealityTrailParticle(ClientLevel level, double x, double y, double z,
                                   double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.spriteSet = spriteSet;

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.quadSize *= 0.5F;
        this.lifetime = 20 + this.random.nextInt(10);
        this.gravity = 0.0F;

        pickSprite(spriteSet); // Randomly pick one of the available particle textures
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(spriteSet); // Animate sprite if multiple frames are available
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new RealityTrailParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet);
        }
    }
}
