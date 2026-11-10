package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ritual.FornaxRecipes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeRegistryPlugin;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.IStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** Sole Fornax provider: native input resolution, reverse outputs, and category catalysts. */
final class FornaxRecipeLookup implements IRecipeRegistryPlugin {
    private static final String UID = "asjeialldrops.fornax";
    private final FornaxRecipes.Snapshot snapshot;
    private final List<RitualRecipeWrapper> pages;
    private final Map<FornaxRecipes.Recipe, FornaxRecipeWrapper> wrappers = new IdentityHashMap<>();
    private final IStackHelper helper;
    private final List<ItemStack> catalysts;

    FornaxRecipeLookup(FornaxRecipes.Snapshot snapshot, List<? extends RitualRecipeWrapper> pages,
                      IStackHelper helper, List<ItemStack> catalysts) {
        this.snapshot = snapshot;
        this.pages = Collections.unmodifiableList(new ArrayList<>(pages));
        this.helper = helper;
        this.catalysts = new ArrayList<>();
        for (ItemStack catalyst : catalysts) if (!catalyst.isEmpty()) this.catalysts.add(catalyst.copy());
        for (RitualRecipeWrapper page : pages) if (page instanceof FornaxRecipeWrapper) {
            FornaxRecipeWrapper wrapper = (FornaxRecipeWrapper) page;
            wrappers.put(wrapper.recipe, wrapper);
        }
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

    private List<RitualRecipeWrapper> find(IFocus<?> focus) {
        if (focus == null) return Collections.emptyList();
        boolean input = focus.getMode() == IFocus.Mode.INPUT;
        if (!input && focus.getMode() != IFocus.Mode.OUTPUT) return Collections.emptyList();
        List<FornaxRecipes.Recipe> matches;
        if (focus.getValue() instanceof ItemStack) {
            ItemStack stack = (ItemStack) focus.getValue();
            if (stack.isEmpty()) return Collections.emptyList();
            if (input) for (ItemStack catalyst : catalysts) if (helper.isEquivalent(catalyst, stack)) return pages;
            matches = input ? snapshot.findInputs(stack) : snapshot.findOutputs(stack, helper);
        } else if (focus.getValue() instanceof FluidStack) {
            matches = snapshot.findFluids((FluidStack) focus.getValue(), input);
        } else return Collections.emptyList();
        List<RitualRecipeWrapper> result = new ArrayList<>();
        for (FornaxRecipes.Recipe recipe : matches) {
            FornaxRecipeWrapper wrapper = wrappers.get(recipe);
            if (wrapper != null) result.add(wrapper);
        }
        return result;
    }
}
