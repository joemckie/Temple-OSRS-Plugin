package com.templeosrs.collectionlog.autosync;

import com.google.inject.testing.fieldbinder.Bind;
import com.templeosrs.MockedTest;
import com.templeosrs.util.collections.CollectionLogRequestManager;
import com.templeosrs.util.collections.autosync.CollectionLogAutoSyncGameTickSubscriber;
import com.templeosrs.util.collections.autosync.CollectionLogAutoSyncManager;
import net.runelite.api.events.GameTick;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class CollectionLogAutoSyncGameTickSubscriberTest extends MockedTest
{
    @Bind
    protected final CollectionLogAutoSyncManager collectionLogAutoSyncManager = spy(CollectionLogAutoSyncManager.class);

    @Bind
    protected final CollectionLogAutoSyncGameTickSubscriber gameTickSubscriber = spy(CollectionLogAutoSyncGameTickSubscriber.class);

    @Bind
    protected final CollectionLogRequestManager collectionLogRequestManager = spy(CollectionLogRequestManager.class);
    
    @BeforeEach
    void registerWithEventBus()
    {
        gameTickSubscriber.startUp();
    }
    
    @AfterEach
    void unregisterWithEventBus()
    {
        gameTickSubscriber.shutDown();
    }

    @Test
    @DisplayName("Ensure no request is made if the game tick to sync has not been set")
    void doesNotRequestIfGameTickToSyncIsUnset()
    {
        GameTick gameTick = new GameTick();

        collectionLogAutoSyncManager.getPendingSyncItems().add(1);

        eventBus.post(gameTick);

        verify(scheduledExecutorService, never()).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("Ensure no request is made if the pending sync items is empty")
    void doesNotRequestIfPendingSyncItemsIsEmpty()
    {
        GameTick gameTick = new GameTick();

        collectionLogAutoSyncManager.startSyncCountdown();

        eventBus.post(gameTick);

        verify(scheduledExecutorService, never()).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("Ensure a request is made if the game tick to sync is less than or equal to the client game tick, and the pending sync items list is not empty")
    void requestsIfGameTickToSyncHasPassedAndItemsArePendingSync()
    {
        GameTick gameTick = new GameTick();

        collectionLogAutoSyncManager.startSyncCountdown();
        collectionLogAutoSyncManager.getPendingSyncItems().add(1);

        final String username = "CousinOfKos";
        final long accountHash = 1234567890;

        when(player.getName()).thenReturn(username);
        when(client.getLocalPlayer()).thenReturn(player);
        when(client.getAccountHash()).thenReturn(accountHash);
        when(client.getTickCount()).thenReturn(100);

        eventBus.post(gameTick);

        verify(scheduledExecutorService, times(1)).execute(any(Runnable.class));
    }
}
