package com.kaduvill.asjeialldrops.ritual;

import hellfirepvp.astralsorcery.common.constellation.effect.CEffectEntityCollect;
import hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectAevitas;
import hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectArmara;
import hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectDiscidia;
import hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectEvorsio;
import hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectHorologium;
import hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectLucerna;
import hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectVicio;
import hellfirepvp.astralsorcery.common.data.config.Config;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** AS 1.10.27 adapter. Instance settings come from configured entries, never new effects. */
public final class AstralRitualEffectReader {
    public RitualEffects.Snapshot read(String key) throws ReflectiveOperationException {
        List<RitualEffects.Detail> normal = new ArrayList<>();
        List<RitualEffects.Detail> corrupted = new ArrayList<>();
        double range;
        double perLens = 0;
        switch (key) {
            case "aevitas":
                if (!CEffectAevitas.enabled) return null;
                range = CEffectAevitas.searchRange;
                int regeneration = CEffectAevitas.potionAmplifier;
                normal.add(detail("regeneration", regeneration + 1));
                corrupted.add(detail("bleeding", regeneration * 2 + 1));
                corrupted.add(detail("weakness", regeneration * 3 + 1));
                corrupted.add(detail("hunger", regeneration * 4 + 1));
                corrupted.add(detail("mining_fatigue", regeneration * 2 + 1));
                break;
            case "armara":
                if (!CEffectArmara.enabled) return null;
                range = CEffectArmara.protectionRange;
                int protection = CEffectArmara.potionAmplifier;
                normal.add(detail("resistance_players_and_hostiles", protection + 1));
                normal.add(detail("absorption_players", protection + 1));
                corrupted.add(detail("speed", protection + 5));
                corrupted.add(detail("regeneration", protection + 5));
                corrupted.add(detail("resistance", protection + 3));
                corrupted.add(detail("strength", protection + 5));
                corrupted.add(detail("fire_resistance", protection + 5));
                corrupted.add(detail("haste", protection + 5));
                break;
            case "discidia":
                CEffectDiscidia discidia = configured(CEffectDiscidia.class);
                if (!discidia.enabled) return null;
                range = field(CEffectEntityCollect.class, "range").getDouble(discidia);
                // The pedestal passes strength 1; AS applies its multiplier using float arithmetic.
                float potency = (float) CEffectDiscidia.potencyMultiplier;
                float damage = potency * CEffectDiscidia.damage;
                normal.add(detail("damage", damage));
                corrupted.add(detail("healing", damage));
                corrupted.add(detail("resistance", 3));
                corrupted.add(detail("owner_damage", 1.2f * potency));
                break;
            case "horologium":
                if (!configured(CEffectHorologium.class).enabled) return null;
                range = CEffectHorologium.searchRange;
                normal.add(detail("acceleration_blacklist"));
                break;
            case "lucerna":
                if (!CEffectLucerna.enabled) return null;
                range = CEffectLucerna.range;
                perLens = CEffectLucerna.rangeIncrease;
                break;
            case "vicio":
                if (!CEffectVicio.enabled) return null;
                range = CEffectVicio.effectRange;
                perLens = CEffectVicio.effectRangePerLens;
                break;
            case "evorsio":
                if (!CEffectEvorsio.enabled) return null;
                range = CEffectEvorsio.searchRange;
                break;
            default:
                throw new IllegalArgumentException("Unsupported ritual effect: " + key);
        }
        boolean denyAll = Config.doesMobSpawnDenyDenyEverything;
        if (key.equals("armara") || key.equals("lucerna")) {
            normal.add(detail("spawners_exempt"));
        }
        if (key.equals("armara")) {
            corrupted.add(detail("spawners_exempt"));
        }
        return new RitualEffects.Snapshot(key, range, perLens, denyAll, normal, corrupted);
    }

    private static RitualEffects.Detail detail(String key, Object... arguments) {
        return new RitualEffects.Detail("effects.detail." + key, arguments);
    }

    private static <T> T configured(Class<T> type) throws ReflectiveOperationException {
        List<?> entries = new ArrayList<>((List<?>) field(Config.class, "dynamicConfigEntries").get(null));
        return configured(type, entries);
    }

    static <T> T configured(Class<T> type, List<?> entries) {
        for (Object entry : entries) if (type.isInstance(entry)) return type.cast(entry);
        throw new IllegalStateException("Loaded " + type.getSimpleName() + " configuration is unavailable");
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
