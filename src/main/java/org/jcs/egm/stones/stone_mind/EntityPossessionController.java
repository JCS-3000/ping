package org.jcs.egm.stones.stone_mind;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.stone_mind.MindStonePossessionHandler.PossessionInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EntityPossessionController {

    // Store MoveControls so we can restore them on unpossession
    private static final Map<UUID, MoveControl> previousMoveControls = new ConcurrentHashMap<>();
    // Store latest input for each possessed mob
    private static final Map<UUID, InputPacket> latestInputs = new ConcurrentHashMap<>();

    public static void receiveControlPacket(org.jcs.egm.network.PossessionControlPacket packet, ServerPlayer player) {
        PossessionInfo info = MindStonePossessionHandler.getPossessionInfo(player);
        if (info == null) {
            System.out.println("[SERVER PossessionController] No possession info for player " + player.getName().getString());
            return;
        }

        LivingEntity mob = getEntity(player.level(), info.getEntityId());
        if (!(mob instanceof Mob mobEntity)) {
            System.out.println("[SERVER PossessionController] No valid mob entity for possession");
            return;
        }

        // DEBUG: Print incoming input
        System.out.println("[SERVER PossessionController] Received input: strafe=" + packet.strafe +
                ", forward=" + packet.forward +
                ", jump=" + packet.jump +
                ", attack=" + packet.attack +
                ", yaw=" + packet.yaw +
                ", pitch=" + packet.pitch +
                " for mob: " + mob.getName().getString() +
                " from controller: " + player.getName().getString());

        // Save latest input for use during mob tick
        latestInputs.put(mob.getUUID(), new InputPacket(packet, player.getUUID()));

        // Always update mob rotation to match player head direction
        mob.setYRot(packet.yaw);
        mob.setXRot(packet.pitch);
        mob.yHeadRot = packet.yaw;
    }

    // Attach custom MoveControl to mob
    public static void handle(Player player, UUID mobId) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        LivingEntity entity = getEntity(serverLevel, mobId);
        if (!(entity instanceof Mob mob)) return;

        if (!(mob.getMoveControl() instanceof PossessedMoveControl)) {
            System.out.println("[SERVER PossessionController] Injecting PossessedMoveControl into mob: " + mob.getName().getString());
            previousMoveControls.put(mob.getUUID(), mob.getMoveControl());
            MobMoveControlUtil.setMoveControl(mob, new PossessedMoveControl(
                    mob, player.getUUID(), uuid -> latestInputs.get(uuid)
            ));
        }

        spawnMindParticles(serverLevel, player);
        spawnMindParticles(serverLevel, mob);
    }

    // Restore previous MoveControl and AI on possession end
    public static void cleanup(LivingEntity mob) {
        if (mob instanceof Mob m) {
            if (previousMoveControls.containsKey(m.getUUID())) {
                System.out.println("[SERVER PossessionController] Restoring original MoveControl for mob: " + m.getName().getString());
                MobMoveControlUtil.setMoveControl(m, previousMoveControls.remove(m.getUUID()));
            }
            m.setNoAi(false);
            latestInputs.remove(m.getUUID());
        }
    }

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

    public static Entity getNearestAttackableEntity(LivingEntity mob, double range, UUID controllerUuid) {
        return mob.level().getEntities(
                mob,
                mob.getBoundingBox().inflate(range),
                e -> e instanceof LivingEntity le
                        && le != mob
                        && le.isAlive()
                        && (le.getUUID() == null || !le.getUUID().equals(controllerUuid))
        ).stream().findFirst().orElseGet(() -> {
            // If the controller is within range, return them (for possession cancel)
            return mob.level().getEntities(
                    mob,
                    mob.getBoundingBox().inflate(range),
                    e -> e instanceof Player p && p.getUUID().equals(controllerUuid)
            ).stream().findFirst().orElse(null);
        });
    }

    // Stores latest player input for a mob
    public static class InputPacket {
        public final float strafe, forward;
        public final boolean jump, attack;
        public final UUID controller;

        public InputPacket(org.jcs.egm.network.PossessionControlPacket p, UUID controller) {
            this.strafe = p.strafe;
            this.forward = p.forward;
            this.jump = p.jump;
            this.attack = p.attack;
            this.controller = controller;
        }
    }
}
