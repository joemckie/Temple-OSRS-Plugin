package com.templeosrs.util.collections.parser;

import com.google.gson.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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

    public void parseAndStore(String rawUsername, String json) {
        final String username = rawUsername.toLowerCase();

        log.debug("🧹 Starting parseAndStore() for user: {}...", username);

        JsonElement root;

        // Log the raw JSON for debugging purposes
        log.debug("Raw JSON: {}", json);

        try {
            // Directly parse the JSON string using Gson
            root = gson.fromJson(json, JsonElement.class);

            // Check if the root element is a primitive (string, number, etc.)
            if (root.isJsonPrimitive()) {
                log.error("❌ The response for user {} is a primitive value: {}", username, root.getAsString());
                return;
            }

            // Check if the root element is a JsonObject
            if (root.isJsonObject()) {
                log.debug("The root is a valid JsonObject.");
            }

        } catch (JsonSyntaxException e) {
            log.error("❌ Failed to parse JSON for {}: {}", username, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("❌ Unexpected error while parsing JSON for {}: {}", username, e.getMessage());
            return;
        }

        JsonObject rootObject = root.getAsJsonObject();

        // Handle error response
        if (rootObject.has("error")) {
            JsonObject error = rootObject.getAsJsonObject("error");
            String errorMessage = error.get("Message").getAsString();
            if (errorMessage.contains("Player has not synced")) {
                log.warn("⚠️ Player {} has not synced their collection log yet.", username);
            } else {
                log.warn("⚠️ API error for {}: {}", username, errorMessage);
            }
            return; // Stop further processing for this player
        }

        // Handle success response
        if (rootObject.has("data")) {
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

                // ✅ Perform batch insert for the whole category with player name
                CollectionDatabase.insertItemsBatch(username.toLowerCase(), categoryName, entryList);
            }

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
    }
}