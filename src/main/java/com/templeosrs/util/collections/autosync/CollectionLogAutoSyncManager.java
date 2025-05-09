package com.templeosrs.util.collections.autosync;

import com.google.gson.Gson;
import com.templeosrs.util.collections.CollectionLogRequestManager;
import com.templeosrs.util.collections.PlayerProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class CollectionLogAutoSyncManager {
    
    @Inject
    private CollectionLogAutoSyncChatMessageSubscriber collectionLogAutoSyncChatMessageSubscriber;

    @Inject
    private CollectionLogAutoSyncItemContainerChangedSubscriber collectionLogAutoSyncItemContainerChangedSubscriber;

    @Inject
    private CollectionLogAutoSyncNpcLootReceivedSubscriber collectionLogAutoSyncNpcLootReceivedSubscriber;
    
    @Inject
    private OkHttpClient okHttpClient;
    
    @Inject
    private Gson gson;
    
    @Inject
    private Client client;
    
    @Inject
    private ScheduledExecutorService scheduledExecutorService;
    
    @Inject
    private EventBus eventBus;

    @Inject
    private CollectionLogRequestManager requestManager;

    @Getter
    protected final HashSet<String> obtainedItemNames = new HashSet<>();
    
    @Getter
    @Nullable
    private Integer gameTickToSync;

    /**
     * Keeps track of what item IDs are pending a server sync
     */
    @Getter
    protected final HashSet<Integer> pendingSyncItems = new HashSet<>();

    public void startUp()
    {
        obtainedItemNames.add("Bones");

        eventBus.register(this);
        collectionLogAutoSyncChatMessageSubscriber.startUp();
        collectionLogAutoSyncItemContainerChangedSubscriber.startUp();
        collectionLogAutoSyncNpcLootReceivedSubscriber.startUp();
    }

    public void shutDown()
    {
        eventBus.unregister(this);
        collectionLogAutoSyncChatMessageSubscriber.shutDown();
        collectionLogAutoSyncItemContainerChangedSubscriber.shutDown();
        collectionLogAutoSyncNpcLootReceivedSubscriber.shutDown();
    }
    
    /**
     * Starts the sync countdown.
     * 
     * This utilises a 17-tick delay (which corresponds to a roughly 10-second wait) as a way to batch requests.
     * This is useful for when multiple items are obtained in quick succession, such as Chompy Hats.
     */
    public void startSyncCountdown()
    {
        gameTickToSync = client.getTickCount() + 17;
    }
    
    /**
     * Listens for game ticks and checks if the sync countdown has completed.
     * 
     * Once the countdown is complete, and there are items pending a sync
     * it will upload any newly obtained items to the server.
     * 
     * @param gameTick
     */
    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        // Note: There shouldn't be an instance of gameTickToSync
        // being non-null without items in the pendingSyncItems set.
        if (gameTickToSync == null || pendingSyncItems.isEmpty()) {
            return;
        }

        // Sync new collection log items when the game has reached correct tick.
        if (client.getTickCount() >= gameTickToSync) {
            gameTickToSync = null;
            scheduledExecutorService.execute(this::uploadObtainedCollectionLogItems);
        }
    }
    
    /**
     * Uploads the obtained collection log items to the server.
     * 
     * This is called when the sync countdown has completed and there are items pending a sync.
     */
    synchronized private void uploadObtainedCollectionLogItems()
    {
        String username = client.getLocalPlayer().getName();
        RuneScapeProfileType profileType = RuneScapeProfileType.getCurrent(client);
        PlayerProfile profileKey = new PlayerProfile(username, profileType);
        
        PlayerDataSync submission = new PlayerDataSync(
                profileKey.getUsername(),
                profileKey.getProfileType().name(),
                client.getAccountHash(),
                pendingSyncItems
        );
        
        requestManager.uploadObtainedCollectionLogItems(submission, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                // log.debug("Failed to submit: ", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    if (!response.isSuccessful()) {
                        // log.debug("Failed to submit: {}", response.code());
                        return;
                    }

                    obtainedItemNames.clear();
                    pendingSyncItems.clear();
                } finally {
                    response.close();
                }
            }
        });

    }
}
