package com.templeosrs.collectionlog;

import com.google.inject.testing.fieldbinder.Bind;
import com.templeosrs.MockedTest;
import com.templeosrs.util.collections.CollectionLogChatMessageSubscriber;
import com.templeosrs.util.collections.CollectionLogItemContainerChangedSubscriber;
import com.templeosrs.util.collections.CollectionLogManager;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CollectionLogItemContainerChangedSubscriberTest extends MockedTest
{
    @Bind
    protected final CollectionLogManager collectionLogManager = spy(CollectionLogManager.class);

    @Bind
    protected final CollectionLogItemContainerChangedSubscriber collectionLogItemContainerChangedSubscriber = spy(CollectionLogItemContainerChangedSubscriber.class);

    @Bind
    protected final CollectionLogChatMessageSubscriber collectionLogChatMessageSubscriber = spy(CollectionLogChatMessageSubscriber.class);

    @BeforeEach
    // In-game, the chat message triggers before the inventory is updated with new items, so emulate this in the tests
    void setupChatMessageEventAndTrigger()
    {
        triggerChatMessageEvent("Twisted bow");
    }

    @Test
    @DisplayName("Ensure the bitset and item counts are not modified when the changed container is not the inventory")
    void doesNotAddToBitSetForNonInventoryContainers()
    {
        ItemContainerChanged itemContainerChanged = buildItemContainerChangedEvent(
                InventoryID.DREAM_BANK_INVENTORY,
                new Item[]{}
        );

        collectionLogItemContainerChangedSubscriber.onItemContainerChanged(itemContainerChanged);

        assertEquals(new BitSet(), collectionLogManager.getClogItemsBitSet());
        assertEquals(new HashMap<>(), collectionLogManager.getClogItemsCountSet());
    }

    @Test
    @DisplayName("Ensure the bitset and item counts are not modified when no inventory items are found in the obtained items list")
    void doesNotAddToBitSetOrItemCountsWhenNoInventoryItemsMatchObtainedItems()
    {
        final Item[] mockItems = {
                new Item(ItemID.BONES, 1)
        };

        ItemContainerChanged itemContainerChanged = buildItemContainerChangedEvent(
                InventoryID.INV,
                mockItems
        );

        collectionLogItemContainerChangedSubscriber.onItemContainerChanged(itemContainerChanged);

        assertEquals(new BitSet(), collectionLogManager.getClogItemsBitSet());
        assertEquals(null, collectionLogManager.getClogItemsCountSet().get(ItemID.BONES));
    }

    @Test
    @DisplayName("Ensure the bitset and item counts are modified when inventory items are found in the obtained items list")
    void addsToBitSetWhenInventoryItemsMatchObtainedItems()
    {
        final Item[] mockItems = {
                new Item(ItemID.TWISTED_BOW, 1)
        };

        ItemContainerChanged itemContainerChanged = buildItemContainerChangedEvent(
                InventoryID.INV,
                mockItems
        );

        final int expectedIndex = 0;

        when(itemComposition.getName()).thenReturn("Twisted bow");

        collectionLogItemContainerChangedSubscriber.onItemContainerChanged(itemContainerChanged);

        BitSet expectedBitset = new BitSet();
        expectedBitset.set(expectedIndex);

        assertEquals(expectedBitset, collectionLogManager.getClogItemsBitSet());
        assertEquals(1, collectionLogManager.getClogItemsCountSet().get(ItemID.TWISTED_BOW));
    }

    @Test
    @DisplayName("Ensure any items with duplicate names are assigned to their item ID")
    void duplicateItemNamesAreAssignedToItemId()
    {
        collectionLogManager.getObtainedItemNames().clear();
        collectionLogManager.getClogItemsCountSet().put(ItemID.GRACEFUL_BOOTS, 0);
        collectionLogManager.getClogItemsCountSet().put(ItemID.GRACEFUL_BOOTS_WYRM, 0);

        triggerChatMessageEvent("Graceful boots");

        final Item[] mockItems = { new Item(ItemID.GRACEFUL_BOOTS, 1) };
        final BitSet expectedBitSet = new BitSet();

        collectionLogManager.getCollectionLogItemIdToBitsetIndex().put(ItemID.GRACEFUL_BOOTS, 1);
        collectionLogManager.getCollectionLogItemIdToBitsetIndex().put(ItemID.GRACEFUL_BOOTS_WYRM, 2);

        ItemContainerChanged itemContainerChanged = new ItemContainerChanged(InventoryID.INV, itemContainer);
        when(itemContainer.getItems()).thenReturn(mockItems);

        ItemComposition gracefulBootsItemComposition = spy(ItemComposition.class);
        when(gracefulBootsItemComposition.getName()).thenReturn("Graceful boots");
        when(itemManager.getItemComposition(ItemID.GRACEFUL_BOOTS)).thenReturn(gracefulBootsItemComposition);

        expectedBitSet.set(1);

        collectionLogItemContainerChangedSubscriber.onItemContainerChanged(itemContainerChanged);

        assertEquals(expectedBitSet, collectionLogManager.getClogItemsBitSet());
        assertEquals(1, collectionLogManager.getClogItemsCountSet().get(ItemID.GRACEFUL_BOOTS));

        // Trigger another collection log event that has a duplicate name to an existing item in the inventory
        triggerChatMessageEvent("Graceful boots");

        final Item[] mockItems2 = { new Item(ItemID.GRACEFUL_BOOTS_WYRM, 1) };

        ItemContainerChanged itemContainerChanged2 = new ItemContainerChanged(InventoryID.INV, itemContainer);

        when(itemContainer.getItems()).thenReturn(mockItems2);

        ItemComposition gracefulBootsWyrmItemComposition = spy(ItemComposition.class);
        when(gracefulBootsWyrmItemComposition.getName()).thenReturn("Graceful boots");
        when(itemManager.getItemComposition(ItemID.GRACEFUL_BOOTS_WYRM)).thenReturn(gracefulBootsWyrmItemComposition);

        expectedBitSet.set(2);

        collectionLogItemContainerChangedSubscriber.onItemContainerChanged(itemContainerChanged2);

        assertEquals(expectedBitSet, collectionLogManager.getClogItemsBitSet());
        assertEquals(1, collectionLogManager.getClogItemsCountSet().get(ItemID.GRACEFUL_BOOTS_WYRM));
    }

    void triggerChatMessageEvent(String itemName)
    {
        final ChatMessage chatMessage = new ChatMessage(
                null,
                ChatMessageType.GAMEMESSAGE,
                "",
                String.format("New item added to your collection log: %s", itemName),
                "",
                0
        );

        collectionLogChatMessageSubscriber.onChatMessage(chatMessage);
    }

    private ItemContainerChanged buildItemContainerChangedEvent(int inventoryID, Item[] items)
    {
        for (int i = 0; i < items.length; i++)
        {
            collectionLogManager.getCollectionLogItemIdToBitsetIndex().put(items[i].getId(), i);
            when(itemManager.getItemComposition(items[i].getId())).thenReturn(itemComposition);
        }

        when(itemContainer.getItems()).thenReturn(items);

        return new ItemContainerChanged(inventoryID, itemContainer);
    }
}
