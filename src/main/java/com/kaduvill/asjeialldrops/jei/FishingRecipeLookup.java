package com.kaduvill.asjeialldrops.jei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeRegistryPlugin;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/** Sole Octans provider, avoiding native exact/wildcard duplicate pages. */
final class FishingRecipeLookup implements IRecipeRegistryPlugin {
    private static final String UID = "asjeialldrops.octans";
    private final FishingRecipeWrapper recipe;
    private final List<RitualRecipeWrapper> pages;
    private final IIngredientHelper<ItemStack> ingredientHelper;
    private final List<ItemStack> catalysts = new ArrayList<>();

    FishingRecipeLookup(FishingRecipeWrapper recipe, List<RitualRecipeWrapper> pages,
                        IIngredientHelper<ItemStack> ingredientHelper, List<ItemStack> catalysts) {
        this.recipe = recipe;
        this.pages = Collections.unmodifiableList(new ArrayList<>(pages));
        this.ingredientHelper = ingredientHelper;
        for (ItemStack catalyst : catalysts) if (!catalyst.isEmpty()) this.catalysts.add(catalyst.copy());
        recipe.indexOutputs(ingredientHelper);
    }

    private List<RitualRecipeWrapper> find(IFocus<?> focus) {
        if (focus == null) return Collections.emptyList();
        if (focus.getValue() instanceof ItemStack) {
            ItemStack stack = (ItemStack) focus.getValue();
            if (stack.isEmpty()) return Collections.emptyList();
            if (focus.getMode() == IFocus.Mode.INPUT && ingredientHelper.getMatch(catalysts, stack) != null)
                return pages;
            if (focus.getMode() == IFocus.Mode.OUTPUT && recipe.findOutput(stack) >= 0)
                return Collections.singletonList(recipe);
        } else if (focus.getMode() == IFocus.Mode.INPUT && focus.getValue() instanceof FluidStack
                && ((FluidStack) focus.getValue()).getFluid() == FluidRegistry.WATER) {
            return Collections.singletonList(recipe);
        }
        return Collections.emptyList();
    }

    @Override public <V> List<String> getRecipeCategoryUids(IFocus<V> focus) {
        return find(focus).isEmpty() ? Collections.emptyList() : Collections.singletonList(UID);
    }

    @Override @SuppressWarnings("unchecked")
    public <T extends IRecipeWrapper, V> List<T> getRecipeWrappers(IRecipeCategory<T> category, IFocus<V> focus) {
        return UID.equals(category.getUid()) ? (List<T>) (List<?>) find(focus) : Collections.emptyList();
    }

    @Override @SuppressWarnings("unchecked")
    public <T extends IRecipeWrapper> List<T> getRecipeWrappers(IRecipeCategory<T> category) {
        return UID.equals(category.getUid()) ? (List<T>) (List<?>) pages : Collections.emptyList();
    }
}
