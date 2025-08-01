package org.jcs.egm.stones.stone_power;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneItem;

public class PowerStoneItem extends StoneItem {
    @Override
    public String getKey() { return "power"; }
    @Override
    public int getColor() { return 0x75179C; }

    public PowerStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResultHolder<ItemStack> handleStoneUse(Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state) {
        IGStoneAbility ability = PowerStoneAbilityRegistry.getSelectedAbility(stack);
        if (ability == null) {
            return InteractionResultHolder.pass(stack);
        }
        // No state gating: works raw, in holder, or in gauntlet!
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
        IGStoneAbility ability = PowerStoneAbilityRegistry.getSelectedAbility(stack);
        if (ability != null && ability.canHoldUse()) {
            ability.onUsingTick(world, player, stack, count);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, Player player, int timeLeft) {
        IGStoneAbility ability = PowerStoneAbilityRegistry.getSelectedAbility(stack);
        if (ability != null && ability.canHoldUse()) {
            ability.releaseUsing(world, player, stack, timeLeft);
        }
    }
}
