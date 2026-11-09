package com.kaduvill.asjeialldrops.ritual;

import hellfirepvp.astralsorcery.common.base.HerdableAnimal;
import hellfirepvp.astralsorcery.common.constellation.effect.CEffectEntityCollect;
import hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectBootes;
import hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectOctans;
import hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectPelotrio;
import hellfirepvp.astralsorcery.common.data.config.Config;
import hellfirepvp.astralsorcery.common.util.data.Tuple;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** Version-specific adapter for AS 1.10.27. Reads loaded state without executing ritual handlers. */
public final class AstralRitualReader {
    public RitualRecipes.Snapshot pelotrio(boolean includeConversions) throws ReflectiveOperationException {
        if (!field(CEffectPelotrio.class, "enabled").getBoolean(null)) return null;
        List<RitualRecipes.Conversion> conversions = new ArrayList<>();
        if (includeConversions) {
            Object[] table = ((Object[]) field(CEffectPelotrio.class, "swapTable").get(null)).clone();
            // AS checks actualClass.isAssignableFrom(tableKey), in this order. Do not widen
            // navigation to subclasses: that would advertise conversions the ritual never performs.
            for (Object value : table) {
                Tuple<?, ?> tuple = (Tuple<?, ?>) value;
                ResourceLocation input = EntityList.getKey((Class<? extends Entity>) tuple.key);
                ResourceLocation output = EntityList.getKey((Class<? extends Entity>) tuple.value);
                if (input == null || output == null) continue;
                conversions.add(new RitualRecipes.Conversion(mob(input), mob(output)));
            }
        }
        return new RitualRecipes.Snapshot(field(CEffectPelotrio.class, "searchRange").getInt(null),
                samples("cow", "pig", "chicken", "sheep"),
                samples("zombie", "skeleton", "spider", "creeper"),
                Collections.emptyList(), conversions);
    }

    public RitualRecipes.Snapshot octans() {
        return CEffectOctans.enabled ? new RitualRecipes.Snapshot(CEffectOctans.searchRange,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()) : null;
    }

    public RitualRecipes.Snapshot bootes() throws ReflectiveOperationException {
        // Range/enabled are instance fields on AS's already-configured dynamic entry.
        // Constructing a fresh effect would silently show its defaults instead of the loaded values.
        CEffectBootes effect = null;
        for (Object entry : new ArrayList<>((List<?>) field(Config.class, "dynamicConfigEntries").get(null))) {
            if (entry instanceof CEffectBootes) { effect = (CEffectBootes) entry; break; }
        }
        if (effect == null) throw new IllegalStateException("Loaded Bootes configuration is unavailable");
        if (!effect.enabled) return null;
        boolean generic = false;
        boolean squid = false;
        for (HerdableAnimal<?> handler : HerdableAnimal.registryHerdable) {
            generic |= handler instanceof HerdableAnimal.GenericAnimal;
            squid |= handler instanceof HerdableAnimal.Squid;
        }
        List<ResourceLocation> blacklist = new ArrayList<>(HerdableAnimal.blacklistedEntities);
        List<RitualRecipes.Mob> animals = new ArrayList<>();
        String[] ids = {"cow", "pig", "chicken", "sheep", "rabbit", "squid"};
        ItemStack[] loot = {new ItemStack(Items.BEEF), new ItemStack(Items.PORKCHOP),
                new ItemStack(Items.CHICKEN), new ItemStack(Items.MUTTON),
                new ItemStack(Items.RABBIT), new ItemStack(Items.DYE)};
        for (int i = 0; i < ids.length; i++) {
            ResourceLocation id = new ResourceLocation("minecraft", ids[i]);
            Class<? extends Entity> type = EntityList.getClass(id);
            if (type == null || blacklist.contains(id) || !EntityList.ENTITY_EGGS.containsKey(id)) continue;
            if ((generic && EntityAnimal.class.isAssignableFrom(type))
                    || (squid && EntitySquid.class.isAssignableFrom(type))) {
                animals.add(new RitualRecipes.Mob(id, loot[i]));
            }
        }
        return new RitualRecipes.Snapshot(field(CEffectEntityCollect.class, "range").getDouble(effect),
                Collections.emptyList(), Collections.emptyList(), animals, Collections.emptyList());
    }

    private static List<RitualRecipes.Mob> samples(String... ids) {
        List<RitualRecipes.Mob> result = new ArrayList<>();
        for (String name : ids) {
            ResourceLocation id = new ResourceLocation("minecraft", name);
            if (EntityList.ENTITY_EGGS.containsKey(id)) result.add(mob(id));
        }
        return result;
    }

    private static RitualRecipes.Mob mob(ResourceLocation id) { return new RitualRecipes.Mob(id, ItemStack.EMPTY); }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field result = owner.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }
}
