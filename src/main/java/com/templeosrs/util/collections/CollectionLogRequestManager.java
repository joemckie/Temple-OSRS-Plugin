package com.templeosrs.util.collections;

import com.templeosrs.util.api.RequestManager;
import com.templeosrs.util.collections.autosync.PlayerDataSync;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;

public class CollectionLogRequestManager extends RequestManager {
    /**
     * Uploads newly obtained collection log items to the server.
     * 
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
                .addPathSegments("api/collection-log/sync_new_collections.php")
                .build();

        post(url, data, callback);
    }

    /**
     * Uploads the full collection log to the server.
     * 
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
     * 
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
}
