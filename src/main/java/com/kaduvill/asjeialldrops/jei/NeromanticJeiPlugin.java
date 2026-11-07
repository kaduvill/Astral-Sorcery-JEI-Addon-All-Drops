package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import com.kaduvill.asjeialldrops.config.ASJeiAllDropsConfig;
import com.kaduvill.asjeialldrops.neromantic.AstralNeromanticReader;
import com.kaduvill.asjeialldrops.neromantic.NeromanticRecipes;
import hellfirepvp.astralsorcery.common.lib.BlocksAS;
import hellfirepvp.astralsorcery.common.tile.TileBore;
import java.util.Collections;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;

@JEIPlugin
public final class NeromanticJeiPlugin implements IModPlugin {
    private NeromanticRecipeCategory category;
    private NeromanticRecipeWrapper recipe;

    @Override public void registerCategories(IRecipeCategoryRegistration registry) {
        category = null;
        recipe = null;
        if (!ASJeiAllDropsConfig.neromanticPrime) return;
        try {
            NeromanticRecipes.Snapshot snapshot = new AstralNeromanticReader().read();
            NeromanticRecipeLayout layout = new NeromanticRecipeLayout(snapshot.outputs.size());
            category = new NeromanticRecipeCategory(registry.getJeiHelpers().getGuiHelper(), layout);
            recipe = new NeromanticRecipeWrapper(snapshot, layout);
            registry.addRecipeCategories(category);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            category = null;
            recipe = null;
            ASJeiAllDrops.LOGGER.warn("Neromantic Prime JEI category disabled: cannot read the loaded AS registry", failure);
        }
    }

    @Override public void register(IModRegistry registry) {
        if (category == null || recipe == null) return;
        try {
            category.setRenderer(registry.getIngredientRegistry().getIngredientRenderer(VanillaTypes.FLUID),
                    registry.getIngredientRegistry().getIngredientHelper(VanillaTypes.FLUID));
            registry.addRecipes(Collections.singletonList(recipe), NeromanticRecipeCategory.UID);
            registry.addRecipeCatalyst(TileBore.BoreType.LIQUID.asStack(), NeromanticRecipeCategory.UID);
            if (BlocksAS.blockBore != null) registry.addRecipeCatalyst(new ItemStack(BlocksAS.blockBore), NeromanticRecipeCategory.UID);
        } catch (RuntimeException | LinkageError failure) {
            ASJeiAllDrops.LOGGER.warn("Cannot register Neromantic Prime JEI recipe", failure);
        }
    }
}
