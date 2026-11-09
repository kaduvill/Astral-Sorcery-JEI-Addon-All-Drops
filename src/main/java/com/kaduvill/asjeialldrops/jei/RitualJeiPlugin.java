package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import com.kaduvill.asjeialldrops.config.ASJeiAllDropsConfig;
import com.kaduvill.asjeialldrops.ritual.AstralRitualReader;
import com.kaduvill.asjeialldrops.ritual.AstralRitualEffectReader;
import com.kaduvill.asjeialldrops.ritual.AstralEvorsioReader;
import com.kaduvill.asjeialldrops.ritual.AstralFornaxReader;
import com.kaduvill.asjeialldrops.ritual.FornaxRecipes;
import com.kaduvill.asjeialldrops.ritual.RitualEffects;
import com.kaduvill.asjeialldrops.ritual.RitualRecipes;
import com.kaduvill.asjeialldrops.ritual.VanillaFishingReader;
import com.kaduvill.asjeialldrops.ritual.FishingLootReader;
import hellfirepvp.astralsorcery.common.constellation.IWeakConstellation;
import hellfirepvp.astralsorcery.common.lib.Constellations;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTException;

/** Snapshots loaded AS settings once per JEI registration, with independent category failures. */
@JEIPlugin
public final class RitualJeiPlugin implements IModPlugin {
    private final Map<RitualRecipeCategory, List<RitualRecipeWrapper>> recipes = new LinkedHashMap<>();
    private FornaxRecipes.Snapshot fornax;
    private final List<FornaxRecipeWrapper> fornaxPages = new ArrayList<>();

    @Override public void registerItemSubtypes(ISubtypeRegistry registry) {
        if (ASJeiAllDropsConfig.pelotrio || ASJeiAllDropsConfig.octans || ASJeiAllDropsConfig.bootes
                || ASJeiAllDropsConfig.fornax || ASJeiAllDropsConfig.evorsio
                || ASJeiAllDropsConfig.aevitas || ASJeiAllDropsConfig.armara
                || ASJeiAllDropsConfig.discidia || ASJeiAllDropsConfig.horologium
                || ASJeiAllDropsConfig.lucerna || ASJeiAllDropsConfig.vicio)
            RitualItems.registerSubtypes(registry);
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registry) {
        recipes.clear();
        fornax = null;
        fornaxPages.clear();
        IGuiHelper helper = registry.getJeiHelpers().getGuiHelper();
        AstralRitualReader reader = new AstralRitualReader();
        if (ASJeiAllDropsConfig.pelotrio) try {
            RitualRecipes.Snapshot snapshot = reader.pelotrio(ASJeiAllDropsConfig.pelotrioCorrupted);
            if (snapshot != null) {
                RitualMobFaces faces = new RitualMobFaces(helper);
                List<RitualRecipeWrapper> pages = new ArrayList<>();
                if (!snapshot.day.isEmpty()) pages.add(new MobRitualRecipeWrapper(faces, snapshot,
                        MobRitualRecipeWrapper.Mode.DAY, snapshot.day, false));
                if (!snapshot.night.isEmpty()) pages.add(new MobRitualRecipeWrapper(faces, snapshot,
                        MobRitualRecipeWrapper.Mode.NIGHT, snapshot.night, false));
                for (RitualRecipes.Conversion conversion : snapshot.conversions)
                    pages.add(new MobRitualRecipeWrapper(faces, snapshot, conversion));
                add(registry, helper, "pelotrio", Constellations.pelotrio, pages);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            failed("Pelotrio", failure);
        }
        if (ASJeiAllDropsConfig.octans) try {
            RitualRecipes.Snapshot snapshot = reader.octans();
            if (snapshot != null) {
                List<RitualRecipes.FishingDrop> drops;
                try {
                    FishingLootReader.Snapshot fishingLoot = new FishingLootReader().readLocal();
                    drops = fishingLoot.drops;
                    if (fishingLoot.partial) ASJeiAllDrops.LOGGER.warn(
                            "Octans fishing list is incomplete: some custom loot conditions or functions could not be resolved");
                } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                    ASJeiAllDrops.LOGGER.warn("Octans local loot table unavailable; trying bundled defaults", failure);
                    try {
                        drops = new VanillaFishingReader().read();
                    } catch (IOException | NBTException | RuntimeException fallbackFailure) {
                        ASJeiAllDrops.LOGGER.warn("Octans default loot list unavailable; retaining ritual explanations", fallbackFailure);
                        drops = Collections.emptyList();
                    }
                }
                FishingRecipeWrapper fishing = new FishingRecipeWrapper(snapshot.range, drops);
                List<RitualRecipeWrapper> pages = new ArrayList<>();
                pages.add(fishing);
                if (ASJeiAllDropsConfig.octansCorrupted)
                    pages.add(new RitualInfoRecipeWrapper(snapshot.range, "octans"));
                add(registry, helper, "octans", Constellations.octans, pages);
            }
        } catch (RuntimeException | LinkageError failure) {
            failed("Octans", failure);
        }
        if (ASJeiAllDropsConfig.bootes) try {
            RitualRecipes.Snapshot snapshot = reader.bootes();
            if (snapshot != null && !snapshot.animals.isEmpty()) {
                RitualMobFaces faces = new RitualMobFaces(helper);
                List<RitualRecipeWrapper> pages = new ArrayList<>();
                pages.add(new MobRitualRecipeWrapper(faces, snapshot, MobRitualRecipeWrapper.Mode.HERDING,
                        snapshot.animals, false));
                if (ASJeiAllDropsConfig.bootesCorrupted)
                    pages.add(new MobRitualRecipeWrapper(faces, snapshot, MobRitualRecipeWrapper.Mode.HERDING,
                            snapshot.animals, true));
                add(registry, helper, "bootes", Constellations.bootes, pages);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            failed("Bootes", failure);
        }
        if (ASJeiAllDropsConfig.fornax) try {
            fornax = new AstralFornaxReader().read(ASJeiAllDropsConfig.fornaxCorrupted);
            if (fornax != null) {
                for (FornaxRecipes.Recipe recipe : fornax.recipes)
                    fornaxPages.add(new FornaxRecipeWrapper(fornax, recipe));
                add(registry, helper, "fornax", Constellations.fornax, new ArrayList<>(fornaxPages));
            }
        } catch (RuntimeException | LinkageError failure) {
            failed("Fornax", failure);
        }
        AstralRitualEffectReader effects = new AstralRitualEffectReader();
        if (ASJeiAllDropsConfig.evorsio) try {
            RitualEffects.Snapshot snapshot = effects.read("evorsio");
            if (snapshot != null) {
                List<RitualRecipeWrapper> pages = new ArrayList<>();
                pages.add(new RitualInfoRecipeWrapper(snapshot, false));
                if (ASJeiAllDropsConfig.evorsioCorrupted) try {
                    AstralEvorsioReader.Snapshot corrupted = new AstralEvorsioReader().read();
                    if (corrupted != null) pages.add(new EvorsioRecipeWrapper(corrupted));
                } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                    failed("Corrupted Evorsio", failure);
                }
                add(registry, helper, "evorsio", Constellations.evorsio, pages);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            failed("Evorsio", failure);
        }
        addEffect(registry, helper, effects, "aevitas", Constellations.aevitas,
                ASJeiAllDropsConfig.aevitas, ASJeiAllDropsConfig.aevitasCorrupted);
        addEffect(registry, helper, effects, "armara", Constellations.armara,
                ASJeiAllDropsConfig.armara, ASJeiAllDropsConfig.armaraCorrupted);
        addEffect(registry, helper, effects, "discidia", Constellations.discidia,
                ASJeiAllDropsConfig.discidia, ASJeiAllDropsConfig.discidiaCorrupted);
        addEffect(registry, helper, effects, "horologium", Constellations.horologium,
                ASJeiAllDropsConfig.horologium, ASJeiAllDropsConfig.horologiumCorrupted);
        addEffect(registry, helper, effects, "lucerna", Constellations.lucerna,
                ASJeiAllDropsConfig.lucerna, ASJeiAllDropsConfig.lucernaCorrupted);
        addEffect(registry, helper, effects, "vicio", Constellations.vicio,
                ASJeiAllDropsConfig.vicio, ASJeiAllDropsConfig.vicioCorrupted);
    }

    private void addEffect(IRecipeCategoryRegistration registry, IGuiHelper helper, AstralRitualEffectReader reader,
                           String key, IWeakConstellation constellation, boolean enabled, boolean corrupted) {
        if (!enabled) return;
        try {
            RitualEffects.Snapshot snapshot = reader.read(key);
            if (snapshot == null) return;
            List<RitualRecipeWrapper> pages = new ArrayList<>();
            pages.add(new RitualInfoRecipeWrapper(snapshot, false));
            if (corrupted) pages.add(new RitualInfoRecipeWrapper(snapshot, true));
            add(registry, helper, key, constellation, pages);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            failed(key, failure);
        }
    }

    private void add(IRecipeCategoryRegistration registry, IGuiHelper helper, String key,
                     IWeakConstellation constellation, List<RitualRecipeWrapper> pages) {
        if (pages.isEmpty()) return;
        int height = 0;
        for (RitualRecipeWrapper page : pages) height = Math.max(height, page.height());
        RitualRecipeCategory category = new RitualRecipeCategory(helper, key, constellation, height);
        recipes.put(category, Collections.unmodifiableList(new ArrayList<>(pages)));
        registry.addRecipeCategories(category);
    }

    @Override public void register(IModRegistry registry) {
        for (Map.Entry<RitualRecipeCategory, List<RitualRecipeWrapper>> entry : recipes.entrySet()) {
            RitualRecipeCategory category = entry.getKey();
            category.itemRenderer = registry.getIngredientRegistry().getIngredientRenderer(VanillaTypes.ITEM);
            category.itemHelper = registry.getIngredientRegistry().getIngredientHelper(VanillaTypes.ITEM);
            IWeakConstellation constellation = category.constellation;
            List<ItemStack> catalysts = RitualItems.catalysts(registry, category.getUid(), constellation);
            if (constellation == Constellations.fornax) {
                // One provider owns both normal browsing and native, precedence-aware focus lookup.
                registry.addRecipeRegistryPlugin(new FornaxRecipeLookup(fornax, fornaxPages,
                        registry.getJeiHelpers().getStackHelper(), catalysts));
            } else if (constellation == Constellations.octans) {
                for (RitualRecipeWrapper page : entry.getValue()) if (page instanceof FishingRecipeWrapper)
                    registry.addRecipeRegistryPlugin(new FishingRecipeLookup((FishingRecipeWrapper) page,
                            entry.getValue(), category.itemHelper, catalysts));
            } else registry.addRecipes(entry.getValue(), category.getUid());
            if (constellation == Constellations.pelotrio || constellation == Constellations.bootes)
                registry.addRecipeRegistryPlugin(new MobRitualRecipeLookup(category.getUid(), entry.getValue()));
        }
    }

    private static void failed(String name, Throwable failure) {
        ASJeiAllDrops.LOGGER.warn(name + " JEI pages disabled: cannot read the installed Astral Sorcery ritual", failure);
    }
}
