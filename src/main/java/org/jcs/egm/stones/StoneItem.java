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
 * Now includes cooldown utilities and a common handler for instant/hold abilities.
 */
public abstract class StoneItem extends Item {

    public StoneItem(Properties properties) {
        super(properties);
    }

    // --- Abstract: Each stone must return its key and color ---
    public abstract String getKey();
    public abstract int getColor();

    // --- Universal state detection ---
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
        if (isInHolder(player, stoneStack)) return StoneState.HOLDER;
        return StoneState.RAW;
    }

    // --- Universal raw stone behavior: 1 HP every 10 ticks, no knockback ---
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof Player player) {
            if (isRawInInventory(player, stack) && level.getGameTime() % 10 == 0 && player.getHealth() > 0.5F) {
                player.hurt(StoneUseDamage.get(level, player), 1.0F);
            }
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    // --- All uses delegate to handleStoneUse (raw-damage shortcut removed) ---
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        StoneState state = getStoneState(player, stack);
        return this.handleStoneUse(world, player, hand, stack, state);
    }

    // --- Stones can be charged like gauntlet/holder ---
    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    // --- Forward use-tick and release to the active ability for RAW stones ---
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
            // NOTE: Cooldown for hold abilities should be applied by the ability
            // at the exact moment it "fires", e.g.:
            // StoneAbilityCooldowns.applyFromStack(player, stack, getKey(), ability);
        }
    }

    // --- Abstract hook for each stone’s actual ability dispatch ---
    protected abstract InteractionResultHolder<ItemStack> handleStoneUse(Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state);

    /** Override if your stone uses on-tick charging */
    public void onUseTick(Level world, Player player, ItemStack stack, int count) {}

    /** Override if your stone needs a release action */
    public void releaseUsing(ItemStack stack, Level world, Player player, int timeLeft) {}

    /** Opens the ability‐selection GUI */
    public static void openStoneAbilityMenu(ItemStack stack, InteractionHand hand, String stoneKey) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null) return;
        var names = org.jcs.egm.stones.StoneAbilityRegistries.getAbilityNames(stoneKey);
        if (names == null || names.isEmpty()) return;
        int idx = stack.hasTag() ? stack.getTag().getInt("AbilityIndex") : 0;
        mc.setScreen(new org.jcs.egm.client.StoneAbilityMenuScreen(stack, hand, names, idx));
    }

    // ========================================================================
    // Cooldown helpers + a common dispatcher you can call from handleStoneUse()
    // ========================================================================

    /** True if the vanilla cooldown overlay is active for this item. */
    protected final boolean isOnCooldown(Player player) {
        return StoneAbilityCooldowns.isCooling(player, this);
    }

    /** Apply cooldown for an instant ability (uses this item for the overlay). */
    protected final void applyInstantCooldown(Player player, IGStoneAbility ability) {
        StoneAbilityCooldowns.apply(player, this, getKey(), ability);
    }

    /**
     * Common dispatcher:
     * - Blocks use if this item is cooling.
     * - For hold abilities: only checks cooldown and starts using; the ability must call
     *   StoneAbilityCooldowns.applyFromStack(...) when it actually fires.
     * - For instant abilities: activates on server and applies cooldown immediately.
     */
    protected final InteractionResultHolder<ItemStack> handleAbilityWithCooldown(Level world, Player player, InteractionHand hand, ItemStack stack, IGStoneAbility ability) {
        if (ability == null) return InteractionResultHolder.pass(stack);

        // If any ability is attempted while this item is on cooldown, block.
        if (isOnCooldown(player)) {
            return InteractionResultHolder.pass(stack);
        }

        if (ability.canHoldUse()) {
            // Start using; cooldown applied later by the ability when it fires.
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        // Instant ability: run server-side & apply cooldown immediately
        if (!world.isClientSide) {
            ability.activate(world, player, stack);
            applyInstantCooldown(player, ability);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }
}
