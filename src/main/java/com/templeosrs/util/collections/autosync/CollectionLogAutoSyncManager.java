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
        pendingSyncItems.add(ItemID.TWISTED_BOW);
        startSyncCountdown();
        
        eventBus.register(this);
        collectionLogAutoSyncChatMessageSubscriber.startUp();
        collectionLogAutoSyncItemContainerChangedSubscriber.startUp();
    }

    public void shutDown()
    {
        eventBus.unregister(this);
        collectionLogAutoSyncChatMessageSubscriber.shutDown();
        collectionLogAutoSyncItemContainerChangedSubscriber.shutDown();
    }
    
    public void startSyncCountdown()
    {
        gameTickToSync = client.getTickCount() + 17; // Roughly equates to a 10-second wait
    }
    
    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        if (gameTickToSync != null && client.getTickCount() >= gameTickToSync && !pendingSyncItems.isEmpty()) {
            gameTickToSync = null;
            scheduledExecutorService.execute(this::uploadObtainedCollectionLogItems);
        }
    }
    
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
        
        requestManager.uploadNewCollectionLogItems(submission, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.debug("Failed to submit: ", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    if (!response.isSuccessful()) {
                        log.debug("Failed to submit: {}", response.code());
                    }
                } finally {
                    response.close();
                }
            }
        });

    }
}
