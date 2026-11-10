package com.kaduvill.asjeialldrops.ritual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Loaded effect settings used by explanation pages; reading them never runs a ritual. */
public final class RitualEffects {
    private RitualEffects() {}

    public static final class Detail {
        public final String key;
        private final Object[] arguments;

        public Detail(String key, Object... arguments) {
            this.key = key;
            this.arguments = arguments.clone();
        }

        public Object[] arguments() { return arguments.clone(); }
    }

    public static final class Snapshot {
        public final String key;
        public final double range;
        public final double rangePerLens;
        public final boolean denyAllSpawns;
        public final List<Detail> normalDetails;
        public final List<Detail> corruptedDetails;

        public Snapshot(String key, double range, double rangePerLens, boolean denyAllSpawns,
                        List<Detail> normalDetails, List<Detail> corruptedDetails) {
            this.key = key;
            this.range = range;
            this.rangePerLens = rangePerLens;
            this.denyAllSpawns = denyAllSpawns;
            this.normalDetails = Collections.unmodifiableList(new ArrayList<>(normalDetails));
            this.corruptedDetails = Collections.unmodifiableList(new ArrayList<>(corruptedDetails));
        }

        public boolean dimensionWide(boolean corrupted) { return corrupted && key.equals("lucerna"); }
    }
}
