package org.jcs.egm.stones.stone_time;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.entity.TimeBubbleFieldEntity;
import org.jcs.egm.network.NetworkHandler;
import org.jcs.egm.registry.ModEntities;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;

import java.util.*;

public class TimeBubbleTimeStoneAbility implements IGStoneAbility {

    @Override public String abilityKey() { return "bubble"; }
    @Override public boolean canHoldUse() { return true; }
    @Override public void activate(Level level, Player player, ItemStack stack) {}

    // Charge/UX
    private static final int CHARGE_TICKS = 80;
    private static final boolean AUTO_FIRE_AT_FULL = true;

    // Effect params
    private static final int   RADIUS_BLOCKS     = 16;
    private static final int   DURATION_TICKS    = 20 * 60;
    private static final int   TARGET_TICKSPEED  = 1200;
    private static final int   FIREWORK_POINTS   = 420;

    // Sounds
    private static final SoundEvent CHARGING_SOUND =
            SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "time_stone_charging"));
    private static final SoundEvent TWINKLE_SOUND =
            SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "universal_twinkle"));

    // Colors
    private static final int COLOR_A = 0x62FF2D;
    private static final int COLOR_B = 0x0AAA67;

    // Client bookkeeping
    private static final Set<UUID> CHARGING_SOUND_PLAYERS = new HashSet<>();
    private static final Map<UUID, Integer> CHARGE = new HashMap<>();

    @Override
    public void onUsingTick(Level level, Player player, ItemStack stack, int count) {
        final UUID id = player.getUUID();
        final int useDuration = player.getUseItem().getUseDuration();
        final int ticksHeld = useDuration - count;

        if (ticksHeld < CHARGE_TICKS) {
            if (!level.isClientSide) {
                CHARGE.put(id, ticksHeld);
                if ((player.tickCount & 1) == 0) {
                    NetworkHandler.sendWristRing(player, ticksHeld, COLOR_A, COLOR_B);
                }
            } else {
                if (!CHARGING_SOUND_PLAYERS.contains(id)) {
                    level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            CHARGING_SOUND, SoundSource.PLAYERS, 0.9f, 1.0f, true);
                    CHARGING_SOUND_PLAYERS.add(id);
                }
            }
            return;
        }

        if (!level.isClientSide && (player.tickCount & 1) == 0) {
            NetworkHandler.sendWristRing(player, ticksHeld, COLOR_A, COLOR_B);
        }

        if (AUTO_FIRE_AT_FULL && !level.isClientSide) {
            Integer prev = CHARGE.get(id);
            if (prev == null || prev < CHARGE_TICKS) {
                CHARGE.put(id, CHARGE_TICKS);
                fire(level, player, stack);
                if (player instanceof ServerPlayer sp) sp.stopUsingItem();
            }
        } else if (AUTO_FIRE_AT_FULL && level.isClientSide) {
            stopChargingSoundClient(id);
            // Mirror the big burst locally so it uses addParticle()
            spawnSphericalBurstClient(level, player.position().add(0, player.getBbHeight() * 0.5, 0));
        }
    }

    @Override
    public void releaseUsing(Level level, Player player, ItemStack stack, int count) {
        final UUID id = player.getUUID();
        Integer charged = CHARGE.remove(id);

        if (level.isClientSide) stopChargingSoundClient(id);
        if (AUTO_FIRE_AT_FULL) return;

        if (charged == null || charged < CHARGE_TICKS) return;
        if (!level.isClientSide) {
            fire(level, player, stack);
        } else {
            spawnSphericalBurstClient(level, player.position().add(0, player.getBbHeight() * 0.5, 0));
        }
    }

    private void fire(Level level, Player player, ItemStack stoneStack) {
        if (StoneAbilityCooldowns.guardUse(player, stoneStack, "time", this)) return;

        // SFX
        level.playSound(null, player.blockPosition(), TWINKLE_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);

        // Server broadcast burst for other players
        if (level instanceof ServerLevel sl) {
            spawnSphericalBurst(sl, player.position().add(0, player.getBbHeight() * 0.5, 0));
        }

        // Spawn the logical field
        if (!level.isClientSide) {
            ServerLevel sl = (ServerLevel) level;
            TimeBubbleFieldEntity field = new TimeBubbleFieldEntity(ModEntities.TIME_ACCEL_FIELD.get(), sl)
                    .configure(player.getUUID(), RADIUS_BLOCKS, TARGET_TICKSPEED, DURATION_TICKS, COLOR_A, COLOR_B);
            field.setPos(player.getX(), player.getY(), player.getZ());
            sl.addFreshEntity(field);
        }

        // Cooldown
        StoneAbilityCooldowns.apply(player, stoneStack, "time", this);
    }

    // ===== visuals (unchanged bursts) =====
    private void spawnSphericalBurst(ServerLevel sl, Vec3 origin) {
        RandomSource r = sl.random;
        for (int i = 0; i < FIREWORK_POINTS; i++) {
            double u = r.nextDouble();
            double v = r.nextDouble();
            double theta = 2 * Math.PI * u;
            double phi = Math.acos(2 * v - 1);
            double sx = Math.sin(phi) * Math.cos(theta);
            double sy = Math.cos(phi);
            double sz = Math.sin(phi) * Math.sin(theta);
            double radius = 2.0 + r.nextDouble() * 1.8;

            double px = origin.x + sx * radius;
            double py = origin.y + sy * radius;
            double pz = origin.z + sz * radius;

            int hex = (i & 1) == 0 ? COLOR_A : COLOR_B;
            float[] rgb = rgb01(hex);

            sl.sendParticles(ModParticles.UNIVERSAL_PARTICLE_ONE.get(),
                    px, py, pz, 0, rgb[0], rgb[1], rgb[2], 0.0);
        }
    }

    private void spawnSphericalBurstClient(Level level, Vec3 origin) {
        final int points = FIREWORK_POINTS;
        final java.util.Random r = new java.util.Random();

        for (int i = 0; i < points; i++) {
            double u = r.nextDouble();
            double v = r.nextDouble();
            double theta = 2 * Math.PI * u;
            double phi = Math.acos(2 * v - 1);
            double sx = Math.sin(phi) * Math.cos(theta);
            double sy = Math.cos(phi);
            double sz = Math.sin(phi) * Math.sin(theta);
            double radius = 2.0 + r.nextDouble() * 1.8;

            double px = origin.x + sx * radius;
            double py = origin.y + sy * radius;
            double pz = origin.z + sz * radius;

            int hex = (i & 1) == 0 ? COLOR_A : COLOR_B;
            float[] rgb = rgb01(hex);

            level.addParticle(ModParticles.UNIVERSAL_PARTICLE_ONE.get(), px, py, pz, rgb[0], rgb[1], rgb[2]);
        }
    }

    private static float[] rgb01(int hex) {
        return new float[]{ ((hex >> 16) & 0xFF) / 255f, ((hex >> 8) & 0xFF) / 255f, (hex & 0xFF) / 255f };
    }

    private void stopChargingSoundClient(UUID id) {
        if (!CHARGING_SOUND_PLAYERS.contains(id)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.getSoundManager().stop(CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
        CHARGING_SOUND_PLAYERS.remove(id);
    }
}
