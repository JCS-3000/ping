package org.jcs.egm.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.constants.VanillaTypes;
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
import org.jcs.egm.egm;
import org.jcs.egm.registry.ModBlocks;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NidavelliranForgeRecipeCategory implements IRecipeCategory<NidavelliranForgeRecipe> {
    
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(egm.MODID, "nidavellir_gui");
    public static final RecipeType<NidavelliranForgeRecipe> RECIPE_TYPE = 
            RecipeType.create(egm.MODID, "nidavellirian_forge", NidavelliranForgeRecipe.class);
    
    private final IDrawable background;
    private final IDrawable icon;

    public NidavelliranForgeRecipeCategory(IGuiHelper guiHelper) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(egm.MODID, "textures/gui/nidavellir_gui.png");
        this.background = guiHelper.createDrawable(location, 7, 12, 162, 63);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.NIDAVELLIRIAN_FORGE.get()));
    }


    @Override
    public void draw(NidavelliranForgeRecipe recipe,
                     IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        Component note = Component.literal("The Infinity Stone is not consumed during this process.");

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int maxWidth = background.getWidth();
        List<FormattedCharSequence> lines = font.split(note, maxWidth - 2); // small margin

        int lineH = font.lineHeight;
        int totalH = lines.size() * lineH;
        int startX = 1;
        int startY = background.getHeight() - totalH - 1 + 36; // lowered by 20 pixels

        if (startY < 0) startY = 0;

        for (int i = 0; i < lines.size(); i++) {
            guiGraphics.drawString(font, lines.get(i), startX, startY + i * lineH, 0x555555, false);
        }
    }

    @Override
    public RecipeType<NidavelliranForgeRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.egm.nidavellirian_forge");
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
    public void setRecipe(IRecipeLayoutBuilder builder, NidavelliranForgeRecipe recipe, IFocusGroup focuses) {
        // Input slot positions (relative to background)
        builder.addSlot(RecipeIngredientRole.INPUT, 22, 6)   // Left slot - pottery sherd
                .addItemStack(recipe.getPotterySherd());
        
        builder.addSlot(RecipeIngredientRole.INPUT, 60, 6)  // Right slot - other item
                .addItemStack(recipe.getOtherItem());

        builder.addSlot(RecipeIngredientRole.CATALYST, 41, 41)  // Bottom slot - stone (catalyst, not consumed)
                .addItemStack(recipe.getStone());
        
        // Output slot
        builder.addSlot(RecipeIngredientRole.OUTPUT, 117, 23)  // Output position
                .addItemStack(recipe.getResult());
    }
}