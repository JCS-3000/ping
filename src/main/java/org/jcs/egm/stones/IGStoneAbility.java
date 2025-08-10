package org.jcs.egm.stones;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Universal contract for all Infinity Stone abilities.
 */
public interface IGStoneAbility {
    // Activate the ability on right-click or main trigger
    void activate(net.minecraft.world.level.Level level, Player player, ItemStack stack);

    // Optional: called every tick while holding right-click (for hold abilities)
    default void onUsingTick(net.minecraft.world.level.Level level, Player player, ItemStack stack, int count) {}

    // Optional: called when right-click/hold is released (for charge/hold abilities)
    default void releaseUsing(net.minecraft.world.level.Level level, Player player, ItemStack stack, int timeLeft) {}

    // Should the ability be triggered via holding use? (otherwise, click triggers)
    default boolean canHoldUse() { return true; }

    default String abilityKey() {
        return this.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

}
