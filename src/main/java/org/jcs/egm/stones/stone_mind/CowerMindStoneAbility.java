package org.jcs.egm.stones.stone_mind;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;

import java.util.List;

public class CowerMindStoneAbility implements IGStoneAbility {
    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(10));
        if (mobs.isEmpty()) {
            serverPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("No mobs nearby to send away."), true);
            return;
        }
        for (Mob mob : mobs) {
            if (mob.distanceTo(player) > 12) continue;
            PathNavigation nav = mob.getNavigation();
            double speed = mob.getAttribute(Attributes.MOVEMENT_SPEED) != null ? mob.getAttribute(Attributes.MOVEMENT_SPEED).getValue() : 0.3;
            // Send the mob 15 blocks directly away from the player
            double dx = mob.getX() - player.getX();
            double dz = mob.getZ() - player.getZ();
            double len = Math.sqrt(dx*dx + dz*dz);
            if (len < 0.01) continue;
            double mult = 15.0 / len;
            double destX = mob.getX() + dx * mult;
            double destZ = mob.getZ() + dz * mult;
            nav.moveTo(destX, mob.getY(), destZ, speed + 0.2);
        }
        serverPlayer.displayClientMessage(
                net.minecraft.network.chat.Component.literal("Nearby mobs commanded to go away."), true);
    }
    @Override
    public boolean canHoldUse() { return false; }
}
