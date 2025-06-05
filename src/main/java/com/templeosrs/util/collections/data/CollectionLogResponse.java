package com.templeosrs.util.collections.data;

import com.google.gson.annotations.SerializedName;
import com.templeosrs.util.api.APIError;
import lombok.Value;

import java.util.Set;

@Value
public class CollectionLogResponse {
    @Value
    public static class Data {
        String player;
        @SerializedName("last_changed") String lastChanged;
        Set<ObtainedCollectionItem> items;
    }

    Data data;
    APIError error;
}
