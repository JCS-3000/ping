package org.jcs.egm.holders;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityRegistries;

public class StoneHolderItem extends Item {
    private final String stoneType;

    public StoneHolderItem(String stoneType, Properties properties) {
        super(properties);
        this.stoneType = stoneType;
    }

    public String getStoneKey() {
        return this.stoneType;
    }

    /** Extracts the single stone inside */
    public static ItemStack getStone(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStackHandler handler = new ItemStackHandler(1);
        if (stack.hasTag() && stack.getTag().contains("Stone")) {
            handler.deserializeNBT(stack.getTag().getCompound("Stone"));
        }
        return handler.getStackInSlot(0);
    }

    /** Puts a (possibly mutated) stone back and updates model predicate bitmask */
    public static void setStone(ItemStack holder, ItemStack inside) {
        if (holder == null || holder.isEmpty()) return;
        ItemStackHandler handler = new ItemStackHandler(1);
        handler.setStackInSlot(0, inside);
        if (!holder.hasTag()) holder.setTag(new CompoundTag());
        holder.getTag().put("Stone", handler.serializeNBT());
        updateStoneBitmaskNBT(holder);
    }

    /** Returns 0 (empty) or 1 (has stone) for predicate */
    public static int getStonePresenceBit(ItemStack stack) {
        return getStone(stack).isEmpty() ? 0 : 1;
    }

    /** Writes the bit to NBT for model predicate override */
    public static void updateStoneBitmaskNBT(ItemStack stack) {
        stack.getOrCreateTag().putInt("StoneBitmask", getStonePresenceBit(stack));
    }

    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        return new net.minecraftforge.common.capabilities.ICapabilityProvider() {
            private final LazyOptional<IItemHandler> handler = LazyOptional.of(() ->
                    new ItemStackHandler(1) {
                        @Override
                        public boolean isItemValid(int slot, @NotNull ItemStack toInsert) {
                            return isCorrectStone(toInsert);
                        }
                    }
            );
            @Override
            public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, net.minecraft.core.Direction side) {
                return cap == ForgeCapabilities.ITEM_HANDLER ? handler.cast() : LazyOptional.empty();
            }
        };
    }

    /** Ensures only the matching stone type can go inside */
    public boolean isCorrectStone(ItemStack stack) {
        if (stoneType == null) return false;
        return switch (stoneType) {
            case "mind"    -> stack.getItem() instanceof org.jcs.egm.stones.stone_mind.MindStoneItem;
            case "power"   -> stack.getItem() instanceof org.jcs.egm.stones.stone_power.PowerStoneItem;
            case "space"   -> stack.getItem() instanceof org.jcs.egm.stones.stone_space.SpaceStoneItem;
            case "reality" -> stack.getItem() instanceof org.jcs.egm.stones.stone_reality.RealityStoneItem;
            case "soul"    -> stack.getItem() instanceof org.jcs.egm.stones.stone_soul.SoulStoneItem;
            case "time"    -> stack.getItem() instanceof org.jcs.egm.stones.stone_time.TimeStoneItem;
            default          -> false;
        };
    }

    public MenuProvider getMenuProvider(ItemStack stack) {
        return new SimpleMenuProvider(
                (id, inv, plyr) -> new StoneHolderMenu(id, inv, stack),
                Component.literal("Stone Holder")
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack holder = player.getItemInHand(hand);
        ItemStack inside = getStone(holder);
        if (inside.isEmpty()) {
            return InteractionResultHolder.pass(holder);
        }
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(this.getStoneKey(), inside);
        if (ability != null && ability.canHoldUse()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(holder);
        } else if (ability != null && !world.isClientSide) {
            ability.activate(world, player, inside);
            setStone(holder, inside);
            return InteractionResultHolder.success(holder);
        }
        return InteractionResultHolder.pass(holder);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level world, LivingEntity entity, ItemStack holder, int count) {
        if (!(entity instanceof Player player)) return;
        ItemStack inside = getStone(holder);
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(this.stoneType, inside);
        if (ability != null && ability.canHoldUse()) {
            ability.onUsingTick(world, player, inside, count);
            setStone(holder, inside);
        }
    }

    @Override
    public void releaseUsing(ItemStack holder, Level world, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        ItemStack inside = getStone(holder);
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(this.stoneType, inside);
        if (ability != null && ability.canHoldUse()) {
            ability.releaseUsing(world, player, inside, timeLeft);
            setStone(holder, inside);
        }
    }
}
