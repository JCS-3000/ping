package org.jcs.egm.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jcs.egm.gui.NidavelliranForgeMenu;
import org.jcs.egm.registry.ModBlockEntities;
import org.jcs.egm.registry.ModItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NidavelliranForgeBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // Trigger crafting check when inputs change
            if (slot < 3) {
                checkAndCraft();
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // Slots 0, 1, 2 are inputs, slot 3 is output
            return slot < 3; // Only allow items in input slots
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Allow extraction from all slots
            return super.extractItem(slot, amount, simulate);
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public NidavelliranForgeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.NIDAVELLIRIAN_FORGE.get(), pos, blockState);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
    }

    public void dropContents() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public void tick() {
        if (level.isClientSide()) {
            return;
        }
        // Crafting is instant and handled in onContentsChanged
    }

    private void checkAndCraft() {
        if (level == null || level.isClientSide()) {
            return;
        }

        // Get input items
        ItemStack input1 = itemHandler.getStackInSlot(0);
        ItemStack input2 = itemHandler.getStackInSlot(1);
        ItemStack input3 = itemHandler.getStackInSlot(2);
        ItemStack currentOutput = itemHandler.getStackInSlot(3);

        // Simple example crafting logic - you can customize this
        ItemStack craftResult = getCraftingResult(input1, input2, input3);

        if (!craftResult.isEmpty()) {
            // Check if output slot can accept the result
            if (currentOutput.isEmpty() || 
                (currentOutput.getItem() == craftResult.getItem() && 
                 currentOutput.getCount() + craftResult.getCount() <= currentOutput.getMaxStackSize())) {
                
                // Perform crafting
                if (currentOutput.isEmpty()) {
                    itemHandler.setStackInSlot(3, craftResult.copy());
                } else {
                    currentOutput.grow(craftResult.getCount());
                }

                // Consume inputs (1 from each input slot)
                if (!input1.isEmpty()) input1.shrink(1);
                if (!input2.isEmpty()) input2.shrink(1);
                if (!input3.isEmpty()) input3.shrink(1);
            }
        } else if (!currentOutput.isEmpty()) {
            // Clear output if no valid recipe
            // This allows manual removal of items
        }
    }

    private ItemStack getCraftingResult(ItemStack input1, ItemStack input2, ItemStack input3) {
        // Red Skull Recipe: Wither Skeleton Skull + Totem of Undying + Beetroot Stew
        if (isItem(input1, Items.WITHER_SKELETON_SKULL) && 
            isItem(input2, Items.TOTEM_OF_UNDYING) && 
            isItem(input3, Items.BEETROOT_SOUP)) {
            return new ItemStack(ModItems.SOUL_STONE_HOLDER.get(), 1);
        }
        
        return ItemStack.EMPTY;
    }
    
    private boolean isItem(ItemStack stack, net.minecraft.world.item.Item item) {
        return !stack.isEmpty() && stack.is(item);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.egm.nidavellirian_forge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NidavelliranForgeMenu(containerId, playerInventory, this);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public MenuProvider getMenuProvider() {
        return this;
    }
}