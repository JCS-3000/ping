package org.jcs.egm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class PowerBeam {
    // Number of particles per segment (change as desired)
    public static final int PARTICLES_PER_SEGMENT = 3; // <--- Adjust for density

    // Spawns a big, jagged arc of particles between 'from' and 'to'
    public static void spawn(Level level, Vec3 from, Vec3 to) {
        if (level == null || level.isClientSide == false) return;

        Random rand = (Random) Minecraft.getInstance().level.getRandom();
        int segments = 10; // Number of segments for jaggedness (adjust for more/less curve)
        Vec3[] points = new Vec3[segments + 1];

        // Generate jagged points
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 point = from.lerp(to, t);

            // Apply big random perpendicular offsets (not at endpoints)
            if (i != 0 && i != segments) {
                double offsetScale = 0.8; // How wild the arc is (adjust!)
                double dx = (rand.nextDouble() - 0.5) * offsetScale;
                double dy = (rand.nextDouble() - 0.5) * offsetScale;
                double dz = (rand.nextDouble() - 0.5) * offsetScale;
                point = point.add(dx, dy, dz);
            }

            points[i] = point;
        }

        // Draw line segments with particles
        for (int i = 0; i < segments; i++) {
            Vec3 start = points[i];
            Vec3 end = points[i + 1];
            Vec3 diff = end.subtract(start);
            double length = diff.length();
            int steps = (int) (length * 10);

            for (int p = 0; p < PARTICLES_PER_SEGMENT; p++) {
                double t = p / (double) PARTICLES_PER_SEGMENT;
                Vec3 pos = start.lerp(end, t);

                // Your mod's particle here (client registry assumed)
                level.addParticle(org.jcs.egm.registry.ModParticles.POWER_STONE_EFFECT_ONE.get(),
                        pos.x, pos.y, pos.z,
                        0, 0, 0);
            }
        }
    }
}
