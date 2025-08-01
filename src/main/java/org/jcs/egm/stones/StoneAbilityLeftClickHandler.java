package org.jcs.egm.stones;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.gauntlet.InfinityGauntletItem;
import org.jcs.egm.holders.StoneHolderItem;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class StoneAbilityLeftClickHandler {

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        openAbilityMenuIfStone(event.getEntity(), event.getEntity().getMainHandItem(), InteractionHand.MAIN_HAND);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        openAbilityMenuIfStone(event.getEntity(), event.getEntity().getMainHandItem(), event.getHand());
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        openAbilityMenuIfStone(event.getEntity(), event.getEntity().getMainHandItem(), InteractionHand.MAIN_HAND);
    }

    /**
     * Opens the ability selection menu client-side for Stones, Stone Holders, or Gauntlet.
     */
    private static void openAbilityMenuIfStone(Player player, ItemStack stack, InteractionHand hand) {
        if (!player.level().isClientSide) return;
        if (stack.isEmpty()) return;

        // Raw Stone
        if (stack.getItem() instanceof StoneItem stoneItem) {
            StoneItem.openStoneAbilityMenu(stack, hand, stoneItem.getKey());
            return;
        }
        // Stone Holder
        if (stack.getItem() instanceof StoneHolderItem holder) {
            StoneItem.openStoneAbilityMenu(stack, hand, holder.getStoneKey());
            return;
        }
        // Infinity Gauntlet
        if (stack.getItem() instanceof InfinityGauntletItem) {
            String stoneKey = InfinityGauntletItem.getSelectedStoneName(stack);
            StoneItem.openStoneAbilityMenu(stack, hand, stoneKey);
        }
    }
}
