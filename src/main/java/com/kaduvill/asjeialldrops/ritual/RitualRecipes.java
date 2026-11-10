package com.kaduvill.asjeialldrops.ritual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.init.Items;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/** Read-only registration snapshots; none of these examples create entities or roll loot. */
public final class RitualRecipes {
    private RitualRecipes() {}

    public static final class Mob {
        public final ResourceLocation id;
        private final ItemStack egg;
        private final ItemStack loot;

        public Mob(ResourceLocation id, ItemStack loot) {
            this.id = id;
            egg = new ItemStack(Items.SPAWN_EGG);
            ItemMonsterPlacer.applyEntityIdToItemStack(egg, id);
            this.loot = loot.copy();
        }

        public ItemStack egg() { return egg.copy(); }
        public ItemStack loot() { return loot.copy(); }
    }

    public static final class Conversion {
        public final Mob input;
        public final Mob output;
        public Conversion(Mob input, Mob output) { this.input = input; this.output = output; }
    }

    public static final class Snapshot {
        public final double range;
        public final List<Mob> day;
        public final List<Mob> night;
        public final List<Mob> animals;
        public final List<Conversion> conversions;

        public Snapshot(double range, List<Mob> day, List<Mob> night, List<Mob> animals,
                        List<Conversion> conversions) {
            this.range = range;
            this.day = immutable(day);
            this.night = immutable(night);
            this.animals = immutable(animals);
            this.conversions = immutable(conversions);
        }
    }

    public static final class FishingDrop {
        private final ItemStack stack;
        public final boolean enchanted;
        public final boolean damaged;
        public final boolean amountsVary;

        public FishingDrop(ItemStack stack, boolean enchanted, boolean damaged) {
            this(stack, enchanted, damaged, false);
        }

        public FishingDrop(ItemStack stack, boolean enchanted, boolean damaged, boolean amountsVary) {
            this.stack = stack.copy();
            this.enchanted = enchanted;
            this.damaged = damaged;
            this.amountsVary = amountsVary;
        }

        public ItemStack stack() { return stack.copy(); }

        public boolean matches(ItemStack candidate) {
            if (candidate.isEmpty() || candidate.getItem() != stack.getItem()) return false;
            if (!(damaged && stack.isItemStackDamageable()) && candidate.getMetadata() != stack.getMetadata()) return false;
            if (!enchanted) return ItemStack.areItemStackTagsEqual(candidate, stack);
            // Random enchantments may vary; names and other fixed loot-table tags must still match.
            NBTTagCompound expected = stack.hasTagCompound() ? stack.getTagCompound().copy() : new NBTTagCompound();
            NBTTagCompound actual = candidate.hasTagCompound() ? candidate.getTagCompound().copy() : new NBTTagCompound();
            String key = stack.getItem() == Items.ENCHANTED_BOOK ? "StoredEnchantments" : "ench";
            expected.removeTag(key);
            actual.removeTag(key);
            return expected.equals(actual);
        }
    }

    private static <T> List<T> immutable(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
