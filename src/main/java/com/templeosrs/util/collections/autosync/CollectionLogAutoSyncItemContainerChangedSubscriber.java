package com.templeosrs.util.collections.autosync;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.templeosrs.util.collections.data.ObtainedCollectionItem;
import java.util.Arrays;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;

@Slf4j
public class CollectionLogAutoSyncItemContainerChangedSubscriber
{
	private final Multiset<Integer> inventoryItems = HashMultiset.create();

	@Inject
	private CollectionLogAutoSyncManager collectionLogAutoSyncManager;
	
	@Inject
	private ItemManager itemManager;

	/**
	 * This method is called when the item container changes, specifically when the inventory is updated.
	 * If any items in the inventory match the newly obtained collection log item names,
	 * they are added to a list of items awaiting a server sync and the sync countdown is started.
	 */
	@Subscribe
	private void onItemContainerChanged(ItemContainerChanged itemContainerChanged)
	{
		if (itemContainerChanged.getContainerId() != InventoryID.INV)
		{
			return;
		}

		final ItemContainer inventory = itemContainerChanged.getItemContainer();
		final Multiset<Integer> currentInventoryItems = HashMultiset.create();

		Arrays.stream(inventory.getItems()).forEach(
			item -> currentInventoryItems.add(item.getId(), item.getQuantity())
		);

		final Multiset<Integer> inventoryDifference = Multisets.difference(currentInventoryItems, inventoryItems);

		final int previousPendingSyncItemsCount = collectionLogAutoSyncManager.pendingSyncItems.size();

		inventoryDifference.entrySet().forEach(item -> {
			String itemName = itemManager.getItemComposition(item.getElement()).getName();
			int itemId = item.getElement();
			int itemCount = item.getCount();

			if (collectionLogAutoSyncManager.obtainedItemNames.contains(itemName))
			{
				collectionLogAutoSyncManager.pendingSyncItems.add(new ObtainedCollectionItem(itemId, itemName, itemCount));
				collectionLogAutoSyncManager.obtainedItemNames.remove(itemName);
			}
		});

		// Overwrite the cached inventory items with the current inventory items
		// to provide a comparator for the next inventory change event
		inventoryItems.clear();
		inventoryItems.addAll(currentInventoryItems);

		if (previousPendingSyncItemsCount < collectionLogAutoSyncManager.pendingSyncItems.size())
		{
			collectionLogAutoSyncManager.startSyncCountdown();
		}
	}
}
