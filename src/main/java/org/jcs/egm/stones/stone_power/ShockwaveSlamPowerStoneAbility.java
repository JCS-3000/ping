package org.jcs.egm.stones.stone_power;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneUseDamage;

/**
 * Power Stone Ability: Shockwave Slam
 * Instant AoE knockback + damage centered on the player.
 * Works raw/holder/gauntlet via your existing IGStoneAbility pipeline.
 */
public class ShockwaveSlamPowerStoneAbility implements IGStoneAbility {

    // Tweakables
    private static final double RADIUS = 8.0D;      // effect radius
    private static final float DAMAGE = 12.0F;      // damage dealt to mobs
    private static final double LIFT   = 0.5D;      // vertical lift
    private static final double PUSH   = 2.0D;      // horizontal push multiplier

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) return;

        // Raw-stone tax (matches your pattern)
        player.hurt(StoneUseDamage.get(level, player), 2.0F);

        Vec3 origin = player.position();

        // Sound + central particle burst
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(
                    ModParticles.POWER_STONE_EFFECT_TWO.get(),
                    origin.x, origin.y, origin.z,
                    120, 1.6, 0.5, 1.6, 0.22
            );
        }

        // Affect entities in a short vertical slice so it feels ground-based
        AABB box = new AABB(
                origin.x - RADIUS, origin.y - 1.0D, origin.z - RADIUS,
                origin.x + RADIUS, origin.y + 2.0D, origin.z + RADIUS
        );

        for (Entity e : level.getEntities(player, box)) {
            if (!(e instanceof LivingEntity target) || target == player) continue;

            // Knockback away from player + slight lift
            Vec3 dir = target.position().subtract(origin);
            if (dir.lengthSqr() < 1.0E-6) dir = new Vec3(0, 0, 1); // avoid NaN if overlapping
            dir = dir.normalize().scale(PUSH);
            target.push(dir.x, LIFT, dir.z);

            // Damage (uses player damage source for attribution/loot/aggro)
            target.hurt(level.damageSources().playerAttack(player), DAMAGE);

            // Rim particles along the target (optional small flair)
            if (level instanceof ServerLevel server) {
                Vec3 tp = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
                server.sendParticles(
                        ModParticles.POWER_STONE_EFFECT_ONE.get(),
                        tp.x, tp.y, tp.z,
                        6, 0.15, 0.15, 0.15, 0.02
                );
            }
        }
    }

    @Override
    public boolean canHoldUse() {
        // Instant cast, not a channel
        return false;
    }
}
