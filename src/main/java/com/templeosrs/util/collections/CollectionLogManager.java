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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.templeosrs.TempleOSRSConfig;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class CollectionLogManager {
    private final int VARBITS_ARCHIVE_ID = 14;
    private final String CONFIG_GROUP = "TempleOSRS";
    private static final String PLUGIN_USER_AGENT = "TempleOSRS RuneLite Plugin Collection Log Sync - For any issues/abuse Contact 44mikael on Discord (https://www.templeosrs.com)";

    private static final String MANIFEST_URL = "https://templeosrs.com/collection-log/manifest.json";
    private static final String SUBMIT_URL = "https://templeosrs.com/api/collection-log/sync_collection.php";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final Map<Integer, VarbitComposition> varbitCompositions = new HashMap<>();

    private Manifest manifest;
    private final Map<PlayerProfile, PlayerData> playerDataMap = new HashMap<>();
    private int cyclesSinceSuccessfulCall = 0;

    // Keeps track of what collection log slots the user has set and map for their counts
    private static final BitSet clogItemsBitSet = new BitSet();
    private final Map<Integer, Integer> clogItemsCountSet = new HashMap<>();

    private static Integer clogItemsCount = null;

    // Map item ids to bit index in the bitset
    private static final HashMap<Integer, Integer> collectionLogItemIdToBitsetIndex = new HashMap<>();
    private int tickCollectionLogScriptFired = -1;
    private final HashSet<Integer> collectionLogItemIdsFromCache = new HashSet<>();

    private SyncButtonManager syncButtonManager;

    private final Multiset<Integer> inventoryItems = HashMultiset.create();

    @Getter
    @VisibleForTesting
    protected final HashSet<String> obtainedItemNames = new HashSet<>();

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private ScheduledExecutorService scheduledExecutorService;

    @Inject
    private Gson gson;

    @Inject
    private TempleOSRSConfig config;

    @Inject
    private ItemManager itemManager;

    @Getter
    @Inject
    private Client client;

    @Getter
    @Inject
    private ClientThread clientThread;

    @Inject
    private EventBus eventBus;

    @Inject
    private CollectionLogChatMessageSubscriber collectionLogChatMessageSubscriber;

    public void startUp(SyncButtonManager mainSyncButtonManager) {
        eventBus.register(this);

        collectionLogChatMessageSubscriber.startUp();

        // I'm not sure if this is how passing the same instance of a SyncButtonManager should be done, but it was the first solution that worked for me
        syncButtonManager = mainSyncButtonManager;

        clientThread.invoke(() -> {
            if (client.getIndexConfig() == null || client.getGameState().ordinal() < GameState.LOGIN_SCREEN.ordinal()) {
                return false;
            }
            collectionLogItemIdsFromCache.addAll(parseCacheForClog());
            populateCollectionLogItemIdToBitsetIndex();
            final int[] varbitIds = client.getIndexConfig().getFileIds(VARBITS_ARCHIVE_ID);
            for (int id : varbitIds) {
                varbitCompositions.put(id, client.getVarbit(id));
            }
            return true;
        });

        checkManifest();
    }

    public void shutDown() {
        eventBus.unregister(this);

        collectionLogChatMessageSubscriber.shutDown();

        clogItemsBitSet.clear();
        clogItemsCountSet.clear();
        clogItemsCount = null;
        syncButtonManager.shutDown();
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        // Submit the collection log data two ticks after the first script prefires
        if (tickCollectionLogScriptFired != -1 &&
                tickCollectionLogScriptFired + 2 < client.getTickCount()) {
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
                clogItemsCount = null;
                break;
        }
    }


    /**
     * Finds the index this itemId is assigned to in the collections mapping.
     *
     * @param itemId: The itemId to look up
     * @return The index of the bit that represents the given itemId, if it is in the map. -1 otherwise.
     */
    private int lookupCollectionLogItemIndex(int itemId) {
        // The map has not loaded yet, or failed to load.
        if (collectionLogItemIdToBitsetIndex.isEmpty()) {
            return -1;
        }
        Integer result = collectionLogItemIdToBitsetIndex.get(itemId);
        if (result == null) {
//			log.debug("Item id {} not found in the mapping of items", itemId);
            return -1;
        }
        return result;
    }

    /**
     * Handles updating the collection log after the Temple sync button has been pressed.
     */
    @Subscribe
    public void onScriptPreFired(ScriptPreFired preFired) {
        if (syncButtonManager.isSyncAllowed() && preFired.getScriptId() == 4100) {
            tickCollectionLogScriptFired = client.getTickCount();
            if (collectionLogItemIdToBitsetIndex.isEmpty()) {
                return;
            }
            clogItemsCount = collectionLogItemIdsFromCache.size();
            Object[] args = preFired.getScriptEvent().getArguments();
            int itemId = (int) args[1];
            int itemCount = (int) args[2];

            int idx = lookupCollectionLogItemIndex(itemId);
            // We should never return -1 under normal circumstances
            if (idx != -1) {
                clogItemsBitSet.set(idx);
                clogItemsCountSet.put(idx, itemCount);
            }
        }
    }



    @Subscribe
    private void onItemContainerChanged(ItemContainerChanged itemContainerChanged)
    {
        if (itemContainerChanged.getContainerId() != InventoryID.INV) {
            return;
        }

        ItemContainer inventory = itemContainerChanged.getItemContainer();
        Multiset<Integer> currentInventoryItems = HashMultiset.create();

        Arrays.stream(inventory.getItems()).forEach(
                item -> currentInventoryItems.add(item.getId(), item.getQuantity())
        );

        Multiset<Integer> inventoryDifference = Multisets.difference(currentInventoryItems, inventoryItems);

        for (Multiset.Entry<Integer> item : inventoryDifference.entrySet())
        {
            String itemName = itemManager.getItemComposition(item.getElement()).getName();
            int itemId = item.getElement();
            int itemCount = item.getCount();

            if (obtainedItemNames.contains(itemName)) {
                int index = lookupCollectionLogItemIndex(itemId);

                System.out.println(itemId);
                System.out.println(itemCount);
                System.out.println(index);

                clogItemsBitSet.set(index);
                clogItemsCountSet.put(itemId, itemCount);
                obtainedItemNames.remove(itemName);
            }
        }

        inventoryItems.clear();
        inventoryItems.addAll(currentInventoryItems);
    }

    synchronized public void submitTask() {
        // If sync hasn't been toggled to be allowed
        if (!syncButtonManager.isSyncAllowed()) {
            return;
        }

        // TODO: do we want other GameStates?
        if (client.getGameState() != GameState.LOGGED_IN || varbitCompositions.isEmpty()) {
            return;
        }

        if (manifest == null || client.getLocalPlayer() == null) {
//			log.debug("Skipped due to bad manifest: {}", manifest);
            return;
        }

        String username = client.getLocalPlayer().getName();
        RuneScapeProfileType profileType = RuneScapeProfileType.getCurrent(client);
        PlayerProfile profileKey = new PlayerProfile(username, profileType);

        PlayerData newPlayerData = getPlayerData();
        PlayerData oldPlayerData = playerDataMap.computeIfAbsent(profileKey, k -> new PlayerData());


        // Uncomment below WikiSync code if we ever want to disable sync requests with no item changes
        // For now item counts might still change even if no new items were obtained - might be worth looking into returning here if not even item count changes

        // Subtraction is done in place so newPlayerData becomes a map of only changed fields
/*
        subtract(newPlayerData, oldPlayerData);

        if (newPlayerData.isEmpty()) {
            return;
        }*/

        // Do not send if slot data wasn't generated
        if (newPlayerData.collectionLogSlots.isEmpty()) {
            return;
        }


        submitPlayerData(profileKey, newPlayerData, oldPlayerData);
    }

    public void manifestTask() {
        if (client.getGameState() == GameState.LOGGED_IN) {
            checkManifest();
        }
    }

    private PlayerData getPlayerData() {
        PlayerData out = new PlayerData();

        out.collectionLogSlots = Base64.getEncoder().encodeToString(clogItemsBitSet.toByteArray());
        out.collectionLogCounts = clogItemsCountSet;

        out.collectionLogItemCount = clogItemsCount;
        return out;
    }

    private void subtract(PlayerData newPlayerData, PlayerData oldPlayerData) {
        if (newPlayerData.collectionLogSlots.equals(oldPlayerData.collectionLogSlots))
            newPlayerData.clearCollectionLog();
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

        Request request = new Request.Builder()
                .addHeader("User-Agent", PLUGIN_USER_AGENT)
                .url(SUBMIT_URL)
                .post(RequestBody.create(JSON, gson.toJson(submission)))
                .build();

        Call call = okHttpClient.newCall(request);
        call.timeout().timeout(3, TimeUnit.SECONDS);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
//				log.debug("Failed to submit: ", e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful()) {
//						log.debug("Failed to submit: {}", response.code());
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
        Request request = new Request.Builder()
                .addHeader("User-Agent", PLUGIN_USER_AGENT)
                .url(MANIFEST_URL)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
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
                    populateCollectionLogItemIdToBitsetIndex();
                } catch (JsonParseException e) {
//                    System.out.println("Failed to parse manifest,");
                } finally {
                    response.close();
                }
            }
        });
    }

    private void populateCollectionLogItemIdToBitsetIndex() {
        if (manifest == null) {
//			System.out.println("Manifest is not present so the collection log bitset index will not be updated");
            return;
        }
        clientThread.invoke(() -> {
            // Add missing keys in order to the map. Order is extremely important here so
            // we get a stable map given the same cache data.
            List<Integer> itemIdsMissingFromManifest = collectionLogItemIdsFromCache
                    .stream()
                    .filter((t) -> !manifest.collections.contains(t))
                    .sorted()
                    .collect(Collectors.toList());

            int currentIndex = 0;
            collectionLogItemIdToBitsetIndex.clear();
            for (Integer itemId : manifest.collections)
                collectionLogItemIdToBitsetIndex.put(itemId, currentIndex++);
            for (Integer missingItemId : itemIdsMissingFromManifest) {
                collectionLogItemIdToBitsetIndex.put(missingItemId, currentIndex++);
            }
        });
    }

    /**
     * Parse the enums and structs in the cache to figure out which item ids
     * exist in the collection log. This can be diffed with the manifest to
     * determine the item ids that need to be appended to the end of the
     * bitset we send to the TempleOSRS server.
     */
    private HashSet<Integer> parseCacheForClog() {
        HashSet<Integer> itemIds = new HashSet<>();
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
                for (int clogItemId : clogItems) itemIds.add(clogItemId);
            }
        }

        // Some items with data saved on them have replacements to fix a duping issue (satchels, flamtaer bag)
        // Enum 3721 contains a mapping of the item ids to replace -> ids to replace them with
        EnumComposition replacements = client.getEnum(3721);
        for (int badItemId : replacements.getKeys())
            itemIds.remove(badItemId);
        for (int goodItemId : replacements.getIntVals())
            itemIds.add(goodItemId);

        return itemIds;
    }


    private int getVarbitValue(int varbitId) {
        VarbitComposition v = varbitCompositions.get(varbitId);
        if (v == null) {
            return -1;
        }

        int value = client.getVarpValue(v.getIndex());
        int lsb = v.getLeastSignificantBit();
        int msb = v.getMostSignificantBit();
        int mask = (1 << ((msb - lsb) + 1)) - 1;
        return (value >> lsb) & mask;
    }
}
