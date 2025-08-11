package org.jcs.egm.client.fx;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jcs.egm.registry.ModParticles;

/**
 * Side-safe particle helpers that obey vanilla broadcast range.
 * Color is encoded via vx,vy,vz = r,g,b in [0..1]. Always use count=0 when spawning from server.
 */
public final class FXParticles {
    private FXParticles() {}

    public enum Universal {
        ONE, TWO, THREE, FOUR
    }

    private static SimpleParticleType typeOf(Universal kind) {
        return switch (kind) {
            case ONE   -> ModParticles.UNIVERSAL_PARTICLE_ONE.get();
            case TWO   -> ModParticles.UNIVERSAL_PARTICLE_TWO.get();
            case THREE -> ModParticles.UNIVERSAL_PARTICLE_THREE.get();
            case FOUR  -> ModParticles.UNIVERSAL_PARTICLE_FOUR.get();
        };
    }

    private static float[] rgb(int hex) {
        return new float[] {
                ((hex >> 16) & 0xFF) / 255f,
                ((hex >>  8) & 0xFF) / 255f,
                ( hex        & 0xFF) / 255f
        };
    }

    /**
     * Spawns a single tintable universal particle.
     * - On SERVER: uses ServerLevel#sendParticles(...) (vanilla range rules).
     * - On CLIENT: uses Level#addParticle(...) (local-only).
     *
     * IMPORTANT: Keep 'count = 0' on server so r,g,b are not randomized.
     */
    public static void universal(Level level, double x, double y, double z, Universal kind, int hex) {
        float[] c = rgb(hex);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(typeOf(kind), x, y, z, /*count*/0, c[0], c[1], c[2], /*speed*/0.0);
        } else {
            level.addParticle(typeOf(kind), x, y, z, c[0], c[1], c[2]);
        }
    }

    /** Convenience burst: calls {@link #universal} 'repeats' times. */
    public static void universalBurst(Level level, double x, double y, double z, Universal kind, int hex, int repeats) {
        for (int i = 0; i < repeats; i++) {
            universal(level, x, y, z, kind, hex);
        }
    }
}
