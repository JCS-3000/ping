package org.jcs.egm.stones.stone_mind;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.stones.IGStoneAbility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StunMindStoneAbility implements IGStoneAbility {
    // Track stunned entity per player for proper stun release
    private static final Map<UUID, Integer> lastStunEntity = new HashMap<>();

    private static final double RANGE = 10.0;

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        // Not used for hold abilities
    }

    @Override
    public boolean canHoldUse() {
        return true;
    }

    @Override
    public void onUsingTick(Level level, Player player, ItemStack stack, int count) {
        if (level.isClientSide) return;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 beamEnd = eye.add(look.scale(RANGE));

        LivingEntity closest = null;
        double minDist = RANGE + 1;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(eye, beamEnd).inflate(1.0))) {
            if (entity == player) continue;
            Vec3 entityPos = entity.getEyePosition();
            double t = getRayParam(eye, beamEnd, entityPos);
            if (t >= 0 && t <= 1) {
                double dist = eye.distanceTo(entityPos);
                if (dist < minDist) {
                    minDist = dist;
                    closest = entity;
                }
            }
        }

        // Draw beam (client: will be handled in packet/visual logic, but spawn server-side for demo)
        int steps = 16;
        Vec3 delta = look.scale(RANGE / steps);
        Vec3 pos = eye;
        for (int i = 0; i < steps; i++) {
            ((ServerPlayer) player).serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    pos.x, pos.y, pos.z, 1, 0, 0, 0, 0.0);
            pos = pos.add(delta);
        }

        // Stun the entity in the beam
        if (closest != null) {
            // Refresh stun each tick (2 ticks duration)
            closest.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2, 9, true, false, false));
            lastStunEntity.put(player.getUUID(), closest.getId());
        } else {
            lastStunEntity.remove(player.getUUID());
        }
    }

    @Override
    public void releaseUsing(Level level, Player player, ItemStack stack, int timeLeft) {
        lastStunEntity.remove(player.getUUID());
    }

    // Helper: Get closest approach t of point to ray [a,b]
    private double getRayParam(Vec3 a, Vec3 b, Vec3 point) {
        Vec3 ab = b.subtract(a);
        double ab2 = ab.lengthSqr();
        if (ab2 == 0) return 0;
        double t = (point.subtract(a)).dot(ab) / ab2;
        return t;
    }
}
