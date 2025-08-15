package org.jcs.egm.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jcs.egm.egm;

import java.util.List;

public class AnvilCraftingRecipeCategory implements IRecipeCategory<AnvilCraftingRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(egm.MODID, "anvil_crafting");
    public static final RecipeType<AnvilCraftingRecipe> RECIPE_TYPE =
            RecipeType.create(egm.MODID, "anvil_crafting", AnvilCraftingRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public AnvilCraftingRecipeCategory(IGuiHelper guiHelper) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(egm.MODID, "textures/gui/anvil_recipe_gui.png");
        this.background = guiHelper.createDrawable(location, 7, 12, 162, 63);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.ANVIL));
    }

    @Override
    public RecipeType<AnvilCraftingRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.egm.anvil_crafting");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AnvilCraftingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 55, 33)
                .addItemStack(recipe.getBaseItem());
        builder.addSlot(RecipeIngredientRole.INPUT, 55, 5)
                .addItemStack(recipe.getAdditionItem());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 89, 12)
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(AnvilCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        Component note = Component.literal("Shift + Right Click the anvil with a Nether Star... then STAND BACK!");

        int maxWidth = background.getWidth();
        List<FormattedCharSequence> lines = font.split(note, maxWidth - 2);

        int lineH = font.lineHeight;
        int totalH = lines.size() * lineH;
        int startX = 1;
        int startY = background.getHeight() - totalH - 1 + 36; // lowered by 20 pixels

        if (startY < 0) startY = 0;

        for (int i = 0; i < lines.size(); i++) {
            guiGraphics.drawString(font, lines.get(i), startX, startY + i * lineH, 0x555555, false);
        }
    }
}
