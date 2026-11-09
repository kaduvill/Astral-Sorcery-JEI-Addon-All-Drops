package com.kaduvill.asjeialldrops.ritual;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.NBTException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTableManager;

/** Reads the bundled default JSON, not a server's effective table. Never samples random loot. */
public final class VanillaFishingReader {
    public List<RitualRecipes.FishingDrop> read() throws IOException, NBTException {
        List<RitualRecipes.FishingDrop> result = new ArrayList<>();
        readTable(new ResourceLocation("minecraft", "gameplay/fishing"), new HashSet<>(), result);
        return result;
    }

    private void readTable(ResourceLocation id, Set<ResourceLocation> visiting,
                           List<RitualRecipes.FishingDrop> result) throws IOException, NBTException {
        if (!visiting.add(id)) throw new IOException("Recursive fishing loot table: " + id);
        String path = "/assets/" + id.getNamespace() + "/loot_tables/" + id.getPath() + ".json";
        try (InputStream stream = LootTableManager.class.getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Missing default loot table: " + id);
            JsonObject table = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement poolElement : table.getAsJsonArray("pools")) {
                JsonObject lootPool = poolElement.getAsJsonObject();
                rejectConditions(lootPool);
                for (JsonElement entryElement : lootPool.getAsJsonArray("entries")) {
                    JsonObject entry = entryElement.getAsJsonObject();
                    rejectConditions(entry);
                    String type = entry.get("type").getAsString();
                    if (type.equals("loot_table")) {
                        ResourceLocation child = new ResourceLocation(entry.get("name").getAsString());
                        readTable(child, visiting, result);
                    } else if (type.equals("item")) {
                        result.addAll(FishingLootReader.readItem(entry));
                    } else throw new IOException("Unsupported default fishing entry: " + type);
                }
            }
        } finally {
            visiting.remove(id);
        }
    }

    private static void rejectConditions(JsonObject object) throws IOException {
        // The stock tables are unconditional. Do not silently turn an unfamiliar override
        // into an apparently complete, unconditional production list.
        if (object.has("conditions") && object.getAsJsonArray("conditions").size() != 0)
            throw new IOException("Conditional default fishing data is not supported");
    }
}
