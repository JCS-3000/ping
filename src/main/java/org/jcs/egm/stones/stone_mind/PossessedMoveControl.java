package org.jcs.egm.stones.stone_mind;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.config.ModCommonConfig;

import java.util.UUID;
import java.util.function.Function;

public class PossessedMoveControl extends MoveControl {
    private final Mob mob;
    private final UUID controllerUuid;
    private final Function<UUID, EntityPossessionController.InputPacket> inputSupplier;

    public PossessedMoveControl(Mob mob, UUID controllerUuid, Function<UUID, EntityPossessionController.InputPacket> inputSupplier) {
        super(mob);
        this.mob = mob;
        this.controllerUuid = controllerUuid;
        this.inputSupplier = inputSupplier;
    }

    @Override
    public void tick() {
        EntityPossessionController.InputPacket input = inputSupplier.apply(mob.getUUID());
        if (input == null) {
            mob.setDeltaMovement(Vec3.ZERO);
            return;
        }

        // --- (Optional) Debug position and motion ---
        // System.out.println("[MOVE] Pre: pos=" + mob.position() + " dM=" + mob.getDeltaMovement());

        ResourceLocation key = mob.getType().builtInRegistryHolder().key().location();
        boolean canFly = key != null && ModCommonConfig.MIND_STONE_FLIGHT_ENTITIES.get().contains(key.toString());
        boolean canWallClimb = key != null && ModCommonConfig.MIND_STONE_WALLCLIMB_ENTITIES.get().contains(key.toString());
        boolean canSwim = key != null && ModCommonConfig.MIND_STONE_SWIMSPEED_ENTITIES.get().contains(key.toString());
        boolean inWater = mob.isInWater();

        double speed = 0.25;
        if (canSwim && inWater) speed = 0.55;
        else if (canFly) speed = 0.3;

        float yaw = mob.getYRot();
        double rad = Math.toRadians(yaw);
        double forward = input.forward * speed;
        double strafe = input.strafe * speed;
        double dx = -Math.sin(rad) * forward + Math.cos(rad) * strafe;
        double dz = Math.cos(rad) * forward + Math.sin(rad) * strafe;
        double dy = mob.getDeltaMovement().y;

        boolean onGround = mob.onGround();

        if (canFly && !onGround) {
            if (input.jump) dy = 0.42;
            if (input.attack) dy = -0.42;
        }

        if (canWallClimb && input.jump && mob.horizontalCollision) {
            dy = 0.25;
        }

        if (input.jump && onGround && !canFly && !canSwim) {
            dy = 0.42;
        }

        mob.setDeltaMovement(dx, dy, dz);
        mob.hasImpulse = true;

        // --- ANTI-CRASH ATTACK LOGIC ---
        if (input.attack) {
            Entity tgt = EntityPossessionController.getNearestAttackableEntity(mob, 2.5, controllerUuid);
            if (tgt instanceof Player p && p.getUUID().equals(controllerUuid)) {
                System.out.println("[SERVER PossessedMoveControl] Attack: Attempted to punch self! Cancelling possession.");
                if (mob.level() instanceof ServerLevel slevel) {
                    Player controller = slevel.getPlayerByUUID(controllerUuid);
                    if (controller != null) {
                        controller.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal(
                                        "Your will clashes with itself! Possession ends."
                                )
                        );
                        MindStonePossessionHandler.cancelPossession(controller);
                        return; // <--- Prevents further processing or vanilla attack
                    }
                }
                return; // Always return after cancel, no matter what
            } else if (tgt instanceof LivingEntity lt) {
                System.out.println("[SERVER PossessedMoveControl] Attack: Mob " + mob.getName().getString() +
                        " is attacking entity: " + lt.getName().getString());
                mob.doHurtTarget(lt);
                mob.swing(mob.getUsedItemHand());
            }
        }

        // --- (Optional) Debug position and motion ---
        // System.out.println("[MOVE] Post: pos=" + mob.position() + " dM=" + mob.getDeltaMovement());
    }
}
