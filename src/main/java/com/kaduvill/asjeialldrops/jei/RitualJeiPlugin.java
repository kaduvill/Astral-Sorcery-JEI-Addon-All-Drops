package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import com.kaduvill.asjeialldrops.config.ASJeiAllDropsConfig;
import com.kaduvill.asjeialldrops.ritual.AstralRitualReader;
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
import net.minecraft.nbt.NBTException;

/** Snapshots loaded AS settings once per JEI registration, with independent category failures. */
@JEIPlugin
public final class RitualJeiPlugin implements IModPlugin {
    private final Map<RitualRecipeCategory, List<RitualRecipeWrapper>> recipes = new LinkedHashMap<>();

    @Override public void registerItemSubtypes(ISubtypeRegistry registry) {
        if (ASJeiAllDropsConfig.pelotrio || ASJeiAllDropsConfig.octans || ASJeiAllDropsConfig.bootes)
            RitualItems.registerSubtypes(registry);
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registry) {
        recipes.clear();
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
                add(registry, helper, "pelotrio", pages);
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
                add(registry, helper, "octans", pages);
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
                add(registry, helper, "bootes", pages);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            failed("Bootes", failure);
        }
    }

    private void add(IRecipeCategoryRegistration registry, IGuiHelper helper, String key, List<RitualRecipeWrapper> pages) {
        if (pages.isEmpty()) return;
        int height = 0;
        for (RitualRecipeWrapper page : pages) height = Math.max(height, page.height());
        RitualRecipeCategory category = new RitualRecipeCategory(helper, key, height);
        recipes.put(category, Collections.unmodifiableList(new ArrayList<>(pages)));
        registry.addRecipeCategories(category);
    }

    @Override public void register(IModRegistry registry) {
        for (Map.Entry<RitualRecipeCategory, List<RitualRecipeWrapper>> entry : recipes.entrySet()) {
            RitualRecipeCategory category = entry.getKey();
            category.itemRenderer = registry.getIngredientRegistry().getIngredientRenderer(VanillaTypes.ITEM);
            registry.addRecipes(entry.getValue(), category.getUid());
            IWeakConstellation constellation = category.getUid().endsWith("pelotrio") ? Constellations.pelotrio
                    : category.getUid().endsWith("octans") ? Constellations.octans : Constellations.bootes;
            RitualItems.catalysts(registry, category.getUid(), constellation);
            if (constellation == Constellations.pelotrio || constellation == Constellations.bootes)
                registry.addRecipeRegistryPlugin(new MobRitualRecipeLookup(category.getUid(), entry.getValue()));
            for (RitualRecipeWrapper page : entry.getValue()) if (page instanceof FishingRecipeWrapper)
                registry.addRecipeRegistryPlugin(new FishingRecipeLookup((FishingRecipeWrapper) page,
                        registry.getIngredientRegistry().getIngredientHelper(VanillaTypes.ITEM)));
        }
    }

    private static void failed(String name, Throwable failure) {
        ASJeiAllDrops.LOGGER.warn(name + " JEI pages disabled: cannot read the installed Astral Sorcery ritual", failure);
    }
}
