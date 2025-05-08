package com.templeosrs.util.collections.autosync;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.util.Arrays;

@Slf4j
public class CollectionLogAutoSyncItemContainerChangedSubscriber
{
    @Inject
    private CollectionLogAutoSyncManager collectionLogAutoSyncManager;

    @Inject
    private EventBus eventBus;

    @Inject
    private ItemManager itemManager;

    private final Multiset<Integer> inventoryItems = HashMultiset.create();

    public void startUp()
    {
        eventBus.register(this);
    }

    public void shutDown()
    {
        eventBus.unregister(this);
    }

    @Subscribe
    private void onItemContainerChanged(ItemContainerChanged itemContainerChanged)
    {
        if (itemContainerChanged.getContainerId() != InventoryID.INV) {
            return;
        }

        final ItemContainer inventory = itemContainerChanged.getItemContainer();
        final Multiset<Integer> currentInventoryItems = HashMultiset.create();

        Arrays.stream(inventory.getItems()).forEach(
                item -> currentInventoryItems.add(item.getId(), item.getQuantity())
        );

        final Multiset<Integer> inventoryDifference = Multisets.difference(currentInventoryItems, inventoryItems);

        for (Multiset.Entry<Integer> item : inventoryDifference.entrySet())
        {
            String itemName = itemManager.getItemComposition(item.getElement()).getName();
            int itemId = item.getElement();

            if (collectionLogAutoSyncManager.obtainedItemNames.contains(itemName)) {
                collectionLogAutoSyncManager.pendingSyncItems.add(itemId);
                collectionLogAutoSyncManager.obtainedItemNames.remove(itemName);
            }
        }

        inventoryItems.clear();
        inventoryItems.addAll(currentInventoryItems);
        
        collectionLogAutoSyncManager.startSyncCountdown();
    }
}
