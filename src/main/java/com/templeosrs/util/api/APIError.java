package com.templeosrs.util.api;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

@Value
public class APIError {
    @SerializedName("Code")
    int code;

    @SerializedName("Message")
    String message;

    public String toString() {
        return "HTTP " + code + ": " + message;
    }
}