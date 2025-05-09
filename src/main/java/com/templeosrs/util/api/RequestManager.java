package com.templeosrs.util.api;

import com.google.gson.Gson;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import java.util.concurrent.TimeUnit;

import static net.runelite.http.api.RuneLiteAPI.JSON;

public class RequestManager {
    @Inject
    Gson gson;

    @Inject
    OkHttpClient okHttpClient;

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
     * Initiates a request with no timeout.
     * @param request The request to be sent.
     * @param callback The callback to handle the response.
     */
    private void doRequest(Request request, Callback callback)
    {
        okHttpClient.newCall(request).enqueue(callback);
    }

    /**
     * Initiates a request with the given timeout.
     * @param request The request to be sent.
     * @param callback The callback to handle the response.
     */
    private void doRequest(Request request, Callback callback, long timeout)
    {
        Call call = okHttpClient.newCall(request);
        call.timeout().timeout(timeout, TimeUnit.SECONDS);
        call.enqueue(callback);
    }

    /**
     * Initiates a GET request.
     * @param url The URL to send the request to.
     * @param callback The callback to handle the response.
     */
    protected void get(@NotNull HttpUrl url, Callback callback)
    {
        final Request request = buildRequest(url).get().build();

        doRequest(request, callback);
    }

    /**
     * Initiates a POST request with the given data.
     * @param url The URL to send the request to.
     * @param data The data to be sent in the request body.
     * @param callback The callback to handle the response.
     */
    protected void post(@NotNull HttpUrl url, @NotNull Object data, Callback callback)
    {
        final Request request = buildRequest(url)
            .post(RequestBody.create(JSON, gson.toJson(data)))
            .build();

        doRequest(request, callback, 3);
    };
}
