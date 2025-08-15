package org.jcs.egm.jei;

import net.minecraft.world.item.ItemStack;

public class AnvilCraftingRecipe {
    private final ItemStack baseItem;
    private final ItemStack additionItem;
    private final ItemStack result;

    public AnvilCraftingRecipe(ItemStack baseItem, ItemStack additionItem, ItemStack result) {
        this.baseItem = baseItem;
        this.additionItem = additionItem;
        this.result = result;
    }

    public ItemStack getBaseItem() {
        return baseItem;
    }

    public ItemStack getAdditionItem() {
        return additionItem;
    }

    public ItemStack getResult() {
        return result;
    }
}