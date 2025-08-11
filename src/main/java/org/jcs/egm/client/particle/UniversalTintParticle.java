package org.jcs.egm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Tintable universal particle:
 * - vx, vy, vz are interpreted as r, g, b ∈ [0..1]
 * - On the server, send with count=0 so RGB aren't randomized
 * - Supports per-spawn size via setScaleMultiplier()
 */
public class UniversalTintParticle extends TextureSheetParticle {
    private static float SCALE_MULT = 1.0f;
    public static void setScaleMultiplier(float m) { SCALE_MULT = m; }

    private final SpriteSet sprites;

    protected UniversalTintParticle(ClientLevel level, double x, double y, double z,
                                    double r, double g, double b, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;

        // Color from spawn params (vx,vy,vz)
        this.setColor((float) r, (float) g, (float) b);
        this.setAlpha(1.0F);

        // Size (scaled per-spawn), lifetime, motion
        this.quadSize = (0.18F + this.random.nextFloat() * 0.06F) * SCALE_MULT;
        this.lifetime = 14 + this.random.nextInt(8);
        this.gravity = 0.0F;
        this.hasPhysics = false;

        // Optional roll variance; bind initial sprite
        this.oRoll = this.roll = (float) (this.random.nextDouble() * Math.PI * 2);
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        // Animate if the sprite sheet has multiple frames
        this.setSpriteFromAge(this.sprites);
        // Simple linear fade
        this.alpha = Math.max(0.0F, 1.0F - ((float) this.age / (float) this.lifetime));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }


    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }



    /** Provider: forwards vx,vy,vz as r,g,b */
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new UniversalTintParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
