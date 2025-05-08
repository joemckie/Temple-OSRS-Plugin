package com.templeosrs.util.collections;

import com.templeosrs.util.api.RequestManager;
import com.templeosrs.util.collections.autosync.PlayerDataSync;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;

public class CollectionLogRequestManager extends RequestManager {
    public void uploadNewCollectionLogItems(@NotNull PlayerDataSync data, Callback callback)
    {
        final HttpUrl url = baseUrl.addPathSegment("api/sync_new_collections.php").build();

        post(url, data, callback);
    }

    public void uploadEntireCollectionLog(@NotNull PlayerDataSubmission data, Callback callback)
    {
        final HttpUrl url = baseUrl.addPathSegment("api/sync_new_collections.php").build();

        post(url, data, callback);
    }

    public void getCollectionLogManifest(Callback callback)
    {
        final HttpUrl url = baseUrl.addPathSegment("collection-log/manifest.json").build();

        get(url, callback);
    }
}
