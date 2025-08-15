package org.jcs.egm.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jcs.egm.blocks.entity.NidavelliranForgeBlockEntity;
import org.jcs.egm.registry.ModBlocks;
import org.jcs.egm.registry.ModMenuTypes;

public class NidavelliranForgeMenu extends AbstractContainerMenu {
    public final NidavelliranForgeBlockEntity blockEntity;
    private final Level level;
    private final ContainerLevelAccess access;

    // Client constructor
    public NidavelliranForgeMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // Server constructor
    public NidavelliranForgeMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.NIDAVELLIRIAN_FORGE_MENU.get(), containerId);
        this.blockEntity = (NidavelliranForgeBlockEntity) entity;
        this.level = inv.player.level();
        this.access = ContainerLevelAccess.create(level, blockEntity.getBlockPos());

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            // Input slots at specified positions
            this.addSlot(new SlotItemHandler(iItemHandler, 0, 29, 18));  // Input 1
            this.addSlot(new SlotItemHandler(iItemHandler, 1, 67, 18));  // Input 2
            this.addSlot(new SlotItemHandler(iItemHandler, 2, 48, 53));  // Input 3
            
            // Output slot - make it output-only
            this.addSlot(new SlotItemHandler(iItemHandler, 3, 124, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false; // Can't place items in output slot
                }
            });
        });
    }

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 4;  // 3 inputs + 1 output
    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the correct forge slot
            boolean moved = false;
            
            // Try to place in the correct slot based on item type
            if (isPotterySherd(sourceStack)) {
                // Pottery sherds go to slot 0 (left)
                moved = moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + 1, false);
            } else if (isStone(sourceStack)) {
                // Stones go to slot 2 (bottom)
                moved = moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX + 2, TE_INVENTORY_FIRST_SLOT_INDEX + 3, false);
            } else if (isOtherRecipeItem(sourceStack)) {
                // Other items go to slot 1 (right)
                moved = moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX + 1, TE_INVENTORY_FIRST_SLOT_INDEX + 2, false);
            }
            
            if (!moved) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.err.println("Invalid slotIndex:" + index);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }
    
    // Helper methods for item type checking (matching those in BlockEntity)
    private boolean isPotterySherd(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.DANGER_POTTERY_SHERD) || 
               stack.is(net.minecraft.world.item.Items.HEART_POTTERY_SHERD) ||
               stack.is(net.minecraft.world.item.Items.PRIZE_POTTERY_SHERD) ||
               stack.is(net.minecraft.world.item.Items.MOURNER_POTTERY_SHERD) ||
               stack.is(net.minecraft.world.item.Items.EXPLORER_POTTERY_SHERD) ||
               stack.is(net.minecraft.world.item.Items.PLENTY_POTTERY_SHERD);
    }
    
    private boolean isOtherRecipeItem(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.NETHERITE_AXE) ||
               stack.is(net.minecraft.world.item.Items.WITHER_SKELETON_SKULL) ||
               stack.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE) ||
               stack.is(net.minecraft.world.item.Items.VERDANT_FROGLIGHT) ||
               stack.is(net.minecraft.world.item.Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE) ||
               stack.is(net.minecraft.world.item.Items.MUSIC_DISC_11);
    }
    
    private boolean isStone(ItemStack stack) {
        return stack.is(org.jcs.egm.registry.ModItems.POWER_STONE.get()) ||
               stack.is(org.jcs.egm.registry.ModItems.SOUL_STONE.get()) ||
               stack.is(org.jcs.egm.registry.ModItems.MIND_STONE.get()) ||
               stack.is(org.jcs.egm.registry.ModItems.TIME_STONE.get()) ||
               stack.is(org.jcs.egm.registry.ModItems.SPACE_STONE.get()) ||
               stack.is(org.jcs.egm.registry.ModItems.REALITY_STONE.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.NIDAVELLIRIAN_FORGE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            // Drop all items from the forge when menu is closed
            for (int i = 0; i < 3; i++) { // Only drop input slots, not output
                ItemStack stack = this.blockEntity.getItemHandler().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    player.drop(stack, false);
                    this.blockEntity.getItemHandler().setStackInSlot(i, ItemStack.EMPTY);
                }
            }
            // Also clear the output slot
            this.blockEntity.getItemHandler().setStackInSlot(3, ItemStack.EMPTY);
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}