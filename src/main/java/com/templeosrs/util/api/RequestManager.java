package com.templeosrs.util.api;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static net.runelite.http.api.RuneLiteAPI.JSON;

@Slf4j
public class RequestManager {
    @Inject
    private Gson gson;

    @Inject
    private OkHttpClient okHttpClient;

    protected final String scheme = "https";

    protected final String host = "templeosrs.com";

    private Request.Builder buildRequest(@NotNull HttpUrl url)
    {
        String PLUGIN_USER_AGENT = "TempleOSRS RuneLite Plugin Collection Log Sync - For any issues/abuse Contact 44mikael on Discord (https://www.templeosrs.com)";

        return new Request.Builder()
            .addHeader("User-Agent", PLUGIN_USER_AGENT)
            .url(url);
    }

    /**
     * Initiates a synchronous request with no timeout.
     * @param request The request to be sent.
     * @return The request data.
     * @throws IOException The request error.
     */
    private String doRequest(Request request) throws IOException {
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(String.format("HTTP error fetching %s: %s", request.url(), response.code()));
            }

            String body = Objects.requireNonNull(response.body()).string();

            if (body.isEmpty()) {
                throw new IOException(String.format("Empty response body was returned from %s", request.url()));
            }

            return body;
        }
    }

    /**
     * Initiates a synchronous GET request.
     * @param url The URL to send the request to.
     */
    protected String get(@NotNull HttpUrl url) throws IOException {
        final Request request = buildRequest(url).get().build();

        return doRequest(request);
    }

    /**
     * Initiates a synchronous POST request with the given data.
     *
     * @param url  The URL to send the request to.
     * @param data The data to be sent in the request body.
     */
    protected String post(@NotNull HttpUrl url, @NotNull Object data) throws IOException {
        final Request request = buildRequest(url)
                .post(RequestBody.create(JSON, gson.toJson(data)))
                .build();

        return doRequest(request);
    }
}
