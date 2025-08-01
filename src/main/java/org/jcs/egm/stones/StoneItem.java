package org.jcs.egm.stones;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.gauntlet.InfinityGauntletItem;
import org.jcs.egm.holders.StoneHolderItem;

public abstract class StoneItem extends Item {

    public StoneItem(Properties properties) {
        super(properties);
    }

    // --- Abstract: Each stone must return its key and color ---
    public abstract String getKey();
    public abstract int getColor();

    // --- Universal state detection ---

    /** Returns true if the player is holding this stone raw (not in gauntlet/holder). */
    public static boolean isRawInInventory(Player player, ItemStack stoneStack) {
        if (player == null || stoneStack == null || stoneStack.isEmpty()) return false;
        for (ItemStack invStack : player.getInventory().items) {
            if (invStack.getItem() instanceof StoneHolderItem) {
                ItemStack heldStone = StoneHolderItem.getStone(invStack);
                if (ItemStack.isSameItemSameTags(stoneStack, heldStone)) return false;
            }
            if (invStack.getItem() instanceof InfinityGauntletItem) {
                for (int i = 0; i < 6; i++) {
                    ItemStack gauntletStone = InfinityGauntletItem.getStoneStack(invStack, i);
                    if (ItemStack.isSameItemSameTags(stoneStack, gauntletStone)) return false;
                }
            }
        }
        return true;
    }

    /** Returns true if this stone is in any Infinity Gauntlet in the player's inventory. */
    public static boolean isInGauntlet(Player player, ItemStack stoneStack) {
        if (player == null || stoneStack == null || stoneStack.isEmpty()) return false;
        for (ItemStack invStack : player.getInventory().items) {
            if (invStack.getItem() instanceof InfinityGauntletItem) {
                for (int i = 0; i < 6; i++) {
                    ItemStack gauntletStone = InfinityGauntletItem.getStoneStack(invStack, i);
                    if (ItemStack.isSameItemSameTags(stoneStack, gauntletStone)) return true;
                }
            }
        }
        return false;
    }

    /** Returns true if this stone is in any Holder in the player's inventory. */
    public static boolean isInHolder(Player player, ItemStack stoneStack) {
        if (player == null || stoneStack == null || stoneStack.isEmpty()) return false;
        for (ItemStack invStack : player.getInventory().items) {
            if (invStack.getItem() instanceof StoneHolderItem) {
                ItemStack heldStone = StoneHolderItem.getStone(invStack);
                if (ItemStack.isSameItemSameTags(stoneStack, heldStone)) return true;
            }
        }
        return false;
    }

    public enum StoneState { RAW, HOLDER, GAUNTLET }
    public static StoneState getStoneState(Player player, ItemStack stoneStack) {
        if (isInGauntlet(player, stoneStack)) return StoneState.GAUNTLET;
        if (isInHolder(player, stoneStack)) return StoneState.HOLDER;
        return StoneState.RAW;
    }

    // --- Universal raw stone behavior: 1HP every 10 ticks, no knockback ---
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof Player player) {
            if (isRawInInventory(player, stack)) {
                if (level.getGameTime() % 10 == 0 && player.getHealth() > 0.5F) {
                    player.hurt(org.jcs.egm.stones.StoneUseDamage.get(level, player), 1.0F);
                }
            }
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    // --- Universal raw stone "use": 6 damage, no knockback ---
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        StoneState state = getStoneState(player, stack);

        // Handle raw stone use: deal 6 damage, no knockback
        if (state == StoneState.RAW && !world.isClientSide) {
            player.hurt(org.jcs.egm.stones.StoneUseDamage.get(world, player), 6.0F);
            return InteractionResultHolder.success(stack);
        }

        // Otherwise, delegate to the stone's ability logic
        return this.handleStoneUse(world, player, hand, stack, state);
    }

    // --- Universal right-click and hold-logic wiring (to be customized per stone) ---
    /** Child classes must implement to handle stone-specific abilities */
    protected abstract InteractionResultHolder<ItemStack> handleStoneUse(Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state);

    /** For use-tick/holding abilities: override if your stone uses onUseTick, etc. */
    public void onUseTick(Level world, Player player, ItemStack stack, int count) {}
    public void releaseUsing(ItemStack stack, Level world, Player player, int timeLeft) {}

    public static void openStoneAbilityMenu(ItemStack stack, InteractionHand hand, String stoneKey) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null) return;
        var abilityNames = org.jcs.egm.stones.StoneAbilityRegistries.getAbilityNames(stoneKey);
        if (abilityNames == null || abilityNames.isEmpty()) return;
        int selectedIdx = 0;
        if (stack.hasTag()) {
            selectedIdx = stack.getTag().getInt("AbilityIndex");
        }
        mc.setScreen(new org.jcs.egm.client.StoneAbilityMenuScreen(stack, hand, abilityNames, selectedIdx));
    }
}
