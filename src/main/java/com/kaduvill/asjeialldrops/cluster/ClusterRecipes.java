package com.kaduvill.asjeialldrops.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;

/** Immutable descriptions of placed-cluster growth and mature harvesting. */
public final class ClusterRecipes {
    private ClusterRecipes() {}

    public enum Condition {
        DAYLIGHT("daylight"), TWILIGHT("twilight"), NIGHT("night"),
        ORDINARY("ordinary"), STARMETAL("starmetal");

        public final String key;
        Condition(String key) { this.key = key; }
        public boolean isCelestial() { return this == ORDINARY || this == STARMETAL; }
    }

    public static final class Output {
        private final ItemStack stack;
        public final double extraChance;
        public final List<String> hints;

        public Output(ItemStack stack, double extraChance, List<String> hints) {
            if (stack == null || stack.isEmpty() || !(extraChance >= 0) || extraChance > 1) {
                throw new IllegalArgumentException("Invalid cluster output");
            }
            this.stack = stack.copy();
            this.extraChance = extraChance;
            this.hints = immutable(hints);
        }

        public ItemStack getStack() { return stack.copy(); }
    }

    /** A valid installed block state; matching never decodes arbitrary item metadata. */
    public static final class Stage {
        private final ItemStack stack;
        public final int growth;
        public final int maximum;

        public Stage(ItemStack stack, int growth, int maximum) {
            if (stack == null || stack.isEmpty() || growth < 0 || maximum <= 0 || growth > maximum) {
                throw new IllegalArgumentException("Invalid cluster stage");
            }
            this.stack = stack.copy();
            this.growth = growth;
            this.maximum = maximum;
        }

        public ItemStack getStack() { return stack.copy(); }
        public boolean isMature() { return growth == maximum; }
        public boolean matches(ItemStack value) {
            return value != null && !value.isEmpty() && value.getItem() == stack.getItem()
                    && value.getMetadata() == stack.getMetadata();
        }
    }

    public static final class Recipe {
        public final Condition condition;
        public final List<Output> outputs;
        public final List<Stage> stages;
        private final List<List<ItemStack>> inputs;

        public Recipe(Condition condition, List<Stage> stages, List<ItemStack> support, List<Output> outputs) {
            if (condition == null || stages.isEmpty()
                    || (condition == Condition.STARMETAL ? support.isEmpty() : !support.isEmpty())
                    || outputs.size() != (condition.isCelestial() ? 2 : 1)) {
                throw new IllegalArgumentException("Invalid cluster recipe shape");
            }
            this.condition = condition;
            this.stages = immutable(stages);
            List<ItemStack> clusters = new ArrayList<>(stages.size());
            for (Stage stage : stages) clusters.add(stage.getStack());
            List<List<ItemStack>> inputs = new ArrayList<>(2);
            inputs.add(clusters);
            if (!support.isEmpty()) inputs.add(support);
            this.inputs = copyInputs(inputs);
            this.outputs = immutable(outputs);
        }

        public List<List<ItemStack>> getInputs() { return copyInputs(inputs); }

        public Stage getStage(ItemStack stack) {
            for (Stage stage : stages) if (stage.matches(stack)) return stage;
            return null;
        }
    }

    private static <T> List<T> immutable(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static List<List<ItemStack>> copyInputs(List<List<ItemStack>> inputs) {
        List<List<ItemStack>> result = new ArrayList<>(inputs.size());
        for (List<ItemStack> alternatives : inputs) {
            if (alternatives.isEmpty()) throw new IllegalArgumentException("Empty cluster input");
            List<ItemStack> copies = new ArrayList<>(alternatives.size());
            for (ItemStack stack : alternatives) {
                if (stack == null || stack.isEmpty()) throw new IllegalArgumentException("Empty cluster stack");
                copies.add(stack.copy());
            }
            result.add(immutable(copies));
        }
        return immutable(result);
    }
}
