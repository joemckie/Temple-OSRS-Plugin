package com.templeosrs.util.collections;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.util.Arrays;

public class CollectionLogItemContainerChangedSubscriber
{
    @Inject
    private CollectionLogManager collectionLogManager;

    @Inject
    private EventBus eventBus;

    @Inject
    private ItemManager itemManager;

    private final Multiset<Integer> inventoryItems = HashMultiset.create();

    void startUp()
    {
        eventBus.register(this);
    }

    void shutDown()
    {
        eventBus.unregister(this);
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged itemContainerChanged)
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
            int itemCount = item.getCount();

            if (collectionLogManager.obtainedItemNames.contains(itemName)) {
                int index = collectionLogManager.lookupCollectionLogItemIndex(itemId);

                collectionLogManager.clogItemsBitSet.set(index);
                collectionLogManager.clogItemsCountSet.put(itemId, itemCount);

                collectionLogManager.obtainedItemNames.remove(itemName);
            }
        }

        inventoryItems.clear();
        inventoryItems.addAll(currentInventoryItems);
    }
}
