package org.jcs.egm.gauntlet;

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
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.registry.ModMenuTypes;

public class InfinityGauntletMenu extends AbstractContainerMenu {

    private final ItemStack gauntletStack;
    private final ItemStackHandler itemHandler;

    // Slot X/Y coordinates for your GUI (adjust as needed!)
    private static final int[] xSlots = {15, 41, 67, 93, 119, 145};
    private static final int ySlot = 36;

    public InfinityGauntletMenu(int id, Inventory playerInventory, ItemStack gauntletStack) {
        super(ModMenuTypes.INFINITY_GAUNTLET.get(), id);
        this.gauntletStack = gauntletStack;

        // Load inventory from stack NBT, or start empty if not present
        this.itemHandler = new ItemStackHandler(6) {
            @Override
            protected void onContentsChanged(int slot) {
                saveToStack();
            }
        };

        // Load persisted inventory if it exists
        if (gauntletStack.hasTag() && gauntletStack.getTag().contains("Stones")) {
            itemHandler.deserializeNBT(gauntletStack.getTag().getCompound("Stones"));
        }

        // Add 6 slots, one for each Infinity Stone (with restrictions)
        for (int i = 0; i < 6; i++) {
            this.addSlot(new InfinityStoneSlot(itemHandler, i, xSlots[i], ySlot, i));
        }

        // Player inventory: starts at y = 87
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 87 + row * 18));
            }
        }
        // Hotbar: starts at y = 145
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 145));
        }
    }

    private void saveToStack() {
        if (!gauntletStack.hasTag()) gauntletStack.setTag(new net.minecraft.nbt.CompoundTag());
        gauntletStack.getTag().put("Stones", itemHandler.serializeNBT());
        // --- Add this line! ---
        InfinityGauntletItem.updateStonesBitmaskNBT(gauntletStack);
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // Add distance or item checks if needed
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveToStack(); // Save when menu is closed!
    }

    // Enable shift-click for stones, player inventory, and hotbar
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            originalStack = slotStack.copy();

            // Gauntlet slots: 0–5, Player inventory: 6+
            if (index < 6) {
                // Shift-clicking from gauntlet -> try move to player inventory
                if (!this.moveItemStackTo(slotStack, 6, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Shift-clicking from player inventory/hotbar
                int stoneSlot = getStoneSlotForItem(slotStack);
                if (stoneSlot >= 0 && this.slots.get(stoneSlot).getItem().isEmpty()) {
                    // Move to correct gauntlet slot
                    if (!this.moveItemStackTo(slotStack, stoneSlot, stoneSlot + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Not a stone, or gauntlet slot full—do nothing or fallback behavior
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

    /** Determines which gauntlet slot (0–5) is for this stone item, or -1 if not an Infinity Stone */
    private int getStoneSlotForItem(ItemStack stack) {
        if (stack.getItem() == ModItems.TIME_STONE.get())    return 0;
        if (stack.getItem() == ModItems.POWER_STONE.get())   return 1;
        if (stack.getItem() == ModItems.SPACE_STONE.get())   return 2;
        if (stack.getItem() == ModItems.REALITY_STONE.get()) return 3;
        if (stack.getItem() == ModItems.SOUL_STONE.get())    return 4;
        if (stack.getItem() == ModItems.MIND_STONE.get())    return 5;
        return -1;
    }

    // Custom Slot: only allows correct Infinity Stone in each slot
    private static class InfinityStoneSlot extends SlotItemHandler {
        private final int stoneIndex;

        public InfinityStoneSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, int stoneIndex) {
            super(itemHandler, index, xPosition, yPosition);
            this.stoneIndex = stoneIndex;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.isEmpty()) return false;
            return switch (stoneIndex) {
                case 0 -> stack.getItem() == ModItems.TIME_STONE.get();
                case 1 -> stack.getItem() == ModItems.POWER_STONE.get();
                case 2 -> stack.getItem() == ModItems.SPACE_STONE.get();
                case 3 -> stack.getItem() == ModItems.REALITY_STONE.get();
                case 4 -> stack.getItem() == ModItems.SOUL_STONE.get();
                case 5 -> stack.getItem() == ModItems.MIND_STONE.get();
                default -> false;
            };
        }
    }

    // ---- MenuProvider Inner Class ----
    public static class Provider implements MenuProvider {
        private final ItemStack stack;

        public Provider(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal("Infinity Gauntlet");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
            return new InfinityGauntletMenu(id, playerInventory, stack);
        }
    }
}
