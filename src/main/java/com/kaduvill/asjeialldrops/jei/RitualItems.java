package com.kaduvill.asjeialldrops.jei;

import hellfirepvp.astralsorcery.common.constellation.IWeakConstellation;
import hellfirepvp.astralsorcery.common.item.crystal.CrystalProperties;
import hellfirepvp.astralsorcery.common.item.crystal.base.ItemTunedCrystalBase;
import hellfirepvp.astralsorcery.common.lib.ItemsAS;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

final class RitualItems {
    private RitualItems() {}

    static void registerSubtypes(ISubtypeRegistry registry) {
        for (ItemTunedCrystalBase item : new ItemTunedCrystalBase[]{ItemsAS.tunedCelestialCrystal, ItemsAS.tunedRockCrystal}) {
            if (item == null || registry.hasSubtypeInterpreter(new ItemStack(item))) continue;
            registry.registerSubtypeInterpreter(item, stack -> {
                IWeakConstellation main = ItemTunedCrystalBase.getMainConstellation(stack.copy());
                return main == null ? ISubtypeRegistry.ISubtypeInterpreter.NONE : main.getUnlocalizedName();
            });
        }
    }

    static List<ItemStack> crystals(IWeakConstellation constellation) {
        List<ItemStack> result = new ArrayList<>();
        if (constellation == null) return result;
        if (ItemsAS.tunedCelestialCrystal != null) {
            ItemStack stack = new ItemStack(ItemsAS.tunedCelestialCrystal);
            CrystalProperties.applyCrystalProperties(stack, CrystalProperties.getMaxCelestialProperties());
            result.add(stack);
        }
        if (ItemsAS.tunedRockCrystal != null) {
            ItemStack stack = new ItemStack(ItemsAS.tunedRockCrystal);
            CrystalProperties.applyCrystalProperties(stack, CrystalProperties.getMaxRockProperties());
            result.add(stack);
        }
        for (ItemStack stack : result) {
            ItemTunedCrystalBase.applyMainConstellation(stack, constellation);
        }
        return result;
    }

    static void catalysts(IModRegistry registry, String uid, IWeakConstellation constellation) {
        ItemStack pedestal = pedestal();
        if (!pedestal.isEmpty()) registry.addRecipeCatalyst(pedestal, uid);
        for (ItemStack crystal : crystals(constellation)) registry.addRecipeCatalyst(crystal, uid);
    }

    static ItemStack pedestal() {
        Item item = Item.getByNameOrId("astralsorcery:blockritualpedestal");
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
