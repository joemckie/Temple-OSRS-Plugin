package com.templeosrs.util.collections;

import com.templeosrs.util.api.RequestManager;
import com.templeosrs.util.collections.autosync.PlayerDataSync;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;

public class CollectionLogRequestManager extends RequestManager {
    public void uploadObtainedCollectionLogItems(@NotNull PlayerDataSync data, Callback callback)
    {
        final HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host("127.0.0.1")
                .port(3000)
                .addPathSegments("api/sync")
                .build();

        post(url, data, callback);
    }

    public void uploadFullCollectionLog(@NotNull PlayerDataSubmission data, Callback callback)
    {
        final HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments("api/collection-log/sync_collection.php")
                .build();

        post(url, data, callback);
    }

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
