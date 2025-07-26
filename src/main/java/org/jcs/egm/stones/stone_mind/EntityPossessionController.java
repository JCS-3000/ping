package org.jcs.egm.stones.stone_mind;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.config.ModCommonConfig;
import org.jcs.egm.stones.stone_mind.MindStonePossessionHandler.PossessionInfo;

import java.util.UUID;

public class EntityPossessionController {

    public static void receiveControlPacket(org.jcs.egm.network.PossessionControlPacket packet, ServerPlayer player) {
        PossessionInfo info = MindStonePossessionHandler.getPossessionInfo(player);
        if (info == null) return;

        LivingEntity mob = getEntity(player.level(), info.getEntityId());
        if (mob == null) return;

        ResourceLocation key = mob.getType().builtInRegistryHolder().key().location();
        if (key == null) return;

        boolean canFly      = ModCommonConfig.MIND_STONE_FLIGHT_ENTITIES.get().contains(key.toString());
        boolean canWallClimb= ModCommonConfig.MIND_STONE_WALLCLIMB_ENTITIES.get().contains(key.toString());
        boolean canSwim     = ModCommonConfig.MIND_STONE_SWIMSPEED_ENTITIES.get().contains(key.toString());

        Vec3 moveVec = new Vec3(packet.strafe, 0, packet.forward);
        boolean inWater = mob.isInWater();
        double speed = 0.25;

        if (canSwim && inWater) {
            speed = 0.55;
        } else if (canFly) {
            speed = 0.3;
        }

        if (moveVec.lengthSqr() > 0) {
            moveVec = moveVec.normalize().scale(speed);
            Vec3 look = mob.getLookAngle();
            double dx = look.x * moveVec.z + look.z * moveVec.x;
            double dz = look.z * moveVec.z - look.x * moveVec.x;
            double dy = mob.getDeltaMovement().y;

            if (canFly && !mob.onGround()) {
                if (packet.jump)   dy = 0.42;
                if (packet.attack) dy = -0.42;
            }

            if (canWallClimb && packet.jump && mob.horizontalCollision) {
                dy = 0.25;
            }

            mob.setDeltaMovement(dx, dy, dz);
            mob.hasImpulse = true;
        }

        if (packet.jump && mob.onGround() && !canFly && !canSwim) {
            mob.setDeltaMovement(mob.getDeltaMovement().x, 0.42, mob.getDeltaMovement().z);
            mob.hasImpulse = true;
        }

        if (packet.attack) {
            Entity tgt = getNearestAttackableEntity(mob, 2.5);
            if (tgt instanceof LivingEntity lt) {
                mob.doHurtTarget(lt);
                mob.swing(mob.getUsedItemHand());
            }
        }
    }

    private static Entity getNearestAttackableEntity(LivingEntity mob, double range) {
        return mob.level().getEntities(
                mob,
                mob.getBoundingBox().inflate(range),
                e -> e instanceof LivingEntity le && le != mob && le.isAlive()
        ).stream().findFirst().orElse(null);
    }

    public static void handle(Player player, UUID mobId) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        LivingEntity mob = getEntity(serverLevel, mobId);
        if (mob == null) return;

        if (mob instanceof Mob m && !m.isNoAi()) {
            m.setNoAi(true);
        }

        spawnMindParticles(serverLevel, player);
        spawnMindParticles(serverLevel, mob);
    }

    public static void cleanup(LivingEntity mob) {
        if (mob instanceof Mob m && m.isNoAi()) {
            m.setNoAi(false);
        }
    }

    public static boolean shouldCancel(Player player) {
        return player.isCrouching() && player.isUsingItem();
    }

    public static LivingEntity getEntity(Level level, UUID id) {
        if (!(level instanceof ServerLevel s)) return null;
        for (Entity e : s.getEntities().getAll()) {
            if (e.getUUID().equals(id) && e instanceof LivingEntity le) {
                return le;
            }
        }
        return null;
    }

    public static void spawnMindParticles(Level level, Entity entity) {
        if (entity == null || !level.isClientSide) return;
        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() / 2.0;
        double z = entity.getZ();
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.1, 0);
            // custom particle option:
            // level.addParticle(YourParticles.REALITY_STONE_EFFECT_ONE.get(), x, y, z, 0, 0.1, 0);
        }
    }
}
