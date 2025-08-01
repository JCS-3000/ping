package org.jcs.egm.holders;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jcs.egm.holders.StoneHolderMenu;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityRegistries;

// Updated: imports for new stone item classes
import org.jcs.egm.stones.stone_mind.MindStoneItem;
import org.jcs.egm.stones.stone_power.PowerStoneItem;
import org.jcs.egm.stones.stone_space.SpaceStoneItem;
import org.jcs.egm.stones.stone_reality.RealityStoneItem;
import org.jcs.egm.stones.stone_time.TimeStoneItem;
import org.jcs.egm.stones.stone_soul.SoulStoneItem;

public class StoneHolderItem extends Item {
    private final String stoneType;

    public StoneHolderItem(String stoneType, Properties properties) {
        super(properties);
        this.stoneType = stoneType;
    }

    /** Returns the key for the stone type this holder accepts (e.g., "mind", "power", etc.) */
    public String getStoneKey() {
        return this.stoneType;
    }

    /** Static helper to get the stone stored inside a StoneHolderItem */
    public static ItemStack getStone(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStackHandler handler = new ItemStackHandler(1);
        if (stack.hasTag() && stack.getTag().contains("Stone")) {
            handler.deserializeNBT(stack.getTag().getCompound("Stone"));
        }
        return handler.getStackInSlot(0);
    }

    /** Static helper to set the stone back inside a StoneHolderItem after mutation */
    public static void setStone(ItemStack holder, ItemStack inside) {
        if (holder == null || holder.isEmpty()) return;
        ItemStackHandler handler = new ItemStackHandler(1);
        handler.setStackInSlot(0, inside);
        if (!holder.hasTag()) holder.setTag(new net.minecraft.nbt.CompoundTag());
        holder.getTag().put("Stone", handler.serializeNBT());
    }

    /** Capability for inventory handler */
    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, net.minecraft.nbt.CompoundTag nbt) {
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

    /** Logic to check if this stack is the correct stone for this holder */
    public boolean isCorrectStone(ItemStack stack) {
        if (stoneType == null) return false;
        return switch (stoneType) {
            case "mind" -> stack.getItem() instanceof MindStoneItem;
            case "power" -> stack.getItem() instanceof PowerStoneItem;
            case "space" -> stack.getItem() instanceof SpaceStoneItem;
            case "reality" -> stack.getItem() instanceof RealityStoneItem;
            case "soul" -> stack.getItem() instanceof SoulStoneItem;
            case "time" -> stack.getItem() instanceof TimeStoneItem;
            default -> false;
        };
    }

    public MenuProvider getMenuProvider(ItemStack stack) {
        return new SimpleMenuProvider(
                (id, inv, plyr) -> new StoneHolderMenu(id, inv, stack),
                Component.literal("Stone Holder")
        );
    }

    public String getStoneType() {
        return stoneType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack holder = player.getItemInHand(hand);
        ItemStack inside = getStone(holder);
        if (inside.isEmpty()) {
            return InteractionResultHolder.pass(holder);
        }
        // Use the *inside* stone for ability selection and activation
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(this.getStoneKey(), inside);
        if (ability != null && ability.canHoldUse()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(holder);
        } else if (ability != null && !world.isClientSide) {
            ability.activate(world, player, inside);
            // After use, save any NBT changes to the stone back into the holder!
            setStone(holder, inside);
            return InteractionResultHolder.success(holder);
        }
        return InteractionResultHolder.pass(holder);
    }
}
