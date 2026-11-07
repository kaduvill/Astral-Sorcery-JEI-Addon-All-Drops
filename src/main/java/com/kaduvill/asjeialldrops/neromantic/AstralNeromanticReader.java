package com.kaduvill.asjeialldrops.neromantic;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import hellfirepvp.astralsorcery.common.base.FluidRarityRegistry;
import hellfirepvp.astralsorcery.common.base.FluidRarityRegistry.FluidRarityEntry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/** Reads the actual loaded registry once. Never reparses config, calls selection or touches chunks. */
public final class AstralNeromanticReader {
    private final Field entries;

    public AstralNeromanticReader() throws ReflectiveOperationException {
        entries = FluidRarityRegistry.class.getDeclaredField("rarityList");
        entries.setAccessible(true);
    }

    public NeromanticRecipes.Snapshot read() throws ReflectiveOperationException {
        Fluid fuel = FluidRegistry.getFluid("astralsorcery.liquidstarlight");
        if (fuel == null) throw new IllegalStateException("Liquid Starlight is unavailable");
        List<?> loaded = new ArrayList<>((List<?>) entries.get(null));
        UniversalTweaksNeromanticCompat compat = UniversalTweaksNeromanticCompat.inspect();
        NeromanticRecipes.Rules rules = compat.rules;
        List<NeromanticRecipes.Entry> snapshot = new ArrayList<>();
        boolean dimensionsFailed = false;
        for (Object value : loaded) {
            if (!(value instanceof FluidRarityEntry)) throw new IllegalStateException("Invalid loaded Neromantic registry entry");
            FluidRarityEntry entry = (FluidRarityEntry) value;
            int[] dimensions = null;
            if (rules.dimensionsAvailable && !dimensionsFailed) {
                try {
                    dimensions = compat.dimensions(entry);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                    dimensionsFailed = true;
                    ASJeiAllDrops.LOGGER.warn("Neromantic JEI: loaded UT dimension metadata is unreadable", failure);
                }
            }
            snapshot.add(new NeromanticRecipes.Entry(entry.fluid, entry.itemWeight,
                    entry.guaranteedAmount, entry.additionalRandomAmount, dimensions));
        }
        if (dimensionsFailed) rules = rules.withoutDimensions();
        return new NeromanticRecipes.Snapshot(snapshot, fuel, FluidRegistry.WATER, rules);
    }
}
