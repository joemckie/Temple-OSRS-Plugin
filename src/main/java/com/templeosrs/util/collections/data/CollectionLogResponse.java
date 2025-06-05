package com.templeosrs.util.collections.data;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

import java.util.Set;

@Value
public class CollectionLogResponse {
    @Value
    public static
    class Data {
        String player;
        @SerializedName("last_changed") String lastChanged;
        Set<ObtainedCollectionItem> items;
    }

    @Value
    public static
    class Error {
        int code;
        String message;
    }

    Data data;
    Error error;
}
