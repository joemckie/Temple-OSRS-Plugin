package com.templeosrs.util.collections.parser;

import com.google.gson.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.templeosrs.util.collections.database.CollectionDatabase;
import com.templeosrs.util.collections.data.CollectionResponse;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class CollectionParser {
    @Inject
    private Gson gson;

    /**
     * Parses the Temple API response to a map of collection log items
     * @param rawUsername The username associated with the response
     * @param json The raw JSON response from the Player Collection Log endpoint
     * @return A map of categories and their respective items, where the key is the API category (e.g. kree_arra).
     */
    public Map<String, List<CollectionResponse.ItemEntry>> parse(String rawUsername, String json)
    {
        final String username = rawUsername.toLowerCase();

        log.debug("🧹 Starting parse() for user: {}...", username);

        JsonElement root;

        // Log the raw JSON for debugging purposes
        log.debug("Raw JSON: {}", json);

        try {
            // Directly parse the JSON string using Gson
            root = gson.fromJson(json, JsonElement.class);

            // Check if the root element is a primitive (string, number, etc.)
            if (root.isJsonPrimitive()) {
                log.error("❌ The response for user {} is a primitive value: {}", username, root.getAsString());
                return null;
            }

            // Check if the root element is a JsonObject
            if (root.isJsonObject()) {
                log.debug("The root is a valid JsonObject.");
            }

        } catch (JsonSyntaxException e) {
            log.error("❌ Failed to parse JSON for {}: {}", username, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("❌ Unexpected error while parsing JSON for {}: {}", username, e.getMessage());
            return null;
        }

        JsonObject rootObject = root.getAsJsonObject();

        // Handle error response
        if (rootObject.has("error")) {
            JsonObject error = rootObject.getAsJsonObject("error");
            String errorMessage = error.get("Message").getAsString();

            if (errorMessage.contains("Player has not synced")){
                log.warn("⚠️ Player {} has not synced their collection log yet.", username);
            } else {
                log.warn("⚠️ API error for {}: {}", username, errorMessage);
            }

            return null; // Stop further processing for this player
        }

        // Handle success response
        if (rootObject.has("data")) {
            Map<String, List<CollectionResponse.ItemEntry>> itemMap = new HashMap<>();
            JsonObject data = rootObject.getAsJsonObject("data");
            JsonObject items = data.getAsJsonObject("items");

            int categoryCount = 0;
            int itemCount = 0;

            for (Map.Entry<String, JsonElement> category : items.entrySet()) {
                String categoryName = category.getKey();
                JsonArray itemArray = category.getValue().getAsJsonArray();

                log.debug("📦 Parsing category: {} ({} items)", categoryName, itemArray.size());
                categoryCount++;

                List<CollectionResponse.ItemEntry> entryList = new ArrayList<>();

                for (JsonElement e : itemArray) {
                    CollectionResponse.ItemEntry item = gson.fromJson(e, CollectionResponse.ItemEntry.class);
                    log.debug("➡️ Queuing: [{}] {} x{} @ {}", categoryName, item.name, item.count, item.date);
                    entryList.add(item);
                    itemCount++;
                }

                itemMap.put(categoryName, entryList);
            }

            log.debug("✅ Parsed {} categories and {} items for {}.", categoryCount, itemCount, username);

            return itemMap;
        }

        return null;
    }

    /**
     * Parses Temple's collection log response and persists it to the local database.
     * @param rawUsername The username associated with the collection log data
     * @param items The collection log item map to persist to the database, where the key is the API category
     */
    public void store(String rawUsername, Map<String, List<CollectionResponse.ItemEntry>> items)
    {
        final String username = rawUsername.toLowerCase();

        if (items == null) {
            log.warn("⚠️ No items found to store for {}", username);
            return;
        }

        log.debug("🧹 Starting store() for user: {}...", username);

        int categoryCount = items.size();
        int itemCount = items.entrySet()
                .stream()
                .mapToInt(item -> items.size())
                .sum();

        CollectionDatabase.insertItemsBatch(username, items);

        log.debug("✅ Parsed {} categories and inserted {} items total for {}.", categoryCount, itemCount, username);

        // ✅ Manually shut down the database after insert
        try (Connection conn = CollectionDatabase.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SHUTDOWN");
            log.debug("🚗 Manually closed H2 database after sync.");
        } catch (SQLException e) {
            log.error("⚠️ Error while trying to shut down the database", e);
        }
    }

    /**
     * Combines the parse and store methods for ease of use.
     * @see #parse(String username, String json)
     * @see #store(String username, Map collectionLogItems)
     */
    public void parseAndStore(String rawUsername, String json)
    {
        store(rawUsername, parse(rawUsername, json));
    }
}