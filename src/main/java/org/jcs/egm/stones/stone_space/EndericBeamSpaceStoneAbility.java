package org.jcs.egm.stones.stone_space;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;
import org.jcs.egm.network.NetworkHandler;
import org.jcs.egm.registry.ModParticles;

import java.util.List;

public class EndericBeamSpaceStoneAbility implements IGStoneAbility {

    private static final SoundEvent SPACE_TELEPORT_SOUND = 
            SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "space_teleport_effect"));

    @Override
    public String abilityKey() { return "enderic_beam"; }

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) return;

        double range = 64.0; // Increased range for more dramatic effect
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        // Play initial charging sound
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);

        HitResult hit = player.pick(range, 1.0F, false);
        Vec3 dest = switch (hit.getType()) {
            case BLOCK, ENTITY -> hit.getLocation();
            default -> eye.add(look.scale(range));
        };

        // Enhanced particle effects with Universal particles matching space stone color
        if (level instanceof ServerLevel server) {
            // New space stone colors: #2e3bc9 (main), #5c6bdb (lighter), #1a2699 (darker)
            // Main color: #2e3bc9 -> RGB: (46, 59, 201)
            float mainR = 0.18f, mainG = 0.23f, mainB = 0.79f;
            // Lighter shade: #5c6bdb -> RGB: (92, 107, 219) 
            float lightR = 0.36f, lightG = 0.42f, lightB = 0.86f;
            // Darker shade: #1a2699 -> RGB: (26, 38, 153)
            float darkR = 0.10f, darkG = 0.15f, darkB = 0.60f;
            
            int steps = 64; // More particles for smoother beam
            Vec3 step = dest.subtract(eye).scale(1.0 / steps);
            Vec3 p = eye;
            
            for (int i = 0; i < steps; i++) {
                // Primary beam trail with main color
                NetworkHandler.sendTintedParticle(server, ModParticles.UNIVERSAL_PARTICLE_ONE.get(),
                        p.x, p.y, p.z, mainR, mainG, mainB);
                NetworkHandler.sendTintedParticle(server, ModParticles.UNIVERSAL_PARTICLE_TWO.get(),
                        p.x, p.y, p.z, mainR, mainG, mainB);
                
                // Secondary sparkle effects with color variation
                if (i % 3 == 0) {
                    NetworkHandler.sendTintedParticle(server, ModParticles.UNIVERSAL_PARTICLE_THREE.get(),
                            p.x + (Math.random() - 0.5) * 0.1, 
                            p.y + (Math.random() - 0.5) * 0.1, 
                            p.z + (Math.random() - 0.5) * 0.1, 
                            lightR, lightG, lightB);
                }
                
                // Occasional darker accent particles
                if (i % 5 == 0) {
                    NetworkHandler.sendTintedParticle(server, ModParticles.UNIVERSAL_PARTICLE_FOUR.get(),
                            p.x + (Math.random() - 0.5) * 0.05, 
                            p.y + (Math.random() - 0.5) * 0.05, 
                            p.z + (Math.random() - 0.5) * 0.05, 
                            darkR, darkG, darkB);
                }
                
                // Damage entities caught in the beam
                AABB hitbox = new AABB(p.subtract(0.5, 0.5, 0.5), p.add(0.5, 0.5, 0.5));
                List<Entity> entities = level.getEntitiesOfClass(Entity.class, hitbox);
                
                for (Entity entity : entities) {
                    if (entity != player && entity instanceof LivingEntity living) {
                        // Deal void-like damage
                        living.hurt(level.damageSources().magic(), 4.0F);
                    }
                }
                
                p = p.add(step);
            }
            
            // Enhanced explosion-like particle burst at beam destination
            for (int i = 0; i < 30; i++) {
                double offsetX = (Math.random() - 0.5) * 2.5;
                double offsetY = (Math.random() - 0.5) * 2.5;
                double offsetZ = (Math.random() - 0.5) * 2.5;
                
                // Mix of all color shades for the destination burst
                if (i % 3 == 0) {
                    NetworkHandler.sendTintedParticle(server, ModParticles.UNIVERSAL_PARTICLE_THREE.get(),
                            dest.x + offsetX, dest.y + offsetY, dest.z + offsetZ, mainR, mainG, mainB);
                } else if (i % 3 == 1) {
                    NetworkHandler.sendTintedParticle(server, ModParticles.UNIVERSAL_PARTICLE_FOUR.get(),
                            dest.x + offsetX * 0.7, dest.y + offsetY * 0.7, dest.z + offsetZ * 0.7, 
                            lightR, lightG, lightB);
                } else {
                    NetworkHandler.sendTintedParticle(server, ModParticles.UNIVERSAL_PARTICLE_ONE.get(),
                            dest.x + offsetX * 0.5, dest.y + offsetY * 0.5, dest.z + offsetZ * 0.5, 
                            darkR, darkG, darkB);
                }
            }
        }

        // Enhanced teleportation with better destination finding
        if (player instanceof ServerPlayer sp) {
            Vec3 safeDest = findSafeTeleportDestination(level, dest);
            if (safeDest != null) {
                // Departure sound
                level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
                
                // Teleport the player
                sp.teleportTo(safeDest.x, safeDest.y, safeDest.z);
                
                // Arrival effects: sound and particle burst
                level.playSound(null, sp.blockPosition(), SPACE_TELEPORT_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
                
                // Teleport arrival particle burst
                if (level instanceof ServerLevel server) {
                    // Use the same color scheme for arrival burst
                    float mainR = 0.18f, mainG = 0.23f, mainB = 0.79f;
                    float lightR = 0.36f, lightG = 0.42f, lightB = 0.86f;
                    float darkR = 0.10f, darkG = 0.15f, darkB = 0.60f;
                    
                    for (int i = 0; i < 40; i++) {
                        double offsetX = (Math.random() - 0.5) * 3.0;
                        double offsetY = Math.random() * 2.0; // Upward bias
                        double offsetZ = (Math.random() - 0.5) * 3.0;
                        
                        // Radial burst pattern with color variation
                        if (i % 4 == 0) {
                            NetworkHandler.sendTintedParticle(server, ModParticles.UNIVERSAL_PARTICLE_ONE.get(),
                                    safeDest.x + offsetX, safeDest.y + offsetY, safeDest.z + offsetZ, 
                                    mainR, mainG, mainB);
                        } else if (i % 4 == 1) {
                            NetworkHandler.sendTintedParticle(server, ModParticles.UNIVERSAL_PARTICLE_TWO.get(),
                                    safeDest.x + offsetX * 0.7, safeDest.y + offsetY * 0.7, safeDest.z + offsetZ * 0.7, 
                                    lightR, lightG, lightB);
                        } else if (i % 4 == 2) {
                            NetworkHandler.sendTintedParticle(server, ModParticles.UNIVERSAL_PARTICLE_THREE.get(),
                                    safeDest.x + offsetX * 0.5, safeDest.y + offsetY * 0.5, safeDest.z + offsetZ * 0.5, 
                                    darkR, darkG, darkB);
                        } else {
                            NetworkHandler.sendTintedParticle(server, ModParticles.UNIVERSAL_PARTICLE_FOUR.get(),
                                    safeDest.x + offsetX * 0.3, safeDest.y + offsetY * 0.8, safeDest.z + offsetZ * 0.3, 
                                    mainR * 1.1f, mainG * 1.1f, mainB * 0.9f);
                        }
                    }
                }
            } else {
                level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_SCREAM, SoundSource.PLAYERS, 0.5F, 1.5F);
            }
        }

        // IMPORTANT: apply container-aware cooldown + player-persistent gate using the STONE stack
        StoneAbilityCooldowns.apply(player, stack, "space", this);
    }

    @Override
    public boolean canHoldUse() { return false; }
    
    /**
     * Finds a safe teleportation destination near the target location
     */
    private Vec3 findSafeTeleportDestination(Level level, Vec3 target) {
        // Try the exact target first
        if (isSafeLocation(level, target)) {
            return target;
        }
        
        // Search in expanding radius around target
        for (int radius = 1; radius <= 5; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dy = -2; dy <= 2; dy++) { // Check a few blocks up and down
                        Vec3 candidate = target.add(dx, dy, dz);
                        if (isSafeLocation(level, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        
        return null; // No safe location found
    }
    
    /**
     * Checks if a location is safe for teleportation
     */
    private boolean isSafeLocation(Level level, Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        BlockPos above = blockPos.above();
        
        // Check feet and head space are clear
        BlockState feetBlock = level.getBlockState(blockPos);
        BlockState headBlock = level.getBlockState(above);
        
        // Check floor is solid
        BlockState floorBlock = level.getBlockState(blockPos.below());
        
        return !feetBlock.blocksMotion() && 
               !headBlock.blocksMotion() && 
               floorBlock.blocksMotion() && 
               !isHazardousBlock(level, blockPos.below());
    }
    
    /**
     * Checks if a block is hazardous to stand on
     */
    private boolean isHazardousBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // Add checks for lava, fire, etc.
        return state.getBlock().toString().toLowerCase().contains("lava") ||
               state.getBlock().toString().toLowerCase().contains("fire") ||
               state.getBlock().toString().toLowerCase().contains("cactus");
    }
}
