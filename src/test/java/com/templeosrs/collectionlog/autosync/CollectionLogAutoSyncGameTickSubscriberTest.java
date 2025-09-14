package com.templeosrs.collectionlog.autosync;

import com.templeosrs.util.collections.data.ObtainedCollectionItem;
import net.runelite.api.events.GameTick;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CollectionLogAutoSyncGameTickSubscriberTest extends MockedCollectionLogAutoSyncTest
{
	@Test
	@DisplayName("Ensure no request is made if the game tick to sync has not been set")
	void doesNotRequestIfGameTickToSyncIsUnset()
	{
		GameTick gameTick = new GameTick();

		collectionLogAutoSyncManager.getPendingSyncItems().add(
			new ObtainedCollectionItem(1, "Item name", 1)
		);

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
		collectionLogAutoSyncManager.getPendingSyncItems().add(
			new ObtainedCollectionItem(1, "Item name", 1)
		);

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
