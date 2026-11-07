package com.kaduvill.asjeialldrops.neromantic;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import hellfirepvp.astralsorcery.common.base.FluidRarityRegistry.FluidRarityEntry;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraftforge.fml.common.Loader;

/** Optional, read-only adapter. No UT/Mixin classes appear in this addon's linkage signatures. */
final class UniversalTweaksNeromanticCompat {
    private static final String ROOT = "mod.acgaming.universaltweaks.";
    private static final String ENTRY_EXTENSION = ROOT + "mods.astralsorcery.neromanticprime.duck.UTFluidRarityEntryExt";
    private final Method dimensions;
    final NeromanticRecipes.Rules rules;

    private UniversalTweaksNeromanticCompat(Method dimensions, NeromanticRecipes.Rules rules) {
        this.dimensions = dimensions;
        this.rules = rules;
    }

    static UniversalTweaksNeromanticCompat inspect() {
        boolean optionalRules = Loader.isModLoaded("universaltweaks");
        Method getter = null;
        try {
            getter = dimensionGetter(FluidRarityEntry.class);
            optionalRules |= getter != null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            ASJeiAllDrops.LOGGER.warn("Neromantic JEI: UT dimension getter is unavailable", failure);
            return new UniversalTweaksNeromanticCompat(null, NeromanticRecipes.Rules.unknown(false, 0));
        }
        if (!optionalRules) return new UniversalTweaksNeromanticCompat(null, NeromanticRecipes.Rules.base());

        if (getter == null) {
            try {
                Class<?> general = Class.forName(ROOT + "config.UTConfigGeneral", false, FluidRarityEntry.class.getClassLoader());
                Object master = general.getField("MASTER_SWITCHES").get(null);
                if (!master.getClass().getField("utMasterSwitchModIntegration").getBoolean(master)) {
                    return new UniversalTweaksNeromanticCompat(null, NeromanticRecipes.Rules.base());
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                ASJeiAllDrops.LOGGER.warn("Neromantic JEI: UT integration setting is unreadable", failure);
            }
            return new UniversalTweaksNeromanticCompat(null, NeromanticRecipes.Rules.unknown(false, 0));
        }

        int quantity = 0;
        try {
            Class<?> config = Class.forName(ROOT + "config.UTConfigMods", false, FluidRarityEntry.class.getClassLoader());
            Object settings = config.getField("ASTRAL_SORCERY").get(null);
            try {
                quantity = settings.getClass().getField("utNeromanticPrimeExtractionQuantity").getInt(settings);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                ASJeiAllDrops.LOGGER.warn("Neromantic JEI: UT extraction setting is unreadable", failure);
            }
            boolean overflow = settings.getClass().getField("utNeromanticPrimeFluidOverflow").getBoolean(settings);
            boolean unlimited = settings.getClass().getField("utNeromanticPrimeUnlimitedVeins").getBoolean(settings);
            // Compatibility contract: the injected API identifies the integration, whose hooks
            // are modeled as intended. No claim of independent call-site verification is made.
            // The inspected UT dimension boolean does not gate its selector or entry parser.
            return new UniversalTweaksNeromanticCompat(getter, NeromanticRecipes.Rules.universalTweaks(overflow, unlimited, quantity));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            ASJeiAllDrops.LOGGER.warn("Neromantic JEI: UT reserve settings are unreadable", failure);
            return new UniversalTweaksNeromanticCompat(getter, NeromanticRecipes.Rules.unknown(true, quantity));
        }
    }

    int[] dimensions(FluidRarityEntry entry) throws ReflectiveOperationException {
        return dimensions == null ? null : (int[]) dimensions.invoke(entry);
    }

    /** Finds the public API on the transformed entry itself, without loading an optional UT class. */
    static Method dimensionGetter(Class<?> entryType) throws NoSuchMethodException {
        for (Class<?> type : entryType.getInterfaces()) {
            if (ENTRY_EXTENSION.equals(type.getName())) {
                Method getter = type.getMethod("ut$getAllowedDims");
                if (getter.getReturnType() != int[].class || Modifier.isStatic(getter.getModifiers())) {
                    throw new NoSuchMethodException("Unsupported UT dimension getter signature");
                }
                return getter;
            }
        }
        return null;
    }
}
