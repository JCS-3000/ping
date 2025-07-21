package org.jcs.egm.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class PowerBeam {
    public static final int PARTICLES_PER_SEGMENT = 5; // More particles = denser beam

    public static void spawn(Level level, Vec3 from, Vec3 to) {
        if (level == null || !level.isClientSide) return;

        Random rand = (Random) Minecraft.getInstance().level.getRandom();
        int segments = 10; // More segments = smoother arc
        Vec3[] points = new Vec3[segments + 1];

        // Generate jagged points
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 point = from.lerp(to, t);
            if (i != 0 && i != segments) {
                double offsetScale = 0.8;
                // Lightning-style offsets
                double dx = (rand.nextDouble() - 0.5) * offsetScale;
                double dy = (rand.nextDouble() - 0.5) * offsetScale;
                double dz = (rand.nextDouble() - 0.5) * offsetScale;
                point = point.add(dx, dy, dz);
            }
            points[i] = point;
        }

        // Draw the arc between all segments!
        for (int i = 0; i < segments; i++) {
            Vec3 start = points[i];
            Vec3 end = points[i + 1];
            int steps = 5; // More steps = more evenly spaced particles between each segment

            for (int s = 0; s <= steps; s++) {
                double t = s / (double) steps;
                Vec3 pos = start.lerp(end, t);
                level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        }
    }
}
