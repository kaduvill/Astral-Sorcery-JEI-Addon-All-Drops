package com.kaduvill.asjeialldrops.neromantic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/** Immutable registration snapshot. No AS entries, UT config objects, world or registry lists survive. */
public final class NeromanticRecipes {
    private NeromanticRecipes() {}

    public static final class Rules {
        public final boolean dimensionsAvailable;
        public final boolean reserveKnown;
        public final boolean overflowFixed, unlimited;
        public final int quantity;

        private Rules(boolean dimensionsAvailable, boolean reserveKnown, boolean overflowFixed, boolean unlimited,
                int quantity) {
            this.dimensionsAvailable = dimensionsAvailable;
            this.reserveKnown = reserveKnown;
            this.overflowFixed = overflowFixed;
            this.unlimited = unlimited;
            this.quantity = quantity;
        }

        public static Rules base() { return new Rules(false, true, false, false, 300); }

        public static Rules universalTweaks(boolean overflowFixed, boolean unlimited, int quantity) {
            return new Rules(true, true, overflowFixed, unlimited, quantity);
        }

        public static Rules unknown(boolean dimensionsAvailable, int quantity) {
            return new Rules(dimensionsAvailable, false, false, false, quantity);
        }

        // Losing dimension metadata also loses the eligible pools, so never revert to base math.
        public Rules withoutDimensions() { return unknown(false, quantity); }
    }

    public static final class Entry {
        public final Fluid fluid;
        public final int weight, guaranteed, randomExtra;
        private final int[] dimensions;

        public Entry(Fluid fluid, int weight, int guaranteed, int randomExtra, int[] dimensions) {
            this.fluid = fluid;
            this.weight = weight;
            this.guaranteed = guaranteed;
            this.randomExtra = randomExtra;
            this.dimensions = dimensions == null ? new int[0] : Arrays.stream(dimensions).distinct().sorted().toArray();
        }

        public int[] getDimensions() { return dimensions.clone(); }
        boolean allowed(Integer dimension) {
            return dimensions.length == 0 || dimension != null && Arrays.binarySearch(dimensions, dimension) >= 0;
        }
    }

    public static final class Pool {
        public final List<Integer> dimensions, entries;
        public final boolean otherDimensions;
        private final double[] shares;
        private final boolean valid;

        private Pool(List<Integer> dimensions, boolean other, List<Integer> entries, List<Entry> all) {
            this.dimensions = immutable(dimensions);
            this.otherDimensions = other;
            this.entries = immutable(entries);
            double[] weights = NeromanticProbabilities.selection(entries.stream().mapToInt(i -> all.get(i).weight).toArray());
            valid = weights.length > 0 && !Double.isNaN(weights[0]);
            shares = new double[all.size()];
            for (int i = 0; i < entries.size(); i++) shares[entries.get(i)] = weights[i];
        }

        public boolean valid() { return valid; }
        public double share(int entry) { return shares[entry]; }
    }

    public static final class Output {
        public final Fluid fluid;
        public final boolean waterFallback;
        public final List<Integer> entries;
        private final int[] dimensions;

        private Output(Fluid fluid, boolean water, List<Integer> entries, List<Entry> all) {
            this.fluid = fluid;
            this.waterFallback = water;
            this.entries = immutable(entries);
            if (water) { dimensions = new int[0]; return; }
            TreeSet<Integer> ids = new TreeSet<>();
            boolean unrestricted = false;
            for (int index : entries) {
                int[] allowed = all.get(index).dimensions;
                if (allowed.length == 0) unrestricted = true;
                for (int dimension : allowed) ids.add(dimension);
            }
            dimensions = unrestricted ? new int[0] : ids.stream().mapToInt(Integer::intValue).toArray();
        }
        /** Union of loaded entry metadata. Empty means unrestricted; water fallback ignores it. */
        public int[] getDimensions() { return dimensions.clone(); }
        /** Symbolic identity, not a fixed extraction yield. */
        public FluidStack getStack() { return new FluidStack(fluid, 1); }
    }

    public static final class Snapshot {
        public final List<Entry> entries;
        public final List<Output> outputs;
        public final List<Pool> pools;
        public final Rules rules;
        private final Fluid fuel;
        private final List<NeromanticProbabilities.Reserve> reserves;

        public Snapshot(List<Entry> entries, Fluid fuel, Fluid water, Rules rules) {
            if (fuel == null || water == null || rules == null) throw new IllegalArgumentException("Missing Neromantic fluids/rules");
            this.entries = immutable(entries);
            this.rules = rules;
            this.fuel = fuel;
            Map<Fluid, List<Integer>> grouped = new LinkedHashMap<>();
            List<Integer> waterEntries = new ArrayList<>();
            for (int i = 0; i < this.entries.size(); i++) {
                Entry entry = this.entries.get(i);
                if (entry.fluid != null && entry.fluid.equals(water)) waterEntries.add(i);
                else if (entry.fluid != null) grouped.computeIfAbsent(entry.fluid, key -> new ArrayList<>()).add(i);
            }
            List<Output> outputs = new ArrayList<>();
            grouped.forEach((fluid, indexes) -> outputs.add(new Output(fluid, false, indexes, this.entries)));
            outputs.add(new Output(water, true, waterEntries, this.entries));
            this.outputs = immutable(outputs);
            pools = rules.reserveKnown ? pools(this.entries, water, rules.dimensionsAvailable) : Collections.emptyList();
            List<NeromanticProbabilities.Reserve> reserves = new ArrayList<>();
            if (rules.reserveKnown) for (Entry entry : this.entries) {
                reserves.add(NeromanticProbabilities.reserve(entry.guaranteed, entry.randomExtra, rules.overflowFixed, rules.unlimited));
            }
            this.reserves = immutable(reserves);
        }

        public FluidStack getFuel() { return new FluidStack(fuel, 1); }
        public List<FluidStack> getOutputs() {
            List<FluidStack> result = new ArrayList<>();
            for (Output output : outputs) result.add(output.getStack());
            return result;
        }

        public NeromanticProbabilities.Reserve reserve(int entry) { return reserves.get(entry); }

        /** Modeled new usable reservoirs, under AS or the intended UT rules; not surveyed chunks. */
        public double occurrence(Output output, Pool pool) {
            if (output.waterFallback || !rules.reserveKnown || !pool.valid()) return Double.NaN;
            double result = 0;
            for (int index : output.entries) {
                double share = pool.share(index);
                // An invalid amount on a ticket that cannot be selected does not poison other branches.
                if (share == 0) continue;
                result += share * reserves.get(index).positive;
            }
            return Math.min(1, result);
        }
    }

    private static List<Pool> pools(List<Entry> entries, Fluid water, boolean dimensions) {
        if (!dimensions) {
            List<Integer> all = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) all.add(i);
            return Collections.singletonList(new Pool(Collections.emptyList(), false, all, entries));
        }
        TreeSet<Integer> explicit = new TreeSet<>();
        for (Entry entry : entries) if (entry.fluid != null && !entry.fluid.equals(water)) {
            for (int id : entry.dimensions) explicit.add(id);
        }
        Map<List<Integer>, List<Integer>> grouped = new LinkedHashMap<>();
        for (Integer id : explicit) grouped.computeIfAbsent(eligible(entries, water, id), key -> new ArrayList<>()).add(id);
        List<Pool> pools = new ArrayList<>();
        grouped.forEach((tickets, ids) -> pools.add(new Pool(ids, false, tickets, entries)));
        // An ID complement is not a claim that additional dimensions exist or can be visited.
        pools.add(new Pool(Collections.emptyList(), true, eligible(entries, water, null), entries));
        return immutable(pools);
    }

    private static List<Integer> eligible(List<Entry> entries, Fluid water, Integer dimension) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (entry.fluid != null && !entry.fluid.equals(water) && entry.allowed(dimension)) result.add(i);
        }
        return result;
    }

    private static <T> List<T> immutable(List<T> source) {
        return Collections.unmodifiableList(new ArrayList<>(source));
    }
}
