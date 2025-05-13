package com.templeosrs.util.collections.autosync;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class CollectionLogAutoSyncGameTickSubscriber
{
    @Inject
    private CollectionLogAutoSyncManager collectionLogAutoSyncManager;

    @Inject
    private Client client;

    @Inject
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * Listens for game ticks and checks if the sync countdown has completed.
     * Once the countdown is complete, and there are items pending a sync
     * it will upload any newly obtained items to the server.
     */
    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        final Integer gameTickToSync = collectionLogAutoSyncManager.getGameTickToSync();
        final HashSet<Pair<String, Integer>> pendingSyncItems = collectionLogAutoSyncManager.getPendingSyncItems();

        // Note: There shouldn't be an instance of gameTickToSync
        // being non-null without items in the pendingSyncItems set.
        if (gameTickToSync == null || pendingSyncItems.isEmpty()) {
            return;
        }

        // Sync new collection log items when the game has reached correct tick.
        if (client.getTickCount() >= gameTickToSync) {
            collectionLogAutoSyncManager.resetSyncCountdown();
            scheduledExecutorService.execute(collectionLogAutoSyncManager::uploadObtainedCollectionLogItems);
        }
    }
}
