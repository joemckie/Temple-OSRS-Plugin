package com.templeosrs.util.collections.parser;

import com.google.gson.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;

import com.templeosrs.util.collections.data.CollectionLogResponse;
import com.templeosrs.util.collections.data.ObtainedCollectionItem;
import com.templeosrs.util.collections.database.CollectionDatabase;
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
     */
    public void parseAndStore(String rawUsername, String json)
    {
        final String username = rawUsername.toLowerCase();

        log.debug("🧹 Starting parse() for user: {}...", username);

        CollectionLogResponse collectionLogResponse;

        // Log the raw JSON for debugging purposes
        log.debug("Raw JSON: {}", json);

        try {
            // Directly parse the JSON string using Gson
            collectionLogResponse = gson.fromJson(json, CollectionLogResponse.class);
        } catch (JsonSyntaxException e) {
            log.error("❌ Failed to parse JSON for {}: {}", username, e.getMessage());

            return;
        } catch (Exception e) {
            log.error("❌ Unexpected error while parsing JSON for {}: {}", username, e.getMessage());

            return;
        }

        CollectionLogResponse.Data data = collectionLogResponse.getData();
        CollectionLogResponse.Error error = collectionLogResponse.getError();

        // Handle error response
        if (error != null) {
            String errorMessage = error.getMessage();

            if (errorMessage.contains("Player has not synced")){
                log.warn("⚠️ Player {} has not synced their collection log yet.", username);
            } else {
                log.warn("⚠️ API error for {}: {}", username, errorMessage);
            }

            return; // Stop further processing for this player
        }

        // Handle success response
        if (data != null) {
            Set<ObtainedCollectionItem> itemList = new HashSet<>();

            for (ObtainedCollectionItem item : data.getItems()) {
                log.debug("➡️ Queuing: {} x{} @ {}", item.getName(), item.getCount(), item.getDate());
                itemList.add(item);
            }

            log.debug("✅ Parsed {} items for {}.", itemList.size(), username);

            if (itemList.isEmpty()) {
                log.warn("⚠️ No items found to store for {}", username);

                return;
            }

            log.debug("🧹 Starting store() for user: {}...", username);

            CollectionDatabase.upsertItemsBatch(username, itemList, Timestamp.valueOf(data.getLastChanged()));

            int itemCount = itemList.size();

            log.debug("✅ Parsed and inserted {} items total for {}.", itemCount, username);

            // ✅ Manually shut down the database after insert
            try (Connection conn = CollectionDatabase.getConnection();
                 Statement stmt = conn.createStatement()
            ) {
                stmt.execute("SHUTDOWN");
                log.debug("🚗 Manually closed H2 database after sync.");
            } catch (SQLException e) {
                log.error("⚠️ Error while trying to shut down the database", e);
            }
        }
    }
}