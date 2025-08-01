package org.jcs.egm.stones.stone_time;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneItem;

public class TimeStoneItem extends StoneItem {
    @Override
    public String getKey() { return "time"; }
    @Override
    public int getColor() { return 0x085828; }

    public TimeStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResultHolder<ItemStack> handleStoneUse(Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state) {
        IGStoneAbility ability = TimeStoneAbilityRegistry.getSelectedAbility(stack);
        if (ability == null) {
            return InteractionResultHolder.pass(stack);
        }

        if (ability.canHoldUse()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        } else if (!world.isClientSide) {
            ability.activate(world, player, stack);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void onUseTick(Level world, Player player, ItemStack stack, int count) {
        // No hold-use ability for the time stone at present
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, Player player, int timeLeft) {
        // No hold-use ability for the time stone at present
    }
}
