package org.jcs.egm.stones.stone_space;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import org.jcs.egm.stones.StoneAbilityCooldowns;

public class EndericBeamSpaceStoneAbility implements IGStoneAbility {

    @Override
    public String abilityKey() { return "enderic_beam"; }

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) return;

        double range = 50.0;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        HitResult hit = player.pick(range, 1.0F, false);
        Vec3 dest = switch (hit.getType()) {
            case BLOCK, ENTITY -> hit.getLocation();
            default -> eye.add(look.scale(range));
        };

        // Draw a particle line from eye -> dest (server-side so everyone sees it)
        if (level instanceof ServerLevel server) {
            int steps = 40;
            Vec3 step = dest.subtract(eye).scale(1.0 / steps);
            Vec3 p = eye;
            for (int i = 0; i < steps; i++) {
                server.sendParticles(ParticleTypes.PORTAL, p.x, p.y, p.z, 2, 0.02, 0.02, 0.02, 0.0);
                p = p.add(step);
            }
        }

        // Teleport (safe-ish): refuse inside-solid destination
        if (player instanceof ServerPlayer sp) {
            BlockPos pos = BlockPos.containing(dest.x, dest.y, dest.z);
            BlockState state = level.getBlockState(pos);
            if (!state.blocksMotion()) {
                sp.teleportTo(dest.x, dest.y, dest.z);
            } else {
                sp.displayClientMessage(Component.literal("Cannot teleport inside a block!"), true);
            }
        }

        // IMPORTANT: apply container-aware cooldown + player-persistent gate using the STONE stack
        StoneAbilityCooldowns.apply(player, stack, "space", this);
    }

    @Override
    public boolean canHoldUse() { return false; }
}
