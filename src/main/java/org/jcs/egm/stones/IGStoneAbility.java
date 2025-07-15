package org.jcs.egm.stones;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IGStoneAbility {
    /**
     * Called once when the ability is activated (on right-click).
     */
    void activate(Level level, Player player, ItemStack gauntletStack);

    /**
     * Whether this ability should continue activating while right-click is held.
     * Default is false (one-off use).
     */
    default boolean canHoldUse() {
        return false;
    }

    /**
     * Called each tick while right-click is held, if canHoldUse() returns true.
     * @param count number of ticks the item has been used for
     */
    default void onUsingTick(Level level, Player player, ItemStack gauntletStack, int count) {
        // no-op by default
    }
}
