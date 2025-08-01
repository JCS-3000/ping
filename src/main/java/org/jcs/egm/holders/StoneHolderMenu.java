package org.jcs.egm.holders;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jcs.egm.registry.ModMenuTypes;

public class StoneHolderMenu extends AbstractContainerMenu {

    private final ItemStack holderStack;
    private final ItemStackHandler itemHandler;

    // Stone slot is between 2 and 3 (slot index 3 visually)
    private static final int STONE_SLOT_INDEX = 3;
    private static final int STONE_SLOT_X = 80; // Adjust based on GUI
    private static final int STONE_SLOT_Y = 36;

    // Dummy container used for all dummy slots so we never pass null!
    private static final net.minecraft.world.Container DUMMY_CONTAINER = new net.minecraft.world.Container() {
        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return true; }
        @Override public net.minecraft.world.item.ItemStack getItem(int slot) { return net.minecraft.world.item.ItemStack.EMPTY; }
        @Override public net.minecraft.world.item.ItemStack removeItem(int slot, int amount) { return net.minecraft.world.item.ItemStack.EMPTY; }
        @Override public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot) { return net.minecraft.world.item.ItemStack.EMPTY; }
        @Override public void setItem(int slot, net.minecraft.world.item.ItemStack stack) {}
        @Override public int getMaxStackSize() { return 1; }
        @Override public void setChanged() {}
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return false; }
        @Override public void clearContent() {}
    };

    public StoneHolderMenu(int id, Inventory playerInventory, ItemStack holderStack) {
        super(ModMenuTypes.STONE_HOLDER.get(), id);
        this.holderStack = holderStack;

        // Single slot for the stone
        this.itemHandler = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                saveToStack();
            }
        };

        // Load saved stone if present
        if (holderStack.hasTag() && holderStack.getTag().contains("Stone")) {
            itemHandler.deserializeNBT(holderStack.getTag().getCompound("Stone"));
        }

        // Add dummy slots for GUI alignment (slots 0–2)
        for (int i = 0; i < 3; i++) {
            this.addSlot(new DummySlot(i));
        }
        // Add real stone slot (slot 3, visible in the middle)
        this.addSlot(new StoneSlot(itemHandler, 0, STONE_SLOT_X, STONE_SLOT_Y, holderStack));
        // Add dummy slots for GUI alignment (slots 4–5)
        for (int i = 4; i < 6; i++) {
            this.addSlot(new DummySlot(i));
        }

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 87 + row * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 145));
        }
    }

    private void saveToStack() {
        if (!holderStack.hasTag()) holderStack.setTag(new net.minecraft.nbt.CompoundTag());
        holderStack.getTag().put("Stone", itemHandler.serializeNBT());
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveToStack();
    }

    // Enable SRC for the stone slot and player inventory
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            originalStack = slotStack.copy();

            if (index == STONE_SLOT_INDEX) {
                // From stone holder -> player inventory
                if (!this.moveItemStackTo(slotStack, 6, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 6) {
                // From player inventory -> stone slot
                if (this.slots.get(STONE_SLOT_INDEX).mayPlace(slotStack) && this.slots.get(STONE_SLOT_INDEX).getItem().isEmpty()) {
                    if (!this.moveItemStackTo(slotStack, STONE_SLOT_INDEX, STONE_SLOT_INDEX + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return originalStack;
    }

    // Only allow the proper stone in the slot
    private static class StoneSlot extends SlotItemHandler {
        private final ItemStack holderStack;

        public StoneSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, ItemStack holderStack) {
            super(itemHandler, index, xPosition, yPosition);
            this.holderStack = holderStack;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!(holderStack.getItem() instanceof StoneHolderItem holderItem)) return false;
            return holderItem.isCorrectStone(stack);
        }
    }

    // Dummy slots for GUI alignment (not usable)
    private static class DummySlot extends Slot {
        public DummySlot(int index) {
            super(DUMMY_CONTAINER, index, -1000, -1000); // Use dummy, never null!
        }

        @Override public boolean isActive() { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }

    // MenuProvider for NetworkHooks
    public static class Provider implements MenuProvider {
        private final ItemStack stack;

        public Provider(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal("Stone Holder");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
            return new StoneHolderMenu(id, playerInventory, stack);
        }
    }
}
