package org.jcs.egm.stones.stone_reality;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityRegistries;
import org.jcs.egm.stones.StoneAbilityCooldowns;
import org.jcs.egm.stones.StoneItem;

public class RealityStoneItem extends StoneItem {

    public RealityStoneItem(Properties properties) { super(properties); }

    @Override public String getKey() { return "reality"; }
    @Override public int getColor() { return 0xFF0033; }

    @Override
    protected InteractionResultHolder<ItemStack> handleStoneUse(Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state) {
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(getKey(), stack);
        if (ability == null) return InteractionResultHolder.pass(stack);
        if (StoneAbilityCooldowns.guardUse(player, stack, getKey(), ability)) return InteractionResultHolder.pass(stack);
        if (ability.canHoldUse()) { player.startUsingItem(hand); return InteractionResultHolder.consume(stack); }
        if (!world.isClientSide) { ability.activate(world, player, stack); StoneAbilityCooldowns.apply(player, stack, getKey(), ability); return InteractionResultHolder.success(stack); }
        return InteractionResultHolder.pass(stack);
    }
}
