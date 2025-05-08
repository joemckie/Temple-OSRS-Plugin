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

    private void doRequest(Request request, Callback callback)
    {
        okHttpClient.newCall(request).enqueue(callback);
    }

    private void doRequest(Request request, Callback callback, long timeout)
    {
        Call call = okHttpClient.newCall(request);
        call.timeout().timeout(timeout, TimeUnit.SECONDS);
        call.enqueue(callback);
    }

    protected void get(@NotNull HttpUrl url, Callback callback)
    {
        final Request request = buildRequest(url).get().build();

        doRequest(request, callback);
    }

    protected void post(@NotNull HttpUrl url, @NotNull Object data, Callback callback)
    {
        final Request request = buildRequest(url)
            .post(RequestBody.create(JSON, gson.toJson(data)))
            .build();

        doRequest(request, callback, 3);
    };
}
