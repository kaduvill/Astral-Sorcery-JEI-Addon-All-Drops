package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import com.kaduvill.asjeialldrops.config.ASJeiAllDropsConfig;
import com.kaduvill.asjeialldrops.fluid.AstralStarlightReader;
import com.kaduvill.asjeialldrops.fluid.StarlightRecipes;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Builds the fluid-interaction snapshot only while JEI registers recipes. */
@JEIPlugin
public final class StarlightJeiPlugin implements IModPlugin {
    private StarlightRecipeCategory category;

    @Override
    public void registerItemSubtypes(ISubtypeRegistry registry) {
        if (!ASJeiAllDropsConfig.liquidStarlightInteractions) return;
        RitualItems.registerSubtypes(registry);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        category = null;
        if (!ASJeiAllDropsConfig.liquidStarlightInteractions) return;
        category = new StarlightRecipeCategory(registry.getJeiHelpers().getGuiHelper());
        registry.addRecipeCategories(category);
    }

    @Override
    public void register(IModRegistry registry) {
        if (category == null) return;
        try {
            List<StarlightRecipes.Recipe> snapshot = new AstralStarlightReader().read(
                    registry.getIngredientRegistry().getAllIngredients(VanillaTypes.ITEM));
            if (snapshot.isEmpty()) return;
            List<StarlightRecipeWrapper> recipes = new ArrayList<>(snapshot.size());
            for (StarlightRecipes.Recipe recipe : snapshot) recipes.add(new StarlightRecipeWrapper(recipe));
            category.setRenderers(registry.getIngredientRegistry().getIngredientRenderer(VanillaTypes.FLUID),
                    registry.getIngredientRegistry().getIngredientRenderer(VanillaTypes.ITEM),
                    registry.getIngredientRegistry().getIngredientHelper(VanillaTypes.ITEM));
            registry.addRecipes(recipes, StarlightRecipeCategory.UID);
            Item journal = Item.getByNameOrId("astralsorcery:itemjournal");
            if (journal != null) {
                registry.addRecipeCatalyst(new ItemStack(journal), StarlightRecipeCategory.UID);
            }
        } catch (RuntimeException | LinkageError failure) {
            ASJeiAllDrops.LOGGER.warn("Liquid Starlight JEI pages disabled: cannot read the installed AS interactions", failure);
        }
    }
}
