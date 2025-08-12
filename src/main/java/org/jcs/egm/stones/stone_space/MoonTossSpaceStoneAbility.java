package org.jcs.egm.stones.stone_space;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import org.jcs.egm.entity.MeteorEntity;
import org.jcs.egm.registry.ModEntities;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;

import net.minecraft.util.RandomSource;
import java.util.*;

public class MoonTossSpaceStoneAbility implements IGStoneAbility {

    private static final int METEOR_COUNT = 20;
    private static final boolean AUTO_FIRE_AT_FULL = true;
    private static final Set<UUID> CHARGING_SOUND_PLAYERS = new HashSet<>();
    private static final Map<UUID, Integer> CHARGE = new HashMap<>();
    private static final SoundEvent SPACE_STONE_CHARGING_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("egm", "space_stone_charging"));

    @Override
    public String abilityKey() { return "moon_toss"; }

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        // This ability uses charge-up, so activate is empty
    }

    @Override
    public boolean canHoldUse() { return true; }

    @Override
    public void onUsingTick(Level level, Player player, ItemStack stack, int count) {
        final String stone = "space";
        final String ability = abilityKey();
        int useDuration = player.getUseItem().getUseDuration();
        int ticksHeld = useDuration - count;
        int chargeTicks = StoneAbilityCooldowns.chargeup(stone, ability);
        UUID id = player.getUUID();

        if (ticksHeld < chargeTicks) {
            if (!level.isClientSide) {
                CHARGE.put(id, ticksHeld);
            } else {
                if (!CHARGING_SOUND_PLAYERS.contains(id)) {
                    player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SPACE_STONE_CHARGING_SOUND, SoundSource.PLAYERS,
                            0.9f, 1.0f, true);
                    CHARGING_SOUND_PLAYERS.add(id);
                }
                if (ticksHeld % 4 == 0) {
                    spawnChargingParticles(level, player);
                    spawnTargetingBeam(level, player);
                }
            }
            return;
        }

        if (AUTO_FIRE_AT_FULL) {
            if (!level.isClientSide) {
                Integer prev = CHARGE.get(id);
                if (prev == null || prev < chargeTicks) {
                    CHARGE.put(id, chargeTicks);
                    executeMoonToss(level, player, stack);
                    if (player instanceof ServerPlayer sp) sp.stopUsingItem();
                }
            } else {
                stopChargingSoundClient(id);
            }
            return;
        }

        if (level.isClientSide) {
            if (ticksHeld % 4 == 0) {
                spawnChargingParticles(level, player);
                spawnTargetingBeam(level, player);
            }
        } else {
            CHARGE.put(id, chargeTicks);
        }
    }

    @Override
    public void releaseUsing(Level level, Player player, ItemStack stack, int timeLeft) {
        final String stone = "space";
        UUID id = player.getUUID();
        Integer charged = CHARGE.remove(id);

        if (level.isClientSide) stopChargingSoundClient(id);
        if (charged == null || charged < StoneAbilityCooldowns.chargeup(stone, abilityKey())) return;

        if (!level.isClientSide && !AUTO_FIRE_AT_FULL) {
            executeMoonToss(level, player, stack);
        }
    }

    private void executeMoonToss(Level level, Player player, ItemStack stack) {
        // Find target location
        Vec3 targetLocation = getTargetLocation(level, player);
        if (targetLocation == null) return;

        // Spawn 5 meteors above the target area
        spawnMeteors((ServerLevel) level, player, targetLocation);

        // Apply cooldown
        StoneAbilityCooldowns.apply(player, stack, "space", this);
    }

    private void stopChargingSoundClient(UUID id) {
        if (!CHARGING_SOUND_PLAYERS.contains(id)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.getSoundManager().stop(SPACE_STONE_CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
        }
        CHARGING_SOUND_PLAYERS.remove(id);
    }

    private Vec3 getTargetLocation(Level level, Player player) {
        double range = 64.0;
        HitResult hit = player.pick(range, 1.0F, false);
        
        switch (hit.getType()) {
            case BLOCK:
                return hit.getLocation();
            case ENTITY:
                Entity entity = ((net.minecraft.world.phys.EntityHitResult) hit).getEntity();
                return entity.position();
            default:
                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                return eye.add(look.scale(range));
        }
    }

    private void spawnMeteors(ServerLevel level, Player player, Vec3 targetLocation) {
        RandomSource random = level.getRandom();
        
        // Space stone colors from the provided hex values
        // #091B93 -> RGB: (9, 27, 147)
        float darkR = 9f/255f, darkG = 27f/255f, darkB = 147f/255f;
        // #000D66 -> RGB: (0, 13, 102)  
        float darkerR = 0f/255f, darkerG = 13f/255f, darkerB = 102f/255f;
        // #0019FF -> RGB: (0, 25, 255)
        float brightR = 0f/255f, brightG = 25f/255f, brightB = 1f;
        
        // Create single massive ring around the entire landing area
        spawnMassiveLandingRing(level, targetLocation, darkR, darkG, darkB, brightR, brightG, brightB);
        
        // Spawn meteors with different delays and positions
        for (int i = 0; i < METEOR_COUNT; i++) {
            // Random position within 8 blocks of target
            double offsetX = (random.nextDouble() - 0.5) * 16; // -8 to +8
            double offsetZ = (random.nextDouble() - 0.5) * 16; // -8 to +8
            Vec3 meteorTarget = targetLocation.add(offsetX, 0, offsetZ);
            
            // Spawn meteor high above the target
            Vec3 spawnPos = meteorTarget.add(0, 50 + random.nextInt(20), 0);
            
            // Determine if this meteor should be stone (60%) or blackstone (40%)
            boolean isStone = i < (METEOR_COUNT * 0.6);
            
            // Calculate delay (0-40 ticks, random)
            int delay = random.nextInt(41);
            
            // Create meteor entity
            MeteorEntity meteor = new MeteorEntity(ModEntities.METEOR.get(), level, 
                    spawnPos, meteorTarget, isStone, delay, player.getUUID());
            level.addFreshEntity(meteor);
        }
    }
    
    private void spawnMassiveLandingRing(ServerLevel level, Vec3 centerLocation, 
                                        float darkR, float darkG, float darkB,
                                        float brightR, float brightG, float brightB) {
        // Create a massive ring encompassing the entire 16-block landing area
        int ringPoints = 128; // More points for a smoother massive ring
        double radius = 10.0; // Larger radius to encompass the whole area
        
        // Create dust particle options for all three colors
        DustParticleOptions darkDust = new DustParticleOptions(new Vector3f(darkR, darkG, darkB), 1.2f);
        DustParticleOptions darkerDust = new DustParticleOptions(new Vector3f(0f/255f, 13f/255f, 102f/255f), 1.2f);
        DustParticleOptions brightDust = new DustParticleOptions(new Vector3f(brightR, brightG, brightB), 1.2f);
        
        for (int layer = 0; layer < 3; layer++) { // Multiple concentric rings
            double layerRadius = radius + (layer * 1.5);
            for (int i = 0; i < ringPoints; i++) {
                double angle = (2.0 * Math.PI) * (i / (double) ringPoints);
                double x = centerLocation.x + Math.cos(angle) * layerRadius;
                double z = centerLocation.z + Math.sin(angle) * layerRadius;
                double y = centerLocation.y + 0.1 + (layer * 0.2);
                
                // Cycle through all three space stone colors using dust particles
                if (i % 3 == 0) {
                    level.sendParticles(brightDust, x, y, z, 1, 0, 0, 0, 0);
                } else if (i % 3 == 1) {
                    level.sendParticles(darkDust, x, y, z, 1, 0, 0, 0, 0);
                } else {
                    level.sendParticles(darkerDust, x, y, z, 1, 0, 0, 0, 0);
                }
            }
        }
    }
    
    private void spawnTargetingBeam(Level level, Player player) {
        if (!level.isClientSide) return;
        
        double range = 64.0;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        
        HitResult hit = player.pick(range, 1.0F, false);
        Vec3 dest = switch (hit.getType()) {
            case BLOCK, ENTITY -> hit.getLocation();
            default -> eye.add(look.scale(range));
        };
        
        // Create beam similar to Enderic Beam but with space stone colors using dust particles
        // Space stone colors
        DustParticleOptions darkDust = new DustParticleOptions(new Vector3f(9f/255f, 27f/255f, 147f/255f), 0.8f);
        DustParticleOptions brightDust = new DustParticleOptions(new Vector3f(0f/255f, 25f/255f, 1f), 0.8f);
        
        int steps = 32; // Fewer steps for charging beam
        Vec3 step = dest.subtract(eye).scale(1.0 / steps);
        Vec3 p = eye;
        
        for (int i = 0; i < steps; i++) {
            // Targeting beam with space stone colors using dust particles (client-side)
            if (i % 2 == 0) {
                level.addParticle(brightDust, p.x, p.y, p.z, 0, 0, 0);
            } else {
                level.addParticle(darkDust, p.x, p.y, p.z, 0, 0, 0);
            }
            p = p.add(step);
        }
    }
    
    private void spawnChargingParticles(Level level, Player player) {
        if (!level.isClientSide) return;
        
        Vec3 playerPos = player.position().add(0, 1.0, 0);
        Vec3 gauntletPos = player.position().add(0, 1.2, 0); // Gauntlet/hand position
        RandomSource rand = level.getRandom();
        
        // Space stone colors for firework trails
        DustParticleOptions darkDust = new DustParticleOptions(new Vector3f(9f/255f, 27f/255f, 147f/255f), 1.0f);
        DustParticleOptions darkerDust = new DustParticleOptions(new Vector3f(0f/255f, 13f/255f, 102f/255f), 1.0f);
        DustParticleOptions brightDust = new DustParticleOptions(new Vector3f(0f/255f, 25f/255f, 1f), 1.0f);
        
        // Create firework trails getting sucked into the gauntlet
        for (int i = 0; i < 15; i++) {
            // Start particles in a wide area around the player
            double startRadius = 4.0 + rand.nextDouble() * 2.0;
            double angle = rand.nextDouble() * 2 * Math.PI;
            double height = rand.nextDouble() * 3.0 - 1.0; // -1 to +2 blocks
            
            Vec3 startPos = playerPos.add(
                Math.cos(angle) * startRadius,
                height,
                Math.sin(angle) * startRadius
            );
            
            // Calculate velocity toward gauntlet
            Vec3 toGauntlet = gauntletPos.subtract(startPos).normalize();
            Vec3 velocity = toGauntlet.scale(0.2 + rand.nextDouble() * 0.1);
            
            // Cycle through the three space stone colors for firework trail particles
            DustParticleOptions chosenDust;
            if (i % 3 == 0) {
                chosenDust = brightDust;
            } else if (i % 3 == 1) {
                chosenDust = darkDust;
            } else {
                chosenDust = darkerDust;
            }
            
            // Spawn firework trail particles with velocity toward gauntlet (client-side)
            level.addParticle(chosenDust,
                    startPos.x, startPos.y, startPos.z,
                    velocity.x, velocity.y, velocity.z);
            
            // Add some regular firework particles too for the trail effect
            level.addParticle(net.minecraft.core.particles.ParticleTypes.FIREWORK,
                    startPos.x, startPos.y, startPos.z,
                    velocity.x * 0.5, velocity.y * 0.5, velocity.z * 0.5);
        }
    }
}