package com.kaduvill.asjeialldrops.ritual;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.LootTableManager;
import net.minecraft.world.storage.loot.conditions.LootCondition;

/** Reads locally loaded Forge tables, including load-event injections, without generating loot. */
public final class FishingLootReader {
    private final Gson gson;
    private final Field pools;
    private final Field entries;
    private final Field conditions;

    public FishingLootReader() throws ReflectiveOperationException {
        gson = (Gson) field(LootTableManager.class, "GSON_INSTANCE", "field_186526_b").get(null);
        pools = field(LootTable.class, "pools", "field_186466_c");
        entries = field(LootPool.class, "lootEntries", "field_186453_a");
        conditions = field(LootPool.class, "poolConditions", "field_186454_b");
    }

    public Snapshot readLocal() throws ReflectiveOperationException {
        // Forge's loader applies registered LootTableLoadEvent handlers. A fresh, private
        // manager avoids borrowing or modifying a live world's cache. No world folder is supplied.
        LootTableManager manager = new LootTableManager(null) {
            // The constructor calls this method; keep our private manager lazy.
            @Override public void reloadLootTables() { }
        };
        return read(manager::getLootTableFromLocation);
    }

    public Snapshot read(Function<ResourceLocation, LootTable> resolver) throws ReflectiveOperationException {
        List<RitualRecipes.FishingDrop> drops = new ArrayList<>();
        boolean[] partial = {false};
        readTable(LootTableList.GAMEPLAY_FISHING, resolver, new HashSet<>(), drops, partial);
        return new Snapshot(drops, partial[0]);
    }

    private void readTable(ResourceLocation id, Function<ResourceLocation, LootTable> resolver,
                           Set<ResourceLocation> visiting, List<RitualRecipes.FishingDrop> drops,
                           boolean[] partial) throws IllegalAccessException {
        if (visiting.size() >= 64) { partial[0] = true; return; }
        if (!visiting.add(id)) return; // LootContext also suppresses recursive table loops.
        try {
            LootTable table = resolver.apply(id);
            if (table == null) { partial[0] = true; return; }
            for (Object value : new ArrayList<>((List<?>) pools.get(table))) {
                LootPool pool = (LootPool) value;
                double maxRolls = Math.floor(pool.getRolls().getMax())
                        + Math.max(0, Math.floor(pool.getBonusRolls().getMax() * Math.nextDown(1F)));
                if (maxRolls <= 0) continue;
                boolean allowed = true;
                for (Object condition : (List<?>) conditions.get(pool)) {
                    int state;
                    try {
                        state = condition(gson.toJsonTree(condition, LootCondition.class).getAsJsonObject());
                    } catch (RuntimeException unsupported) {
                        state = -1;
                    }
                    if (state < 0) partial[0] = true;
                    if (state <= 0) { allowed = false; break; }
                }
                if (!allowed) continue;
                for (Object entryValue : new ArrayList<>((List<?>) entries.get(pool))) {
                    LootEntry entry = (LootEntry) entryValue;
                    // Octans luck is either zero or a random float below one.
                    if (Math.max(entry.getEffectiveWeight(0), entry.getEffectiveWeight(Math.nextDown(1F))) <= 0) continue;
                    try {
                        JsonObject json = gson.toJsonTree(entry, LootEntry.class).getAsJsonObject();
                        int state = conditions(json);
                        if (state < 0) partial[0] = true;
                        if (state <= 0) continue;
                        switch (json.get("type").getAsString()) {
                            case "loot_table":
                                readTable(new ResourceLocation(json.get("name").getAsString()), resolver, visiting, drops, partial);
                                break;
                            case "item":
                                for (RitualRecipes.FishingDrop drop : readItem(json)) add(drops, drop);
                                break;
                            case "empty": break;
                            default: partial[0] = true;
                        }
                    } catch (IOException | NBTException | RuntimeException failure) {
                        // One scripted entry must not hide every ordinary/modded output.
                        partial[0] = true;
                    }
                }
            }
        } finally {
            visiting.remove(id);
        }
    }

    private static int conditions(JsonObject json) {
        if (!json.has("conditions")) return 1;
        int result = 1;
        for (JsonElement value : json.getAsJsonArray("conditions")) {
            int next = condition(value.getAsJsonObject());
            if (next == 0) return 0;
            if (next < 0) result = -1;
        }
        return result;
    }

    private static int condition(JsonObject json) {
        ResourceLocation name = new ResourceLocation(json.get("condition").getAsString());
        if (!"minecraft".equals(name.getNamespace())) return -1;
        switch (name.getPath()) {
            case "random_chance":
            case "random_chance_with_looting":
                return json.get("chance").getAsFloat() > 0 ? 1 : 0;
            case "killed_by_player":
                return json.has("inverse") && json.get("inverse").getAsBoolean() ? 1 : 0;
            case "entity_properties": return 0; // Octans supplies no entity, killer, or player.
            default: return -1;
        }
    }

    static List<RitualRecipes.FishingDrop> readItem(JsonObject entry) throws IOException, NBTException {
        Item item = Item.REGISTRY.getObject(new ResourceLocation(entry.get("name").getAsString()));
        if (item == null || item == Items.AIR) throw new IOException("Unknown fishing item");
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(item));
        boolean enchanted = false;
        boolean damaged = false;
        boolean amountsVary = false;
        if (entry.has("functions")) for (JsonElement value : entry.getAsJsonArray("functions")) {
            JsonObject function = value.getAsJsonObject();
            int applies = conditions(function);
            if (applies == 0) continue;
            // Conditional transformations can change item identity. Do not claim a single
            // unconditional result without resolving both branches and the custom context.
            if (applies < 0 || (function.has("conditions") && function.getAsJsonArray("conditions").size() > 0))
                throw new IOException("Conditional fishing function");
            ResourceLocation name = new ResourceLocation(function.get("function").getAsString());
            if (!"minecraft".equals(name.getNamespace())) throw new IOException("Custom fishing function: " + name);
            switch (name.getPath()) {
                case "set_data": {
                    int min = (int) Math.floor(bound(function.get("data"), "min"));
                    int max = (int) Math.floor(bound(function.get("data"), "max"));
                    if (min < 0 || max > 32766 || max < min || max - min > 255
                            || (long) stacks.size() * (max - min + 1) > 256)
                        throw new IOException("Unbounded fishing metadata");
                    List<ItemStack> variants = new ArrayList<>();
                    for (ItemStack stack : stacks) {
                        if (stack.isItemStackDamageable()) { variants.add(stack); continue; }
                        for (int metadata = min; metadata <= max; metadata++) {
                            ItemStack copy = stack.copy();
                            copy.setItemDamage(metadata);
                            variants.add(copy);
                        }
                    }
                    stacks = variants;
                    break;
                }
                case "set_count": {
                    int min = (int) Math.floor(bound(function.get("count"), "min"));
                    int max = (int) Math.floor(bound(function.get("count"), "max"));
                    if (max <= 0) return Collections.emptyList();
                    amountsVary |= min != max;
                    for (ItemStack stack : stacks) stack.setCount(Math.max(1, min));
                    break;
                }
                case "set_nbt": {
                    NBTTagCompound tag = JsonToNBT.getTagFromJson(function.get("tag").getAsString());
                    for (ItemStack stack : stacks) {
                        NBTTagCompound merged = stack.hasTagCompound() ? stack.getTagCompound().copy() : new NBTTagCompound();
                        merged.merge(tag);
                        stack.setTagCompound(merged);
                    }
                    break;
                }
                case "set_damage": damaged = true; break;
                case "enchant_with_levels":
                case "enchant_randomly":
                    enchanted = true;
                    for (int i = 0; i < stacks.size(); i++) if (stacks.get(i).getItem() == Items.BOOK)
                        stacks.set(i, new ItemStack(Items.ENCHANTED_BOOK, stacks.get(i).getCount()));
                    break;
                case "looting_enchant": break; // Octans has no killer, so this function does nothing.
                default: throw new IOException("Unsupported fishing function: " + name);
            }
        }
        List<RitualRecipes.FishingDrop> result = new ArrayList<>();
        for (ItemStack stack : stacks) if (!stack.isEmpty()) result.add(new RitualRecipes.FishingDrop(stack, enchanted, damaged, amountsVary));
        return result;
    }

    private static float bound(JsonElement value, String key) {
        return value.isJsonObject() ? value.getAsJsonObject().get(key).getAsFloat() : value.getAsFloat();
    }

    private static Field field(Class<?> owner, String... names) throws NoSuchFieldException {
        for (String name : names) try {
            Field result = owner.getDeclaredField(name);
            result.setAccessible(true);
            return result;
        } catch (NoSuchFieldException missing) {
            // Try both the development and runtime mappings, without relying on a launch wrapper.
        }
        throw new NoSuchFieldException(owner.getName() + ": " + String.join(", ", names));
    }

    private static void add(List<RitualRecipes.FishingDrop> drops, RitualRecipes.FishingDrop candidate) {
        for (RitualRecipes.FishingDrop drop : drops) if (ItemStack.areItemStacksEqual(drop.stack(), candidate.stack())
                && drop.enchanted == candidate.enchanted && drop.damaged == candidate.damaged
                && drop.amountsVary == candidate.amountsVary) return;
        drops.add(candidate);
    }

    public static final class Snapshot {
        public final List<RitualRecipes.FishingDrop> drops;
        public final boolean partial;
        Snapshot(List<RitualRecipes.FishingDrop> drops, boolean partial) {
            this.drops = Collections.unmodifiableList(new ArrayList<>(drops));
            this.partial = partial;
        }
    }
}
