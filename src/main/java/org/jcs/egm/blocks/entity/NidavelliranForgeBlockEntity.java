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
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot >= 3) return false; // Output slot
            
            switch (slot) {
                case 0: // Left slot - pottery sherds only
                    return isPotterySherd(stack);
                case 1: // Right slot - other recipe items only  
                    return isOtherRecipeItem(stack);
                case 2: // Bottom slot - stones only
                    return isStone(stack);
                default:
                    return false;
            }
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // When extracting from output slot, trigger crafting consumption
            if (slot == 3 && !simulate) {
                ItemStack outputStack = getStackInSlot(3);
                if (!outputStack.isEmpty()) {
                    // Consume ingredients only when actually taking the output
                    consumeIngredientsForCrafting();
                }
            }
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
        
        // Update output slot display without auto-crafting
        updateOutputDisplay();
    }
    
    private void updateOutputDisplay() {
        if (level == null || level.isClientSide()) {
            return;
        }

        // Get input items - slot positions are now enforced
        ItemStack potterySherd = itemHandler.getStackInSlot(0); // Left slot
        ItemStack otherItem = itemHandler.getStackInSlot(1);    // Right slot
        ItemStack stone = itemHandler.getStackInSlot(2);        // Bottom slot
        ItemStack currentOutput = itemHandler.getStackInSlot(3);

        // Check what can be crafted
        ItemStack craftResult = getCraftingResult(potterySherd, otherItem, stone);

        // Only show the result in the output slot, don't actually craft
        if (!craftResult.isEmpty() && currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(3, craftResult.copy());
        } else if (craftResult.isEmpty() && !currentOutput.isEmpty()) {
            // Clear output if no valid recipe
            itemHandler.setStackInSlot(3, ItemStack.EMPTY);
        }
    }

    private void consumeIngredientsForCrafting() {
        if (level == null || level.isClientSide()) {
            return;
        }

        // Get input items - slot positions are now enforced
        ItemStack potterySherd = itemHandler.getStackInSlot(0); // Left slot
        ItemStack otherItem = itemHandler.getStackInSlot(1);    // Right slot  
        ItemStack stone = itemHandler.getStackInSlot(2);        // Bottom slot

        // Check if crafting is valid
        ItemStack craftResult = getCraftingResult(potterySherd, otherItem, stone);

        if (!craftResult.isEmpty()) {
            // Consume only pottery sherd and other item, NOT the stone
            if (!potterySherd.isEmpty()) potterySherd.shrink(1);
            if (!otherItem.isEmpty()) otherItem.shrink(1);
            // Stone is NOT consumed - intentionally left out
            
            // The output slot will be cleared automatically by the extraction
            // Don't manually clear it here since the player is taking the item
        }
    }
    
    // Helper methods for slot validation
    private boolean isPotterySherd(ItemStack stack) {
        return stack.is(Items.DANGER_POTTERY_SHERD) || 
               stack.is(Items.HEART_POTTERY_SHERD) ||
               stack.is(Items.PRIZE_POTTERY_SHERD) ||
               stack.is(Items.MOURNER_POTTERY_SHERD) ||
               stack.is(Items.EXPLORER_POTTERY_SHERD) ||
               stack.is(Items.PLENTY_POTTERY_SHERD);
    }
    
    private boolean isOtherRecipeItem(ItemStack stack) {
        return stack.is(Items.NETHERITE_AXE) ||
               stack.is(Items.WITHER_SKELETON_SKULL) ||
               stack.is(Items.ENCHANTED_GOLDEN_APPLE) ||
               stack.is(Items.VERDANT_FROGLIGHT) ||
               stack.is(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE) ||
               stack.is(Items.MUSIC_DISC_11);
    }
    
    private boolean isStone(ItemStack stack) {
        return stack.is(ModItems.POWER_STONE.get()) ||
               stack.is(ModItems.SOUL_STONE.get()) ||
               stack.is(ModItems.MIND_STONE.get()) ||
               stack.is(ModItems.TIME_STONE.get()) ||
               stack.is(ModItems.SPACE_STONE.get()) ||
               stack.is(ModItems.REALITY_STONE.get());
    }

    private ItemStack getCraftingResult(ItemStack potterySherd, ItemStack otherItem, ItemStack stone) {
        // Power Stone Holder (Kree Warhammer): Danger Pottery Sherd + Netherite Axe + Power Stone
        if (hasRecipeItems(potterySherd, otherItem, stone, Items.DANGER_POTTERY_SHERD, Items.NETHERITE_AXE, ModItems.POWER_STONE.get())) {
            return new ItemStack(ModItems.POWER_STONE_HOLDER.get(), 1);
        }
        
        // Soul Stone Holder (Red Skull): Heart Pottery Sherd + Wither Skeleton Skull + Soul Stone
        if (hasRecipeItems(potterySherd, otherItem, stone, Items.HEART_POTTERY_SHERD, Items.WITHER_SKELETON_SKULL, ModItems.SOUL_STONE.get())) {
            return new ItemStack(ModItems.SOUL_STONE_HOLDER.get(), 1);
        }
        
        // Mind Stone Holder (Lokian Scepter): Prize Pottery Sherd + Enchanted Golden Apple + Mind Stone
        if (hasRecipeItems(potterySherd, otherItem, stone, Items.PRIZE_POTTERY_SHERD, Items.ENCHANTED_GOLDEN_APPLE, ModItems.MIND_STONE.get())) {
            return new ItemStack(ModItems.MIND_STONE_HOLDER.get(), 1);
        }
        
        // Time Stone Holder (Eye of Agamotto): Mourner Pottery Sherd + Verdant Froglight + Time Stone
        if (hasRecipeItems(potterySherd, otherItem, stone, Items.MOURNER_POTTERY_SHERD, Items.VERDANT_FROGLIGHT, ModItems.TIME_STONE.get())) {
            return new ItemStack(ModItems.TIME_STONE_HOLDER.get(), 1);
        }
        
        // Space Stone Holder (Tesseract): Explorer Pottery Sherd + Wild Armor Trim Smithing Template + Space Stone
        if (hasRecipeItems(potterySherd, otherItem, stone, Items.EXPLORER_POTTERY_SHERD, Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, ModItems.SPACE_STONE.get())) {
            return new ItemStack(ModItems.SPACE_STONE_HOLDER.get(), 1);
        }
        
        // Reality Stone Holder (Aether): Plenty Pottery Sherd + Music Disc 11 + Reality Stone
        if (hasRecipeItems(potterySherd, otherItem, stone, Items.PLENTY_POTTERY_SHERD, Items.MUSIC_DISC_11, ModItems.REALITY_STONE.get())) {
            return new ItemStack(ModItems.REALITY_STONE_HOLDER.get(), 1);
        }
        
        return ItemStack.EMPTY;
    }
    
    private boolean hasRecipeItems(ItemStack potterySherd, ItemStack otherItem, ItemStack stone, 
                                   net.minecraft.world.item.Item requiredSherd, net.minecraft.world.item.Item requiredOther, net.minecraft.world.item.Item requiredStone) {
        return !potterySherd.isEmpty() && potterySherd.is(requiredSherd) &&
               !otherItem.isEmpty() && otherItem.is(requiredOther) &&
               !stone.isEmpty() && stone.is(requiredStone);
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