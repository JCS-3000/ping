package org.jcs.egm.jei;

import net.minecraft.world.item.ItemStack;

public class NidavelliranForgeRecipe {
    private final ItemStack potterySherd;
    private final ItemStack otherItem;
    private final ItemStack stone;
    private final ItemStack result;

    public NidavelliranForgeRecipe(ItemStack potterySherd, ItemStack otherItem, ItemStack stone, ItemStack result) {
        this.potterySherd = potterySherd;
        this.otherItem = otherItem;
        this.stone = stone;
        this.result = result;
    }

    public ItemStack getPotterySherd() {
        return potterySherd;
    }

    public ItemStack getOtherItem() {
        return otherItem;
    }

    public ItemStack getStone() {
        return stone;
    }

    public ItemStack getResult() {
        return result;
    }
}