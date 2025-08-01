package org.jcs.egm.stones.stone_mind;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;

public class PossessionMindStoneAbility implements IGStoneAbility {
    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        // Implement your mob possession logic here!
        // Example: open possession targeting, swap player control to mob, etc.
    }

    @Override
    public boolean canHoldUse() { return false; }
}
