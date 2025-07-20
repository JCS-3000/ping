package org.jcs.egm.stones;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IGStoneAbility {
    // Universal stone ability methods:
    void activate(net.minecraft.world.level.Level level, Player player, ItemStack stack);

    default void onUsingTick(net.minecraft.world.level.Level level, Player player, ItemStack stack, int count) {}

    default void releaseUsing(net.minecraft.world.level.Level level, Player player, ItemStack stack, int timeLeft) {}

    default boolean canHoldUse() { return true; }

    /**
     ** Returns true if the stone is being used inside the Infinity Gauntlet, else false.
     **/
    static boolean isInGauntlet(Player player, ItemStack stack) {
        // Checks the player's main/offhand or the stack for being the gauntlet.
        if (stack != null && stack.getItem().getClass().getSimpleName().equals("InfinityGauntletItem"))
            return true;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return (main != null && main.getItem().getClass().getSimpleName().equals("InfinityGauntletItem"))
                || (off != null && off.getItem().getClass().getSimpleName().equals("InfinityGauntletItem"));
    }
}
