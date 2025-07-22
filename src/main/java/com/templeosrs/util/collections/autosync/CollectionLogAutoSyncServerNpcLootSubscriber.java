package com.templeosrs.util.collections.autosync;

import com.templeosrs.util.collections.data.CollectionLogItem;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;

@Slf4j
public class CollectionLogAutoSyncServerNpcLootSubscriber
{
	@Inject
	private CollectionLogAutoSyncManager collectionLogAutoSyncManager;

	@Inject
	private ItemManager itemManager;

	/**
	 * This method is called when an NPC loot event is received.
	 * If any items in the loot match the newly obtained collection log item names,
	 * they are added to a list of items awaiting a server sync and the sync countdown is started.
	 */
	@Subscribe
	private void onServerNpcLoot(ServerNpcLoot serverNpcLoot)
	{
		if (collectionLogAutoSyncManager.obtainedItemNames.isEmpty())
		{
			return;
		}

		final int previousPendingSyncItemsCount = collectionLogAutoSyncManager.pendingSyncItems.size();

		serverNpcLoot.getItems().forEach(item ->
		{
			final int itemId = item.getId();
			final int itemCount = item.getQuantity();
			final String itemName = itemManager.getItemComposition(itemId).getName();

			if (collectionLogAutoSyncManager.obtainedItemNames.contains(itemName))
			{
				collectionLogAutoSyncManager.pendingSyncItems.add(new CollectionLogItem(itemId, itemName, itemCount));
				collectionLogAutoSyncManager.obtainedItemNames.remove(itemName);
			}
		});
		
		if (previousPendingSyncItemsCount < collectionLogAutoSyncManager.pendingSyncItems.size())
		{
			collectionLogAutoSyncManager.startSyncCountdown();
		}
	}
}
