package org.jcs.egm.particles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.registry.ModParticles;

/**
 * Reusable charging particle system for infinity stone abilities.
 * Creates particles that are "sucked in" towards the player during charging phases.
 */
public class ChargingParticleHelper {

    /**
     * Spawns charging particles that get sucked into the player's position.
     * Based on the Infinite Lightning ability's particle system.
     * 
     * @param level The level/world
     * @param player The player charging the ability
     * @param particle The particle type to spawn
     * @param particleCount Number of particles to spawn (default: 14)
     * @param outerRadius Radius where particles start (default: 2.2)
     */
    public static void spawnSuckInParticles(Level level, Player player, ParticleOptions particle, int particleCount, double outerRadius) {
        if (!level.isClientSide) return; // Only spawn on client side
        
        Vec3 playerCenter = player.position().add(0, 1.0, 0); // Center around player chest
        RandomSource rand = level.getRandom();
        
        for (int i = 0; i < particleCount; i++) {
            // Generate random spherical coordinates for particle start positions
            double theta = rand.nextDouble() * 2 * Math.PI; // Azimuth angle (0 to 2π)
            double phi = Math.acos(2 * rand.nextDouble() - 1); // Polar angle (-1 to 1 mapped to proper distribution)
            
            // Convert spherical to cartesian coordinates
            double x = outerRadius * Math.sin(phi) * Math.cos(theta);
            double y = outerRadius * Math.sin(phi) * Math.sin(theta);
            double z = outerRadius * Math.cos(phi);
            
            Vec3 startPos = playerCenter.add(x, y, z);
            
            // Calculate velocity vector pointing toward player center
            Vec3 velocityVec = playerCenter.subtract(startPos).normalize();
            
            // Add some randomization to velocity speed (0.22 to 0.34)
            double speed = 0.22 + rand.nextDouble() * 0.12;
            Vec3 velocity = velocityVec.scale(speed);
            
            // Spawn the specified particle with velocity toward player
            level.addParticle(particle,
                    startPos.x, startPos.y, startPos.z,
                    velocity.x, velocity.y, velocity.z);
        }
    }
    
    // ===== POWER STONE CHARGING PARTICLES =====
    
    /**
     * Spawns Power Stone charging particles with default parameters.
     */
    public static void spawnPowerSuckInParticles(Level level, Player player) {
        spawnSuckInParticles(level, player, ModParticles.CHARGING_PARTICLE_POWER.get(), 14, 2.2);
    }
    
    /**
     * Spawns intense Power Stone charging particles for powerful abilities.
     */
    public static void spawnIntensePowerSuckInParticles(Level level, Player player) {
        spawnSuckInParticles(level, player, ModParticles.CHARGING_PARTICLE_POWER.get(), 20, 2.8);
    }
    
    // ===== SPACE STONE CHARGING PARTICLES =====
    
    /**
     * Spawns Space Stone charging particles with default parameters.
     */
    public static void spawnSpaceSuckInParticles(Level level, Player player) {
        spawnSuckInParticles(level, player, ModParticles.CHARGING_PARTICLE_SPACE.get(), 14, 2.2);
    }
    
    /**
     * Spawns intense Space Stone charging particles for powerful abilities.
     */
    public static void spawnIntenseSpaceSuckInParticles(Level level, Player player) {
        spawnSuckInParticles(level, player, ModParticles.CHARGING_PARTICLE_SPACE.get(), 20, 2.8);
    }
    
    // ===== LEGACY METHODS (for backward compatibility) =====
    
    /**
     * @deprecated Use stone-specific methods instead
     */
    @Deprecated
    public static void spawnSuckInParticles(Level level, Player player) {
        spawnSuckInParticles(level, player, ParticleTypes.SOUL_FIRE_FLAME, 14, 2.2);
    }
    
    /**
     * @deprecated Use stone-specific methods instead
     */
    @Deprecated
    public static void spawnIntenseSuckInParticles(Level level, Player player) {
        spawnSuckInParticles(level, player, ParticleTypes.SOUL_FIRE_FLAME, 20, 2.8);
    }
    
    /**
     * @deprecated Use stone-specific methods instead
     */
    @Deprecated
    public static void spawnSubtleSuckInParticles(Level level, Player player) {
        spawnSuckInParticles(level, player, ParticleTypes.SOUL_FIRE_FLAME, 8, 1.8);
    }
}