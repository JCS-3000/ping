package org.jcs.egm.stones.stone_space;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneItem;

public class SpaceStoneItem extends StoneItem {
    @Override
    public String getKey() { return "space"; }
    @Override
    public int getColor() { return 0x283095; }

    public SpaceStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResultHolder<ItemStack> handleStoneUse(Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state) {
        IGStoneAbility ability = SpaceStoneAbilityRegistry.getSelectedAbility(stack);
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
}
