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

        // Always set mob rotation to match player's head direction
        mob.setYRot(packet.yaw);
        mob.setXRot(packet.pitch);
        mob.yHeadRot = packet.yaw;

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

        // --- MOVEMENT FIX: forcibly move mob in input direction ---
        // This is a very direct approach, should work for chickens and other mobs with AI off.
        Vec3 look = mob.getLookAngle();
        double forward = moveVec.z * speed;
        double strafe  = moveVec.x * speed;
        double dx = -Math.sin(Math.toRadians(mob.getYRot())) * forward + Math.cos(Math.toRadians(mob.getYRot())) * strafe;
        double dz = Math.cos(Math.toRadians(mob.getYRot())) * forward + Math.sin(Math.toRadians(mob.getYRot())) * strafe;
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

        // Call move() to force position update (fix for AI-off mobs like chicken)
        mob.move(net.minecraft.world.entity.MoverType.SELF, mob.getDeltaMovement());

        // Regular jump (if on ground, not flying/swimming)
        if (packet.jump && mob.onGround() && !canFly && !canSwim) {
            mob.setDeltaMovement(mob.getDeltaMovement().x, 0.42, mob.getDeltaMovement().z);
            mob.hasImpulse = true;
            mob.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, 0.42, 0));
        }

        // --- ATTACK FIX: Exclude controller from possible targets ---
        if (packet.attack) {
            Entity tgt = getNearestAttackableEntity(mob, 2.5, player.getUUID());
            if (tgt instanceof LivingEntity lt) {
                mob.doHurtTarget(lt);
                mob.swing(mob.getUsedItemHand());
            }
        }
    }

    // Now excludes the player who is possessing the mob
    private static Entity getNearestAttackableEntity(LivingEntity mob, double range, UUID controllerUuid) {
        return mob.level().getEntities(
                mob,
                mob.getBoundingBox().inflate(range),
                e -> e instanceof LivingEntity le && le != mob && le.isAlive() && (le.getUUID() == null || !le.getUUID().equals(controllerUuid))
        ).stream().findFirst().orElse(null);
    }

    // Overload for legacy usage (should not be used, but left for compatibility)
    @Deprecated
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

    // Cancel on shift (sneak) only
    public static boolean shouldCancel(Player player) {
        return player.isCrouching();
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
        }
    }
}
