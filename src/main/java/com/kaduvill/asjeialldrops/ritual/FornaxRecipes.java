package com.kaduvill.asjeialldrops.ritual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mezz.jei.api.recipe.IStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/** Read-only in-world conversions. Empty endpoints describe world conditions, never fake items. */
public final class FornaxRecipes {
    private FornaxRecipes() {}

    public enum Output { BLOCK, FLUID, DROP, AIR, TEMPERATURE_RULE }

    /** JEI's item identity can differ from the stack AS reconstructs for furnace lookup. */
    static final class Input {
        private final ItemStack display;
        private final ItemStack furnace;

        Input(ItemStack display, ItemStack furnace) {
            this.display = display.copy();
            this.furnace = furnace.copy();
        }

        boolean matches(ItemStack display, ItemStack furnace) {
            return ItemStack.areItemsEqual(this.display, display) && ItemStack.areItemsEqual(this.furnace, furnace);
        }
    }

    public static final class Recipe {
        public final boolean corrupted;
        public final boolean builtIn;
        public final Output kind;
        public final String worldInput;
        private final List<Input> inputs;
        private final List<FluidStack> fluidInputs;
        private final ItemStack output;
        private final FluidStack fluidOutput;
        private final ItemStack furnaceOutput;

        Recipe(boolean corrupted, boolean builtIn, Output kind, String worldInput,
               List<Input> inputs, List<FluidStack> fluidInputs, ItemStack output,
               FluidStack fluidOutput, ItemStack furnaceOutput) {
            this.corrupted = corrupted;
            this.builtIn = builtIn;
            this.kind = kind;
            this.worldInput = worldInput;
            this.inputs = copyInputs(inputs);
            this.fluidInputs = copyFluids(fluidInputs);
            this.output = output.copy();
            this.fluidOutput = fluidOutput == null ? null : fluidOutput.copy();
            this.furnaceOutput = furnaceOutput.copy();
        }

        public List<ItemStack> inputs() {
            List<ItemStack> copies = new ArrayList<>();
            Map<Item, Set<Integer>> seen = new IdentityHashMap<>();
            for (Input input : inputs) {
                if (seen.computeIfAbsent(input.display.getItem(), item -> new HashSet<>())
                        .add(input.display.getMetadata())) copies.add(input.display.copy());
            }
            return Collections.unmodifiableList(copies);
        }
        public List<FluidStack> fluidInputs() { return copyFluids(fluidInputs); }
        public ItemStack output() { return output.copy(); }
        public FluidStack fluidOutput() { return fluidOutput == null ? null : fluidOutput.copy(); }

        public boolean acceptsInput(ItemStack searched) {
            if (searched.isEmpty()) return false;
            for (Input input : inputs) {
                if (!ItemStack.areItemsEqual(input.display, searched)) continue;
                // AS reconstructs the stack from the world block, so input NBT is immaterial.
                // Query AS's reconstructed stack, not the normalized JEI display identity.
                if (builtIn || corrupted) return true;
                try {
                    if (ItemStack.areItemStacksEqual(furnaceOutput,
                            FurnaceRecipes.instance().getSmeltingResult(input.furnace))) return true;
                } catch (RuntimeException | LinkageError failure) {
                    // Another state with the same displayed item may still resolve successfully.
                }
            }
            return false;
        }

        public boolean acceptsFluid(FluidStack searched) {
            if (searched == null) return false;
            for (FluidStack input : fluidInputs) if (input.isFluidEqual(searched)) return true;
            return false;
        }
    }

    public static final class Snapshot {
        public final double range;
        public final double failChance;
        public final List<Recipe> recipes;
        private final Map<Item, List<Recipe>> byInput = new IdentityHashMap<>();
        private final Map<Item, List<Recipe>> byOutput = new IdentityHashMap<>();
        private final Map<Fluid, List<Recipe>> byFluidInput = new IdentityHashMap<>();
        private final Map<Fluid, List<Recipe>> byFluidOutput = new IdentityHashMap<>();

        Snapshot(double range, double failChance, List<Recipe> recipes) {
            this.range = range;
            this.failChance = failChance;
            this.recipes = Collections.unmodifiableList(new ArrayList<>(recipes));
            for (Recipe recipe : recipes) {
                for (Input input : recipe.inputs) add(byInput, input.display.getItem(), recipe);
                if (!recipe.output.isEmpty()) add(byOutput, recipe.output.getItem(), recipe);
                for (FluidStack input : recipe.fluidInputs) add(byFluidInput, input.getFluid(), recipe);
                if (recipe.fluidOutput != null) add(byFluidOutput, recipe.fluidOutput.getFluid(), recipe);
            }
        }

        public List<Recipe> findInputs(ItemStack input) {
            List<Recipe> result = new ArrayList<>();
            for (Recipe recipe : byInput.getOrDefault(input.getItem(), Collections.emptyList()))
                if (recipe.acceptsInput(input)) result.add(recipe);
            return result;
        }

        public List<Recipe> findOutputs(ItemStack output, IStackHelper helper) {
            List<Recipe> result = new ArrayList<>();
            for (Recipe recipe : byOutput.getOrDefault(output.getItem(), Collections.emptyList()))
                if (helper.isEquivalent(recipe.output, output)) result.add(recipe);
            return result;
        }

        public List<Recipe> findFluids(FluidStack searched, boolean input) {
            List<Recipe> result = new ArrayList<>();
            Map<Fluid, List<Recipe>> index = input ? byFluidInput : byFluidOutput;
            for (Recipe recipe : index.getOrDefault(searched.getFluid(), Collections.emptyList())) {
                if (input ? recipe.acceptsFluid(searched) : recipe.fluidOutput.isFluidEqual(searched))
                    result.add(recipe);
            }
            return result;
        }

        private static <K> void add(Map<K, List<Recipe>> map, K key, Recipe recipe) {
            List<Recipe> values = map.computeIfAbsent(key, ignored -> new ArrayList<>());
            if (!values.contains(recipe)) values.add(recipe);
        }
    }

    /** Inclusive thresholds from CEffectFornax; kept separate for boundary verification. */
    public static OutputTemperature temperatureBand(int temperature) {
        return temperature <= 200 ? OutputTemperature.PACKED_ICE
                : temperature >= 500 ? OutputTemperature.OBSIDIAN : OutputTemperature.STONE;
    }

    public enum OutputTemperature { PACKED_ICE, STONE, OBSIDIAN }

    private static List<Input> copyInputs(List<Input> source) {
        List<Input> copies = new ArrayList<>(source.size());
        for (Input input : source) copies.add(new Input(input.display, input.furnace));
        return Collections.unmodifiableList(copies);
    }

    private static List<FluidStack> copyFluids(List<FluidStack> source) {
        List<FluidStack> copies = new ArrayList<>(source.size());
        for (FluidStack fluid : source) copies.add(fluid.copy());
        return Collections.unmodifiableList(copies);
    }
}
