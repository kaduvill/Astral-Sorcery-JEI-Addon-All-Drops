package com.kaduvill.asjeialldrops.jei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeRegistryPlugin;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;

/** Lookup-only mob aliases, following the same contract as ClusterRecipeLookup. */
final class MobRitualRecipeLookup implements IRecipeRegistryPlugin {
    private final String uid;
    private final List<MobRitualRecipeWrapper> recipes;

    MobRitualRecipeLookup(String uid, List<RitualRecipeWrapper> pages) {
        this.uid = uid;
        List<MobRitualRecipeWrapper> mobs = new ArrayList<>();
        for (RitualRecipeWrapper page : pages) if (page instanceof MobRitualRecipeWrapper)
            mobs.add((MobRitualRecipeWrapper) page);
        recipes = Collections.unmodifiableList(mobs);
    }

    private List<MobRitualRecipeWrapper> find(IFocus<?> focus) {
        if (focus == null || !(focus.getValue() instanceof ItemStack)) return Collections.emptyList();
        List<MobRitualRecipeWrapper> result = new ArrayList<>();
        for (MobRitualRecipeWrapper recipe : recipes)
            if (recipe.matchesEggAlias(focus.getMode(), (ItemStack) focus.getValue())) result.add(recipe);
        return result;
    }

    @Override public <V> List<String> getRecipeCategoryUids(IFocus<V> focus) {
        return find(focus).isEmpty() ? Collections.emptyList() : Collections.singletonList(uid);
    }

    @Override @SuppressWarnings("unchecked")
    public <T extends IRecipeWrapper, V> List<T> getRecipeWrappers(IRecipeCategory<T> category, IFocus<V> focus) {
        return category != null && uid.equals(category.getUid()) ? (List<T>) (List<?>) find(focus) : Collections.emptyList();
    }

    @Override public <T extends IRecipeWrapper> List<T> getRecipeWrappers(IRecipeCategory<T> category) {
        return Collections.emptyList();
    }
}
