package org.jcs.egm.stones.stone_time;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.client.particle.UniversalTintParticle;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;

import java.util.*;

public class PacifyTimeStoneAbility implements IGStoneAbility {

    @Override
    public String abilityKey() { return "pacify"; }

    private static final int CHARGE_TICKS = 80; // 4s aesthetic charge
    private static final boolean AUTO_FIRE_AT_FULL = true;

    private static final SoundEvent CHARGING_SOUND =
            SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "time_stone_charging"));
    private static final SoundEvent TWINKLE_SOUND =
            SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "universal_twinkle"));

    private static final Set<UUID> CHARGING_SOUND_PLAYERS = new HashSet<>();
    private static final Map<UUID, Integer> CHARGE = new HashMap<>();

    private static final double AOE_RADIUS = 8.0;

    // Colors
    private static final int COLOR_A = 0x62FF2D; // brighter green
    private static final int COLOR_B = 0x0AAA67; // darker green

    @Override
    public boolean canHoldUse() { return true; }

    @Override
    public void activate(Level level, Player player, ItemStack stack) {}

    @Override
    public void onUsingTick(Level level, Player player, ItemStack stack, int count) {
        final UUID id = player.getUUID();
        final int useDuration = player.getUseItem().getUseDuration();
        final int ticksHeld = useDuration - count;

        if (ticksHeld < CHARGE_TICKS) {
            if (!level.isClientSide) {
                CHARGE.put(id, ticksHeld);
            } else {
                if (!CHARGING_SOUND_PLAYERS.contains(id)) {
                    level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            CHARGING_SOUND, SoundSource.PLAYERS, 0.9f, 1.0f, true);
                    CHARGING_SOUND_PLAYERS.add(id);
                }
                if ((ticksHeld & 1) == 0) spawnRightArmRing(level, player, ticksHeld);
            }
            return;
        }

        if (level.isClientSide && (ticksHeld & 1) == 0) {
            spawnRightArmRing(level, player, ticksHeld);
        }

        if (AUTO_FIRE_AT_FULL && !level.isClientSide) {
            Integer prev = CHARGE.get(id);
            if (prev == null || prev < CHARGE_TICKS) {
                CHARGE.put(id, CHARGE_TICKS);
                doPacify(level, player, stack);
                if (player instanceof ServerPlayer sp) sp.stopUsingItem();
            }
        } else if (AUTO_FIRE_AT_FULL && level.isClientSide) {
            stopChargingSoundClient(id);
            // Mirror burst locally so it uses addParticle() path
            spawnOutwardSpokes(level, player);
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
            doPacify(level, player, stack);
        } else {
            spawnOutwardSpokes(level, player);
        }
    }

    private void doPacify(Level level, Player player, ItemStack stoneStack) {
        if (StoneAbilityCooldowns.guardUse(player, stoneStack, "time", this)) return;

        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(AOE_RADIUS));
        boolean pacifiedAny = false;

        for (Mob mob : mobs) {
            if (mob instanceof AgeableMob ageable && !ageable.isBaby()) {
                ageable.setBaby(true);
                pacifiedAny = true;

                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ModParticles.TIME_STONE_EFFECT_ONE.get(),
                            mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                            12, 0.3, 0.3, 0.3, 0.01);
                }
            }
        }

        if (pacifiedAny) {
            level.playSound(null, player.blockPosition(), TWINKLE_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
            // Server-side spokes so other players see them
            spawnOutwardSpokes(level, player);
            StoneAbilityCooldowns.apply(player, stoneStack, "time", this);
        } else if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal("No mobs nearby to pacify."), true);
        }
    }

    // ===== Visuals =====

    // Right-arm ring using UNIVERSAL_PARTICLE_TWO, shrunk to 0.25x just for these spawns
    private void spawnRightArmRing(Level level, Player player, int ticksHeld) {
        final double armY = player.getEyeY() - 0.35;

        // Arm points forward; ring wraps around forward axis using right & up
        final Vec3 forward = Vec3.directionFromRotation(0, player.getYRot()).normalize();
        final Vec3 up = new Vec3(0, 1, 0);
        final Vec3 right = forward.cross(up).normalize(); // TRUE right

        // Placement (original)
        final Vec3 center = new Vec3(player.getX(), armY, player.getZ())
                .add(right.scale(.22))
                .add(forward.scale(.33));

        final int points = 12;           // one-particle-thick ring
        final double radius = 0.20;      // ring diameter
        final double rotateSpeed = 0.30; // rad/tick (clockwise)
        final double angleOffset = ticksHeld * rotateSpeed;

        // Shrink only these (client-side construction)
        if (level.isClientSide) UniversalTintParticle.setScaleMultiplier(0.25f);

        for (int i = 0; i < points; i++) {
            double a = angleOffset + (2 * Math.PI * i / points);
            Vec3 p = center
                    .add(right.scale(Math.cos(a) * radius))
                    .add(up.scale(Math.sin(a) * radius));

            int hex = (i & 1) == 0 ? COLOR_A : COLOR_B;
            float[] rgb = rgb01(hex);

            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ModParticles.UNIVERSAL_PARTICLE_TWO.get(),
                        p.x, p.y, p.z,
                        0, rgb[0], rgb[1], rgb[2], 0.0);
            } else {
                level.addParticle(ModParticles.UNIVERSAL_PARTICLE_TWO.get(),
                        p.x, p.y, p.z,
                        rgb[0], rgb[1], rgb[2]);
            }
        }

        if (level.isClientSide) UniversalTintParticle.setScaleMultiplier(1.0f);
    }

    // 12 ground spokes using UNIVERSAL_PARTICLE_ONE (normal size), alternating colors
    private void spawnOutwardSpokes(Level level, Player player) {
        final int spokes = 12;
        final int stepsPerSpoke = 14;         // particles per spoke
        final double y = player.getY() + 0.22; // lifted to avoid ground darkening
        final double start = 0.6;             // small gap at center
        final double step = (AOE_RADIUS - start) / stepsPerSpoke;

        for (int i = 0; i < spokes; i++) {
            double angle = 2 * Math.PI * i / spokes;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);

            int hex = (i & 1) == 0 ? COLOR_A : COLOR_B;
            float[] rgb = rgb01(hex);

            for (int s = 0; s <= stepsPerSpoke; s++) {
                double r = start + s * step;
                double px = player.getX() + dx * r;
                double pz = player.getZ() + dz * r;

                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ModParticles.UNIVERSAL_PARTICLE_ONE.get(),
                            px, y, pz,
                            0, rgb[0], rgb[1], rgb[2], 0.0);
                } else {
                    level.addParticle(ModParticles.UNIVERSAL_PARTICLE_ONE.get(),
                            px, y, pz,
                            rgb[0], rgb[1], rgb[2]);
                }
            }
        }
    }

    // Color helper: 0xRRGGBB -> [r,g,b] ∈ [0..1]
    private static float[] rgb01(int hex) {
        return new float[] {
                ((hex >> 16) & 0xFF) / 255f,
                ((hex >>  8) & 0xFF) / 255f,
                ( hex        & 0xFF) / 255f
        };
    }

    private void stopChargingSoundClient(UUID id) {
        if (!CHARGING_SOUND_PLAYERS.contains(id)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.getSoundManager().stop(CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
        }
        CHARGING_SOUND_PLAYERS.remove(id);
    }
}
