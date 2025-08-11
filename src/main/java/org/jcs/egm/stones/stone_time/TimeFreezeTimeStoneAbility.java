package org.jcs.egm.stones.stone_time;

import net.minecraft.client.Minecraft;
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
import org.jcs.egm.client.particle.UniversalTintParticle;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;

import java.util.*;

@Mod.EventBusSubscriber(modid = "egm", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TimeFreezeTimeStoneAbility implements IGStoneAbility {

    @Override public String abilityKey() { return "freeze"; }
    @Override public boolean canHoldUse() { return true; }

    // Tuning
    private static final int CHARGE_TICKS = 80;            // 4s (like Bubble)
    private static final boolean AUTO_FIRE_AT_FULL = true;
    private static final double AOE_RADIUS = 10.0;
    private static final int FREEZE_TICKS = 200;           // use your current duration
    private static final int SLOWNESS_LEVEL = 255;         // immobile
    private static final int DOME_DURATION_TICKS = 200;    // 10s

    // Colors
    private static final int COLOR_A_HEX = 0x62FF2D;
    private static final int COLOR_B_HEX = 0x0AAA67;

    // Sounds (ad-hoc ok client-local)
    private static final SoundEvent CHARGING_SOUND =
            SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "time_stone_charging"));
    private static final SoundEvent ACTIVATE_SOUND =
            SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "universal_twinkle"));

    // Charge state
    private static final Set<UUID> CHARGING_SOUND_PLAYERS = new HashSet<>();
    private static final Map<UUID, Integer> CHARGE = new HashMap<>();

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
            } else {
                // Client ring: identical pattern to Pacify/Bubble (never blacks out)
                if (!CHARGING_SOUND_PLAYERS.contains(id)) {
                    level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            CHARGING_SOUND, SoundSource.PLAYERS, 0.9f, 1.0f, true);
                    CHARGING_SOUND_PLAYERS.add(id);
                }
                if ((ticksHeld & 1) == 0) spawnRightArmRingClient(level, player, ticksHeld);
            }
            return;
        }

        // Auto-release at full (server authoritative)
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
                    ACTIVATE_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F, false);
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
                    ACTIVATE_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F, false);
        } else {
            fire(level, player, stack);
        }
    }

    // ===== Core effect (server) =====
    private void fire(Level level, Player player, ItemStack stoneStack) {
        if (StoneAbilityCooldowns.guardUse(player, stoneStack, "time", this)) return;

        // (Client already played local SFX)
        level.playSound(null, player.blockPosition(), ACTIVATE_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);

        // 10s dome — server spawns with count=0 (RGB carried in vx,vy,vz)
        Vec3 origin = player.position().add(0, 0.2, 0);
        if (level instanceof ServerLevel sl) DomeTicker.spawn(sl, origin, AOE_RADIUS, DOME_DURATION_TICKS,
                COLOR_A_HEX, COLOR_B_HEX);

        // Freeze
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

    // ===== Client ring (same pattern as your working abilities) =====
    private void spawnRightArmRingClient(Level level, Player player, int ticksHeld) {
        final double armY = player.getEyeY() - 0.35;

        final Vec3 forward = Vec3.directionFromRotation(0, player.getYRot()).normalize();
        final Vec3 up = new Vec3(0, 1, 0);
        final Vec3 right = forward.cross(up).normalize();

        final Vec3 center = new Vec3(player.getX(), armY, player.getZ())
                .add(right.scale(.22))
                .add(forward.scale(.33));

        final int points = 12;
        final double radius = 0.20;
        final double rotateSpeed = 0.30;
        final double angleOffset = ticksHeld * rotateSpeed;

        UniversalTintParticle.setScaleMultiplier(0.25f);
        for (int i = 0; i < points; i++) {
            double a = angleOffset + (2 * Math.PI * i / points);
            Vec3 p = center.add(right.scale(Math.cos(a) * radius)).add(up.scale(Math.sin(a) * radius));

            float[] rgb = rgb01(((i & 1) == 0) ? COLOR_A_HEX : COLOR_B_HEX);
            // CLIENT path: addParticle with r,g,b (works consistently)
            level.addParticle(ModParticles.UNIVERSAL_PARTICLE_TWO.get(),
                    p.x, p.y, p.z,
                    rgb[0], rgb[1], rgb[2]);
        }
        UniversalTintParticle.setScaleMultiplier(1.0f);
    }

    private static float[] rgb01(int hex) {
        return new float[]{
                ((hex >> 16) & 0xFF) / 255f,
                ((hex >> 8) & 0xFF) / 255f,
                (hex & 0xFF) / 255f
        };
    }

    private static void stopChargingSoundClient(UUID id) {
        if (!CHARGING_SOUND_PLAYERS.contains(id)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.getSoundManager().stop(CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
        CHARGING_SOUND_PLAYERS.remove(id);
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
            // dissipate density over time
            double life = d.age / (double) d.duration; // 0..1
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

                    // SERVER path: count=0 so rgb go in vx,vy,vz (no black)
                    sl.sendParticles(ModParticles.UNIVERSAL_PARTICLE_ONE.get(),
                            x, y, z,
                            0, rgb[0], rgb[1], rgb[2], 0.0);
                }
            }
        }
    }
}
