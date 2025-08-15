package org.jcs.egm.stones.stone_soul;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.entity.SoulBeamEntity;
import org.jcs.egm.particles.ChargingParticleHelper;
import org.jcs.egm.registry.ModEntities;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LifeDrainSoulStoneAbility implements IGStoneAbility {

    @Override
    public String abilityKey() { return "life_drain"; }

    // Track active beams
    private final Map<UUID, SoulBeamEntity> activeBeams = new HashMap<>();
    
    // Track targets being drained
    private final Map<UUID, LivingEntity> drainingTargets = new HashMap<>();
    
    // Rate limiting for damage/healing intervals
    private final Map<UUID, Long> lastDrainTime = new HashMap<>();
    
    // Client sound state
    private final Set<UUID> chargingSoundPlayers = new HashSet<>();
    private final Set<UUID> drainingSoundPlayers = new HashSet<>();
    private final Map<UUID, Integer> drainingSoundStartTick = new HashMap<>();

    private static final int DRAINING_SOUND_LENGTH_TICKS = 160;
    private static final SoundEvent CHARGING_SOUND = SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("egm", "soul_stone_charging"));
    private static final SoundEvent DRAINING_SOUND = SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("egm", "soul_stone_firing"));

    @Override
    public void activate(Level level, Player player, ItemStack stack) {}

    @Override
    public boolean canHoldUse() { return true; }

    @Override
    public void onUsingTick(Level level, Player player, ItemStack stack, int count) {
        final String stone = "soul";
        final String ability = abilityKey();

        int useDuration = player.getUseItem().getUseDuration();
        int ticksHeld = useDuration - count;
        int chargeTicks = StoneAbilityCooldowns.chargeup(stone, ability);
        UUID uuid = player.getUUID();

        // --- CHARGING PHASE (2 seconds) ---
        if (ticksHeld < chargeTicks) {
            if (level.isClientSide && ticksHeld % 4 == 0) {
                ChargingParticleHelper.spawnSoulSuckInParticles(level, player);
            }
            if (level.isClientSide) {
                if (!chargingSoundPlayers.contains(uuid)) {
                    player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                            CHARGING_SOUND, SoundSource.PLAYERS, 0.8f, 1.0f, false);
                    chargingSoundPlayers.add(uuid);
                }
                if (drainingSoundPlayers.contains(uuid)) {
                    Minecraft.getInstance().getSoundManager().stop(DRAINING_SOUND.getLocation(), SoundSource.PLAYERS);
                    drainingSoundPlayers.remove(uuid);
                    drainingSoundStartTick.remove(uuid);
                }
            }
            return;
        }

        // --- DRAINING PHASE ---
        // Handle draining sound every tick (client-side)
        if (level.isClientSide) {
            if (chargingSoundPlayers.contains(uuid)) {
                Minecraft.getInstance().getSoundManager().stop(CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
                chargingSoundPlayers.remove(uuid);
            }
            int currentTick = Minecraft.getInstance().level != null ? (int)Minecraft.getInstance().level.getGameTime() : 0;
            Integer startedAt = drainingSoundStartTick.get(uuid);
            boolean needsNewSound = !drainingSoundPlayers.contains(uuid)
                    || startedAt == null
                    || (currentTick - startedAt) >= DRAINING_SOUND_LENGTH_TICKS;
            if (needsNewSound) {
                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                        DRAINING_SOUND, SoundSource.PLAYERS, 1.0f, 1.0f, true);
                drainingSoundPlayers.add(uuid);
                drainingSoundStartTick.put(uuid, currentTick);
            }
        }

        // Rate-limited server-side logic (1 damage/heal per second = every 20 ticks)
        long now = level.getGameTime();
        Long last = lastDrainTime.get(uuid);
        if (last != null && now - last < 20) {
            // Still update beam visuals for smoothness
            updateBeamOnly(level, player, uuid);
            return;
        }
        lastDrainTime.put(uuid, now);

        // ---- Raycast and life drain ----
        performLifeDrain(level, player, uuid);
    }

    @Override
    public void releaseUsing(Level level, Player player, ItemStack stack, int count) {
        UUID uuid = player.getUUID();

        // Stop sounds client-side
        if (level.isClientSide) {
            Minecraft.getInstance().getSoundManager().stop(CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
            Minecraft.getInstance().getSoundManager().stop(DRAINING_SOUND.getLocation(), SoundSource.PLAYERS);
            chargingSoundPlayers.remove(uuid);
            drainingSoundPlayers.remove(uuid);
            drainingSoundStartTick.remove(uuid);
        }

        // Remove beam server-side
        if (!level.isClientSide) {
            SoulBeamEntity beam = activeBeams.remove(uuid);
            if (beam != null && beam.isAlive()) beam.discard();
        } else {
            // Client side: just clear the reference
            activeBeams.remove(uuid);
        }

        // Clear drain target
        drainingTargets.remove(uuid);
        lastDrainTime.remove(uuid);

        // Apply centralized cooldown
        StoneAbilityCooldowns.apply(player, stack, "soul", this);
    }

    private void performLifeDrain(Level level, Player player, UUID uuid) {
        Vec3 eye = player.position().add(0, player.getEyeHeight(player.getPose()), 0);
        Vec3 look = player.getLookAngle();
        double range = 64.0D; // Extended range
        double step = 0.5D;

        Vec3 rayHit = null;
        LivingEntity target = null;

        // Raycast for living entities
        for (double d = 0; d <= range; d += step) {
            Vec3 pos = eye.add(look.scale(d));
            
            // Check for entities at this position
            for (Entity entity : level.getEntities(player, player.getBoundingBox().move(look.scale(d)).inflate(0.5D))) {
                if (entity instanceof LivingEntity livingEntity && entity != player) {
                    target = livingEntity;
                    rayHit = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
                    break;
                }
            }
            
            if (target != null) break;
        }

        if (rayHit == null) rayHit = eye.add(look.scale(range));

        // Update beam visuals
        updateBeam(level, player, uuid, eye, look, rayHit);

        // Server-side damage and healing
        if (!level.isClientSide && target != null) {
            // Drain 1 health from target
            target.hurt(level.damageSources().playerAttack(player), 1.0F);
            
            // Heal player by 1 health
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(1.0F);
            }
            
            // Store current target
            drainingTargets.put(uuid, target);
            
            // Particle effects on target
            if (level instanceof ServerLevel serverLevel) {
                Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2.0, 0);
                serverLevel.sendParticles(ModParticles.SOUL_STONE_EFFECT_ONE.get(),
                        targetPos.x, targetPos.y, targetPos.z, 8, 0.3, 0.3, 0.3, 0.05);
                
                // Healing particles on player
                Vec3 playerPos = player.position().add(0, player.getBbHeight() / 2.0, 0);
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                        playerPos.x, playerPos.y, playerPos.z, 3, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    // Keep beam visuals even on skipped intervals
    private void updateBeamOnly(Level level, Player player, UUID uuid) {
        Vec3 eye = player.position().add(0, player.getEyeHeight(player.getPose()), 0);
        Vec3 look = player.getLookAngle();
        Vec3 rayHit = eye.add(look.scale(64.0D));
        updateBeam(level, player, uuid, eye, look, rayHit);
    }

    private void updateBeam(Level level, Player player, UUID uuid, Vec3 eye, Vec3 look, Vec3 hit) {
        Vec3 chest = player.position().add(0, 1.2, 0);
        Vec3 renderStart = chest.add(look.scale(0.2));
        Vec3 renderEnd = hit;

        double toHit = renderStart.distanceTo(hit);
        double toEye = renderStart.distanceTo(eye);
        if (toHit < 0.1 || toHit < toEye) {
            renderEnd = renderStart;
        }

        // Only create and manage beam entities on server side
        if (!level.isClientSide) {
            SoulBeamEntity beam = activeBeams.get(uuid);
            if (beam == null || !beam.isAlive()) {
                beam = new SoulBeamEntity(ModEntities.SOUL_BEAM.get(), level);
                level.addFreshEntity(beam);
                activeBeams.put(uuid, beam);
            }
            beam.setEndpoints(renderStart, renderEnd);
        }

        // Server-side particle effects floating above/around the beam
        if (level instanceof ServerLevel serverLevel) {
            Vec3 beamVec = renderEnd.subtract(renderStart);
            double beamLength = beamVec.length();
            Vec3 direction = beamVec.normalize();
            int particlePoints = Math.max(8, (int) (beamLength * 0.5)); // Fewer, more spread out particles
            net.minecraft.util.RandomSource rand = serverLevel.random;
            
            // Create perpendicular vectors for floating particles around the beam
            Vec3 perp = direction.cross(new Vec3(0, 1, 0));
            if (perp.lengthSqr() < 1e-3) perp = direction.cross(new Vec3(1, 0, 0));
            perp = perp.normalize();
            Vec3 up = perp.cross(direction).normalize();
            
            for (int i = 0; i < particlePoints; i++) {
                double t = (i + rand.nextDouble() * 0.5) / (double) particlePoints;
                Vec3 base = renderStart.add(direction.scale(t * beamLength));
                
                // Create floating particles around the beam (not in it)
                double angle = rand.nextDouble() * Math.PI * 2;
                double radius = 0.8 + rand.nextDouble() * 0.4; // Float around beam, not in it
                double heightOffset = (rand.nextDouble() - 0.5) * 0.6;
                
                Vec3 offsetPos = base.add(
                    perp.scale(Math.cos(angle) * radius)
                    .add(up.scale(Math.sin(angle) * radius))
                    .add(0, heightOffset, 0)
                );
                
                // Slow drift towards beam center with some randomness
                Vec3 driftDirection = base.subtract(offsetPos).normalize();
                Vec3 velocity = driftDirection.scale(0.02 + rand.nextDouble() * 0.02)
                               .add((rand.nextDouble() - 0.5) * 0.01, 
                                    (rand.nextDouble() - 0.5) * 0.01, 
                                    (rand.nextDouble() - 0.5) * 0.01);
                
                serverLevel.sendParticles(ModParticles.CHARGING_PARTICLE_SOUL.get(),
                        offsetPos.x, offsetPos.y, offsetPos.z, 1, velocity.x, velocity.y, velocity.z, 0.0);
            }
        }
    }
}