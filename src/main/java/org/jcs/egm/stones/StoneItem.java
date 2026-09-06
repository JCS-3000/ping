package org.jcs.egm.stones;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jcs.egm.registry.ModEffects;

/**
 * Base class for all Infinity Stones.
 * Base class for raw Infinity Stones.
 */
public abstract class StoneItem extends Item {

    public StoneItem(Properties properties) { super(properties); }

    public abstract String getKey();
    public abstract int getColor();

    // ---- Location helpers ----
    public static boolean isRawInInventory(Player player, ItemStack stoneStack) {
        if (player == null || stoneStack == null || stoneStack.isEmpty()) return false;
        return stoneStack.getItem() instanceof StoneItem;
    }

    public static boolean hasRawStone(Player player) {
        if (player == null) return false;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof StoneItem) return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItem() instanceof StoneItem) return true;
        }
        return false;
    }

    public static boolean isInGauntlet(Player player, ItemStack stoneStack) {
        if (player == null || stoneStack == null || stoneStack.isEmpty()) return false;
        for (ItemStack invStack : player.getInventory().items) {
            if (invStack.getItem() instanceof org.jcs.egm.gauntlet.InfinityGauntletItem
                    && StoneContainer.containsStone(invStack, stoneStack)) return true;
        }
        return false;
    }

    public static boolean isInHolder(Player player, ItemStack stoneStack) {
        if (player == null || stoneStack == null || stoneStack.isEmpty()) return false;
        for (ItemStack invStack : player.getInventory().items) {
            if (StoneContainer.isHolderLike(invStack) && StoneContainer.containsStone(invStack, stoneStack)) return true;
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
            if (!player.getAbilities().instabuild && isRawInInventory(player, stack)) {
                if (level.getGameTime() % 20 == 0) {
                    applyStoneSicknessEffects(player);
                }
                if (isHeldBy(player, stack) && level.getGameTime() % 200 == 0 && player.getHealth() > 0.5F) {
                    StoneUseDamage.hurtPlayerWithoutKnockback(level, player, 1.0F);
                }
            }
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    private static boolean isHeldBy(Player player, ItemStack stack) {
        return player.getMainHandItem() == stack || player.getOffhandItem() == stack;
    }

    private static void applyStoneSicknessEffects(Player player) {
        player.addEffect(new MobEffectInstance(ModEffects.STONE_SICKNESS.get(), 40, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 2, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 2, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, false));
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
        StoneAbilityUse.onUseTick(world, player, stack, this.getKey(), count);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        StoneAbilityUse.releaseUsing(world, player, stack, this.getKey(), timeLeft);
    }

    // ---- Dispatcher with energy gate ----
    protected final InteractionResultHolder<ItemStack> handleAbilityWithEnergy(
            Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state) {

        StoneAbilityContext context = StoneAbilityUse.context(world, player, hand, getKey(), stack, stack);
        return StoneAbilityUse.use(context, stack, () -> {});
    }

    protected InteractionResultHolder<ItemStack> handleStoneUse(
            Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state) {
        return handleAbilityWithEnergy(world, player, hand, stack, state);
    }
}
