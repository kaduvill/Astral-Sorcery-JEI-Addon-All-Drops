package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.cluster.ClusterRecipes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeRegistryPlugin;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Lookup-only stage aliases: clusters never become harvested outputs or transfer ingredients. */
final class ClusterRecipeLookup implements IRecipeRegistryPlugin {
    private final Map<Item, Map<Integer, List<ClusterRecipeWrapper>>> usages;
    private final Map<Item, Map<Integer, List<ClusterRecipeWrapper>>> recipes;

    ClusterRecipeLookup(List<ClusterRecipes.Recipe> snapshot, List<ClusterRecipeWrapper> wrappers) {
        Map<Item, Map<Integer, List<ClusterRecipeWrapper>>> usages = new HashMap<>();
        Map<Item, Map<Integer, List<ClusterRecipeWrapper>>> recipes = new HashMap<>();
        List<ClusterRecipeWrapper> gems = new ArrayList<>(3);
        for (int i = 0; i < snapshot.size(); i++) {
            if (!snapshot.get(i).condition.isCelestial()) gems.add(wrappers.get(i));
        }
        for (int i = 0; i < snapshot.size(); i++) {
            ClusterRecipes.Recipe recipe = snapshot.get(i);
            ClusterRecipeWrapper wrapper = wrappers.get(i);
            for (ClusterRecipes.Stage stage : recipe.stages) {
                ItemStack stack = stage.getStack();
                if (stage.growth > 0) add(recipes, stack, wrapper);
                if (!recipe.condition.isCelestial() && stage.isMature()) {
                    // The matching mature recipe is already indexed as a normal usage.
                    for (ClusterRecipeWrapper other : gems) if (other != wrapper) add(usages, stack, other);
                }
            }
        }
        this.usages = freeze(usages);
        this.recipes = freeze(recipes);
    }

    @Override public <V> List<String> getRecipeCategoryUids(IFocus<V> focus) {
        return find(focus).isEmpty() ? Collections.emptyList() : Collections.singletonList(ClusterRecipeCategory.UID);
    }

    @SuppressWarnings("unchecked")
    @Override public <T extends IRecipeWrapper, V> List<T> getRecipeWrappers(IRecipeCategory<T> category, IFocus<V> focus) {
        if (!(category instanceof ClusterRecipeCategory)) return Collections.emptyList();
        return (List<T>) find(focus);
    }

    @Override public <T extends IRecipeWrapper> List<T> getRecipeWrappers(IRecipeCategory<T> category) {
        // Normal registration owns unfiltered results; returning them again would duplicate pages.
        return Collections.emptyList();
    }

    private List<ClusterRecipeWrapper> find(IFocus<?> focus) {
        if (focus == null || !(focus.getValue() instanceof ItemStack)) return Collections.emptyList();
        ItemStack stack = (ItemStack) focus.getValue();
        if (stack.isEmpty()) return Collections.emptyList();
        Map<Item, Map<Integer, List<ClusterRecipeWrapper>>> routes;
        if (focus.getMode() == IFocus.Mode.INPUT) routes = usages;
        else if (focus.getMode() == IFocus.Mode.OUTPUT) routes = recipes;
        else return Collections.emptyList();
        Map<Integer, List<ClusterRecipeWrapper>> stages = routes.get(stack.getItem());
        if (stages == null) return Collections.emptyList();
        List<ClusterRecipeWrapper> result = stages.get(stack.getMetadata());
        return result == null ? Collections.emptyList() : result;
    }

    private static void add(Map<Item, Map<Integer, List<ClusterRecipeWrapper>>> routes,
                            ItemStack stack, ClusterRecipeWrapper wrapper) {
        List<ClusterRecipeWrapper> matches = routes.computeIfAbsent(stack.getItem(), key -> new HashMap<>())
                .computeIfAbsent(stack.getMetadata(), key -> new ArrayList<>());
        if (!matches.contains(wrapper)) matches.add(wrapper);
    }

    private static Map<Item, Map<Integer, List<ClusterRecipeWrapper>>> freeze(
            Map<Item, Map<Integer, List<ClusterRecipeWrapper>>> routes) {
        Map<Item, Map<Integer, List<ClusterRecipeWrapper>>> result = new HashMap<>();
        routes.forEach((item, stages) -> {
            Map<Integer, List<ClusterRecipeWrapper>> copies = new HashMap<>();
            stages.forEach((meta, wrappers) -> copies.put(meta, Collections.unmodifiableList(new ArrayList<>(wrappers))));
            result.put(item, Collections.unmodifiableMap(copies));
        });
        return Collections.unmodifiableMap(result);
    }
}
