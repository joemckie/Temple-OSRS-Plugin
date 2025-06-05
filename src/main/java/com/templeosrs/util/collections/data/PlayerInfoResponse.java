package com.templeosrs.util.collections.data;

import com.google.gson.annotations.SerializedName;
import com.templeosrs.util.api.APIError;
import lombok.Value;

import javax.annotation.Nullable;

@Value
public class PlayerInfoResponse {
    @Value
    public static class CollectionLog {
        @SerializedName("last_changed")
        String lastChanged;
    }

    @Value
    public static class Data {
        @SerializedName("collection_log")
        CollectionLog collectionLog;
    }

    @Nullable
    Data data;

    @Nullable
    APIError error;
}
