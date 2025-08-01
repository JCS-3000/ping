package org.jcs.egm.stones.stone_time;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;

import java.util.List;

public class PacifyTimeStoneAbility implements IGStoneAbility {
    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Find all mobs in reach
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(8.0));
        boolean found = false;

        for (Mob mob : mobs) {
            if (mob instanceof AgeableMob ageable) {
                if (!ageable.isBaby()) {
                    ageable.setBaby(true);
                    found = true;
                }
            } else {
                // Not ageable
                serverPlayer.displayClientMessage(
                        Component.literal("§oI'm sorry, little one..."), true); // §o = italic
                found = true;
            }
        }

        if (!found) {
            serverPlayer.displayClientMessage(
                    Component.literal("No mobs nearby to pacify."), true);
        }
    }

    @Override
    public boolean canHoldUse() { return false; }
}
