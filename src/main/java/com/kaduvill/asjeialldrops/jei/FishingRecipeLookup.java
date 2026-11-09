package com.kaduvill.asjeialldrops.jei;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeRegistryPlugin;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;

/** Fallback aliases for random variants missing from JEI's normal output index. */
final class FishingRecipeLookup implements IRecipeRegistryPlugin {
    private static final String UID = "asjeialldrops.octans";
    private final FishingRecipeWrapper recipe;
    private final IIngredientHelper<ItemStack> ingredientHelper;
    private final Set<String> indexedOutputs = new HashSet<>();

    FishingRecipeLookup(FishingRecipeWrapper recipe, IIngredientHelper<ItemStack> ingredientHelper) {
        this.recipe = recipe;
        this.ingredientHelper = ingredientHelper;
        for (List<ItemStack> outputs : recipe.outputLists()) {
            for (ItemStack output : ingredientHelper.expandSubtypes(outputs)) {
                if (output != null && !output.isEmpty()) indexedOutputs.add(ingredientHelper.getUniqueId(output));
            }
        }
    }

    private boolean matches(IFocus<?> focus) {
        if (focus == null || focus.getMode() != IFocus.Mode.OUTPUT || !(focus.getValue() instanceof ItemStack))
            return false;
        ItemStack stack = (ItemStack) focus.getValue();
        return !stack.isEmpty()
                && !indexedOutputs.contains(ingredientHelper.getUniqueId(stack))
                && !indexedOutputs.contains(ingredientHelper.getWildcardId(stack))
                && recipe.findOutput(stack) >= 0;
    }

    @Override public <V> List<String> getRecipeCategoryUids(IFocus<V> focus) {
        return matches(focus) ? Collections.singletonList(UID) : Collections.emptyList();
    }

    @Override @SuppressWarnings("unchecked")
    public <T extends IRecipeWrapper, V> List<T> getRecipeWrappers(IRecipeCategory<T> category, IFocus<V> focus) {
        return UID.equals(category.getUid()) && matches(focus)
                ? (List<T>) (List<?>) Collections.singletonList(recipe) : Collections.emptyList();
    }

    @Override public <T extends IRecipeWrapper> List<T> getRecipeWrappers(IRecipeCategory<T> category) {
        // Normal registration owns the unfiltered list, including the corrupted explanation.
        return Collections.emptyList();
    }
}
