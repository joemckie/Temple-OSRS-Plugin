package com.templeosrs.collectionlog.autosync;

import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CollectionLogAutoSyncItemContainerChangedSubscriberTest extends MockedCollectionLogAutoSyncTest
{
    @BeforeEach
    // In-game, the chat message triggers before the inventory is updated with new items, so emulate this in the tests
    void setupChatMessageEventAndTrigger()
    {
        triggerNewCollectionLogItemChatMessageEvent("Twisted bow");
    }
    
    @Test
    @DisplayName("Ensure the pending items are not modified when the changed container is not the inventory")
    void doesNotAddToPendingItemsForNonInventoryContainers()
    {
        ItemContainerChanged itemContainerChanged = buildItemContainerChangedEvent(
                InventoryID.DREAM_BANK_INVENTORY,
                new Item[]{}
        );

        eventBus.post(itemContainerChanged);

        assertEquals(new HashSet<>(), collectionLogAutoSyncManager.getPendingSyncItems());
    }

    @Test
    @DisplayName("Ensure the pending items are not modified when no inventory items are found in the obtained items list")
    void doesNotAddToPendingItemsWhenNoInventoryItemsMatchObtainedItems()
    {
        final Item[] mockItems = {
                new Item(ItemID.BONES, 1)
        };

        ItemContainerChanged itemContainerChanged = buildItemContainerChangedEvent(
                InventoryID.INV,
                mockItems
        );

        eventBus.post(itemContainerChanged);

        assertEquals(new HashSet<>(), collectionLogAutoSyncManager.getPendingSyncItems());
    }

    @Test
    @DisplayName("Ensure the pending items are modified when inventory items are found in the obtained items list")
    void addsToPendingItemsWhenInventoryItemsMatchObtainedItems()
    {
        final Item[] mockItems = {
                new Item(ItemID.TWISTED_BOW, 1)
        };

        ItemContainerChanged itemContainerChanged = buildItemContainerChangedEvent(
                InventoryID.INV,
                mockItems
        );

        when(itemComposition.getName()).thenReturn("Twisted bow");

        eventBus.post(itemContainerChanged);

        HashSet<Integer> expectedHashSet = new HashSet<>();
        expectedHashSet.add(ItemID.TWISTED_BOW);

        assertEquals(expectedHashSet, collectionLogAutoSyncManager.getPendingSyncItems());
    }

    @Test
    @DisplayName("Ensure the sync countdown is started when inventory items are found in the obtained items list")
    void startsSyncCountdownWhenInventoryItemsMatchObtainedItems()
    {
        final Item[] mockItems = {
                new Item(ItemID.TWISTED_BOW, 1)
        };
        
        ItemContainerChanged itemContainerChanged = buildItemContainerChangedEvent(
                InventoryID.INV,
                mockItems
        );
        
        when(itemComposition.getName()).thenReturn("Twisted bow");
        when(client.getTickCount()).thenReturn(100);
        
        eventBus.post(itemContainerChanged);
        
        assertEquals(117, collectionLogAutoSyncManager.getGameTickToSync());
    }

    @Test
    @DisplayName("Ensure any items with duplicate names are assigned to their item ID")
    void duplicateItemNamesAreAssignedToItemId()
    {
        collectionLogAutoSyncManager.getObtainedItemNames().clear();

        triggerNewCollectionLogItemChatMessageEvent("Graceful boots");

        final Item[] mockItems = { new Item(ItemID.GRACEFUL_BOOTS, 1) };
        final HashSet<Integer> expectedHashSet = new HashSet<>();

        ItemContainerChanged itemContainerChanged = new ItemContainerChanged(InventoryID.INV, itemContainer);
        when(itemContainer.getItems()).thenReturn(mockItems);

        ItemComposition gracefulBootsItemComposition = spy(ItemComposition.class);
        when(gracefulBootsItemComposition.getName()).thenReturn("Graceful boots");
        when(itemManager.getItemComposition(ItemID.GRACEFUL_BOOTS)).thenReturn(gracefulBootsItemComposition);

        expectedHashSet.add(ItemID.GRACEFUL_BOOTS);

        eventBus.post(itemContainerChanged);

        assertEquals(expectedHashSet, collectionLogAutoSyncManager.getPendingSyncItems());

        // Trigger another collection log event that has a duplicate name to an existing item in the inventory
        triggerNewCollectionLogItemChatMessageEvent("Graceful boots");

        final Item[] mockItems2 = {
                new Item(ItemID.GRACEFUL_BOOTS_WYRM, 1)
        };

        ItemContainerChanged itemContainerChanged2 = new ItemContainerChanged(InventoryID.INV, itemContainer);

        when(itemContainer.getItems()).thenReturn(mockItems2);

        ItemComposition gracefulBootsWyrmItemComposition = spy(ItemComposition.class);
        when(gracefulBootsWyrmItemComposition.getName()).thenReturn("Graceful boots");
        when(itemManager.getItemComposition(ItemID.GRACEFUL_BOOTS_WYRM)).thenReturn(gracefulBootsWyrmItemComposition);

        expectedHashSet.add(ItemID.GRACEFUL_BOOTS_WYRM);

        eventBus.post(itemContainerChanged2);

        assertEquals(expectedHashSet, collectionLogAutoSyncManager.getPendingSyncItems());
    }
}
