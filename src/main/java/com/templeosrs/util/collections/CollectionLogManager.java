/*
 * Copyright (c) 2025, andmcadams
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.templeosrs.util.collections;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.templeosrs.TempleOSRSConfig;
import com.templeosrs.TempleOSRSPlugin;
import com.templeosrs.util.collections.autosync.CollectionLogAutoSyncManager;
import com.templeosrs.util.collections.chatcommands.CollectionLogChatCommandChatMessageSubscriber;
import com.templeosrs.util.collections.data.*;
import com.templeosrs.util.collections.database.CollectionDatabase;
import com.templeosrs.util.collections.services.CollectionLogService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class CollectionLogManager {
    private Manifest manifest;
    private final Map<PlayerProfile, PlayerData> playerDataMap = new HashMap<>();
    private int cyclesSinceSuccessfulCall = 0;

    /**
     * Keeps track of what collection log slots the user has set
     */
    @Getter
    protected final BitSet clogItemsBitSet = new BitSet();

    /**
     * Keeps track of the item count for each collection log item
     */
    @Getter
    protected final Map<Integer, Integer> clogItemsCountSet = new HashMap<>();

    private int tickCollectionLogScriptFired = -1;

    /**
     * List of items found in the collection log, computed by reading the in-game enums/structs.
     */
    private final Map<Integer, CollectionItem> collectionLogItemMap = new HashMap<>();

    private final Map<Integer, ObtainedCollectionItem> obtainedCollectionLogItems = new HashMap<>();

    @Inject
    private SyncButtonManager syncButtonManager;

    @Inject
    private ScheduledExecutorService scheduledExecutorService;

    @Inject
    private Gson gson;

    @Getter
    @Inject
    private Client client;

    @Getter
    @Inject
    private ClientThread clientThread;

    @Inject
    private EventBus eventBus;

    @Inject
    private CollectionLogAutoSyncManager collectionLogAutoSyncManager;

    @Inject
    private TempleOSRSPlugin templeOSRSPlugin;

    @Inject
    private CollectionLogRequestManager requestManager;

    @Inject
    private CollectionLogService collectionLogService;

    @Inject
    private CollectionLogChatCommandChatMessageSubscriber collectionLogChatCommandChatMessageSubscriber;

    @Getter
    @Setter
    private boolean syncAllowed;

    public void startUp() {
        eventBus.register(this);

        CollectionDatabase.init();

        if (templeOSRSPlugin.getConfig().enableClogChatCommand()) {
            collectionLogChatCommandChatMessageSubscriber.startUp();
        }

        if (templeOSRSPlugin.getConfig().autoSyncClog()) {
            collectionLogAutoSyncManager.startUp();
        }

        clientThread.invoke(() -> {
            if (client.getIndexConfig() == null || client.getGameState().ordinal() < GameState.LOGIN_SCREEN.ordinal()) {
                return false;
            }

            collectionLogItemMap.putAll(parseCacheForClog());

            return true;
        });

        checkManifest();
    }

    public void shutDown() {
        eventBus.unregister(this);

        if (templeOSRSPlugin.getConfig().enableClogChatCommand()) {
            collectionLogChatCommandChatMessageSubscriber.shutDown();
        }

        if (templeOSRSPlugin.getConfig().autoSyncClog()) {
            collectionLogAutoSyncManager.shutDown();
        }

        clogItemsBitSet.clear();
        clogItemsCountSet.clear();
        syncButtonManager.shutDown();
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged configChanged)
    {
        if (!configChanged.getGroup().equals(TempleOSRSConfig.TEMPLE_OSRS_CONFIG_GROUP)) {
            return;
        }

        if (templeOSRSPlugin.getConfig().autoSyncClog()) {
            collectionLogAutoSyncManager.startUp();
        } else {
            collectionLogAutoSyncManager.shutDown();
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        // Submit the collection log data two ticks after the first script prefires
        if (tickCollectionLogScriptFired != -1 && tickCollectionLogScriptFired + 2 < client.getTickCount()) {
            tickCollectionLogScriptFired = -1;

            if (manifest == null) {
                client.addChatMessage(ChatMessageType.CONSOLE, "TempleOSRS", "Failed to sync collection log. Try restarting the TempleOSRS plugin.", "TempleOSRS");
                return;
            }

            scheduledExecutorService.execute(this::submitTask);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        GameState state = gameStateChanged.getGameState();

        switch (state) {
            // When hopping, we need to clear any state related to the player
            case HOPPING:
            case LOGGING_IN:
            case CONNECTION_LOST:
                clogItemsBitSet.clear();
                clogItemsCountSet.clear();
                break;
            case LOGGED_IN: {
                // Attempt to synchronise the player's collection log on login
                clientThread.invokeLater(() -> {
                    final String username = client.getLocalPlayer().getName();

                    // Wait for username to be available
                    if (username == null) {
                        return false;
                    }

                    // Skip sync if the player's collection log has already been saved and is up-to-date
                    if (collectionLogService.isDataFresh(username) && CollectionDatabase.hasPlayerData(username)) {
                        return true;
                    }

                    collectionLogService.syncCollectionLog();

                    return true;
                });
            }
        }
    }

    /**
     * Handles updating the collection log after the log has opened or the Temple sync button has been pressed.
     */
    @Subscribe
    public void onScriptPreFired(ScriptPreFired preFired) {
        if (preFired.getScriptId() == 4100) {
            if (collectionLogItemMap.isEmpty()) {
                return;
            }

            if (isSyncAllowed()) {
                tickCollectionLogScriptFired = client.getTickCount();
            }

            Object[] args = preFired.getScriptEvent().getArguments();
            int itemId = (int) args[1];
            int itemCount = (int) args[2];

            ObtainedCollectionItem item = ObtainedCollectionItem.builder()
                .count(itemCount)
                .category(collectionLogItemMap.get(itemId).getCategory())
                .itemId(itemId)
                .build();

            obtainedCollectionLogItems.put(itemId, item);
        }
    }

    synchronized public void submitTask() {
        // TODO: do we want other GameStates?
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null) {
            return;
        }

        String username = client.getLocalPlayer().getName();

        if (username == null) {
            return;
        }

        RuneScapeProfileType profileType = RuneScapeProfileType.getCurrent(client);
        PlayerProfile profileKey = new PlayerProfile(username, profileType);

        // If player data exists and auto-sync is enabled, check to see if any items have changed that require a sync
        final Multiset<Integer> collectionLogItemIdCountMap = HashMultiset.create();

        for (Map.Entry<Integer, ObtainedCollectionItem> item : obtainedCollectionLogItems.entrySet())
        {
            final int itemId = item.getValue().getItemId();
            final int itemCount = item.getValue().getCount();

            collectionLogItemIdCountMap.add(itemId, itemCount);
        }

        final Multiset<Integer> itemDiff = CollectionDatabase.getCollectionLogDiff(username, collectionLogItemIdCountMap);

        if (itemDiff == null) {
            return;
        }

        // If the local data is out of sync with the current collection log, update it
        if (!itemDiff.isEmpty()) {
            List<Map.Entry<Integer, ObtainedCollectionItem>> itemsToAdd = obtainedCollectionLogItems.entrySet()
                    .stream()
                    .filter(item -> itemDiff.contains(item.getKey()))
                    .collect(Collectors.toList());

            collectionLogAutoSyncManager.resetSyncCountdown();

            // Save the current state of the player's collection log for future diffing
            CollectionDatabase.upsertPlayerCollectionLogItems(username, itemsToAdd);
        }

        // If sync hasn't been toggled to be allowed
        if (!isSyncAllowed()) {
            return;
        }

        final boolean hasPlayerData = CollectionDatabase.hasPlayerData(username);

        // If no API player data exists, this is the first sync, so send everything.
        if (!hasPlayerData) {
            PlayerData newPlayerData = getPlayerData();
            PlayerData oldPlayerData = playerDataMap.computeIfAbsent(profileKey, k -> new PlayerData());

            // Do not send if slot data wasn't generated
            if (newPlayerData.collectionLogSlots.isEmpty()) {
                return;
            }

            submitPlayerData(profileKey, newPlayerData, oldPlayerData);
        }
    }

    private PlayerData getPlayerData() {
        PlayerData out = new PlayerData();

        out.collectionLogSlots = Base64.getEncoder().encodeToString(clogItemsBitSet.toByteArray());
        out.collectionLogCounts = clogItemsCountSet;
        out.collectionLogItemCount = collectionLogItemMap.size();

        return out;
    }

    private void merge(PlayerData oldPlayerData, PlayerData delta) {
        oldPlayerData.collectionLogSlots = delta.collectionLogSlots;
        oldPlayerData.collectionLogItemCount = delta.collectionLogItemCount;
    }

    private void submitPlayerData(PlayerProfile profileKey, PlayerData delta, PlayerData old) {
        // If cyclesSinceSuccessfulCall is not a perfect square, we should not try to submit.
        // This gives us quadratic backoff.
        cyclesSinceSuccessfulCall += 1;
        if (Math.pow((int) Math.sqrt(cyclesSinceSuccessfulCall), 2) != cyclesSinceSuccessfulCall) {
            return;
        }

        PlayerDataSubmission submission = new PlayerDataSubmission(
                profileKey.getUsername(),
                profileKey.getProfileType().name(),
                client.getAccountHash(),
                delta
        );

        requestManager.uploadFullCollectionLog(submission, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
				log.debug("Failed to submit: ", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    if (!response.isSuccessful()) {
						log.debug("Failed to submit: {}", response.code());
                        return;
                    }
                    merge(old, delta);
                    cyclesSinceSuccessfulCall = 0;
                } finally {
                    response.close();
                }
            }
        });
    }

    private void checkManifest() {
        requestManager.getCollectionLogManifest(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
//				log.debug("Failed to get manifest: ", e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful()) {
//						log.debug("Failed to get manifest: {}", response.code());
                        return;
                    }
                    InputStream in = response.body().byteStream();
                    manifest = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Manifest.class);
                } catch (JsonParseException e) {
//                    System.out.println("Failed to parse manifest,");
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * Parse the enums and structs in the cache to figure out which item ids
     * exist in the collection log. This can be diffed with the manifest to
     * determine the item ids that need to be appended to the end of the
     * bitset we send to the TempleOSRS server.
     */
    private Map<Integer, CollectionItem> parseCacheForClog() {
        Map<Integer, CollectionItem> items = new HashMap<>();
        // 2102 - Struct that contains the highest level tabs in the collection log (Bosses, Raids, etc)
        // https://chisel.weirdgloop.org/structs/index.html?type=enums&id=2102
        int[] topLevelTabStructIds = client.getEnum(2102).getIntVals();
        for (int topLevelTabStructIndex : topLevelTabStructIds) {
            // The collection log top level tab structs contain a param that points to the enum
            // that contains the pointers to sub tabs.
            // ex: https://chisel.weirdgloop.org/structs/index.html?type=structs&id=471
            StructComposition topLevelTabStruct = client.getStructComposition(topLevelTabStructIndex);

            // Param 683 contains the pointer to the enum that contains the subtabs ids
            // ex: https://chisel.weirdgloop.org/structs/index.html?type=enums&id=2103
            int[] subtabStructIndices = client.getEnum(topLevelTabStruct.getIntValue(683)).getIntVals();
            for (int subtabStructIndex : subtabStructIndices) {

                // The subtab structs are for subtabs in the collection log (Commander Zilyana, Chambers of Xeric, etc.)
                // and contain a pointer to the enum that contains all the item ids for that tab.
                // ex subtab struct: https://chisel.weirdgloop.org/structs/index.html?type=structs&id=476
                // ex subtab enum: https://chisel.weirdgloop.org/structs/index.html?type=enums&id=2109
                StructComposition subtabStruct = client.getStructComposition(subtabStructIndex);
                int[] clogItems = client.getEnum(subtabStruct.getIntValue(690)).getIntVals();

                for (int clogItemId : clogItems) {
                    CollectionItem item = CollectionItem.builder()
                        .category(subtabStructIndex)
                        .itemId(clogItemId)
                        .build();

                    items.put(clogItemId, item);
                }
            }
        }

        // Some items with data saved on them have replacements to fix a duping issue (satchels, flamtaer bag)
        // Enum 3721 contains a mapping of the item ids to replace -> ids to replace them with
        EnumComposition replacements = client.getEnum(3721);

        for (int badItemId : replacements.getKeys()) {
            items.remove(badItemId);
        }

        for (int goodItemId : replacements.getIntVals()) {
            CollectionItem item = CollectionItem.builder()
                .category(0)
                .itemId(goodItemId)
                .build();

            items.put(goodItemId, item);
        }

        return items;
    }
}
