package org.jcs.egm.stones.stone_space;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.stones.IGStoneAbility;

public class EndericBeamSpaceStoneAbility implements IGStoneAbility {
    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) return;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = 50.0;
        Vec3 target = eye.add(look.scale(range));

        // Raytrace for teleport destination
        HitResult hit = player.pick(range, 1.0F, false);

        Vec3 dest;
        if (hit.getType() == HitResult.Type.BLOCK) {
            dest = hit.getLocation();
        } else if (hit.getType() == HitResult.Type.ENTITY) {
            dest = hit.getLocation();
        } else {
            dest = target;
        }

        // Create a line of bubble particles
        if (level instanceof ServerLevel serverLevel) {
            int steps = 30;
            Vec3 stepVec = dest.subtract(eye).scale(1.0 / steps);
            Vec3 p = eye;
            for (int i = 0; i < steps; i++) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                        p.x, p.y, p.z,
                        1, 0, 0, 0, 0.0);
                p = p.add(stepVec);
            }
        }

        // Teleport player
        if (player instanceof ServerPlayer serverPlayer) {
            BlockPos tp = BlockPos.containing(dest.x, dest.y, dest.z);
            BlockState state = level.getBlockState(tp);
            if (!state.blocksMotion()) {
                serverPlayer.teleportTo(dest.x, dest.y, dest.z);
            } else {
                serverPlayer.displayClientMessage(
                        Component.literal("Cannot teleport inside a block!"), true);
            }
        }
    }

    @Override
    public boolean canHoldUse() { return false; }
}
