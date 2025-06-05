package com.templeosrs.util.api;

import lombok.Value;

@Value
public class APIError {
    int code;

    String message;

    public String toString() {
        return "HTTP " + code + ": " + message;
    }
}