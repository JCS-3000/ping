package org.jcs.egm.stones.stone_time;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.network.NetworkHandler;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber(modid = "egm", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TimeFreezeTimeStoneAbility implements IGStoneAbility {

    @Override public String abilityKey() { return "freeze"; }
    @Override public boolean canHoldUse() { return true; }

    private static final int CHARGE_TICKS = 80;
    private static final boolean AUTO_FIRE_AT_FULL = true;
    private static final double AOE_RADIUS = 10.0;
    private static final int FREEZE_TICKS = 200;
    private static final int SLOWNESS_LEVEL = 255;
    private static final int DOME_DURATION_TICKS = 200;

    private static final int COLOR_A_HEX = 0x62FF2D;
    private static final int COLOR_B_HEX = 0x0AAA67;

    private static final SoundEvent CHARGING_SOUND =
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("egm", "time_stone_charging"));
    private static final SoundEvent ACTIVATE_SOUND =
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("egm", "universal_twinkle"));
    private static final SoundEvent RARE_ACTIVATE_SOUND =
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("egm", "how_strange"));

    private static final Set<UUID> CHARGING_SOUND_PLAYERS = new HashSet<>();
    private static final Map<UUID, Integer> CHARGE = new HashMap<>();
    
    /**
     * Returns the activation sound - 95% chance for universal_twinkle, 5% chance for how_strange
     */
    private static SoundEvent getRandomActivationSound() {
        return ThreadLocalRandom.current().nextDouble() < 0.05 ? RARE_ACTIVATE_SOUND : ACTIVATE_SOUND;
    }

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (StoneAbilityCooldowns.guardUse(player, stack, "time", this)) return;
        CHARGE.remove(player.getUUID());
    }

    @Override
    public void onUsingTick(Level level, Player player, ItemStack stack, int count) {
        final UUID id = player.getUUID();
        final int useDuration = player.getUseItem().getUseDuration();
        final int ticksHeld = useDuration - count;

        if (ticksHeld < CHARGE_TICKS) {
            if (!level.isClientSide) {
                CHARGE.put(id, ticksHeld);
                if ((player.tickCount & 1) == 0) {
                    NetworkHandler.sendWristRing(player, ticksHeld, COLOR_A_HEX, COLOR_B_HEX);
                }
            } else if (!CHARGING_SOUND_PLAYERS.contains(id)) {
                level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                        CHARGING_SOUND, SoundSource.PLAYERS, 0.9f, 1.0f, true);
                CHARGING_SOUND_PLAYERS.add(id);
            }
            return;
        }

        if (!level.isClientSide && (player.tickCount & 1) == 0) {
            NetworkHandler.sendWristRing(player, ticksHeld, COLOR_A_HEX, COLOR_B_HEX);
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
            level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                    getRandomActivationSound(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
        }
    }

    @Override
    public void releaseUsing(Level level, Player player, ItemStack stack, int count) {
        final UUID id = player.getUUID();
        Integer charged = CHARGE.remove(id);

        if (level.isClientSide) stopChargingSoundClient(id);
        if (AUTO_FIRE_AT_FULL) return;
        if (charged == null || charged < CHARGE_TICKS) return;

        if (level.isClientSide) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                    getRandomActivationSound(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
        } else {
            fire(level, player, stack);
        }
    }

    // ===== Core effect (server) =====
    private void fire(Level level, Player player, ItemStack stoneStack) {
        if (StoneAbilityCooldowns.guardUse(player, stoneStack, "time", this)) return;

        level.playSound(null, player.blockPosition(), getRandomActivationSound(), SoundSource.PLAYERS, 1.0F, 1.0F);

        // Visual burst first, then persistent dome
        Vec3 origin = player.position().add(0, 0.2, 0);

        if (level instanceof ServerLevel sl) {
            BurstTicker.spawn(sl, origin, AOE_RADIUS, COLOR_A_HEX, COLOR_B_HEX);
            DomeTicker.spawn(sl, origin, AOE_RADIUS, DOME_DURATION_TICKS, COLOR_A_HEX, COLOR_B_HEX);

            // A few accent vanilla particles right away (server -> everyone)
            sl.sendParticles(ParticleTypes.EXPLOSION, origin.x, origin.y + 0.1, origin.z, 1, 0, 0, 0, 0.0);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, origin.x, origin.y + 0.1, origin.z, 1, 0, 0, 0, 0.0);
            // Subtle sonic shock line (very sparse)
            sl.sendParticles(ParticleTypes.SONIC_BOOM, origin.x, origin.y + 0.2, origin.z, 1, 0, 0, 0, 0.0);
            // Rising enchant motes for “time magic” vibes
            sl.sendParticles(ParticleTypes.ENCHANT, origin.x, origin.y + 0.5, origin.z, 24, 0.6, 0.2, 0.6, 0.0);
            // A little smoke that will dissipate
            sl.sendParticles(ParticleTypes.CLOUD, origin.x, origin.y + 0.2, origin.z, 12, 0.25, 0.10, 0.25, 0.01);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, origin.x, origin.y + 0.2, origin.z, 6, 0.20, 0.05, 0.20, 0.01);
        }

        if (!level.isClientSide) {
            final AABB box = player.getBoundingBox().inflate(AOE_RADIUS);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
                le.setDeltaMovement(Vec3.ZERO);
                le.hurtMarked = true;
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_TICKS, SLOWNESS_LEVEL, false, true, true));
            }
            StoneAbilityCooldowns.apply(player, stoneStack, "time", this);
        }
    }

    private static void stopChargingSoundClient(UUID id) {
        if (!CHARGING_SOUND_PLAYERS.contains(id)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.getSoundManager().stop(CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
        CHARGING_SOUND_PLAYERS.remove(id);
    }

    // ===== EXPANDING BURST (SERVER) =====
    @Mod.EventBusSubscriber(modid = "egm", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class BurstTicker {
        private static final class Burst {
            final Vec3 origin;
            final double maxRadius;
            final int colorAHex, colorBHex;
            int age = 0;
            Burst(Vec3 origin, double maxRadius, int colorAHex, int colorBHex) {
                this.origin = origin; this.maxRadius = maxRadius;
                this.colorAHex = colorAHex; this.colorBHex = colorBHex;
            }
        }

        // Tunables
        private static final int DURATION_TICKS = 18;      // how long the expanding shell lasts
        private static final double SPEED_PER_TICK = 0.75; // expansion speed
        private static final int RINGS_PER_TICK = 3;       // number of horizontal rings emitted
        private static final int BASE_POINTS = 18;         // points per ring at peak radius
        private static final double HEMISPHERE = Math.PI * 0.5; // emit only upward hemisphere

        private static final Map<ServerLevel, List<Burst>> SERVER_BURSTS = new IdentityHashMap<>();

        public static void spawn(ServerLevel level, Vec3 origin, double maxRadius, int colorAHex, int colorBHex) {
            SERVER_BURSTS.computeIfAbsent(level, k -> new ArrayList<>())
                    .add(new Burst(origin, maxRadius, colorAHex, colorBHex));
        }

        @SubscribeEvent
        public static void onLevelTick(TickEvent.LevelTickEvent e) {
            if (e.phase != TickEvent.Phase.END || e.level.isClientSide || !(e.level instanceof ServerLevel sl)) return;
            List<Burst> list = SERVER_BURSTS.get(sl);
            if (list == null || list.isEmpty()) return;

            list.removeIf(b -> {
                emitBurst(sl, b);
                b.age++;
                return b.age >= DURATION_TICKS;
            });
        }

        private static void emitBurst(ServerLevel sl, Burst b) {
            double t = b.age + 1;
            double radius = Math.min(b.maxRadius, t * SPEED_PER_TICK);

            float[] A = rgb01(b.colorAHex), B = rgb01(b.colorBHex);

            // Emit a few horizontal rings across the hemisphere
            for (int r = 0; r < RINGS_PER_TICK; r++) {
                double frac = (r + 0.5) / (double) RINGS_PER_TICK; // 0..1
                double phi = HEMISPHERE * frac; // 0 (top) .. π/2 (horizon)
                double ringR = radius * Math.sin(phi);
                double y = b.origin.y + radius * Math.cos(phi);

                int points = Math.max(10, (int) Math.round(BASE_POINTS * (ringR / Math.max(0.001, b.maxRadius))));
                for (int i = 0; i < points; i++) {
                    double theta = (2 * Math.PI) * (i / (double) points);
                    double x = b.origin.x + ringR * Math.cos(theta);
                    double z = b.origin.z + ringR * Math.sin(theta);
                    float[] rgb = ((i + r + b.age) & 1) == 0 ? A : B;

                    // Your tinted “time” motes — match NetworkHandler signature exactly
                    NetworkHandler.sendTintedParticle(sl, ModParticles.UNIVERSAL_PARTICLE_ONE.get(),
                            x, y, z, rgb[0], rgb[1], rgb[2]);

                    // A few vanilla accents mixed in (sparse)
                    if ((i & 7) == 0) {
                        // drifting enchant + cloud along the wavefront
                        sl.sendParticles(ParticleTypes.ENCHANT, x, y + 0.05, z, 2, 0.03, 0.02, 0.03, 0.0);
                        sl.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.02, 0.01, 0.02, 0.01);
                    }
                    if ((i & 15) == 0) {
                        sl.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.02, 0.02, 0.02, 0.005);
                    }
                }
            }

            // One-time faint dust pulse at the start
            if (b.age == 0) {
                DustColorTransitionOptions dust = new DustColorTransitionOptions(
                        new Vector3f(A[0], A[1], A[2]),
                        new Vector3f(Math.min(1f, A[0] * 1.15f), Math.min(1f, A[1] * 1.15f), Math.min(1f, A[2] * 1.15f)),
                        0.8f
                );
                sendRadialDust(sl, b.origin, dust, 24, 0.8);
            }
        }

        private static void sendRadialDust(ServerLevel sl, Vec3 origin, ParticleOptions opts, int rays, double speed) {
            for (int i = 0; i < rays; i++) {
                double theta = (2 * Math.PI) * (i / (double) rays);
                double dx = Math.cos(theta) * speed;
                double dz = Math.sin(theta) * speed;
                sl.sendParticles(opts, origin.x, origin.y + 0.2, origin.z, 1, dx, 0.02, dz, 0.0);
            }
        }

        private static float[] rgb01(int hex) {
            return new float[]{
                    ((hex >> 16) & 0xFF) / 255f,
                    ((hex >> 8) & 0xFF) / 255f,
                    (hex & 0xFF) / 255f
            };
        }
    }

    // ===== Persistent dome ticker (SERVER) =====
    @Mod.EventBusSubscriber(modid = "egm", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class DomeTicker {
        private static final class Dome {
            final double radius; final Vec3 origin;
            final int colorAHex, colorBHex;
            int age; final int duration;
            Dome(Vec3 origin, double radius, int duration, int colorAHex, int colorBHex) {
                this.origin = origin; this.radius = radius; this.duration = duration;
                this.colorAHex = colorAHex; this.colorBHex = colorBHex; this.age = 0;
            }
        }
        private static final Map<ServerLevel, List<Dome>> SERVER_DOMES = new IdentityHashMap<>();

        public static void spawn(ServerLevel level, Vec3 origin, double radius, int duration,
                                 int colorAHex, int colorBHex) {
            SERVER_DOMES.computeIfAbsent(level, k -> new ArrayList<>()).add(
                    new Dome(origin, radius, duration, colorAHex, colorBHex));
        }

        @SubscribeEvent
        public static void onLevelTick(TickEvent.LevelTickEvent e) {
            if (e.phase != TickEvent.Phase.END || e.level.isClientSide || !(e.level instanceof ServerLevel sl)) return;
            List<Dome> list = SERVER_DOMES.get(sl);
            if (list == null || list.isEmpty()) return;

            list.removeIf(d -> {
                spawnDomeShell(sl, d);
                d.age++;
                return d.age >= d.duration;
            });
        }

        private static void spawnDomeShell(ServerLevel sl, Dome d) {
            double life = d.age / (double) d.duration;
            int ringCount = 12, basePoints = 22;
            if (d.age > d.duration / 2 && (d.age & 1) == 1) return;

            final double r = d.radius;
            final double cx = d.origin.x, cy = d.origin.y, cz = d.origin.z;

            float[] A = rgb01(d.colorAHex), B = rgb01(d.colorBHex);

            for (int ring = 0; ring <= ringCount; ring++) {
                double phi = (Math.PI * 0.5) * (ring / (double) ringCount);
                double ringR = r * Math.sin(phi);
                double y = cy + r * Math.cos(phi);

                int points = Math.max(10, (int) Math.round(basePoints * Math.sin(phi) * (1.0 - 0.4 * life)));
                for (int i = 0; i < points; i++) {
                    double theta = (2 * Math.PI) * (i / (double) points);
                    double x = cx + ringR * Math.cos(theta);
                    double z = cz + ringR * Math.sin(theta);
                    float[] rgb = ((i + ring + d.age) & 1) == 0 ? A : B;

                    // S2C: preserve tint (avoid count==0 black); match signature exactly
                    NetworkHandler.sendTintedParticle(sl, ModParticles.UNIVERSAL_PARTICLE_ONE.get(),
                            x, y, z, rgb[0], rgb[1], rgb[2]);

                    // Subtle vanilla motes on outer shell (very sparse)
                    if ((i & 31) == 0 && ring > 2) {
                        sl.sendParticles(ParticleTypes.ENCHANT, x, y + 0.05, z, 1, 0.01, 0.01, 0.01, 0.0);
                    }
                }
            }
        }

        private static float[] rgb01(int hex) {
            return new float[]{
                    ((hex >> 16) & 0xFF) / 255f,
                    ((hex >> 8) & 0xFF) / 255f,
                    (hex & 0xFF) / 255f
            };
        }
    }
}
