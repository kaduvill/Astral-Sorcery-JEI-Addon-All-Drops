package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import com.kaduvill.asjeialldrops.cluster.AstralClusterReader;
import com.kaduvill.asjeialldrops.cluster.ClusterRecipes;
import com.kaduvill.asjeialldrops.config.ASJeiAllDropsConfig;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Existing placed clusters remain usable independently of the liquid-creation switches. */
@JEIPlugin
public final class ClusterJeiPlugin implements IModPlugin {
    private ClusterRecipeCategory category;

    @Override public void registerCategories(IRecipeCategoryRegistration registry) {
        category = null;
        if (!ASJeiAllDropsConfig.crystalClusterGrowth) return;
        category = new ClusterRecipeCategory(registry.getJeiHelpers().getGuiHelper());
        registry.addRecipeCategories(category);
    }

    @Override public void register(IModRegistry registry) {
        if (category == null) return;
        try {
            List<ClusterRecipes.Recipe> snapshot = new AstralClusterReader().read();
            if (snapshot.isEmpty()) return;
            List<ClusterRecipeWrapper> recipes = new ArrayList<>(snapshot.size());
            for (ClusterRecipes.Recipe recipe : snapshot) recipes.add(new ClusterRecipeWrapper(recipe));
            category.setRenderer(registry.getIngredientRegistry().getIngredientRenderer(VanillaTypes.ITEM));
            registry.addRecipes(recipes, ClusterRecipeCategory.UID);
            registry.addRecipeRegistryPlugin(new ClusterRecipeLookup(snapshot, recipes));
            Item journal = Item.getByNameOrId("astralsorcery:itemjournal");
            if (journal != null) registry.addRecipeCatalyst(new ItemStack(journal), ClusterRecipeCategory.UID);
        } catch (RuntimeException | LinkageError failure) {
            ASJeiAllDrops.LOGGER.warn("Crystal Cluster JEI pages disabled: cannot describe the installed AS clusters", failure);
        }
    }
}
