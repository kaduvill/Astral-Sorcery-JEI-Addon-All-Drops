package com.kaduvill.asjeialldrops.neromantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Pure analysis of AS arithmetic and intended UT rules. Never calls RNG. */
public final class NeromanticProbabilities {
    private NeromanticProbabilities() {}

    /** Invalid weighted pools are unknown, never repaired by removing tickets. */
    public static double[] selection(int[] weights) {
        long total = 0;
        for (int weight : weights) {
            if (weight < 0) return unknown(weights.length);
            total += weight;
        }
        if (total <= 0 || total > Integer.MAX_VALUE) return unknown(weights.length);
        double[] result = new double[weights.length];
        for (int i = 0; i < weights.length; i++) result[i] = (double) weights[i] / total;
        return result;
    }

    private static double[] unknown(int count) {
        double[] result = new double[count];
        java.util.Arrays.fill(result, Double.NaN);
        return result;
    }

    public static Reserve reserve(int guaranteed, int randomExtra, boolean fixNegative, boolean unlimited) {
        if (randomExtra <= 0) return new Reserve(false, Collections.emptyList(), false, Double.NaN);
        long low = Math.max(0L, 1L - guaranteed);
        long high = Math.min((long) randomExtra - 1, (long) Integer.MAX_VALUE - guaranteed);
        long positive = Math.max(0, high - low + 1);
        long overflowStart = Math.max(0L, (long) Integer.MAX_VALUE - guaranteed + 1);
        long overflow = Math.max(0L, (long) randomExtra - overflowStart);
        // UT's intended amount correction clamps any negative signed result, including negative
        // configured amounts. Zero remains empty. Positive sums above MAX wrap negative in AS.
        long negative = Math.min((long) randomExtra, Math.max(0L, -(long) guaranteed)) + overflow;
        long corrected = fixNegative ? negative : 0;
        long maximumRoll = (long) Integer.MAX_VALUE - guaranteed;
        boolean maximum = corrected > 0 || maximumRoll >= 0 && maximumRoll < randomExtra;
        List<Amount> finite = new ArrayList<>();
        long finiteHigh = Math.min(high, (long) Integer.MAX_VALUE - (unlimited ? 1 : 0) - guaranteed);
        if (finiteHigh >= low) finite.add(new Amount((long) guaranteed + low, (long) guaranteed + finiteHigh));
        if (corrected > 0 && !unlimited) finite.add(new Amount(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return new Reserve(true, mergeAmounts(finite), unlimited && maximum,
                (double) (positive + corrected) / randomExtra);
    }

    /** Merge only overlapping or adjacent intervals, never invent amounts across a gap. */
    public static List<Amount> mergeAmounts(List<Amount> amounts) {
        List<Amount> sorted = new ArrayList<>(amounts);
        sorted.sort(Comparator.comparingLong((Amount amount) -> amount.minimum).thenComparingLong(amount -> amount.maximum));
        List<Amount> merged = new ArrayList<>();
        for (Amount amount : sorted) {
            if (!merged.isEmpty() && amount.minimum <= merged.get(merged.size() - 1).maximum + 1) {
                Amount previous = merged.remove(merged.size() - 1);
                merged.add(new Amount(previous.minimum, Math.max(previous.maximum, amount.maximum)));
            } else merged.add(amount);
        }
        return Collections.unmodifiableList(merged);
    }

    public static boolean validQuantity(int quantity) {
        return quantity > 0 && 2L * quantity - 1 <= Integer.MAX_VALUE;
    }

    public static final class Reserve {
        public final boolean valid;
        public final List<Amount> finite;
        public final boolean infinite;
        public final double positive;

        private Reserve(boolean valid, List<Amount> finite, boolean infinite, double positive) {
            this.valid = valid;
            this.finite = finite;
            this.infinite = infinite;
            this.positive = positive;
        }
    }

    public static final class Amount {
        public final long minimum, maximum;
        public Amount(long minimum, long maximum) { this.minimum = minimum; this.maximum = maximum; }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Amount)) return false;
            Amount amount = (Amount) other;
            return minimum == amount.minimum && maximum == amount.maximum;
        }
        @Override public int hashCode() { return Objects.hash(minimum, maximum); }
    }
}
