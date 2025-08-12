package org.jcs.egm.stones.stone_space;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityRegistries;
import org.jcs.egm.stones.StoneAbilityCooldowns;
import org.jcs.egm.stones.StoneItem;

public class SpaceStoneItem extends StoneItem {

    public SpaceStoneItem(Properties properties) {
        super(properties);
    }

    @Override public String getKey()  { return "space"; }
    @Override public int    getColor(){ return 0x2e3bc9; }

    @Override
    protected InteractionResultHolder<ItemStack> handleStoneUse(Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state) {
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(getKey(), stack);
        if (ability == null) return InteractionResultHolder.pass(stack);

        // Block if cooling (also re-syncs overlay to whatever container is used now)
        if (StoneAbilityCooldowns.guardUse(player, stack, getKey(), ability)) {
            return InteractionResultHolder.pass(stack);
        }

        if (ability.canHoldUse()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        if (!world.isClientSide) {
            ability.activate(world, player, stack);
            // Container-aware cooldown + player-persistent gate (overlay goes to holder/gauntlet or raw stone)
            StoneAbilityCooldowns.apply(player, stack, getKey(), ability);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }
}
