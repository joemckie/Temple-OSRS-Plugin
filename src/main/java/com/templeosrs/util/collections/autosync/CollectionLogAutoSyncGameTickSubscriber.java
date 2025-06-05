package com.templeosrs.util.collections.autosync;

import com.templeosrs.util.api.QuadraticBackoffStrategy;
import com.templeosrs.util.collections.CollectionLogManager;
import com.templeosrs.util.collections.data.ObtainedCollectionItem;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class CollectionLogAutoSyncGameTickSubscriber
{
    @Inject
    private CollectionLogAutoSyncManager collectionLogAutoSyncManager;

    @Inject
    private CollectionLogManager collectionLogManager;

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
        QuadraticBackoffStrategy backoffStrategy = collectionLogAutoSyncManager.getBackoffStrategy();

        if (backoffStrategy.isRequestLimitReached()) {
            collectionLogAutoSyncManager.clearSyncCountdown();
            return;
        }

        if (backoffStrategy.isSubmitting() || collectionLogAutoSyncManager.isComputingDiff()) {
            return;
        }

        final Integer gameTickToSync = collectionLogAutoSyncManager.getGameTickToSync();
        final HashSet<ObtainedCollectionItem> pendingSyncItems = collectionLogAutoSyncManager.getPendingSyncItems();

        // Add any diffed items to the pendingSync list when the log has been opened
        if (
            gameTickToSync != null &&
            client.getTickCount() >= gameTickToSync &&
            collectionLogAutoSyncManager.isLogOpenAutoSync() &&
            pendingSyncItems.isEmpty()
        ) {
            collectionLogAutoSyncManager.setComputingDiff(true);
            scheduledExecutorService.execute(collectionLogAutoSyncManager::computeCollectionLogDiff);

            return;
        }

        if (gameTickToSync == null || pendingSyncItems.isEmpty()) {
            return;
        }

        // Sync new collection log items when the game has reached correct tick.
        if (client.getTickCount() >= gameTickToSync) {
            backoffStrategy.setSubmitting(true);
            scheduledExecutorService.execute(collectionLogAutoSyncManager::uploadObtainedCollectionLogItems);
        }
    }
}
