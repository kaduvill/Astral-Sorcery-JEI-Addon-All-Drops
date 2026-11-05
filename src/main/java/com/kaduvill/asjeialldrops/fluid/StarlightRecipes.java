package com.kaduvill.asjeialldrops.fluid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** Immutable, registration-time descriptions of AS's world-fluid interactions. */
public final class StarlightRecipes {
    private StarlightRecipes() {}

    public enum Kind {
        INFUSED_WOOD("infused_wood"), CELESTIAL_CLUSTER("celestial_cluster"),
        GEM_CLUSTER("gem_cluster"), CRYSTAL_GROWTH("crystal_growth"),
        CRYSTAL_REPAIR("crystal_repair"), CRYSTAL_SPLIT("crystal_split"),
        TOOL_GROWTH("tool_growth"), COLD_CONTACT("cold_contact"), HOT_CONTACT("hot_contact");

        public final String key;
        Kind(String key) { this.key = key; }
    }

    public static final class Hint {
        public final String key;
        private final Object[] args;
        public Hint(String key, Object... args) {
            this.key = key;
            this.args = args.clone();
        }
        public Object[] getArgs() { return args.clone(); }
    }

    public static final class Output {
        private final ItemStack stack;
        public final double probability;
        public final boolean exampleProperties;
        public final List<Hint> hints;

        public Output(ItemStack stack, double probability, boolean exampleProperties, List<Hint> hints) {
            if (stack == null || stack.isEmpty() || !(probability > 0) || probability > 1)
                throw new IllegalArgumentException("Invalid starlight output");
            this.stack = stack.copy();
            this.probability = probability;
            this.exampleProperties = exampleProperties;
            this.hints = immutable(hints);
        }
        public ItemStack getOutput() { return stack.copy(); }
    }

    /** One complete input/output pairing; its outputs must always be displayed together. */
    public static final class Variant {
        private final ItemStack input;
        public final List<Output> outputs;

        private Variant(ItemStack input, List<Output> outputs) {
            this.input = input.copy();
            this.outputs = immutable(outputs);
        }
        public ItemStack getInput() { return input.copy(); }
    }

    public static final class Recipe {
        public final Kind kind;
        public final List<Hint> arrowHints;
        public final List<Output> outputs;
        private final List<List<ItemStack>> itemInputs;
        private final List<List<FluidStack>> fluidInputs;
        private final List<List<Hint>> itemHints;
        private final List<List<Hint>> fluidHints;
        private final List<Variant> variants;

        public Recipe(Kind kind, List<List<ItemStack>> items, List<List<FluidStack>> fluids,
                List<List<Hint>> itemHints, List<List<Hint>> fluidHints,
                List<Output> outputs, List<Hint> arrowHints) {
            this(kind, items, fluids, itemHints, fluidHints, outputs, arrowHints, Collections.emptyList());
        }

        private Recipe(Kind kind, List<List<ItemStack>> items, List<List<FluidStack>> fluids,
                List<List<Hint>> itemHints, List<List<Hint>> fluidHints,
                List<Output> outputs, List<Hint> arrowHints, List<Variant> variants) {
            if (kind == null || items.size() > 2 || fluids.isEmpty() || fluids.size() > 2
                    || (fluids.size() == 2 && !items.isEmpty())
                    || outputs.isEmpty() || outputs.size() > 2
                    || itemHints.size() != items.size() || fluidHints.size() != fluids.size())
                throw new IllegalArgumentException("Invalid starlight recipe shape");
            this.kind = kind;
            this.itemInputs = copyItems(items);
            this.fluidInputs = copyFluids(fluids);
            this.itemHints = copyHints(itemHints);
            this.fluidHints = copyHints(fluidHints);
            this.outputs = immutable(outputs);
            this.arrowHints = immutable(arrowHints);
            this.variants = immutable(variants);
        }

        public static Recipe grouped(List<Recipe> recipes) {
            if (recipes.isEmpty()) throw new IllegalArgumentException("Empty starlight group");
            Recipe first = recipes.get(0);
            List<ItemStack> inputs = new ArrayList<>(recipes.size());
            List<Variant> variants = new ArrayList<>(recipes.size());
            for (Recipe recipe : recipes) {
                if (recipe.kind != first.kind || recipe.itemInputs.size() != 1
                        || recipe.itemInputs.get(0).size() != 1 || recipe.outputs.size() != first.outputs.size()
                        || !recipe.variants.isEmpty()) throw new IllegalArgumentException("Incompatible starlight variants");
                ItemStack input = recipe.itemInputs.get(0).get(0);
                inputs.add(input);
                variants.add(new Variant(input, recipe.outputs));
            }
            return new Recipe(first.kind, Collections.singletonList(inputs), first.fluidInputs,
                    first.itemHints, first.fluidHints, first.outputs, first.arrowHints, variants);
        }

        public List<List<ItemStack>> getItemInputs() { return copyItems(itemInputs); }
        public List<List<FluidStack>> getFluidInputs() { return copyFluids(fluidInputs); }
        public List<Hint> getItemHints(int index) { return itemHints.get(index); }
        public List<Hint> getFluidHints(int index) { return fluidHints.get(index); }
        public List<Variant> getVariants() { return variants; }
    }

    private static <T> List<T> immutable(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static List<List<Hint>> copyHints(List<List<Hint>> groups) {
        List<List<Hint>> result = new ArrayList<>(groups.size());
        for (List<Hint> group : groups) result.add(immutable(group));
        return immutable(result);
    }

    private static List<List<ItemStack>> copyItems(List<List<ItemStack>> groups) {
        List<List<ItemStack>> result = new ArrayList<>(groups.size());
        for (List<ItemStack> group : groups) {
            if (group.isEmpty()) throw new IllegalArgumentException("Empty starlight item alternatives");
            List<ItemStack> copies = new ArrayList<>(group.size());
            for (ItemStack stack : group) {
                if (stack == null || stack.isEmpty()) throw new IllegalArgumentException("Empty starlight input");
                copies.add(stack.copy());
            }
            result.add(immutable(copies));
        }
        return immutable(result);
    }

    private static List<List<FluidStack>> copyFluids(List<List<FluidStack>> groups) {
        List<List<FluidStack>> result = new ArrayList<>(groups.size());
        for (List<FluidStack> group : groups) {
            if (group.isEmpty()) throw new IllegalArgumentException("Empty starlight fluid alternatives");
            List<FluidStack> copies = new ArrayList<>(group.size());
            for (FluidStack stack : group) {
                if (stack == null || stack.getFluid() == null || stack.amount <= 0)
                    throw new IllegalArgumentException("Invalid starlight fluid input");
                copies.add(stack.copy());
            }
            result.add(immutable(copies));
        }
        return immutable(result);
    }
}

