package com.templeosrs.util.collections;

import com.google.gson.Gson;
import com.templeosrs.util.api.APIError;
import com.templeosrs.util.api.RequestManager;
import com.templeosrs.util.collections.autosync.PlayerDataSync;
import com.templeosrs.util.collections.data.PlayerDataSubmission;
import com.templeosrs.util.collections.data.PlayerInfoResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.IOException;

@Slf4j
public class CollectionLogRequestManager extends RequestManager {
    @Inject
    private Gson gson;

    /**
     * Uploads newly obtained collection log items to the server.
     * Used by the auto-sync feature to automatically synchronise the collection log.
     *
     * @param data The data to be uploaded.
     * @return The API response data
     */
    public String uploadObtainedCollectionLogItems(@NotNull PlayerDataSync data) throws IOException {
        final HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments("api/collection-log/sync_new_collections.php")
                .build();

        return post(url, data);
    }

    /**
     * Uploads the full collection log to the server.
     * Triggered by the Collection Log Sync button.
     *
     * @param data The data to be uploaded.
     */
    public void uploadFullCollectionLog(@NotNull PlayerDataSubmission data) throws IOException {
        final HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments("api/collection-log/sync_collection.php")
                .build();

        post(url, data);
    }

    /**
     * Retrieves player info from the Player Info endpoint
     *
     * @param username The username to check
     * @link <a href="https://templeosrs.com/api_doc.php#Player_Information">Player Info API</a>
     */
    @NotNull
    public PlayerInfoResponse.Data getPlayerInfo(@NotNull String username) throws IOException, NullPointerException {
        final HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments("api/player_info.php")
                .addQueryParameter("player", username)
                .addQueryParameter("cloginfo", "1")
                .addQueryParameter("formattedrsn", "1")
                .build();


        String response = get(url);

        PlayerInfoResponse playerInfoResponse = gson.fromJson(response, PlayerInfoResponse.class);
        PlayerInfoResponse.Data data = playerInfoResponse.getData();
        APIError error = playerInfoResponse.getError();

        if (error != null) {
            if (error.getCode() == 402) {
                throw new NullPointerException("Player has no TempleOSRS profile");
            }

            throw new IOException(String.valueOf(error));
        }

        if (data != null) {
            return data;
        }

        throw new IOException("Unexpected response format: " + response);
    }

    /**
     * Retrieves the given player's full collection log
     * @param username The username to query
     * @return The collection log data
     * @link <a href="https://templeosrs.com/api_doc.php#Player_Collection_Log">Player Collection Log API</a>
     */
    public String getPlayerCollectionLog(@NotNull String username)
    {
        final HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments("api/collection-log/player_collection_log.php")
                .addQueryParameter("player", username)
                .addQueryParameter("categories", "all")
                .addQueryParameter("includenames", "1")
                .addQueryParameter("onlyitems", "1")
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
