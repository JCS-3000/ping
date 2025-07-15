package org.jcs.egm.stones.stone_time;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;

public class TimeStoneAbility implements IGStoneAbility {

    @Override
    public void activate(Level level, Player player, ItemStack gauntletStack) {
        // Single click does nothing — effect is hold-only
    }

    @Override
    public boolean canHoldUse() {
        return true;
    }

    @Override
    public void onUsingTick(Level level, Player player, ItemStack gauntletStack, int count) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            long current = serverLevel.getDayTime();
            serverLevel.setDayTime(current + 1000);
        }
    }
}
