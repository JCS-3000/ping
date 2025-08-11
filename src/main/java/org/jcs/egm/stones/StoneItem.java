package org.jcs.egm.stones;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jcs.egm.gauntlet.InfinityGauntletItem;
import org.jcs.egm.holders.StoneHolderItem;

/**
 * Base class for all Infinity Stones.
 * Uses container-aware, per-player cooldown gating so moving stones between containers
 * cannot reset their cooldown. Also re-syncs the hotbar overlay to the active container.
 */
public abstract class StoneItem extends Item {

    public StoneItem(Properties properties) { super(properties); }

    public abstract String getKey();
    public abstract int getColor();

    // ---- Location helpers ----
    public static boolean isRawInInventory(Player player, ItemStack stoneStack) {
        if (player == null || stoneStack == null || stoneStack.isEmpty()) return false;
        for (ItemStack invStack : player.getInventory().items) {
            if (invStack.getItem() instanceof StoneHolderItem) {
                ItemStack heldStone = StoneHolderItem.getStone(invStack);
                if (ItemStack.isSameItemSameTags(stoneStack, heldStone)) return false;
            }
            if (invStack.getItem() instanceof InfinityGauntletItem) {
                for (int i = 0; i < 6; i++) {
                    ItemStack gs = InfinityGauntletItem.getStoneStack(invStack, i);
                    if (ItemStack.isSameItemSameTags(stoneStack, gs)) return false;
                }
            }
        }
        return true;
    }

    public static boolean isInGauntlet(Player player, ItemStack stoneStack) {
        if (player == null || stoneStack == null || stoneStack.isEmpty()) return false;
        for (ItemStack invStack : player.getInventory().items) {
            if (invStack.getItem() instanceof InfinityGauntletItem) {
                for (int i = 0; i < 6; i++) {
                    ItemStack gs = InfinityGauntletItem.getStoneStack(invStack, i);
                    if (ItemStack.isSameItemSameTags(stoneStack, gs)) return true;
                }
            }
        }
        return false;
    }

    public static boolean isInHolder(Player player, ItemStack stoneStack) {
        if (player == null || stoneStack == null || stoneStack.isEmpty()) return false;
        for (ItemStack invStack : player.getInventory().items) {
            if (invStack.getItem() instanceof StoneHolderItem) {
                ItemStack hs = StoneHolderItem.getStone(invStack);
                if (ItemStack.isSameItemSameTags(stoneStack, hs)) return true;
            }
        }
        return false;
    }

    public enum StoneState { RAW, HOLDER, GAUNTLET }

    public static StoneState getStoneState(Player player, ItemStack stoneStack) {
        if (isInGauntlet(player, stoneStack)) return StoneState.GAUNTLET;
        if (isInHolder(player, stoneStack))   return StoneState.HOLDER;
        return StoneState.RAW;
    }

    // ---- Raw stone drawback ----
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof Player player) {
            if (isRawInInventory(player, stack) && level.getGameTime() % 10 == 0 && player.getHealth() > 0.5F) {
                player.hurt(StoneUseDamage.get(level, player), 1.0F);
            }
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    // ---- Use / Hold plumbing ----
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        StoneState state = getStoneState(player, stack);
        return this.handleStoneUse(world, player, hand, stack, state);
    }

    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public void onUseTick(Level world, LivingEntity entity, ItemStack stack, int count) {
        if (!(entity instanceof Player player)) return;
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(this.getKey(), stack);
        if (ability != null && ability.canHoldUse()) {
            ability.onUsingTick(world, player, stack, count);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(this.getKey(), stack);
        if (ability != null && ability.canHoldUse()) {
            ability.releaseUsing(world, player, stack, timeLeft);
        }
    }

    // ---- Dispatcher with persistent gate + overlay re-sync ----
    protected final InteractionResultHolder<ItemStack> handleAbilityWithCooldown(
            Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state) {

        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(this.getKey(), stack);
        if (ability == null) return InteractionResultHolder.pass(stack);

        // BLOCK if cooling (and re-sync overlay to the current container)
        if (StoneAbilityCooldowns.guardUse(player, stack, getKey(), ability)) {
            return InteractionResultHolder.pass(stack);
        }

        if (ability.canHoldUse()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        if (!world.isClientSide) {
            ability.activate(world, player, stack);
            // Apply container-aware cooldown + player gate using the stone stack
            StoneAbilityCooldowns.apply(player, stack, getKey(), ability);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    protected abstract InteractionResultHolder<ItemStack> handleStoneUse(
            Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state);

    public static void openStoneAbilityMenu(ItemStack stack, InteractionHand hand, String stoneKey) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null) return;
        var names = org.jcs.egm.stones.StoneAbilityRegistries.getAbilityNames(stoneKey);
        if (names == null || names.isEmpty()) return;
        int idx = stack.hasTag() ? stack.getTag().getInt("AbilityIndex") : 0;
        mc.setScreen(new org.jcs.egm.client.StoneAbilityMenuScreen(stack, hand, names, idx));
    }
}
