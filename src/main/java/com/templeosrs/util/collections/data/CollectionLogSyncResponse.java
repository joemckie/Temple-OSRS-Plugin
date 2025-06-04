package com.templeosrs.util.collections.data;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

@Value
public class CollectionLogSyncResponse {
    @Value
    public static class Data {
        @SerializedName("sync_date") String syncDate;
        @SerializedName("sync_date_unix") int syncDateUnix;
        @SerializedName("last_changed") String lastChanged;
        @SerializedName("last_changed_unix") int lastChangedUnix;
        String username;
        String message;
    }

    @Value
    public static class Error {
        int code;
        String message;

        public String toString() {
            return "HTTP " + code + ": " + message;
        }
    }

    Data data;
    Error error;
}
