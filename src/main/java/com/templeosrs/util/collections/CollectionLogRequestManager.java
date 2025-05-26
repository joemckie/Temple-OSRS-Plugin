package com.templeosrs.util.collections;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.templeosrs.util.api.RequestManager;
import com.templeosrs.util.collections.autosync.PlayerDataSync;
import com.templeosrs.util.collections.data.PlayerDataSubmission;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.StringReader;

@Slf4j
public class CollectionLogRequestManager extends RequestManager {
    @Inject
    private Gson gson;

    /**
     * Uploads newly obtained collection log items to the server.
     * Used by the auto-sync feature to automatically synchronise the collection log.
     * 
     * @param data The data to be uploaded.
     * @param callback The callback to handle the response.
     */
    public void uploadObtainedCollectionLogItems(@NotNull PlayerDataSync data, Callback callback)
    {
        final HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .port(3000)
                .addPathSegments("api/sync")
                .build();

        post(url, data, callback);
    }

    /**
     * Uploads the full collection log to the server.
     * Triggered by the Collection Log Sync button.
     * 
     * @param data The data to be uploaded.
     * @param callback The callback to handle the response.
     */
    public void uploadFullCollectionLog(@NotNull PlayerDataSubmission data, Callback callback)
    {
        final HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments("api/collection-log/sync_collection.php")
                .build();

        post(url, data, callback);
    }

    /**
     * Retrieves the collection log manifest from the server.
     * The manifest contains information that maps the collection log items to their respective IDs/varbits.
     * 
     * @param callback The callback to handle the response.
     */
    public void getCollectionLogManifest(Callback callback)
    {
        final HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments("collection-log/manifest.json")
                .build();

        get(url, callback);
    }

    /**
     * Retrieves the time the user's collection log last changed from the Player Info endpoint
     *
     * @param username The username to check
     */
    public String getLastChangedTimestamp(String username) {
        final HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments("api/player_info.php")
                .addQueryParameter("player", username)
                .addQueryParameter("cloginfo", "1")
                .build();

        try {
            String response = get(url);

            JsonReader reader = new JsonReader(new StringReader(response));
            reader.setLenient(true);

            JsonElement element = gson.fromJson(reader, JsonElement.class);

            if (element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();

                if (root.has("data")) {
                    JsonObject data = root.getAsJsonObject("data");

                    if (data.has("collection_log")) {
                        JsonObject collectionLog = data.getAsJsonObject("collection_log");

                        if (collectionLog.has("last_changed")) {
                            return collectionLog.get("last_changed").getAsString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to get last_changed for {}: {}", username, e.getMessage());
        }

        return null;
    }

    public String getPlayerCollectionLog(String username)
    {
        final HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments("api/collection-log/player_collection_log.php")
                .addQueryParameter("player", username)
                .addQueryParameter("categories", "all")
                .build();

        try {
            String response = get(url);

            if (response.contains("\"Code\":402") && response.contains("has not synced")) {
                return "error:unsynced";
            }

            return response;
        } catch (Exception e) {
            log.error("❌ Exception while fetching log for {}: {}", username, e.getMessage());

            return null;
        }
    }
}
