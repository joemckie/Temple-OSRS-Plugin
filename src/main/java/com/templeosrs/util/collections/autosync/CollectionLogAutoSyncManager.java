package com.templeosrs.util.collections.autosync;

import com.templeosrs.util.collections.CollectionLogManager;
import com.templeosrs.util.collections.CollectionLogRequestManager;
import com.templeosrs.util.collections.data.ObtainedCollectionItem;
import com.templeosrs.util.collections.data.PlayerProfile;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import okhttp3.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashSet;

@Slf4j
public class CollectionLogAutoSyncManager {

    @Inject
    private CollectionLogAutoSyncChatMessageSubscriber collectionLogAutoSyncChatMessageSubscriber;

    @Inject
    private CollectionLogAutoSyncItemContainerChangedSubscriber collectionLogAutoSyncItemContainerChangedSubscriber;

    @Inject
    private CollectionLogAutoSyncNpcLootReceivedSubscriber collectionLogAutoSyncNpcLootReceivedSubscriber;

    @Inject
    private CollectionLogAutoSyncGameTickSubscriber collectionLogAutoSyncGameTickSubscriber;

    @Inject
    private CollectionLogAutoSyncConfigChecker collectionLogAutoSyncConfigChecker;
    
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;
    
    @Inject
    private EventBus eventBus;

    @Inject
    private CollectionLogRequestManager requestManager;

    @Inject
    private CollectionLogManager collectionLogManager;

    @Getter
    protected final HashSet<String> obtainedItemNames = new HashSet<>();
    
    @Getter
    @Nullable
    private Integer gameTickToSync;

    @Getter
    @Setter
    private boolean triggerSyncAllowed;

    /**
     * Keeps track of what item IDs are pending a server sync
     */
    @Getter
    protected final HashSet<ObtainedCollectionItem> pendingSyncItems = new HashSet<>();

    public void startUp()
    {
        eventBus.register(this);
        eventBus.register(collectionLogAutoSyncChatMessageSubscriber);
        eventBus.register(collectionLogAutoSyncItemContainerChangedSubscriber);
        eventBus.register(collectionLogAutoSyncNpcLootReceivedSubscriber);
        eventBus.register(collectionLogAutoSyncGameTickSubscriber);
        eventBus.register(collectionLogAutoSyncConfigChecker);

        collectionLogAutoSyncConfigChecker.startUp();
    }

    public void shutDown()
    {
        eventBus.unregister(this);
        eventBus.unregister(collectionLogAutoSyncChatMessageSubscriber);
        eventBus.unregister(collectionLogAutoSyncItemContainerChangedSubscriber);
        eventBus.unregister(collectionLogAutoSyncNpcLootReceivedSubscriber);
        eventBus.unregister(collectionLogAutoSyncGameTickSubscriber);
        eventBus.unregister(collectionLogAutoSyncConfigChecker);

        collectionLogAutoSyncConfigChecker.shutDown();
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded widgetLoaded)
    {
        if (widgetLoaded.getGroupId() == InterfaceID.COLLECTION) {
            setTriggerSyncAllowed(true);
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired scriptPostFired) {
        if (scriptPostFired.getScriptId() == 7797 && isTriggerSyncAllowed()) {
            clientThread.invokeLater(() -> {
                client.menuAction(-1, 40697932, MenuAction.CC_OP, 1, -1, "Search", null);
                client.menuAction(-1, 40697932, MenuAction.CC_OP, 1, -1, "Back", null);

                setTriggerSyncAllowed(false);

                collectionLogManager.setSyncAllowed(true);
            });
        }
    }


    /**
     * Starts the sync countdown.
     * This utilises a 17-tick delay (which corresponds to a roughly 10-second wait) as a way to batch requests.
     * This is useful for when multiple items are obtained in quick succession, such as Chompy Hats.
     */
    public void startSyncCountdown()
    {
        final int syncDelayInTicks = 17;

        gameTickToSync = client.getTickCount() + syncDelayInTicks;
    }

    /**
     * Resets the sync countdown.
     * Used after the request has successfully completed.
     */
    public void resetSyncCountdown()
    {
        gameTickToSync = null;
    }

    /**
     * Uploads the obtained collection log items to the server.
     * This is called when the sync countdown has completed and there are items pending a sync.
     */
    synchronized public void uploadObtainedCollectionLogItems()
    {
        String username = client.getLocalPlayer().getName();
        RuneScapeProfileType profileType = RuneScapeProfileType.getCurrent(client);
        PlayerProfile profileKey = new PlayerProfile(username, profileType);
        
        PlayerDataSync submission = new PlayerDataSync(
                profileKey.getUsername(),
                profileKey.getProfileType().name(),
                client.getAccountHash(),
                pendingSyncItems.toArray()
        );

        requestManager.uploadObtainedCollectionLogItems(submission, new Callback() {
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

                    obtainedItemNames.clear();
                    pendingSyncItems.clear();
                } finally {
                    response.close();
                }
            }
        });
    }
}
