package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import com.kaduvill.asjeialldrops.config.ASJeiAllDropsConfig;
import com.kaduvill.asjeialldrops.mineralis.AstralMineralisReader;
import com.kaduvill.asjeialldrops.mineralis.AstralMineralisReader.Snapshot;
import hellfirepvp.astralsorcery.common.lib.Constellations;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;

@JEIPlugin
public final class MineralisJeiPlugin implements IModPlugin {
    private Snapshot snapshot;
    private RitualInfoRecipeWrapper corruptedRecipe;

    @Override
    public void registerItemSubtypes(ISubtypeRegistry registry) {
        if (!ASJeiAllDropsConfig.mineralis) return;
        RitualItems.registerSubtypes(registry);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        snapshot = null;
        corruptedRecipe = null;
        if (!ASJeiAllDropsConfig.mineralis) return;
        snapshot = readSafely();
        if (ASJeiAllDropsConfig.mineralisCorrupted && snapshot.getSearchRange() > 0)
            corruptedRecipe = new RitualInfoRecipeWrapper(snapshot.getSearchRange(), "mineralis");
        MineralisRecipeCategory category = new MineralisRecipeCategory(registry.getJeiHelpers().getGuiHelper(),
                snapshot.getInputs().size(), snapshot.getDrops().size(),
                corruptedRecipe == null ? 0 : corruptedRecipe.height());
        registry.addRecipeCategories(category);
    }

    @Override
    public void register(IModRegistry registry) {
        if (snapshot == null) return;
        List<IRecipeWrapper> recipes = new ArrayList<>();
        MineralisRecipeWrapper recipe = wrap(snapshot, registry.getIngredientRegistry().getIngredientHelper(VanillaTypes.ITEM));
        if (recipe != null) recipes.add(recipe);
        if (corruptedRecipe != null) recipes.add(corruptedRecipe);
        registry.addRecipes(recipes, MineralisRecipeCategory.UID);
        RitualItems.catalysts(registry, MineralisRecipeCategory.UID, Constellations.mineralis);
    }

    private static Snapshot readSafely() {
        try {
            return new AstralMineralisReader().read();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            ASJeiAllDrops.LOGGER.warn("Mineralis JEI page disabled: cannot read the installed Astral Sorcery table", failure);
            return Snapshot.empty();
        }
    }

    private static MineralisRecipeWrapper wrap(Snapshot snapshot, IIngredientHelper<ItemStack> ingredientHelper) {
        return snapshot.getDrops().isEmpty() ? null
                : new MineralisRecipeWrapper(snapshot.getInputs(), snapshot.getDrops(),
                        snapshot.getNoResultProbability(), snapshot.getSearchRange(), ingredientHelper);
    }
}
