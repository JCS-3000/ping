package org.jcs.egm.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jcs.egm.egm;
import org.jcs.egm.client.NidavelliranForgeScreen;
import org.jcs.egm.registry.ModBlocks;
import org.jcs.egm.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class EGMJEIPlugin implements IModPlugin {
    
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(egm.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
            new NidavelliranForgeRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
            new AnvilCraftingRecipeCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Nidavellirian Forge Recipes
        List<NidavelliranForgeRecipe> forgeRecipes = new ArrayList<>();
        
        // Power Stone Holder (Kree Warhammer): Danger Pottery Sherd + Netherite Axe + Power Stone
        forgeRecipes.add(new NidavelliranForgeRecipe(
            new ItemStack(Items.DANGER_POTTERY_SHERD),
            new ItemStack(Items.NETHERITE_AXE),
            new ItemStack(ModItems.POWER_STONE.get()),
            new ItemStack(ModItems.POWER_STONE_HOLDER.get())
        ));
        
        // Soul Stone Holder (Red Skull): Heart Pottery Sherd + Wither Skeleton Skull + Soul Stone
        forgeRecipes.add(new NidavelliranForgeRecipe(
            new ItemStack(Items.HEART_POTTERY_SHERD),
            new ItemStack(Items.WITHER_SKELETON_SKULL),
            new ItemStack(ModItems.SOUL_STONE.get()),
            new ItemStack(ModItems.SOUL_STONE_HOLDER.get())
        ));
        
        // Mind Stone Holder (Lokian Scepter): Prize Pottery Sherd + Enchanted Golden Apple + Mind Stone
        forgeRecipes.add(new NidavelliranForgeRecipe(
            new ItemStack(Items.PRIZE_POTTERY_SHERD),
            new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),
            new ItemStack(ModItems.MIND_STONE.get()),
            new ItemStack(ModItems.MIND_STONE_HOLDER.get())
        ));
        
        // Time Stone Holder (Eye of Agamotto): Mourner Pottery Sherd + Verdant Froglight + Time Stone
        forgeRecipes.add(new NidavelliranForgeRecipe(
            new ItemStack(Items.MOURNER_POTTERY_SHERD),
            new ItemStack(Items.VERDANT_FROGLIGHT),
            new ItemStack(ModItems.TIME_STONE.get()),
            new ItemStack(ModItems.TIME_STONE_HOLDER.get())
        ));
        
        // Space Stone Holder (Tesseract): Explorer Pottery Sherd + Wild Armor Trim Smithing Template + Space Stone
        forgeRecipes.add(new NidavelliranForgeRecipe(
            new ItemStack(Items.EXPLORER_POTTERY_SHERD),
            new ItemStack(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE),
            new ItemStack(ModItems.SPACE_STONE.get()),
            new ItemStack(ModItems.SPACE_STONE_HOLDER.get())
        ));
        
        // Reality Stone Holder (Aether): Plenty Pottery Sherd + Music Disc 11 + Reality Stone
        forgeRecipes.add(new NidavelliranForgeRecipe(
            new ItemStack(Items.PLENTY_POTTERY_SHERD),
            new ItemStack(Items.MUSIC_DISC_11),
            new ItemStack(ModItems.REALITY_STONE.get()),
            new ItemStack(ModItems.REALITY_STONE_HOLDER.get())
        ));
        
        registration.addRecipes(NidavelliranForgeRecipeCategory.RECIPE_TYPE, forgeRecipes);
        
        // Anvil Crafting Recipes
        List<AnvilCraftingRecipe> anvilRecipes = new ArrayList<>();
        
        // Nidavellirian Forge: Anvil + Nether Star = Nidavellirian Forge
        anvilRecipes.add(new AnvilCraftingRecipe(
            new ItemStack(Items.ANVIL),
            new ItemStack(Items.NETHER_STAR),
            new ItemStack(ModBlocks.NIDAVELLIRIAN_FORGE.get())
        ));
        
        registration.addRecipes(AnvilCraftingRecipeCategory.RECIPE_TYPE, anvilRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.NIDAVELLIRIAN_FORGE.get()), 
                                      NidavelliranForgeRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), 
                                      AnvilCraftingRecipeCategory.RECIPE_TYPE);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(NidavelliranForgeScreen.class, 76, 30, 20, 30, 
                                       NidavelliranForgeRecipeCategory.RECIPE_TYPE);
    }
}