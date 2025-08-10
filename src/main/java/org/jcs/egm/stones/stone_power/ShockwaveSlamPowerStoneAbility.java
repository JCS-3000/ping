package org.jcs.egm.stones.stone_power;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.config.ModCommonConfig;
import org.jcs.egm.network.packet.ShakeCameraPacket;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneUseDamage;

import java.util.*;

public class ShockwaveSlamPowerStoneAbility implements IGStoneAbility {

    // ========= Tuning =========
    // Charge
    private static final int CHARGE_TICKS = 60;          // 3s charge at 20 TPS
    private static final boolean AUTO_FIRE_AT_FULL = true;

    // AoE
    private static final double RADIUS = 8.0D;
    private static final float DAMAGE = 12.0F;           // 6 hearts
    private static final double LIFT   = 0.5D;           // upward bump
    private static final double PUSH   = 2.0D;           // horizontal push

    // Particles (impact - Power Stone)
    private static final int CORE_BURST_COUNT = 960;
    private static final double CORE_BURST_SPREAD_XZ = 2.2;
    private static final double CORE_BURST_SPREAD_Y  = 0.9;
    private static final double CORE_BURST_SPEED     = 0.30;

    private static final int RING_SEGMENTS = 96;
    private static final int RING_LAYERS   = 4;
    private static final double RING_STEP  = 1.4;
    private static final double RING_Y_JITTER = 0.10;

    // Particles (charging)
    private static final int CHARGE_RING_POINTS = 48;
    private static final double CHARGE_RING_MIN = 0.8;
    private static final double CHARGE_RING_MAX = RING_LAYERS * RING_STEP; // grows to final ring size

    // Camera shake (caster only)
    private static final int   SHAKE_TICKS     = 14;
    private static final float SHAKE_INTENSITY = 1.1f;

    // ========= Sounds =========
    private static final SoundEvent CHARGING_SOUND = SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "power_stone_charging"));
    private static final SoundEvent POWER_STONE_EXPLOSION = SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "power_stone_explosion"));

    // Track who is currently playing the charging loop (client set)
    private static final Set<UUID> CHARGING_SOUND_PLAYERS = new HashSet<>();

    // ========= Per-player charge state (server only) =========
    private static final Map<UUID, Integer> CHARGE = new HashMap<>();

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        // No-op: we use onUsingTick/releaseUsing (hold-to-charge)
    }

    @Override
    public boolean canHoldUse() {
        return true; // ensure onUsingTick() fires while held
    }

    @Override
    public void onUsingTick(Level level, Player player, ItemStack stack, int count) {
        int useDuration = player.getUseItem().getUseDuration();
        int ticksHeld = useDuration - count;
        UUID id = player.getUUID();

        // --- CHARGING PHASE ---
        if (ticksHeld < CHARGE_TICKS) {
            if (!level.isClientSide) {
                CHARGE.put(id, ticksHeld);
            } else {
                if (!CHARGING_SOUND_PLAYERS.contains(id)) {
                    player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                            CHARGING_SOUND, SoundSource.PLAYERS,
                            0.9f, 1.0f, true); // loop
                    CHARGING_SOUND_PLAYERS.add(id);
                }
                if (ticksHeld % 4 == 0) spawnChargeParticles(level, player, ticksHeld);
            }
            return;
        }

        // --- AT/AFTER FULL CHARGE ---
        if (AUTO_FIRE_AT_FULL) {
            if (!level.isClientSide) {
                Integer prev = CHARGE.get(id);
                if (prev == null || prev < CHARGE_TICKS) {
                    CHARGE.put(id, CHARGE_TICKS);
                    doSlam(level, player);
                    if (player instanceof ServerPlayer sp) sp.stopUsingItem();
                }
            } else {
                stopChargingSoundClient(id);
            }
            return;
        }

        // If not auto-firing, keep showing "fully charged" effect client-side
        if (level.isClientSide) {
            if (ticksHeld % 4 == 0) spawnChargeParticles(level, player, CHARGE_TICKS);
        } else {
            CHARGE.put(id, CHARGE_TICKS);
        }
    }

    @Override
    public void releaseUsing(Level level, Player player, ItemStack stack, int count) {
        UUID id = player.getUUID();
        Integer charged = CHARGE.remove(id);

        // stop charging loop on release (client)
        if (level.isClientSide) stopChargingSoundClient(id);

        // If released early and not fully charged → do nothing
        if (charged == null || charged < CHARGE_TICKS) return;

        // If you disabled AUTO_FIRE_AT_FULL, releasing triggers the slam
        if (!level.isClientSide && !AUTO_FIRE_AT_FULL) {
            doSlam(level, player);
        }
    }

    // ========= Core effect =========
    private void doSlam(Level level, Player player) {
        // Self-cost on cast (matches your pattern)
        player.hurt(StoneUseDamage.get(level, player), 2.0F);

        Vec3 origin = player.position();

        // Impact sound (custom)
        level.playSound(null, player.blockPosition(), POWER_STONE_EXPLOSION, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (level instanceof ServerLevel server) {
            // 1) POWER STONE PARTICLES (your custom set)
            server.sendParticles(
                    ModParticles.POWER_STONE_EFFECT_TWO.get(),
                    origin.x, origin.y, origin.z,
                    CORE_BURST_COUNT,
                    CORE_BURST_SPREAD_XZ, CORE_BURST_SPREAD_Y, CORE_BURST_SPREAD_XZ,
                    CORE_BURST_SPEED
            );

            // 2) VANILLA EXPLOSION PARTICLES (centered)
            // Large, one-shot emitter + a few small bursts for extra pop
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, origin.x, origin.y, origin.z, 1, 0, 0, 0, 0.0);
            server.sendParticles(ParticleTypes.EXPLOSION, origin.x, origin.y + 0.1, origin.z, 12, 0.6, 0.2, 0.6, 0.15);

            // 3) VANILLA SMOKE (outward ring + upward plume)
            // Outward ring of LARGE_SMOKE to sell the ground shockwave
            double baseY = origin.y + 0.05;
            for (int layer = 1; layer <= RING_LAYERS; layer++) {
                double r = layer * RING_STEP;
                for (int i = 0; i < RING_SEGMENTS; i++) {
                    double theta = (2.0 * Math.PI) * (i / (double) RING_SEGMENTS);
                    double px = origin.x + Math.cos(theta) * r;
                    double pz = origin.z + Math.sin(theta) * r;
                    double py = baseY + (level.getRandom().nextDouble() - 0.5) * RING_Y_JITTER;

                    // Your existing ring (Power Stone)
                    server.sendParticles(
                            ModParticles.POWER_STONE_EFFECT_ONE.get(),
                            px, py, pz,
                            1,
                            0.0, 0.0, 0.0,
                            0.0
                    );

                    // Add LARGE_SMOKE at same ring points, with faint outward drift
                    double vx = Math.cos(theta) * 0.05;
                    double vz = Math.sin(theta) * 0.05;
                    server.sendParticles(ParticleTypes.LARGE_SMOKE, px, py + 0.1, pz, 1, 0.0, 0.0, 0.0, 0.0);
                    server.sendParticles(ParticleTypes.SMOKE, px, py + 0.1, pz, 2, 0.15, 0.02, 0.15, 0.02);

                    // Tiny hint of flames licking along the ring
                    server.sendParticles(ParticleTypes.FLAME, px, py + 0.05, pz, 1, 0.05, 0.01, 0.05, 0.005);
                }
            }

            // Upward smoke plume at center
            server.sendParticles(ParticleTypes.LARGE_SMOKE, origin.x, origin.y + 0.2, origin.z, 40, 0.4, 0.3, 0.4, 0.01);
            server.sendParticles(ParticleTypes.SMOKE, origin.x, origin.y + 0.2, origin.z, 60, 0.6, 0.4, 0.6, 0.02);

            // 4) VANILLA FLAMES (central flare + embers)
            server.sendParticles(ParticleTypes.FLAME, origin.x, origin.y + 0.1, origin.z, 40, 0.35, 0.1, 0.35, 0.01);
            // Scattered embers drifting outward
            server.sendParticles(ParticleTypes.FLAME, origin.x, origin.y + 0.1, origin.z, 80, 1.2, 0.2, 1.2, 0.03);
        }

        // --- AoE: knock + damage ---
        AABB box = new AABB(
                origin.x - RADIUS, origin.y - 1.0D, origin.z - RADIUS,
                origin.x + RADIUS, origin.y + 2.0D, origin.z + RADIUS
        );

        for (Entity e : level.getEntities(player, box)) {
            if (!(e instanceof LivingEntity target) || target == player) continue;

            Vec3 dir = target.position().subtract(origin);
            if (dir.lengthSqr() < 1.0E-6) dir = new Vec3(0, 0, 1);
            dir = dir.normalize().scale(PUSH);
            target.push(dir.x, LIFT, dir.z);

            target.hurt(level.damageSources().playerAttack(player), DAMAGE);
        }

        // Camera shake (caster only, config-gated)
        if (ModCommonConfig.ENABLE_CAMERA_SHAKE.get() && player instanceof ServerPlayer sp) {
            ShakeCameraPacket.send(sp, SHAKE_TICKS, SHAKE_INTENSITY);
        }
    }

    // ========= Visual charging ring (client) =========
    private void spawnChargeParticles(Level level, Player player, int ticksHeld) {
        double frac = Math.min(1.0, ticksHeld / (double) CHARGE_TICKS);
        double radius = CHARGE_RING_MIN + (CHARGE_RING_MAX - CHARGE_RING_MIN) * frac;

        Vec3 origin = player.position();
        double y = origin.y + 0.05;

        for (int i = 0; i < CHARGE_RING_POINTS; i++) {
            double theta = (2.0 * Math.PI) * (i / (double) CHARGE_RING_POINTS);
            double px = origin.x + Math.cos(theta) * radius;
            double pz = origin.z + Math.sin(theta) * radius;

            // Your subtle Power Stone charge ring
            level.addParticle(ModParticles.POWER_STONE_EFFECT_ONE.get(), px, y, pz, 0.0, 0.0, 0.0);

            // Light vanilla smoke hints as it charges
            if (i % 6 == 0) {
                level.addParticle(ParticleTypes.SMOKE, px, y + 0.05, pz, 0.0, 0.004, 0.0);
            }
        }
    }

    // ========= Client sound helpers =========
    private void stopChargingSoundClient(UUID id) {
        if (!CHARGING_SOUND_PLAYERS.contains(id)) return;
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.getSoundManager().stop(CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
        }
        CHARGING_SOUND_PLAYERS.remove(id);
    }
}
